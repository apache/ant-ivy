/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.sftp.SFTPRepository;

/**
 * This resolver is able to work with any sftp server.
 * 
 * It supports listing and publishing.
 * 
 * The server host should absolutely be set using setHost.
 * 
 * basedir defaults to .
 * port default to 22
 * 
 * username and password will be prompted using a dialog box if not set. So if you are in
 * an headless environment, provide username and password.
 */
public class SFTPResolver extends RepositoryResolver {  
    public SFTPResolver() {
        setRepository(new SFTPRepository());
    }
    public String getTypeName() {
        return "sftp";
    }
    public SFTPRepository getSFTPRepository() {
    	return (SFTPRepository) getRepository();
    }
	public String getHost() {
		return getSFTPRepository().getHost();
	}
	public String getPasswd() {
		return getSFTPRepository().getPasswd();
	}
	public int getPort() {
		return getSFTPRepository().getPort();
	}
	public String getUsername() {
		return getSFTPRepository().getUsername();
	}
	public void setHost(String host) {
		getSFTPRepository().setHost(host);
	}
	public void setPasswd(String passwd) {
		getSFTPRepository().setPasswd(passwd);
	}
	public void setPort(int port) {
		getSFTPRepository().setPort(port);
	}
	public void setUsername(String username) {
		getSFTPRepository().setUsername(username);
	}
    
}
