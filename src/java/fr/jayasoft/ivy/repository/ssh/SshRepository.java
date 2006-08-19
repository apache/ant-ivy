package fr.jayasoft.ivy.repository.ssh;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;

/**
 * Ivy Repository based on SSH
 */
public class SshRepository extends AbstractSshBasedRepository {

	private char fileSeparator = '/';
    private String listCommand = "ls -1";
    private String existCommand = "ls";
    private String createDirCommand = "mkdir";
    private final static String ARGUMENT_PLACEHOLDER = "%arg";
	private final static int POLL_SLEEP_TIME = 500;
    /**
     * create a new resource with lazy initializing
     */
    public Resource getResource(String source) {
        Message.debug("SShRepository:getResource called: "+source);
        return new SshResource(this,source);
    }
    
    /**
     * fetch the needed file information for a given file (size, last modification time)
     * and report it back in a SshResource
     * @param uri ssh uri for the file to get info for
     * @return SshResource filled with the needed informations
	 * @see fr.jayasoft.ivy.repository.Repository#getResource(java.lang.String)
	 */
	public SshResource resolveResource(String source) {
		Message.debug("SShRepository:resolveResource called: "+source);
		SshResource result = null;
        Session session = null;
		try {
            session = getSession(source);
            Scp myCopy = new Scp(session);
            Scp.FileInfo fileInfo = myCopy.getFileinfo(new URI(source).getPath());
            result = new SshResource(this,
                                     source,
                                     true,
                                     fileInfo.getLength(),
                                     fileInfo.getLastModified());
        } catch (IOException e) {
            if(session != null)
                releaseSession(session,source);
            result = new SshResource();
        } catch (URISyntaxException e) {
             if(session != null)
                 releaseSession(session,source);
             result = new SshResource();
        } catch (RemoteScpException e) {
            result = new SshResource();
        }
        Message.debug("SShRepository:resolveResource end.");
		return result;
	}

    /**
     * Reads out the output of a ssh session exec
     * @param channel Channel to read from
     * @param strStdout StringBuffer that receives Session Stdout output
     * @param strStderr StringBuffer that receives Session Stderr output
     * @throws IOException in case of trouble with the network
     */
    private void readSessionOutput(ChannelExec channel, StringBuffer strStdout, StringBuffer strStderr) throws IOException {
        InputStream stdout = channel.getInputStream();
        InputStream stderr = channel.getErrStream();
        
        try {
            channel.connect();
        } catch (JSchException e1) {
            throw (IOException) new IOException("Channel connection problems").initCause(e1);
        }
        
        byte[] buffer = new byte[8192];
        while(true){
            int avail = 0;
            while ((avail = stdout.available()) > 0) {
                int len = stdout.read(buffer,0,(avail > 8191 ? 8192 : avail));
                strStdout.append(new String(buffer,0,len));
            }
            while ((avail = stderr.available()) > 0) {
                int len = stderr.read(buffer,0,(avail > 8191 ? 8192 : avail));
                strStderr.append(new String(buffer, 0, len));
            }
            if(channel.isClosed()){
                break;
            }
            try{Thread.sleep(POLL_SLEEP_TIME);}catch(Exception ee){}
        }
        int avail = 0;
        while ((avail = stdout.available()) > 0) {
            int len = stdout.read(buffer,0,(avail > 8191 ? 8192 : avail));
            strStdout.append(new String(buffer,0,len));
        }
        while ((avail = stderr.available()) > 0) {
            int len = stderr.read(buffer,0,(avail > 8191 ? 8192 : avail));
            strStderr.append(new String(buffer, 0, len));
        }
    }

