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

import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;

/**
 * Resource for SSH Ivy Repository
 */
public class SshResource implements Resource {

    private boolean resolved = false;

    private String uri = null;

    private boolean bExists = false;

    private long len = 0;

    private long lastModified = 0;

    private SshRepository repository = null;

    public SshResource() {
        resolved = true;
    }

    public SshResource(SshRepository repository, String uri) {
        this.uri = uri;
        this.repository = repository;
        resolved = false;
    }

    public SshResource(SshRepository repository, String uri, boolean bExists, long len,
            long lastModified) {
        this.uri = uri;
        this.bExists = bExists;
        this.len = len;
        this.lastModified = lastModified;
        this.repository = repository;
        resolved = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.repository.Resource#exists()
     */
    public boolean exists() {
        if (!resolved) {
            resolve();
        }
        return bExists;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.repository.Resource#getContentLength()
     */
    public long getContentLength() {
        if (!resolved) {
            resolve();
        }
        return len;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.repository.Resource#getLastModified()
     */
    public long getLastModified() {
        if (!resolved) {
            resolve();
        }
        return lastModified;
    }

    private void resolve() {
        Message.debug("SShResource: resolving " + uri);
        SshResource res = repository.resolveResource(uri);
        len = res.getContentLength();
        lastModified = res.getLastModified();
        bExists = res.exists();
        resolved = true;
        Message.debug("SShResource: resolved " + this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.ivy.repository.Resource#getName()
     */
    public String getName() {
        return uri;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SshResource:");
        buffer.append(uri);
        buffer.append(" (");
        buffer.append(len);
        buffer.append(")]");
        return buffer.toString();
    }

    public boolean isLocal() {
        return false;
    }

    public Resource clone(String cloneName) {
        return new SshResource(repository, cloneName);
    }

    public InputStream openStream() throws IOException {
        return repository.openStream(this);
    }
}
