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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParserTest;
import org.apache.ivy.util.FileUtil;
import org.custommonkey.xmlunit.XMLTestCase;

public class XmlModuleDescriptorWriterTest extends XMLTestCase {
    private static String LICENSE;
    static {
        try {
            LICENSE = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                    XmlModuleDescriptorWriterTest.class.getResourceAsStream("license.xml"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File dest = new File("build/test/test-write.xml");

    public void testSimple() throws Exception {
        DefaultModuleDescriptor md = (DefaultModuleDescriptor) XmlModuleDescriptorParser
                .getInstance().parseDescriptor(new IvySettings(),
                    XmlModuleDescriptorWriterTest.class.getResource("test-simple.xml"), true);
        md.setResolvedPublicationDate(new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime());
        md.setResolvedModuleRevisionId(new ModuleRevisionId(md.getModuleRevisionId().getModuleId(),
                "NONE"));
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-simple.xml").replaceAll("\r\n", "\n").replace(
            '\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testInfo() throws Exception {
        DefaultModuleDescriptor md = (DefaultModuleDescriptor) XmlModuleDescriptorParser
                .getInstance().parseDescriptor(new IvySettings(),
                    XmlModuleDescriptorWriterTest.class.getResource("test-info.xml"), true);
        md.setResolvedPublicationDate(new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime());
        md.setResolvedModuleRevisionId(new ModuleRevisionId(md.getModuleRevisionId().getModuleId(),
                "NONE"));
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-info.xml").replaceAll("\r\n", "\n").replace(
            '\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testDependencies() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            XmlModuleDescriptorWriterTest.class.getResource("test-dependencies.xml"), true);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-dependencies.xml").replaceAll("\r\n", "\n")
                .replace('\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testFull() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), XmlModuleDescriptorWriterTest.class.getResource("test.xml"), false);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-full.xml").replaceAll("\r\n", "\n").replace(
            '\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testExtraInfos() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            XmlModuleDescriptorWriterTest.class.getResource("test-extrainfo.xml"), false);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-extrainfo.xml").replaceAll("\r\n", "\n")
                .replace('\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testExtraInfosWithNestedElement() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            XmlModuleDescriptorWriterTest.class.getResource("test-extrainfo-nested.xml"), false);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        String expected = readEntirely("test-write-extrainfo-nested.xml").replaceAll("\r\n", "\n")
                .replace('\r', '\n');
        assertEquals(expected, wrote);
    }

    public void testExtraInfosFromMaven() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            PomModuleDescriptorParserTest.class
                    .getResource("test-versionPropertyDependencyMgt.pom"), false);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)))
                .replaceAll("\r\n", "\n").replace('\r', '\n');
        wrote = wrote.replaceFirst("publication=\"([0-9])*\"", "publication=\"20140429153143\"");
        System.out.println(wrote);

        String expected = readEntirely("test-write-extrainfo-from-maven.xml").replaceAll("\r\n",
            "\n").replace('\r', '\n');
        assertXMLEqual(expected, wrote);
    }

    public void testExtends() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            XmlModuleDescriptorWriterTest.class.getResource("test-extends-all.xml"), false);
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        assertTrue(dest.exists());
        String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(dest))).replaceAll(
            "\r\n?", "\n");
        String expected = readEntirely("test-write-extends.xml").replaceAll("\r\n?", "\n");
        assertEquals(expected, wrote);
    }

    /**
     * Test that the transitive attribute is written for non-transitive configurations.
     * 
     * <code><conf ... transitive="false" ... /></code>
     * 
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1207">IVY-1207</a>
     * @throws Exception
     */
    public void testTransitiveAttributeForNonTransitiveConfs() throws Exception {
        // Given a ModuleDescriptor with a non-transitive configuration
        DefaultModuleDescriptor md = new DefaultModuleDescriptor(new ModuleRevisionId(new ModuleId(
                "myorg", "myname"), "1.0"), "integration", new Date());
        Configuration conf = new Configuration("conf", Visibility.PUBLIC, "desc", null, false, null);
        md.addConfiguration(conf);

        // When the ModuleDescriptor is written
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        // Then the transitive attribute must be set to false
        String output = FileUtil.readEntirely(dest);
        String writtenConf = output.substring(output.indexOf("<configurations>") + 16,
            output.indexOf("</configurations>")).trim();
        assertTrue("Transitive attribute not set to false: " + writtenConf,
            writtenConf.indexOf("transitive=\"false\"") >= 0);
    }

    /**
     * Test that the transitive attribute is not written when the configuration IS transitive.
     * 
     * This is the default and writing it will only add noise and cause a deviation from the known
     * behavior (before fixing IVY-1207).
     * 
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1207">IVY-1207</a>
     * @throws Exception
     */
    public void testTransitiveAttributeNotWrittenForTransitiveConfs() throws Exception {
        // Given a ModuleDescriptor with a transitive configuration
        DefaultModuleDescriptor md = new DefaultModuleDescriptor(new ModuleRevisionId(new ModuleId(
                "myorg", "myname"), "1.0"), "integration", new Date());
        Configuration conf = new Configuration("conf", Visibility.PUBLIC, "desc", null, true, null);
        md.addConfiguration(conf);

        // When the ModuleDescriptor is written
        XmlModuleDescriptorWriter.write(md, LICENSE, dest);

        // Then the transitive attribute must NOT be written
        String output = FileUtil.readEntirely(dest);
        String writtenConf = output.substring(output.indexOf("<configurations>") + 16,
            output.indexOf("</configurations>")).trim();
        assertFalse("Transitive attribute set: " + writtenConf,
            writtenConf.indexOf("transitive=") >= 0);
    }

    private String readEntirely(String resource) throws IOException {
        return FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                XmlModuleDescriptorWriterTest.class.getResource(resource).openStream())));
    }

    public void setUp() {
        if (dest.exists()) {
            dest.delete();
        }
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
    }

    protected void tearDown() throws Exception {
        if (dest.exists()) {
            dest.delete();
        }
    }
}
