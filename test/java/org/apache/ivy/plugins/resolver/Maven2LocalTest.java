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
import java.net.MalformedURLException;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.util.CacheCleaner;

import junit.framework.TestCase;

public class Maven2LocalTest extends TestCase {
    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    private File cache;

    protected void setUp() throws Exception {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        cache = new File("build/cache");
        data = new ResolveData(engine, new ResolveOptions());
        cache.mkdirs();
        settings.setDefaultCache(cache);
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testUseMetadataForListing() throws Exception {
        IBiblioResolver resolver = maven2Resolver();

        ResolvedModuleRevision m = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org.apache", "test-metadata", "latest.integration"),
                false), data);

        assertNotNull(m);
        // should trust the metadata (latest=1.1) instead of listing revisions (latest=1.2)
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-metadata", "1.1"), m.getId());
    }

    public void testNotUseMetadataForListing() throws Exception {
        IBiblioResolver resolver = maven2Resolver();
        resolver.setUseMavenMetadata(false);

        ResolvedModuleRevision m = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org.apache", "test-metadata", "latest.integration"),
                false), data);

        assertNotNull(m);
        // should trust listing revisions (latest=1.2) instead of the metadata (latest=1.1)
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-metadata", "1.2"), m.getId());
    }

    private IBiblioResolver maven2Resolver() throws MalformedURLException {
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setSettings(settings);
        resolver.setName("maven2");
        resolver.setM2compatible(true);
        resolver.setRoot(new File("test/repositories/m2").toURI().toURL().toExternalForm());
        return resolver;
    }
}
