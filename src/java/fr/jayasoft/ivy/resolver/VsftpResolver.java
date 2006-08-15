/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.vsftp.VsftpRepository;

/**
 * This resolver uses SecureCRT vsft to access an sftp server.
 * 
 * It supports listing and publishing.
 * 
 * The server host should absolutely be set using setHost, so does the username.
 * 
 */
public class VsftpResolver extends RepositoryResolver { 
	public VsftpResolver() {
        setRepository(new VsftpRepository());
    }
    public String getTypeName() {
        return "vsftp";
    }
    public VsftpRepository getVsftpRepository() {
    	return (VsftpRepository) getRepository();
    }
	public void disconnect() {
		getVsftpRepository().disconnect();
	}
	public String getAuthentication() {
		return getVsftpRepository().getAuthentication();
	}
	public String getHost() {
		return getVsftpRepository().getHost();
	}
	public String getUsername() {
		return getVsftpRepository().getUsername();
	}
	public void setAuthentication(String authentication) {
		getVsftpRepository().setAuthentication(authentication);
	}
	public void setHost(String host) {
		getVsftpRepository().setHost(host);
	}
	public void setUsername(String username) {
		getVsftpRepository().setUsername(username);
	}
	public void setReuseConnection(long time) {
		getVsftpRepository().setReuseConnection(time);
	}
	public void setReadTimeout(long readTimeout) {
		getVsftpRepository().setReadTimeout(readTimeout);
	}
}
