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
package org.apache.ivy.plugins.repository.vfs;

import org.apache.ivy.Ivy;

public class VfsURI {
    private String host;

    private String passwd;

    private String path;

    private String scheme;

    private String user;

    // VFS Schemes
    public static final String SCHEME_CIFS = "smb";

    public static final String SCHEME_FILE = "file";

    public static final String SCHEME_FTP = "ftp";

    public static final String SCHEME_HTTP = "http";

    public static final String SCHEME_HTTPS = "https";

    public static final String SCHEME_SFTP = "sftp";

    public static final String SCHEME_WEBDAV = "webdav";

    public static final String[] SUPPORTED_SCHEMES = new String[] {
    // add other schemes here if other can be tested on your machine
    SCHEME_FILE};

    /**
     * Create a set of valid VFS URIs for the file access protocol
     * 
     * @param resourcePath
     *            relative path (from the base repo) to the resource to be accessed
     * @return
     */
    public static VfsURI vfsURIFactory(String scheme, String resource, Ivy ivy) {
        VfsURI vfsURI = null;
        if (scheme.equals(SCHEME_CIFS)) {
            vfsURI = new VfsURI(SCHEME_CIFS, ivy.getVariable(VfsTestHelper.PROP_VFS_USER_ID),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_USER_PASSWD),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_HOST),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_SAMBA_REPO) + "/" + resource);
        } else if (scheme.equals(SCHEME_FILE)) {
            vfsURI = new VfsURI(SCHEME_FILE, null, null, null, VfsTestHelper.CWD + "/"
                    + VfsTestHelper.TEST_REPO_DIR + "/" + resource);
        } else if (scheme.equals(SCHEME_FTP)) {
            vfsURI = new VfsURI(SCHEME_FTP, ivy.getVariable(VfsTestHelper.PROP_VFS_USER_ID),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_USER_PASSWD),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_HOST), VfsTestHelper.CWD + "/"
                            + VfsTestHelper.TEST_REPO_DIR + "/" + resource);
        } else if (scheme.equals(SCHEME_SFTP)) {
            vfsURI = new VfsURI(SCHEME_SFTP, ivy.getVariable(VfsTestHelper.PROP_VFS_USER_ID),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_USER_PASSWD),
                    ivy.getVariable(VfsTestHelper.PROP_VFS_HOST), VfsTestHelper.CWD + "/"
                            + VfsTestHelper.TEST_REPO_DIR + "/" + resource);
        }
        return vfsURI;
    }

    /**
     * Create a wellformed VFS resource identifier
     * 
     * @param scheme
     *            the name of the scheme used to acces the resource
     * @param user
     *            a user name. May be <code>null</code>
     * @param passwd
     *            a passwd. May be <code>null</code>
     * @param host
     *            a host identifier. May be <code>null</code>
     * @param path
     *            a scheme spacific path to a resource
     */
    public VfsURI(String scheme, String user, String passwd, String host, String path) {
        this.scheme = scheme.trim();

        if (user != null) {
            this.user = user.trim();
        } else {
            this.user = null;
        }

        if (passwd != null) {
            this.passwd = passwd.trim();
        } else {
            this.passwd = null;
        }

        if (host != null) {
            this.host = host.trim();
        } else {
            this.host = null;
        }

        this.path = normalizePath(path);
    }

    /**
     * Return a well-formed VFS Resource identifier
     * 
     * @return <code>String<code> representing a well formed VFS resource identifier
     */
    public String getVfsURI() {
        StringBuffer uri = new StringBuffer();
        uri.append(this.scheme + "://");

        // not all resource identifiers include user/passwd specifiers
        if (user != null && user.trim().length() > 0) {
            uri.append(this.user + ":");

            if (passwd != null && passwd.trim().length() > 0) {
                this.passwd = passwd.trim();
            } else {
                this.passwd = "";
            }
            uri.append(this.passwd + "@");
        }

        // not all resource identifiers include a host specifier
        if (host != null && host.trim().length() > 0) {
            this.host = host.trim();
            uri.append(this.host);
        }

        uri.append(this.path);
        return uri.toString();
    }

    /**
     * Convert a resource path to the format required for a VFS resource identifier
     * 
     * @param path
     *            <code>String</code> path to the resource
     * @return <code>String</code> representing a normalized resource path
     */
    private String normalizePath(String path) {
        // all backslashes replaced with forward slashes
        String normalizedPath = path.replaceAll("\\\\", "/");

        // collapse multiple instance of forward slashes to single slashes
        normalizedPath = normalizedPath.replaceAll("//+", "/");

        // ensure that our path starts with a forward slash
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        return normalizedPath.trim();
    }

    public String toString() {
        return getVfsURI();
    }

    public String getScheme() {
        return scheme;
    }
}
