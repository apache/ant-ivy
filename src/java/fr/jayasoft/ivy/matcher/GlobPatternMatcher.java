/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.matcher;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import fr.jayasoft.ivy.util.Message;


public final class GlobPatternMatcher implements PatternMatcher {
    public static class GlobMatcher implements Matcher {
        private Pattern _p;

        public GlobMatcher(String exp) {
            try {
                _p = new GlobCompiler().compile(exp);
            } catch (MalformedPatternException e) {
                Message.error("impossible to compile glob pattern: "+exp);
            }
        }

        public boolean matches(String str) {
            return _p != null && new Perl5Matcher().matches(str, _p);
        }

        public boolean isExact() {
            return false;
        }
    }
    private static final GlobPatternMatcher INSTANCE = new GlobPatternMatcher();
    public static PatternMatcher getInstance() {
        return INSTANCE;
    }
    
    private GlobPatternMatcher() {        
    }

    public String getName() {
        return GLOB;
    }

    public Matcher getMatcher(String exp) {
        if (ANY_EXPRESSION.equals(exp)) {
            return AnyMatcher.getInstance();
        }
        return new GlobMatcher(exp);
    }
}
