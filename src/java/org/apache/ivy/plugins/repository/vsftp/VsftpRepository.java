/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.repository.vsftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyThread;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Message;

/**
 * Repository using SecureCRT vsftp command line program to access an sftp repository This is
 * especially useful to leverage the gssapi authentication supported by SecureCRT. In caseswhere
 * usual sftp is enough, prefer the 100% java solution of sftp repository. This requires SecureCRT
 * to be in the PATH. Tested with SecureCRT 5.0.5
 */
public class VsftpRepository extends AbstractRepository {
    private static final int LS_DATE_INDEX4 = 7;

    private static final int LS_DATE_INDEX3 = 6;

    private static final int LS_DATE_INDEX2 = 5;

    private static final int LS_DATE_INDEX1 = 4;

    private static final int LS_SIZE_INDEX = 3;

    private static final int LS_PARTS_NUMBER = 9;

    private static final int DISCONNECT_COMMAND_TIMEOUT = 300;

    private static final int REUSE_CONNECTION_SLEEP_TIME = 10;

    private static final int READER_ALIVE_SLEEP_TIME = 100;

    private static final int MAX_READER_ALIVE_ATTEMPT = 5;

    private static final int ERROR_SLEEP_TIME = 30;

    private static final int PROMPT_SLEEP_TIME = 50;

    private static final int MAX_READ_PROMPT_ATTEMPT = 5;

    private static final int GET_JOIN_MAX_TIME = 100;

    private static final int DEFAULT_REUSE_CONNECTION_TIME = 300000;

    // reuse connection during 5 minutes by default

    private static final int DEFAULT_READ_TIMEOUT = 30000;

