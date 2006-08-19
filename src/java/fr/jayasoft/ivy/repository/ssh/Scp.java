package fr.jayasoft.ivy.repository.ssh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;

/**
 * This class is using the scp client to transfer data and information for the
 * repository. It is based on the SCPClient from the ganymed ssh library from
 * Christian Plattner. To minimize the dependency to the ssh library and because
 * I needed some additional functionality, I decided to copy'n'paste the single
 * class rather than to inherit or delegate it somehow. Nevertheless credit
 * should go to the original author.
 * 
 * @author Andreas Sahlbach
 * @author Christian Plattner, plattner@inf.ethz.ch
 */

public class Scp {
    Session session;

    public class FileInfo {
        private String filename;
        private long length;
        private long lastModified;
        /**
         * @param filename The filename to set.
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
         * @param length The length to set.
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
         * @param lastModified The lastModified to set.
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
        if (session == null)
            throw new IllegalArgumentException("Cannot accept null argument!");
        this.session = session;
    }

    private void readResponse(InputStream is) throws IOException, RemoteScpException {
        int c = is.read();

        if (c == 0)
            return;

        if (c == -1)
            throw new RemoteScpException("Remote scp terminated unexpectedly.");

        if ((c != 1) && (c != 2))
            throw new RemoteScpException("Remote scp sent illegal error code.");

        if (c == 2)
            throw new RemoteScpException("Remote scp terminated with error.");

        String err = receiveLine(is);
        throw new RemoteScpException("Remote scp terminated with error (" + err + ").");
    }

    private String receiveLine(InputStream is) throws IOException,RemoteScpException {
        StringBuffer sb = new StringBuffer(30);

        while (true) {
            /*
             * This is a random limit - if your path names are longer, then
             * adjust it
             */

            if (sb.length() > 8192)
                throw new RemoteScpException("Remote scp sent a too long line");

            int c = is.read();

            if (c < 0)
                throw new RemoteScpException("Remote scp terminated unexpectedly.");

            if (c == '\n')
                break;

