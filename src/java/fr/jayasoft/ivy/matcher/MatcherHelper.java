/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;

public class MatcherHelper {
    public static boolean match(Matcher m, ModuleId mid, ModuleId exp) {
        return m.match(mid.getOrganisation(), exp.getOrganisation())
        && m.match(mid.getName(), exp.getName());
    }
    
    public static boolean match(Matcher m, ModuleRevisionId mrid, ModuleRevisionId exp) {
        return m.match(mrid.getOrganisation(), exp.getOrganisation())
        && m.match(mrid.getName(), exp.getName())
        && m.match(mrid.getRevision(), exp.getRevision());
    }
    
    public static boolean isExact(Matcher m) {
        return m.getClass() == ExactMatcher.class;
    }
}
