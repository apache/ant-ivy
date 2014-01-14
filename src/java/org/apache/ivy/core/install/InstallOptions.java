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
package org.apache.ivy.core.install;

import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

public class InstallOptions {
    private boolean transitive = true;

    private boolean validate = true;

    private boolean overwrite = false;

    private boolean installOriginalMetadata = false;

    private String[] confs = {"*"};

    private Filter artifactFilter = FilterHelper.NO_FILTER;

    private String matcherName = PatternMatcher.EXACT;

    public boolean isTransitive() {
        return transitive;
    }

    public InstallOptions setTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    public boolean isValidate() {
        return validate;
    }

    public InstallOptions setValidate(boolean validate) {
        this.validate = validate;
        return this;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public InstallOptions setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public Filter getArtifactFilter() {
        return artifactFilter;
    }

    public InstallOptions setArtifactFilter(Filter artifactFilter) {
        this.artifactFilter = artifactFilter == null ? FilterHelper.NO_FILTER : artifactFilter;
        return this;
    }

    public String getMatcherName() {
        return matcherName;
    }

    public InstallOptions setMatcherName(String matcherName) {
        this.matcherName = matcherName;
        return this;
    }

    public String[] getConfs() {
        return confs;
    }

    public InstallOptions setConfs(String[] conf) {
        this.confs = conf;
        return this;
    }

    public boolean isInstallOriginalMetadata() {
        return installOriginalMetadata;
    }

    public InstallOptions setInstallOriginalMetadata(boolean installOriginalMetadata) {
        this.installOriginalMetadata = installOriginalMetadata;
        return this;
    }
}
