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
package org.apache.ivy.plugins.repository.ssh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * This class is using the scp client to transfer data and information for the repository. 
 * <p>
 * It is based on the SCPClient from the ganymed ssh library from Christian Plattner,
 * released under a BSD style license. 
 * <p>
 * To minimize the dependency to the ssh library and because we needed some additional 
 * functionality, we decided to copy'n'paste the single class rather than to inherit or 
 * delegate it somehow. 
 * <p>
 * Nevertheless credit should go to the original author.
 */
public class Scp {
    private static final int BUFFER_SIZE = 64 * 1024;

    /*
     * Maximum length authorized for scp lines. 
     * This is a random limit - if your path names are longer, then adjust it.
     */
    private static final int MAX_SCP_LINE_LENGTH = 8192;
    
    private Session session;

    public class FileInfo {
        private String filename;

        private long length;

        private long lastModified;

        /**
         * @param filename
         *            The filename to set.
         */
        public void setFilename(String filename) {
            this.filename = filename;
        }

        /**
         * @return Returns the filename.
         */
        public String getFilename() {
            return filename;
        }

        /**
         * @param length
         *            The length to set.
         */
        public void setLength(long length) {
            this.length = length;
        }

        /**
         * @return Returns the length.
         */
        public long getLength() {
            return length;
        }

        /**
         * @param lastModified
         *            The lastModified to set.
         */
        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        /**
         * @return Returns the lastModified.
         */
        public long getLastModified() {
            return lastModified;
        }
    }

