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

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.FixedConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;

public class IvyConflict {

    private String org;

    private String module;

    private String manager;

    private String rev;

    private String matcher;

    public void setOrg(String org) {
        this.org = org;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    void addConflict(DefaultModuleDescriptor md, IvySettings settings) {
        String matcherName = matcher == null ? PatternMatcher.EXACT : matcher;
        String orgPattern = org == null ? PatternMatcher.ANY_EXPRESSION : org;
        String modulePattern = module == null ? PatternMatcher.ANY_EXPRESSION : module;
        ConflictManager cm = null;
        if (rev != null) {
            String[] revs = rev.split(",");
            for (int i = 0; i < revs.length; i++) {
                revs[i] = revs[i].trim();
            }
            cm = new FixedConflictManager(revs);
        } else if (manager != null) {
            cm = settings.getConflictManager(manager);
        }
        md.addConflictManager(new ModuleId(orgPattern, modulePattern),
            settings.getMatcher(matcherName), cm);
    }

}
