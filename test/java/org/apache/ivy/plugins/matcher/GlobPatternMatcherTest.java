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

/**
 * @see GlobPatternMatcher
 */
public class GlobPatternMatcherTest extends AbstractPatternMatcherTest {

    protected void setUp() throws Exception {
        setUp(new GlobPatternMatcher());
    }

    protected String[] getExactExpressions() {
        return new String[] {"abc", "123", "abc-123", "abc_123"};
    }

    protected String[] getInexactExpressions() {
        return new String[] {"abc*", "12?3", "abc[123]"};
    }

    public void testValidRegexpSyntaxAsNormalCharacter() {
        Matcher matcher = patternMatcher.getMatcher(".");
        assertTrue(matcher.isExact());
        assertFalse(matcher.matches(""));
        assertTrue(matcher.matches("."));
        assertFalse(matcher.matches("a"));
        assertFalse(matcher.matches("aa"));
    }

    public void testRegexpSyntaxAndGlob() {
        Matcher matcher = patternMatcher.getMatcher(".*");
        assertFalse(matcher.isExact());
        assertTrue(matcher.matches(".*"));
        assertFalse(matcher.matches(""));
        assertFalse(matcher.matches("a"));
        assertTrue(matcher.matches(".a"));
        assertFalse(matcher.matches("abcdef"));
        assertTrue(matcher.matches(".abcdef"));
    }

    public void testImplementation() {
        Matcher matcher = patternMatcher.getMatcher("abc-123_ABC");
        assertTrue(matcher.isExact());
    }

    public void testQuoteMeta() {
        Matcher matcher = patternMatcher.getMatcher("\\*");
        assertTrue(matcher.matches("*"));
        assertFalse(matcher.matches("X"));
        assertFalse(matcher.matches("Xsfsdfsd"));
    }

    public void testInvalidRegexpSyntaxAsNormalCharacter() {
        Matcher matcher = patternMatcher.getMatcher("(");
        assertTrue(matcher.matches("("));
    }

    public void testGlob() {
        Matcher matcher = patternMatcher.getMatcher("*ivy*");
        assertTrue(matcher.matches("ivy"));
        assertTrue(matcher.matches("abcdefivyuvw"));
        assertTrue(matcher.matches("ivyuvw"));
        assertTrue(matcher.matches("abcdefivy"));
    }

    public void testInvalidSyntax() {
        try {
            patternMatcher.getMatcher("[");
            fail("Should fail on invalid regexp syntax");
        } catch (PatternSyntaxException e) {

        }
    }
}