            sb.append((char) c);

        }
        return sb.toString();
    }

    private void parseCLine(String line, FileInfo fileInfo) throws RemoteScpException {
        /* Minimum line: "xxxx y z" ---> 8 chars */

        long len;

        if (line.length() < 8)
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, line too short.");

        if ((line.charAt(4) != ' ') || (line.charAt(5) == ' '))
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");

        int length_name_sep = line.indexOf(' ', 5);

        if (length_name_sep == -1)
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");

        String length_substring = line.substring(5, length_name_sep);
        String name_substring = line.substring(length_name_sep + 1);

        if ((length_substring.length() <= 0) || (name_substring.length() <= 0))
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");

        if ((6 + length_substring.length() + name_substring.length()) != line
                .length())
            throw new RemoteScpException("Malformed C line sent by remote SCP binary.");

        try {
            len = Long.parseLong(length_substring);
        } catch (NumberFormatException e) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, cannot parse file length.");
        }

        if (len < 0)
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, illegal file length.");

        fileInfo.setLength(len);
        fileInfo.setFilename(name_substring);
    }
    
    private void parseTLine(String line, FileInfo fileInfo) throws RemoteScpException {
        /* Minimum line: "0 0 0 0" ---> 8 chars */

        long modtime;
        long first_msec;
        long atime;
        long second_msec;

        if (line.length() < 8)
            throw new RemoteScpException(
                    "Malformed T line sent by remote SCP binary, line too short.");

        int first_msec_begin = line.indexOf(" ")+1;
        if(first_msec_begin == 0 || first_msec_begin >= line.length())
            throw new RemoteScpException(
            "Malformed T line sent by remote SCP binary, line not enough data.");
       
        int atime_begin = line.indexOf(" ",first_msec_begin+1)+1;
        if(atime_begin == 0 || atime_begin >= line.length())
            throw new RemoteScpException(
            "Malformed T line sent by remote SCP binary, line not enough data.");
        
        int second_msec_begin = line.indexOf(" ",atime_begin+1)+1;
        if(second_msec_begin == 0 || second_msec_begin >= line.length())
            throw new RemoteScpException(
            "Malformed T line sent by remote SCP binary, line not enough data.");
       
        try {
            modtime = Long.parseLong(line.substring(0,first_msec_begin-1));
            first_msec =  Long.parseLong(line.substring(first_msec_begin,atime_begin-1));
            atime = Long.parseLong(line.substring(atime_begin,second_msec_begin-1));
            second_msec =  Long.parseLong(line.substring(second_msec_begin));
        } catch (NumberFormatException e) {
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, cannot parse file length.");
        }

        if (modtime < 0 || first_msec < 0 || atime < 0 || second_msec < 0)
            throw new RemoteScpException(
                    "Malformed C line sent by remote SCP binary, illegal file length.");

        fileInfo.setLastModified(modtime);
    }
    
    private void sendBytes(Channel channel, byte[] data, String fileName,
            String mode) throws IOException,RemoteScpException {
        OutputStream os = channel.getOutputStream();
        InputStream is = new BufferedInputStream(channel.getInputStream(), 512);

        try {
            if(channel.isConnected())
                channel.start();
            else
                channel.connect();
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
        byte[] buffer = new byte[8192];

        OutputStream os = new BufferedOutputStream(channel.getOutputStream(), 40000);
        InputStream is = new BufferedInputStream(channel.getInputStream(), 512);

        try {
            if(channel.isConnected())
                channel.start();
            else
                channel.connect();
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
                if (remain > buffer.length)
                    trans = buffer.length;
                else
                    trans = (int) remain;

                if (fis.read(buffer, 0, trans) != trans)
                    throw new IOException(
                            "Cannot read enough from local file "
                                    + localFile);

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
     * @param channel ssh channel to use
     * @param file to receive from remote
     * @param target to store file into (if null, get only file info)
     * @return file information of the file we received
     * @throws IOException in case of network or protocol trouble
     * @throws RemoteScpException in case of problems on the target system (connection is fine)
     */
    private FileInfo receiveStream(Channel channel, String file, OutputStream targetStream) 
      throws IOException,RemoteScpException {
        byte[] buffer = new byte[8192];

        OutputStream os = channel.getOutputStream();
        InputStream is = channel.getInputStream();
        try {
            if(channel.isConnected())
                channel.start();
            else
                channel.connect();
        } catch (JSchException e1) {
            throw (IOException) new IOException("Channel connection problems").initCause(e1);
        }
        os.write(0x0);
        os.flush();

        FileInfo fileInfo = new FileInfo();

        while (true) {
            int c = is.read();
            if (c < 0)
                throw new RemoteScpException("Remote scp terminated unexpectedly.");

            String line = receiveLine(is);

            if (c == 'T') {
                parseTLine(line,fileInfo);
                os.write(0x0);
                os.flush();
                continue;
            }
            if ((c == 1) || (c == 2))
                throw new RemoteScpException("Remote SCP error: " + line);

            if (c == 'C') {
                parseCLine(line,fileInfo);
                break;
            }
            throw new RemoteScpException("Remote SCP error: " + ((char) c) + line);
        }
        if(targetStream != null) {
            
            os.write(0x0);
            os.flush();
    
            try {
                long remain = fileInfo.getLength();
    
                while (remain > 0) {
                    int trans;
                    if (remain > buffer.length)
                        trans = buffer.length;
                    else
                        trans = (int) remain;
    
                    int this_time_received = is.read(buffer, 0, trans);
    
                    if (this_time_received < 0) {
                        throw new IOException(
                                "Remote scp terminated connection unexpectedly");
                    }
    
                    targetStream.write(buffer, 0, this_time_received);
    
                    remain -= this_time_received;
                }
    
                targetStream.close();
            } catch (IOException e) {
                if (targetStream != null)
                    targetStream.close();
    
                throw (e);
            }

            readResponse(is);
    
            os.write(0x0);
            os.flush();
        }
        return fileInfo;
    }

    /**
     * Copy a local file to a remote directory, uses mode 0600 when creating the
     * file on the remote side.
     * 
     * @param localFile Path and name of local file.
     * @param remoteTargetDirectory Remote target directory where the file has to end up (optional)
     * @param remoteName target filename to use
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public void put(String localFile, String remoteTargetDirectory, String remoteName)
    throws IOException, RemoteScpException {
        put(localFile, remoteTargetDirectory, remoteName, "0600");
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into
     * it. Uses mode 0600 for creating the remote file.
     * 
     * @param data the data to be copied into the remote file.
     * @param remoteFileName The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory Remote target directory where the file has to end up (optional)
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */

    public void put(byte[] data, String remoteFileName, String remoteTargetDirectory) 
    throws IOException,RemoteScpException {
        put(data, remoteFileName, remoteTargetDirectory, "0600");
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into
     * it. The method use the specified mode when creating the file on the
     * remote side.
     * 
     * @param data the data to be copied into the remote file.
     * @param remoteFileName The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory Remote target directory where the file has to end up (optional)
     * @param mode a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public void put(byte[] data, String remoteFileName, String remoteTargetDirectory, String mode) 
    throws IOException,RemoteScpException {
        ChannelExec channel = null;

        if ((remoteFileName == null) || (mode == null))
            throw new IllegalArgumentException("Null argument.");

        if (mode.length() != 4)
            throw new IllegalArgumentException("Invalid mode.");

        for (int i = 0; i < mode.length(); i++)
            if (Character.isDigit(mode.charAt(i)) == false)
                throw new IllegalArgumentException("Invalid mode.");

        String cmd = "scp -t ";
        if(remoteTargetDirectory != null && remoteTargetDirectory.length() > 0) {
            cmd = cmd + "-d " + remoteTargetDirectory;
        }

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            sendBytes(channel, data, remoteFileName, mode);
            //channel.disconnect();
        } catch (JSchException e) {
            if (channel != null)
                channel.disconnect();
            throw (IOException) new IOException("Error during SCP transfer."+e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * @return
     * @throws JSchException
     */
    private ChannelExec getExecChannel() throws JSchException {
        ChannelExec channel;
        channel = (ChannelExec)session.openChannel("exec");
        return channel;
    }

    /**
     * Copy a local file to a remote site, uses the specified mode when
     * creating the file on the remote side.
     * 
     * @param localFile Path and name of local file.
     * @param remoteTargetDir Remote target directory where the file has to end up (optional)
     * @param remoteTargetName file name to use on the target system 
     * @param mode a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public void put(String localFile, String remoteTargetDir, String remoteTargetName, String mode) 
    throws IOException,RemoteScpException {
        ChannelExec channel = null;

        if ((localFile == null) || (remoteTargetName == null) || (mode == null))
            throw new IllegalArgumentException("Null argument.");

        if (mode.length() != 4)
            throw new IllegalArgumentException("Invalid mode.");

        for (int i = 0; i < mode.length(); i++)
            if (Character.isDigit(mode.charAt(i)) == false)
                throw new IllegalArgumentException("Invalid mode.");
           
        String cmd = "scp -t ";
        if(remoteTargetDir != null && remoteTargetDir.length() > 0) {
            cmd = cmd + "-d " + remoteTargetDir;
        }

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            sendFile(channel, localFile, remoteTargetName, mode);
            channel.disconnect();
        } catch (JSchException e) {
            if (channel != null)
                channel.disconnect();
            throw (IOException) new IOException("Error during SCP transfer."+e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * Download a file from the remote server to a local file.
     * @param remoteFile Path and name of the remote file.
     * @param localTarget Local file where to store the data.
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public void get(String remoteFile, String localTarget) throws IOException,RemoteScpException {
        File f = new File(localTarget);
        FileOutputStream fop = new FileOutputStream(f);
        get(remoteFile,fop);
    }
    
    /**
     * Download a file from the remote server into an OutputStream
     * @param remoteFile Path and name of the remote file.
     * @param localTarget OutputStream to store the data.
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public void get(String remoteFile, OutputStream localTarget) throws IOException,RemoteScpException {
        ChannelExec channel = null;

        if ((remoteFile == null) || (localTarget == null))
            throw new IllegalArgumentException("Null argument.");

        String cmd = "scp -p -f "+ remoteFile;

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            receiveStream(channel, remoteFile, localTarget);
            channel.disconnect();
        } catch (JSchException e) {
            if (channel != null)
                channel.disconnect();
            throw (IOException) new IOException("Error during SCP transfer."+e.getMessage())
                    .initCause(e);
        }
    }
    
    /**
     * Initiates an SCP sequence but stops after getting fileinformation header
     * @param remoteFile to get information for
     * @return the file information got
     * @throws IOException in case of network problems
     * @throws RemoteScpException in case of problems on the target system (connection ok)
     */
    public FileInfo getFileinfo(String remoteFile) throws IOException,RemoteScpException {
        ChannelExec channel = null;
        FileInfo fileInfo = null;
        
        if (remoteFile == null)
            throw new IllegalArgumentException("Null argument.");

        String cmd = "scp -p -f \""+remoteFile+"\"";

        try {
            channel = getExecChannel();
            channel.setCommand(cmd);
            fileInfo = receiveStream(channel, remoteFile, null);
            channel.disconnect();
        } catch (JSchException e) {
            throw (IOException) new IOException("Error during SCP transfer."+e.getMessage())
            .initCause(e);
        } finally {
            if (channel != null)
              channel.disconnect();
        }
        return fileInfo;
    }
}
