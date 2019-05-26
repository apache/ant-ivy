/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

/**
 * A set of options used during retrieve related tasks
 *
 * @see RetrieveEngine
 */
public class RetrieveOptions extends LogOptions {
    public static final String OVERWRITEMODE_NEVER = "never";

    public static final String OVERWRITEMODE_ALWAYS = "always";

    public static final String OVERWRITEMODE_NEWER = "newer";

    public static final String OVERWRITEMODE_DIFFERENT = "different";

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
     * The pattern to which artifacts should be retrieved.
     */
    private String destArtifactPattern = null;

    /**
     * The filter to apply before retrieving artifacts.
     */
    private Filter<Artifact> artifactFilter = FilterHelper.NO_FILTER;

    /**
     * True if a synchronisation of the destination directory should be done, false if a simple
     * copy is enough. Synchronisation means that after the retrieve only files which have been
     * retrieved will be present in the destination directory, which means that some files may be
     * deleted.
     */
    private boolean sync = false;

    private String overwriteMode = OVERWRITEMODE_NEWER;

    /**
     * True if the original files should be used instead of their cache copy.
     */
    private boolean useOrigin = false;

    /**
     * True if symbolic links should be created instead of plain copy. Works only on OS supporting
     * symbolic links.
     */
    private boolean makeSymlinks = false;

    @Deprecated
    private boolean makeSymlinksInMass = false;

    /**
     * The id used to store the resolve information.
     */
    private String resolveId;

    private FileNameMapper mapper;

    public RetrieveOptions() {
    }

    public RetrieveOptions(RetrieveOptions options) {
        super(options);
        this.confs = options.confs;
        this.destIvyPattern = options.destIvyPattern;
        this.destArtifactPattern = options.destArtifactPattern;
        this.artifactFilter = options.artifactFilter;
        this.sync = options.sync;
        this.overwriteMode = options.overwriteMode;
        this.useOrigin = options.useOrigin;
        this.makeSymlinks = options.makeSymlinks;
        this.makeSymlinksInMass = options.makeSymlinksInMass;
        this.resolveId = options.resolveId;
        this.mapper = options.mapper;
    }

    public String getDestArtifactPattern() {
        return destArtifactPattern;
    }

    public RetrieveOptions setDestArtifactPattern(String destArtifactPattern) {
        this.destArtifactPattern = destArtifactPattern;
        return this;
    }

    public Filter<Artifact> getArtifactFilter() {
        return artifactFilter;
    }

    public RetrieveOptions setArtifactFilter(Filter<Artifact> artifactFilter) {
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

    public String getOverwriteMode() {
        return overwriteMode == null ? OVERWRITEMODE_NEWER : overwriteMode;
    }

    public RetrieveOptions setOverwriteMode(String overwriteMode) {
        this.overwriteMode = overwriteMode;
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
        // we also do a check on makeSymlinkInMass just to allow backward compatibility for
        // a version or so, to allow users time to move away from symlinkmass option
        return makeSymlinks || makeSymlinksInMass;
    }

    /**
     * @return false
     * @deprecated Starting 2.5, creating symlinks in mass is no longer supported and this
     * method will always return false
     */
    @Deprecated
    public boolean isMakeSymlinksInMass() {
        return false;
    }

    public RetrieveOptions setMakeSymlinks(boolean makeSymlinks) {
        this.makeSymlinks = makeSymlinks;
        return this;
    }

    /**
     * @param makeSymlinksInMass ditto
     * @return RetrieveOptions
     * @deprecated Starting 2.5, creating symlinks in mass is no longer supported and this
     * method plays no role in creation of symlinks. Use {@link #setMakeSymlinks(boolean)} instead
     */
    @Deprecated
    public RetrieveOptions setMakeSymlinksInMass(boolean makeSymlinksInMass) {
        this.makeSymlinksInMass = makeSymlinksInMass;
        Message.warn("symlinkmass option has been deprecated and will no longer be supported");
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

    public FileNameMapper getMapper() {
        return mapper;
    }

    public RetrieveOptions setMapper(FileNameMapper mapper) {
        this.mapper = mapper;
        return this;
    }

}
