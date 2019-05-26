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
package org.apache.ivy.core.settings;

import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.ivy.plugins.lock.AbstractLockStrategy;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistryTest;
import org.apache.ivy.plugins.report.ReportOutputter;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.MockResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.plugins.resolver.packager.PackagerResolver;
import org.apache.ivy.plugins.version.ChainVersionMatcher;
import org.apache.ivy.plugins.version.MavenTimedSnapshotVersionMatcher;
import org.apache.ivy.plugins.version.MockVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the parsing of Ivy settings file through the {@link XmlSettingsParser}
 */
public class XmlSettingsParserTest {
    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Test
    public void test() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-test.xml"));

        File defaultCache = settings.getDefaultCache();
        assertNotNull(defaultCache);
        assertEquals("mycache", defaultCache.getName());

        assertFalse(settings.isCheckUpToDate());
        assertFalse(settings.doValidate());

        assertEquals("[module]/ivys/ivy-[revision].xml", settings.getDefaultCacheIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]",
                settings.getDefaultCacheArtifactPattern());

        LatestStrategy latest = settings.getLatestStrategy("mylatest-revision");
        assertNotNull(latest);
        assertTrue(latest instanceof LatestRevisionStrategy);
        LatestRevisionStrategy l = (LatestRevisionStrategy) latest;
        assertEquals(new Integer(-2), l.getSpecialMeanings().get("pre"));
        assertEquals(new Integer(4), l.getSpecialMeanings().get("qa"));

        DependencyResolver defaultResolver = settings.getDefaultResolver();
        assertNotNull(defaultResolver);
        assertEquals("libraries", defaultResolver.getName());
        assertTrue(defaultResolver instanceof FileSystemResolver);
        FileSystemResolver fsres = (FileSystemResolver) defaultResolver;
        List<String> ivyPatterns = fsres.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals("lib/[organisation]/[module]/ivys/ivy-[revision].xml",
                ivyPatterns.get(0));

        LatestStrategy strategy = fsres.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestRevisionStrategy);

        DependencyResolver internal = settings.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) internal;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("int1", fsInt1.getName());
        assertEquals(1, fsInt1.getIvyPatterns().size());
        assertLocationEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", fsInt1
                .getIvyPatterns().get(0));
        assertEquals("int2", subresolvers.get(1).getName());

        strategy = fsInt1.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestTimeStrategy);

        assertEquals("libraries",
                settings.getResolver(ModuleRevisionId.newInstance("unknown", "lib", "1.0")).getName());
        assertEquals("internal",
                settings.getResolver(ModuleRevisionId.newInstance("apache", "ant", "1.0")).getName());
        assertEquals("int2",
                settings.getResolver(ModuleRevisionId.newInstance("apache", "ivy", "2.0")).getName());
        assertEquals("int1",
                settings.getResolver(ModuleRevisionId.newInstance("apache", "ivy", "1.0")).getName());
        assertEquals("int1",
                settings.getResolver(ModuleRevisionId.newInstance("apache", "ivyde", "1.0")).getName());
    }

    @Test
    public void testTypedef() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-typedef.xml"));

        DependencyResolver mock = settings.getResolver("mock3");
        assertNotNull(mock);
        assertTrue(mock instanceof MockResolver);

        DependencyResolver internal = settings.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) internal;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        assertEquals("mock1", subresolvers.get(0).getName());
        assertEquals("mock2", subresolvers.get(1).getName());
        assertTrue(subresolvers.get(0) instanceof MockResolver);
        assertTrue(subresolvers.get(1) instanceof MockResolver);
    }

    @Test
    public void testStatuses() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-status.xml"));

        assertEquals("bronze", settings.getStatusManager().getDefaultStatus());
        assertEquals(0, settings.getStatusManager().getPriority("gold"));
        assertEquals(1, settings.getStatusManager().getPriority("silver"));
        assertEquals(2, settings.getStatusManager().getPriority("bronze"));
        assertFalse(settings.getStatusManager().isIntegration("gold"));
        assertFalse(settings.getStatusManager().isIntegration("silver"));
        assertTrue(settings.getStatusManager().isIntegration("bronze"));
    }

    @Test
    public void testConflictManager() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-conflict-manager.xml"));

        assertEquals("latest-time", settings.getConflictManager(new ModuleId("apache", "ivyde"))
                .getName());
        assertEquals("all", settings.getConflictManager(new ModuleId("apache", "ant")).getName());
    }

    @Test
    public void testResolveMode() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-resolveMode.xml"));

        assertEquals("dynamic", settings.getDefaultResolveMode());
        assertEquals("dynamic", settings.getResolveMode(new ModuleId("foo", "bar")));
        assertEquals("dynamic", settings.getResolveMode(new ModuleId("apache", "ivyde")));
        assertEquals("default", settings.getResolveMode(new ModuleId("apache", "ant")));
    }

    @Test
    public void testExtraModuleAttribute() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class
                .getResource("ivysettings-extra-module-attribute.xml"));

        assertEquals("default", settings.getResolveMode(new ModuleId("apache", "ivy")));
    }

    @Test
    public void testCache() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-cache.xml"));

        assertEquals(new File("repository").getCanonicalFile(), settings
                .getDefaultRepositoryCacheBasedir().getCanonicalFile());
        assertEquals(new File("resolution").getCanonicalFile(), settings
                .getDefaultResolutionCacheBasedir().getCanonicalFile());
        assertEquals("artifact-lock", settings.getDefaultLockStrategy().getName());

        assertEquals("[module]/ivys/ivy-[revision].xml", settings.getDefaultCacheIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]",
                settings.getDefaultCacheArtifactPattern());
        assertTrue(settings.isDefaultUseOrigin());

        DefaultRepositoryCacheManager c = (DefaultRepositoryCacheManager) settings
                .getRepositoryCacheManager("mycache");
        assertNotNull(c);
        assertEquals("mycache", c.getName());
        assertEquals(1000, c.getDefaultTTL());
        assertEquals(200,
                c.getTTL(ModuleRevisionId.newInstance("apache", "ivy", "latest.integration")));
        assertEquals(10 * 60 * 1000 + 20 * 1000, // 10m 20s
                c.getTTL(ModuleRevisionId.newInstance("org1", "A", "A")));
        assertEquals(5 * 3600 * 1000, // 5h
                c.getTTL(ModuleRevisionId.newInstance("org2", "A", "A")));
        assertEquals(60 * 3600 * 1000, // 2d 12h = 60h
                c.getTTL(ModuleRevisionId.newInstance("org3", "A", "A")));
        assertEquals(new File("mycache").getCanonicalFile(), c.getBasedir().getCanonicalFile());
        assertFalse(c.isUseOrigin());
        assertEquals("no-lock", c.getLockStrategy().getName());

        assertEquals("[module]/ivy-[revision].xml", c.getIvyPattern());
        assertEquals("[module]/[artifact]-[revision].[ext]", c.getArtifactPattern());

        DefaultRepositoryCacheManager c2 = (DefaultRepositoryCacheManager) settings
                .getRepositoryCacheManager("mycache2");
        assertNotNull(c2);
        assertEquals("mycache2", c2.getName());
        assertEquals(new File("repository").getCanonicalFile(), c2.getBasedir().getCanonicalFile());
        assertEquals("artifact-lock", c2.getLockStrategy().getName());

        assertEquals("[module]/ivys/ivy-[revision].xml", c2.getIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]", c2.getArtifactPattern());

        assertTrue(c2.isUseOrigin());

        assertEquals(c2, settings.getResolver("A").getRepositoryCacheManager());
        assertEquals(c, settings.getResolver("B").getRepositoryCacheManager());
    }

    /**
     * Test of resolver referencing a non existent cache.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testInvalidCache() throws Exception {
        expExc.expect(ParseException.class);
        expExc.expectMessage("mycache");

        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);

        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-cache-invalid.xml"));
    }

    @Test
    public void testVersionMatchers1() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-vmatcher1.xml"));

        VersionMatcher mock = settings.getVersionMatcher("vmock");
        assertNotNull(mock);
        assertTrue(mock instanceof MockVersionMatcher);

        VersionMatcher v = settings.getVersionMatcher();
        assertTrue(v instanceof ChainVersionMatcher);
        ChainVersionMatcher chain = (ChainVersionMatcher) v;
        assertEquals(3, chain.getMatchers().size());
        assertTrue(chain.getMatchers().contains(mock));
        assertTrue(chain.getMatchers().contains(settings.getVersionMatcher("exact")));
        assertTrue(chain.getMatchers().contains(settings.getVersionMatcher("latest")));
    }

    @Test
    public void testVersionMatchers2() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-vmatcher2.xml"));

        VersionMatcher mock = settings.getVersionMatcher("vmock");
        assertNotNull(mock);
        assertTrue(mock instanceof MockVersionMatcher);

        VersionMatcher v = settings.getVersionMatcher();
        assertTrue(v instanceof ChainVersionMatcher);
        ChainVersionMatcher chain = (ChainVersionMatcher) v;
        assertEquals(5, chain.getMatchers().size());
        assertTrue(chain.getMatchers().contains(mock));
    }

    /**
     * Tests that the {@code maven-tsnap-vm} version matcher, configured in the settings file,
     * is parsed correctly
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testMavenTimedSnapshotVersionMatcher() throws Exception {
        final IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-maven-tsnap-vmatcher.xml"));

        final VersionMatcher mavenTSnapVersionMatcher = settings.getVersionMatcher(new MavenTimedSnapshotVersionMatcher().getName());
        assertNotNull("Maven timestamped snapshot version matcher is missing", mavenTSnapVersionMatcher);
        assertTrue("Unexpected type for Maven timestamped snapshot version matcher", mavenTSnapVersionMatcher instanceof MavenTimedSnapshotVersionMatcher);
    }

    @Test
    public void testRef() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-ref.xml"));

        DependencyResolver internal = settings.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) internal;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("fs", fsInt1.getName());

        List<String> ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml",
                ivyPatterns.get(0));

        DependencyResolver external = settings.getResolver("external");
        assertNotNull(external);
        assertTrue(external instanceof ChainResolver);
        chain = (ChainResolver) external;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        FileSystemResolver fsInt2 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("fs", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml",
                ivyPatterns.get(0));
    }

    @Test
    public void testMacro() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-macro.xml"));

        DependencyResolver def = settings.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) def;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List<String> ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
                ivyPatterns.get(0));

        FileSystemResolver fsInt2 = (FileSystemResolver) subresolvers.get(1);
        assertEquals("default-fs2", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "path/to/secondrep/[organisation]/[module]/[type]s/ivy-[revision].xml",
                ivyPatterns.get(0));

        DependencyResolver other = settings.getResolver("other");
        assertNotNull(other);
        assertTrue(other instanceof ChainResolver);
        chain = (ChainResolver) other;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        fsInt2 = (FileSystemResolver) subresolvers.get(1);
        assertEquals("other-fs2", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals("path/to/secondrep/[module]/[type]s/ivy-[revision].xml",
                ivyPatterns.get(0));
    }

    /**
     * Test case for IVY-319.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-319">IVY-319</a>
     */
    @Test
    public void testMacroAndRef() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-macro+ref.xml"));

        DependencyResolver shared = settings.getResolver("shared");
        assertNotNull(shared);
        assertTrue(shared instanceof FileSystemResolver);

        DependencyResolver mychain = settings.getResolver("mychain");
        assertNotNull(mychain);
        assertTrue(mychain instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) mychain;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        shared = subresolvers.get(0);
        assertEquals("shared", shared.getName());
        assertTrue(shared instanceof FileSystemResolver);
    }

    /**
     * Test case for IVY-860.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-860">IVY-860</a>
     */
    @Test
    public void testMacroAndRef2() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-macro+ref2.xml"));

        DependencyResolver macrores = settings.getResolver("macroresolver");
        assertNotNull(macrores);
        assertTrue(macrores instanceof ChainResolver);

        DependencyResolver testResolver = settings.getResolver("test");
        assertNotNull(testResolver);
        assertTrue(testResolver instanceof IBiblioResolver);

        ChainResolver chain = (ChainResolver) macrores;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        assertEquals(testResolver, subresolvers.get(0));
    }

    @Test
    public void testPropertiesMissingFile() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class
                .getResource("ivysettings-properties-missing-file.xml"));

        // no error must have been thrown, check that the parsing didn't stop...
        DependencyResolver defaultResolver = settings.getDefaultResolver();
        assertNotNull(defaultResolver);
        assertEquals("libraries", defaultResolver.getName());
    }

    @Test
    public void testInclude() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-include.xml"));

        DependencyResolver def = settings.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) def;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List<String> ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
                ivyPatterns.get(0));

        DependencyResolver inc = settings.getResolver("includeworks");
        assertNotNull(inc);
        assertTrue(inc instanceof ChainResolver);
        chain = (ChainResolver) inc;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("includeworks-fs1", fsInt1.getName());

        ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "included/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
                ivyPatterns.get(0));

        // properties defined in included file should be available to including file (IVY-780)
        assertEquals("myvalue", settings.getVariable("ivy.test.prop"));
    }

    @Test
    public void testIncludeAbsoluteFile() throws Exception {
        // WARNING : this test will only work if the test are launched from the project root
        // directory
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class
                .getResource("ivysettings-include-absolute-file.xml"));

        DependencyResolver inc = settings.getResolver("includeworks");
        assertNotNull(inc);
        assertTrue(inc instanceof ChainResolver);
    }

    @Test(expected = Exception.class)
    public void testIncludeMissingFile() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class
                .getResource("ivysettings-include-missing-file.xml"));
    }

    @Test
    public void testIncludeSpecialCharInName() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-include-special.xml"));

        DependencyResolver def = settings.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) def;
        List<DependencyResolver> subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List<String> ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
                ivyPatterns.get(0));

        DependencyResolver inc = settings.getResolver("includeworks");
        assertNotNull(inc);
        assertTrue(inc instanceof ChainResolver);
        chain = (ChainResolver) inc;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("includeworks-fs1", fsInt1.getName());

        ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertLocationEquals(
                "included/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
                ivyPatterns.get(0));

        // properties defined in included file should be available to including file (IVY-780)
        assertEquals("myvalue", settings.getVariable("ivy.test.prop"));
    }

    @Test
    public void testRelativePropertiesFile() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParser.class
                .getResource("ivyconf-properties-relative-to-ivyconf.xml"));

        assertLocationEquals("lib", settings.getVariable("libraries.dir"));
    }

    @Test
    public void testParser() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-parser.xml"));
        assertEquals(ModuleDescriptorParserRegistryTest.MyParser.class.getName(),
                ModuleDescriptorParserRegistry.getInstance().getParsers()[0].getClass().getName());
    }

    @Test
    public void testOutputter() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-outputter.xml"));

        // System.out.println(Arrays.asList(ivy.getReportOutputters()));

        ReportOutputter testOutputter = settings.getReportOutputter("test");
        assertNotNull(testOutputter);
        assertTrue(testOutputter instanceof MyOutputter);
    }

    @Test
    public void testLockingStrategies() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-lock-strategies.xml"));

        LockStrategy lockStrategy = settings.getLockStrategy("test");
        assertNotNull(lockStrategy);
        assertTrue(lockStrategy instanceof MyLockStrategy);
    }

    @Test
    public void testFileAttribute() throws Exception {
        IvySettings settings = new IvySettings();
        File basedir = new File("test").getAbsoluteFile();
        settings.setBaseDir(basedir);
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-packager.xml"));

        DependencyResolver r = settings.getResolver("packager");
        assertNotNull(r);
        assertTrue(r instanceof PackagerResolver);
        PackagerResolver packager = (PackagerResolver) r;
        assertEquals(new File(basedir, "packager/build"), packager.getBuildRoot());
        assertEquals(new File(basedir, "packager/cache"), packager.getResourceCache());
    }

    @Test
    public void testBaseDirVariables() {
        IvySettings settings = new IvySettings();
        settings.setBaseDir(new File("test/base/dir"));
        assertEquals(new File("test/base/dir").getAbsolutePath(), settings.getVariable("basedir"));
        assertEquals(new File("test/base/dir").getAbsolutePath(),
                settings.getVariable("ivy.basedir"));

        settings = new IvySettings();
        settings.setVariable("basedir", new File("other/base/dir").getAbsolutePath());
        settings.setBaseDir(new File("test/base/dir"));
        assertEquals(new File("other/base/dir").getAbsolutePath(), settings.getVariable("basedir"));
        assertEquals(new File("test/base/dir").getAbsolutePath(),
                settings.getVariable("ivy.basedir"));
    }

    /**
     * Test case for IVY-1495.
     * <code>&lt;ttl&gt;</code> containing the <code>matcher</code> attribute,
     * in an ivy settings file, must work as expected.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1495">IVY-1495</a>
     */
    @Test
    public void testCacheTTLMatcherAttribute() throws Exception {
        final IvySettings settings = new IvySettings();
        settings.setBaseDir(new File("test/base/dir"));
        final XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-cache-ttl-matcher.xml"));
        // verify ttl
        final DefaultRepositoryCacheManager cacheManager = (DefaultRepositoryCacheManager) settings.getRepositoryCacheManager("foo");
        assertNotNull("Missing cache manager 'foo'", cacheManager);
        assertEquals("Unexpected default ttl on cache manager", 30000, cacheManager.getDefaultTTL());
        final ModuleRevisionId module1 = new ModuleRevisionId(new ModuleId("foo", "bar"), "*");
        final long module1SpecificTTL = cacheManager.getTTL(module1);
        assertEquals("Unexpected ttl for module " + module1 + " on cache manager", 60000, module1SpecificTTL);
        final ModuleRevisionId module2 = new ModuleRevisionId(new ModuleId("food", "*"), "1.2.4");
        final long module2SpecificTTL = cacheManager.getTTL(module2);
        assertEquals("Unexpected ttl for module " + module2 + " on cache manager", 60000, module2SpecificTTL);
    }

    /**
     * Tests that the <code>timeout-constraint</code> elements in a Ivy settings file are parsed correctly
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testTimeoutConstraints() throws Exception {
        final IvySettings settings = new IvySettings();
        settings.setBaseDir(new File("test/base/dir"));
        final XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-timeout-constraints.xml"));

        final TimeoutConstraint timeout1 = settings.getTimeoutConstraint("test-timeout-1");
        assertNotNull("test-timeout-1 timeout constraint is missing", timeout1);
        assertEquals("Unexpected connection timeout " + timeout1.getConnectionTimeout() + " on time constraint test-timeout-1", 100, timeout1.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + timeout1.getReadTimeout() + " on time constraint test-timeout-1", 500, timeout1.getReadTimeout());


        final TimeoutConstraint timeout2 = settings.getTimeoutConstraint("test-timeout-2");
        assertNotNull("test-timeout-2 timeout constraint is missing", timeout2);
        assertEquals("Unexpected connection timeout " + timeout2.getConnectionTimeout() + " on time constraint test-timeout-2", -1, timeout2.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + timeout2.getReadTimeout() + " on time constraint test-timeout-2", 20, timeout2.getReadTimeout());


        final TimeoutConstraint timeout3 = settings.getTimeoutConstraint("test-timeout-3");
        assertNotNull("test-timeout-3 timeout constraint is missing", timeout3);
        assertEquals("Unexpected connection timeout " + timeout3.getConnectionTimeout() + " on time constraint test-timeout-3", 400, timeout3.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + timeout3.getReadTimeout() + " on time constraint test-timeout-3", -1, timeout3.getReadTimeout());

        final TimeoutConstraint timeout4 = settings.getTimeoutConstraint("test-timeout-4");
        assertNotNull("test-timeout-4 timeout constraint is missing", timeout4);
        assertEquals("Unexpected connection timeout " + timeout4.getConnectionTimeout() + " on time constraint test-timeout-4", -1, timeout4.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + timeout4.getReadTimeout() + " on time constraint test-timeout-4", -1, timeout4.getReadTimeout());

    }

    /**
     * Tests that timeout constraints referenced by resolvers, in an ivy settings file, are
     * processed correctly and the corresponding resolvers use the right timeout constraints
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testResolverTimeoutConstraintRefs() throws Exception {
        final IvySettings settings = new IvySettings();
        settings.setBaseDir(new File("test/base/dir"));
        final XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-timeout-constraints.xml"));

        final URLResolver resolver1 = (URLResolver) settings.getResolver("urlresolver-1");
        assertNotNull("Missing resolver urlresolver-1", resolver1);
        final TimeoutConstraint resolver1Timeouts = resolver1.getTimeoutConstraint();
        assertNotNull("Timeout constraint missing on resolver " + resolver1.getName(), resolver1Timeouts);
        assertEquals("Unexpected connection timeout " + resolver1Timeouts.getConnectionTimeout() + " on resolver " + resolver1.getName(), 400, resolver1Timeouts.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + resolver1Timeouts.getReadTimeout() + " on resolver " + resolver1.getName(), -1, resolver1Timeouts.getReadTimeout());

        final IBiblioResolver resolver2 = (IBiblioResolver) settings.getResolver("ibiblio-resolver");
        assertNotNull("Missing resolver ibiblio-resolver", resolver2);
        final TimeoutConstraint resolver2Timeouts = resolver2.getTimeoutConstraint();
        assertNotNull("Timeout constraint missing on resolver " + resolver2.getName(), resolver2Timeouts);
        assertEquals("Unexpected connection timeout " + resolver2Timeouts.getConnectionTimeout() + " on resolver " + resolver2.getName(), 100, resolver2Timeouts.getConnectionTimeout());
        assertEquals("Unexpected read timeout " + resolver2Timeouts.getReadTimeout() + " on resolver " + resolver2.getName(), 500, resolver2Timeouts.getReadTimeout());

        final FileSystemResolver resolver3 = (FileSystemResolver) settings.getResolver("fs");
        assertNotNull("Missing resolver fs", resolver3);
        final TimeoutConstraint resolver3Timeouts = resolver3.getTimeoutConstraint();
        assertNull("No timeout was expected on resolver " + resolver3.getName(), resolver3Timeouts);

    }

    public static class MyOutputter implements ReportOutputter {
        public void output(ResolveReport report, ResolutionCacheManager cacheMgr,
                           ResolveOptions options) {
        }

        public String getName() {
            return "test";
        }

    }

    public static class MyLockStrategy extends AbstractLockStrategy {
        public boolean lockArtifact(Artifact artifact, File artifactFileToDownload)
                throws InterruptedException {
            return false;
        }

        public void unlockArtifact(Artifact artifact, File artifactFileToDownload) {
        }
    }

    private void assertLocationEquals(String expected, String pattern) throws IOException {
        assertEquals(new File(expected).getCanonicalFile(), new File(pattern).getCanonicalFile());
    }
}
