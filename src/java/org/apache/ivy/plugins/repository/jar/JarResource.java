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
package org.apache.ivy.plugins.repository.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.ivy.plugins.repository.Resource;

public class JarResource implements Resource {

    private final JarFile jarFile;

    private final String path;

    private ZipEntry entry;

    public JarResource(JarFile jarFile, String path) {
        this.jarFile = jarFile;
        this.path = path;
        entry = jarFile.getEntry(path);
    }

    public String getName() {
        return path;
    }

    public long getLastModified() {
        return entry.getTime();
    }

    public long getContentLength() {
        return entry.getSize();
    }

    public boolean exists() {
        return entry != null;
    }

    public boolean isLocal() {
        // not local as it is not a directly accessible file
        return false;
    }

    public Resource clone(String cloneName) {
        return new JarResource(jarFile, cloneName);
    }

    public InputStream openStream() throws IOException {
        return jarFile.getInputStream(entry);
    }

    public String toString() {
        return jarFile.getName() + "!" + getName();
    }
}
