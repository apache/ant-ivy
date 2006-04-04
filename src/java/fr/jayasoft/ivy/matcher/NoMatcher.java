/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

public class NoMatcher implements Matcher {
    private final static Matcher INSTANCE = new NoMatcher();
    
    public static Matcher getInstance() {
        return INSTANCE;
    }
    
    private NoMatcher() {
    }

    public boolean matches(String str) {
        return false;
    }

    public boolean isExact() {
        return true;
    }

}
