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

import java.util.regex.PatternSyntaxException;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ExactOrRegexpPatternMatcherTest extends AbstractPatternMatcherTest {

    @Before
    public void setUp() {
        setUp(new ExactOrRegexpPatternMatcher());
    }

    protected String[] getExactExpressions() {
        return new String[] {"abc", "123", "abc-123", "abc_123"};
    }

    protected String[] getInexactExpressions() {
        return new String[] {"abc+", "12.3", "abc-123*", "abc_123\\d"};
    }

    @Test(expected = PatternSyntaxException.class)
    public void testImplementation() {
        Matcher matcher = patternMatcher.getMatcher(".");
        assertFalse(matcher.isExact());
        assertFalse(matcher.matches(""));
        assertTrue("Exact match failed", matcher.matches("."));
        assertTrue("Regexp match failed", matcher.matches("a"));
        assertFalse(matcher.matches("aa"));

        matcher = patternMatcher.getMatcher(".*");
        assertFalse(matcher.isExact());
        assertTrue("Exact match failed", matcher.matches(".*"));
        assertTrue("Regexp match failed", matcher.matches(""));
        assertTrue(matcher.matches("a"));
        assertTrue(matcher.matches("aa"));

        matcher = patternMatcher.getMatcher("abc-123_ABC");
        assertTrue(matcher.isExact());

        matcher = patternMatcher.getMatcher("(");
    }
}
