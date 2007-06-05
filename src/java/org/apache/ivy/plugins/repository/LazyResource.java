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

public abstract class LazyResource implements Resource {
    private boolean _init = false;

    private boolean _local;

    private String _name;

    private long _lastModified;

    private long _contentLength;

    private boolean _exists;

    public LazyResource(String name) {
        _name = name;
    }

    protected abstract void init();

    private void checkInit() {
        if (!_init) {
            init();
            _init = true;
        }
    }

    public boolean exists() {
        checkInit();
        return _exists;
    }

    public long getContentLength() {
        checkInit();
        return _contentLength;
    }

    public long getLastModified() {
        checkInit();
        return _lastModified;
    }

    public String getName() {
        return _name;
    }

    public boolean isLocal() {
        checkInit();
        return _local;
    }

    public String toString() {
        return getName();
    }

    protected void setContentLength(long contentLength) {
        _contentLength = contentLength;
    }

    protected void setExists(boolean exists) {
        _exists = exists;
    }

    protected void setLastModified(long lastModified) {
        _lastModified = lastModified;
    }

    protected void setLocal(boolean local) {
        _local = local;
    }

    protected void init(Resource r) {
        setContentLength(r.getContentLength());
        setLocal(r.isLocal());
        setLastModified(r.getLastModified());
        setExists(r.exists());
    }

}
