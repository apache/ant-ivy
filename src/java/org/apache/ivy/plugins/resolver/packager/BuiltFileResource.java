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
package org.apache.ivy.plugins.resolver.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.plugins.repository.Resource;

/**
 * Represents an artifact built by a {@link PackagerResolver}.
 */
public class BuiltFileResource implements Resource {

    /**
     * Where the build file should put built artifacts (relative to the build directory). Value is:
     * * {@value}
     */
    public static final String BUILT_ARTIFACT_PATTERN = "artifacts/[type]s/[artifact].[ext]";

    private final File file;

    public BuiltFileResource(File file) {
        this.file = file;
    }

    public BuiltFileResource(File dir, Artifact artifact) {
        this(new File(dir, IvyPatternHelper.substitute(BUILT_ARTIFACT_PATTERN, artifact)));
    }

    public String getName() {
        return file.toURI().toString();
    }

    public Resource clone(String name) {
        return new BuiltFileResource(new File(name));
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

    public boolean isLocal() {
        return false;
    }

    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }
}
