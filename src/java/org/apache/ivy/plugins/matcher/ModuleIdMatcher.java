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
package org.apache.ivy.plugins.matcher;

import org.apache.ivy.core.module.id.ModuleId;

public class ModuleIdMatcher {
    // TODO this class should be moved out of this package
    private Matcher orgMatcher;

    private Matcher moduleMatcher;

    private ModuleId mid;

    private PatternMatcher pm;

    public ModuleIdMatcher(ModuleId mid, PatternMatcher pm) {
        this.mid = mid;
        this.pm = pm;
        this.orgMatcher = pm.getMatcher(mid.getOrganisation() == null 
                ? PatternMatcher.ANY_EXPRESSION
                : mid.getOrganisation());
        this.moduleMatcher = pm.getMatcher(mid.getName());
    }

    public boolean matches(ModuleId mid) {
        return orgMatcher.matches(mid.getOrganisation()) && moduleMatcher.matches(mid.getName());
    }

    public String toString() {
        return mid + " (" + pm.getName() + ")";
    }
}
