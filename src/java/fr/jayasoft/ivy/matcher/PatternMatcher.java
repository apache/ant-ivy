/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;


public interface PatternMatcher {
    public static final String EXACT = "exact";
    public static final String REGEXP = "regexp";
    public static final String GLOB = "glob";
    public static final String EXACT_OR_REGEXP = "exactOrRegexp";
    
    public static final String ANY_EXPRESSION = "*";

    public Matcher getMatcher(String exp);
    public String getName();
}
