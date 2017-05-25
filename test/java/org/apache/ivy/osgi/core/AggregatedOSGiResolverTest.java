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
package org.apache.ivy.osgi.core;

import java.io.File;
import java.text.ParseException;

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
import org.apache.ivy.osgi.obr.OBRResolver;
import org.apache.ivy.osgi.repo.AbstractOSGiResolver.RequirementStrategy;
import org.apache.ivy.osgi.repo.AggregatedOSGiResolver;
import org.apache.ivy.osgi.updatesite.UpdateSiteResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;

import junit.framework.TestCase;

public class AggregatedOSGiResolverTest extends TestCase {

    private IvySettings settings;

    private File cache;

    private ResolveData data;

    private Ivy ivy;

    private AggregatedOSGiResolver resolver;

    public void setUp() throws Exception {
        settings = new IvySettings();

        OBRResolver bundleResolver = new OBRResolver();
        bundleResolver.setRepoXmlFile(new File("test/test-repo/bundlerepo/repo.xml")
                .getAbsolutePath());
        bundleResolver.setName("bundle");
        bundleResolver.setSettings(settings);
        settings.addResolver(bundleResolver);

        UpdateSiteResolver updatesite = new UpdateSiteResolver();
        updatesite.setUrl(new File("test/test-p2/ivyde-repo").toURI().toURL().toExternalForm());
        updatesite.setName("updatesite");
        updatesite.setSettings(settings);
        settings.addResolver(updatesite);

        OBRResolver repo1 = new OBRResolver();
        repo1.setRepoXmlFile(new File("test/test-repo/multi-osgi/repo1/obr.xml").getAbsolutePath());
        repo1.setName("repo1");
        repo1.setSettings(settings);
        repo1.setRequirementStrategy(RequirementStrategy.noambiguity);
        settings.addResolver(repo1);

        OBRResolver repo2 = new OBRResolver();
        repo2.setRepoXmlFile(new File("test/test-repo/multi-osgi/repo2/obr.xml").getAbsolutePath());
        repo2.setName("repo2");
        repo2.setSettings(settings);
        repo2.setRequirementStrategy(RequirementStrategy.noambiguity);
        settings.addResolver(repo2);

        resolver = new AggregatedOSGiResolver();
        resolver.add(bundleResolver);
        resolver.add(updatesite);
        resolver.add(repo1);
        resolver.add(repo2);
        resolver.setName("multiosgi");
        resolver.setSettings(settings);
        settings.addResolver(resolver);

        settings.setDefaultResolver("multiosgi");

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

    private void genericTestResolveDownload(DependencyResolver resolver, ModuleRevisionId mrid)
            throws ParseException {
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        Artifact artifact = rmr.getDescriptor().getAllArtifacts()[0];
        DownloadReport report = resolver.download(new Artifact[] {artifact}, new DownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, new DownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testResolveInUpdatesite() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy", "2.0.0.final_20090108225011");
        genericTestResolveDownload(resolver, mrid);
    }

    public void testResolveInObr() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy.osgi.testbundle", "1.2.3");
        genericTestResolveDownload(resolver, mrid);
    }

    public void testCrossResolve() throws Exception {
        ModuleRevisionId mrid1 = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy.osgi.testbundle1", "1.2.3");
        genericTestResolveDownload(resolver, mrid1);

        ModuleRevisionId mrid2 = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy.osgi.testbundle2", "1.2.3");
        genericTestResolveDownload(resolver, mrid2);
    }

}
