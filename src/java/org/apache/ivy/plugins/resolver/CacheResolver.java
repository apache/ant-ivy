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
import java.util.Collections;
import java.util.Date;

import org.apache.ivy.core.cache.CacheSettings;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
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
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;

public class CacheResolver extends FileSystemResolver {
    public CacheResolver() {
    }

    public CacheResolver(ResolverSettings settings) {
        setSettings(settings);
        setName("cache");
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        clearIvyAttempts();

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        // check revision

        // if we do not have to check modified and if the revision is exact and not changing,
        // we first search for it in cache
        if (!getSettings().getVersionMatcher().isDynamic(mrid)) {
            ResolvedModuleRevision rmr = getRepositoryCacheManager()
                .findModuleInCache(mrid, doValidate(data), null);
            if (rmr != null) {
                Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
                return rmr;
            } else {
                logIvyAttempt(getRepositoryCacheManager().getArchiveFileInCache(
                    DefaultArtifact.newIvyArtifact(mrid, new Date())).getAbsolutePath());
                Message.verbose("\t" + getName() + ": no ivy file in cache found for " + mrid);
                return null;
            }
        } else {
            ensureConfigured();
            ResolvedResource ivyRef = findIvyFileRef(dd, data);
            if (ivyRef != null) {
                Message.verbose("\t" + getName() + ": found ivy file in cache for " + mrid);
                Message.verbose("\t\t=> " + ivyRef);

                ModuleRevisionId resolvedMrid = ModuleRevisionId.newInstance(mrid, ivyRef
                        .getRevision());
                IvyNode node = data.getNode(resolvedMrid);
                if (node != null && node.getModuleRevision() != null) {
                    // this revision has already be resolved : return it
                    Message.verbose("\t" + getName() + ": revision already resolved: "
                            + resolvedMrid);
                    return node.getModuleRevision();
                }
                ResolvedModuleRevision rmr = getRepositoryCacheManager()
                    .findModuleInCache(resolvedMrid, doValidate(data), null);
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

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        clearArtifactAttempts();
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
            dr.addArtifactReport(adr);
            File archiveFile = getRepositoryCacheManager().getArchiveFileInCache(artifacts[i]);
            if (archiveFile.exists()) {
                Message.verbose("\t[NOT REQUIRED] " + artifacts[i]);
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(archiveFile.length());
                adr.setLocalFile(archiveFile);
            } else {
                logArtifactAttempt(artifacts[i], archiveFile.getAbsolutePath());
                adr.setDownloadStatus(DownloadStatus.FAILED);
            }
        }
        return dr;
    }

    public boolean exists(Artifact artifact) {
        ensureConfigured();
        return super.exists(artifact);
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        ensureConfigured();
        super.publish(artifact, src, overwrite);
    }

    public OrganisationEntry[] listOrganisations() {
        ensureConfigured();
        return super.listOrganisations();
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureConfigured();
        return super.listModules(org);
    }

    public RevisionEntry[] listRevisions(ModuleEntry module) {
        ensureConfigured();
        return super.listRevisions(module);
    }

    public void dumpSettings() {
        Message.verbose("\t" + getName() + " [cache]");
    }

    private void ensureConfigured() {
        if (getSettings() != null) {
            // TODO: we need to address new cache management 
            // (where repository cache is not always in default cache directory)
            ensureConfigured(getSettings(), getSettings().getDefaultCache());
        }
    }

    private void ensureConfigured(CacheSettings settings, File cache) {
        if (settings == null || cache == null) {
            return;
        }
        setIvyPatterns(Collections.singletonList(cache.getAbsolutePath() + "/"
                + settings.getCacheIvyPattern()));
        setArtifactPatterns(Collections.singletonList(cache.getAbsolutePath() + "/"
                + settings.getCacheArtifactPattern()));
    }

    public String getTypeName() {
        return "cache";
    }
}
