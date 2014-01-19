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

import junit.framework.TestCase;

/**
 * Base test classes for PatternMatcher testcase implementation
 */
public abstract class AbstractPatternMatcherTest extends TestCase {
    protected PatternMatcher patternMatcher;

    protected abstract void setUp() throws Exception;

    protected void setUp(PatternMatcher matcher) {
        this.patternMatcher = matcher;
    }

    public void testAnyExpression() {
        Matcher matcher = patternMatcher.getMatcher("*");
        assertTrue(matcher.matches(""));
        assertTrue(matcher.matches("We shall transcend borders. The new is old."));
        assertTrue(matcher.matches("        "));
    }

    public void testIsExact() {
        // '*' is a special matcher
        Matcher matcher = patternMatcher.getMatcher("*");
        assertEquals(false, matcher.isExact());
        matcher.matches("The words aren't what they were.");
        assertEquals(false, matcher.isExact());

        // test some exact patterns for this matcher
        String[] expressions = getExactExpressions();
        for (int i = 0; i < expressions.length; i++) {
            matcher = patternMatcher.getMatcher(expressions[i]);
            assertTrue("Expression '" + expressions[i] + "' should be exact", matcher.isExact());
            matcher.matches("The words aren't what they were.");
            assertTrue("Expression '" + expressions[i] + "' should be exact", matcher.isExact());
        }

        // test some inexact patterns for this matcher
        expressions = getInexactExpressions();
        for (int i = 0; i < expressions.length; i++) {
            matcher = patternMatcher.getMatcher(expressions[i]);
            assertFalse("Expression '" + expressions[i] + "' should be inexact", matcher.isExact());
            matcher.matches("The words aren't what they were.");
            assertFalse("Expression '" + expressions[i] + "' should be inexact", matcher.isExact());
        }

    }

    protected abstract String[] getExactExpressions();

    protected abstract String[] getInexactExpressions();

    public void testNullInput() {
        Matcher matcher = patternMatcher.getMatcher("some expression");
        try {
            matcher.matches(null);
            fail("Should fail for null input");
        } catch (NullPointerException expected) {

        }
    }

    public void testNullExpression() {
        try {
            patternMatcher.getMatcher(null);
            fail("Should fail for null expression");
        } catch (NullPointerException expected) {

        }
    }

    public abstract void testImplementation();

    public void testLoadTestMatches() {
        Matcher matcher = patternMatcher.getMatcher("this.is.an.expression");
        String[] inputs = {"this.is.an.expression", "this:is:an:expression",
                "this is an expression", "whatever this is", "maybe, maybe not"};
        for (int i = 0; i < 100000; i++) {
            String input = inputs[i % inputs.length];
            matcher.matches(input);
        }
    }

    public void testLoadTestGetMatcher() {
        String[] inputs = {"this.is.an.expression", "this:is:an:expression",
                "this is an expression", "whatever this is", "maybe, maybe not"};

        for (int i = 0; i < 100000; i++) {
            String expression = inputs[i % inputs.length];
            patternMatcher.getMatcher(expression);
        }
    }
}
