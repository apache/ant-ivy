/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

public final class ExactMatcher implements Matcher {
    private static final ExactMatcher INSTANCE = new ExactMatcher();
    public static Matcher getInstance() {
        return INSTANCE;
    }
    
    private ExactMatcher() {        
    }
    
    public boolean match(String str, String exp) {
        return str == null ? exp == null : str.equals(exp);
    }

    public String getName() {
        return EXACT;
    }
}
