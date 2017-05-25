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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A pattern matcher matching input using regular expressions.
 * 
 * @see Pattern
 */
public final/* @Immutable */class RegexpPatternMatcher extends AbstractPatternMatcher {
    public static final RegexpPatternMatcher INSTANCE = new RegexpPatternMatcher();

    /*
     * NOTE: Regexp compiler does ~200K compilation/s - If necessary look into using ThreadLocal
     * Pattern to cut on useless object creation - If expression are reused over and over a LRU
     * cache could make sense
     */

    public RegexpPatternMatcher() {
        super(REGEXP);
    }

    @Override
    protected Matcher newMatcher(String expression) {
        return new RegexpMatcher(expression);
    }

    private static/* @Immutable */class RegexpMatcher implements Matcher {
        private Pattern pattern;

        private String expression;

        private Boolean exact;

        public RegexpMatcher(String expression) throws PatternSyntaxException {
            if (expression == null) {
                throw new NullPointerException();
            }
            this.expression = expression;
            pattern = Pattern.compile(expression);
        }

        public boolean matches(String input) {
            if (input == null) {
                throw new NullPointerException();
            }
            return pattern.matcher(input).matches();
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
                if (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch) && ('-' != ch)
                        && ('_' != ch)) {
                    result = Boolean.FALSE;
                    break;
                }
            }

            return result;
        }
    }
}
