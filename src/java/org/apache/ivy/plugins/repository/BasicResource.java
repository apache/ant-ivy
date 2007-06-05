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
    private boolean _local;

    private String _name;

    private long _lastModified;

    private long _contentLength;

    private boolean _exists;

    public BasicResource(String name, boolean exists, long contentLength, long lastModified,
            boolean local) {
        _name = name;
        _exists = exists;
        _contentLength = contentLength;
        _lastModified = lastModified;
        _local = local;
    }

    public Resource clone(String cloneName) {
        throw new UnsupportedOperationException("basic resource do not support the clone method");
    }

    public boolean exists() {
        return _exists;
    }

    public long getContentLength() {
        return _contentLength;
    }

    public long getLastModified() {
        return _lastModified;
    }

    public String getName() {
        return _name;
    }

    public boolean isLocal() {
        return _local;
    }

    public InputStream openStream() throws IOException {
        throw new UnsupportedOperationException(
                "basic resource do not support the openStream method");
    }

    public String toString() {
        return getName();
    }

}
