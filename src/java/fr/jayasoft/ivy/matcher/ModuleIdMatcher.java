/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

import fr.jayasoft.ivy.ModuleId;

public class ModuleIdMatcher {
    private Matcher _orgMatcher;
    private Matcher _moduleMatcher;
    private ModuleId _mid;
    private PatternMatcher _pm;
    
    public ModuleIdMatcher(ModuleId mid, PatternMatcher pm) {
        _mid = mid;
        _pm = pm;
        _orgMatcher = pm.getMatcher(mid.getOrganisation());
        _moduleMatcher = pm.getMatcher(mid.getName());
    }
    
    public boolean matches(ModuleId mid) {
        return _orgMatcher.matches(mid.getOrganisation()) && _moduleMatcher.matches(mid.getName());
    }
    
    public String toString() {
        return _mid+" ("+_pm.getName()+")";
    }
}
