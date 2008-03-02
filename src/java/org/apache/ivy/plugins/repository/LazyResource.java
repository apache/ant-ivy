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
    private boolean init = false;

    private boolean local;

    private String name;

    private long lastModified;

    private long contentLength;

    private boolean exists;

    public LazyResource(String name) {
        this.name = name;
    }

    protected abstract void init();

    private void checkInit() {
        if (!init) {
            init();
            init = true;
        }
    }

    public boolean exists() {
        checkInit();
        return exists;
    }

    public long getContentLength() {
        checkInit();
        return contentLength;
    }

    public long getLastModified() {
        checkInit();
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public boolean isLocal() {
        checkInit();
        return local;
    }

    public String toString() {
        return getName();
    }

    protected void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    protected void setExists(boolean exists) {
        this.exists = exists;
    }

    protected void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    protected void setLocal(boolean local) {
        this.local = local;
    }

    protected void init(Resource r) {
        setContentLength(r.getContentLength());
        setLocal(r.isLocal());
        setLastModified(r.getLastModified());
        setExists(r.exists());
    }

}
