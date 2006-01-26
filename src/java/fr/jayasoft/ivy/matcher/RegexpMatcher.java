/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

import java.util.regex.Pattern;

public final class RegexpMatcher implements Matcher {
    private static final RegexpMatcher INSTANCE = new RegexpMatcher();
    public static Matcher getInstance() {
        return INSTANCE;
    }
    
    private RegexpMatcher() {        
    }
    
    public boolean match(String str, String exp) {
        if (exp == null) {
            return str == null;
        }
        return Pattern.matches(exp, str);
    }

    public String getName() {
        return REGEXP;
    }
}
