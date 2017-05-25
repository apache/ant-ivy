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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.resolver.util.HasLatestStrategy;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;

/**
 *
 */
public class ChainResolver extends AbstractResolver {
    public static class ResolvedModuleRevisionArtifactInfo implements ArtifactInfo {
        private ResolvedModuleRevision rmr;

        public ResolvedModuleRevisionArtifactInfo(ResolvedModuleRevision rmr) {
            this.rmr = rmr;
        }

        public String getRevision() {
            return rmr.getId().getRevision();
        }

        public long getLastModified() {
            return rmr.getPublicationDate().getTime();
        }

    }

    private boolean returnFirst = false;

    private List<DependencyResolver> chain = new ArrayList<DependencyResolver>();

    private boolean dual;

    public void add(DependencyResolver resolver) {
        chain.add(resolver);
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        data = new ResolveData(data, doValidate(data));

        List<Exception> errors = new ArrayList<Exception>();

        ResolvedModuleRevision resolved = data.getCurrentResolvedModuleRevision();
        ResolvedModuleRevision mr = resolved;

        if (mr == null) {
            Message.verbose(getName() + ": Checking cache for: " + dd);
            mr = findModuleInCache(dd, data, true);
            if (mr != null) {
                Message.verbose(getName() + ": module revision found in cache: " + mr.getId());
                mr = forcedRevision(mr);
            }
        }

        for (DependencyResolver resolver : chain) {
            LatestStrategy oldLatest = setLatestIfRequired(resolver, getLatestStrategy());
            try {
                ResolvedModuleRevision previouslyResolved = mr;
                data.setCurrentResolvedModuleRevision(previouslyResolved);
                mr = resolver.getDependency(dd, data);
                if (mr != previouslyResolved && isReturnFirst()) {
                    mr = forcedRevision(mr);
                }
            } catch (Exception ex) {
                Message.verbose("problem occurred while resolving " + dd + " with " + resolver, ex);
                errors.add(ex);
            } finally {
                if (oldLatest != null) {
                    setLatest(resolver, oldLatest);
                }
            }
            checkInterrupted();
        }
        if (mr == null && !errors.isEmpty()) {
            if (errors.size() == 1) {
                Exception ex = errors.get(0);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else if (ex instanceof ParseException) {
                    throw (ParseException) ex;
                } else {
                    throw new RuntimeException(ex.toString(), ex);
                }
            } else {
                StringBuffer err = new StringBuffer();
                for (Exception ex : errors) {
                    err.append("\t").append(StringUtils.getErrorMessage(ex)).append("\n");
                }
                err.setLength(err.length() - 1);
                throw new RuntimeException("several problems occurred while resolving " + dd
                        + ":\n" + err);
            }
        }
        if (resolved == mr) {
            // nothing has actually been resolved here, we don't need to touch the returned rmr
            return resolved;
        }
        return resolvedRevision(mr);
    }

    private ResolvedModuleRevision resolvedRevision(ResolvedModuleRevision mr) {
        if (isDual() && mr != null) {
            return new ResolvedModuleRevision(mr.getResolver(), this, mr.getDescriptor(),
                    mr.getReport(), mr.isForce());
        } else {
            return mr;
        }
    }

    private ResolvedModuleRevision forcedRevision(ResolvedModuleRevision rmr) {
        if (rmr == null) {
            return null;
        }
        return new ResolvedModuleRevision(rmr.getResolver(), rmr.getArtifactResolver(),
                rmr.getDescriptor(), rmr.getReport(), true);
    }

