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
package fr.jayasoft.ivy.matcher;

import fr.jayasoft.ivy.ArtifactId;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;

public class MatcherHelper {
    public static boolean matches(PatternMatcher m, String exp, String str) {
        return m.getMatcher(exp).matches(str);
    }
    public static boolean matches(PatternMatcher m, ModuleId exp, ModuleId mid) {
        return matches(m, exp.getOrganisation(), mid.getOrganisation())
        && matches(m, exp.getName(), mid.getName());
    }
    
    public static boolean matches(PatternMatcher m, ModuleRevisionId exp, ModuleRevisionId mrid) {
        return matches(m, exp.getOrganisation(), mrid.getOrganisation())
            && matches(m, exp.getName(), mrid.getName())
            && matches(m, exp.getRevision(), mrid.getRevision());
    }
    public static boolean matches(PatternMatcher m, ArtifactId exp, ArtifactId aid) {
        return matches(m, exp.getModuleId().getOrganisation(), aid.getModuleId().getOrganisation())
            && matches(m, exp.getModuleId().getName(), aid.getModuleId().getName())
            && matches(m, exp.getName(), aid.getName())
            && matches(m, exp.getExt(), aid.getExt())
            && matches(m, exp.getType(), aid.getType())
            ;
    }
    
    public static boolean isExact(PatternMatcher m, ModuleRevisionId exp) {
        return isExact(m, exp.getOrganisation())
            && isExact(m, exp.getName())
            && isExact(m, exp.getRevision());
    }
    public static boolean isExact(PatternMatcher m, ModuleId exp) {
        return isExact(m, exp.getOrganisation())
            && isExact(m, exp.getName());
    }
    public static boolean isExact(PatternMatcher m, String exp) {
        return m.getMatcher(exp).isExact();
    }
}