	/* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Repository#list(java.lang.String)
	 */
	public List list(String parent) throws IOException {
		Message.debug("SShRepository:list called: "+parent);
        ArrayList result = new ArrayList();
        Session session = null;
        ChannelExec channel = null;
        session = getSession(parent);
        channel = getExecChannel(session);
        URI parentUri = null;
        try {
            parentUri = new URI(parent);
        } catch (URISyntaxException e1) {
            // failed earlier
        }
        String fullCmd = replaceArgument(listCommand,parentUri.getPath());
        channel.setCommand(fullCmd);
        StringBuffer stdOut = new StringBuffer();
        StringBuffer stdErr = new StringBuffer();
        readSessionOutput(channel,stdOut,stdErr);
        if(channel.getExitStatus() != 0) {
            Message.error("Ssh ListCommand exited with status != 0");
            Message.error(stdErr.toString());
            return null;
        } else {
            BufferedReader br = new BufferedReader(new StringReader(stdOut.toString()));
            String line = null;
            while((line = br.readLine()) != null) {
                result.add(line);
            }
        }
		return result;
	}

    /**
     * @param session
     * @return
     * @throws JSchException
     */
    private ChannelExec getExecChannel(Session session) throws IOException {
        ChannelExec channel;
        try {
            channel = (ChannelExec)session.openChannel("exec");
        } catch (JSchException e) {
            throw new IOException();
        }
        return channel;
    }

    /**
     * Replace the argument placeholder with argument or append the argument if no 
     * placeholder is present
     * @param command with argument placeholder or not
     * @param argument 
     * @return replaced full command
     */
    private String replaceArgument(String command, String argument) {
        String fullCmd;
        if(command.indexOf(ARGUMENT_PLACEHOLDER) == -1) {
            fullCmd = command + " " + argument;
        } else {
            fullCmd = command.replaceAll(ARGUMENT_PLACEHOLDER,argument);
        }
        return fullCmd;
    }

	/* (non-Javadoc)
	 * @see fr.jayasoft.ivy.repository.Repository#put(java.io.File, java.lang.String, boolean)
	 */
	public void put(File source, String destination, boolean overwrite) throws IOException {
		Message.debug("SShRepository:put called: "+destination);
        Session session = getSession(destination);
        try {
            URI destinationUri = null;
            try {
                destinationUri = new URI(destination);
            } catch (URISyntaxException e) {
                // failed earlier in getSession()
            }
            String filePath = destinationUri.getPath();
            int lastSep = filePath.lastIndexOf(fileSeparator);
            String path;
            String name;
            if(lastSep == -1) {
                name = filePath;
                path = null;
            } else {
                name = filePath.substring(lastSep+1);
                path = filePath.substring(0,lastSep);
            }
            if (!overwrite) {
                if(checkExistence(filePath,session)) {
                    throw new IOException("destination file exists and overwrite == true");
                }
            }
            if(path != null) {
                makePath(path,session);
            }
            Scp myCopy = new Scp(session);
            myCopy.put(source.getCanonicalPath(),path,name);  
        } catch (IOException e) {
            if(session != null)
                releaseSession(session,destination);
            throw e;
        } catch (RemoteScpException e) {
            throw new IOException(e.getMessage());
        }
	}
    
    /**
     * Tries to create a directory path on the target system
     * @param path to create
     * @param connnection to use
     */
    private void makePath(String path, Session session) throws IOException {
        ChannelExec channel = null;
        String trimmed = path;
        try {
            while(trimmed.length() > 0 && trimmed.charAt(trimmed.length()-1) == fileSeparator)
                trimmed = trimmed.substring(0,trimmed.length()-1);
            if(trimmed.length() == 0 || checkExistence(trimmed,session)) {
                return;
            }
            int nextSlash = trimmed.lastIndexOf(fileSeparator);
            if(nextSlash > 0) {
                String parent = trimmed.substring(0,nextSlash);
                makePath(parent,session);
            }
            channel = getExecChannel(session);
            String mkdir = replaceArgument( createDirCommand, trimmed);
            Message.debug("SShRepository: trying to create path: " + mkdir);
            channel.setCommand(mkdir);
            StringBuffer stdOut = new StringBuffer();
            StringBuffer stdErr = new StringBuffer();
            readSessionOutput(channel,stdOut,stdErr);
        } finally {
            if(channel != null)
                channel.disconnect();
        }
    }

