/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;


public final class ExactOrRegexpPatternMatcher implements PatternMatcher {
    public static class ExactOrRegexpMatcher implements Matcher {
        private Matcher _exact;
        private Matcher _regexp;

        public ExactOrRegexpMatcher(String exp) {
            _exact = ExactPatternMatcher.getInstance().getMatcher(exp);
            _regexp = RegexpPatternMatcher.getInstance().getMatcher(exp);
        }

        public boolean matches(String str) {
            return _exact.matches(str) || _regexp.matches(str);
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final ExactOrRegexpPatternMatcher INSTANCE = new ExactOrRegexpPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private ExactOrRegexpPatternMatcher() {        
    }
    
    public String getName() {
        return EXACT_OR_REGEXP;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new ExactOrRegexpMatcher(exp);
    }
}
