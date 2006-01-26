/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;


public interface Matcher {
    public static final String EXACT = "exact";
    public static final String REGEXP = "regexp";
    public static final String EXACT_OR_REGEXP = "exactOrRegexp";
    
    public boolean match(String str, String exp);
    public String getName();
}