    private static final String PROMPT = "vsftp> ";

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm",
            Locale.US);

    private String host;

    private String username;

    private String authentication = "gssapi";

    private Reader in;

    private Reader err;

    private PrintWriter out;

    private volatile StringBuffer errors = new StringBuffer();

    private long readTimeout = DEFAULT_READ_TIMEOUT;

    private long reuseConnection = DEFAULT_REUSE_CONNECTION_TIME;

    private volatile long lastCommand;

    private volatile boolean inCommand;

    private Process process;

    private Thread connectionCleaner;

    private Thread errorsReader;

    private volatile long errorsLastUpdateTime;

    private Ivy ivy = null;

    public Resource getResource(String source) throws IOException {
        initIvy();
        return new VsftpResource(this, source);
    }

    private void initIvy() {
        ivy = IvyContext.getContext().getIvy();
    }

    protected Resource getInitResource(String source) throws IOException {
        try {
            return lslToResource(source, sendCommand("ls -l " + source, true, true));
        } catch (IOException ex) {
            cleanup(ex);
            throw ex;
        } finally {
            cleanup();
        }
    }

    public void get(final String source, File destination) throws IOException {
        initIvy();
        try {
            fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
            File destDir = destination.getParentFile();
            if (destDir != null) {
                sendCommand("lcd " + destDir.getAbsolutePath());
            }
            if (destination.exists()) {
                destination.delete();
            }

            int index = source.lastIndexOf('/');
            String srcName = index == -1 ? source : source.substring(index + 1);
            final File to = destDir == null ? Checks.checkAbsolute(srcName, "source") : new File(
                    destDir, srcName);

            final IOException[] ex = new IOException[1];
            Thread get = new IvyThread() {
                public void run() {
                    initContext();
                    try {
                        sendCommand("get " + source, getExpectedDownloadMessage(source, to), 0);
                    } catch (IOException e) {
                        ex[0] = e;
                    }
                }
            };
            get.start();

            long prevLength = 0;
            long lastUpdate = System.currentTimeMillis();
            long timeout = readTimeout;
            while (get.isAlive()) {
                checkInterrupted();
                long length = to.exists() ? to.length() : 0;
                if (length > prevLength) {
                    fireTransferProgress(length - prevLength);
                    lastUpdate = System.currentTimeMillis();
                    prevLength = length;
                } else {
                    if (System.currentTimeMillis() - lastUpdate > timeout) {
                        Message.verbose("download hang for more than " + timeout
                                + "ms. Interrupting.");
                        get.interrupt();
                        if (to.exists()) {
                            to.delete();
                        }
                        throw new IOException(source + " download timeout from " + getHost());
                    }
                }
                try {
                    get.join(GET_JOIN_MAX_TIME);
                } catch (InterruptedException e) {
                    if (to.exists()) {
                        to.delete();
                    }
                    return;
                }
            }
            if (ex[0] != null) {
                if (to.exists()) {
                    to.delete();
                }
                throw ex[0];
            }

            to.renameTo(destination);
            fireTransferCompleted(destination.length());
        } catch (IOException ex) {
            fireTransferError(ex);
            cleanup(ex);
            throw ex;
        } finally {
            cleanup();
        }
    }

    public List list(String parent) throws IOException {
        initIvy();
        try {
            if (!parent.endsWith("/")) {
                parent = parent + "/";
            }
            String response = sendCommand("ls -l " + parent, true, true);
            if (response.startsWith("ls")) {
                return null;
            }
            String[] lines = response.split("\n");
            List ret = new ArrayList(lines.length);
            for (int i = 0; i < lines.length; i++) {
                while (lines[i].endsWith("\r") || lines[i].endsWith("\n")) {
                    lines[i] = lines[i].substring(0, lines[i].length() - 1);
                }
                if (lines[i].trim().length() != 0) {
                    ret.add(parent + lines[i].substring(lines[i].lastIndexOf(' ') + 1));
                }
            }
            return ret;
        } catch (IOException ex) {
            cleanup(ex);
            throw ex;
        } finally {
            cleanup();
        }
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        initIvy();
        try {
            if (getResource(destination).exists()) {
                if (overwrite) {
                    sendCommand("rm " + destination, getExpectedRemoveMessage(destination));
                } else {
                    return;
                }
            }
            int index = destination.lastIndexOf('/');
            String destDir = null;
            if (index != -1) {
                destDir = destination.substring(0, index);
                mkdirs(destDir);
                sendCommand("cd " + destDir);
            }
            String to = destDir != null ? destDir + "/" + source.getName() : source.getName();
            sendCommand("put " + source.getAbsolutePath(), getExpectedUploadMessage(source, to), 0);
            sendCommand("mv " + to + " " + destination);
        } catch (IOException ex) {
            cleanup(ex);
            throw ex;
        } finally {
            cleanup();
        }
    }

    private void mkdirs(String destDir) throws IOException {
        if (dirExists(destDir)) {
            return;
        }
        if (destDir.endsWith("/")) {
            destDir = destDir.substring(0, destDir.length() - 1);
        }
        int index = destDir.lastIndexOf('/');
        if (index != -1) {
            mkdirs(destDir.substring(0, index));
        }
        sendCommand("mkdir " + destDir);
    }

    private boolean dirExists(String dir) throws IOException {
        return !sendCommand("ls " + dir, true).startsWith("ls: ");
    }

    protected String sendCommand(String command) throws IOException {
        return sendCommand(command, false, readTimeout);
    }

    protected void sendCommand(String command, Pattern expectedResponse) throws IOException {
        sendCommand(command, expectedResponse, readTimeout);
    }

    /**
     * The behaviour of vsftp with some commands is to log the resulting message on the error
     * stream, even if everything is ok. So it's quite difficult if there was an error or not. Hence
     * we compare the response with the expected message and deal with it. The problem is that this
     * is very specific to the version of vsftp used for the test, That's why expected messages are
     * obtained using overridable protected methods.
     */
    protected void sendCommand(String command, Pattern expectedResponse, long timeout)
            throws IOException {
        String response = sendCommand(command, true, timeout);
        if (!expectedResponse.matcher(response).matches()) {
            Message.debug("invalid response from server:");
            Message.debug("expected: '" + expectedResponse + "'");
            Message.debug("was:      '" + response + "'");
            throw new IOException(response);
        }
    }

    protected String sendCommand(String command, boolean sendErrorAsResponse) throws IOException {
        return sendCommand(command, sendErrorAsResponse, readTimeout);
    }

    protected String sendCommand(String command, boolean sendErrorAsResponse, boolean single)
            throws IOException {
        return sendCommand(command, sendErrorAsResponse, single, readTimeout);
    }

    protected String sendCommand(String command, boolean sendErrorAsResponse, long timeout)
            throws IOException {
        return sendCommand(command, sendErrorAsResponse, false, timeout);
    }

    protected String sendCommand(String command, boolean sendErrorAsResponse, boolean single,
            long timeout) throws IOException {
        single = false; // use of alone commands does not work properly due to a long delay between
        // end of process and end of stream...

        checkInterrupted();
        inCommand = true;
        errorsLastUpdateTime = 0;
        synchronized (this) {
            if (!single || in != null) {
                ensureConnectionOpened();
                Message.debug("sending command '" + command + "' to " + getHost());
                updateLastCommandTime();
                out.println(command);
                out.flush();
            } else {
                sendSingleCommand(command);
            }
        }

        try {
            return readResponse(sendErrorAsResponse, timeout);
        } finally {
            inCommand = false;
            if (single) {
                closeConnection();
            }
        }
    }

    protected String readResponse(boolean sendErrorAsResponse) throws IOException {
        return readResponse(sendErrorAsResponse, readTimeout);
    }

    protected synchronized String readResponse(final boolean sendErrorAsResponse, long timeout)
            throws IOException {
        final StringBuffer response = new StringBuffer();
        final IOException[] exc = new IOException[1];
        final boolean[] done = new boolean[1];
        Runnable r = new Runnable() {
            public void run() {
                synchronized (VsftpRepository.this) {
                    try {
                        int c;
                        boolean getPrompt = false;
                        // the reading is done in a for loop making five attempts to read the stream
                        // if we do not reach the next prompt
                        for (int attempts = 0; !getPrompt && attempts < MAX_READ_PROMPT_ATTEMPT; attempts++) {
                            while ((c = in.read()) != -1) {
                                attempts = 0; // we manage to read something, reset numer of
                                // attempts
                                response.append((char) c);
                                if (response.length() >= PROMPT.length()
                                        && response.substring(response.length() - PROMPT.length(),
                                            response.length()).equals(PROMPT)) {
                                    response.setLength(response.length() - PROMPT.length());
                                    getPrompt = true;
                                    break;
                                }
                            }
                            if (!getPrompt) {
                                try {
                                    Thread.sleep(PROMPT_SLEEP_TIME);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        }
                        if (getPrompt) {
                            // wait enough for error stream to be fully read
                            if (errorsLastUpdateTime == 0) {
                                // no error written yet, but it may be pending...
                                errorsLastUpdateTime = lastCommand;
                            }

                            while ((System.currentTimeMillis() - errorsLastUpdateTime) < PROMPT_SLEEP_TIME) {
                                try {
                                    Thread.sleep(ERROR_SLEEP_TIME);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        }
                        if (errors.length() > 0) {
                            if (sendErrorAsResponse) {
                                response.append(errors);
                                errors.setLength(0);
                            } else {
                                throw new IOException(chomp(errors).toString());
                            }
                        }
                        chomp(response);
                        done[0] = true;
                    } catch (IOException e) {
                        exc[0] = e;
                    } finally {
                        VsftpRepository.this.notify();
                    }
                }
            }
        };
        Thread reader = null;
        if (timeout == 0) {
            r.run();
        } else {
            reader = new IvyThread(r);
            reader.start();
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                // nothing to do
            }
        }
        updateLastCommandTime();
        if (exc[0] != null) {
            throw exc[0];
        } else if (!done[0]) {
            if (reader != null && reader.isAlive()) {
                reader.interrupt();
                for (int i = 0; i < MAX_READER_ALIVE_ATTEMPT && reader.isAlive(); i++) {
                    try {
                        Thread.sleep(READER_ALIVE_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                if (reader.isAlive()) {
                    reader.stop(); // no way to interrupt it non abruptly
                }
            }
            throw new IOException("connection timeout to " + getHost());
        } else {
            if ("Not connected.".equals(response.toString())) {
                Message.info("vsftp connection to " + getHost() + " reset");
                closeConnection();
                throw new IOException("not connected to " + getHost());
            }
            Message.debug("received response '" + response + "' from " + getHost());
            return response.toString();
        }
    }

    private synchronized void sendSingleCommand(String command) throws IOException {
        exec(getSingleCommand(command));
    }

    protected synchronized void ensureConnectionOpened() throws IOException {
        if (in == null) {
            Message.verbose("connecting to " + getUsername() + "@" + getHost() + "... ");
            String connectionCommand = getConnectionCommand();
            exec(connectionCommand);

            try {
                readResponse(false); // waits for first prompt

                if (reuseConnection > 0) {
                    connectionCleaner = new IvyThread() {
                        public void run() {
                            initContext();
                            try {
                                long sleep = REUSE_CONNECTION_SLEEP_TIME;
                                while (in != null && sleep > 0) {
                                    sleep(sleep);
                                    sleep = reuseConnection
                                            - (System.currentTimeMillis() - lastCommand);
                                    if (inCommand) {
                                        sleep = sleep <= 0 ? reuseConnection : sleep;
                                    }
                                }
                            } catch (InterruptedException e) {
                                // nothing to do
                            }
                            disconnect();
                        }
                    };
                    connectionCleaner.start();
                }

                if (ivy != null) {
                    ivy.getEventManager().addIvyListener(new IvyListener() {
                        public void progress(IvyEvent event) {
                            disconnect();
                            event.getSource().removeIvyListener(this);
                        }
                    }, EndResolveEvent.NAME);
                }

            } catch (IOException ex) {
                closeConnection();
                throw new IOException("impossible to connect to " + getUsername() + "@" + getHost()
                        + " using " + getAuthentication() + ": " + ex.getMessage());
            }
            Message.verbose("connected to " + getHost());
        }
    }

    private void updateLastCommandTime() {
        lastCommand = System.currentTimeMillis();
    }

    private void exec(String command) throws IOException {
        Message.debug("launching '" + command + "'");
        process = Runtime.getRuntime().exec(command);
        in = new InputStreamReader(process.getInputStream());
        err = new InputStreamReader(process.getErrorStream());
        out = new PrintWriter(process.getOutputStream());

        errorsReader = new IvyThread() {
            public void run() {
                initContext();
                int c;
                try {
                    // CheckStyle:InnerAssignment OFF
                    while (err != null && (c = err.read()) != -1) {
                        errors.append((char) c);
                        errorsLastUpdateTime = System.currentTimeMillis();
                    }
                    // CheckStyle:InnerAssignment ON
                } catch (IOException e) {
                    // nothing to do
                }
            }
        };
        errorsReader.start();
    }

    private void checkInterrupted() {
        if (ivy != null) {
            ivy.checkInterrupted();
        }
    }

    /**
     * Called whenever an api level method end
     */
    private void cleanup(Exception ex) {
        if (ex.getMessage().equals("connection timeout to " + getHost())) {
            closeConnection();
        } else {
            disconnect();
        }
    }

    /**
     * Called whenever an api level method end
     */
    private void cleanup() {
        if (reuseConnection == 0) {
            disconnect();
        }
    }

    public synchronized void disconnect() {
        if (in != null) {
            Message.verbose("disconnecting from " + getHost() + "... ");
            try {
                sendCommand("exit", false, DISCONNECT_COMMAND_TIMEOUT);
            } catch (IOException e) {
                // nothing I can do
            } finally {
                closeConnection();
                Message.verbose("disconnected of " + getHost());
            }
        }
    }

    private synchronized void closeConnection() {
        if (connectionCleaner != null) {
            connectionCleaner.interrupt();
        }
        if (errorsReader != null) {
            errorsReader.interrupt();
        }
        try {
            process.destroy();
        } catch (Exception ex) {
            // nothing I can do
        }
        try {
            in.close();
        } catch (Exception e) {
            // nothing I can do
        }
        try {
            err.close();
        } catch (Exception e) {
            // nothing I can do
        }
        try {
            out.close();
        } catch (Exception e) {
            // nothing I can do
        }

        connectionCleaner = null;
        errorsReader = null;
        process = null;
        in = null;
        out = null;
        err = null;
        Message.debug("connection to " + getHost() + " closed");
    }

    /**
     * Parses a ls -l line and transforms it in a resource
     * 
     * @param file
     * @param responseLine
     * @return
     */
    protected Resource lslToResource(String file, String responseLine) {
        if (responseLine == null || responseLine.startsWith("ls")) {
            return new BasicResource(file, false, 0, 0, false);
        } else {
            String[] parts = responseLine.split("\\s+");
            if (parts.length != LS_PARTS_NUMBER) {
                Message.debug("unrecognized ls format: " + responseLine);
                return new BasicResource(file, false, 0, 0, false);
            } else {
                try {
                    long contentLength = Long.parseLong(parts[LS_SIZE_INDEX]);
                    String date = parts[LS_DATE_INDEX1] + " " + parts[LS_DATE_INDEX2] + " "
                            + parts[LS_DATE_INDEX3] + " " + parts[LS_DATE_INDEX4];
                    return new BasicResource(file, true, contentLength, FORMAT.parse(date)
                            .getTime(), false);
                } catch (Exception ex) {
                    Message.warn("impossible to parse server response: " + responseLine, ex);
                    return new BasicResource(file, false, 0, 0, false);
                }
            }
        }
    }

    protected String getSingleCommand(String command) {
        return "vsh -noprompt -auth " + authentication + " " + username + "@" + host + " "
                + command;
    }

    protected String getConnectionCommand() {
        return "vsftp -noprompt -auth " + authentication + " " + username + "@" + host;
    }

    protected Pattern getExpectedDownloadMessage(String source, File to) {
        return Pattern.compile("Downloading " + to.getName() + " from [^\\s]+");
    }

    protected Pattern getExpectedRemoveMessage(String destination) {
        return Pattern.compile("Removing [^\\s]+");
    }

    protected Pattern getExpectedUploadMessage(File source, String to) {
        return Pattern.compile("Uploading " + source.getName() + " to [^\\s]+");
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private static StringBuffer chomp(StringBuffer str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        while ("\n".equals(str.substring(str.length() - 1))
                || "\r".equals(str.substring(str.length() - 1))) {
            str.setLength(str.length() - 1);
        }
        return str;
    }

    public String toString() {
        return getName() + " " + getUsername() + "@" + getHost() + " (" + getAuthentication() + ")";
    }

    /**
     * Sets the reuse connection time. The same connection will be reused if the time here does not
     * last between two commands. O indicates that the connection should never be reused
     * 
     * @param time
     */
    public void setReuseConnection(long time) {
        this.reuseConnection = time;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
}
