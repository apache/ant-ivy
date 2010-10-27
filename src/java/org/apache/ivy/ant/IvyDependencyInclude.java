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

import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;

public class IvyDependencyInclude {

    private String name;

    private String type;

    private String ext;

    private String matcher;

    public void setName(String name) {
        this.name = name;
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

    DefaultIncludeRule asRule(IvySettings settings) {
        String matcherName = matcher == null ? PatternMatcher.EXACT : matcher;
        String namePattern = name == null ? PatternMatcher.ANY_EXPRESSION : name;
        String typePattern = type == null ? PatternMatcher.ANY_EXPRESSION : type;
        String extPattern = ext == null ? typePattern : ext;
        ArtifactId aid = new ArtifactId(new ModuleId(PatternMatcher.ANY_EXPRESSION,
                PatternMatcher.ANY_EXPRESSION), namePattern, typePattern, extPattern);
        return new DefaultIncludeRule(aid, settings.getMatcher(matcherName), null);
    }

}
