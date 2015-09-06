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
package org.apache.ivy.plugins.parser.m2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParserTest;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.MockResolver;

public class PomModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    // junit test -- DO NOT REMOVE used by ant to know it's a junit test

    private IvySettings settings = new IvySettings();

    private class MockedDependencyResolver extends MockResolver {
        public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                throws ParseException {
            // TODO make it a real mock and check that dd and data are the one that are expected
            final ModuleDescriptor moduleDesc = getModuleDescriptor(dd);
            ResolvedModuleRevision r = new ResolvedModuleRevision(this, this, moduleDesc, null);
            return r;
        }

        protected ModuleDescriptor getModuleDescriptor(final DependencyDescriptor dependencyDescriptor) {
            return DefaultModuleDescriptor.newDefaultInstance(dependencyDescriptor.getDependencyRevisionId());
        }
    }

    private File dest = new File("build/test/test-write.xml");

    private MockResolver mockedResolver = new MockedDependencyResolver();

    protected void setUp() throws Exception {
        settings.setDictatorResolver(mockedResolver);
        super.setUp();
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

    public void testAccept() throws Exception {
        assertTrue(PomModuleDescriptorParser.getInstance().accept(
            new URLResource(getClass().getResource("test-simple.pom"))));
        assertFalse(PomModuleDescriptorParser.getInstance().accept(
            new URLResource(XmlModuleDescriptorParserTest.class.getResource("test.xml"))));
    }

    public void testSimple() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-simple.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS),
            Arrays.asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("jar", artifact[0].getType());
    }

    public void testLargePom() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-large-pom.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.myfaces", "myfaces", "6");
        assertEquals(mrid, md.getModuleRevisionId());
    }

    public void testPackaging() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-packaging.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("war", artifact[0].getExt());
        assertEquals("war", artifact[0].getType());
    }

    public void testEjbPackaging() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-ejb-packaging.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("ejb", artifact[0].getType());
    }

    public void testEjbType() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-ejb-type.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test-ejb-type", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        DependencyDescriptor[] deps = md.getDependencies();
        assertNotNull(deps);
        assertEquals(1, deps.length);

        DependencyArtifactDescriptor[] artifacts = deps[0].getAllDependencyArtifacts();
        assertNotNull(artifacts);
        assertEquals(1, artifacts.length);
        assertEquals("test", artifacts[0].getName());
        assertEquals("jar", artifacts[0].getExt());
        assertEquals("ejb", artifacts[0].getType());
    }

    public void testParent() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parent.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParentNotFound() throws Exception {
        try {
            PomModuleDescriptorParser.getInstance().parseDescriptor(new IvySettings(),
                getClass().getResource("test-parent-not-found.pom"), false);
            fail("IOException should have been thrown!");
        } catch (IOException e) {
            assertTrue(e.getMessage().indexOf("Impossible to load parent") != -1);
        }
    }

    public void testParent2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parent2.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParentVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parent.version.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParentGroupId() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parent.groupid.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testProjectParentVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-project.parent.version.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testDependencies() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencies.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals("There is no special artifact when there is no classifier", 0,
            dds[0].getAllDependencyArtifacts().length);
    }

    public void testDependenciesWithClassifier() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencies-with-classifier.pom"), true);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        Map extraAtt = Collections.singletonMap("classifier", "asl");
        assertEquals(1, dds[0].getAllDependencyArtifacts().length);
        assertEquals(extraAtt, dds[0].getAllDependencyArtifacts()[0].getExtraAttributes());

        // now we verify the conversion to an Ivy file
        PomModuleDescriptorParser.getInstance().toIvyFile(
            getClass().getResource("test-dependencies-with-classifier.pom").openStream(),
            new URLResource(getClass().getResource("test-dependencies-with-classifier.pom")), dest,
            md);

        assertTrue(dest.exists());

        // the converted Ivy file should be parsable with validate=true
        ModuleDescriptor md2 = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURI().toURL(), true);

        // and the parsed module descriptor should be similar to the original
        assertNotNull(md2);
        assertEquals(md.getModuleRevisionId(), md2.getModuleRevisionId());
        dds = md2.getDependencies();
        assertEquals(1, dds[0].getAllDependencyArtifacts().length);
        assertEquals(extraAtt, dds[0].getAllDependencyArtifacts()[0].getExtraAttributes());
    }

    public void testDependenciesWithType() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencies-with-type.pom"), true);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(1, dds[0].getAllDependencyArtifacts().length);
        assertEquals("dll", dds[0].getAllDependencyArtifacts()[0].getExt());
        assertEquals("dll", dds[0].getAllDependencyArtifacts()[0].getType());
    }

    public void testWithVersionPropertyAndPropertiesTag() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-version.pom"), false);
        assertNotNull(md);

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-other", "1.0"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-yet-other", "5.76"),
            dds[1].getDependencyRevisionId());
    }

    // IVY-392
    public void testDependenciesWithProfile() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencies-with-profile.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testWithoutVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-without-version.pom"), false);
        assertNotNull(md);

        assertEquals(new ModuleId("org.apache", "test"), md.getModuleRevisionId().getModuleId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testProperties() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-properties.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("drools", "drools-smf", "2.0-beta-18"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("drools", "drools-core", "2.0-beta-18"),
            dds[0].getDependencyRevisionId());
    }

    public void testReal() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("commons-lang-1.0.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("commons-lang", "commons-lang", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("junit", "junit", "3.7"),
            dds[0].getDependencyRevisionId());
    }

    public void testReal2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("wicket-1.3-incubating-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(
            ModuleRevisionId.newInstance("org.apache.wicket", "wicket", "1.3-incubating-SNAPSHOT"),
            md.getModuleRevisionId());
    }

    public void testVariables() throws Exception {
        // test case for IVY-425
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("spring-hibernate3-2.0.2.pom"), false);
        assertNotNull(md);

        assertEquals(
            ModuleRevisionId.newInstance("org.springframework", "spring-hibernate3", "2.0.2"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(11, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.springframework", "spring-web", "2.0.2"),
            dds[10].getDependencyRevisionId());
    }

    public void testDependenciesInProfile() throws Exception {
        // test case for IVY-423
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("mule-module-builders-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(
            ModuleRevisionId.newInstance("org.mule.modules", "mule-module-builders", "1.3.3"),
            md.getModuleRevisionId());
    }

    public void testIVY424() throws Exception {
        // test case for IVY-424
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("shale-tiger-1.1.0-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(
            ModuleRevisionId.newInstance("org.apache.shale", "shale-tiger", "1.1.0-SNAPSHOT"),
            md.getModuleRevisionId());
    }

    public void testOptional() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-optional.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());
        assertTrue(Arrays.asList(md.getConfigurationsNames()).contains("optional"));

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"optional"})),
            new HashSet(Arrays.asList(dds[0].getModuleConfigurations())));
        // I don't know what it should be. Ivy has no notion of optional dependencies
        // assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)",
        // "master(*)"})), new HashSet(Arrays.asList(dds[0]
        // .getDependencyConfigurations("optional"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"),
            dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib-extra", "2.0.2"),
            dds[2].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("runtime"))));
    }

    public void testDependenciesWithScope() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencies-with-scope.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("odmg", "odmg", "3.0"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime"})),
            new HashSet(Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(
            new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)", "master(*)"})),
            new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"),
            dds[2].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("runtime"))));
    }

    public void testExclusion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-exclusion.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[0].getAllExcludeRules().length);

        assertEquals(ModuleRevisionId.newInstance("dom4j", "dom4j", "1.6"),
            dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));
        assertDependencyModulesExcludes(dds[1], new String[] {"compile"}, new String[] {
                "jaxme-api", "jaxen"});
        assertDependencyModulesExcludes(dds[1], new String[] {"runtime"}, new String[] {
                "jaxme-api", "jaxen"});

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"),
            dds[2].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[2].getAllExcludeRules().length);
    }

    public void testWithPlugins() throws Exception {
        // test case for IVY-417
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("mule-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule", "mule", "1.3.3"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(0, dds.length);
    }

    public void testHomeAndDescription() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("mule-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule", "mule", "1.3.3"),
            md.getModuleRevisionId());

        assertEquals("http://mule.mulesource.org", md.getHomePage());
        assertEquals(
            "Mule is a simple yet robust and highly scalable Integration and ESB services "
                    + "framework. It is designed\n        as a light-weight, event-driven component "
                    + "technology that handles communication with disparate systems\n        "
                    + "transparently providing a simple component interface.", md.getDescription()
                    .replaceAll("\r\n", "\n").replace('\r', '\n'));
    }

    public void testLicense() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("spring-hibernate3-2.0.2.pom"), false);

        License[] licenses = md.getLicenses();
        assertNotNull(licenses);
        assertEquals(1, licenses.length);
        assertEquals("The Apache Software License, Version 2.0", licenses[0].getName());
        assertEquals("http://www.apache.org/licenses/LICENSE-2.0.txt", licenses[0].getUrl());
    }

    /**
     * Tests that if a module doesn't have a license specified, then parent pom's license (if any) is used for the child
     * module
     *
     * @throws Exception
     */
    public void testLicenseFromParent() throws Exception {
        final IvySettings customIvySettings = createIvySettingsForParentLicenseTesting("test-parent-with-licenses.pom",
                "org.apache", "test-ivy-license-parent");
        final String pomFile = "test-project-with-parent-licenses.pom";
        final ModuleDescriptor childModule = PomModuleDescriptorParser.getInstance().parseDescriptor(customIvySettings,
                this.getClass().getResource(pomFile), false);
        assertNotNull("Could not find " + pomFile, pomFile);
        final License[] licenses = childModule.getLicenses();
        assertNotNull("No licenses found in the module " + childModule, licenses);
        assertEquals("Unexpected number of licenses found in the module " + childModule, 1, licenses.length);
        assertEquals("Unexpected license name", "MIT License", licenses[0].getName());
        assertEquals("Unexpected license URL", "http://opensource.org/licenses/MIT", licenses[0].getUrl());
    }

    /**
     * Tests that if a project explicitly specifies the licenses, then the licenses (if any) from its parent pom
     * aren't applied to the child project
     *
     * @throws Exception
     */
    public void testOverriddenLicense() throws Exception {
        final IvySettings customIvySettings = createIvySettingsForParentLicenseTesting("test-parent-with-licenses.pom",
                "org.apache", "test-ivy-license-parent");
        final String pomFile = "test-project-with-overridden-licenses.pom";
        final ModuleDescriptor childModule = PomModuleDescriptorParser.getInstance().parseDescriptor(customIvySettings,
                this.getClass().getResource(pomFile), false);
        assertNotNull("Could not find " + pomFile, pomFile);
        final License[] licenses = childModule.getLicenses();
        assertNotNull("No licenses found in the module " + childModule, licenses);
        assertEquals("Unexpected number of licenses found in the module " + childModule, 1, licenses.length);
        assertEquals("Unexpected license name", "The Apache Software License, Version 2.0", licenses[0].getName());
        assertEquals("Unexpected license URL", "http://www.apache.org/licenses/LICENSE-2.0.txt", licenses[0].getUrl());
    }


    public void testDependencyManagment() throws ParseException, IOException {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencyMgt.pom"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-depMgt", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals("There is no special artifact when there is no classifier", 0,
            dds[0].getAllDependencyArtifacts().length);
        assertEquals(4, md.getExtraInfos().size());
    }

    public void testDependencyManagmentWithScope() throws ParseException, IOException {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-dependencyMgt-with-scope.pom"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-depMgt", "1.1"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals("There is no special artifact when there is no classifier", 0,
            dds[0].getAllDependencyArtifacts().length);
        assertEquals("The number of configurations is incorrect", 1,
            dds[0].getModuleConfigurations().length);
        assertEquals("The configuration must be test", "test", dds[0].getModuleConfigurations()[0]);
    }

    public void testParentDependencyMgt() throws ParseException, IOException {
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                    throws ParseException {
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance()
                            .parseDescriptor(settings,
                                getClass().getResource("test-dependencyMgt.pom"), false);
                    return new ResolvedModuleRevision(null, null, moduleDescriptor, null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });

        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parentDependencyMgt.pom"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-parentdep", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(
            ModuleRevisionId.newInstance("commons-collection", "commons-collection", "1.0.5"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[1].getDependencyRevisionId());

        ExcludeRule[] excludes = dds[0].getAllExcludeRules();
        assertNotNull(excludes);
        assertEquals(2, excludes.length);
        assertEquals("javax.mail", excludes[0].getId().getModuleId().getOrganisation());
        assertEquals("mail", excludes[0].getId().getModuleId().getName());
        assertEquals("javax.jms", excludes[1].getId().getModuleId().getOrganisation());
        assertEquals("jms", excludes[1].getId().getModuleId().getName());
    }

    public void testOverrideParentVersionPropertyDependencyMgt() throws ParseException, IOException {
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                    throws ParseException {
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance()
                            .parseDescriptor(settings,
                                getClass().getResource("test-versionPropertyDependencyMgt.pom"),
                                false);
                    return new ResolvedModuleRevision(null, null, moduleDescriptor, null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });

        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-overrideParentVersionPropertyDependencyMgt.pom"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-parentdep", "1.0"),
            md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(
            ModuleRevisionId.newInstance("commons-collections", "commons-collections", "3.2.1"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.1.1"),
            dds[1].getDependencyRevisionId());

        ExcludeRule[] excludes = dds[0].getAllExcludeRules();
        assertNotNull(excludes);
        assertEquals(2, excludes.length);
        assertEquals("javax.mail", excludes[0].getId().getModuleId().getOrganisation());
        assertEquals("mail", excludes[0].getId().getModuleId().getName());
        assertEquals("javax.jms", excludes[1].getId().getModuleId().getOrganisation());
        assertEquals("jms", excludes[1].getId().getModuleId().getName());
    }

    public void testParentProperties() throws ParseException, IOException {
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                    throws ParseException {
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance()
                            .parseDescriptor(settings, getClass().getResource("test-version.pom"),
                                false);
                    return new ResolvedModuleRevision(null, null, moduleDescriptor, null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });

        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-parent-properties.pom"), false);
        assertNotNull(md);
        assertEquals("1.0", md.getRevision());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        // 2 are inherited from parent. Only the first one is important for this test

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-version-other", "5.76"),
            dds[0].getDependencyRevisionId());// present in the pom using a property defined in the
                                              // parent
    }

    public void testOverrideParentProperties() throws ParseException, IOException {
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                    throws ParseException {
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance()
                            .parseDescriptor(settings, getClass().getResource("test-version.pom"),
                                false);
                    return new ResolvedModuleRevision(null, null, moduleDescriptor, null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-override-parent-properties.pom"), false);
        assertNotNull(md);
        assertEquals("1.0", md.getRevision());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        // 2 are inherited from parent. Only the first one is important for this test

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-yet-other", "5.79"),
            dds[1].getDependencyRevisionId());
    }

    public void testOverrideGrandparentProperties() throws ParseException, IOException {
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                    throws ParseException {
                String resource;
                if ("test".equals(dd.getDependencyId().getName())) {
                    resource = "test-parent-properties.pom";
                } else {
                    resource = "test-version.pom";
                }
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance()
                            .parseDescriptor(settings, getClass().getResource(resource), false);
                    return new ResolvedModuleRevision(null, null, moduleDescriptor, null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });

        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-override-grandparent-properties.pom"), false);
        assertNotNull(md);
        assertEquals("1.0", md.getRevision());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-version-other", "5.79"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-yet-other", "5.79"),
            dds[2].getDependencyRevisionId());
    }

    public void testPomWithEntity() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-entity.pom"), true);
        assertNotNull(md);
    }

    public void testModel() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(settings,
            getClass().getResource("test-model.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS),
            Arrays.asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("jar", artifact[0].getType());
    }

    private IvySettings createIvySettingsForParentLicenseTesting(final String parentPomFileName, final String parentOrgName,
                                                                 final String parentModuleName) throws Exception {
        final URL parentPomURL = this.getClass().getResource(parentPomFileName);
        assertNotNull("Could not find " + parentPomFileName, parentPomURL);
        final PomReader parentPomReader = new PomReader(parentPomURL, new URLResource(parentPomURL));
        final License[] parentLicenses = parentPomReader.getLicenses();
        assertNotNull("Missing licenses in parent pom " + parentPomFileName, parentLicenses);
        assertEquals("Unexpected number of licenses in parent pom " + parentPomFileName, 1, parentLicenses.length);
        final DependencyResolver dependencyResolver = new MockedDependencyResolver() {
            @Override
            protected ModuleDescriptor getModuleDescriptor(DependencyDescriptor dependencyDescriptor) {
                final String depOrg = dependencyDescriptor.getDependencyId().getOrganisation();
                final String depModuleName = dependencyDescriptor.getDependencyId().getName();
                if (depOrg.equals(parentOrgName) && depModuleName.equals(parentModuleName)) {
                    final DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(dependencyDescriptor.getDependencyRevisionId());
                    for (final License license : parentLicenses) {
                        moduleDescriptor.addLicense(license);
                    }
                    return moduleDescriptor;
                } else {
                    return super.getModuleDescriptor(dependencyDescriptor);
                }
            }
        };
        final IvySettings ivySettings = new IvySettings();
        ivySettings.setDictatorResolver(dependencyResolver);

        return ivySettings;
    }
}
