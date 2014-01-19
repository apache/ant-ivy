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
package org.apache.ivy.plugins.repository.sftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.ssh.AbstractSshBasedRepository;
import org.apache.ivy.plugins.repository.ssh.SshCache;
import org.apache.ivy.util.Message;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

/**
 * SFTP Repository, allow to use a repository accessed by sftp protocol. It supports all operations:
 * get, put and list. It relies on jsch for sftp handling, and thus is compatible with sftp version
 * 0, 1, 2 and 3
 */
public class SFTPRepository extends AbstractSshBasedRepository {
    // this must be a long to ensure the multiplication done below uses longs
    // instead of ints which are not big enough to hold the result
    private static final long MILLIS_PER_SECOND = 1000;

    private final class MyProgressMonitor implements SftpProgressMonitor {
        private long totalLength;

        public void init(int op, String src, String dest, long max) {
            totalLength = max;
            fireTransferStarted(max);
        }

        public void end() {
            fireTransferCompleted(totalLength);
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
     * This method is similar to getResource, except that the returned resource is fully initialized
     * (resolved in the sftp repository), and that the given string is a full remote path
     * 
     * @param path
     *            the full remote path in the repository of the resource
     * @return a fully initialized resource, able to answer to all its methods without needing any
     *         further connection
     */
    public Resource resolveResource(String path) {
        try {
            ChannelSftp c = getSftpChannel(path);

            Collection r = c.ls(getPath(path));

            if (r != null) {
                for (Iterator iter = r.iterator(); iter.hasNext();) {
                    Object obj = iter.next();
                    if (obj instanceof LsEntry) {
                        LsEntry entry = (LsEntry) obj;
                        SftpATTRS attrs = entry.getAttrs();
                        return new BasicResource(path, true, attrs.getSize(), attrs.getMTime()
                                * MILLIS_PER_SECOND, false);
                    }
                }
            }
        } catch (Exception e) {
            Message.debug("Error while resolving resource " + path, e);
            // silent fail, return unexisting resource
        }

        return new BasicResource(path, false, 0, 0, false);
    }

    public InputStream openStream(SFTPResource resource) throws IOException {
        ChannelSftp c = getSftpChannel(resource.getName());
        try {
            String path = getPath(resource.getName());
            return c.get(path);
        } catch (SftpException e) {
            IOException ex = new IOException("impossible to open stream for " + resource + " on "
                    + getHost() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            ex.initCause(e);
            throw ex;
        } catch (URISyntaxException e) {
            IOException ex = new IOException("impossible to open stream for " + resource + " on "
                    + getHost() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            ex.initCause(e);
            throw ex;
        }
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        ChannelSftp c = getSftpChannel(source);
        try {
            String path = getPath(source);
            c.get(path, destination.getAbsolutePath(), new MyProgressMonitor());
        } catch (SftpException e) {
            IOException ex = new IOException("impossible to get " + source + " on " + getHost()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            ex.initCause(e);
            throw ex;
        } catch (URISyntaxException e) {
            IOException ex = new IOException("impossible to get " + source + " on " + getHost()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            ex.initCause(e);
            throw ex;
        }
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
        ChannelSftp c = getSftpChannel(destination);
        try {
            String path = getPath(destination);
            if (!overwrite && checkExistence(path, c)) {
                throw new IOException("destination file exists and overwrite == false");
            }
            if (path.indexOf('/') != -1) {
                mkdirs(path.substring(0, path.lastIndexOf('/')), c);
            }
            c.put(source.getAbsolutePath(), path, new MyProgressMonitor());
        } catch (SftpException e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (URISyntaxException e) {
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
                mkdirs(directory.substring(0, directory.lastIndexOf('/')), c);
            }
            c.mkdir(directory);
        }
    }

    private String getPath(String sftpURI) throws URISyntaxException {
        String result = null;
        URI uri = new URI(sftpURI);
        result = uri.getPath();

        if (result == null) {
            throw new URISyntaxException(sftpURI, "Missing path in URI.");
        }

        return result;
    }

    public List list(String parent) throws IOException {
        try {
            ChannelSftp c = getSftpChannel(parent);
            String path = getPath(parent);
            Collection r = c.ls(path);
            if (r != null) {
                if (!path.endsWith("/")) {
                    path = parent + "/";
                }
                List result = new ArrayList();
                for (Iterator iter = r.iterator(); iter.hasNext();) {
                    Object obj = iter.next();
                    if (obj instanceof LsEntry) {
                        LsEntry entry = (LsEntry) obj;
                        if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                            continue;
                        }
                        result.add(path + entry.getFilename());
                    }
                }
                return result;
            }
        } catch (SftpException e) {
            IOException ex = new IOException("Failed to return a listing for '" + parent + "'");
            ex.initCause(e);
            throw ex;
        } catch (URISyntaxException usex) {
            IOException ex = new IOException("Failed to return a listing for '" + parent + "'");
            ex.initCause(usex);
            throw ex;
        }
        return null;
    }

    /**
     * Checks the existence for a remote file
     * 
     * @param file
     *            to check
     * @param channel
     *            to use
     * @returns true if file exists, false otherwise
     * @throws IOException
     * @throws SftpException
     */
    private boolean checkExistence(String file, ChannelSftp channel) throws IOException,
            SftpException {
        try {
            return channel.stat(file) != null;
        } catch (SftpException ex) {
            return false;
        }
    }

    /**
     * Establish the connection to the server if not yet connected, and listen to ivy events for
     * closing connection when resolve is finished. Not meant to be used in multi threaded
     * environment.
     * 
     * @return the ChannelSftp with which a connection is established
     * @throws IOException
     *             if any connection problem occurs
     */
    private ChannelSftp getSftpChannel(String pathOrUri) throws IOException {
        Session session = getSession(pathOrUri);
        String host = session.getHost();
        ChannelSftp channel = SshCache.getInstance().getChannelSftp(session);
        if (channel == null) {
            try {
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                Message.verbose(":: SFTP :: connected to " + host + "!");
                SshCache.getInstance().attachChannelSftp(session, channel);
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
