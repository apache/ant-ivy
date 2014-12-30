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
package org.apache.ivy.core.resolve;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.event.resolve.StartResolveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;

/**
 * The resolve engine which is the core of the dependency resolution mechanism used in Ivy. It
 * features several resolve methods, some very simple, like {@link #resolve(File)} and
 * {@link #resolve(URL)} which allow to simply resolve dependencies of a single module descriptor,
 * or more complete one, like the {@link #resolve(ModuleDescriptor, ResolveOptions)} which allows to
 * provide options to the resolution engine.
 * 
 * @see ResolveOptions
 */
public class ResolveEngine {
    private ResolveEngineSettings settings;

    private EventManager eventManager;

    private SortEngine sortEngine;

    private Set<String> fetchedSet = new HashSet<String>();

    private DependencyResolver dictatorResolver;

    /**
     * Constructs a ResolveEngine.
     * 
     * @param settings
     *            the settings to use to configure the engine. Must not be null.
     * @param eventManager
     *            the event manager to use to send events about the resolution process. Must not be
     *            null.
     * @param sortEngine
     *            the sort engine to use to sort modules before producing the dependency resolution
     *            report. Must not be null.
     */
    public ResolveEngine(ResolveEngineSettings settings, EventManager eventManager,
            SortEngine sortEngine) {
        this.settings = settings;
        this.eventManager = eventManager;
        this.sortEngine = sortEngine;
    }

    /**
     * Returns the currently configured dictator resolver, which when non null is used in place of
     * any specified resolver in the {@link IvySettings}
     * 
     * @return the currently configured dictator resolver, may be null.
     */
    public DependencyResolver getDictatorResolver() {
        return dictatorResolver;
    }

    /**
     * Sets a dictator resolver, which is used in place of regular dependency resolver for
     * subsequent dependency resolution by this engine.
     * 
     * @param dictatorResolver
     *            the dictator resolver to use in this engine, null if regular settings should used
     */
    public void setDictatorResolver(DependencyResolver dictatorResolver) {
        this.dictatorResolver = dictatorResolver;
        settings.setDictatorResolver(dictatorResolver);
    }

    public ResolveReport resolve(File ivySource) throws ParseException, IOException {
        return resolve(ivySource.toURI().toURL());
    }