    /**
     * check for existence of file or dir on target system
     * @param filePath to the object to check
     * @param session to use
     * @return true: object exists, false otherwise
     */
    private boolean checkExistence(String filePath, Session session) throws IOException {
        Message.debug("SShRepository: checkExistence called: " + filePath);
        ChannelExec channel = null;
        channel = getExecChannel(session);
        String fullCmd = replaceArgument( existCommand, filePath);
        channel.setCommand(fullCmd);
        StringBuffer stdOut = new StringBuffer();
        StringBuffer stdErr = new StringBuffer();
        readSessionOutput(channel,stdOut,stdErr);
        return channel.getExitStatus() == 0;
    }

    /* (non-Javadoc)
     * @see fr.jayasoft.ivy.repository.Repository#get(java.lang.String, java.io.File)
     */
    public void get(String source, File destination) throws IOException {
        Message.debug("SShRepository:get called: "+source+" to "+destination.getCanonicalPath());
        if (destination.getParentFile() != null) {
            destination.getParentFile().mkdirs();
        }
        Session session = getSession(source);
        try {
            URI sourceUri = null;
            try {
                sourceUri = new URI(source);
            } catch (URISyntaxException e) {
                // fails earlier
            }
            if(sourceUri == null) {
                Message.error("could not parse URI "+source);
                return;
            }
            Scp myCopy = new Scp(session);
            myCopy.get(sourceUri.getPath(),destination.getCanonicalPath());   
        } catch (IOException e) {
            if(session != null)
                releaseSession(session,source);
            throw e;
        } catch (RemoteScpException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * sets the list command to use for a directory listing
     * listing must be only the filename and each filename on a separate line
     * @param cmd to use. default is "ls -1"
     */
    public void setListCommand(String cmd) {
        this.listCommand = cmd.trim();
    }
    
    /**
     * @return the list command to use
     */
    public String getListCommand() {
        return listCommand;
    }
    
    /**
     * @return the createDirCommand
     */
    public String getCreateDirCommand() {
        return createDirCommand;
    }

    /**
     * @param createDirCommand the createDirCommand to set
     */
    public void setCreateDirCommand(String createDirCommand) {
        this.createDirCommand = createDirCommand;
    }

    /**
     * @return the existCommand
     */
    public String getExistCommand() {
        return existCommand;
    }

    /**
     * @param existCommand the existCommand to set
     */
    public void setExistCommand(String existCommand) {
        this.existCommand = existCommand;
    }

    /**
     * The file separator is the separator to use on the target system 
     * On a unix system it is '/', but I don't know, how this is solved 
     * on different ssh implementations. Using the default might be fine
     * @param fileSeparator The fileSeparator to use. default '/'
     */
    public void setFileSeparator(char fileSeparator) {
        this.fileSeparator = fileSeparator;
    }

    /**
     * return ssh as scheme
     * use the Resolver type name here? 
     * would be nice if it would be static, so we could use SshResolver.getTypeName()
     */
    protected String getRepositoryScheme() {
        return "ssh";
    }
    
    /**
     * Not really streaming...need to implement a proper streaming approach?
     * @param resource to stream
     * @return InputStream of the resource data
     */
    public InputStream openStream(SshResource resource) throws IOException {
        Session session = getSession(resource.getName());
        Scp scp = new Scp(session);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            scp.get(resource.getName(),os);
        } catch (IOException e) {
            if(session != null)
                releaseSession(session,resource.getName());
            throw e;
        } catch (RemoteScpException e) {
            throw new IOException(e.getMessage());
        }
        return new ByteArrayInputStream(os.toByteArray());
    }
}
