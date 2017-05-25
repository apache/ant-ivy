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
package org.apache.ivy.plugins.resolver;

import java.io.File;

import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.ssh.AbstractSshBasedRepository;

/**
 * Abstract base class for all resolvers using SSH All necessary connection parameters can be set
 * here via attributes. However all attributes defined in the pattern url of the resolver will have
 * higher priority and will overwrite the values given here. To specify connection parameters in the
 * pattern, you have to specify a full url and not just a path as pattern. e.g.
 * pattern="/path/to/my/repos/[artifact].[ext]" will use all connection parameters from this class
 * e.g. pattern="ssh://myserver.com/path/to/my/repos/[artifact].[ext]" will use all parameters from
 * this class with the exception of the host, which will be "myserver.com" e.g.
 * pattern="sftp://user:geheim@myserver.com:8022/path/to/my/repos/[artifact].[ext]" will use only
 * the keyFile and keyFilePassword from this class (if needed). Rest will come from the url.
 */
public abstract class AbstractSshBasedResolver extends RepositoryResolver {

    private boolean passfileSet = false;

    public AbstractSshBasedResolver() {
        super();
    }

    private AbstractSshBasedRepository getSshBasedRepository() {
        return ((AbstractSshBasedRepository) getRepository());
    }

    /**
     * Sets the location of the Public Key file to use for authentication
     * 
     * @param filePath
     *            full file path name
     */
    public void setKeyFile(File filePath) {
        getSshBasedRepository().setKeyFile(filePath);
    }

    /**
     * Determines whether a local SSH agent may be used for authentication
     * 
     * @param allowedAgentUse
     *            true if an agent may be used if available
     */
    public void setAllowedAgentUse(boolean allowedAgentUse) {
        getSshBasedRepository().setAllowedAgentUse(allowedAgentUse);
    }

    /**
     * Optional password file. If set the repository will use it as an encypted property file, to
     * load username and passwd entries, and to store them if the user choose to do so. Defaults to
     * user.dir/.ivy/[host].sftp.passwd, set it to null to disable this feature.
     */
    public void setPassfile(File passfile) {
        getSshBasedRepository().setPassFile(passfile);
        passfileSet = true;
    }

    public void setSettings(IvySettings settings) {
        super.setSettings(settings);
        if (!passfileSet) {
            getSshBasedRepository().setPassFile(
                new File(settings.getDefaultIvyUserDir(), getSshBasedRepository().getHost()
                        + ".ssh.passwd"));
        }
    }

    /**
     * Sets the password to authenticate the user if password based login is used if no password is
     * set and password based login is used, user will be prompted for it the password can also be
     * set by using a full url for the pattern, like
     * "sftp://user:password@myserver.com/path/to/repos/[artifact].[ext]"
     * 
     * @param password
     *            to use
     */
    public void setUserPassword(String password) {
        getSshBasedRepository().setUserPassword(password);
    }

    /**
     * Sets the password to use for decrypting key file (if it is encrypted) if no password is set
     * and the keyfile is encrypted, the user will be prompted for the password if the keyfile is
     * passwordless, this parameter will be ignored if given
     * 
     * @param password
     *            to use
     */
    public void setKeyFilePassword(String password) {
        getSshBasedRepository().setKeyFilePassword(password);
    }

    /**
     * sets the user to use for the ssh communication the user can also be set by using a full url
     * for the pattern, like "ssh://user@myserver.com/path/to/repos/[artifact].[ext]"
     * 
     * @param user
     *            on the target system
     */
    public void setUser(String user) {
        getSshBasedRepository().setUser(user);
    }

    /**
     * sets the host to use for the ssh communication the host can also be set by using a full url
     * for the pattern, like "ssh://myserver.com/path/to/repos/[artifact].[ext]"
     * 
     * @param host
     *            of the target system
     */
    public void setHost(String host) {
        getSshBasedRepository().setHost(host);
    }

    /**
     * sets the port to use for the ssh communication port 22 is default the port can also be set by
     * using a full url for the pattern, like
     * "sftp://myserver.com:8022/path/to/repos/[artifact].[ext]"
     * 
     * @param port
     *            of the target system
     */
    public void setPort(int port) {
        getSshBasedRepository().setPort(port);
    }

    /**
     * sets the path to an OpenSSH-style config file to be used for reading configuration values for
     * an ssh repository, such as a username
     *
     * @param path
     *            of the config file
     */
    public void setSshConfig(String sshConfig) {
        getSshBasedRepository().setSshConfig(sshConfig);
    }

    @Override
    public abstract String getTypeName();
}
