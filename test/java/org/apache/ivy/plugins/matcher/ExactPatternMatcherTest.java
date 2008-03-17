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
 * @see ExactPatternMatcher
 */
public class ExactPatternMatcherTest extends AbstractPatternMatcherTest {

    protected void setUp() throws Exception {
        setUp(new ExactPatternMatcher());
    }

    protected String[] getExactExpressions() {
        return new String[] {"abc", "123", "abc-123", "abc_123"};
    }

    protected String[] getInexactExpressions() {
        return new String[0]; // there are no inexact expressions possible
    }

    public void testImplementation() {
        Matcher matcher = patternMatcher.getMatcher(".");
        assertFalse(matcher.matches(""));
        assertTrue(matcher.matches("."));
        assertFalse(matcher.matches("a"));
        assertFalse(matcher.matches("aa"));
    }
}
