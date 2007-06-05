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
package org.apache.ivy.plugins.parser.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.util.FileUtil;
import org.xml.sax.SAXParseException;

public class XmlModuleUpdaterTest extends TestCase {

    public void testUpdate() throws Exception {
        /*
         * For updated file to be equals to updated.xml, we have to fix the line separator to the
         * one used in updated.xml, in order for this test to works in all platforms (default line
         * separator used in updater being platform dependent
         */
        XmlModuleDescriptorUpdater.LINE_SEPARATOR = "\n";
        File dest = new File("build/updated-test.xml");
        dest.deleteOnExit();
        Map resolvedRevisions = new HashMap();
        resolvedRevisions.put(ModuleRevisionId.newInstance("yourorg", "yourmodule2", "2+"), "2.5");
        resolvedRevisions.put(ModuleRevisionId.newInstance("yourorg", "yourmodule6",
            "latest.integration"), "6.3");

        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2005, 2, 22, 14, 32, 54);

        Ivy ivy = Ivy.newInstance();
        ivy.setVariable("myvar", "myconf1");
        XmlModuleDescriptorUpdater.update(ivy.getSettings(), XmlModuleUpdaterTest.class
                .getResource("test-update.xml"), dest, resolvedRevisions, "release", "mynewrev",
            cal.getTime(), null, true, null);

        assertTrue(dest.exists());
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                XmlModuleUpdaterTest.class.getResourceAsStream("updated.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
    }

    public void testUpdateWithImportedMappingOverride() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                .getResourceAsStream("test-configurations-import4.xml"), buffer, new HashMap(),
            "release", "mynewrev", new Date(), null, true);

        String updatedXml = buffer.toString();

        // just make sure that 'confmappingoverride="true"' is declared somewhere in the XML.
        assertTrue("Updated XML doesn't define the confmappingoverride attribute", updatedXml
                .indexOf("confmappingoverride=\"true\"") != -1);
    }

    public void testUpdateWithExcludeConfigurations1() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                .getResourceAsStream("test-update-excludedconfs1.xml"), buffer, new HashMap(),
            "release", "mynewrev", new Date(), null, true, new String[] {"myconf2"});

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        // test the number of configurations
        Configuration[] configs = updatedMd.getConfigurations();
        assertNotNull("Configurations shouldn't be null", configs);
        assertEquals("Number of configurations incorrect", 3, configs.length);

        // test that the correct configuration has been removed
        assertNull("myconf2 hasn't been removed", updatedMd.getConfiguration("myconf2"));

        // test that the other configurations aren't removed
        assertNotNull("myconf1 has been removed", updatedMd.getConfiguration("myconf1"));
        assertNotNull("myconf3 has been removed", updatedMd.getConfiguration("myconf3"));
        assertNotNull("myconf4 has been removed", updatedMd.getConfiguration("myconf4"));
    }

    public void testUpdateWithExcludeConfigurations2() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                    .getResourceAsStream("test-update-excludedconfs2.xml"), buffer, new HashMap(),
                "release", "mynewrev", new Date(), null, true, new String[] {"myconf2"});
            fail("IllegalArgumentException hasn't been thrown");
        } catch (IllegalArgumentException e) {
            // this is ok
        } catch (SAXParseException e) {
            // this is ok too
        }
    }

    public void testUpdateWithExcludeConfigurations3() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                .getResourceAsStream("test-update-excludedconfs3.xml"), buffer, new HashMap(),
            "release", "mynewrev", new Date(), null, true, new String[] {"myconf2", "conf2"});

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        // test the number of configurations
        Configuration[] configs = updatedMd.getConfigurations();
        assertNotNull("Configurations shouldn't be null", configs);
        assertEquals("Number of configurations incorrect", 4, configs.length);

        // test that the correct configuration has been removed
        assertNull("myconf2 hasn't been removed", updatedMd.getConfiguration("myconf2"));
        assertNull("conf2 hasn't been removed", updatedMd.getConfiguration("conf2"));

        // test that the other configurations aren't removed
        assertNotNull("conf1 has been removed", updatedMd.getConfiguration("conf1"));
        assertNotNull("myconf1 has been removed", updatedMd.getConfiguration("myconf1"));
        assertNotNull("myconf3 has been removed", updatedMd.getConfiguration("myconf3"));
        assertNotNull("myconf4 has been removed", updatedMd.getConfiguration("myconf4"));
    }

    public void testUpdateWithExcludeConfigurations4() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                .getResourceAsStream("test-update-excludedconfs4.xml"), buffer, new HashMap(),
            "release", "mynewrev", new Date(), null, true, new String[] {"myconf2"});

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        // test the number of configurations
        Artifact[] artifacts = updatedMd.getAllArtifacts();
        assertNotNull("Published artifacts shouldn't be null", artifacts);
        assertEquals("Number of published artifacts incorrect", 4, artifacts.length);

        // test that the correct configuration has been removed
        for (int i = 0; i < artifacts.length; i++) {
            Artifact current = artifacts[i];
            List currentConfs = Arrays.asList(current.getConfigurations());
            assertTrue("myconf2 hasn't been removed for artifact " + current.getName(),
                !currentConfs.contains("myconf2"));
        }
    }

    public void testUpdateWithExcludeConfigurations5() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(new IvySettings(), XmlModuleUpdaterTest.class
                .getResourceAsStream("test-update-excludedconfs5.xml"), buffer, new HashMap(),
            "release", "mynewrev", new Date(), null, true, new String[] {"myconf2"});

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        DependencyDescriptor[] deps = updatedMd.getDependencies();
        assertNotNull("Dependencies shouldn't be null", deps);
        assertEquals("Number of dependencies is incorrect", 8, deps.length);

        // check that none of the dependencies contains myconf2
        for (int i = 0; i < deps.length; i++) {
            String name = deps[i].getDependencyId().getName();
            assertFalse("Dependency " + name + " shouldn't have myconf2 as module configuration",
                Arrays.asList(deps[i].getModuleConfigurations()).contains("myconf2"));
            assertEquals("Dependency " + name
                    + " shouldn't have a dependency artifact for configuration myconf2", 0, deps[i]
                    .getDependencyArtifacts("myconf2").length);
        }
    }
}
