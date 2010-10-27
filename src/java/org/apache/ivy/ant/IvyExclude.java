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
package org.apache.ivy.ant;

import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;

public class IvyExclude {

    private String org;

    private String module;

    private String artifact;

    private String type;

    private String ext;

    private String matcher;

    public void setOrg(String org) {
        this.org = org;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    DefaultExcludeRule asRule(IvySettings settings) {
        String matcherName = matcher == null ? PatternMatcher.EXACT : matcher;
        String orgPattern = org == null ? PatternMatcher.ANY_EXPRESSION : org;
        String modulePattern = module == null ? PatternMatcher.ANY_EXPRESSION : module;
        String artifactPattern = artifact == null ? PatternMatcher.ANY_EXPRESSION : artifact;
        String typePattern = type == null ? PatternMatcher.ANY_EXPRESSION : type;
        String extPattern = ext == null ? typePattern : ext;
        ArtifactId aid = new ArtifactId(new ModuleId(orgPattern, modulePattern), artifactPattern,
                typePattern, extPattern);
        return new DefaultExcludeRule(aid, settings.getMatcher(matcherName), null);
    }
}
