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
package org.apache.ivy.osgi.p2;

import java.io.File;
import java.text.ParseException;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.updatesite.UpdateSiteResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;

public class P2DescriptorTest extends TestCase {

    private File cache;

    private IvySettings settings;

    private UpdateSiteResolver resolver;

    private Ivy ivy;

    private ResolveData data;

    protected void setUp() throws Exception {
        settings = new IvySettings();

        resolver = new UpdateSiteResolver();
        resolver.setName("p2-sources");
        resolver.setUrl(new File("test/test-p2/sources").toURL().toExternalForm());
        resolver.setSettings(settings);
        settings.addResolver(resolver);

        settings.setDefaultResolver("p2-sources");

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

    public void testResolve() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy", "2.2.0.final_20100923230623");

        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        assertEquals(2, rmr.getDescriptor().getAllArtifacts().length);

        DownloadReport report = resolver.download(rmr.getDescriptor().getAllArtifacts(),
            new DownloadOptions());
        assertNotNull(report);

        assertEquals(2, report.getArtifactsReports().length);

        for (int i = 0; i < 2; i++) {
            Artifact artifact = rmr.getDescriptor().getAllArtifacts()[i];
            ArtifactDownloadReport ar = report.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

            // test to ask to download again, should use cache
            DownloadReport report2 = resolver.download(new Artifact[] {artifact},
                new DownloadOptions());
            assertNotNull(report2);

            assertEquals(1, report2.getArtifactsReports().length);

            ar = report2.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
        }
    }
}