    public Scp(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Cannot accept null argument!");
        }
        this.session = session;
    }

    private void readResponse(InputStream is) throws IOException, RemoteScpException {
        int c = is.read();

        if (c == 0) {
            return;
        }

        if (c == -1) {
            throw new RemoteScpException("Remote scp terminated unexpectedly.");
        }

        if ((c != 1) && (c != 2)) {
            throw new RemoteScpException("Remote scp sent illegal error code.");
        }

        if (c == 2) {
            throw new RemoteScpException("Remote scp terminated with error.");
        }

        String err = receiveLine(is);
        throw new RemoteScpException("Remote scp terminated with error (" + err + ").");
    }

    private String receiveLine(InputStream is) throws IOException, RemoteScpException {
        StringBuffer sb = new StringBuffer(30);

        while (true) {

            if (sb.length() > MAX_SCP_LINE_LENGTH) {
                throw new RemoteScpException("Remote scp sent a too long line");
            }

            int c = is.read();

            if (c < 0) {
                throw new RemoteScpException("Remote scp terminated unexpectedly.");
            }

            if (c == '\n') {
                break;
            }

            sb.append((char) c);

        }
        return sb.toString();
    }

    private void parseCLine(String line, FileInfo fileInfo) throws RemoteScpException {
        /* Minimum line: "xxxx y z" ---> 8 chars */

        long len;

        if (line.length() < 8) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, line too short.");
        }

        if ((line.charAt(4) != ' ') || (line.charAt(5) == ' ')) {
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");
        }

        int lengthNameSep = line.indexOf(' ', 5);

        if (lengthNameSep == -1) {
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");
        }

        String lengthSubstring = line.substring(5, lengthNameSep);
        String nameSubstring = line.substring(lengthNameSep + 1);

        if ((lengthSubstring.length() <= 0) || (nameSubstring.length() <= 0)) {
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");
        }

        if ((6 + lengthSubstring.length() + nameSubstring.length()) != line.length()) {
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");
        }

        try {
            len = Long.parseLong(lengthSubstring);
        } catch (NumberFormatException e) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, cannot parse file length.");
        }

        if (len < 0) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, illegal file length.");
        }

        fileInfo.setLength(len);
        fileInfo.setFilename(nameSubstring);
    }

    private void parseTLine(String line, FileInfo fileInfo) throws RemoteScpException {
        /* Minimum line: "0 0 0 0" ---> 8 chars */

        long modtime;
        long firstMsec;
        long atime;
        long secondMsec;

        if (line.length() < 8) {
            throw new RemoteScpException(
                    "Malformed T line sent by remote SCP binary, line too short.");
        }

        int firstMsecBegin = line.indexOf(" ") + 1;
        if (firstMsecBegin == 0 || firstMsecBegin >= line.length()) {
            throw new RemoteScpException(
                    "Malformed T line sent by remote SCP binary, line not enough data.");
        }

        int atimeBegin = line.indexOf(" ", firstMsecBegin + 1) + 1;
        if (atimeBegin == 0 || atimeBegin >= line.length()) {
            throw new RemoteScpException(
                    "Malformed T line sent by remote SCP binary, line not enough data.");
        }

        int secondMsecBegin = line.indexOf(" ", atimeBegin + 1) + 1;
        if (secondMsecBegin == 0 || secondMsecBegin >= line.length()) {
            throw new RemoteScpException(
                    "Malformed T line sent by remote SCP binary, line not enough data.");
        }

        try {
            modtime = Long.parseLong(line.substring(0, firstMsecBegin - 1));
            firstMsec = Long.parseLong(line.substring(firstMsecBegin, atimeBegin - 1));
            atime = Long.parseLong(line.substring(atimeBegin, secondMsecBegin - 1));
            secondMsec = Long.parseLong(line.substring(secondMsecBegin));
        } catch (NumberFormatException e) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, cannot parse file length.");
        }

        if (modtime < 0 || firstMsec < 0 || atime < 0 || secondMsec < 0) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, illegal file length.");
        }

        fileInfo.setLastModified(modtime);
    }

    private void sendBytes(Channel channel, byte[] data, String fileName, String mode)
            throws IOException, RemoteScpException {
        OutputStream os = channel.getOutputStream();
        InputStream is = new BufferedInputStream(channel.getInputStream(), 512);

        try {
            if (channel.isConnected()) {
                channel.start();
            } else {
                channel.connect();
            }
        } catch (JSchException e1) {
            throw (IOException) new IOException("Channel connection problems").initCause(e1);
        }

        readResponse(is);

        String cline = "C" + mode + " " + data.length + " " + fileName + "\n";

        os.write(cline.getBytes());
        os.flush();

        readResponse(is);

        os.write(data, 0, data.length);
        os.write(0);
        os.flush();

        readResponse(is);

        os.write("E\n".getBytes());
        os.flush();
    }

    private void sendFile(Channel channel, String localFile, String remoteName, String mode)
            throws IOException, RemoteScpException {
        byte[] buffer = new byte[BUFFER_SIZE];

        OutputStream os = new BufferedOutputStream(channel.getOutputStream(), 40000);
        InputStream is = new BufferedInputStream(channel.getInputStream(), 512);

        try {
            if (channel.isConnected()) {
                channel.start();
            } else {
                channel.connect();
            }
        } catch (JSchException e1) {
            throw (IOException) new IOException("Channel connection problems").initCause(e1);
        }

        readResponse(is);

        File f = new File(localFile);
        long remain = f.length();

        String cline = "C" + mode + " " + remain + " " + remoteName + "\n";

        os.write(cline.getBytes());
        os.flush();

        readResponse(is);

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(f);

            while (remain > 0) {
                int trans;
                if (remain > buffer.length) {
                    trans = buffer.length;
                } else {
                    trans = (int) remain;
                }
                if (fis.read(buffer, 0, trans) != trans) {
                    throw new IOException("Cannot read enough from local file " + localFile);
                }

                os.write(buffer, 0, trans);

                remain -= trans;
            }

            fis.close();
        } catch (IOException e) {
            if (fis != null) {
                fis.close();
            }
            throw (e);
        }

        os.write(0);
        os.flush();

        readResponse(is);

        os.write("E\n".getBytes());
        os.flush();
    }

    /**
     * Receive a file via scp and store it in a stream
     * 
     * @param channel
     *            ssh channel to use
     * @param file
     *            to receive from remote
     * @param target
     *            to store file into (if null, get only file info)
     * @return file information of the file we received
     * @throws IOException
     *             in case of network or protocol trouble
     * @throws RemoteScpException
     *             in case of problems on the target system (connection is fine)
     */
    private FileInfo receiveStream(Channel channel, String file, OutputStream targetStream)
            throws IOException, RemoteScpException {
        byte[] buffer = new byte[BUFFER_SIZE];

        OutputStream os = channel.getOutputStream();
        InputStream is = channel.getInputStream();
        try {
            if (channel.isConnected()) {
                channel.start();
            } else {
                channel.connect();
            }
        } catch (JSchException e1) {
            throw (IOException) new IOException("Channel connection problems").initCause(e1);
        }
        os.write(0x0);
        os.flush();

        FileInfo fileInfo = new FileInfo();

        while (true) {
            int c = is.read();
            if (c < 0) {
                throw new RemoteScpException("Remote scp terminated unexpectedly.");
            }

            String line = receiveLine(is);

            if (c == 'T') {
                parseTLine(line, fileInfo);
                os.write(0x0);
                os.flush();
                continue;
            }
            if ((c == 1) || (c == 2)) {
                throw new RemoteScpException("Remote SCP error: " + line);
            }

            if (c == 'C') {
                parseCLine(line, fileInfo);
                break;
            }
            throw new RemoteScpException("Remote SCP error: " + ((char) c) + line);
        }
        if (targetStream != null) {

            os.write(0x0);
            os.flush();

            try {
                long remain = fileInfo.getLength();

                while (remain > 0) {
                    int trans;
                    if (remain > buffer.length) {
                        trans = buffer.length;
                    } else {
                        trans = (int) remain;
                    }

                    int thisTimeReceived = is.read(buffer, 0, trans);

                    if (thisTimeReceived < 0) {
                        throw new IOException("Remote scp terminated connection unexpectedly");
                    }

                    targetStream.write(buffer, 0, thisTimeReceived);

                    remain -= thisTimeReceived;
                }

                targetStream.close();
            } catch (IOException e) {
                if (targetStream != null) {
                    targetStream.close();
                }
                throw (e);
            }

            readResponse(is);

            os.write(0x0);
            os.flush();
        }
        return fileInfo;
    }

    /**
     * Copy a local file to a remote directory, uses mode 0600 when creating the file on the remote
     * side.
     * 
     * @param localFile
     *            Path and name of local file.
     * @param remoteTargetDirectory
     *            Remote target directory where the file has to end up (optional)
     * @param remoteName
     *            target filename to use
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public void put(String localFile, String remoteTargetDirectory, String remoteName)
            throws IOException, RemoteScpException {
        put(localFile, remoteTargetDirectory, remoteName, "0600");
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into it. Uses mode 0600
     * for creating the remote file.
     * 
     * @param data
     *            the data to be copied into the remote file.
     * @param remoteFileName
     *            The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory
     *            Remote target directory where the file has to end up (optional)
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */

    public void put(byte[] data, String remoteFileName, String remoteTargetDirectory)
            throws IOException, RemoteScpException {
        put(data, remoteFileName, remoteTargetDirectory, "0600");
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into it. The method use
     * the specified mode when creating the file on the remote side.
     * 
     * @param data
     *            the data to be copied into the remote file.
     * @param remoteFileName
     *            The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory
     *            Remote target directory where the file has to end up (optional)
     * @param mode
     *            a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public void put(byte[] data, String remoteFileName, String remoteTargetDirectory, String mode)
            throws IOException, RemoteScpException {
        ChannelExec channel = null;

        if ((remoteFileName == null) || (mode == null)) {
            throw new IllegalArgumentException("Null argument.");
        }

        if (mode.length() != 4) {
            throw new IllegalArgumentException("Invalid mode.");
        }

        for (int i = 0; i < mode.length(); i++) {
            if (!Character.isDigit(mode.charAt(i))) {
                throw new IllegalArgumentException("Invalid mode.");
            }
        }

        String cmd = "scp -t ";
        if (remoteTargetDirectory != null && remoteTargetDirectory.length() > 0) {
            cmd = cmd + "-d " + remoteTargetDirectory;
        }

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            sendBytes(channel, data, remoteFileName, mode);
            // channel.disconnect();
        } catch (JSchException e) {
            if (channel != null) {
                channel.disconnect();
            }
            throw (IOException) new IOException("Error during SCP transfer." + e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * @return
     * @throws JSchException
     */
    private ChannelExec getExecChannel() throws JSchException {
        ChannelExec channel;
        channel = (ChannelExec) session.openChannel("exec");
        return channel;
    }

    /**
     * Copy a local file to a remote site, uses the specified mode when creating the file on the
     * remote side.
     * 
     * @param localFile
     *            Path and name of local file.
     * @param remoteTargetDir
     *            Remote target directory where the file has to end up (optional)
     * @param remoteTargetName
     *            file name to use on the target system
     * @param mode
     *            a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public void put(String localFile, String remoteTargetDir, String remoteTargetName, String mode)
            throws IOException, RemoteScpException {
        ChannelExec channel = null;

        if ((localFile == null) || (remoteTargetName == null) || (mode == null)) {
            throw new IllegalArgumentException("Null argument.");
        }

        if (mode.length() != 4) {
            throw new IllegalArgumentException("Invalid mode.");
        }

        for (int i = 0; i < mode.length(); i++) {
            if (!Character.isDigit(mode.charAt(i))) {
                throw new IllegalArgumentException("Invalid mode.");
            }
        }

        String cmd = "scp -t ";
        if (remoteTargetDir != null && remoteTargetDir.length() > 0) {
            cmd = cmd + "-d " + remoteTargetDir;
        }

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            sendFile(channel, localFile, remoteTargetName, mode);
            channel.disconnect();
        } catch (JSchException e) {
            if (channel != null) {
                channel.disconnect();
            }
            throw (IOException) new IOException("Error during SCP transfer." + e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * Download a file from the remote server to a local file.
     * 
     * @param remoteFile
     *            Path and name of the remote file.
     * @param localTarget
     *            Local file where to store the data.
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public void get(String remoteFile, String localTarget) throws IOException, RemoteScpException {
        File f = new File(localTarget);
        FileOutputStream fop = new FileOutputStream(f);
        get(remoteFile, fop);
    }

    /**
     * Download a file from the remote server into an OutputStream
     * 
     * @param remoteFile
     *            Path and name of the remote file.
     * @param localTarget
     *            OutputStream to store the data.
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public void get(String remoteFile, OutputStream localTarget) throws IOException,
            RemoteScpException {
        ChannelExec channel = null;

        if ((remoteFile == null) || (localTarget == null)) {
            throw new IllegalArgumentException("Null argument.");
        }

        String cmd = "scp -p -f " + remoteFile;

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            receiveStream(channel, remoteFile, localTarget);
            channel.disconnect();
        } catch (JSchException e) {
            if (channel != null) {
                channel.disconnect();
            }
            throw (IOException) new IOException("Error during SCP transfer." + e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * Initiates an SCP sequence but stops after getting fileinformation header
     * 
     * @param remoteFile
     *            to get information for
     * @return the file information got
     * @throws IOException
     *             in case of network problems
     * @throws RemoteScpException
     *             in case of problems on the target system (connection ok)
     */
    public FileInfo getFileinfo(String remoteFile) throws IOException, RemoteScpException {
        ChannelExec channel = null;
        FileInfo fileInfo = null;

        if (remoteFile == null) {
            throw new IllegalArgumentException("Null argument.");
        }

        String cmd = "scp -p -f \"" + remoteFile + "\"";

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            fileInfo = receiveStream(channel, remoteFile, null);
            channel.disconnect();
        } catch (JSchException e) {
            throw (IOException) new IOException("Error during SCP transfer." + e.getMessage())
                    .initCause(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return fileInfo;
    }
}
