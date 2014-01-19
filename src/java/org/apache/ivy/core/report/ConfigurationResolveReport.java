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
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.Message;

/**
 * Represents a whole resolution report for a module but for a specific configuration
 */
public class ConfigurationResolveReport {

    private final ModuleDescriptor md;

    private final String conf;

    private final Date date;

    private final ResolveOptions options;

    private Map<IvyNode, List<ArtifactDownloadReport>> dependencyReports = new LinkedHashMap<IvyNode, List<ArtifactDownloadReport>>();

    private Map<ModuleRevisionId, IvyNode> dependencies = new LinkedHashMap<ModuleRevisionId, IvyNode>();

    private final ResolveEngine resolveEngine;

    private Map<ModuleId, Collection<IvyNode>> modulesIdsMap = new LinkedHashMap<ModuleId, Collection<IvyNode>>();

    private List<ModuleId> modulesIds;

    private Boolean hasChanged = null;

    public ConfigurationResolveReport(ResolveEngine resolveEngine, ModuleDescriptor md,
            String conf, Date date, ResolveOptions options) {
        this.resolveEngine = resolveEngine;
        this.md = md;
        this.conf = conf;
        this.date = date;
        this.options = options;
    }

    /**
     * Check if the set of dependencies has changed since the previous execution of a resolution.<br/>
     * This function use the report file found in the cache. So the function must be called before
     * the new report is serialized there.</br> This function also use the internal dependencies
     * that must already be filled. This function might be 'heavy' because it may have to parse the
     * previous report.
     * 
     * @return
     */
    public void checkIfChanged() {
        ResolutionCacheManager cache = resolveEngine.getSettings().getResolutionCacheManager();
        String resolveId = options.getResolveId();
        File previousReportFile = cache.getConfigurationResolveReportInCache(resolveId, conf);
        if (previousReportFile.exists()) {
            try {
                XmlReportParser parser = new XmlReportParser();
                parser.parse(previousReportFile);
                List<ModuleRevisionId> previousDeps = Arrays.asList(parser
                        .getDependencyRevisionIds());
                HashSet<ModuleRevisionId> previousDepSet = new HashSet<ModuleRevisionId>(
                        previousDeps);
                hasChanged = Boolean.valueOf(!previousDepSet.equals(getModuleRevisionIds()));
            } catch (Exception e) {
                Message.warn("Error while parsing configuration resolve report "
                        + previousReportFile.getAbsolutePath(), e);
                hasChanged = Boolean.TRUE;
            }
        } else {
            hasChanged = Boolean.TRUE;
        }
    }

    /**
     * @pre checkIfChanged has been called.
     */
    public boolean hasChanged() {
        return hasChanged.booleanValue();
    }

    /**
     * Returns all non evicted and non error dependency mrids The returned set is ordered so that a
     * dependency will always be found before their own dependencies
     * 
     * @return all non evicted and non error dependency mrids
     */
    public Set<ModuleRevisionId> getModuleRevisionIds() {
        Set<ModuleRevisionId> mrids = new LinkedHashSet<ModuleRevisionId>();
        for (IvyNode node : getDependencies()) {
            if (!node.isEvicted(getConfiguration()) && !node.hasProblem()) {
                mrids.add(node.getResolvedId());
            }
        }
        return mrids;
    }

    public void addDependency(IvyNode node) {
        dependencies.put(node.getId(), node);
        dependencies.put(node.getResolvedId(), node);
        dependencyReports.put(node, Collections.<ArtifactDownloadReport> emptyList());
    }

    public void updateDependency(ModuleRevisionId mrid, IvyNode node) {
        dependencies.put(mrid, node);
    }

