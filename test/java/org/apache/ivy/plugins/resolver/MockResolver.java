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
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

public class MockResolver extends AbstractResolver {
    static MockResolver buildMockResolver(ResolverSettings settings, String name,
            boolean findRevision, final Date publicationDate) {
        return buildMockResolver(settings, name, findRevision,
            ModuleRevisionId.newInstance("test", "test", "test"), publicationDate);
    }

    static MockResolver buildMockResolver(ResolverSettings settings, String name,
            boolean findRevision, final ModuleRevisionId mrid, final Date publicationDate) {
        return buildMockResolver(settings, name, findRevision, mrid, publicationDate, false);
    }

    static MockResolver buildMockResolver(ResolverSettings settings, String name,
            boolean findRevision, final ModuleRevisionId mrid, final Date publicationDate,
            final boolean isdefault) {
        final MockResolver r = new MockResolver();
        r.setName(name);
        r.setSettings(settings);
        if (findRevision) {
            DefaultModuleDescriptor md = new DefaultModuleDescriptor(mrid, "integration",
                    publicationDate, isdefault);
            r.rmr = new ResolvedModuleRevision(r, r, md, new MetadataArtifactDownloadReport(
                    md.getMetadataArtifact()));
        }
        return r;
    }

    List askedDeps = new ArrayList();

    ResolvedModuleRevision rmr;

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        ResolvedModuleRevision mr = data.getCurrentResolvedModuleRevision();
        if (mr != null) {
            if (shouldReturnResolvedModule(dd, mr)) {
                return mr;
            }
        }
        askedDeps.add(dd);
        return checkLatest(dd, rmr, data);
    }

    private boolean shouldReturnResolvedModule(DependencyDescriptor dd, ResolvedModuleRevision mr) {
        // a resolved module revision has already been found by a prior dependency resolver
        // let's see if it should be returned and bypass this resolver

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        boolean isDynamic = getSettings().getVersionMatcher().isDynamic(mrid);
        boolean shouldReturn = mr.isForce();
        shouldReturn |= !isDynamic && !mr.getDescriptor().isDefault();

        return shouldReturn;
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            dr.addArtifactReport(new ArtifactDownloadReport(artifacts[i]));
        }
        return dr;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        return null;
    }

    public RepositoryCacheManager getRepositoryCacheManager() {
        return null;
    }

    protected void saveModuleRevisionIfNeeded(DependencyDescriptor dd,
            ResolvedModuleRevision newModuleFound) {
    }
}
