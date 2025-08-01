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

import java.util.Collections;
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
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib/[type]/[artifact].[ext]"));
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib/([type]/)[artifact].[ext]"));
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib(/[type])/[artifact].[ext]"));
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib/(type-[type]/)[artifact].[ext]"));
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib(/type-[type])/[artifact].[ext]"));
        assertEquals("lib/", IvyPatternHelper.getTokenRoot("lib/([type]/)"));
        assertEquals("lib/lib (JDK 17)/", IvyPatternHelper.getTokenRoot("lib/lib (JDK 17)/[artifact].[ext]")); //IVY-1660
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInOrganisation() {
        String pattern = "[organisation]/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "../org", "module", "revision", "artifact", "type", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInOrganization() {
        String pattern = "[organization]/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "../org", "module", "revision", "artifact", "type", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInModule() {
        String pattern = "[module]/build/archives (x86)/[type]s/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "..\\module", "revision", "artifact", "type", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInRevision() {
        String pattern = "[type]s/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "module", "revision/..", "artifact", "type", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInArtifact() {
        String pattern = "[type]s/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact\\..", "type", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInType() {
        String pattern = "[type]s/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact", "ty/../pe", "ext", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInExt() {
        String pattern = "[type]s/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact", "type", "ex//..//t", "conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInConf() {
        String pattern = "[conf]/[artifact]-[revision].[ext]";
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact", "type", "ext", "co\\..\\nf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInModuleAttributes() {
        String pattern = "[foo]/[artifact]-[revision].[ext]";
        Map<String, String> a = new HashMap<String, String>() {{
            put("foo", "..");
        }};
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact", "type", "ext", "conf",
            a, Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalInArtifactAttributes() {
        String pattern = "[foo]/[artifact]-[revision].[ext]";
        Map<String, String> a = new HashMap<String, String>() {{
            put("foo", "a/../b");
        }};
        IvyPatternHelper.substitute(pattern, "org", "module", "revision", "artifact", "type", "ext", "conf",
            Collections.emptyMap(), a);
    }


    @Test
    public void ignoresPathTraversalInCoordinatesNotUsedInPatern() {
        String pattern = "abc";
        Map<String, String> a = new HashMap<String, String>() {{
            put("foo", "a/../b");
        }};
        assertEquals("abc",
            IvyPatternHelper.substitute(pattern, "../org", "../module", "../revision", "../artifact", "../type", "../ext", "../conf",
                a, a)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathTraversalWithoutExplicitDoubleDot() {
        String pattern = "root/[conf]/[artifact]-[revision].[ext]";
        // forms revision/../ext after substitution
        IvyPatternHelper.substitute(pattern, "org", "module", "revision/", "artifact", "type", "./ext", "conf");
    }


}
