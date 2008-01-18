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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.Message;

/**
 * Represents a whole resolution report for a module but for a specific configuration
 */
public class ConfigurationResolveReport {

    private ModuleDescriptor md;

    private String conf;

    private Date date;

    private Map dependencyReports = new LinkedHashMap();

    private Map dependencies = new LinkedHashMap();

    private ResolveEngine resolveEngine;

    private Map modulesIdsMap = new LinkedHashMap();

    private List modulesIds;

    private List previousDeps;

    public ConfigurationResolveReport(ResolveEngine resolveEngine, ModuleDescriptor md,
            String conf, Date date, ResolveOptions options) {
        this.resolveEngine = resolveEngine;
        this.md = md;
        this.conf = conf;
        this.date = date;

        // parse previous deps from previous report file if any
        ResolutionCacheManager cache = resolveEngine.getSettings().getResolutionCacheManager();
        String resolveId = options.getResolveId();
        File previousReportFile = cache.getConfigurationResolveReportInCache(resolveId, conf);
        if (previousReportFile.exists()) {
            try {
                XmlReportParser parser = new XmlReportParser();
                parser.parse(previousReportFile);
                previousDeps = Arrays.asList(parser.getDependencyRevisionIds());
            } catch (Exception e) {
                Message.warn("Error while parsing configuration resolve report "
                        + previousReportFile.getAbsolutePath());
                e.printStackTrace();
                previousDeps = null;
            }
        } else {
            previousDeps = null;
        }
    }

    public boolean hasChanged() {
        if (previousDeps == null) {
            return true;
        }
        return !new HashSet(previousDeps).equals(getModuleRevisionIds());
    }

