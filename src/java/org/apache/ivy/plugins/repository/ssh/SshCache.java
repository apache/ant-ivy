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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Credentials;
import org.apache.ivy.util.CredentialsUtil;
import org.apache.ivy.util.Message;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

/**
 * a class to cache SSH Connections and Channel for the SSH Repository each session is defined by
 * connecting user / host / port two maps are used to find cache entries one map is using the above
 * keys, the other uses the session itself
 */
public final class SshCache {

    private static final int SSH_DEFAULT_PORT = 22;

    private SshCache() {
    };

    private static SshCache instance = new SshCache();

    public static SshCache getInstance() {
        return instance;
    }

    private class Entry {
        private Session session = null;

        private ChannelSftp channelSftp = null;

        private String host = null;

        private String user = null;

        private int port = SSH_DEFAULT_PORT;

        /**
         * @return the host
         */
        public String getHost() {
            return host;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * @return the user
         */
        public String getUser() {
            return user;
        }

        public Entry(Session newSession, String newUser, String newHost, int newPort) {
            session = newSession;
            host = newHost;
            user = newUser;
            port = newPort;
            IvyContext.getContext().getEventManager().addIvyListener(new IvyListener() {
                public void progress(IvyEvent event) {
                    event.getSource().removeIvyListener(this);
                    clearSession(session);
                }
            }, EndResolveEvent.NAME);
        }

        /**
         * attach an sftp channel to this cache entry
         * 
         * @param newChannel
         *            to attach
         */
        public void setChannelSftp(ChannelSftp newChannel) {
            if (channelSftp != null && newChannel != null) {
                throw new IllegalStateException("Only one sftp channelSftp per session allowed");
            }
            this.channelSftp = newChannel;
        }

        /**
         * @return the attached sftp channel
         */
        public ChannelSftp getChannelSftp() {
            return channelSftp;
        }

        /**
         * @return the session
         */
        private Session getSession() {
            return session;
        }

        /**
         * remove channelSftp and disconnect if necessary
         */
        public void releaseChannelSftp() {
            if (channelSftp != null) {
                if (channelSftp.isConnected()) {
                    Message.verbose(":: SFTP :: closing sftp connection from " + host + "...");
                    channelSftp.disconnect();
                    channelSftp = null;
                    Message.verbose(":: SFTP :: sftp connection closed from " + host);
                }
            }
        }
    }

    /**
     * key is username / host / port
     * 
     * @see SshCache.createCacheKey() for details
     */
    private Map uriCacheMap = new HashMap();

    /**
     * key is the session itself
     */
    private Map sessionCacheMap = new HashMap();

    /**
     * retrieves a session entry for a given hostname from the cache
     * 
     * @param hostname
     *            to retrieve session for
     * @return null or the existing entry
     */
    private Entry getCacheEntry(String user, String host, int port) {
        return (Entry) uriCacheMap.get(createCacheKey(user, host, port));
    }

    /**
     * Creates a cobined cache key from the given key parts
     * 
     * @param user
     *            name of the user
     * @param host
     *            of the connection
     * @param port
     *            of the connection
     * @return key for the cache
     */
    private static String createCacheKey(String user, String host, int port) {
        String portToUse = "22";
        if (port != -1 && port != SSH_DEFAULT_PORT) {
            portToUse = Integer.toString(port);
        }
        return user.toLowerCase(Locale.US).trim() + "@" + host.toLowerCase(Locale.US).trim() + ":"
                + portToUse;
    }

    /**
     * retrieves a session entry for a given session from the cache
     * 
     * @param session
     *            to retrieve cache entry for
     * @return null or the existing entry
     */
    private Entry getCacheEntry(Session session) {
        return (Entry) sessionCacheMap.get(session);
    }

    /**
     * Sets a session to a given combined key into the cache If an old session object already
     * exists, close and remove it
     * 
     * @param user
     *            of the session
     * @param host
     *            of the session
     * @param port
     *            of the session
     * @param newSession
     *            Session to save
     */
    private void setSession(String user, String host, int port, Session newSession) {
        Entry entry = (Entry) uriCacheMap.get(createCacheKey(user, host, port));
        Session oldSession = null;
        if (entry != null) {
            oldSession = entry.getSession();
        }
        if (oldSession != null && !oldSession.equals(newSession) && oldSession.isConnected()) {
            entry.releaseChannelSftp();
            String oldhost = oldSession.getHost();
            Message.verbose(":: SSH :: closing ssh connection from " + oldhost + "...");
            oldSession.disconnect();
            Message.verbose(":: SSH :: ssh connection closed from " + oldhost);
        }
        if ((newSession == null) && (entry != null)) {
            uriCacheMap.remove(createCacheKey(user, host, port));
            if (entry.getSession() != null) {
                sessionCacheMap.remove(entry.getSession());
            }
        } else {
            Entry newEntry = new Entry(newSession, user, host, port);
            uriCacheMap.put(createCacheKey(user, host, port), newEntry);
            sessionCacheMap.put(newSession, newEntry);
        }
    }

