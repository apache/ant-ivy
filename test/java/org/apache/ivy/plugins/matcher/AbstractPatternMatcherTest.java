/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Base test classes for PatternMatcher testcase implementation
 */
public abstract class AbstractPatternMatcherTest {
    protected PatternMatcher patternMatcher;

    // used by setUp() in subclasses
    protected void setUp(PatternMatcher matcher) {
        this.patternMatcher = matcher;
    }

    @Test
    public void testAnyExpression() {
        Matcher matcher = patternMatcher.getMatcher("*");
        assertTrue(matcher.matches(""));
        assertTrue(matcher.matches("We shall transcend borders. The new is old."));
        assertTrue(matcher.matches("        "));
    }

    @Test
    public void testIsExact() {
        // '*' is a special matcher
        Matcher matcher = patternMatcher.getMatcher("*");
        assertFalse(matcher.isExact());
        matcher.matches("The words aren't what they were.");
        assertFalse(matcher.isExact());

        // test some exact patterns for this matcher
        String[] expressions = getExactExpressions();
        for (String expression : expressions) {
            matcher = patternMatcher.getMatcher(expression);
            assertTrue("Expression '" + expression + "' should be exact", matcher.isExact());
            matcher.matches("The words aren't what they were.");
            assertTrue("Expression '" + expression + "' should be exact", matcher.isExact());
        }

        // test some inexact patterns for this matcher
        expressions = getInexactExpressions();
        for (String expression : expressions) {
            matcher = patternMatcher.getMatcher(expression);
            assertFalse("Expression '" + expression + "' should be inexact", matcher.isExact());
            matcher.matches("The words aren't what they were.");
            assertFalse("Expression '" + expression + "' should be inexact", matcher.isExact());
        }

    }

    protected abstract String[] getExactExpressions();

    protected abstract String[] getInexactExpressions();

    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        Matcher matcher = patternMatcher.getMatcher("some expression");
        matcher.matches(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullExpression() {
        patternMatcher.getMatcher(null);
    }

    public abstract void testImplementation();

    @Test
    public void testLoadTestMatches() {
        Matcher matcher = patternMatcher.getMatcher("this.is.an.expression");
        String[] inputs = {"this.is.an.expression", "this:is:an:expression",
                "this is an expression", "whatever this is", "maybe, maybe not"};
        for (int i = 0; i < 100000; i++) {
            String input = inputs[i % inputs.length];
            matcher.matches(input);
        }
    }

    @Test
    public void testLoadTestGetMatcher() {
        String[] inputs = {"this.is.an.expression", "this:is:an:expression",
                "this is an expression", "whatever this is", "maybe, maybe not"};

        for (int i = 0; i < 100000; i++) {
            String expression = inputs[i % inputs.length];
            patternMatcher.getMatcher(expression);
        }
    }
}
