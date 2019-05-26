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
package org.apache.ivy.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.junit.Test;

public class IvyPatternHelperTest {
    @Test
    public void testSubstitute() {
        String pattern = "[organisation]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals("apache/Test/build/archives/jars/test-1.0.jar",
            IvyPatternHelper.substitute(pattern, "apache", "Test", "1.0", "test", "jar", "jar"));
    }

    @Test(expected = Exception.class)
    public void testCyclicSubstitute() {
        String pattern = "${var}";
        Map<String, String> variables = new HashMap<>();
        variables.put("var", "${othervar}");
        variables.put("othervar", "${var}");

        IvyPatternHelper.substituteVariables(pattern, variables);
    }

    @Test
    public void testOptionalSubstitute() {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("token", "");
        tokens.put("othertoken", "myval");
        assertEquals("test-myval",
            IvyPatternHelper.substituteTokens("test(-[token])(-[othertoken])", tokens));
        tokens.put("token", "val");
        assertEquals("test-val-myval",
            IvyPatternHelper.substituteTokens("test(-[token])(-[othertoken])", tokens));
    }

    @Test
    public void testOrganization() {
        String pattern = "[organization]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals("apache/Test/build/archives/jars/test-1.0.jar",
            IvyPatternHelper.substitute(pattern, "apache", "Test", "1.0", "test", "jar", "jar"));
    }

    @Test
    public void testSpecialCharsInsidePattern() {
        String pattern = "[organization]/[module]/build/archives (x86)/[type]s/[artifact]-[revision].[ext]";
        assertEquals("apache/Test/build/archives (x86)/jars/test-1.0.jar",
            IvyPatternHelper.substitute(pattern, "apache", "Test", "1.0", "test", "jar", "jar"));
    }

    @Test
    public void testTokenRoot() {
        String pattern = "lib/[type]/[artifact].[ext]";
        assertEquals("lib/", IvyPatternHelper.getTokenRoot(pattern));
    }

    @Test
    public void testTokenRootWithOptionalFirstToken() {
        String pattern = "lib/([type]/)[artifact].[ext]";
        assertEquals("lib/", IvyPatternHelper.getTokenRoot(pattern));
    }
}