    /**
     * Returns all non evicted and non error dependency mrids The returned set is ordered so that a
     * dependency will always be found before their own dependencies
     * 
     * @return all non evicted and non error dependency mrids
     */
    public Set getModuleRevisionIds() {
        Set mrids = new LinkedHashSet();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (!node.isEvicted(getConfiguration()) && !node.hasProblem()) {
                mrids.add(node.getResolvedId());
            }
        }
        return mrids;
    }

    public void addDependency(IvyNode node) {
        dependencies.put(node.getId(), node);
        dependencies.put(node.getResolvedId(), node);
        dependencyReports.put(node, Collections.EMPTY_LIST);
    }

    public void addDependency(IvyNode node, DownloadReport report) {
        dependencies.put(node.getId(), node);
        dependencies.put(node.getResolvedId(), node);
        List adrs = new ArrayList();
        Artifact[] artifacts = node.getArtifacts(conf);
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactDownloadReport artifactReport = report.getArtifactReport(artifacts[i]);
            if (artifactReport != null) {
                adrs.add(artifactReport);
            } else {
                Message.debug("no report found for " + artifacts[i]);
            }
        }
        dependencyReports.put(node, adrs);
    }

    public String getConfiguration() {
        return conf;
    }

    public Date getDate() {
        return date;
    }

    public ModuleDescriptor getModuleDescriptor() {
        return md;
    }

    public IvyNode[] getUnresolvedDependencies() {
        List unresolved = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (node.hasProblem()) {
                unresolved.add(node);
            }
        }
        return (IvyNode[]) unresolved.toArray(new IvyNode[unresolved.size()]);
    }

    private Collection getDependencies() {
        return new LinkedHashSet(dependencies.values());
    }

    public IvyNode[] getEvictedNodes() {
        List evicted = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (node.isEvicted(conf)) {
                evicted.add(node);
            }
        }
        return (IvyNode[]) evicted.toArray(new IvyNode[evicted.size()]);
    }

    private Set/*<ModuleRevisionId>*/ getEvictedMrids() {
        Set/*<ModuleRevisionId>*/ evicted = new LinkedHashSet();
        IvyNode[] evictedNodes = getEvictedNodes();
        for (int i = 0; i < evictedNodes.length; i++) {
            evicted.add(evictedNodes[i].getId());
        }
        return evicted;
    }

    public IvyNode[] getDownloadedNodes() {
        List downloaded = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (node.isDownloaded() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return (IvyNode[]) downloaded.toArray(new IvyNode[downloaded.size()]);
    }

    public IvyNode[] getSearchedNodes() {
        List downloaded = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (node.isSearched() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return (IvyNode[]) downloaded.toArray(new IvyNode[downloaded.size()]);
    }

    public ArtifactDownloadReport[] getDownloadReports(ModuleRevisionId mrid) {
        Collection col = (Collection) dependencyReports.get(getDependency(mrid));
        if (col == null) {
            return new ArtifactDownloadReport[0];
        }
        return (ArtifactDownloadReport[]) col.toArray(new ArtifactDownloadReport[col.size()]);
    }

    public IvyNode getDependency(ModuleRevisionId mrid) {
        return (IvyNode) dependencies.get(mrid);
    }

    /**
     * gives all the modules ids concerned by this report, from the most dependent to the least one
     * 
     * @return a list of ModuleId
     */
    public List getModuleIds() {
        if (modulesIds == null) {
            List sortedDependencies = resolveEngine.getSortEngine().sortNodes(getDependencies());
            Collections.reverse(sortedDependencies);
            for (Iterator iter = sortedDependencies.iterator(); iter.hasNext();) {
                IvyNode dependency = (IvyNode) iter.next();
                ModuleId mid = dependency.getResolvedId().getModuleId();
                Collection deps = (Collection) modulesIdsMap.get(mid);
                if (deps == null) {
                    deps = new LinkedHashSet();
                    modulesIdsMap.put(mid, deps);
                }
                deps.add(dependency);
            }
            modulesIds = new ArrayList(modulesIdsMap.keySet());
        }
        return Collections.unmodifiableList(modulesIds);
    }

    public Collection getNodes(ModuleId mid) {
        if (modulesIds == null) {
            getModuleIds();
        }
        return (Collection) modulesIdsMap.get(mid);
    }

    public ResolveEngine getResolveEngine() {
        return resolveEngine;
    }

    public int getArtifactsNumber() {
        int total = 0;
        for (Iterator iter = dependencyReports.values().iterator(); iter.hasNext();) {
            Collection reports = (Collection) iter.next();
            total += reports == null ? 0 : reports.size();
        }
        return total;
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
     *            retrieved.
     * @return the list of reports, never <code>null</code>
     * @see ArtifactDownloadReport
     */
    public ArtifactDownloadReport[] getArtifactsReports(
            DownloadStatus downloadStatus, boolean withEvicted) {
        Collection all = new LinkedHashSet();
        Collection evictedMrids = null;
        if (!withEvicted) {
            evictedMrids = getEvictedMrids();
        }
        for (Iterator iter = dependencyReports.values().iterator(); iter.hasNext();) {
            Collection reports = (Collection) iter.next();
            for (Iterator itReport  = reports.iterator(); itReport.hasNext();) {
                ArtifactDownloadReport report = (ArtifactDownloadReport) itReport.next();
                if (downloadStatus != null && report.getDownloadStatus() != downloadStatus) {
                    continue;
                }
                if (withEvicted 
                        || !evictedMrids.contains(report.getArtifact().getModuleRevisionId())) {
                    all.add(report);   
                }
            }
        }
        return (ArtifactDownloadReport[]) all.toArray(new ArtifactDownloadReport[all.size()]);
    }

    /**
     * Get the report on the sucessfull download requests with the evicted modules
     * 
     * @return the list of reports, never <code>null</code>
     */
    public ArtifactDownloadReport[] getDownloadedArtifactsReports() {
        return getArtifactsReports(DownloadStatus.SUCCESSFUL, true);
    }

    /**
     * Get the report on the failed download requests with the evicted modules
     * 
     * @return the list of reports, never <code>null</code>
     */
    public ArtifactDownloadReport[] getFailedArtifactsReports() {
        return getArtifactsReports(DownloadStatus.FAILED, true);
    }

    public boolean hasError() {
        return getUnresolvedDependencies().length > 0 || getFailedArtifactsReports().length > 0;
    }

    public int getNodesNumber() {
        return getDependencies().size();
    }

}