    private LatestStrategy setLatestIfRequired(DependencyResolver resolver,
            LatestStrategy latestStrategy) {
        String latestName = getLatestStrategyName(resolver);
        if (latestName != null && !"default".equals(latestName)) {
            LatestStrategy oldLatest = getLatest(resolver);
            setLatest(resolver, latestStrategy);
            return oldLatest;
        } else {
            return null;
        }
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        for (DependencyResolver resolver : chain) {
            ResolvedResource result = resolver.findIvyFileRef(dd, data);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    public Map<String, String>[] listTokenValues(String[] tokens, Map<String, Object> criteria) {
        Set<Map<String, String>> result = new HashSet<Map<String, String>>();
        for (DependencyResolver resolver : chain) {
            Map<String, String>[] temp = resolver.listTokenValues(tokens,
                new HashMap<String, Object>(criteria));
            result.addAll(Arrays.asList(temp));
        }

        return result.toArray(new Map[result.size()]);
    }

    @Override
    public void reportFailure() {
        for (DependencyResolver resolver : chain) {
            resolver.reportFailure();
        }
    }

    @Override
    public void reportFailure(Artifact art) {
        for (DependencyResolver resolver : chain) {
            resolver.reportFailure(art);
        }
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        List<Artifact> artifactsToDownload = new ArrayList<Artifact>(Arrays.asList(artifacts));
        DownloadReport report = new DownloadReport();
        for (Iterator<DependencyResolver> iter = chain.iterator(); iter.hasNext()
                && !artifactsToDownload.isEmpty();) {
            DependencyResolver resolver = iter.next();
            DownloadReport r = resolver.download(
                artifactsToDownload.toArray(new Artifact[artifactsToDownload.size()]), options);
            ArtifactDownloadReport[] adr = r.getArtifactsReports();
            for (int i = 0; i < adr.length; i++) {
                if (adr[i].getDownloadStatus() != DownloadStatus.FAILED) {
                    artifactsToDownload.remove(adr[i].getArtifact());
                    report.addArtifactReport(adr[i]);
                }
            }
        }
        for (Artifact art : artifactsToDownload) {
            ArtifactDownloadReport adr = new ArtifactDownloadReport(art);
            adr.setDownloadStatus(DownloadStatus.FAILED);
            report.addArtifactReport(adr);
        }
        return report;
    }

    public List<DependencyResolver> getResolvers() {
        return chain;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {

        getFirstResolver().publish(artifact, src, overwrite);
    }

    @Override
    public void abortPublishTransaction() throws IOException {
        getFirstResolver().abortPublishTransaction();
    }

    @Override
    public void beginPublishTransaction(ModuleRevisionId module, boolean overwrite)
            throws IOException {
        getFirstResolver().beginPublishTransaction(module, overwrite);
    }

    @Override
    public void commitPublishTransaction() throws IOException {
        getFirstResolver().commitPublishTransaction();
    }

    private DependencyResolver getFirstResolver() {
        if (chain.isEmpty()) {
            throw new IllegalStateException("invalid chain resolver with no sub resolver");
        }
        return chain.get(0);
    }

    public boolean isReturnFirst() {
        return returnFirst;
    }

    public void setReturnFirst(boolean returnFirst) {
        this.returnFirst = returnFirst;
    }

    @Override
    public void dumpSettings() {
        Message.verbose("\t" + getName() + " [chain] " + chain);
        Message.debug("\t\treturn first: " + isReturnFirst());
        Message.debug("\t\tdual: " + isDual());
        for (DependencyResolver resolver : chain) {
            Message.debug("\t\t-> " + resolver.getName());
        }
    }

    @Override
    public boolean exists(Artifact artifact) {
        for (DependencyResolver resolver : chain) {
            if (resolver.exists(artifact)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        for (DependencyResolver resolver : chain) {
            ArtifactOrigin origin = resolver.locate(artifact);
            if (!ArtifactOrigin.isUnknown(origin)) {
                return origin;
            }
        }
        return ArtifactOrigin.unkwnown(artifact);
    }

    @Override
    public ArtifactDownloadReport download(ArtifactOrigin artifact, DownloadOptions options) {
        for (DependencyResolver resolver : chain) {
            ArtifactDownloadReport adr = resolver.download(artifact, options);
            if (adr.getDownloadStatus() != DownloadStatus.FAILED) {
                return adr;
            }
        }
        ArtifactDownloadReport adr = new ArtifactDownloadReport(artifact.getArtifact());
        adr.setDownloadStatus(DownloadStatus.FAILED);
        return adr;
    }

    private static void setLatest(DependencyResolver resolver, LatestStrategy latest) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy) resolver;
            r.setLatestStrategy(latest);
        }
    }

    private static LatestStrategy getLatest(DependencyResolver resolver) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy) resolver;
            return r.getLatestStrategy();
        }
        return null;
    }

    private static String getLatestStrategyName(DependencyResolver resolver) {
        if (resolver instanceof HasLatestStrategy) {
            HasLatestStrategy r = (HasLatestStrategy) resolver;
            return r.getLatest();
        }
        return null;
    }

    public void setDual(boolean b) {
        dual = b;
    }

    public boolean isDual() {
        return dual;
    }

}
