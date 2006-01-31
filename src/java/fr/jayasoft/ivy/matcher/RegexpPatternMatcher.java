/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

import java.util.regex.Pattern;

public final class RegexpPatternMatcher implements PatternMatcher {
    public static class RegexpMatcher implements Matcher {
        private Pattern _p;

        public RegexpMatcher(String exp) {
            _p = Pattern.compile(exp);
        }

        public boolean matches(String str) {
            return _p.matcher(str).matches();
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final RegexpPatternMatcher INSTANCE = new RegexpPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private RegexpPatternMatcher() {        
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

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new RegexpMatcher(exp);
    }
}
