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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.util.VersionRange;
import org.junit.Test;


public class ManifestParserTest {

    @Test
    public void testParseManifest() throws Exception {
        BundleInfo bundleInfo;

        bundleInfo = ManifestParser.parseJarManifest(getClass().getClassLoader().getResourceAsStream(
                "com.acme.alpha-1.0.0.20080101.jar"));
        assertEquals("com.acme.alpha", bundleInfo.getSymbolicName());
        assertEquals("1.0.0", bundleInfo.getVersion().numbersAsString());
        assertEquals("20080101", bundleInfo.getVersion().qualifier());
        assertEquals("1.0.0.20080101", bundleInfo.getVersion().toString());
        assertEquals(2, bundleInfo.getRequires().size());
        Set<BundleRequirement> expectedRequires = new HashSet<BundleRequirement>();
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.bravo", new VersionRange("2.0.0"),
                null));
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.delta", new VersionRange("4.0.0"),
                null));
        assertEquals(expectedRequires, bundleInfo.getRequires());
        assertEquals(0, bundleInfo.getExports().size());
        assertEquals(2, bundleInfo.getImports().size());

        final String importsList = bundleInfo.getImports().toString();
        assertTrue(importsList.contains("com.acme.bravo"));
        assertTrue(importsList.contains("com.acme.delta"));

        bundleInfo = ManifestParser.parseJarManifest(getClass().getClassLoader().getResourceAsStream(
                "com.acme.bravo-2.0.0.20080202.jar"));
        assertNotNull(bundleInfo);
        assertEquals("com.acme.bravo", bundleInfo.getSymbolicName());
        assertEquals("2.0.0", bundleInfo.getVersion().numbersAsString());
        assertEquals("20080202", bundleInfo.getVersion().qualifier());
        assertEquals("2.0.0.20080202", bundleInfo.getVersion().toString());
        assertEquals(1, bundleInfo.getRequires().size());
        expectedRequires = new HashSet<BundleRequirement>();
        expectedRequires.add(new BundleRequirement(BundleInfo.BUNDLE_TYPE, "com.acme.charlie",
                new VersionRange("3.0.0"), null));
        assertEquals(1, bundleInfo.getExports().size());
        assertTrue(bundleInfo.getExports().toString().contains("com.acme.bravo"));
        assertEquals(1, bundleInfo.getImports().size());
        assertTrue(bundleInfo.getImports().toString().contains("com.acme.charlie"));
    }
}
