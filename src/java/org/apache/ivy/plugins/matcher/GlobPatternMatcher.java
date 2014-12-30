/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.matcher;

import java.util.regex.PatternSyntaxException;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * A pattern matcher matching input using unix-like glob matcher expressions. Meta characters are:
 * <ul>
 * <li>* - Matches zero or more characters</li>
 * <li>? - Matches exactly one character.</li>
 * </ul>
 * <p/>
 * <b> Note that this matcher is available only with <a href="http://jakarta.apache.org/oro"Apache
 * Jakarta Oro 2.0.8</a> in your classpath.</b>
 * 
 * @see <a
 *      href="http://jakarta.apache.org/oro/api/org/apache/oro/text/GlobCompiler.html">GlobCompiler</a>
 */
public/* @Immutable */final class GlobPatternMatcher extends AbstractPatternMatcher {

    public static final GlobPatternMatcher INSTANCE = new GlobPatternMatcher();

    /*
     * NOTE: GlobCompiler does ~100K compilation/s - If necessary look into using ThreadLocal for
     * GlobCompiler/Perl5Matcher to cut on useless object creation - If expression are reused over
     * and over a LRU cache could make sense
     */

    public GlobPatternMatcher() {
        super(GLOB);
    }

    @Override
    protected Matcher newMatcher(String expression) {
        return new GlobMatcher(expression);
    }

    private static class GlobMatcher implements Matcher {
        private Pattern pattern;

        private String expression;

        private Boolean exact;

        public GlobMatcher(String expression) throws PatternSyntaxException {
            this.expression = expression;
            try {
                pattern = new GlobCompiler().compile(expression);
            } catch (MalformedPatternException e) {
                throw new PatternSyntaxException(e.getMessage(), expression, 0);
            }
        }

        public boolean matches(String input) {
            if (input == null) {
                throw new NullPointerException();
            }
            return new Perl5Matcher().matches(input, pattern);
        }

        public boolean isExact() {
            if (exact == null) {
                exact = calculateExact();
            }
            return exact.booleanValue();
        }

        private Boolean calculateExact() {
            Boolean result = Boolean.TRUE;

            char[] expressionChars = expression.toCharArray();
            for (int i = 0; i < expressionChars.length; i++) {
                char ch = expressionChars[i];
                if (ch == '*' || ch == '?' || ch == '[' || ch == ']') {
                    result = Boolean.FALSE;
                    break;
                }
            }

            return result;
        }
    }

}
