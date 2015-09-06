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

import junit.framework.TestCase;

public class P2DescriptorTest extends TestCase {

    private File cache;

    private IvySettings settings;

    private UpdateSiteResolver p2SourceResolver;

    private UpdateSiteResolver p2ZippedResolver;

    private UpdateSiteResolver p2WithPackedResolver;

    private Ivy ivy;

    private ResolveData data;

    protected void setUp() throws Exception {
        settings = new IvySettings();

        p2SourceResolver = new UpdateSiteResolver();
        p2SourceResolver.setName("p2-sources");
        p2SourceResolver.setUrl(new File("test/test-p2/sources").toURI().toURL().toExternalForm());
        p2SourceResolver.setSettings(settings);
        settings.addResolver(p2SourceResolver);

        p2ZippedResolver = new UpdateSiteResolver();
        p2ZippedResolver.setName("p2-zipped");
        p2ZippedResolver.setUrl(new File("test/test-p2/zipped").toURI().toURL().toExternalForm());
        p2ZippedResolver.setSettings(settings);
        settings.addResolver(p2ZippedResolver);

        p2WithPackedResolver = new UpdateSiteResolver();
        p2WithPackedResolver.setName("p2-with-packed");
        p2WithPackedResolver.setUrl(new File("test/test-p2/packed").toURI().toURL()
                .toExternalForm());
        p2WithPackedResolver.setSettings(settings);
        settings.addResolver(p2WithPackedResolver);

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

    @Override
    protected void tearDown() throws Exception {
        ivy.getLoggerEngine().sumupProblems();
    }

    public void testResolveSource() throws Exception {
        settings.setDefaultResolver("p2-sources");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy", "2.2.0.final_20100923230623");

        ResolvedModuleRevision rmr = p2SourceResolver.getDependency(
            new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        assertEquals(2, rmr.getDescriptor().getAllArtifacts().length);

        DownloadReport report = p2SourceResolver.download(rmr.getDescriptor().getAllArtifacts(),
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
            DownloadReport report2 = p2SourceResolver.download(new Artifact[] {artifact},
                new DownloadOptions());
            assertNotNull(report2);

            assertEquals(1, report2.getArtifactsReports().length);

            ar = report2.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
        }
    }

    public void testResolveNotZipped() throws Exception {
        settings.setDefaultResolver("p2-zipped");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.eclipse.e4.core.services", "1.0.0.v20120521-2346");

        ResolvedModuleRevision rmr = p2ZippedResolver.getDependency(
            new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        assertEquals(1, rmr.getDescriptor().getAllArtifacts().length);

        DownloadOptions options = new DownloadOptions();
        DownloadReport report = p2ZippedResolver.download(rmr.getDescriptor().getAllArtifacts(),
            options);
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        Artifact artifact = rmr.getDescriptor().getAllArtifacts()[0];
        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());
        assertNull(ar.getUnpackedLocalFile());
    }

    public void testResolveZipped() throws Exception {
        settings.setDefaultResolver("p2-zipped");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ant", "1.8.3.v20120321-1730");

        ResolvedModuleRevision rmr = p2ZippedResolver.getDependency(
            new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        assertEquals(2, rmr.getDescriptor().getAllArtifacts().length);

        DownloadOptions options = new DownloadOptions();
        DownloadReport report = p2ZippedResolver.download(rmr.getDescriptor().getAllArtifacts(),
            options);
        assertNotNull(report);

        assertEquals(2, report.getArtifactsReports().length);

        for (int i = 0; i < 2; i++) {
            Artifact artifact = rmr.getDescriptor().getAllArtifacts()[i];
            ArtifactDownloadReport ar = report.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());
            // only the binary get unpacked
            if (ar.getArtifact().getType().equals("source")) {
                assertNull(ar.getUnpackedLocalFile());
            } else {
                assertNotNull(ar.getUnpackedLocalFile());
            }
        }
    }

    public void testResolvePacked() throws Exception {
        settings.setDefaultResolver("p2-with-packed");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE, "org.junit",
            "4.10.0.v4_10_0_v20120426-0900");

        ResolvedModuleRevision rmr = p2WithPackedResolver.getDependency(
            new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        assertEquals(1, rmr.getDescriptor().getAllArtifacts().length);

        DownloadOptions options = new DownloadOptions();
        DownloadReport report = p2WithPackedResolver.download(
            rmr.getDescriptor().getAllArtifacts(), options);
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        Artifact artifact = rmr.getDescriptor().getAllArtifacts()[0];
        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());
        assertNotNull(ar.getUnpackedLocalFile());
    }
}
