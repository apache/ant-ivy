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
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.plugins.resolver.DependencyResolver;

import junit.framework.TestCase;

public class UpdateSiteResolverTest extends TestCase {

    private IvySettings settings;

    private UpdateSiteResolver resolver;

    private File cache;

    private Ivy ivy;

    private ResolveData data;

    public void setUp() throws Exception {
        settings = new IvySettings();

        resolver = new UpdateSiteResolver();
        resolver.setName("ivyde-repo");
        resolver.setUrl(new File("test/test-p2/ivyde-repo").toURI().toURL().toExternalForm());
        resolver.setSettings(settings);
        settings.addResolver(resolver);

        settings.setDefaultResolver("ivyde-repo");

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

    public void testListOrganization() throws Exception {
        OrganisationEntry[] orgs = resolver.listOrganisations();
        assertEquals(2, orgs.length);
        assertTrue((orgs[0].getOrganisation().equals(BundleInfo.BUNDLE_TYPE) && orgs[1]
                .getOrganisation().equals(BundleInfo.PACKAGE_TYPE))
                || (orgs[0].getOrganisation().equals(BundleInfo.PACKAGE_TYPE) && orgs[1]
                        .getOrganisation().equals(BundleInfo.BUNDLE_TYPE)));
    }

    public void testListModules() throws Exception {
        ModuleEntry[] modules = resolver.listModules(new OrganisationEntry(resolver,
                BundleInfo.BUNDLE_TYPE));
        assertEquals(3, modules.length);
        modules = resolver.listModules(new OrganisationEntry(resolver, BundleInfo.PACKAGE_TYPE));
        assertEquals(64, modules.length);
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

    public void testResolve() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(BundleInfo.BUNDLE_TYPE,
            "org.apache.ivy", "2.0.0.final_20090108225011");
        genericTestResolveDownload(resolver, mrid);
    }
}