    /**
     * discardes session entries from the cache
     * 
     * @param session
     *            to clear
     */
    public void clearSession(Session session) {
        Entry entry = (Entry) sessionCacheMap.get(session);
        if (entry != null) {
            setSession(entry.getUser(), entry.getHost(), entry.getPort(), null);
        }
    }

    /**
     * retrieves an sftp channel from the cache
     * 
     * @param session
     *            to connect to
     * @return channelSftp or null if not successful (channel not existent or dead)
     */
    public ChannelSftp getChannelSftp(Session session) throws IOException {
        ChannelSftp channel = null;
        Entry entry = getCacheEntry(session);
        if (entry != null) {
            channel = entry.getChannelSftp();
            if (channel != null && !channel.isConnected()) {
                entry.releaseChannelSftp();
                channel = null;
            }
        }
        return channel;
    }

    /**
     * attaches a channelSftp to an existing session cache entry
     * 
     * @param session
     *            to attach the channel to
     * @param channel
     *            channel to attach
     */
    public void attachChannelSftp(Session session, ChannelSftp channel) {
        Entry entry = getCacheEntry(session);
        if (entry == null) {
            throw new IllegalArgumentException("No entry for " + session + " in the cache");
        }
        entry.setChannelSftp(channel);
    }

    /**
     * Attempts to connect to a local SSH agent (using either UNIX sockets or PuTTY's Pageant)
     * 
     * @param jsch
     *            Connection to be attached to an available local agent
     * @return true if connected to agent, false otherwise
     */
    private boolean attemptAgentUse(JSch jsch) {
        try {
            Connector con = ConnectorFactory.getDefault().createConnector();
            jsch.setIdentityRepository(new RemoteIdentityRepository(con));
            return true;
        } catch (Exception e) {
            Message.verbose(":: SSH :: Failure connecting to agent :: " + e.toString());
            return false;
        }
    }

    /**
     * Gets a session from the cache or establishes a new session if necessary
     * 
     * @param host
     *            to connect to
     * @param port
     *            to use for session (-1 == use standard port)
     * @param username
     *            for the session to use
     * @param userPassword
     *            to use for authentication (optional)
     * @param pemFile
     *            File to use for public key authentication
     * @param pemPassword
     *            to use for accessing the pemFile (optional)
     * @param passFile
     *            to store credentials
     * @param allowedAgentUse
     *            Whether to communicate with an agent for authentication
     * @return session or null if not successful
     */
    public Session getSession(String host, int port, String username, String userPassword,
            File pemFile, String pemPassword, File passFile, boolean allowedAgentUse)
            throws IOException {
        Checks.checkNotNull(host, "host");
        Checks.checkNotNull(username, "user");
        Entry entry = getCacheEntry(username, host, port);
        Session session = null;
        if (entry != null) {
            session = entry.getSession();
        }
        if (session == null || !session.isConnected()) {
            Message.verbose(":: SSH :: connecting to " + host + "...");
            try {
                JSch jsch = new JSch();
                if (port != -1) {
                    session = jsch.getSession(username, host, port);
                } else {
                    session = jsch.getSession(username, host);
                }
                if (allowedAgentUse) {
                    attemptAgentUse(jsch);
                }
                if (pemFile != null) {
                    jsch.addIdentity(pemFile.getAbsolutePath(), pemPassword);
                }
                session.setUserInfo(new CfUserInfo(host, username, userPassword, pemFile,
                        pemPassword, passFile));
                session.setDaemonThread(true);

                Properties config = new Properties();
                config.setProperty("PreferredAuthentications",
                    "publickey,keyboard-interactive,password");
                session.setConfig(config);

                session.connect();
                Message.verbose(":: SSH :: connected to " + host + "!");
                setSession(username, host, port, session);
            } catch (JSchException e) {
                if (passFile != null && passFile.exists()) {
                    passFile.delete();
                }
                IOException ex = new IOException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return session;
    }

    /**
     * feeds in password silently into JSch
     */
    private static class CfUserInfo implements UserInfo, UIKeyboardInteractive {

        private String userPassword;

        private String pemPassword;

        private String userName;

        private final File pemFile;

        private final String host;

        private final File passfile;

        public CfUserInfo(String host, String userName, String userPassword, File pemFile,
                String pemPassword, File passfile) {
            this.userPassword = userPassword;
            this.pemPassword = pemPassword;
            this.host = host;
            this.passfile = passfile;
            this.userName = userName;
            this.pemFile = pemFile;
        }

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
            if (userPassword == null) {
                Credentials c = CredentialsUtil.promptCredentials(new Credentials(null, host,
                        userName, userPassword), passfile);
                if (c != null) {
                    userName = c.getUserName();
                    userPassword = c.getPasswd();
                }
            }
            return userPassword;
        }

        public String getPassphrase() {
            if (pemPassword == null && pemFile != null) {
                Credentials c = CredentialsUtil.promptCredentials(
                    new Credentials(null, pemFile.getAbsolutePath(), userName, pemPassword),
                    passfile);
                if (c != null) {
                    userName = c.getUserName();
                    pemPassword = c.getPasswd();
                }
            }
            return pemPassword;
        }

        public String[] promptKeyboardInteractive(String destination, String name,
                String instruction, String[] prompt, boolean[] echo) {
            return new String[] {getPassword()};
        }
    }
}
