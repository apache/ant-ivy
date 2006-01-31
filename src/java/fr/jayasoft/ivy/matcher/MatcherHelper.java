/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
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
