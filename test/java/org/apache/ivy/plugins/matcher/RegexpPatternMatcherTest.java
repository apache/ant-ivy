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
 * @see RegexpPatternMatcher
 */
public class RegexpPatternMatcherTest extends AbstractPatternMatcherTest {

    protected void setUp() throws Exception {
        setUp(new RegexpPatternMatcher());
    }

    protected String[] getExactExpressions() {
        return new String[] {"abc", "123", "abc-123", "abc_123"};
    }

    protected String[] getInexactExpressions() {
        return new String[] {"abc+", "12.3", "abc-123*", "abc_123\\d"};
    }

    public void testImplementation() {
        Matcher matcher = patternMatcher.getMatcher(".*");
        assertTrue(matcher.matches(".*"));
        assertTrue(matcher.matches(""));
        assertTrue(matcher.matches("a"));
        assertTrue(matcher.matches("aa"));

        try {
            matcher = patternMatcher.getMatcher("(");
            fail("Should fail on invalid syntax");
        } catch (PatternSyntaxException e) {

        }
    }
}
