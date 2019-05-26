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
package org.apache.ivy.plugins.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.ivy.util.MockMessageLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests ChainResolver
 */
public class ChainResolverTest extends AbstractDependencyResolverTest {

    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    @Before
    public void setUp() {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        TestHelper.createCache();
        data = new ResolveData(engine, new ResolveOptions());
        settings.setDefaultCache(TestHelper.cache);
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testOrderFromConf() throws Exception {
        new XmlSettingsParser(settings).parse(ChainResolverTest.class
                .getResource("chainresolverconf.xml"));
        DependencyResolver resolver = settings.getResolver("chain");
        assertNotNull(resolver);
        assertTrue(resolver instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) resolver;
        assertResolversSizeAndNames(chain, 3);
    }

    private void assertResolversSizeAndNames(ChainResolver chain, int size) {
        List<DependencyResolver> resolvers = chain.getResolvers();
        assertEquals(size, resolvers.size());
        for (int i = 0; i < resolvers.size(); i++) {
            DependencyResolver r = resolvers.get(i);
            assertEquals(String.valueOf(i + 1), r.getName());
        }
    }

    @Test
    public void testName() {
        ChainResolver chain = new ChainResolver();
        chain.setSettings(settings);
        chain.setName("chain");
        assertEquals("chain", chain.getName());
    }

    @Test
    public void testResolveOrder() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", false, null),
                MockResolver.buildMockResolver(settings, "2", true, null),
                MockResolver.buildMockResolver(settings, "3", true, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals(Collections.<DependencyDescriptor> singletonList(dd), resolvers[0].askedDeps);
        assertEquals(Collections.<DependencyDescriptor> singletonList(dd), resolvers[1].askedDeps);
        assertTrue(resolvers[2].askedDeps.isEmpty());
    }

    @Test
    public void testLatestTimeResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        settings.setDefaultLatestStrategy(new LatestTimeStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", true,
                    new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver(settings, "2", false, null),
                MockResolver.buildMockResolver(settings, "3", true,
                    new GregorianCalendar(2005, 1, 25).getTime()),
                    // younger -> should the one kept
                MockResolver.buildMockResolver(settings, "4", false, null),
                MockResolver.buildMockResolver(settings, "5", true,
                    new GregorianCalendar(2005, 1, 22).getTime()),
                MockResolver.buildMockResolver(settings, "6", true,
                    new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver(settings, "7", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("3", rmr.getResolver().getName());
        List<DependencyDescriptor> ddAsList = Collections.<DependencyDescriptor> singletonList(dd);
        for (MockResolver resolver : resolvers) {
            assertEquals(ddAsList, resolver.askedDeps);
        }
    }

    @Test
    public void testLatestRevisionResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", true,
                    ModuleRevisionId.newInstance("org", "mod", "1"),
                    new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver(settings, "2", false, null),
                MockResolver.buildMockResolver(settings, "3", true,
                    ModuleRevisionId.newInstance("org", "mod", "2"),
                    new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver(settings, "4", false, null),
                MockResolver.buildMockResolver(settings, "5", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime()),
                    // latest -> should the one kept
                MockResolver.buildMockResolver(settings, "6", true,
                    ModuleRevisionId.newInstance("org", "mod", "3"),
                    new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver(settings, "7", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List<DependencyDescriptor> ddAsList = Collections.<DependencyDescriptor> singletonList(dd);
        for (MockResolver resolver : resolvers) {
            assertEquals(ddAsList, resolver.askedDeps);
        }
    }

    @Test
    public void testWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", false, null),
                MockResolver.buildMockResolver(settings, "2", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime(), true),
                    // latest -> but default
                MockResolver.buildMockResolver(settings, "3", false, null),
                MockResolver.buildMockResolver(settings, "4", false, null),
                MockResolver.buildMockResolver(settings, "5", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime()),
                    // latest -> should be the one kept
                MockResolver.buildMockResolver(settings, "6", false, null),
                MockResolver.buildMockResolver(settings, "7", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List<DependencyDescriptor> ddAsList = Collections.<DependencyDescriptor> singletonList(dd);
        for (int i = 0; i < 5; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
        for (int i = 5; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }

    @Test
    public void testLatestWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", true,
                    ModuleRevisionId.newInstance("org", "mod", "1"),
                    new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver(settings, "2", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime(), true),
                    // latest -> but default
                MockResolver.buildMockResolver(settings, "3", true,
                    ModuleRevisionId.newInstance("org", "mod", "2"),
                    new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver(settings, "4", false, null),
                MockResolver.buildMockResolver(settings, "5", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime()),
                    // latest -> should the one kept
                MockResolver.buildMockResolver(settings, "6", true,
                    ModuleRevisionId.newInstance("org", "mod", "3"),
                    new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver(settings, "7", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List<DependencyDescriptor> ddAsList = Collections.<DependencyDescriptor> singletonList(dd);
        for (MockResolver resolver : resolvers) {
            assertEquals(ddAsList, resolver.askedDeps);
        }
    }

    @Test
    public void testFixedWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", false, null),
                MockResolver.buildMockResolver(settings, "2", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime(), true),
                    // default
                MockResolver.buildMockResolver(settings, "3", false, null),
                MockResolver.buildMockResolver(settings, "4", true,
                    ModuleRevisionId.newInstance("org", "mod", "4"),
                    new GregorianCalendar(2005, 1, 22).getTime()),
                    // not default -> should be the one kept
                MockResolver.buildMockResolver(settings, "5", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("4", rmr.getResolver().getName());
        List<DependencyDescriptor> ddAsList = Collections.<DependencyDescriptor> singletonList(dd);
        for (int i = 0; i < 4; i++) {
            assertEquals("invalid asked dependencies for " + resolvers[i], ddAsList,
                resolvers[i].askedDeps);
        }
        for (int i = 4; i < resolvers.length; i++) {
            assertTrue("invalid asked dependencies for " + resolvers[i],
                resolvers[i].askedDeps.isEmpty());
        }
    }

    /**
     * Test case for IVY-206.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-206">IVY-206</a>
     */
    @Test
    public void testFixedWithDefaultAndRealResolver() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);

        // no ivy pattern for first resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setSettings(settings);

        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        // second resolver has an ivy pattern and will thus find the real module, which should be
        // kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setSettings(settings);

        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        settings.addResolver(chain);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
    }

    @Test
    public void testUseCache() throws Exception {
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), false);
        // resolve dependency twice
        chainToPutDefaultModuleInCache().getDependency(dd, data);

        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        MockResolver[] resolvers = new MockResolver[] {MockResolver.buildMockResolver(settings,
            "1", true, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        chain.getDependency(dd, data);
        // should not have asked any dependency, should have hit the cache
        assertEquals(Collections.emptyList(), resolvers[0].askedDeps);
    }

    @Test
    public void testReturnFirst() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setReturnFirst(true);

        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", true,
                    new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver(settings, "2", false, null),
                MockResolver.buildMockResolver(settings, "3", true,
                    new GregorianCalendar(2005, 1, 25).getTime()),
                    // younger -> should the one kept
                MockResolver.buildMockResolver(settings, "4", false, null),
                MockResolver.buildMockResolver(settings, "5", true,
                    new GregorianCalendar(2005, 1, 22).getTime()),
                MockResolver.buildMockResolver(settings, "6", true,
                    new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver(settings, "7", false, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("1", rmr.getResolver().getName());
        for (int i = 1; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }

    /**
     * Test case for IVY-389.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-389">IVY-389</a>
     */
    @Test
    public void testReturnFirstWithDefaultAndCacheAndRealResolver() throws Exception {
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), false);

        // 1 ---- we first do a first resolve which puts a default file in cache
        chainToPutDefaultModuleInCache().getDependency(dd, data);

        // 2 ---- now we ask to resolve dependencies with a chain in return first mode with check
        // modified = false, in which the first resolver is not able to find the module, but the
        // second is
        ChainResolver chain = returnFirstChain();

        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        // the module returned should be the default one found in cache since check modified is
        // false
        assertTrue(rmr.getDescriptor().isDefault());
    }

    /**
     * Test case for IVY-207.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-207">IVY-207</a>
     */
    @Test
    public void testReturnFirstWithCheckModifiedAndDefaultAndCacheAndRealResolver()
            throws Exception {
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), false);

        // 1 ---- we first do a first resolve which puts a default file in cache
        chainToPutDefaultModuleInCache().getDependency(dd, data);

        // 2 ---- now we ask to resolve dependencies with a chain in return first mode, in which the
        // first resolver is not able to find the module, but the second is
        ChainResolver chain = returnFirstChain();
        chain.setCheckmodified(true);

        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertFalse(rmr.getDescriptor().isDefault());
        assertEquals("2", rmr.getResolver().getName());
    }

    private ChainResolver returnFirstChain() {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setReturnFirst(true);

        // no pattern for first resolver: will not find the module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setSettings(settings);

        chain.add(resolver);

        // second resolver will find the real module, which should be kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setSettings(settings);

        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        settings.addResolver(chain);
        return chain;
    }

    private ChainResolver chainToPutDefaultModuleInCache() {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);

        // no ivy pattern for resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("old");
        resolver.setSettings(settings);

        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        settings.addResolver(chain);
        return chain;
    }

    @Test
    public void testDual() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setDual(true);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver(settings, "1", false, null),
                MockResolver.buildMockResolver(settings, "2", true, null),
                MockResolver.buildMockResolver(settings, "3", true, null)};
        for (MockResolver resolver : resolvers) {
            chain.add(resolver);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals("chain", rmr.getArtifactResolver().getName());
    }

    @Test
    public void testDownloadWithDual() {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);
        chain.setDual(true);

        // first resolver has only an artifact pattern which don't lead to anything: it won't find
        // the module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setSettings(settings);
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/nowhere/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        chain.add(resolver);

        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setSettings(settings);

        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        settings.addResolver(chain);

        MockMessageLogger mockLogger = new MockMessageLogger();
        IvyContext.getContext().getIvy().getLoggerEngine().setDefaultLogger(mockLogger);
        DownloadReport report = chain.download(
            new Artifact[] {new DefaultArtifact(ModuleRevisionId.parse("org1#mod1.1;1.0"),
                    new Date(), "mod1.1", "jar", "jar")}, new DownloadOptions());
        assertNotNull(report);
        assertEquals(1, report.getArtifactsReports().length);
        assertEquals(DownloadStatus.SUCCESSFUL, report.getArtifactsReports()[0].getDownloadStatus());
        mockLogger.assertLogDoesntContain("[FAILED     ] org1#mod1.1;1.0!mod1.1.jar");
    }

}
