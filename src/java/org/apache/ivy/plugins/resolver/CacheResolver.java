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

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;

public class CacheResolver extends FileSystemResolver {
    public CacheResolver() {
    }

    public CacheResolver(ResolverSettings settings) {
        setSettings(settings);
        setName("cache");
    }

    @Override
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        clearIvyAttempts();

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        // check revision

        ResolvedModuleRevision rmr = getRepositoryCacheManager().findModuleInCache(dd, mrid,
            getCacheOptions(data), null);
        if (rmr != null) {
            Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
            return rmr;
        } else if (!getSettings().getVersionMatcher().isDynamic(mrid)) {
            Message.verbose("\t" + getName() + ": no ivy file in cache found for " + mrid);
            return null;
        } else {
            ensureConfigured();
            ResolvedResource ivyRef = findIvyFileRef(dd, data);
            if (ivyRef != null) {
                Message.verbose("\t" + getName() + ": found ivy file in cache for " + mrid);
                Message.verbose("\t\t=> " + ivyRef);

                ModuleRevisionId resolvedMrid = ModuleRevisionId.newInstance(mrid,
                    ivyRef.getRevision());
                IvyNode node = data.getNode(resolvedMrid);
                if (node != null && node.getModuleRevision() != null) {
                    // this revision has already be resolved : return it
                    Message.verbose("\t" + getName() + ": revision already resolved: "
                            + resolvedMrid);
                    return node.getModuleRevision();
                }
                rmr = getRepositoryCacheManager().findModuleInCache(
                    dd.clone(ModuleRevisionId.newInstance(dd.getDependencyRevisionId(),
                        ivyRef.getRevision())), dd.getDependencyRevisionId(),
                    getCacheOptions(data), null);
                if (rmr != null) {
                    Message.verbose("\t" + getName() + ": revision in cache: " + resolvedMrid);
                    return rmr;
                } else {
                    Message.error("\t" + getName()
                            + ": inconsistent cache: clean it and resolve again");
                    return null;
                }
            } else {
                Message.verbose("\t" + getName() + ": no ivy file in cache found for " + mrid);
                return null;
            }
        }
    }

    @Override
    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        ensureConfigured();
        clearArtifactAttempts();
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
            dr.addArtifactReport(adr);
            ResolvedResource artifactRef = getArtifactRef(artifacts[i], null);
            if (artifactRef != null) {
                Message.verbose("\t[NOT REQUIRED] " + artifacts[i]);
                ArtifactOrigin origin = new ArtifactOrigin(artifacts[i], true, artifactRef
                        .getResource().getName());
                File archiveFile = ((FileResource) artifactRef.getResource()).getFile();
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(archiveFile.length());
                adr.setArtifactOrigin(origin);
                adr.setLocalFile(archiveFile);
            } else {
                adr.setDownloadStatus(DownloadStatus.FAILED);
            }
        }
        return dr;
    }

    @Override
    public boolean exists(Artifact artifact) {
        ensureConfigured();
        return super.exists(artifact);
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        ensureConfigured();
        return super.locate(artifact);
    }

    @Override
    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        ensureConfigured();
        super.publish(artifact, src, overwrite);
    }

    @Override
    public OrganisationEntry[] listOrganisations() {
        ensureConfigured();
        return super.listOrganisations();
    }

    @Override
    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureConfigured();
        return super.listModules(org);
    }

    @Override
    public RevisionEntry[] listRevisions(ModuleEntry module) {
        ensureConfigured();
        return super.listRevisions(module);
    }

    @Override
    public void dumpSettings() {
        Message.verbose("\t" + getName() + " [cache]");
    }

    private void ensureConfigured() {
        if (getIvyPatterns().isEmpty()) {
            setIvyPatterns(new ArrayList<String>());
            setArtifactPatterns(new ArrayList<String>());
            RepositoryCacheManager[] caches = getSettings().getRepositoryCacheManagers();
            for (int i = 0; i < caches.length; i++) {
                if (caches[i] instanceof DefaultRepositoryCacheManager) {
                    DefaultRepositoryCacheManager c = (DefaultRepositoryCacheManager) caches[i];
                    addIvyPattern(c.getBasedir().getAbsolutePath() + "/" + c.getIvyPattern());
                    addArtifactPattern(c.getBasedir().getAbsolutePath() + "/"
                            + c.getArtifactPattern());
                } else {
                    Message.verbose(caches[i]
                            + ": cache implementation is not a DefaultRepositoryCacheManager:"
                            + " unable to configure cache resolver with it");
                }
            }
        }
    }

    @Override
    public String getTypeName() {
        return "cache";
    }
}
