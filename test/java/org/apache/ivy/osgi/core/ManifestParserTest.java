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
package org.apache.ivy.osgi.core;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ivy.osgi.util.VersionRange;

import junit.framework.TestCase;

public class ManifestParserTest extends TestCase {

    public void testParseManifest() throws Exception {
        BundleInfo bundleInfo;

        bundleInfo = ManifestParser.parseJarManifest(getClass().getResourceAsStream(
            "com.acme.alpha-1.0.0.20080101.jar"));
        assertEquals("com.acme.alpha", bundleInfo.getSymbolicName());
        assertEquals("20080101", bundleInfo.getVersion().qualifier());
        assertEquals("1.0.0.20080101", bundleInfo.getVersion().toString());
        assertEquals(2, bundleInfo.getRequires().size());
        Set<BundleRequirement> expectedRequires = new HashSet<BundleRequirement>();
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.bravo",
                new VersionRange("2.0.0"), null));
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.delta",
                new VersionRange("4.0.0"), null));
        assertEquals(expectedRequires, bundleInfo.getRequires());
        assertEquals(0, bundleInfo.getExports().size());
        assertEquals(2, bundleInfo.getImports().size());
        assertNull(bundleInfo.getClasspath());

        final String importsList = bundleInfo.getImports().toString();
        assertTrue(importsList.indexOf("com.acme.bravo") != -1);
        assertTrue(importsList.indexOf("com.acme.delta") != -1);

        bundleInfo = ManifestParser.parseJarManifest(getClass().getResourceAsStream(
            "com.acme.bravo-2.0.0.20080202.jar"));
        assertNotNull(bundleInfo);
        assertEquals("com.acme.bravo", bundleInfo.getSymbolicName());
        assertEquals("20080202", bundleInfo.getVersion().qualifier());
        assertEquals("2.0.0.20080202", bundleInfo.getVersion().toString());
        assertEquals(1, bundleInfo.getRequires().size());
        expectedRequires = new HashSet<BundleRequirement>();
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.charlie",
                new VersionRange("3.0.0"), null));
        assertEquals(1, bundleInfo.getExports().size());
        assertTrue(bundleInfo.getExports().toString().indexOf("com.acme.bravo") != -1);
        assertEquals(1, bundleInfo.getImports().size());
        assertTrue(bundleInfo.getImports().toString().indexOf("com.acme.charlie") != -1);
    }

    public void testClasspath() throws Exception {
        InputStream in = this.getClass().getResourceAsStream(
            "/org/apache/ivy/osgi/core/MANIFEST_classpath.MF");
        BundleInfo bundleInfo;
        try {
            bundleInfo = ManifestParser.parseManifest(in);
        } finally {
            in.close();
        }
        List<String> cp = bundleInfo.getClasspath();
        assertNotNull(cp);
        assertEquals(4, cp.size());
        assertEquals(
            Arrays.asList(new String[] {"lib/ant-antlr.jar", "lib/ant-apache-bcel.jar",
                    "lib/ant-apache-bsf.jar", "lib/ant-apache-log4j.jar"}), cp);

        in = this.getClass()
                .getResourceAsStream("/org/apache/ivy/osgi/core/MANIFEST_classpath2.MF");
        try {
            bundleInfo = ManifestParser.parseManifest(in);
        } finally {
            in.close();
        }
        cp = bundleInfo.getClasspath();
        assertNotNull(cp);
        assertEquals(1, cp.size());
        assertEquals(Arrays.asList(new String[] {"."}), cp);
    }

    public void testFormatLines() throws Exception {
        assertEquals("foo bar\n", ManifestParser.formatLines("foo bar"));
        assertEquals(
            "123456789012345678901234567890123456789012345678901234567890123456789012\n",
            ManifestParser
                    .formatLines("123456789012345678901234567890123456789012345678901234567890123456789012"));
        assertEquals(
            "123456789012345678901234567890123456789012345678901234567890123456789012\n 3\n",
            ManifestParser
                    .formatLines("1234567890123456789012345678901234567890123456789012345678901234567890123"));
        assertEquals("foo bar\n"
                + "123456789012345678901234567890123456789012345678901234567890123456789012\n"
                + " 12345678901234567890123456789012345678901234567890123456789012345678901\n"
                + " 21234\n" + "foo bar\n", ManifestParser.formatLines("foo bar\n"
                + "123456789012345678901234567890123456789012345678901234567890123456789012"
                + "123456789012345678901234567890123456789012345678901234567890123456789012"
                + "1234\n" + "foo bar\n"));
    }
}
