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
package org.apache.ivy.core.settings;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.report.ResolveReport;
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
import org.apache.ivy.plugins.resolver.MockResolver;
import org.apache.ivy.plugins.version.ChainVersionMatcher;
import org.apache.ivy.plugins.version.MockVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;

/**
 * TODO write javadoc
 */
public class XmlSettingsParserTest extends TestCase {
    public void test() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-test.xml"));

        File defaultCache = settings.getDefaultCache();
        assertNotNull(defaultCache);
        assertEquals("mycache", defaultCache.getName());

        assertFalse(settings.isCheckUpToDate());
        assertFalse(settings.doValidate());

        assertEquals("[module]/ivys/ivy-[revision].xml", settings.getCacheIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]", settings
                .getCacheArtifactPattern());

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
        List ivyPatterns = fsres.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("lib/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns.get(0));

        LatestStrategy strategy = fsres.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestRevisionStrategy);

        DependencyResolver internal = settings.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) internal;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("int1", fsInt1.getName());
        assertEquals(1, fsInt1.getIvyPatterns().size());
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", fsInt1
                .getIvyPatterns().get(0));
        assertEquals("int2", ((DependencyResolver) subresolvers.get(1)).getName());

        strategy = fsInt1.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestTimeStrategy);

        assertEquals("libraries", settings.getResolver(new ModuleId("unknown", "lib")).getName());
        assertEquals("internal", settings.getResolver(new ModuleId("apache", "ant")).getName());
        assertEquals("int1", settings.getResolver(new ModuleId("apache", "ivy")).getName());
        assertEquals("int1", settings.getResolver(new ModuleId("apache", "ivyde")).getName());
    }

    public void testNoOrgInModule() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        try {
            parser.parse(XmlSettingsParserTest.class
                    .getResource("ivysettings-no-org-in-module.xml"));
            fail("no organisation in module is supposed to raise an exception");
        } catch (ParseException e) {
            assertTrue("bad exception message: " + e.getMessage(), e.getMessage().indexOf(
                "'organisation'") != -1);
        }
    }

    public void testNoNameInModule() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        try {
            parser.parse(XmlSettingsParserTest.class
                    .getResource("ivysettings-no-name-in-module.xml"));
            fail("no name in module is supposed to raise an exception");
        } catch (ParseException e) {
            assertTrue("bad exception message: " + e.getMessage(),
                e.getMessage().indexOf("'name'") != -1);
        }
    }

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
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        assertEquals("mock1", ((DependencyResolver) subresolvers.get(0)).getName());
        assertEquals("mock2", ((DependencyResolver) subresolvers.get(1)).getName());
        assertTrue(subresolvers.get(0) instanceof MockResolver);
        assertTrue(subresolvers.get(1) instanceof MockResolver);
    }

    public void testStatuses() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-status.xml"));

        assertEquals("bronze", settings.getStatusManager().getDefaultStatus());
        assertEquals(0, settings.getStatusManager().getPriority("gold"));
        assertEquals(1, settings.getStatusManager().getPriority("silver"));
        assertEquals(2, settings.getStatusManager().getPriority("bronze"));
        assertEquals(false, settings.getStatusManager().isIntegration("gold"));
        assertEquals(false, settings.getStatusManager().isIntegration("silver"));
        assertEquals(true, settings.getStatusManager().isIntegration("bronze"));
    }

    public void testConflictManager() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-conflict-manager.xml"));

        assertEquals("latest-time", settings.getConflictManager(new ModuleId("apache", "ivyde"))
                .getName());
        assertEquals("all", settings.getConflictManager(new ModuleId("apache", "ant")).getName());
    }

    public void testCache() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-cache.xml"));

        File defaultCache = settings.getDefaultCache();
        assertNotNull(defaultCache);
        assertEquals("mycache", defaultCache.getName());
        assertEquals(new File(defaultCache, "repository"), settings.getRepositoryCacheRoot(defaultCache));
        assertEquals(new File(defaultCache, "resolution"), settings.getResolutionCacheRoot(defaultCache));
        assertEquals("artifact-lock", settings.getDefaultLockStrategy().getName());

        assertEquals("[module]/ivys/ivy-[revision].xml", settings.getCacheIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]", settings
                .getCacheArtifactPattern());
    }

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

    public void testRef() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-ref.xml"));

        DependencyResolver internal = settings.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) internal;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("fs", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns
                .get(0));

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
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns
                .get(0));
    }

    public void testMacro() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-macro.xml"));

        DependencyResolver def = settings.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) def;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
            ivyPatterns.get(0));

        FileSystemResolver fsInt2 = (FileSystemResolver) subresolvers.get(1);
        assertEquals("default-fs2", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/secondrep/[organisation]/[module]/[type]s/ivy-[revision].xml",
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
        assertEquals("path/to/secondrep/[module]/[type]s/ivy-[revision].xml", ivyPatterns.get(0));
    }

    public void testMacroAndRef() throws Exception {
        // test case for IVY-319
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
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        shared = (DependencyResolver) subresolvers.get(0);
        assertEquals("shared", shared.getName());
        assertTrue(shared instanceof FileSystemResolver);
    }

    public void testInclude() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-include.xml"));

        DependencyResolver def = settings.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) def;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver) subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
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
        assertEquals("included/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]",
            ivyPatterns.get(0));
    }

    public void testParser() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-parser.xml"));
        assertEquals(ModuleDescriptorParserRegistryTest.MyParser.class.getName(),
            ModuleDescriptorParserRegistry.getInstance().getParsers()[0].getClass().getName());
    }

    public void testOutputter() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-outputter.xml"));

        // System.out.println(Arrays.asList(ivy.getReportOutputters()));

        ReportOutputter testOutputter = settings.getReportOutputter("test");
        assertNotNull(testOutputter);
        assertTrue(testOutputter instanceof MyOutputter);
    }

    public void testLockingStrategies() throws Exception {
        IvySettings settings = new IvySettings();
        XmlSettingsParser parser = new XmlSettingsParser(settings);
        parser.parse(XmlSettingsParserTest.class.getResource("ivysettings-lock-strategies.xml"));

        LockStrategy lockStrategy = settings.getLockStrategy("test");
        assertNotNull(lockStrategy);
        assertTrue(lockStrategy instanceof MyLockStrategy);
    }

    public static class MyOutputter implements ReportOutputter {
        public void output(ResolveReport report, ResolutionCacheManager cacheMgr) {
        }

        public String getName() {
            return "test";
        }

    }
    
    public static class MyLockStrategy extends AbstractLockStrategy {
        public boolean lockArtifact(Artifact artifact, File artifactFileToDownload) throws InterruptedException {
            return false;
        }

        public void unlockArtifact(Artifact artifact, File artifactFileToDownload) {
        }
    }
}