    public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
        return resolve(ivySource, new ResolveOptions());
    }

    /**
     * Resolves the module identified by the given mrid with its dependencies if transitive is set
     * to true.
     */
    public ResolveReport resolve(final ModuleRevisionId mrid, ResolveOptions options,
            boolean changing) throws ParseException, IOException {
        DefaultModuleDescriptor md;
        ResolveOptions optionsToUse = new ResolveOptions(options);

        if (options.useSpecialConfs()) {
            // create new resolve options because this is a different resolve than the real resolve
            // (which will be a resolve of a newCallerInstance module)
            ResolvedModuleRevision rmr = findModule(mrid, new ResolveOptions(options));
            if (rmr == null) {
                Message.verbose("module not found " + mrid);

                // we will continue the resolve anyway to get a nice error message back
                // to the user, however reduce the amount of logging in this case
                optionsToUse.setLog(LogOptions.LOG_DOWNLOAD_ONLY);
                md = DefaultModuleDescriptor.newCallerInstance(mrid, new String[] {"default"},
                    options.isTransitive(), changing);
            } else {
                String[] confs = options.getConfs(rmr.getDescriptor());
                md = DefaultModuleDescriptor.newCallerInstance(
                    ModuleRevisionId.newInstance(mrid, rmr.getId().getRevision()), confs,
                    options.isTransitive(), changing);
            }
        } else {
            md = DefaultModuleDescriptor.newCallerInstance(mrid, options.getConfs(),
                options.isTransitive(), changing);
        }

        return resolve(md, optionsToUse);
    }

    /**
     * Resolve dependencies of a module described by an ivy file.
     */
    public ResolveReport resolve(URL ivySource, ResolveOptions options) throws ParseException,
            IOException {
        URLResource res = new URLResource(ivySource);
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
        Message.verbose("using " + parser + " to parse " + ivySource);
        ModuleDescriptor md = parser.parseDescriptor(settings, ivySource, options.isValidate());
        String revision = options.getRevision();
        if (revision == null && md.getResolvedModuleRevisionId().getRevision() == null) {
            revision = Ivy.getWorkingRevision();
        }
        if (revision != null) {
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(md.getModuleRevisionId(),
                revision));
        }

        return resolve(md, options);
    }

    /**
     * Resolve dependencies of a module described by a module descriptor.
     */
    public ResolveReport resolve(ModuleDescriptor md, ResolveOptions options)
            throws ParseException, IOException {
        DependencyResolver oldDictator = getDictatorResolver();
        IvyContext context = IvyContext.getContext();
        try {
            String[] confs = options.getConfs(md);
            options.setConfs(confs);

            if (options.getResolveId() == null) {
                options.setResolveId(ResolveOptions.getDefaultResolveId(md));
            }

            eventManager.fireIvyEvent(new StartResolveEvent(md, confs));

            long start = System.currentTimeMillis();
            if (ResolveOptions.LOG_DEFAULT.equals(options.getLog())) {
                Message.info(":: resolving dependencies :: " + md.getResolvedModuleRevisionId()
                        + (options.isTransitive() ? "" : " [not transitive]"));
                Message.info("\tconfs: " + Arrays.asList(confs));
            } else {
                Message.verbose(":: resolving dependencies :: " + md.getResolvedModuleRevisionId()
                        + (options.isTransitive() ? "" : " [not transitive]"));
                Message.verbose("\tconfs: " + Arrays.asList(confs));
            }
            Message.verbose("\tvalidate = " + options.isValidate());
            Message.verbose("\trefresh = " + options.isRefresh());

            ResolveReport report = new ResolveReport(md, options.getResolveId());

            ResolveData data = new ResolveData(this, options);
            context.setResolveData(data);

            // resolve dependencies
            IvyNode[] dependencies = getDependencies(md, options, report);
            report.setDependencies(Arrays.asList(dependencies), options.getArtifactFilter());

            if (options.getCheckIfChanged()) {
                report.checkIfChanged();
            }

            // produce resolved ivy file and ivy properties in cache
            ResolutionCacheManager cacheManager = settings.getResolutionCacheManager();
            cacheManager.saveResolvedModuleDescriptor(md);

            // we store the resolved dependencies revisions and statuses per asked dependency
            // revision id, for direct dependencies only.
            // this is used by the deliver task to resolve dynamic revisions to static ones
            File ivyPropertiesInCache = cacheManager.getResolvedIvyPropertiesInCache(md
                    .getResolvedModuleRevisionId());
            Properties props = new Properties();
            if (dependencies.length > 0) {
                Map<ModuleId, ModuleRevisionId> forcedRevisions = new HashMap<ModuleId, ModuleRevisionId>();
                for (int i = 0; i < dependencies.length; i++) {
                    if (dependencies[i].getModuleRevision() != null
                            && dependencies[i].getModuleRevision().isForce()) {
                        forcedRevisions.put(dependencies[i].getModuleId(),
                            dependencies[i].getResolvedId());
                    }
                }

                IvyNode root = dependencies[0].getRoot();

                Map<ModuleId, IvyNode> topLevelDeps = new HashMap<ModuleId, IvyNode>();
                for (int i = 0; i < dependencies.length; i++) {
                    if (!dependencies[i].hasProblem()) {
                        DependencyDescriptor dd = dependencies[i].getDependencyDescriptor(root);
                        if (dd != null) {
                            ModuleId orgMod = dependencies[i].getModuleId();
                            topLevelDeps.put(orgMod, dependencies[i]);
                        }
                    }
                }

                for (int i = 0; i < dependencies.length; i++) {
                    if (!dependencies[i].hasProblem() && !dependencies[i].isCompletelyEvicted()) {
                        DependencyDescriptor dd = dependencies[i].getDependencyDescriptor(root);
                        if (dd == null) {
                            ModuleId mid = dependencies[i].getModuleId();
                            IvyNode tlDep = topLevelDeps.get(mid);
                            if (tlDep != null) {
                                dd = tlDep.getDependencyDescriptor(root);
                            }
                        }
                        if (dd != null) {
                            ModuleRevisionId depResolvedId = dependencies[i].getResolvedId();
                            ModuleDescriptor depDescriptor = dependencies[i].getDescriptor();
                            ModuleRevisionId depRevisionId = dd.getDependencyRevisionId();
                            ModuleRevisionId forcedRevisionId = forcedRevisions.get(dependencies[i]
                                    .getModuleId());

                            if (dependencies[i].getModuleRevision() != null
                                    && dependencies[i].getModuleRevision().isForce()
                                    && !depResolvedId.equals(depRevisionId)
                                    && !settings.getVersionMatcher().isDynamic(depRevisionId)) {
                                // if we were forced to this revision and we
                                // are not a dynamic revision, reset to the
                                // asked revision
                                depResolvedId = depRevisionId;
                                depDescriptor = null;
                            }

                            if (depResolvedId == null) {
                                throw new NullPointerException("getResolvedId() is null for "
                                        + dependencies[i].toString());
                            }
                            if (depRevisionId == null) {
                                throw new NullPointerException("getDependencyRevisionId() "
                                        + "is null for " + dd.toString());
                            }
                            String rev = depResolvedId.getRevision();
                            String forcedRev = forcedRevisionId == null ? rev : forcedRevisionId
                                    .getRevision();

                            // The evicted modules have no description, so we can't put the status
                            String status = depDescriptor == null ? "?" : depDescriptor.getStatus();
                            Message.debug("storing dependency " + depResolvedId + " in props");
                            props.put(depRevisionId.encodeToString(), rev + " " + status + " "
                                    + forcedRev + " " + depResolvedId.getBranch());
                        }
                    }
                }
            }
            FileOutputStream out = new FileOutputStream(ivyPropertiesInCache);
            props.store(out, md.getResolvedModuleRevisionId() + " resolved revisions");
            out.close();
            Message.verbose("\tresolved ivy file produced in cache");

            report.setResolveTime(System.currentTimeMillis() - start);

            if (options.isDownload()) {
                Message.verbose(":: downloading artifacts ::");

                DownloadOptions downloadOptions = new DownloadOptions();
                downloadOptions.setLog(options.getLog());
                downloadArtifacts(report, options.getArtifactFilter(), downloadOptions);
            }

            if (options.isOutputReport()) {
                outputReport(report, cacheManager, options);
            }

            Message.verbose("\tresolve done (" + report.getResolveTime() + "ms resolve - "
                    + report.getDownloadTime() + "ms download)");
            Message.sumupProblems();

            eventManager.fireIvyEvent(new EndResolveEvent(md, confs, report));
            return report;
        } catch (RuntimeException ex) {
            Message.debug(ex);
            Message.error(ex.getMessage());
            Message.sumupProblems();
            throw ex;
        } finally {
            context.setResolveData(null);
            setDictatorResolver(oldDictator);
        }
    }

    public void outputReport(ResolveReport report, ResolutionCacheManager cacheMgr,
            ResolveOptions options) throws IOException {
        if (ResolveOptions.LOG_DEFAULT.equals(options.getLog())) {
            Message.info(":: resolution report :: resolve " + report.getResolveTime() + "ms"
                    + " :: artifacts dl " + report.getDownloadTime() + "ms");
        } else {
            Message.verbose(":: resolution report :: resolve " + report.getResolveTime() + "ms"
                    + " :: artifacts dl " + report.getDownloadTime() + "ms");
        }
        report.setProblemMessages(Message.getProblems());
        // output report
        report.output(settings.getReportOutputters(), cacheMgr, options);
    }

    public void downloadArtifacts(ResolveReport report, Filter<Artifact> artifactFilter,
            DownloadOptions options) {
        long start = System.currentTimeMillis();
        IvyNode[] dependencies = report.getDependencies().toArray(
            new IvyNode[report.getDependencies().size()]);

        eventManager.fireIvyEvent(new PrepareDownloadEvent(report.getArtifacts().toArray(
            new Artifact[report.getArtifacts().size()])));

        long totalSize = 0;
        for (int i = 0; i < dependencies.length; i++) {
            checkInterrupted();
            // download artifacts required in all asked configurations
            if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()
                    && dependencies[i].getModuleRevision() != null) {
                DependencyResolver resolver = dependencies[i].getModuleRevision()
                        .getArtifactResolver();
                Artifact[] selectedArtifacts = dependencies[i].getSelectedArtifacts(artifactFilter);
                DownloadReport dReport = resolver.download(selectedArtifacts, options);
                ArtifactDownloadReport[] adrs = dReport.getArtifactsReports();
                for (int j = 0; j < adrs.length; j++) {
                    if (adrs[j].getDownloadStatus() == DownloadStatus.FAILED) {
                        if (adrs[j].getArtifact().getExtraAttribute("ivy:merged") != null) {
                            Message.warn("\tmerged artifact not found: " + adrs[j].getArtifact()
                                    + ". It was required in "
                                    + adrs[j].getArtifact().getExtraAttribute("ivy:merged"));
                        } else {
                            Message.warn("\t" + adrs[j]);
                            resolver.reportFailure(adrs[j].getArtifact());
                        }
                    } else if (adrs[j].getDownloadStatus() == DownloadStatus.SUCCESSFUL) {
                        totalSize += adrs[j].getSize();
                    }
                }
                // update concerned reports
                String[] dconfs = dependencies[i].getRootModuleConfigurations();
                for (int j = 0; j < dconfs.length; j++) {
                    // the report itself is responsible to take into account only
                    // artifacts required in its corresponding configuration
                    // (as described by the Dependency object)
                    if (dependencies[i].isEvicted(dconfs[j])
                            || dependencies[i].isBlacklisted(dconfs[j])) {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i]);
                    } else {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i],
                            dReport);
                    }
                }
            }
        }
        report.setDownloadTime(System.currentTimeMillis() - start);
        report.setDownloadSize(totalSize);
    }

    /**
     * Download an artifact to the cache. Not used internally, useful especially for IDE plugins
     * needing to download artifact one by one (for source or javadoc artifact, for instance).
     * <p>
     * Downloaded artifact file can be accessed using {@link ArtifactDownloadReport#getLocalFile()}.
     * </p>
     * <p>
     * It is possible to track the progression of the download using classical ivy progress
     * monitoring feature (see addTransferListener).
     * </p>
     * 
     * @param artifact
     *            the artifact to download
     * @return a report concerning the download
     * @see #download(ArtifactOrigin, DownloadOptions)
     */
    public ArtifactDownloadReport download(Artifact artifact, DownloadOptions options) {
        DependencyResolver resolver = settings.getResolver(artifact.getModuleRevisionId());
        DownloadReport r = resolver.download(new Artifact[] {artifact}, options);
        return r.getArtifactReport(artifact);
    }

    /**
     * Locates an artifact in dependency resolvers, and return its location if it can be located and
     * actually exists, or an unknown {@link ArtifactOrigin} in other cases.
     * 
     * @param artifact
     *            the artifact to locate.
     * @return the artifact location, should be tested with
     *         {@link ArtifactOrigin#isUnknown(ArtifactOrigin)} to check if the artifact has
     *         actually been located.
     */
    public ArtifactOrigin locate(Artifact artifact) {
        DependencyResolver resolver = settings.getResolver(artifact.getModuleRevisionId());
        return resolver.locate(artifact);
    }

    /**
     * Materialize an artifact already located.
     * <p>
     * Not used internally, useful especially for IDE plugins needing to download artifact one by
     * one (for source or javadoc artifact, for instance).
     * </p>
     * <p>
     * Materialized artifact file can be accessed using
     * {@link ArtifactDownloadReport#getLocalFile()}.
     * </p>
     * <p>
     * It is possible to track the progression of the download using classical ivy progress
     * monitoring feature (see addTransferListener).
     * </p>
     * 
     * @param origin
     *            the artifact origin to materialize
     * @return a report concerning the download
     * @see #download(Artifact, DownloadOptions)
     * @see #locate(Artifact)
     */
    public ArtifactDownloadReport download(ArtifactOrigin origin, DownloadOptions options) {
        DependencyResolver resolver = settings.getResolver(origin.getArtifact()
                .getModuleRevisionId());
        return resolver.download(origin, options);
    }

    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts. The module
     * to resolve is given by its ivy file URL. This method requires appropriate configuration of
     * the ivy instance, especially resolvers.
     * 
     * @param ivySource
     *            url of the ivy file to use for dependency resolving
     * @param confs
     *            an array of configuration names to resolve - must not be null nor empty
     * @param getCache
     *            the cache to use - default cache is used if null
     * @param date
     *            the date to which resolution must be done - may be null
     * @return an array of the resolved dependencies
     * @throws ParseException
     *             if a parsing problem occurred in the ivy file
     * @throws IOException
     *             if an IO problem was raised during ivy file parsing
     */
    public IvyNode[] getDependencies(URL ivySource, ResolveOptions options) throws ParseException,
            IOException {
        return getDependencies(
            ModuleDescriptorParserRegistry.getInstance().parseDescriptor(settings, ivySource,
                options.isValidate()), options, null);
    }

    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts. The module
     * to resolve is given by its module descriptor. This method requires appropriate configuration
     * of the ivy instance, especially resolvers.
     * <p>
     * The <code>IvyNode</code>s are ordered from the most dependent to the less dependent, so that
     * an IvyNode is always found in the list after all IvyNode depending directly on it.
     * 
     * @param md
     *            the descriptor of the module for which we want to get dependencies - must not be
     *            null
     * @param options
     *            the resolve options to use to resolve the dependencies
     * @param report
     *            a resolve report to fill during resolution - may be null
     * @return an array of the resolved Dependencies
     */
    public IvyNode[] getDependencies(ModuleDescriptor md, ResolveOptions options,
            ResolveReport report) {
        // check parameters
        if (md == null) {
            throw new NullPointerException("module descriptor must not be null");
        }
        String[] confs = options.getConfs(md);
        Collection<String> missingConfs = new ArrayList<String>();
        for (int i = 0; i < confs.length; i++) {
            if (confs[i] == null) {
                throw new NullPointerException("null conf not allowed: confs where: "
                        + Arrays.asList(confs));
            }

            if (md.getConfiguration(confs[i]) == null) {
                missingConfs.add(" '" + confs[i] + "' ");
            }
        }
        if (!missingConfs.isEmpty()) {
            throw new IllegalArgumentException("requested configuration"
                    + (missingConfs.size() > 1 ? "s" : "") + " not found in "
                    + md.getModuleRevisionId() + ": " + missingConfs);
        }

        IvyContext context = IvyContext.pushNewCopyContext();
        try {
            options.setConfs(confs);

            Date reportDate = new Date();
            ResolveData data = context.getResolveData();
            if (data == null) {
                data = new ResolveData(this, options);
                context.setResolveData(data);
            }
            IvyNode rootNode = new IvyNode(data, md);

            for (int i = 0; i < confs.length; i++) {
                Message.verbose("resolving dependencies for configuration '" + confs[i] + "'");
                // for each configuration we clear the cache of what's been fetched
                fetchedSet.clear();

                ConfigurationResolveReport confReport = null;
                if (report != null) {
                    confReport = report.getConfigurationReport(confs[i]);
                    if (confReport == null) {
                        confReport = new ConfigurationResolveReport(this, md, confs[i], reportDate,
                                options);
                        report.addReport(confs[i], confReport);
                    }
                }
                // we reuse the same resolve data with a new report for each conf
                data.setReport(confReport);

                // update the root module conf we are about to fetch
                VisitNode root = new VisitNode(data, rootNode, null, confs[i], null);
                root.setRequestedConf(confs[i]);
                rootNode.updateConfsToFetch(Collections.singleton(confs[i]));

                // go fetch !
                boolean fetched = false;
                while (!fetched) {
                    try {
                        fetchDependencies(root, confs[i], false);
                        fetched = true;
                    } catch (RestartResolveProcess restart) {
                        Message.verbose("====================================================");
                        Message.verbose("=           RESTARTING RESOLVE PROCESS");
                        Message.verbose("= " + restart.getMessage());
                        Message.verbose("====================================================");
                        fetchedSet.clear();
                    }
                }

                // clean data
                for (IvyNode dep : data.getNodes()) {
                    dep.clean();
                }
            }

            // prune and reverse sort fectched dependencies
            Collection<IvyNode> nodes = data.getNodes();
            // use a Set to avoid duplicates, linked to preserve order
            Collection<IvyNode> dependencies = new LinkedHashSet<IvyNode>(nodes.size());
            for (IvyNode node : nodes) {
                if (node != null && !node.isRoot() && !node.isCompletelyBlacklisted()) {
                    dependencies.add(node);
                }
            }
            List<IvyNode> sortedDependencies = sortEngine.sortNodes(dependencies,
                SortOptions.SILENT);
            Collections.reverse(sortedDependencies);

            handleTransiviteEviction(md, confs, data, sortedDependencies);

            return dependencies.toArray(new IvyNode[dependencies.size()]);
        } finally {
            IvyContext.popContext();
        }
    }

    private void handleTransiviteEviction(ModuleDescriptor md, String[] confs, ResolveData data,
            List<IvyNode> sortedDependencies) {
        // handle transitive eviction now:
        // if a module has been evicted then all its dependencies required only by it should be
        // evicted too. Since nodes are now sorted from the more dependent to the less one, we
        // can traverse the list and check only the direct parent and not all the ancestors
        for (IvyNode node : sortedDependencies) {
            if (!node.isCompletelyEvicted()) {
                for (int i = 0; i < confs.length; i++) {
                    IvyNodeCallers.Caller[] callers = node.getCallers(confs[i]);
                    if (settings.debugConflictResolution()) {
                        Message.debug("checking if " + node.getId()
                                + " is transitively evicted in " + confs[i]);
                    }
                    boolean allEvicted = callers.length > 0;
                    for (int j = 0; j < callers.length; j++) {
                        if (callers[j].getModuleRevisionId().equals(md.getModuleRevisionId())) {
                            // the caller is the root module itself, it can't be evicted
                            allEvicted = false;
                            break;
                        } else {
                            IvyNode callerNode = data.getNode(callers[j].getModuleRevisionId());
                            if (callerNode == null) {
                                Message.warn("ivy internal error: no node found for "
                                        + callers[j].getModuleRevisionId() + ": looked in "
                                        + data.getNodeIds() + " and root module id was "
                                        + md.getModuleRevisionId());
                            } else if (!callerNode.isEvicted(confs[i])) {
                                allEvicted = false;
                                break;
                            } else {
                                if (settings.debugConflictResolution()) {
                                    Message.debug("caller " + callerNode.getId() + " of "
                                            + node.getId() + " is evicted");
                                }
                            }
                        }
                    }
                    if (allEvicted) {
                        Message.verbose("all callers are evicted for " + node + ": evicting too");
                        node.markEvicted(confs[i], null, null, null);
                    } else {
                        if (settings.debugConflictResolution()) {
                            Message.debug(node.getId()
                                    + " isn't transitively evicted, at least one caller was"
                                    + " not evicted");
                        }
                    }
                }
            }
        }
    }

    private void fetchDependencies(VisitNode node, String conf, boolean shouldBePublic) {
        checkInterrupted();
        long start = System.currentTimeMillis();
        if (node.getParent() != null) {
            Message.verbose("== resolving dependencies " + node.getParent().getId() + "->"
                    + node.getId() + " [" + node.getParentConf() + "->" + conf + "]");
        } else {
            Message.verbose("== resolving dependencies for " + node.getId() + " [" + conf + "]");
        }
        ResolveData data = node.getNode().getData();
        VisitNode parentVisitNode = data.getCurrentVisitNode();

        data.setCurrentVisitNode(node);
        DependencyDescriptor dd = node.getDependencyDescriptor();
        VersionMatcher versionMatcher = node.getNode().getData().getSettings().getVersionMatcher();
        if (dd != null
                && !(node.getRoot() == node.getParent() && versionMatcher.isDynamic(dd
                        .getDependencyRevisionId()))) {
            /*
             * we don't resolve conflicts before loading data for direct dependencies on dynamic
             * revisions, so that direct dynamic revisions are always resolved, which is mandatory
             * for proper replacement of dynamic revisions during 'deliver'
             */
            resolveConflict(node, conf);
        }

        if (node.loadData(conf, shouldBePublic)) {
            // we resolve conflict again now that we have all information loaded
            // indeed in some cases conflict manager need more information than just asked
            // dependency to take the decision
            resolveConflict(node, conf);
            if (!node.isEvicted() && !node.isCircular()) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        } else if (!node.hasProblem()) {
            // the node has not been loaded but hasn't problem: it was already loaded
            // => we just have to update its dependencies data
            if (!node.isEvicted() && !node.isCircular()) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        }
        if (node.isEvicted()) {
            // update selected nodes with confs asked in evicted one
            EvictionData ed = node.getEvictedData();
            if (ed.getSelected() != null) {
                for (IvyNode selected : ed.getSelected()) {
                    if (!selected.isLoaded()) {
                        // the node is not yet loaded, we can simply update its set of
                        // configurations to fetch
                        selected.updateConfsToFetch(Collections.singleton(conf));
                    } else {
                        // the node has already been loaded, we must fetch its dependencies in the
                        // required conf
                        fetchDependencies(node.gotoNode(selected), conf, true);
                    }
                }
            }
        }
        if (settings.debugConflictResolution()) {
            Message.debug(node.getId() + " => dependencies resolved in " + conf + " ("
                    + (System.currentTimeMillis() - start) + "ms)");
        }
        data.setCurrentVisitNode(parentVisitNode);
    }

    private void doFetchDependencies(VisitNode node, String conf) {
        Configuration c = node.getConfiguration(conf);
        if (c == null) {
            if (!node.isConfRequiredByMergedUsageOnly(conf)) {
                Message.warn("configuration not found '" + conf + "' in " + node.getResolvedId()
                        + ": ignoring");
                if (node.getParent() != null) {
                    Message.warn("it was required from " + node.getParent().getResolvedId());
                }
            }
            return;
        }
        // we handle the case where the asked configuration extends others:
        // we have to first fetch the extended configurations

        // first we check if this is the actual requested conf (not an extended one)
        boolean requestedConfSet = false;
        if (node.getRequestedConf() == null) {
            node.setRequestedConf(conf);
            requestedConfSet = true;
        }
        // now let's recurse in extended confs
        String[] extendedConfs = c.getExtends();
        if (extendedConfs.length > 0) {
            node.updateConfsToFetch(Arrays.asList(extendedConfs));
        }
        for (int i = 0; i < extendedConfs.length; i++) {
            fetchDependencies(node, extendedConfs[i], false);
        }

        // now we can actually resolve this configuration dependencies
        if (!isDependenciesFetched(node.getNode(), conf) && node.isTransitive()) {
            Collection<VisitNode> dependencies = node.getDependencies(conf);
            for (VisitNode dep : dependencies) {
                dep.useRealNode(); // the node may have been resolved to another real one while
                // resolving other deps
                String[] confs = dep.getRequiredConfigurations(node, conf);
                for (int i = 0; i < confs.length; i++) {
                    fetchDependencies(dep, confs[i], true);
                }
                if (!dep.isEvicted() && !dep.hasProblem()) {
                    // if there are still confs to fetch (usually because they have
                    // been updated when evicting another module), we fetch them now
                    confs = dep.getConfsToFetch();
                    for (int i = 0; i < confs.length; i++) {
                        // shouldBeFixed=false to because some of those dependencies might
                        // be private when they were actually extending public conf.
                        // Should we keep two list of confs to fetch (private&public)?
                        // I don't think, visibility is already checked, and a change in the
                        // configuration between version might anyway have worse problems.
                        fetchDependencies(dep, confs[i], false);
                    }
                }
            }
            markDependenciesFetched(node.getNode(), conf);
        }
        // we have finiched with this configuration, if it was the original requested conf
        // we can clean it now
        if (requestedConfSet) {
            node.setRequestedConf(null);
        }

    }

    /**
     * Returns true if we've already fetched the dependencies for this node and configuration
     * 
     * @param node
     *            node to check
     * @param conf
     *            configuration to check
     * @return true if we've already fetched this dependency
     */
    private boolean isDependenciesFetched(IvyNode node, String conf) {
        String key = getDependenciesFetchedKey(node, conf);
        return fetchedSet.contains(key);
    }

    private void markDependenciesFetched(IvyNode node, String conf) {
        String key = getDependenciesFetchedKey(node, conf);
        fetchedSet.add(key);
    }

    private String getDependenciesFetchedKey(IvyNode node, String conf) {
        ModuleRevisionId moduleRevisionId = node.getResolvedId();
        String key = moduleRevisionId.getOrganisation() + "|" + moduleRevisionId.getName() + "|"
                + moduleRevisionId.getRevision() + "|" + conf;
        return key;
    }

    private void resolveConflict(VisitNode node, String conf) {
        resolveConflict(node, node.getParent(), conf, Collections.<IvyNode> emptySet());
    }

    /**
     * Resolves conflict for the given node in the given ancestor. This method do conflict
     * resolution in ancestor parents recursively, unless not necessary.
     * 
     * @param node
     *            the node for which conflict resolution should be done
     * @param ancestor
     *            the ancestor in which the conflict resolution should be done
     * @param toevict
     *            a collection of IvyNode to evict (as computed by conflict resolution in
     *            descendants of ancestor)
     * @return true if conflict resolution has been done, false it can't be done yet
     */
    private boolean resolveConflict(VisitNode node, VisitNode ancestor, String conf,
            Collection<IvyNode> toevict) {
        if (ancestor == null || node == ancestor) {
            return true;
        }
        // check if job is not already done
        if (checkConflictSolvedEvicted(node, ancestor)) {
            // job is done and node is evicted, nothing to do
            return true;
        }
        boolean debugConflictResolution = settings.debugConflictResolution();
        if (checkConflictSolvedSelected(node, ancestor)) {
            // job is done and node is selected, nothing to do for this ancestor, but we still have
            // to check higher levels, for which conflict resolution might have been impossible
            // before
            if (resolveConflict(node, ancestor.getParent(), conf, toevict)) {
                // now that conflict resolution is ok in ancestors
                // we just have to check if the node wasn't previously evicted in root ancestor
                EvictionData evictionData = node.getEvictionDataInRoot(node.getRootModuleConf(),
                    ancestor);
                if (evictionData != null) {
                    // node has been previously evicted in an ancestor: we mark it as evicted
                    if (debugConflictResolution) {
                        Message.debug(node + " was previously evicted in root module conf "
                                + node.getRootModuleConf());
                    }
                    node.markEvicted(evictionData);
                    if (debugConflictResolution) {
                        Message.debug("evicting " + node + " by " + evictionData);
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        // compute conflicts
        Set<IvyNode> resolvedNodes = ancestor.getNode().getResolvedNodes(node.getModuleId(),
            node.getRootModuleConf());
        resolvedNodes.addAll(ancestor.getNode().getPendingConflicts(node.getRootModuleConf(),
            node.getModuleId()));
        Collection<IvyNode> conflicts = computeConflicts(node, ancestor, conf, toevict,
            resolvedNodes);

        ConflictManager conflictManager = ancestor.getNode().getConflictManager(node.getModuleId());

        Collection<IvyNode> resolved = resolveConflicts(node, ancestor, conflicts, conflictManager);

        if (resolved == null) {
            if (debugConflictResolution) {
                Message.debug("impossible to resolve conflicts for " + node + " in " + ancestor
                        + " yet");
                Message.debug("setting all nodes as pending conflicts for later conflict"
                        + " resolution: " + conflicts);
            }
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                conflicts);
            return false;
        }

        if (debugConflictResolution) {
            Message.debug("selected revisions for " + node + " in " + ancestor + ": " + resolved);
        }
        if (resolved.contains(node.getNode())) {
            // node has been selected for the current parent

            // handle previously selected nodes that are now evicted by this new node
            toevict = resolvedNodes;
            toevict.removeAll(resolved);

            for (IvyNode te : toevict) {
                te.markEvicted(node.getRootModuleConf(), ancestor.getNode(), conflictManager,
                    resolved);

                if (debugConflictResolution) {
                    Message.debug("evicting " + te + " by "
                            + te.getEvictedData(node.getRootModuleConf()));
                }
            }

            // it's very important to update resolved and evicted nodes BEFORE recompute parent call
            // to allow it to recompute its resolved collection with correct data
            // if necessary
            ancestor.getNode().setResolvedNodes(node.getModuleId(), node.getRootModuleConf(),
                resolved);

            Collection<IvyNode> evicted = new HashSet<IvyNode>(ancestor.getNode().getEvictedNodes(
                node.getModuleId(), node.getRootModuleConf()));
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            ancestor.getNode().setEvictedNodes(node.getModuleId(), node.getRootModuleConf(),
                evicted);
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                Collections.<IvyNode> emptySet());

            return resolveConflict(node, ancestor.getParent(), conf, toevict);
        } else {
            // node has been evicted for the current parent
            if (resolved.isEmpty()) {
                if (debugConflictResolution) {
                    Message.verbose("conflict manager '" + conflictManager
                            + "' evicted all revisions among " + conflicts);
                }
            }

            // it's time to update parent resolved and evicted with what was found

            Collection<IvyNode> evicted = new HashSet<IvyNode>(ancestor.getNode().getEvictedNodes(
                node.getModuleId(), node.getRootModuleConf()));
            toevict.removeAll(resolved);
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            evicted.add(node.getNode());
            ancestor.getNode().setEvictedNodes(node.getModuleId(), node.getRootModuleConf(),
                evicted);
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                Collections.<IvyNode> emptySet());

            node.markEvicted(ancestor, conflictManager, resolved);
            if (debugConflictResolution) {
                Message.debug("evicting " + node + " by " + node.getEvictedData());
            }

            // if resolved changed we have to go up in the graph
            Collection<IvyNode> prevResolved = ancestor.getNode().getResolvedNodes(
                node.getModuleId(), node.getRootModuleConf());
            boolean solved = true;
            if (!prevResolved.equals(resolved)) {
                ancestor.getNode().setResolvedNodes(node.getModuleId(), node.getRootModuleConf(),
                    resolved);
                for (IvyNode sel : resolved) {
                    if (!prevResolved.contains(sel)) {
                        solved &= resolveConflict(node.gotoNode(sel), ancestor.getParent(), conf,
                            toevict);
                    }
                }
            }
            return solved;
        }
    }

    private Collection<IvyNode> resolveConflicts(VisitNode node, VisitNode ancestor,
            Collection<IvyNode> conflicts, ConflictManager conflictManager) {
        if (node.getParent() != ancestor
        // we are not handling the direct parent

                && conflictManager == settings.getConflictManager(node.getModuleId())
                // the conflict manager is the default one

                && node.getParent().getNode()
                        .getResolvedNodes(node.getModuleId(), node.getRootModuleConf())
                        .equals(conflicts)
        // there is no new conflict in this ancestor

        ) {
            // IVY-465 case
            if (settings.debugConflictResolution()) {
                Message.debug("no new conflicting revisions for " + node + " in " + ancestor + ": "
                        + conflicts);
            }

            return conflicts;
        } else {
            if (settings.debugConflictResolution()) {
                Message.debug("found conflicting revisions for " + node + " in " + ancestor + ": "
                        + conflicts);
            }

            return conflictManager.resolveConflicts(ancestor.getNode(), conflicts);
        }
    }

    /**
     * Compute possible conflicts for a node, in the context of an ancestor (a node which has a
     * dependency - direct or indirect - on the node for which conflicts should be computed.
     * 
     * @param node
     *            the node for which conflicts should be computed
     * @param ancestor
     *            the ancestor in which conflicts should be computed
     * @param conf
     *            the configuration of the node in which conflicts should be computed
     * @param toevict
     *            a collection of nodes which have been evicted during conflict resolution at lower
     *            level. It may be empty if no conflict resolution has occurred for this node yet,
     *            or if no node has been evicted.
     * @param selectedNodes
     *            a collection of nodes selected during previous conflict resolution for the given
     *            node and ancestor. This collection is updated by this call, removing nodes which
     *            should be evicted.
     * @return a collection of IvyNode which may be in conflict with the given node in the given
     *         ancestor. This collection always contain at least the given node.
     */
    private Collection<IvyNode> computeConflicts(VisitNode node, VisitNode ancestor, String conf,
            Collection<IvyNode> toevict, Collection<IvyNode> selectedNodes) {
        Collection<IvyNode> conflicts = new LinkedHashSet<IvyNode>();
        conflicts.add(node.getNode());
        /*
         * We first try to remove all evicted nodes from the collection of selected nodes to update
         * this collection. If the collection changes, it means that it contained evicted nodes, and
         * thus is not up to date.
         */
        boolean evictedInSelected = selectedNodes.removeAll(toevict);
        /*
         * Another case where we need to deeply compute selected nodes is when selectedNodes is
         * empty (not computed yet) and we aren't in the context of the direct parent of the node.
         */
        if (evictedInSelected
                || (selectedNodes.isEmpty() && !node.getParent().getNode()
                        .equals(ancestor.getNode()))) {
            IvyContext context = IvyContext.getContext();
            ResolveData data = context.getResolveData();
            VisitNode oldVisitNode = data.getCurrentVisitNode();
            data.setCurrentVisitNode(ancestor);
            try {
                // In this case we need to compute selected nodes again.
                Collection<IvyNode> deps = ancestor.getNode().getDependencies(
                    node.getRootModuleConf(),
                    ancestor.getNode().getConfigurations(node.getRootModuleConf()),
                    ancestor.getRequestedConf());
                for (IvyNode dep : deps) {
                    if (dep.getModuleId().equals(node.getModuleId())) {
                        conflicts.add(dep);
                    }
                    conflicts.addAll(dep.getResolvedNodes(node.getModuleId(),
                        node.getRootModuleConf()));
                }
            } finally {
                data.setCurrentVisitNode(oldVisitNode);
            }
        } else if (selectedNodes.isEmpty()) {
            /*
             * No selected nodes at all yet, and we are in the context of the direct parent
             * (otherwise previous block would have been reached). We can compute conflicts based on
             * the parent direct dependencies in current root module conf.
             */
            VisitNode parent = node.getParent();
            Collection<IvyNode> parentDepIvyNodes = parent.getNode().getDependencies(
                node.getRootModuleConf(),
                parent.getNode().getConfigurations(node.getRootModuleConf()),
                parent.getRequestedConf());
            for (IvyNode parentDep : parentDepIvyNodes) {
                if (parentDep.getModuleId().equals(node.getModuleId())) {
                    conflicts.add(parentDep);
                }
            }
        } else {
            conflicts.addAll(selectedNodes);
        }
        return conflicts;
    }

    private boolean checkConflictSolvedSelected(VisitNode node, VisitNode ancestor) {
        if (ancestor.getResolvedRevisions(node.getModuleId()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            if (settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for " + node + " in " + ancestor);
            }
            return true;
        }
        return false;
    }

    private boolean checkConflictSolvedEvicted(VisitNode node, VisitNode ancestor) {
        if (ancestor.getEvictedRevisions(node.getModuleId()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            if (settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for " + node + " in " + ancestor);
            }
            return true;
        }
        return false;
    }

    public ResolvedModuleRevision findModule(ModuleRevisionId id, ResolveOptions options) {
        DependencyResolver r = settings.getResolver(id);
        if (r == null) {
            throw new IllegalStateException("no resolver found for " + id.getModuleId());
        }
        DefaultModuleDescriptor md = DefaultModuleDescriptor.newCallerInstance(id,
            new String[] {"*"}, false, false);

        if (options.getResolveId() == null) {
            options.setResolveId(ResolveOptions.getDefaultResolveId(md));
        }

        try {
            return r.getDependency(new DefaultDependencyDescriptor(id, true), new ResolveData(this,
                    options, new ConfigurationResolveReport(this, md, "default", null, options)));
        } catch (ParseException e) {
            throw new RuntimeException("problem while parsing repository module descriptor for "
                    + id + ": " + e, e);
        }
    }

    /**
     * Mediates the given dependency descriptor according to given options.
     * <p>
     * The mediated dependency descriptor must return the actually requested module revision id when
     * the method {@link DependencyDescriptor#getDependencyRevisionId()} is called.
     * </p>
     * 
     * @param dd
     *            the dependency descriptor for which the requested module revision id should be
     *            returned
     * @param options
     *            the resolve options to use
     * @return the mediated {@link DependencyDescriptor}.
     */
    public DependencyDescriptor mediate(DependencyDescriptor dd, ResolveOptions options) {
        if (dd == null) {
            return null;
        }
        String resolveMode = options.getResolveMode() == null ? settings.getResolveMode(dd
                .getDependencyId()) : options.getResolveMode();
        if (ResolveOptions.RESOLVEMODE_DYNAMIC.equals(resolveMode)
                && !dd.getDynamicConstraintDependencyRevisionId().equals(
                    dd.getDependencyRevisionId())) {
            // the dynamicRevId can contain a null branch, so make sure this
            // has been replaced by the default branch (if any!)
            return dd.clone(ModuleRevisionId.newInstance(dd
                    .getDynamicConstraintDependencyRevisionId(), dd
                    .getDynamicConstraintDependencyRevisionId().getRevision()));
        } else {
            return dd;
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ResolveEngineSettings getSettings() {
        return settings;
    }

    public SortEngine getSortEngine() {
        return sortEngine;
    }

    private void checkInterrupted() {
        IvyContext.getContext().getIvy().checkInterrupted();
    }

}
