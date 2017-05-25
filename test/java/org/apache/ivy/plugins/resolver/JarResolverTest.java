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

import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
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

public class JarResolverTest extends TestCase {

    private IvySettings settings;

    private File cache;

    private ResolveEngine engine;

    private ResolveData data;

    private DefaultRepositoryCacheManager cacheManager;

    protected void setUp() throws Exception {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        cache = new File("build/cache");
        data = new ResolveData(engine, new ResolveOptions());
        cache.mkdirs();
        settings.setDefaultCache(cache);
        cacheManager = (DefaultRepositoryCacheManager) settings.getDefaultRepositoryCacheManager();
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testSimpleFile() throws Exception {
        JarResolver resolver = new JarResolver();
        resolver.setName("jarresolver1");
        resolver.setFile(new File("test/jar-repos/jarrepo1.jar").getAbsolutePath());
        resolver.addIvyPattern("[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setSettings(settings);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
    }

    public void testSubdirInFile() throws Exception {
        JarResolver resolver = new JarResolver();
        resolver.setName("jarresolver1_subdir");
        resolver.setFile(new File("test/jar-repos/jarrepo1_subdir.jar").getAbsolutePath());
        resolver.addIvyPattern("1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setSettings(settings);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
    }

    public void testUrl() throws Exception {
        JarResolver resolver = new JarResolver();
        resolver.setName("jarresolver1");
        resolver.setUrl(new File("test/jar-repos/jarrepo1.jar").toURI().toURL().toExternalForm());
        resolver.addIvyPattern("[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setSettings(settings);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
    }

}
