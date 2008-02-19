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
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
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
import org.apache.ivy.plugins.resolver.MockResolver;

public class PomModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    // junit test -- DO NOT REMOVE used by ant to know it's a junit test

    private IvySettings settings = new IvySettings();
    
    private class MockedDependencyResolver extends MockResolver {        
        public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) 
                throws ParseException {
            //TODO make it a real mock and check that dd and data are the one that are expected
            DefaultModuleDescriptor moduleDesc = DefaultModuleDescriptor.newDefaultInstance(
                                                                    dd.getDependencyRevisionId());
            ResolvedModuleRevision r = new ResolvedModuleRevision(this,this,moduleDesc,null);
            return r;
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
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-simple.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("jar", artifact[0].getType());
    }

    public void testPackaging() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-packaging.pom"), false);
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

    
    public void testParent() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-parent.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParent2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-parent2.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParentVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-parent.version.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testDependencies() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-dependencies.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals("There is no special artifact when there is no classifier", 
                     0, dds[0].getAllDependencyArtifacts().length);
    }

    public void testDependenciesWithClassifier() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-dependencies-with-classifier.pom"),
            true);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

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
            new URLResource(getClass().getResource("test-dependencies-with-classifier.pom")),
            dest, md);

        assertTrue(dest.exists());

        // the converted Ivy file should be parsable with validate=true
        ModuleDescriptor md2 = XmlModuleDescriptorParser.getInstance()
            .parseDescriptor(new IvySettings(), dest.toURL(), true);

        // and the parsed module descriptor should be similar to the original
        assertNotNull(md2);
        assertEquals(md.getModuleRevisionId(), md2.getModuleRevisionId());
        dds = md2.getDependencies();        assertEquals(1, dds[0].getAllDependencyArtifacts().length);
        assertEquals(extraAtt, dds[0].getAllDependencyArtifacts()[0].getExtraAttributes());
    }

    public void testWithVersionProperty() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-version.pom"), false);
        assertNotNull(md);

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-other", "1.0"), dds[0]
                .getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-yet-other", "5.76"), dds[1]
                .getDependencyRevisionId());
    }

    // IVY-392
    public void testDependenciesWithProfile() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-dependencies-with-profile.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testWithoutVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-without-version.pom"), false);
        assertNotNull(md);

        assertEquals(new ModuleId("org.apache", "test"), md.getModuleRevisionId().getModuleId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testProperties() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-properties.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("drools", "drools-smf", "2.0-beta-18"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("drools", "drools-core", "2.0-beta-18"), dds[0]
                .getDependencyRevisionId());
    }

    public void testReal() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("commons-lang-1.0.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("commons-lang", "commons-lang", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("junit", "junit", "3.7"), dds[0]
                .getDependencyRevisionId());
    }

    public void testReal2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("wicket-1.3-incubating-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache.wicket", "wicket",
            "1.3-incubating-SNAPSHOT"), md.getModuleRevisionId());
    }

    public void testVariables() throws Exception {
        // test case for IVY-425
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("spring-hibernate3-2.0.2.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.springframework", "spring-hibernate3",
            "2.0.2"), md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(11, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.springframework", "spring-web", "2.0.2"),
            dds[10].getDependencyRevisionId());
    }

    public void testDependenciesInProfile() throws Exception {
        // test case for IVY-423
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("mule-module-builders-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule.modules", "mule-module-builders",
            "1.3.3"), md.getModuleRevisionId());
    }

    public void testIVY424() throws Exception {
        // test case for IVY-424
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("shale-tiger-1.1.0-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache.shale", "shale-tiger",
            "1.1.0-SNAPSHOT"), md.getModuleRevisionId());
    }

    public void testOptional() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-optional.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());
        assertTrue(Arrays.asList(md.getConfigurationsNames()).contains("optional"));

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"optional"})), new HashSet(Arrays
                .asList(dds[0].getModuleConfigurations())));
        //I don't know what it should be.  Ivy has no notion of optional dependencies
        //assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)",
        //        "master(*)"})), new HashSet(Arrays.asList(dds[0]
        //        .getDependencyConfigurations("optional"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[1]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));
    }

    public void testDependenciesWithScope() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-dependencies-with-scope.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("odmg", "odmg", "3.0"), dds[0]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime"})), new HashSet(Arrays
                .asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)",
                "master(*)"})), new HashSet(Arrays.asList(dds[0]
                .getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[2].getDependencyConfigurations("runtime"))));
    }

    public void testExclusion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-exclusion.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[0].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[0].getAllExcludeRules().length);

        assertEquals(ModuleRevisionId.newInstance("dom4j", "dom4j", "1.6"), dds[1]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));
        assertDependencyModulesExcludes(dds[1], new String[] {"compile"}, new String[] {
                "jaxme-api", "jaxen"});
        assertDependencyModulesExcludes(dds[1], new String[] {"runtime"}, new String[] {
                "jaxme-api", "jaxen"});

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[2].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[2].getAllExcludeRules().length);
    }

    public void testWithPlugins() throws Exception {
        // test case for IVY-417
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("mule-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule", "mule", "1.3.3"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(0, dds.length);
    }

    
    public void testDependencyManagment() throws ParseException, IOException {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-dependencieMgt.pom"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-depMgt", "1.0"), 
                md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals("There is no special artifact when there is no classifier", 
                     0, dds[0].getAllDependencyArtifacts().length);
        
    }
    
    public void testParentDependencyMgt() throws ParseException, IOException {        
        settings.setDictatorResolver(new MockResolver() {
            public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
                try {
                    ModuleDescriptor moduleDescriptor = PomModuleDescriptorParser.getInstance().parseDescriptor(
                                            settings, getClass().getResource("test-dependencieMgt.pom"), false);
                    return new ResolvedModuleRevision(null,null,moduleDescriptor,null);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });
        
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            settings, getClass().getResource("test-parentDependencieMgt.pom"), false);
        assertNotNull(md);        
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-parentdep", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-collection", "commons-collection", "1.0.5"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[1].getDependencyRevisionId());
    }
}
