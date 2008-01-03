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
package org.apache.ivy.plugins.repository;

import java.io.IOException;
import java.io.InputStream;

public class BasicResource implements Resource {
    private boolean local;

    private String name;

    private long lastModified;

    private long contentLength;

    private boolean exists;

    public BasicResource(String name, boolean exists, long contentLength, long lastModified,
            boolean local) {
        this.name = name;
        this.exists = exists;
        this.contentLength = contentLength;
        this.lastModified = lastModified;
        this.local = local;
    }

    public Resource clone(String cloneName) {
        throw new UnsupportedOperationException("basic resource do not support the clone method");
    }

    public boolean exists() {
        return this.exists;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public String getName() {
        return this.name;
    }

    public boolean isLocal() {
        return this.local;
    }

    public InputStream openStream() throws IOException {
        throw new UnsupportedOperationException(
                "basic resource do not support the openStream method");
    }

    public String toString() {
        return getName();
    }

}