    public void addDependency(IvyNode node, DownloadReport report) {
        dependencies.put(node.getId(), node);
        dependencies.put(node.getResolvedId(), node);
        List<ArtifactDownloadReport> adrs = new ArrayList<ArtifactDownloadReport>();
        Artifact[] artifacts = node.getArtifacts(conf);
        for (Artifact artifact : artifacts) {
            ArtifactDownloadReport artifactReport = report.getArtifactReport(artifact);
            if (artifactReport != null) {
                adrs.add(artifactReport);
            } else {
                Message.debug("no report found for " + artifact);
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
        List<IvyNode> unresolved = new ArrayList<IvyNode>();
        for (IvyNode node : getDependencies()) {
            if (node.hasProblem()) {
                unresolved.add(node);
            }
        }
        return unresolved.toArray(new IvyNode[unresolved.size()]);
    }

    private Collection<IvyNode> getDependencies() {
        return new LinkedHashSet<IvyNode>(dependencies.values());
    }

    public IvyNode[] getEvictedNodes() {
        List<IvyNode> evicted = new ArrayList<IvyNode>();
        for (IvyNode node : getDependencies()) {
            if (node.isEvicted(conf)) {
                evicted.add(node);
            }
        }
        return evicted.toArray(new IvyNode[evicted.size()]);
    }

    private Set<ModuleRevisionId> getEvictedMrids() {
        Set<ModuleRevisionId> evicted = new LinkedHashSet<ModuleRevisionId>();
        IvyNode[] evictedNodes = getEvictedNodes();
        for (IvyNode node : evictedNodes) {
            evicted.add(node.getId());
        }
        return evicted;
    }

    public IvyNode[] getDownloadedNodes() {
        List<IvyNode> downloaded = new ArrayList<IvyNode>();
        for (IvyNode node : getDependencies()) {
            if (node.isDownloaded() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return downloaded.toArray(new IvyNode[downloaded.size()]);
    }

    public IvyNode[] getSearchedNodes() {
        List<IvyNode> downloaded = new ArrayList<IvyNode>();
        for (IvyNode node : getDependencies()) {
            if (node.isSearched() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return downloaded.toArray(new IvyNode[downloaded.size()]);
    }

    public ArtifactDownloadReport[] getDownloadReports(ModuleRevisionId mrid) {
        Collection<ArtifactDownloadReport> col = dependencyReports.get(getDependency(mrid));
        if (col == null) {
            return new ArtifactDownloadReport[0];
        }
        return col.toArray(new ArtifactDownloadReport[col.size()]);
    }

    public IvyNode getDependency(ModuleRevisionId mrid) {
        return dependencies.get(mrid);
    }

    /**
     * gives all the modules ids concerned by this report, from the most dependent to the least one
     * 
     * @return a list of ModuleId
     */
    public List<ModuleId> getModuleIds() {
        if (modulesIds == null) {
            List<IvyNode> sortedDependencies = resolveEngine.getSortEngine().sortNodes(
                getDependencies(), SortOptions.SILENT);
            Collections.reverse(sortedDependencies);
            for (IvyNode dependency : sortedDependencies) {
                ModuleId mid = dependency.getResolvedId().getModuleId();
                Collection<IvyNode> deps = modulesIdsMap.get(mid);
                if (deps == null) {
                    deps = new LinkedHashSet<IvyNode>();
                    modulesIdsMap.put(mid, deps);
                }
                deps.add(dependency);
            }
            modulesIds = new ArrayList<ModuleId>(modulesIdsMap.keySet());
        }
        return Collections.unmodifiableList(modulesIds);
    }

    public Collection<IvyNode> getNodes(ModuleId mid) {
        if (modulesIds == null) {
            getModuleIds();
        }
        return modulesIdsMap.get(mid);
    }

    public ResolveEngine getResolveEngine() {
        return resolveEngine;
    }

    public int getArtifactsNumber() {
        int total = 0;
        for (Collection<ArtifactDownloadReport> reports : dependencyReports.values()) {
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
     *            the status of download to retreive. Set it to <code>null</code> for no restriction
     *            on the download status
     * @param withEvicted
     *            set it to <code>true</code> if the report for the evicted modules have to be
     *            retrieved.
     * @return the list of reports, never <code>null</code>
     * @see ArtifactDownloadReport
     */
    public ArtifactDownloadReport[] getArtifactsReports(DownloadStatus downloadStatus,
            boolean withEvicted) {
        Collection<ArtifactDownloadReport> all = new LinkedHashSet<ArtifactDownloadReport>();
        Collection<ModuleRevisionId> evictedMrids = null;
        if (!withEvicted) {
            evictedMrids = getEvictedMrids();
        }
        for (Collection<ArtifactDownloadReport> reports : dependencyReports.values()) {
            for (ArtifactDownloadReport report : reports) {
                if (downloadStatus != null && report.getDownloadStatus() != downloadStatus) {
                    continue;
                }
                if (withEvicted
                        || !evictedMrids.contains(report.getArtifact().getModuleRevisionId())) {
                    all.add(report);
                }
            }
        }
        return all.toArray(new ArtifactDownloadReport[all.size()]);
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
        ArtifactDownloadReport[] allFailedReports = getArtifactsReports(DownloadStatus.FAILED, true);
        return filterOutMergedArtifacts(allFailedReports);
    }

    public boolean hasError() {
        return getUnresolvedDependencies().length > 0 || getFailedArtifactsReports().length > 0;
    }

    public int getNodesNumber() {
        return getDependencies().size();
    }

    public static ArtifactDownloadReport[] filterOutMergedArtifacts(
            ArtifactDownloadReport[] allFailedReports) {
        Collection<ArtifactDownloadReport> adrs = new ArrayList<ArtifactDownloadReport>(
                Arrays.asList(allFailedReports));
        for (Iterator<ArtifactDownloadReport> iterator = adrs.iterator(); iterator.hasNext();) {
            ArtifactDownloadReport adr = iterator.next();

            if (adr.getArtifact().getExtraAttribute("ivy:merged") != null) {
                iterator.remove();
            }
        }
        return adrs.toArray(new ArtifactDownloadReport[adrs.size()]);
    }

}
