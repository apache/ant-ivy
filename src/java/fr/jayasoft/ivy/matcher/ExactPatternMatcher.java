/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

public final class ExactPatternMatcher implements PatternMatcher {

    public static class ExactMatcher implements Matcher {
        protected String _exp;

        public ExactMatcher(String exp) {
            _exp = exp;
        }

        public boolean matches(String str) {
            return str == null ? _exp == null : str.equals(_exp);
        }

        public boolean isExact() {
            return true;
        }
    }

    private static final ExactPatternMatcher INSTANCE = new ExactPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private ExactPatternMatcher() {        
    }
    
    public String getName() {
        return EXACT;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new ExactMatcher(exp);
    }
}
