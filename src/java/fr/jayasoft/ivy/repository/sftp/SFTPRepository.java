/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.sftp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.event.IvyEvent;
import fr.jayasoft.ivy.event.IvyListener;
import fr.jayasoft.ivy.event.resolve.EndResolveEvent;
import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.repository.BasicResource;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.TransferEvent;
import fr.jayasoft.ivy.util.Credentials;
import fr.jayasoft.ivy.util.CredentialsUtil;
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
public class SFTPRepository extends AbstractRepository {
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
			fireTransferProgress(null, count);
			return true;
		}
	}


	// mandatory attributes
    private String _host;
    
    //optional attribute
    private int _port = 22;
    
    // optional attributes, asked using a dialog if not provided
    private String _username;
    private String _passwd;
    private File _passfile;

	private transient ChannelSftp _channel;
	private transient Session _session;

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
			ChannelSftp c = getSftpChannel();
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
			//silent fail, return unexisting resource
		}
		return new BasicResource(path, false, 0, 0, false);
	}

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        ChannelSftp c = getSftpChannel();
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
        ChannelSftp c = getSftpChannel();
        try {
        	if (destination.indexOf('/') != -1) {
        		mkdirs(destination.substring(0, destination.lastIndexOf('/')));
        	}
			c.put(source.getAbsolutePath(), destination, new MyProgressMonitor());
		} catch (SftpException e) {
			IOException ex = new IOException(e.getMessage());
			ex.initCause(e);
			throw ex;
		}
	}

	private void mkdirs(String directory) throws IOException, SftpException {
    	ChannelSftp c = getSftpChannel();
    	try {
	    	SftpATTRS att = c.stat(directory);
	    	if (att != null) {
	    		if (att.isDir()) {
	    			return;
	    		}
	    	}
    	} catch (SftpException ex) {
        	if (directory.indexOf('/') != -1) {
        		mkdirs(directory.substring(0, directory.lastIndexOf('/')));
        	}
        	c.mkdir(directory);
    	}
	}


	public List list(String parent) throws IOException {
		try {
			ChannelSftp c = getSftpChannel();
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
	 * Establish the connection to the server if not yet connected, and listen to ivy events
	 * for closing connection when resolve is finished.
	 * 
	 * Not meant to be used in multi threaded environment.
	 * 
	 * @return the ChannelSftp with which a connection is established
	 * @throws IOException if any connection problem occurs
	 */
	private ChannelSftp getSftpChannel() throws IOException {
		if (_channel == null) {
			try {
				JSch jsch = new JSch();
				_session = jsch.getSession(getUsername(), getHost(), getPort());
				_session.setUserInfo(new UserInfo() {
					public void showMessage(String message) {
						Message.info(message);
					}

					public boolean promptYesNo(String message) {
						return true;
					}

					public boolean promptPassword(String message) {
						return true;
					}

					public boolean promptPassphrase(String message) {
						return true;
					}

					public String getPassword() {
						return SFTPRepository.this.getPasswd();
					}

					public String getPassphrase() {
						return null;
					}
				});
				IvyContext.getContext().getIvy().addIvyListener(new IvyListener() {
					public void progress(IvyEvent event) {
						if (_channel != null) {
							Message.verbose(":: SFTP :: disconnecting from "+getHost()+"...");
							_channel.disconnect();
							_channel = null;
							_session.disconnect();
							_session = null;
							event.getSource().removeIvyListener(this);
							Message.verbose(":: SFTP :: disconnected from "+getHost());
						}
					}
				}, EndResolveEvent.NAME);
				Message.verbose(":: SFTP :: connecting to "+getHost()+"...");
				_session.connect();

				_channel = (ChannelSftp) _session.openChannel("sftp");
				_channel.connect();
				Message.verbose(":: SFTP :: connected to "+getHost()+"!");
			} catch (JSchException e) {
				if (_passfile.exists()) {
					_passfile.delete();
				}
				IOException ex = new IOException(e.getMessage());
				ex.initCause(e);
				throw ex;
			}
    	}
    	return _channel;
	}



	public String getHost() {
		return _host;
	}


	public void setHost(String host) {
		_host = host;
	}


	public String getPasswd() {
		if (_passwd == null) {
			promptCredentials();
		}
		return _passwd;
	}


	public void setPasswd(String passwd) {
		if (passwd == null || passwd.trim().length() == 0) {
			_passwd = null;
		} else  {
			_passwd = passwd;
		}
	}


	public String getUsername() {
		if (_username == null) {
			promptCredentials();
		}
		return _username;
	}



	public void setUsername(String username) {
		if (username == null || username.trim().length() == 0) {
			_username = null;
		} else  {
			_username = username;
		}
	}

	private void promptCredentials() {
		Credentials c = CredentialsUtil.promptCredentials(new Credentials(null, getHost(), _username, _passwd), _passfile);
		if (c != null) {
			_username = c.getUserName();
			_passwd = c.getPasswd();
		}
	}



	public int getPort() {
		return _port;
	}


	public void setPort(int port) {
		_port = port;
	}


	public File getPassfile() {
		return _passfile;
	}


	public void setPassfile(File passfile) {
		_passfile = passfile;
	}
}
