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
package org.apache.ivy.core.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.ReportOutputter;
import org.apache.ivy.util.filter.Filter;

/**
 * Represents a whole resolution report for a module
 */
public class ResolveReport {
    private ModuleDescriptor md;

    /** String conf -> ConfigurationResolveReport report */
    private Map confReports = new LinkedHashMap();

    private List problemMessages = new ArrayList();

    /**
     * the list of all dependencies resolved, ordered from the more dependent to the less dependent
     */
    private List dependencies = new ArrayList();

    private List artifacts = new ArrayList();

    private long resolveTime;

    private long downloadTime;

    private String resolveId;

    private long downloadSize;

    public ResolveReport(ModuleDescriptor md) {
        this(md, ResolveOptions.getDefaultResolveId(md));
    }

    public ResolveReport(ModuleDescriptor md, String resolveId) {
        this.md = md;
        this.resolveId = resolveId;
    }

    public void addReport(String conf, ConfigurationResolveReport report) {
        confReports.put(conf, report);
    }

    public ConfigurationResolveReport getConfigurationReport(String conf) {
        return (ConfigurationResolveReport) confReports.get(conf);
    }

    public String[] getConfigurations() {
        return (String[]) confReports.keySet().toArray(new String[confReports.size()]);
    }

    public boolean hasError() {
        boolean hasError = false;
        for (Iterator it = confReports.values().iterator(); it.hasNext() && !hasError;) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) it.next();
            hasError |= report.hasError();
        }
        return hasError;
    }

    public void output(
            ReportOutputter[] outputters, ResolutionCacheManager cacheMgr, ResolveOptions options)
            throws IOException {
        for (int i = 0; i < outputters.length; i++) {
            outputters[i].output(this, cacheMgr, options);
        }
    }

    public ModuleDescriptor getModuleDescriptor() {
        return md;
    }

    public IvyNode[] getEvictedNodes() {
        Collection all = new LinkedHashSet();
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) iter.next();
            all.addAll(Arrays.asList(report.getEvictedNodes()));
        }
        return (IvyNode[]) all.toArray(new IvyNode[all.size()]);
    }

    public IvyNode[] getUnresolvedDependencies() {
        Collection all = new LinkedHashSet();
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) iter.next();
            all.addAll(Arrays.asList(report.getUnresolvedDependencies()));
        }
        return (IvyNode[]) all.toArray(new IvyNode[all.size()]);
    }

    /**
     * Get every report on the download requests.
     * 
     * @return the list of reports, never <code>null</code>
     */
    public ArtifactDownloadReport[] getFailedArtifactsReports() {
        return getArtifactsReports(DownloadStatus.FAILED, true);
    }

    /**
     * Get every report on the download requests.
     * 
     * @return the list of reports, never <code>null</code>
     */
    public ArtifactDownloadReport[] getAllArtifactsReports() {
        return getArtifactsReports(null, true);
    }

    /**
     * Get the report on the download requests. The list of download report can be restricted to a
     * specific download status, and also remove the download report for the evicted modules.
     * 
     * @param downloadStatus
     *            the status of download to retreive. Set it to <code>null</code> for no
     *            restriction on the download status
     * @param withEvicted
     *            set it to <code>true</code> if the report for the evicted modules have to be
     *            retrieved, <code>false</code> to exclude reports from modules evicted in all
     *            configurations.
     * @return the list of reports, never <code>null</code>
     * @see ConfigurationResolveReport#getArtifactsReports(DownloadStatus, boolean)
     */
    public ArtifactDownloadReport[] getArtifactsReports(
            DownloadStatus downloadStatus, boolean withEvicted) {
        Collection all = new LinkedHashSet();
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) iter.next();
            ArtifactDownloadReport[] reports = 
                report.getArtifactsReports(downloadStatus, withEvicted);
            all.addAll(Arrays.asList(reports));
        }
        return (ArtifactDownloadReport[]) all.toArray(new ArtifactDownloadReport[all.size()]);
    }

    public ArtifactDownloadReport[] getArtifactsReports(ModuleRevisionId mrid) {
        Collection all = new LinkedHashSet();
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) iter.next();
            all.addAll(Arrays.asList(report.getDownloadReports(mrid)));
        }
        return (ArtifactDownloadReport[]) all.toArray(new ArtifactDownloadReport[all.size()]);
    }

    public boolean hasChanged() {
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport) iter.next();
            if (report.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    public void setProblemMessages(List problems) {
        problemMessages = problems;
    }

    public List getProblemMessages() {
        return problemMessages;
    }

    public List getAllProblemMessages() {
        List ret = new ArrayList(problemMessages);
        for (Iterator iter = confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport r = (ConfigurationResolveReport) iter.next();
            IvyNode[] unresolved = r.getUnresolvedDependencies();
            for (int i = 0; i < unresolved.length; i++) {
                String errMsg = unresolved[i].getProblemMessage();
                if (errMsg.length() > 0) {
                    ret.add("unresolved dependency: " + unresolved[i].getId() + ": " + errMsg);
                } else {
                    ret.add("unresolved dependency: " + unresolved[i].getId());
                }
            }
            ArtifactDownloadReport[] adrs = r.getFailedArtifactsReports();
            for (int i = 0; i < adrs.length; i++) {
                ret.add("download failed: " + adrs[i].getArtifact());
            }
        }
        return ret;
    }

    public void setDependencies(List dependencies, Filter artifactFilter) {
        this.dependencies = dependencies;
        // collect list of artifacts
        artifacts = new ArrayList();
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
            IvyNode dependency = (IvyNode) iter.next();
            if (!dependency.isCompletelyEvicted() && !dependency.hasProblem()) {
                artifacts.addAll(Arrays.asList(dependency.getSelectedArtifacts(artifactFilter)));
            }
            // update the configurations reports with the dependencies
            // these reports will be completed later with download information, if any
            String[] dconfs = dependency.getRootModuleConfigurations();
            for (int j = 0; j < dconfs.length; j++) {
                ConfigurationResolveReport configurationReport = getConfigurationReport(dconfs[j]);
                if (configurationReport != null) {
                    configurationReport.addDependency(dependency);
                }
            }
        }
    }

    /**
     * Returns the list of all dependencies concerned by this report as a List of IvyNode ordered
     * from the more dependent to the least one
     * 
     * @return The list of all dependencies.
     */
    public List getDependencies() {
        return dependencies;
    }

    /**
     * Returns the list of all artifacts which should be downloaded per this resolve To know if the
     * artifact have actually been downloaded use information found in ConfigurationResolveReport.
     * 
     * @return The list of all artifacts.
     */
    public List getArtifacts() {
        return artifacts;
    }

    /**
     * gives all the modules ids concerned by this report, from the most dependent to the least one
     * 
     * @return a list of ModuleId
     */
    public List getModuleIds() {
        List ret = new ArrayList();
        List sortedDependencies = new ArrayList(dependencies);
        for (Iterator iter = sortedDependencies.iterator(); iter.hasNext();) {
            IvyNode dependency = (IvyNode) iter.next();
            ModuleId mid = dependency.getResolvedId().getModuleId();
            if (!ret.contains(mid)) {
                ret.add(mid);
            }
        }
        return ret;
    }

    public void setResolveTime(long elapsedTime) {
        resolveTime = elapsedTime;
    }

    public long getResolveTime() {
        return resolveTime;
    }

    public void setDownloadTime(long elapsedTime) {
        downloadTime = elapsedTime;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadSize(long size) {
        this.downloadSize = size;
    }

    /**
     * The total size of downloaded artifacts, in bytes.
     * <p>
     * This only includes artifacts actually downloaded to cache (DownloadStatus.SUCCESSFUL), and
     * not artifacts already in cache or used at their original location.
     * </p>
     * 
     * @return The total size of downloaded artifacts, in bytes.
     */
    public long getDownloadSize() {
        return downloadSize;
    }

    public String getResolveId() {
        return resolveId;
    }
}
