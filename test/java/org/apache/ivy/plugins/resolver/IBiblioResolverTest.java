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

import static org.apache.ivy.plugins.resolver.IBiblioResolver.DEFAULT_M2_ROOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.util.MockMessageLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class IBiblioResolverTest extends AbstractDependencyResolverTest {
    // remote.test

    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    @Before
    public void setUp() {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        data = new ResolveData(engine, new ResolveOptions());
        TestHelper.createCache();
        settings.setDefaultCache(TestHelper.cache);
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testDefaults() {
        IBiblioResolver resolver = new IBiblioResolver();
        settings.setVariable("ivy.ibiblio.default.artifact.root",
            "http://www.ibiblio.org/mymaven/");
        settings.setVariable("ivy.ibiblio.default.artifact.pattern",
            "[module]/jars/[artifact]-[revision].jar");
        resolver.setSettings(settings);
        List<String> l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/jars/[artifact]-[revision].jar",
            l.get(0));
    }

    @Test
    public void testInitFromConf() throws Exception {
        settings.setVariable("ivy.ibiblio.default.artifact.root", "http://www.ibiblio.org/maven/");
        settings.setVariable("ivy.ibiblio.default.artifact.pattern",
            "[module]/jars/[artifact]-[revision].jar");
        settings.setVariable("my.ibiblio.root", "http://www.ibiblio.org/mymaven/");
        settings.setVariable("my.ibiblio.pattern", "[module]/[artifact]-[revision].jar");
        settings.load(IBiblioResolverTest.class.getResource("ibiblioresolverconf.xml"));
        IBiblioResolver resolver = (IBiblioResolver) settings.getResolver("ibiblioA");
        assertNotNull(resolver);
        List<String> l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/[artifact]-[revision].jar", l.get(0));

        resolver = (IBiblioResolver) settings.getResolver("ibiblioB");
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[organisation]/jars/[artifact]-[revision].jar",
            l.get(0));

        resolver = (IBiblioResolver) settings.getResolver("ibiblioC");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertTrue("Default Maven URL must end with '/'", DEFAULT_M2_ROOT.endsWith("/"));
        assertEquals(DEFAULT_M2_ROOT
            + "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]",
            l.get(0));

        resolver = (IBiblioResolver) settings.getResolver("ibiblioD");
        assertFalse(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/maven/[module]/jars/[artifact]-[revision].jar",
            l.get(0));

        resolver = (IBiblioResolver) settings.getResolver("ibiblioE");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals(
            "http://www.ibiblio.org/mymaven/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]",
            l.get(0));

        resolver = (IBiblioResolver) settings.getResolver("ibiblioF");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals(
            "http://www.ibiblio.org/mymaven/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]",
            l.get(0));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testMaven2Listing() {
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        resolver.setM2compatible(true);
        assertEquals("test", resolver.getName());

        ModuleEntry[] modules = resolver
                .listModules(new OrganisationEntry(resolver, "commons-lang"));
        assertNotNull(modules);
        assertEquals(1, modules.length);
        assertEquals("commons-lang", modules[0].getModule());

        RevisionEntry[] revisions = resolver.listRevisions(modules[0]);
        assertTrue(revisions.length > 0);

        Map otherTokenValues = new HashMap();
        otherTokenValues.put(IvyPatternHelper.ORGANISATION_KEY, "commons-lang");
        String[] values = resolver.listTokenValues(IvyPatternHelper.MODULE_KEY, otherTokenValues);
        assertNotNull(values);
        assertEquals(1, values.length);
        assertEquals("commons-lang", values[0]);

        Map[] valuesMaps = resolver.listTokenValues(new String[] {IvyPatternHelper.MODULE_KEY},
            otherTokenValues);
        Set vals = new HashSet();
        for (Map valuesMap : valuesMaps) {
            vals.add(valuesMap.get(IvyPatternHelper.MODULE_KEY));
        }
        values = (String[]) vals.toArray(new String[vals.size()]);
        assertEquals(1, values.length);
        assertEquals("commons-lang", values[0]);
    }

    @Test
    public void testErrorReport() throws Exception {
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot("http://unknown.host.comx/");
        resolver.setName("test");
        resolver.setM2compatible(true);
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        MockMessageLogger mockMessageImpl = new MockMessageLogger();
        IvyContext.getContext().getIvy().getLoggerEngine().setDefaultLogger(mockMessageImpl);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "commons-fileupload",
            "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNull(rmr);

        mockMessageImpl
                .assertLogContains("tried http://unknown.host.comx/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.pom");
        mockMessageImpl
                .assertLogContains("tried http://unknown.host.comx/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.jar");
    }

}
