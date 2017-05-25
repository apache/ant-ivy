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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import junit.framework.TestCase;

public class XmlModuleUpdaterTest extends TestCase {

    protected void tearDown() throws Exception {
        super.tearDown();

        XmlModuleDescriptorUpdater.LINE_SEPARATOR = System.getProperty("line.separator");
    }

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
        resolvedRevisions.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule2", "branch1", "2+"), "2.5");
        resolvedRevisions.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule6", "trunk", "latest.integration"),
            "6.3");

        Map resolvedBranches = new HashMap();
        resolvedBranches.put(ModuleRevisionId.newInstance("yourorg", "yourmodule3", "3.1"),
            "branch1");
        resolvedBranches.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule2", "branch1", "2+"), null);

        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2005, 2, 22, 14, 32, 54);

        Ivy ivy = Ivy.newInstance();
        ivy.setVariable("myvar", "myconf1");
        XmlModuleDescriptorUpdater.update(
            XmlModuleUpdaterTest.class.getResource("test-update.xml"),
            dest,
            getUpdateOptions(ivy.getSettings(), resolvedRevisions, "release", "mynewrev",
                cal.getTime()).setResolvedBranches(resolvedBranches));

        assertTrue(dest.exists());
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                XmlModuleUpdaterTest.class.getResourceAsStream("updated.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
    }

    public void testUpdateWithComments() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-with-comments.xml").toURI().toURL();
        XmlModuleDescriptorUpdater.update(settingsUrl, new BufferedOutputStream(buffer, 1024),
            getUpdateOptions("release", "mynewrev"));

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        DependencyDescriptor[] dependencies = updatedMd.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.length);
    }

    public void testVariableReplacement() throws Exception {
        /*
         * For updated file to be equals to updated.xml, we have to fix the line separator to the
         * one used in updated.xml, in order for this test to works in all platforms (default line
         * separator used in updater being platform dependent
         */
        XmlModuleDescriptorUpdater.LINE_SEPARATOR = "\n";
        File dest = new File("build/updated-test2.xml");
        dest.deleteOnExit();
        Map resolvedRevisions = new HashMap();
        resolvedRevisions.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule2", "branch1", "2+"), "2.5");
        resolvedRevisions.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule6", "trunk", "latest.integration"),
            "6.3");

        Map resolvedBranches = new HashMap();
        resolvedBranches.put(ModuleRevisionId.newInstance("yourorg", "yourmodule3", "3.1"),
            "branch1");
        resolvedBranches.put(
            ModuleRevisionId.newInstance("yourorg", "yourmodule2", "branch1", "2+"), null);

        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2005, 2, 22, 14, 32, 54);

        Ivy ivy = Ivy.newInstance();
        ivy.setVariable("myorg", "myorg");
        ivy.setVariable("mymodule", "mymodule");
        ivy.setVariable("myrev", "myrev");
        ivy.setVariable("mystatus", "integration");
        ivy.setVariable("mypubdate", "20050322143254");
        ivy.setVariable("mylicense", "MyLicense");
        ivy.setVariable("mylicenseurl", "http://www.my.org/mymodule/mylicense.html");
        ivy.setVariable("myorgurl", "http://www.myorg.org/");
        ivy.setVariable("ivyrep", "ivyrep");
        ivy.setVariable("ivyrepurl", "http://www.jayasoft.fr/org/ivyrep/");
        ivy.setVariable("ivyreppattern", "[organisation]/[module]/ivy-[revision].xml");
        ivy.setVariable("ivys", "true");
        ivy.setVariable("artifacts", "false");
        ivy.setVariable("homepage", "http://www.my.org/mymodule/");
        ivy.setVariable("includefile", "imported-configurations-with-mapping.xml");
        ivy.setVariable("mydesc", "desc 1");
        ivy.setVariable("visibility", "public");
        ivy.setVariable("myvar", "myconf1");
        ivy.setVariable("deprecated", "20050115");
        ivy.setVariable("myartifact1", "myartifact1");
        ivy.setVariable("mytype", "jar");
        ivy.setVariable("mymodule2", "mymodule2");
        ivy.setVariable("mymodule2rev", "2.0");
        ivy.setVariable("changing", "true");
        ivy.setVariable("transitive", "false");
        ivy.setVariable("targetconf", "yourconf1");
        ivy.setVariable("art9-1", "yourartifact9-1");
        ivy.setVariable("conf3", "myconf3");
        ivy.setVariable("includename", "your.*");
        ivy.setVariable("includeext", "xml");
        ivy.setVariable("excludename", "toexclude");
        ivy.setVariable("excludemodule", "*servlet*");
        ivy.setVariable("excludematcher", "glob");
        ivy.setVariable("excludeorg", "acme");
        ivy.setVariable("excludeartifact", "test");
        ivy.setVariable("excludetype", "source");
        ivy.setVariable("yourorg", "yourorg");
        ivy.setVariable("yourmodule", ".*");
        ivy.setVariable("all", "all");
        ivy.setVariable("regexp", "regexp");
        ivy.setVariable("theirrev", "1.0, 1.1");

        XmlModuleDescriptorUpdater.update(
            XmlModuleUpdaterTest.class.getResource("test-update-withvar.xml"),
            dest,
            getUpdateOptions(ivy.getSettings(), resolvedRevisions, "release", "mynewrev",
                cal.getTime()).setResolvedBranches(resolvedBranches));

        assertTrue(dest.exists());
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                XmlModuleUpdaterTest.class.getResourceAsStream("updated.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
    }

    public void testUpdateWithImportedMappingOverride() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-configurations-import4.xml").toURI().toURL();
        XmlModuleDescriptorUpdater.update(settingsUrl, buffer,
            getUpdateOptions("release", "mynewrev"));

        String updatedXml = buffer.toString();

        // just make sure that 'confmappingoverride="true"' is declared somewhere in the XML.
        assertTrue("Updated XML doesn't define the confmappingoverride attribute",
            updatedXml.indexOf("confmappingoverride=\"true\"") != -1);
    }

    public void testUpdateWithExcludeConfigurations1() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-update-excludedconfs1.xml").toURI().toURL();
        XmlModuleDescriptorUpdater.update(settingsUrl, buffer,
            getUpdateOptions("release", "mynewrev").setConfsToExclude(new String[] {"myconf2"}));

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
        URL settingFile = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-update-excludedconfs2.xml").toURI().toURL();
        try {
            XmlModuleDescriptorUpdater
                    .update(settingFile, buffer, getUpdateOptions("release", "mynewrev")
                            .setConfsToExclude(new String[] {"myconf2"}));
            fail("IllegalArgumentException hasn't been thrown");
        } catch (IllegalArgumentException e) {
            // this is ok
        } catch (SAXParseException e) {
            // this is ok too
        }
    }

    public void testUpdateWithExcludeConfigurations3() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-update-excludedconfs3.xml").toURI().toURL();

        XmlModuleDescriptorUpdater.update(
            settingsUrl,
            buffer,
            getUpdateOptions("release", "mynewrev").setConfsToExclude(
                new String[] {"myconf2", "conf2"}));

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
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-update-excludedconfs4.xml").toURI().toURL();
        XmlModuleDescriptorUpdater.update(settingsUrl, buffer,
            getUpdateOptions("release", "mynewrev").setConfsToExclude(new String[] {"myconf2"}));

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
        URL settingsUrl = new File("test/java/org/apache/ivy/plugins/parser/xml/"
                + "test-update-excludedconfs5.xml").toURI().toURL();
        XmlModuleDescriptorUpdater.update(settingsUrl, buffer,
            getUpdateOptions("release", "mynewrev").setConfsToExclude(new String[] {"myconf2"}));

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
                    + " shouldn't have a dependency artifact for configuration myconf2", 0,
                deps[i].getDependencyArtifacts("myconf2").length);
        }
    }

    // IVY-1356
    public void testMergedUpdateWithExtendsAndExcludes() throws Exception {
        URL url = XmlModuleUpdaterTest.class.getResource("test-extends-dependencies-exclude.xml");

        XmlModuleDescriptorParser parser = XmlModuleDescriptorParser.getInstance();
        ModuleDescriptor md = parser.parseDescriptor(new IvySettings(), url, true);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlModuleDescriptorUpdater.update(url, buffer, getUpdateOptions("release", "mynewrev")
                .setMerge(true).setMergedDescriptor(md));

        ModuleDescriptor updatedMd = parser.parseDescriptor(new IvySettings(),
            new ByteArrayInputStream(buffer.toByteArray()), new BasicResource("test", false, 0, 0,
                    false), true);

        DependencyDescriptor[] deps = updatedMd.getDependencies();
        assertNotNull("Dependencies shouldn't be null", deps);
        assertEquals("Number of dependencies is incorrect", 2, deps.length);

        // test indentation
        String updatedXml = buffer.toString();
        System.out.println(updatedXml);
        assertTrue(updatedXml
                .indexOf(XmlModuleDescriptorUpdater.LINE_SEPARATOR
                        + "\t\t<dependency org=\"myorg\" name=\"mymodule1\" rev=\"1.0\" conf=\"default->default\"/>"
                        + XmlModuleDescriptorUpdater.LINE_SEPARATOR) != -1);
    }

    private UpdateOptions getUpdateOptions(String status, String revision) {
        return getUpdateOptions(new IvySettings(), new HashMap(), status, revision, new Date());
    }

    private UpdateOptions getUpdateOptions(IvySettings settings, Map resolvedRevisions,
            String status, String revision, Date pubdate) {
        return new UpdateOptions().setSettings(settings).setResolvedRevisions(resolvedRevisions)
                .setStatus(status).setRevision(revision).setPubdate(pubdate);
    }
}
