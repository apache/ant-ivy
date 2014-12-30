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
 * Implementation of an exact matcher.
 * <p/>
 * The matching will be performed against an expression being a string. It will only matches if both
 * strings are equal (per equals()) rule or if both strings are null.
 */
public/* @Immutable */final class ExactPatternMatcher extends AbstractPatternMatcher {

    public static final ExactPatternMatcher INSTANCE = new ExactPatternMatcher();

    public ExactPatternMatcher() {
        super(EXACT);
    }

    @Override
    protected Matcher newMatcher(String expression) {
        return new ExactMatcher(expression);
    }

    private static/* @Immutable */class ExactMatcher implements Matcher {
        private String expression;

        public ExactMatcher(String expression) {
            this.expression = expression;
        }

        public boolean matches(String input) {
            if (input == null) {
                throw new NullPointerException();
            }
            return input.equals(expression);
        }

        public boolean isExact() {
            return true;
        }
    }
}
