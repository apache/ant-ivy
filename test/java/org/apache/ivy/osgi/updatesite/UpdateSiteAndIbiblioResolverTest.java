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
package org.apache.ivy.osgi.updatesite;

import java.io.File;
import java.text.ParseException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;

import junit.framework.TestCase;

public class UpdateSiteAndIbiblioResolverTest extends TestCase {

    private IvySettings settings;

    private UpdateSiteResolver resolver;

    IBiblioResolver resolver2;

    private File cache;

    private Ivy ivy;

    private ResolveData data;

    ChainResolver chain;

    public void setUp() throws Exception {
        settings = new IvySettings();

        chain = new ChainResolver();
        chain.setName("chain");
        chain.setSettings(settings);

        resolver = new UpdateSiteResolver();
        resolver.setName("ivyde-repo");
        resolver.setUrl(new File("test/test-p2/ivyde-repo").toURI().toURL().toExternalForm());
        resolver.setSettings(settings);

        resolver2 = new IBiblioResolver();
        resolver2.setName("maven2");
        settings.setVariable("ivy.ibiblio.default.artifact.root", "https://repo1.maven.org/maven2/");
        settings.setVariable("ivy.ibiblio.default.artifact.pattern",
            "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");
        resolver2.setSettings(settings);

        chain.add(resolver);
        chain.add(resolver2);

        settings.addResolver(chain);

        settings.setDefaultResolver("chain");

        cache = new File("build/cache");
        cache.mkdirs();
        settings.setDefaultCache(cache);

        ivy = new Ivy();
        ivy.setSettings(settings);
        ivy.bind();

        ivy.getResolutionCacheManager().clean();
        RepositoryCacheManager[] caches = settings.getRepositoryCacheManagers();
        for (int i = 0; i < caches.length; i++) {
            caches[i].clean();
        }

        data = new ResolveData(ivy.getResolveEngine(), new ResolveOptions());
    }

    public void testArtifactRef() throws ParseException {

        // Simple Dependency for ibiblio
        ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("log4j", "log4j", "1.2.16");
        ResolvedModuleRevision rmr1 = chain.getDependency(new DefaultDependencyDescriptor(mrid1,
                false), data);

        // Simple Dependency for updatesite
        ModuleRevisionId mrid2 = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy", "2.0.0.final_20090108225011");
        ResolvedModuleRevision rmr2 = chain.getDependency(new DefaultDependencyDescriptor(mrid2,
                false), data);

        assertNotNull(rmr1);
        assertNotNull(rmr2);

        Artifact[] artifacts1 = rmr1.getDescriptor().getArtifacts("default");
        Artifact[] artifacts2 = rmr2.getDescriptor().getArtifacts("default");

        chain.exists(artifacts2[0]);
        chain.exists(artifacts1[0]);
    }

}
