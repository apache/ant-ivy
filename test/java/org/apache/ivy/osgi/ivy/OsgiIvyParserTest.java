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
package org.apache.ivy.osgi.ivy;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;

public class OsgiIvyParserTest extends TestCase {

    public void testSimple() throws Exception {
        IvySettings settings = new IvySettings();
        settings.load(new File("java/test-ivy/include/ivysettings.xml"));

        URLResource includingResource = new URLResource(
                new File("java/test-ivy/include/ivy.xml").toURL());
        ModuleDescriptorParser includingParser = ModuleDescriptorParserRegistry.getInstance()
                .getParser(includingResource);
        assertTrue(includingParser instanceof OsgiIvyParser);
        ModuleDescriptor includingMd = includingParser.parseDescriptor(settings,
            includingResource.getURL(), false);

        assertNotNull(includingMd);

        URLResource resultResource = new URLResource(new File(
                "java/test-ivy/include/ivy-result.xml").toURL());
        ModuleDescriptorParser resultParser = ModuleDescriptorParserRegistry.getInstance()
                .getParser(resultResource);
        ModuleDescriptor resultMd = resultParser.parseDescriptor(settings, resultResource.getURL(),
            false);

        assertEquals(resultMd.getModuleRevisionId(), includingMd.getModuleRevisionId());
        assertEquals(resultMd.getResolvedModuleRevisionId(),
            includingMd.getResolvedModuleRevisionId());
        assertEquals(resultMd.getDescription(), includingMd.getDescription());
        assertEquals(resultMd.getHomePage(), includingMd.getHomePage());
        // assertEquals(resultMd.getLastModified(), includingMd.getLastModified());
        assertEquals(resultMd.getStatus(), includingMd.getStatus());
        assertEquals(resultMd.getExtraInfo(), includingMd.getExtraInfo());
        assertArrayEquals((Object[]) resultMd.getLicenses(), (Object[]) includingMd.getLicenses());
        assertArrayEquals(resultMd.getConfigurations(), includingMd.getConfigurations());
        assertArrayEquals((Object[]) resultMd.getAllArtifacts(),
            (Object[]) includingMd.getAllArtifacts());
        assertEquals(resultMd.getDependencies().length, includingMd.getDependencies().length);
        for (int i = 0; i < resultMd.getDependencies().length; i++) {
            assertEquals(resultMd.getDependencies()[i].getDependencyRevisionId(),
                includingMd.getDependencies()[i].getDependencyRevisionId());
            assertArrayEquals((Object[]) resultMd.getDependencies()[i].getModuleConfigurations(),
                (Object[]) includingMd.getDependencies()[i].getModuleConfigurations());
        }
    }

    private static/* <T1,T2> */void assertArrayEquals(Object/* T1 */[] expected,
            Object/* T2 */[] actual) {
        assertSetEquals(Arrays.asList(expected), Arrays.asList(actual));
    }

    private static/* <T1,T2> */void assertSetEquals(List/* <T1> */expected, List/* <T2> */actual) {
        assertEquals(new HashSet/* <T1> */(expected), new HashSet/* <T2> */(actual));
    }

}
