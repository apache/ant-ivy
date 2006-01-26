/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;


public final class ExactOrRegexpMatcher implements Matcher {
    private static final ExactOrRegexpMatcher INSTANCE = new ExactOrRegexpMatcher();
    public static Matcher getInstance() {
        return INSTANCE;
    }
    
    private ExactOrRegexpMatcher() {        
    }
    
    public boolean match(String str, String exp) {
        return ExactMatcher.getInstance().match(str, exp) 
            || RegexpMatcher.getInstance().match(str, exp); 
    }

    public String getName() {
        return EXACT_OR_REGEXP;
    }
}
