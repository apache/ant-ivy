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
package org.apache.ivy.plugins.repository.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.plugins.repository.Resource;

public class FileResource implements Resource {
    private File file;

    private FileRepository repository;

    public FileResource(FileRepository repository, File f) {
        this.repository = repository;
        this.file = f;
    }

    public String getName() {
        return file.getPath();
    }

    public Resource clone(String cloneName) {
        return new FileResource(repository, repository.getFile(cloneName));
    }

    public long getLastModified() {
        return file.lastModified();
    }

    public long getContentLength() {
        return file.length();
    }

    public boolean exists() {
        return file.exists();
    }

    public String toString() {
        return getName();
    }

    public File getFile() {
        return file;
    }

    public FileRepository getRepository() {
        return repository;
    }

    public boolean isLocal() {
        return repository.isLocal();
    }

    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }
}
