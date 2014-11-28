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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.CredentialsUtil;
import org.apache.ivy.util.Message;

import com.jcraft.jsch.ConfigRepository.Config;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.ConfigRepository;

public abstract class AbstractSshBasedRepository extends AbstractRepository {

    private File keyFile = null;

    private File passFile = null;

    private String userPassword = null;

    private String keyFilePassword = null;

    private String user = null;

    private String host = null;

    private int port = -1;

    private boolean allowedAgentUse = false;

    private String sshConfig = null;

    public AbstractSshBasedRepository() {
        super();
    }

    /**
     * hashmap of user/hosts with credentials. key is hostname, value is Credentials
     **/
    private static HashMap credentialsCache = new HashMap();

    private static final int MAX_CREDENTILAS_CACHE_SIZE = 100;

    /**
     * get a new session using the default attributes if the given String is a full uri, use the
     * data from the uri instead
     * 
     * @param pathOrUri
     *            might be just a path or a full ssh or sftp uri
     * @return matching Session
     */
    protected Session getSession(String pathOrUri) throws IOException {
        URI uri = parseURI(pathOrUri);
        String host = getHost();
        int port = getPort();
        String user = getUser();
        String userPassword = getUserPassword();
        String sshConfig = getSshConfig();
        File keyFile = getKeyFile();
        if (uri != null && uri.getScheme() != null) {
            if (uri.getHost() != null) {
                host = uri.getHost();
            }
            if (uri.getPort() != -1) {
                port = uri.getPort();
            }
            if (uri.getUserInfo() != null) {
                String userInfo = uri.getUserInfo();
                if (userInfo.indexOf(":") == -1) {
                    user = userInfo;
                } else {
                    user = userInfo.substring(0, userInfo.indexOf(":"));
                    userPassword = userInfo.substring(userInfo.indexOf(":") + 1);
                }
            }
        }

        if (sshConfig != null) {
            ConfigRepository configRepository = OpenSSHConfig.parseFile(sshConfig);
            Config config = configRepository.getConfig(host);
            host = config.getHostname();
            if (user == null) {
                user = config.getUser();
            }
            String keyFilePath = config.getValue("IdentityFile");
            if (keyFilePath != null && keyFile == null) {
                keyFile = new File(keyFilePath);
            }
        }


        if (host == null) {
            throw new IllegalArgumentException(
                    "missing host information. host should be provided either "
                            + "directly on the repository or in the connection URI "
                            + ", or in the openssh config file specified by sshConfig");
        }
        if (user == null) {
            Credentials c = requestCredentials(host);
            if (c != null) {
                user = c.getUserName();
                userPassword = c.getPasswd();
            } else {
                Message.error("username is not set");
            }
        }
        return SshCache.getInstance().getSession(host, port, user, userPassword, keyFile,
            getKeyFilePassword(), getPassFile(), isAllowedAgentUse());
    }

    /**
     * Just check the uri for sanity
     * 
     * @param source
     *            String of the uri
     * @return URI object of the String or null
     */
    private URI parseURI(String source) {
        try {
            URI uri = new URI(source);
            if (uri.getScheme() != null
                    && !uri.getScheme().toLowerCase(Locale.US)
                            .equals(getRepositoryScheme().toLowerCase(Locale.US))) {
                throw new URISyntaxException(source, "Wrong scheme in URI. Expected "
                        + getRepositoryScheme() + " as scheme!");
            }
            if (uri.getHost() == null && getHost() == null) {
                throw new URISyntaxException(source, "Missing host in URI or in resolver");
            }
            if (uri.getPath() == null) {
                throw new URISyntaxException(source, "Missing path in URI");
            }
            // if (uri.getUserInfo() == null && getUser() == null) {
            // throw new URISyntaxException(source, "Missing username in URI or in resolver");
            // }
            return uri;
        } catch (URISyntaxException e) {
            Message.error(e.getMessage());
            Message.error("The uri '" + source + "' is in the wrong format.");
            Message.error("Please use " + getRepositoryScheme()
                    + "://user:pass@hostname/path/to/repository");
            return null;
        }
    }

    /**
     * Called, when user was not found in URL. Maintain static hashe of credentials and retrieve or
     * ask credentials for host.
     * 
     * @param host
     *            host for which we want to get credentials.
     * @return credentials for given host
     **/
    private Credentials requestCredentials(String host) {
        Object o = credentialsCache.get(host);
        if (o == null) {
            Credentials c = CredentialsUtil.promptCredentials(new Credentials(null, host, user,
                    userPassword), getPassFile());
            if (c != null) {
                if (credentialsCache.size() > MAX_CREDENTILAS_CACHE_SIZE) {
                    credentialsCache.clear();
                }
                credentialsCache.put(host, c);
            }
            return c;
        } else {
            return (Credentials) o;
        }
    }

    /**
     * closes the session and remove it from the cache (eg. on case of errors)
     * 
     * @param session
     *            key for the cache
     * @param pathOrUri
     *            to release
     */
    protected void releaseSession(Session session, String pathOrUri) {
        session.disconnect();
        SshCache.getInstance().clearSession(session);
    }

    /**
     * set the default user to use for the connection if no user is given or a PEM file is used
     * 
     * @param user
     *            to use
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the user to use for the connection if no user is given or a PEM file is used
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the full file path to use for accessing a PEM key file
     * 
     * @param filePath
     *            fully qualified name
     */
    public void setKeyFile(File filePath) {
        this.keyFile = filePath;
        if (!keyFile.exists()) {
            Message.warn("Pemfile " + keyFile.getAbsolutePath() + " doesn't exist.");
            keyFile = null;
        } else if (!keyFile.canRead()) {
            Message.warn("Pemfile " + keyFile.getAbsolutePath() + " not readable.");
            keyFile = null;
        } else {
            Message.debug("Using " + keyFile.getAbsolutePath() + " as keyfile.");
        }
    }

    /**
     * @return the keyFile
     */
    public File getKeyFile() {
        return keyFile;
    }

    /**
     * @param password
     *            password to use for user/password authentication
     */
    public void setUserPassword(String password) {
        this.userPassword = password;
    }

    /**
     * @return the keyFile password for public key based authentication
     */
    public String getKeyFilePassword() {
        return keyFilePassword;
    }

    /**
     * @param keyFilePassword
     *            sets password for public key based authentication
     */
    public void setKeyFilePassword(String keyFilePassword) {
        this.keyFilePassword = keyFilePassword;
    }

    /**
     * @return the user password
     */
    public String getUserPassword() {
        return userPassword;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @param passFile
     *            the passfile to set
     */
    public void setPassFile(File passFile) {
        this.passFile = passFile;
    }

    /**
     * @return the passFile
     */
    public File getPassFile() {
        return passFile;
    }

    /**
     * @return allowedAgentUse Whether use of a local SSH agent for authentication is allowed
     */
    public boolean isAllowedAgentUse() {
        return allowedAgentUse;
    }

    /**
     * @param allowedAgentUse
     *            Whether use of a local SSH agent for authentication is allowed
     */
    public void setAllowedAgentUse(boolean allowedAgentUse) {
        this.allowedAgentUse = allowedAgentUse;
    }

    /**
     * @return sshConfig Path to a local ssh config file
     */
    public String getSshConfig() {
        return sshConfig;
    }

    /**
     * @param sshConfig
     *            Path to a local ssh config file
     */
    public void setSshConfig(String sshConfig) {
        this.sshConfig = sshConfig;
    }

    protected abstract String getRepositoryScheme();

}
