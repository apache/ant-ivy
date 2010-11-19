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
package org.apache.ivy.osgi.ivy.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.repository.file.FileResource;

/**
 * A resource decorator that handles extracting jar file entries using the bang(!) notation to
 * separate the internal entry name.
 */
public class JarEntryResource implements Resource {

    private final String entryName;

    private final Resource resource;

    public JarEntryResource(String name) {
        final String[] tokens = name.split("[!]");
        final String path = tokens[0];
        resource = new FileResource(new FileRepository(), new File(path));
        entryName = tokens[1];
    }

    public JarEntryResource(Resource resource, String entryName) {
        this.resource = resource;
        this.entryName = entryName;
    }

    public String toString() {
        return "resource:" + resource + ", jarEntry=" + entryName;
    }

    public Resource clone(String cloneName) {
        return resource.clone(cloneName);
    }

    public boolean exists() {
        return resource.exists();
    }

    public long getContentLength() {
        return resource.getContentLength();
    }

    public long getLastModified() {
        return resource.getLastModified();
    }

    public String getName() {
        return resource.getName() + "!" + entryName;
    }

    public boolean isLocal() {
        return resource.isLocal();
    }

    public InputStream openStream() throws IOException {
        final ZipInputStream zis = new ZipInputStream(resource.openStream());
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equalsIgnoreCase(entryName)) {
                break;
            }
        }
        if (entry == null) {
            throw new IllegalStateException("Jar entry: " + entryName + ", not in resource:"
                    + resource);
        }
        return zis;
    }

}
