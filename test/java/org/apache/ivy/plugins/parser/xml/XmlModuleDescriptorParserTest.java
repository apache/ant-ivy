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
package org.apache.ivy.plugins.parser.xml;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.ExtraInfoHolder;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.FixedConflictManager;
import org.apache.ivy.plugins.conflict.NoConflictManager;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.GlobPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PRIVATE;
import static org.apache.ivy.core.module.descriptor.Configuration.Visibility.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XmlModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    private IvySettings settings = null;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_WARN));

        this.settings = new IvySettings();
        // prevent test from polluting local cache
        settings.setDefaultCache(new File("build/cache"));
    }

    @Test
    public void testSimple() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-simple.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        assertNotNull(md.getConfigurations());
        assertEquals(Collections.singletonList(new Configuration("default")),
            Arrays.asList(md.getConfigurations()));

        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);
        assertEquals("mymodule", md.getArtifacts("default")[0].getName());
        assertEquals("jar", md.getArtifacts("default")[0].getType());

        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }

    @Test
    public void testNamespaces() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-namespaces.xml"), true);
        assertNotNull(md);
        ModuleRevisionId mrid = md.getModuleRevisionId();
        assertEquals("myorg", mrid.getOrganisation());
        assertEquals("mymodule", mrid.getName());
        assertEquals("myval", mrid.getExtraAttribute("e:myextra"));
        assertEquals(Collections.singletonMap("e:myextra", "myval"),
            mrid.getQualifiedExtraAttributes());
        assertEquals("myval", mrid.getExtraAttribute("myextra"));
        assertEquals(Collections.singletonMap("myextra", "myval"), mrid.getExtraAttributes());
        assertEquals("http://ant.apache.org/ivy/extra", md.getExtraAttributesNamespaces().get("e"));
    }

    @Test
    public void testEmptyDependencies() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-empty-dependencies.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        assertNotNull(md.getConfigurations());
        assertEquals(Collections.singletonList(new Configuration("default")),
            Arrays.asList(md.getConfigurations()));

        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);
        assertEquals("mymodule", md.getArtifacts("default")[0].getName());
        assertEquals("jar", md.getArtifacts("default")[0].getType());

        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }

    @Test
    public void testBad() throws IOException, ParseException {
        expExc.expect(ParseException.class);
        expExc.expectMessage("'modul'");

        assertTrue(XMLHelper.canUseSchemaValidation());

        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-bad.xml"), true);
    }

    @Test
    public void testBadOrg() throws IOException, ParseException {
        expExc.expect(ParseException.class);
        expExc.expectMessage("organization");

        assertTrue(XMLHelper.canUseSchemaValidation());

        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-bad-org.xml"), true);
    }

    @Test
    public void testBadConfs() throws IOException, ParseException {
        expExc.expect(ParseException.class);
        expExc.expectMessage("invalidConf");

        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-bad-confs.xml"), true);
    }

    @Test
    public void testCyclicConfs2() throws IOException, ParseException {
        expExc.expect(ParseException.class);
        expExc.expectMessage("A => B => A");

        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-cyclic-confs1.xml"), true);
    }

    @Test
    public void testCyclicConfs3() throws IOException, ParseException {
        expExc.expect(ParseException.class);
        expExc.expectMessage("A => C => B => A");

        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-cyclic-confs2.xml"), true);
    }

    @Test
    public void testNoValidate() throws IOException, ParseException {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-novalidate.xml"), false);
    }

    /**
     * Test must fail because of bad version.
     *
     * @throws IOException if something goes wrong
     * @throws ParseException if something goes wrong
     */
    @Test(expected = ParseException.class)
    public void testBadVersion() throws IOException, ParseException {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-bad-version.xml"), true);
    }

    @Test
    public void testFull() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());

        License[] licenses = md.getLicenses();
        assertEquals(1, licenses.length);
        assertEquals("MyLicense", licenses[0].getName());
        assertEquals("http://www.my.org/mymodule/mylicense.html", licenses[0].getUrl());

        assertEquals("http://www.my.org/mymodule/", md.getHomePage());
        assertEquals("This module is <b>great</b> !<br/>\n\t"
                + "You can use it especially with myconf1 and myconf2, "
                + "and myconf4 is not too bad too.", md.getDescription().replaceAll("\r\n", "\n")
                .replace('\r', '\n'));

        assertEquals(1, md.getExtraInfos().size());
        ExtraInfoHolder someExtraElement = md.getExtraInfos().get(0);
        assertEquals("e:someExtra", someExtraElement.getName());
        assertEquals("56576", someExtraElement.getContent());
        assertEquals(0, someExtraElement.getAttributes().size());

        Configuration[] confs = md.getConfigurations();
        assertNotNull(confs);
        assertEquals(5, confs.length);

        assertConf(md, "myconf1", "desc 1", PUBLIC, new String[0]);
        assertConf(md, "myconf2", "desc 2", PUBLIC, new String[0]);
        assertConf(md, "myconf3", "desc 3", PRIVATE, new String[0]);
        assertConf(md, "myconf4", "desc 4", PUBLIC, new String[] {
                "myconf1", "myconf2"});
        assertConf(md, "myoldconf", "my old desc", PUBLIC, new String[0]);

        assertArtifacts(md.getArtifacts("myconf1"), new String[] {"myartifact1", "myartifact2",
                "myartifact3", "myartifact4"});
        assertArtifacts(md.getArtifacts("myconf2"), new String[] {"myartifact1", "myartifact3"});
        assertArtifacts(md.getArtifacts("myconf3"), new String[] {"myartifact1", "myartifact3",
                "myartifact4"});
        assertArtifacts(md.getArtifacts("myconf4"), new String[] {"myartifact1"});

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(13, dependencies.length);

        // no conf def => equivalent to *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3",
                    "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);
        assertFalse(dd.isChanging());
        assertTrue(dd.isTransitive());

        // changing = true
        dd = getDependency(dependencies, "mymodule3");
        assertNotNull(dd);
        assertTrue(dd.isChanging());
        assertFalse(dd.isTransitive());

        // conf="myconf1" => equivalent to myconf1->myconf1
        dd = getDependency(dependencies, "yourmodule1");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("trunk", dd.getDependencyRevisionId().getBranch());
        assertEquals("1.1", dd.getDependencyRevisionId().getRevision());
        assertEquals("branch1", dd.getDynamicConstraintDependencyRevisionId().getBranch());
        assertEquals("1+", dd.getDynamicConstraintDependencyRevisionId().getRevision());
        assertEquals("yourorg#yourmodule1#branch1;1+", dd
                .getDynamicConstraintDependencyRevisionId().toString());

        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3",
                    "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        // conf="myconf1->yourconf1"
        dd = getDependency(dependencies, "yourmodule2");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("2+", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("yourconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3",
                    "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        // conf="myconf1->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule3");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("3.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3",
                    "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        // conf="myconf1, myconf2->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule4");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("4.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Arrays.asList("myconf1", "myconf2")),
                new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf2")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        // conf="myconf1->yourconf1;myconf2->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule5");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("5.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Arrays.asList("myconf1", "myconf2")),
                new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Collections.singletonList("yourconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf2")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        // conf="*->@"
        dd = getDependency(dependencies, "yourmodule11");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("11.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singleton("*"),
            new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Collections.singletonList("myconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf2")));
        assertEquals(Collections.singletonList("myconf3"),
            Arrays.asList(dd.getDependencyConfigurations("myconf3")));
        assertEquals(Collections.singletonList("myconf4"),
            Arrays.asList(dd.getDependencyConfigurations("myconf4")));

        dd = getDependency(dependencies, "yourmodule6");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("latest.integration", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Arrays.asList("myconf1", "myconf2")),
                new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Collections.singletonList("yourconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf2")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        dd = getDependency(dependencies, "yourmodule7");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("7.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Arrays.asList("myconf1", "myconf2")),
                new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Collections.singletonList("yourconf1"),
            Arrays.asList(dd.getDependencyConfigurations("myconf1")));
        assertEquals(Arrays.asList("yourconf1", "yourconf2"),
            Arrays.asList(dd.getDependencyConfigurations("myconf2")));
        assertEquals(Collections.emptyList(),
            Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1", "myconf2", "myconf3",
                "myconf4"}, new String[0]);

        dd = getDependency(dependencies, "yourmodule8");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("8.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Collections.singletonList("*")),
            new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifacts(dd, new String[] {"myconf1"}, new String[] {"yourartifact8-1",
                "yourartifact8-2"});
        assertDependencyArtifacts(dd, new String[] {"myconf2"}, new String[] {"yourartifact8-1",
                "yourartifact8-2"});
        assertDependencyArtifacts(dd, new String[] {"myconf3"}, new String[] {"yourartifact8-1",
                "yourartifact8-2"});
        assertDependencyArtifacts(dd, new String[] {"myconf4"}, new String[] {"yourartifact8-1",
                "yourartifact8-2"});

        dd = getDependency(dependencies, "yourmodule9");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("9.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet<>(Arrays.asList("myconf1", "myconf2", "myconf3")),
            new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifacts(dd, new String[] {"myconf1"}, new String[] {"yourartifact9-1"});
        assertDependencyArtifacts(dd, new String[] {"myconf2"}, new String[] {"yourartifact9-1",
                "yourartifact9-2"});
        assertDependencyArtifacts(dd, new String[] {"myconf3"}, new String[] {"yourartifact9-2"});
        assertDependencyArtifacts(dd, new String[] {"myconf4"}, new String[] {});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf1"}, new String[] {});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf2"}, new String[] {});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf3"}, new String[] {});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf4"}, new String[] {});

        dd = getDependency(dependencies, "yourmodule10");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("10.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singleton("*"),
            new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf1"}, new String[] {"your.*",
                PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf2"}, new String[] {"your.*",
                PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf3"}, new String[] {"your.*",
                PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactIncludeRules(dd, new String[] {"myconf4"}, new String[] {"your.*",
                PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf1"},
            new String[] {"toexclude"});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf2"},
            new String[] {"toexclude"});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf3"},
            new String[] {"toexclude"});
        assertDependencyArtifactExcludeRules(dd, new String[] {"myconf4"},
            new String[] {"toexclude"});

        ConflictManager cm = md.getConflictManager(new ModuleId("yourorg", "yourmodule1"));
        assertNotNull(cm);
        assertTrue(cm instanceof NoConflictManager);

        cm = md.getConflictManager(new ModuleId("yourorg", "yourmodule2"));
        assertNotNull(cm);
        assertTrue(cm instanceof NoConflictManager);

        cm = md.getConflictManager(new ModuleId("theirorg", "theirmodule1"));
        assertNotNull(cm);
        assertTrue(cm instanceof FixedConflictManager);
        FixedConflictManager fcm = (FixedConflictManager) cm;
        assertEquals(2, fcm.getRevs().size());
        assertTrue(fcm.getRevs().contains("1.0"));
        assertTrue(fcm.getRevs().contains("1.1"));

        cm = md.getConflictManager(new ModuleId("theirorg", "theirmodule2"));
        assertNull(cm);

        assertEquals(
            ModuleRevisionId.parse("yourorg#yourmodule1#BRANCH;1.0"),
            md.mediate(
                new DefaultDependencyDescriptor(ModuleRevisionId.parse("yourorg#yourmodule1;2.0"),
                        false)).getDependencyRevisionId());

        ExcludeRule[] rules = md.getAllExcludeRules();
        assertNotNull(rules);
        assertEquals(2, rules.length);
        assertEquals(GlobPatternMatcher.INSTANCE, rules[0].getMatcher());
        assertEquals(ExactPatternMatcher.INSTANCE, rules[1].getMatcher());
        assertEquals(Collections.singletonList("myconf1"),
            Arrays.asList(rules[0].getConfigurations()));
        assertEquals(
            Arrays.asList("myconf1", "myconf2", "myconf3", "myconf4", "myoldconf"),
            Arrays.asList(rules[1].getConfigurations()));
    }

    @Test
    public void testFullNoValidation() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test.xml"), false);
        assertNotNull(md);
        assertEquals(1, md.getExtraInfos().size());
        ExtraInfoHolder someExtraElement = md.getExtraInfos().get(0);
        assertEquals("e:someExtra", someExtraElement.getName());
        assertEquals("56576", someExtraElement.getContent());
        assertEquals(0, someExtraElement.getAttributes().size());
        assertEquals(0, someExtraElement.getNestedExtraInfoHolder().size());
    }

    @Test
    public void testExtraInfos() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extrainfo.xml"), true);
        assertNotNull(md);
        assertEquals(2, md.getExtraInfos().size());
        ExtraInfoHolder firstExtraInfoElement = md.getExtraInfos().get(0);
        assertEquals("e:someExtra", firstExtraInfoElement.getName());
        assertEquals("56576", firstExtraInfoElement.getContent());
        assertEquals(0, firstExtraInfoElement.getAttributes().size());
        assertEquals(0, firstExtraInfoElement.getNestedExtraInfoHolder().size());
        ExtraInfoHolder secondExtraInfoElement = md.getExtraInfos().get(1);
        assertEquals("e:someExtraWithAttributes", secondExtraInfoElement.getName());
        assertEquals("", secondExtraInfoElement.getContent());
        assertEquals(2, secondExtraInfoElement.getAttributes().size());
        assertEquals("foo", secondExtraInfoElement.getAttributes().get("attr1"));
        assertEquals("bar", secondExtraInfoElement.getAttributes().get("attr2"));
        assertEquals(0, secondExtraInfoElement.getNestedExtraInfoHolder().size());
    }

    @Test
    public void testExtraInfosNested() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extrainfo-nested.xml"), true);
        assertNotNull(md);
        assertEquals(2, md.getExtraInfos().size());
        ExtraInfoHolder someExtraElement = md.getExtraInfos().get(0);
        assertEquals("e:someExtra", someExtraElement.getName());
        assertEquals("56576", someExtraElement.getContent());
        assertEquals(0, someExtraElement.getAttributes().size());
        assertEquals(0, someExtraElement.getNestedExtraInfoHolder().size());
        ExtraInfoHolder someExtraElementWithAttributes = md.getExtraInfos().get(1);
        assertEquals("e:someExtraWithAttributes", someExtraElementWithAttributes.getName());
        assertEquals("", someExtraElementWithAttributes.getContent());
        assertEquals(2, someExtraElementWithAttributes.getAttributes().size());
        assertEquals("foo", someExtraElementWithAttributes.getAttributes().get("attr1"));
        assertEquals("bar", someExtraElementWithAttributes.getAttributes().get("attr2"));
        assertEquals(1, someExtraElementWithAttributes.getNestedExtraInfoHolder().size());
        ExtraInfoHolder anotherExtraInfoElement = someExtraElementWithAttributes
                .getNestedExtraInfoHolder().get(0);
        assertEquals("e:anotherExtraInfo", anotherExtraInfoElement.getName());
        assertEquals("", anotherExtraInfoElement.getContent());
        assertEquals(1, anotherExtraInfoElement.getAttributes().size());
        assertEquals("foobar", anotherExtraInfoElement.getAttributes().get("myattribute"));
        assertEquals(1, anotherExtraInfoElement.getNestedExtraInfoHolder().size());
        ExtraInfoHolder yetAnotherExtraInfoElement = anotherExtraInfoElement
                .getNestedExtraInfoHolder().get(0);
        assertEquals("e:yetAnotherExtraInfo", yetAnotherExtraInfoElement.getName());
        assertEquals("", yetAnotherExtraInfoElement.getContent());
        assertEquals(1, yetAnotherExtraInfoElement.getAttributes().size());
        assertEquals("value", yetAnotherExtraInfoElement.getAttributes().get("anAttribute"));
        assertEquals(0, yetAnotherExtraInfoElement.getNestedExtraInfoHolder().size());
    }

    @Test
    public void testBug60() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-bug60.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());

        assertEquals(Collections.singletonList(new Configuration("default")),
            Arrays.asList(md.getConfigurations()));

        assertArtifacts(md.getArtifacts("default"), new String[] {"myartifact1", "myartifact2"});
    }

    @Test
    public void testNoArtifact() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-noartifact.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        assertNotNull(md.getConfigurations());
        assertEquals(Collections.singletonList(new Configuration("default")),
            Arrays.asList(md.getConfigurations()));

        assertNotNull(md.getArtifacts("default"));
        assertEquals(0, md.getArtifacts("default").length);

        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }

    @Test
    public void testNoPublication() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-nopublication.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());

        assertNotNull(md.getConfigurations());
        assertEquals(Collections.singletonList(new Configuration("default")),
            Arrays.asList(md.getConfigurations()));

        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);

        assertNotNull(md.getDependencies());
        assertEquals(1, md.getDependencies().length);
    }

    @Test
    public void testArtifactsDefaults() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-artifacts-defaults.xml"), true);
        assertNotNull(md);

        Artifact[] artifacts = md.getArtifacts("default");
        assertNotNull(artifacts);
        assertEquals(3, artifacts.length);
        assertArtifactEquals("mymodule", "jar", "jar", artifacts[0]);
        assertArtifactEquals("myartifact", "jar", "jar", artifacts[1]);
        assertArtifactEquals("mymodule", "dll", "dll", artifacts[2]);
    }

    private void assertArtifactEquals(String name, String type, String ext, Artifact artifact) {
        assertEquals(name + "/" + type + "/" + ext, artifact.getName() + "/" + artifact.getType()
                + "/" + artifact.getExt());
    }

    @Test
    public void testDefaultConfWithDefaultConfMapping() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-defaultconf-withdefaultconfmapping.xml"), true);
        assertNotNull(md);

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("default")));

        // confs def: *->*
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("test"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("test")));
    }

    @Test
    public void testDefaultConf() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-defaultconf.xml"), true);
        assertNotNull(md);

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("default")));

        // confs def: *->*
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("default")));
    }

    @Test
    public void testDefaultConf2() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-defaultconf2.xml"), true);
        assertNotNull(md);

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: *->default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("default")));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("test")));

        // confs def: test: should not use default conf for the right side (use of
        // defaultconfmapping is required for that) => test->test
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("test"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("test"),
            Arrays.asList(dd.getDependencyConfigurations("test")));
    }

    @Test
    public void testPublicationDefaultConf() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-publication-defaultconf.xml"), true);
        assertNotNull(md);

        Artifact[] artifacts = md.getArtifacts("default");
        assertNotNull(artifacts);
        assertEquals(3, artifacts.length);

        artifacts = md.getArtifacts("test");
        assertNotNull(artifacts);
        assertEquals(2, artifacts.length);

        artifacts = md.getArtifacts("other");
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
    }

    @Test
    public void testDefaultConfMapping() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-defaultconfmapping.xml"), true);
        assertNotNull(md);

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: *->default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("default")));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("test")));

        // confs def: test: should use default conf mapping for the right side => test->default
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Collections.singletonList("test"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default"),
            Arrays.asList(dd.getDependencyConfigurations("test")));
    }

   @Test
    public void testExtraAttributes() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extra-attributes.xml"), false);
        assertNotNull(md);

        assertEquals("infoextravalue", md.getAttribute("infoextra"));
        assertEquals("infoextravalue", md.getModuleRevisionId().getAttribute("infoextra"));

        assertEquals("confextravalue", md.getConfiguration("default").getAttribute("confextra"));

        Artifact[] artifacts = md.getArtifacts("default");
        assertEquals(1, artifacts.length);
        Artifact art = artifacts[0];
        assertEquals("art1", art.getName());
        assertEquals("artextravalue", art.getAttribute("artextra"));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(1, dependencies.length);

        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals("depextravalue", dd.getAttribute("depextra"));
        assertEquals("depextravalue", dd.getDependencyRevisionId().getAttribute("depextra"));
    }

    @Test
    public void testImportConfigurations1() throws Exception {
        // import configurations
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configurations-import1.xml"), true);
        assertNotNull(md);

        // should have imported configurations
        assertNotNull(md.getConfigurations());
        assertEquals(
            Arrays.asList(new Configuration("conf1", PUBLIC, "", new String[0], true, null),
                    new Configuration("conf2", PRIVATE, "", new String[0], true, null)),
            Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));

        // confs def: conf1->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Collections.singletonList("conf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));
    }

    @Test
    public void testImportConfigurations2() throws Exception {
        // import configurations and add another one
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configurations-import2.xml"), true);
        assertNotNull(md);

        // should have imported configurations and added the one defined in the file itself
        assertNotNull(md.getConfigurations());
        assertEquals(
            Arrays.asList(new Configuration("conf1", PUBLIC, "", new String[0], true, null),
                    new Configuration("conf2", PRIVATE, "", new String[0], true, null),
                    new Configuration("conf3", PUBLIC, "", new String[0], true, null)),
            Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));

        // confs def: conf2,conf3->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(new HashSet<>(Arrays.asList("conf2", "conf3")),
                new HashSet<>(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf2")));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf3")));
    }

    @Test
    public void testImportConfigurations3() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configurations-import3.xml"), true);
        assertNotNull(md);

        // should have imported configurations
        assertNotNull(md.getConfigurations());
        assertEquals(
            Arrays.asList(new Configuration("conf1", PUBLIC, "", new String[0], true, null),
                    new Configuration("conf2", PRIVATE, "", new String[0], true, null)),
            Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf defined in imported file: *->@
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("conf1"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));
        assertEquals(Collections.singletonList("conf2"),
            Arrays.asList(dd.getDependencyConfigurations("conf2")));

        // confs def: conf1->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Collections.singletonList("conf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));
    }

    @Test
    public void testImportConfigurations5() throws Exception {
        // import configurations
        settings.setVariable("base.dir", new File(".").getAbsolutePath());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configurations-import5.xml"), true);
        assertNotNull(md);

        // should have imported configurations
        assertNotNull(md.getConfigurations());
        assertEquals(
            Arrays.asList(new Configuration("conf1", PUBLIC, "", new String[0], true, null),
                    new Configuration("conf2", PRIVATE, "", new String[0], true, null)),
            Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // no conf def => defaults to defaultConf: *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));

        // confs def: conf1->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Collections.singletonList("conf1"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("*"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));
    }

    @Test
    public void testExtendOtherConfigs() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configextendsothers1.xml"), true);
        assertNotNull(md);

        // has an 'all-public' configuration
        Configuration allPublic = md.getConfiguration("all-public");
        assertNotNull(allPublic);

        // 'all-public' extends all other public configurations
        String[] allPublicExt = allPublic.getExtends();
        assertEquals(Arrays.asList("default", "test"), Arrays.asList(allPublicExt));
    }

    @Test
    public void testImportConfigurationsWithExtendOtherConfigs() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configextendsothers2.xml"), true);
        assertNotNull(md);

        // has an 'all-public' configuration
        Configuration allPublic = md.getConfiguration("all-public");
        assertNotNull(allPublic);

        // 'all-public' extends all other public configurations
        String[] allPublicExt = allPublic.getExtends();
        assertEquals(Arrays.asList("default", "test", "extra"),
            Arrays.asList(allPublicExt));
    }

    @Test
    public void testImportConfigurationsWithMappingOverride() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configurations-import4.xml"), true);
        assertNotNull(md);

        // has 2 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // confs dep1: conf1->A;conf2->B (mappingoverride = true)
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList("conf1", "conf2"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("A"),
            Arrays.asList(dd.getDependencyConfigurations("conf1")));
        assertEquals(Collections.singletonList("B"),
            Arrays.asList(dd.getDependencyConfigurations("conf2")));

        // confs dep2: conf2->B
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Collections.singletonList("conf2"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("B"),
            Arrays.asList(dd.getDependencyConfigurations("conf2")));
    }

    @Test
    public void testImportConfigurationsWithWildcardAndMappingOverride() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-configextendsothers3.xml"), true);
        assertNotNull(md);

        // has 2 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);

        // confs dep1: all-public->all-public (mappingoverride = true)
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("all-public"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("all-public"),
            Arrays.asList(dd.getDependencyConfigurations("all-public")));

        // confs dep2: extra->extra;all-public->all-public (mappingoverride = true)
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList("extra", "all-public"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("extra"),
            Arrays.asList(dd.getDependencyConfigurations("extra")));
        assertEquals(Collections.singletonList("all-public"),
            Arrays.asList(dd.getDependencyConfigurations("all-public")));
    }

    @Test
    public void testDefaultConfMappingWithSelectors() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-defaultconfmapping-withselectors.xml"), true);
        assertNotNull(md);

        // has 3 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.length);

        // confs dep1: *->default1,default3
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList("default1", "default3"),
            Arrays.asList(dd.getDependencyConfigurations("default")));

        // confs dep2: test->default2,default3
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Collections.singletonList("test"),
            Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList("default2", "default3"),
            Arrays.asList(dd.getDependencyConfigurations("test")));

        // confs dep3: *->default4
        dd = getDependency(dependencies, "mymodule3");
        assertEquals(Collections.singletonList("*"), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Collections.singletonList("default4"),
            Arrays.asList(dd.getDependencyConfigurations("bla")));
    }

    /**
     * Test case for IVY-442: expect parser failure.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-442">IVY-442</a>
     */
    @Test(expected = ParseException.class)
    public void testWithNonExistingConfigInDependency() throws Exception {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-incorrectconf1.xml"), true);
    }

    /**
     * Test for parser failure.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = ParseException.class)
    public void testWithNonExistingConfigInPublications() throws Exception {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-incorrectconf2.xml"), true);
    }

    /**
     * Test case for IVY-441: expect parser failure.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-441">IVY-441</a>
     */
    @Test(expected = ParseException.class)
    public void testWithExistingConfigsInPublicationsSeparatedBySemiColon() throws Exception {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                getClass().getResource("test-incorrectconf3.xml"), true);
    }

    @Test
    public void testExtendsAll() throws Exception {
        Message.setDefaultLogger(new DefaultMessageLogger(99));

        // default extends type is 'all' when no extendsType attribute is specified.
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-all.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was merged.
        assertEquals("Parent module description.", md.getDescription());

        // verify that the parent and child configurations were merged together.
        final Configuration[] expectedConfs = {new Configuration("default"),
                new Configuration("conf1"), new Configuration("conf2")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent and child dependencies were merged together.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule1", dep.getModuleId().getName());
        assertEquals("1.0", dep.getRevision());

        assertEquals(Arrays.asList("conf1", "conf2"),
            Arrays.asList(deps[1].getModuleConfigurations()));
        dep = deps[1].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsDependencies() throws Exception {
        // descriptor specifies that only parent dependencies should be included
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-dependencies.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was ignored.
        assertEquals("", md.getDescription());

        // verify that the parent configurations were ignored.
        final Configuration[] expectedConfs = {new Configuration("default")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent dependencies were merged.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule1", dep.getModuleId().getName());
        assertEquals("1.0", dep.getRevision());

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[1].getModuleConfigurations()));
        dep = deps[1].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsConfigurations() throws Exception {
        // descriptor specifies that only parent configurations should be included
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-configurations.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was ignored.
        assertEquals("", md.getDescription());

        // verify that the parent and child configurations were merged together.
        final Configuration[] expectedConfs = {new Configuration("default"),
                new Configuration("conf1"), new Configuration("conf2")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent dependencies were ignored.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(1, deps.length);

        assertEquals(Arrays.asList("conf1", "conf2"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsDescription() throws Exception {
        // descriptor specifies that only parent description should be included
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-description.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was merged.
        assertEquals("Parent module description.", md.getDescription());

        // verify that the parent configurations were ignored.
        final Configuration[] expectedConfs = {new Configuration("default")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent dependencies were ignored.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(1, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsDescriptionWithOverride() throws Exception {
        // descriptor specifies that only parent description should be included
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-description-override.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // child description should always be preferred, even if extendType="description"
        assertEquals("Child description overrides parent.", md.getDescription());

        // verify that the parent configurations were ignored.
        final Configuration[] expectedConfs = {new Configuration("default")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent dependencies were ignored.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(1, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsMixed() throws Exception {
        // descriptor specifies that parent configurations and dependencies should be included
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-mixed.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(Ivy.getWorkingRevision(), md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was ignored.
        assertEquals("", md.getDescription());

        // verify that the parent and child configurations were merged together.
        final Configuration[] expectedConfs = {new Configuration("default"),
                new Configuration("conf1"), new Configuration("conf2")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent and child dependencies were merged together.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule1", dep.getModuleId().getName());
        assertEquals("1.0", dep.getRevision());

        assertEquals(Arrays.asList("conf1", "conf2"),
            Arrays.asList(deps[1].getModuleConfigurations()));
        dep = deps[1].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    @Test
    public void testExtendsCached() throws Exception {
        // configure a resolver to serve the parent descriptor, so that parse succeeds.
        File resolveRoot = new File("build/tmp/xmlModuleDescriptorTest");
        assertTrue(resolveRoot.exists() || resolveRoot.mkdirs());

        FileUtil.copy(getClass().getResource("test-extends-parent.xml"), new File(resolveRoot,
                "myorg/myparent/ivy.xml"), null, null);

        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setSettings(settings);
        resolver.setName("testExtendsCached");
        resolver.addIvyPattern(resolveRoot.getAbsolutePath()
                + "/[organisation]/[module]/[artifact].[ext]");

        settings.addResolver(resolver);
        settings.setDefaultResolver("testExtendsCached");

        // descriptor extends a module without a location="..." attribute, so resolver lookup
        // must be performed.
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-extends-cached.xml"), true);
        assertNotNull(md);

        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());

        // verify that the parent description was merged.
        assertEquals("Parent module description.", md.getDescription());

        // verify that the parent and child configurations were merged together.
        final Configuration[] expectedConfs = {new Configuration("default"),
                new Configuration("conf1"), new Configuration("conf2")};
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(expectedConfs), Arrays.asList(md.getConfigurations()));

        // verify parent and child dependencies were merged together.
        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.length);

        assertEquals(Collections.singletonList("default"),
            Arrays.asList(deps[0].getModuleConfigurations()));
        ModuleRevisionId dep = deps[0].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule1", dep.getModuleId().getName());
        assertEquals("1.0", dep.getRevision());

        assertEquals(Arrays.asList("conf1", "conf2"),
            Arrays.asList(deps[1].getModuleConfigurations()));
        dep = deps[1].getDependencyRevisionId();
        assertEquals("myorg", dep.getModuleId().getOrganisation());
        assertEquals("mymodule2", dep.getModuleId().getName());
        assertEquals("2.0", dep.getRevision());

        // verify only child publications are present
        Artifact[] artifacts = md.getAllArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("mymodule", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getType());
    }

    /**
     * Tests that when the <code>location</code> attribute of the <code>extends</code> element of
     * a module descriptor file, includes an {@link File#isAbsolute() absolute path} with
     * characters that {@link java.net.URI} considers as encoded characters (for example,
     * <code>%2F</code>) then the module descriptor and the location of the parent descriptor
     * are resolved and parsed correctly.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1562">IVY-1562</a>
     */
    @Test
    public void testExtendsAbsoluteLocation() throws Exception {
        final URL ivyXML = this.getClass().getResource("foo%2Fbar/hello/test-ivy-extends-absolute.xml");
        assertNotNull("Ivy xml file is missing", ivyXML);
        final URL parentIvyXML = this.getClass().getResource("foo%2Fbar/parent-ivy.xml");
        assertNotNull("Parent Ivy xml file is missing", parentIvyXML);
        // the ivy xml references a parent ivy xml via extends "location" and expects the parent
        // ivy to be present at a location under java.io.tmpdir, so we copy over the parent ivy
        // file over there
        final Path targetDir = Paths.get(System.getProperty("java.io.tmpdir"), "foo%2Fbar");
        Files.createDirectories(targetDir);
        final Path parentIvyXMLPath = Paths.get(targetDir.toString(), "parent-ivy.xml");
        try (final InputStream is = parentIvyXML.openStream()) {
            Files.copy(is, parentIvyXMLPath, StandardCopyOption.REPLACE_EXISTING);
        }
        assertTrue("Parent ivy xml file wasn't copied", Files.isRegularFile(parentIvyXMLPath));
        try {
            // now start parsing the Ivy xml
            final ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings, ivyXML, true);
            assertNotNull("Parsed module descriptor is null", md);
            assertEquals("Unexpected org for the parsed module descriptor", "myorg", md.getModuleRevisionId().getOrganisation());
            assertEquals("Unexpected module name for the parsed module descriptor", "mymodule", md.getModuleRevisionId().getName());
            assertEquals("Unexpected revision for the parsed module descriptor", "1.0.0", md.getModuleRevisionId().getRevision());

            final Configuration[] confs = md.getConfigurations();
            assertNotNull("No configurations found in module descriptor", confs);
            assertEquals("Unexpected number of configurations found in module descriptor", 3, confs.length);

            final Set<String> expectedConfs = new HashSet<>(Arrays.asList("parent-conf1", "parent-conf2", "conf2"));
            for (final Configuration conf : confs) {
                assertNotNull("One of the configurations was null in module descriptor", conf);
                assertTrue("Unexpected configuration " + conf.getName() + " found in parsed module descriptor", expectedConfs.remove(conf.getName()));
            }
            assertTrue("Missing configurations " + expectedConfs + " from the parsed module descriptor", expectedConfs.isEmpty());
        } finally {
            // clean up the copied over file
            Files.delete(parentIvyXMLPath);
        }
    }
}
