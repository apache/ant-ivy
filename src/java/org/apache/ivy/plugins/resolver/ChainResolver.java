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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

    private List chain = new ArrayList();

    private boolean dual;

    private Boolean checkmodified = null;

    public void add(DependencyResolver resolver) {
        chain.add(resolver);
    }

    /**
     * True if this resolver should check lastmodified date to know if ivy files are up to date.
     * 
     * @return
     */
    public boolean isCheckmodified() {
        if (checkmodified == null) {
            if (getSettings() != null) {
                String check = getSettings().getVariable("ivy.resolver.default.check.modified");
                return check != null ? Boolean.valueOf(check).booleanValue() : false;
            } else {
                return false;
            }
        } else {
            return checkmodified.booleanValue();
        }
    }

    public void setCheckmodified(boolean check) {
        checkmodified = Boolean.valueOf(check);
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        data = new ResolveData(data, doValidate(data));
        ResolvedModuleRevision ret = null;

        List errors = new ArrayList();

        ResolvedModuleRevision mr = null;

        ModuleRevisionId mrid = dd.getDependencyRevisionId();


        Message.verbose(getName() + ": Checking cache for: " + dd);
        mr = findModuleInCache(dd, getCacheOptions(data), true);
        if (mr != null) {
            Message.verbose(getName() + ": module revision found in cache: " + mr.getId());
            return resolvedRevision(mr);
        }

        boolean isDynamic = getSettings().getVersionMatcher().isDynamic(mrid);
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            LatestStrategy oldLatest = setLatestIfRequired(resolver, getLatestStrategy());
            try {
                mr = resolver.getDependency(dd, data);
            } catch (Exception ex) {
                Message.verbose("problem occured while resolving " + dd + " with " + resolver
                        + ": " + StringUtils.getStackTrace(ex));
                errors.add(ex);
            } finally {
                if (oldLatest != null) {
                    setLatest(resolver, oldLatest);
                }
            }
            checkInterrupted();
            if (mr != null) {
                boolean shouldReturn = returnFirst;
                shouldReturn |= !isDynamic
                        && ret != null && !ret.getDescriptor().isDefault();
                if (!shouldReturn) {
                    // check if latest is asked and compare to return the most recent
                    String mrDesc = mr.getId()
                            + (mr.getDescriptor().isDefault() ? "[default]" : "") + " from "
                            + mr.getResolver().getName();
                    Message.debug("\tchecking " + mrDesc + " against " + ret);
                    if (ret == null) {
                        Message.debug("\tmodule revision kept as first found: " + mrDesc);
                        ret = mr;
                    } else if (isAfter(mr, ret, data.getDate())) {
                        Message.debug("\tmodule revision kept as younger: " + mrDesc);
                        ret = mr;
                    } else if (!mr.getDescriptor().isDefault() && ret.getDescriptor().isDefault()) {
                        Message.debug("\tmodule revision kept as better (not default): " + mrDesc);
                        ret = mr;
                    } else {
                        Message.debug("\tmodule revision discarded as older: " + mrDesc);
                    }
                    if (!isDynamic
                            && !ret.getDescriptor().isDefault()) {
                        Message.debug("\tmodule revision found and is not default: returning "
                                + mrDesc);
                        return resolvedRevision(mr);
                    }
                } else {
                    return resolvedRevision(mr);
                }
            }
        }
        if (ret == null && !errors.isEmpty()) {
            if (errors.size() == 1) {
                Exception ex = (Exception) errors.get(0);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else if (ex instanceof ParseException) {
                    throw (ParseException) ex;
                } else {
                    throw new RuntimeException(ex.toString(), ex);
                }
            } else {
                StringBuffer err = new StringBuffer();
                for (Iterator iter = errors.iterator(); iter.hasNext();) {
                    Exception ex = (Exception) iter.next();
                    err.append("\t").append(StringUtils.getErrorMessage(ex)).append("\n");
                }
                err.setLength(err.length() - 1);
                throw new RuntimeException("several problems occured while resolving " + dd + ":\n"
                        + err);
            }
        }
        return resolvedRevision(ret);
    }

    private ResolvedModuleRevision resolvedRevision(ResolvedModuleRevision mr) {
        if (isDual() && mr != null) {
            return new ResolvedModuleRevision(
                mr.getResolver(), this, mr.getDescriptor(), mr.getReport());
        } else {
            return mr;
        }
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

    /**
     * Returns true if rmr1 is after rmr2, using the latest strategy to determine which is the
     * latest
     * 
     * @param rmr1
     * @param rmr2
     * @return
     */
    private boolean isAfter(ResolvedModuleRevision rmr1, ResolvedModuleRevision rmr2, Date date) {
        ArtifactInfo[] ais = new ArtifactInfo[] {new ResolvedModuleRevisionArtifactInfo(rmr2),
                new ResolvedModuleRevisionArtifactInfo(rmr1)};
        return getLatestStrategy().findLatest(ais, date) != ais[0];
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            ResolvedResource result = resolver.findIvyFileRef(dd, data);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }

    public void reportFailure() {
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            resolver.reportFailure();
        }
    }

    public void reportFailure(Artifact art) {
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            resolver.reportFailure(art);
        }
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        List artifactsToDownload = new ArrayList(Arrays.asList(artifacts));
        DownloadReport report = new DownloadReport();
        for (Iterator iter = chain.iterator(); iter.hasNext() && !artifactsToDownload.isEmpty();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            DownloadReport r = resolver.download((Artifact[]) artifactsToDownload
                    .toArray(new Artifact[artifactsToDownload.size()]), options);
            ArtifactDownloadReport[] adr = r.getArtifactsReports();
            for (int i = 0; i < adr.length; i++) {
                if (adr[i].getDownloadStatus() != DownloadStatus.FAILED) {
                    artifactsToDownload.remove(adr[i].getArtifact());
                    report.addArtifactReport(adr[i]);
                }
            }
        }
        for (Iterator iter = artifactsToDownload.iterator(); iter.hasNext();) {
            Artifact art = (Artifact) iter.next();
            ArtifactDownloadReport adr = new ArtifactDownloadReport(art);
            adr.setDownloadStatus(DownloadStatus.FAILED);
            report.addArtifactReport(adr);
        }
        return report;
    }

    public List getResolvers() {
        return chain;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {

        getFirstResolver().publish(artifact, src, overwrite);
    }

    public void abortPublishTransaction() throws IOException {
        getFirstResolver().abortPublishTransaction();
    }

    public void beginPublishTransaction(
            ModuleRevisionId module, boolean overwrite) throws IOException {
        getFirstResolver().beginPublishTransaction(module, overwrite);
    }

    public void commitPublishTransaction() throws IOException {
        getFirstResolver().commitPublishTransaction();
    }

    private DependencyResolver getFirstResolver() {
        if (chain.isEmpty()) {
            throw new IllegalStateException("invalid chain resolver with no sub resolver");
        }
        return ((DependencyResolver) chain.get(0));
    }

    public boolean isReturnFirst() {
        return returnFirst;
    }

    public void setReturnFirst(boolean returnFirst) {
        this.returnFirst = returnFirst;
    }

    public void dumpSettings() {
        Message.verbose("\t" + getName() + " [chain] " + chain);
        Message.debug("\t\treturn first: " + isReturnFirst());
        Message.debug("\t\tdual: " + isDual());
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver r = (DependencyResolver) iter.next();
            Message.debug("\t\t-> " + r.getName());
        }
    }

    public boolean exists(Artifact artifact) {
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            if (resolver.exists(artifact)) {
                return true;
            }
        }
        return false;
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
