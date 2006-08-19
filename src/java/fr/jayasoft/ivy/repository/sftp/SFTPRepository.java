/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.sftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import fr.jayasoft.ivy.repository.BasicResource;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.TransferEvent;
import fr.jayasoft.ivy.repository.ssh.AbstractSshBasedRepository;
import fr.jayasoft.ivy.repository.ssh.SshCache;
import fr.jayasoft.ivy.util.Message;

/**
 * SFTP Repository, allow to use a repository accessed by sftp protocol.
 * 
 * It supports all operations: get, put and list.
 * 
 * It relies on jsch for sftp handling, and thus is compatible with sftp version 0, 1, 2 and 3
 * 
 * @author Xavier Hanin
 *
 */
public class SFTPRepository extends AbstractSshBasedRepository {
    private final class MyProgressMonitor implements SftpProgressMonitor {
		private long _totalLength;

		public void init(int op, String src, String dest, long max) {
		    _totalLength = max;
		    fireTransferStarted(max);
		}

		public void end() {
			fireTransferCompleted(_totalLength);
		}

		public boolean count(long count) {
			fireTransferProgress(count);
			return true;
		}
	}

    
    public SFTPRepository() {
    }

    
    public Resource getResource(String source) {
        return new SFTPResource(this, source);
    }


    /**
     * This method is similar to getResource, except that the returned resource is fully initialised 
     * (resolved in the sftp repository), and that the given string is a full remote path
     * @param path the full remote path in the repository of the resource
     * @return a fully initialised resource, able to answer to all its methods without needing
     * 	any further connection
     */
	public Resource resolveResource(String path) {
		try {
			ChannelSftp c = getSftpChannel(path);
			Collection r = c.ls(path);
			if (r != null) {
				for (Iterator iter = r.iterator(); iter.hasNext();) {
					Object obj=iter.next();
	                if(obj instanceof LsEntry){
	                	LsEntry entry = (LsEntry) obj;
	                	SftpATTRS attrs = entry.getAttrs();
	                	return new BasicResource(path, true, attrs.getSize(), attrs.getMTime() * 1000, false);
	                }
				}
			}
		} catch (Exception e) {
            Message.debug("reolving resource error: "+e.getMessage());
			//silent fail, return unexisting resource
		}
		return new BasicResource(path, false, 0, 0, false);
	}

	public InputStream openStream(SFTPResource resource) throws IOException {
        ChannelSftp c = getSftpChannel(resource.getName());
        try {
			return c.get(resource.getName());
		} catch (SftpException e) {
			e.printStackTrace();
			IOException ex = new IOException("impossible to open stream for "+resource+" on "+getHost()+(e.getMessage() != null?": " + e.getMessage():""));
			ex.initCause(e);
			throw ex;
		}
	}

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        ChannelSftp c = getSftpChannel(source);
        try {
			c.get(source, destination.getAbsolutePath(), new MyProgressMonitor());
		} catch (SftpException e) {
			e.printStackTrace();
			IOException ex = new IOException("impossible to get "+source+" on "+getHost()+(e.getMessage() != null?": " + e.getMessage():""));
			ex.initCause(e);
			throw ex;
		}
    }

	public void put(File source, String destination, boolean overwrite) throws IOException {
        fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
        ChannelSftp c = getSftpChannel(destination);
        try {
            if(!overwrite && checkExistence(destination, c))
                throw new IOException("destination file exists and overwrite == true");
        	if (destination.indexOf('/') != -1) {
        		mkdirs(destination.substring(0, destination.lastIndexOf('/')),c);
        	}
			c.put(source.getAbsolutePath(), destination, new MyProgressMonitor());
		} catch (SftpException e) {
			IOException ex = new IOException(e.getMessage());
			ex.initCause(e);
			throw ex;
		}
	}

	private void mkdirs(String directory, ChannelSftp c) throws IOException, SftpException {
    	try {
	    	SftpATTRS att = c.stat(directory);
	    	if (att != null) {
	    		if (att.isDir()) {
	    			return;
	    		}
	    	}
    	} catch (SftpException ex) {
        	if (directory.indexOf('/') != -1) {
        		mkdirs(directory.substring(0, directory.lastIndexOf('/')),c);
        	}
        	c.mkdir(directory);
    	}
	}


	public List list(String parent) throws IOException {
		try {
			ChannelSftp c = getSftpChannel(parent);
			Collection r = c.ls(parent);
			if (r != null) {
				if (!parent.endsWith("/")) {
					parent = parent+"/";
				}
				List result = new ArrayList();
				for (Iterator iter = r.iterator(); iter.hasNext();) {
					Object obj=iter.next();
	                if(obj instanceof LsEntry){
	                	LsEntry entry = (LsEntry) obj;
	                	if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
	                		continue;
	                	}
	                	result.add(parent+entry.getFilename());
	                }
				}
				return result;
			}
		} catch (Exception e) {
			//silent fail, return null listing
		}
		return null;
	}        

    /**
     * Checks the existence for a remote file
     * @param file to check
     * @param channel to use
     * @returns true if file exists, false otherwise
     * @throws IOException
     * @throws SftpException
     */
    private boolean checkExistence(String file, ChannelSftp channel) throws IOException, SftpException {
        try {
            return channel.stat(file) != null;
        } catch (SftpException ex) {
            return false;
        }
    }
    
	/**
	 * Establish the connection to the server if not yet connected, and listen to ivy events
	 * for closing connection when resolve is finished.
	 * 
	 * Not meant to be used in multi threaded environment.
	 * 
	 * @return the ChannelSftp with which a connection is established
	 * @throws IOException if any connection problem occurs
	 */
	private ChannelSftp getSftpChannel(String pathOrUri) throws IOException {
	    Session session = getSession(pathOrUri);
        String host = session.getHost();
        ChannelSftp channel = SshCache.getInstance().getChannelSftp(session);
        if(channel == null) {
            try {
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                Message.verbose(":: SFTP :: connected to "+host+"!");
                SshCache.getInstance().attachChannelSftp(session,channel);
            } catch (JSchException e) {
                IOException ex = new IOException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return channel;
	}

    protected String getRepositoryScheme() {
        // use the Resolver type name here? 
        // would be nice if it would be static, so we could use SFTPResolver.getTypeName()
        return "sftp";
    }
}
