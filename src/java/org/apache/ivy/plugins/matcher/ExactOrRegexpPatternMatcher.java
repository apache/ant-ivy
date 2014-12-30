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

/**
 * A pattern matcher that tries to match exactly the input with the expression, or match it as a
 * pattern.
 * <p/>
 * The evaluation for matching is perform first by checking if expression and input are equals (via
 * equals method) else it attempts to do it by trying to match the input using the expression as a
 * regexp.
 * 
 * @see ExactPatternMatcher
 * @see RegexpPatternMatcher
 */
public/* @Immutable */final class ExactOrRegexpPatternMatcher extends AbstractPatternMatcher {

    public static final ExactOrRegexpPatternMatcher INSTANCE = new ExactOrRegexpPatternMatcher();

    public ExactOrRegexpPatternMatcher() {
        super(EXACT_OR_REGEXP);
    }

    @Override
    protected Matcher newMatcher(String expression) {
        return new ExactOrRegexpMatcher(expression);
    }

    private static final class ExactOrRegexpMatcher implements Matcher {
        private Matcher exact;

        private Matcher regexp;

        public ExactOrRegexpMatcher(String expression) {
            exact = ExactPatternMatcher.INSTANCE.getMatcher(expression);
            regexp = RegexpPatternMatcher.INSTANCE.getMatcher(expression);
        }

        public boolean matches(String input) {
            if (input == null) {
                throw new NullPointerException();
            }
            return exact.matches(input) || regexp.matches(input);
        }

        public boolean isExact() {
            return regexp.isExact(); // && exact.isExact();
        }
    }
}
