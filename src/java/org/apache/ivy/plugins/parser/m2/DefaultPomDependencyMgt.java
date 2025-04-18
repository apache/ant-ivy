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
package org.apache.ivy.plugins.parser.m2;

import org.apache.ivy.core.module.id.ModuleId;

import java.util.List;

public class DefaultPomDependencyMgt implements PomDependencyMgt {
    private String groupId;

    private String artifactId;

    private String type;

    private String classifier;

    private String version;

    private String scope;

    private List<ModuleId> excludedModules;

    public DefaultPomDependencyMgt(String groupId, String artifactId, String type, String classifier, String version, String scope,
            List<ModuleId> excludedModules) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.classifier = classifier;
        this.version = version;
        this.scope = scope;
        this.excludedModules = excludedModules;
    }

    public String getScope() {
        return scope;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public String getVersion() {
        return version;
    }

    public List<ModuleId> getExcludedModules() {
        return excludedModules;
    }
}
