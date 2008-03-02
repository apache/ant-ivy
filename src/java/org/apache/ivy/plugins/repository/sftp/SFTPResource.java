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

import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.plugins.repository.Resource;

public class SFTPResource implements Resource {
    private SFTPRepository repository;

    private String path;

    private transient boolean init = false;

    private transient boolean exists;

    private transient long lastModified;

    private transient long contentLength;

    public SFTPResource(SFTPRepository repository, String path) {
        this.repository = repository;
        this.path = path;
    }

    public String getName() {
        return path;
    }

    public Resource clone(String cloneName) {
        return new SFTPResource(repository, cloneName);
    }

    public long getLastModified() {
        init();
        return lastModified;
    }

    public long getContentLength() {
        init();
        return contentLength;
    }

    public boolean exists() {
        init();
        return exists;
    }

    private void init() {
        if (!init) {
            Resource r = repository.resolveResource(path);
            contentLength = r.getContentLength();
            lastModified = r.getLastModified();
            exists = r.exists();
            init = true;
        }
    }

    public String toString() {
        return getName();
    }

    public boolean isLocal() {
        return false;
    }

    public InputStream openStream() throws IOException {
        return repository.openStream(this);
    }
}
