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
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

/**
 * Tests ChainResolver
 */
public class ChainResolverTest extends TestCase {
    private IvySettings _settings;

    private ResolveEngine _engine;

    private ResolveData _data;

    private File _cache;

    protected void setUp() throws Exception {
        _settings = new IvySettings();
        _engine = new ResolveEngine(_settings, new EventManager(), new SortEngine(_settings));
        _cache = new File("build/cache");
        _data = new ResolveData(_engine, new ResolveOptions().setCache(CacheManager.getInstance(
            _settings, _cache)));
        _cache.mkdirs();
        _settings.setDefaultCache(_cache);
    }

    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    public void testOrderFromConf() throws Exception {
        new XmlSettingsParser(_settings).parse(ChainResolverTest.class
                .getResource("chainresolverconf.xml"));
        DependencyResolver resolver = _settings.getResolver("chain");
        assertNotNull(resolver);
        assertTrue(resolver instanceof ChainResolver);
        ChainResolver chain = (ChainResolver) resolver;
        assertResolversSizeAndNames(chain, 3);
    }

    private void assertResolversSizeAndNames(ChainResolver chain, int size) {
        List resolvers = chain.getResolvers();
        assertEquals(size, resolvers.size());
        for (int i = 0; i < resolvers.size(); i++) {
            DependencyResolver r = (DependencyResolver) resolvers.get(i);
            assertEquals(String.valueOf(i + 1), r.getName());
        }
    }

    public void testName() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setSettings(_settings);
        chain.setName("chain");
        assertEquals("chain", chain.getName());
    }

    public void testResolveOrder() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null),
                MockResolver.buildMockResolver("2", true, null),
                MockResolver.buildMockResolver("3", true, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), resolvers[0].askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), resolvers[1].askedDeps);
        assertTrue(resolvers[2].askedDeps.isEmpty());
    }

    public void testLatestTimeResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setLatestStrategy(new LatestTimeStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, new GregorianCalendar(2005, 1, 20)
                        .getTime()),
                MockResolver.buildMockResolver("2", false, null),
                MockResolver.buildMockResolver("3", true, new GregorianCalendar(2005, 1, 25)
                        .getTime()), // younger -> should the one kept
                MockResolver.buildMockResolver("4", false, null),
                MockResolver.buildMockResolver("5", true, new GregorianCalendar(2005, 1, 22)
                        .getTime()),
                MockResolver.buildMockResolver("6", true, new GregorianCalendar(2005, 1, 18)
                        .getTime()), MockResolver.buildMockResolver("7", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("3", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }

    public void testLatestRevisionResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, ModuleRevisionId.newInstance("org",
                    "mod", "1"), new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver("2", false, null),
                MockResolver.buildMockResolver("3", true, ModuleRevisionId.newInstance("org",
                    "mod", "2"), new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver("4", false, null),
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest ->
                // should the
                // one kept
                MockResolver.buildMockResolver("6", true, ModuleRevisionId.newInstance("org",
                    "mod", "3"), new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }

    public void testWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null),
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // latest
                // ->
                // but
                // default
                MockResolver.buildMockResolver("3", false, null),
                MockResolver.buildMockResolver("4", false, null),
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest ->
                // should the
                // one kept
                MockResolver.buildMockResolver("6", false, null),
                MockResolver.buildMockResolver("7", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < 5; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
        for (int i = 5; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }

    public void testLatestWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, ModuleRevisionId.newInstance("org",
                    "mod", "1"), new GregorianCalendar(2005, 1, 20).getTime()),
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // latest
                // ->
                // but
                // default
                MockResolver.buildMockResolver("3", true, ModuleRevisionId.newInstance("org",
                    "mod", "2"), new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver("4", false, null),
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest ->
                // should the
                // one kept
                MockResolver.buildMockResolver("6", true, ModuleRevisionId.newInstance("org",
                    "mod", "3"), new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }

    public void testFixedWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null),
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // default
                MockResolver.buildMockResolver("3", false, null),
                MockResolver.buildMockResolver("4", true, ModuleRevisionId.newInstance("org",
                    "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // not default
                // -> should the
                // one kept
                MockResolver.buildMockResolver("5", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("4", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < 4; i++) {
            assertEquals("invalid asked dependencies for " + resolvers[i], ddAsList,
                resolvers[i].askedDeps);
        }
        for (int i = 4; i < resolvers.length; i++) {
            assertTrue("invalid asked dependencies for " + resolvers[i], resolvers[i].askedDeps
                    .isEmpty());
        }
    }

    public void testFixedWithDefaultAndRealResolver() throws Exception {
        // test case for IVY-206
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);

        // no ivy pattern for first resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setSettings(_settings);

        resolver
                .addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        // second resolver has an ivy pattern and will thus find the real module, which should be
        // kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setSettings(_settings);

        resolver
                .addIvyPattern("test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver
                .addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        _settings.addResolver(chain);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org1", "mod1.1", "1.0"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
    }

    public void testReturnFirst() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setReturnFirst(true);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, new GregorianCalendar(2005, 1, 20)
                        .getTime()),
                MockResolver.buildMockResolver("2", false, null),
                MockResolver.buildMockResolver("3", true, new GregorianCalendar(2005, 1, 25)
                        .getTime()), // younger -> should the one kept
                MockResolver.buildMockResolver("4", false, null),
                MockResolver.buildMockResolver("5", true, new GregorianCalendar(2005, 1, 22)
                        .getTime()),
                MockResolver.buildMockResolver("6", true, new GregorianCalendar(2005, 1, 18)
                        .getTime()), MockResolver.buildMockResolver("7", false, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("1", rmr.getResolver().getName());
        for (int i = 1; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }

    public void testReturnFirstWithDefaultAndCacheAndRealResolver() throws Exception {
        // test case for IVY-207

        // 1 ---- we first do a first resolve which puts a default file in cache
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);

        // no ivy pattern for resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("old");
        resolver.setSettings(_settings);

        resolver
                .addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        _settings.addResolver(chain);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org1", "mod1.1", "1.0"), false);
        chain.getDependency(dd, _data);

        // 2 ---- now we ask to resolve dependencies with a chain in return first mode, in which the
        // first resolver
        // is not able to find the module, but the second is

        chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setReturnFirst(true);

        // no pattern for first resolver: will not find the module
        resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setSettings(_settings);

        chain.add(resolver);

        // second resolver will find the real module, which should be kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setSettings(_settings);

        resolver
                .addIvyPattern("test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver
                .addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);

        _settings.addResolver(chain);

        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
    }

    public void testDual() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(_settings);
        chain.setDual(true);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null),
                MockResolver.buildMockResolver("2", true, null),
                MockResolver.buildMockResolver("3", true, null)};
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId
                .newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals("chain", rmr.getArtifactResolver().getName());
    }

}
