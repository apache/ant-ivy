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
package org.apache.ivy.core.retrieve;

import org.apache.ivy.core.LogOptions;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

/**
 * A set of options used during retrieve related tasks
 * 
 * @see RetrieveEngine
 */
public class RetrieveOptions extends LogOptions {
    /**
     * The names of configurations to retrieve. If the array consists only of '*', then all
     * configurations of the module will be retrieved.
     */
    private String[] confs = new String[] {"*"};

    /**
     * The pattern to which ivy files should be retrieved. If destIvyPattern is null no ivy files
     * will be copied.
     */
    private String destIvyPattern = null;

    /**
     * The filter to apply before retrieving artifacts.
     */
    private Filter artifactFilter = FilterHelper.NO_FILTER;

    /**
     * True if a synchronisation of the destination directory should be done, false if a simple copy
     * is enough. Synchronisation means that after the retrieve only files which have been retrieved
     * will be present in the destination directory, which means that some files may be deleted.
     */
    private boolean sync = false;

    /**
     * True if the original files should be used insteaad of their cache copy.
     */
    private boolean useOrigin = false;

    /**
     * True if symbolic links should be created instead of plain copy. Works only on OS supporting
     * symbolic links.
     */
    private boolean makeSymlinks = false;

    /**
     * The id used to store the resolve information.
     */
    private String resolveId;

    public Filter getArtifactFilter() {
        return artifactFilter;
    }

    public RetrieveOptions setArtifactFilter(Filter artifactFilter) {
        this.artifactFilter = artifactFilter;
        return this;
    }

    public String[] getConfs() {
        return confs;
    }

    public RetrieveOptions setConfs(String[] confs) {
        this.confs = confs;
        return this;
    }

    public String getDestIvyPattern() {
        return destIvyPattern;
    }

    public RetrieveOptions setDestIvyPattern(String destIvyPattern) {
        this.destIvyPattern = destIvyPattern;
        return this;
    }

    public boolean isMakeSymlinks() {
        return makeSymlinks;
    }

    public RetrieveOptions setMakeSymlinks(boolean makeSymlinks) {
        this.makeSymlinks = makeSymlinks;
        return this;
    }

    public boolean isSync() {
        return sync;
    }

    public RetrieveOptions setSync(boolean sync) {
        this.sync = sync;
        return this;
    }

    public boolean isUseOrigin() {
        return useOrigin;
    }

    public RetrieveOptions setUseOrigin(boolean useOrigin) {
        this.useOrigin = useOrigin;
        return this;
    }

    public String getResolveId() {
        return resolveId;
    }

    public RetrieveOptions setResolveId(String resolveId) {
        this.resolveId = resolveId;
        return this;
    }

}
