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

/**
 * Test for DualResolver
 */
public class DualResolverTest extends AbstractDependencyResolverTest {
    private IvySettings _settings;

    private ResolveEngine _engine;

    private ResolveData _data;

    private File _cache;

    protected void setUp() throws Exception {
        _settings = new IvySettings();
        _engine = new ResolveEngine(_settings, new EventManager(), new SortEngine(_settings));
        _cache = new File("build/cache");
        _data = new ResolveData(_engine, new ResolveOptions());
        _cache.mkdirs();
        _settings.setDefaultCache(_cache);
    }

    public void testFromConf() throws Exception {
        new XmlSettingsParser(_settings).parse(DualResolverTest.class
                .getResource("dualresolverconf.xml"));

        DependencyResolver resolver = _settings.getResolver("dualok");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        DualResolver dual = (DualResolver) resolver;
        assertNotNull(dual.getIvyResolver());
        assertEquals("ivy", dual.getIvyResolver().getName());
        assertNotNull(dual.getArtifactResolver());
        assertEquals("artifact", dual.getArtifactResolver().getName());

        resolver = _settings.getResolver("dualnotenough");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        dual = (DualResolver) resolver;
        assertNotNull(dual.getIvyResolver());
        assertNull(dual.getArtifactResolver());
    }

    public void testFromBadConf() throws Exception {
        try {
            new XmlSettingsParser(_settings).parse(DualResolverTest.class
                    .getResource("dualresolverconf-bad.xml"));
            fail("bad dual resolver configuration should raise exception");
        } catch (Exception ex) {
            // ok -> bad conf has raised an exception
        }
    }

    public void testBad() throws Exception {
        DualResolver dual = new DualResolver();
        dual.setIvyResolver(new IBiblioResolver());
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        try {
            dual.getDependency(dd, _data);
            fail("bad dual resolver configuration should raise exception");
        } catch (Exception ex) {
            // ok -> should have raised an exception
        }
    }

    public void testResolve() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver(_settings, "ivy", true,
            new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver(_settings, "artifact",
            false, new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);

        assertNotNull(rmr);
        assertEquals(dual, rmr.getArtifactResolver());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertTrue(artifactResolver.askedDeps.isEmpty());
    }

    public void testResolveFromArtifact() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver(_settings, "ivy", false,
            new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver(_settings, "artifact", true,
            new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);

        assertNotNull(rmr);
        assertEquals(artifactResolver, rmr.getResolver());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), artifactResolver.askedDeps);
    }

    public void testResolveFail() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver(_settings, "ivy", false,
            new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver(_settings, "artifact",
            false, new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org", "mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);

        assertNull(rmr);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), artifactResolver.askedDeps);
    }
}
