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
package org.apache.ivy.osgi.repo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.util.ResolverHelper;

public class RepositoryManifestIterable extends AbstractFSManifestIterable<String> {

    private final Repository repo;

    /**
     * Default constructor
     * 
     * @param root
     *            the root directory of the file system to lookup
     */
    public RepositoryManifestIterable(Repository repo) {
        super("");
        this.repo = repo;
    }

    protected URI buildBundleURI(String location) throws IOException {
        Resource resource = repo.getResource(location);
        // We have a resource to transform into an URI, let's use some heuristics
        try {
            return new URI(resource.getName());
        } catch (URISyntaxException e) {
            return new File(resource.getName()).toURI();
        }
    }

    protected InputStream getInputStream(String f) throws IOException {
        return repo.getResource(f).openStream();
    }

    protected List<String> listBundleFiles(String dir) throws IOException {
        return asList(ResolverHelper.listAll(repo, dir));
    }

    protected List<String> listDirs(String dir) throws IOException {
        return asList(ResolverHelper.listAll(repo, dir));
    }

    private List<String> asList(String[] array) {
        return array == null ? Collections.<String> emptyList() : Arrays.<String> asList(array);
    }
}
