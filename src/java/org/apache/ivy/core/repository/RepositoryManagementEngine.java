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
package org.apache.ivy.core.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.matcher.RegexpPatternMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.MemoryUtil;
import org.apache.ivy.util.Message;

/**
 * The repository management can be used to load all metadata from a repository, analyze them, and
 * provide a bunch of information about the whole repository state.
 * <p>
 * Since loading all metadata from a repository is not a light task, this engine should only be used
 * on a machine having good access to the repository (on the same filesystem being usually the best
 * suited).
 * </p>
 * <p>
 * To access information, you usually have before to call a method to init the data: {@link #load()}
 * is used to load repository metadata, {@link #analyze()} is used to analyze them. These methods
 * being very time consuming, they must always be called explicitly.
 * </p>
 * <p>
 * On a large repository, this engine can be very memory consuming to use, it is not suited to be
 * used in a long running process, but rather in short process loading data and taking action about
 * the current state of the repository.
 * </p>
 * <p>
 * This engine is not intended to be used concurrently with publish, the order of repository loaded
 * being undeterministic and long, it could end up in having an inconsistent in memory state.
 * </p>
 * <p>
 * For better performance, we strongly suggest using this engine with cache in useOrigin mode.
 * </p>
 */
public class RepositoryManagementEngine {
    private static final double THOUSAND = 1000.0;

    private static final int KILO = 1024;

    // /////////////////////////////////////////
    // state loaded on #load()
    // /////////////////////////////////////////

    /**
     * True if the repository has already been loaded, false otherwise.
     */
    private boolean loaded;

    /**
     * ModuleDescriptors stored by ModuleRevisionId
     */
    private Map/* <ModuleRevisionId,ModuleDescriptor> */revisions = new HashMap();

    /**
     * ModuleRevisionId for which loading was not possible, with corresponding error message.
     */
    private Map/* <ModuleRevisionId,String> */errors = new HashMap();

    /**
     * List of ModuleRevisionId per ModuleId.
     */
    private Map/* <ModuleId,Collection<ModuleRevisionId>> */modules = new HashMap();

    // /////////////////////////////////////////
    // state loaded on #analyze()
    // /////////////////////////////////////////

    /**
     * True when the repository has been analyzed, false otherwise
     */
    private boolean analyzed;

    /**
     * Cache from requested module revision id to actual module revision id.
     */
    private Map/* <ModuleRevisionId,ModuleRevisionId> */cache = new HashMap();

    /**
     * list of dependers per ModuleRevisionId.
     */
    private Map/* <ModuleRevisionId,List<ModuleRevisionId>> */dependers = new HashMap();

    // /////////////////////////////////////////
    // dependencies
    // /////////////////////////////////////////
    private SearchEngine searchEngine;

    private ResolveEngine resolveEngine;

    private RepositoryManagementEngineSettings settings;

    public RepositoryManagementEngine(RepositoryManagementEngineSettings settings,
            SearchEngine searchEngine, ResolveEngine resolveEngine) {
        this.settings = settings;
        this.searchEngine = searchEngine;
        this.resolveEngine = resolveEngine;
    }

    /**
     * Loads data from the repository.
     * <p>
     * This method usually takes a long time to proceed. It should never be called from event
     * dispatch thread in a GUI.
     * </p>
     */
    public void load() {
        long startingMemoryUse = 0;
        if (settings.dumpMemoryUsage()) {
            startingMemoryUse = MemoryUtil.getUsedMemory();
        }
        long startTime = System.currentTimeMillis();
        Message.rawinfo("searching modules... ");
        ModuleRevisionId[] mrids = searchModules();
        Message.info("loading repository metadata...");
        for (int i = 0; i < mrids.length; i++) {
            try {
                loadModuleRevision(mrids[i]);
            } catch (Exception e) {
                Message.debug(e);
                errors.put(mrids[i], e.getMessage());
            }
        }
        long endTime = System.currentTimeMillis();
        Message.info("\nrepository loaded: "
                + modules.size()
                + " modules; "
                + revisions.size()
                + " revisions; "
                + (settings.dumpMemoryUsage() ? (MemoryUtil.getUsedMemory() - startingMemoryUse)
                        / KILO + "kB; " : "") + (endTime - startTime) / THOUSAND + "s");
        loaded = true;
    }

    /**
     * Analyze data in the repository.
     * <p>
     * This method may take a long time to proceed. It should never be called from event dispatch
     * thread in a GUI.
     * </p>
     * 
     * @throws IllegalStateException
     *             if the repository has not been loaded yet
     * @see #load()
     */
    public void analyze() {
        ensureLoaded();
        Message.info("\nanalyzing dependencies...");
        for (Iterator iterator = revisions.values().iterator(); iterator.hasNext();) {
            ModuleDescriptor md = (ModuleDescriptor) iterator.next();
            DependencyDescriptor[] dds = md.getDependencies();
            for (int i = 0; i < dds.length; i++) {
                ModuleRevisionId dep = getDependency(dds[i]);
                if (dep == null) {
                    Message.warn("inconsistent repository: declared dependency not found: "
                            + dds[i]);
                } else {
                    getDependers(dep).add(md.getModuleRevisionId());
                }
            }
            Message.progress();
        }
        analyzed = true;
    }

    /**
     * Returns the number of Module Revision in the repository.
     * 
     * @return the number of module revisions in the repository.
     * @throws IllegalStateException
     *             if the repository has not been loaded yet
     * @see #load()
     */
    public int getRevisionsNumber() {
        ensureLoaded();
        return revisions.size();
    }

    /**
     * Returns the number of ModuleId in the repository.
     * 
     * @return the number of ModuleId in the repository.
     * @throws IllegalStateException
     *             if the repository has not been loaded yet
     * @see #load()
     */
    public int getModuleIdsNumber() {
        ensureLoaded();
        return modules.size();
    }

    /**
     * Returns Module Revisions which have no dependers.
     * 
     * @return a Collection of the {@link ModuleRevisionId} of module revisions which have no
     *         dependers in the repository.
     * @throws IllegalStateException
     *             if the repository has not been analyzed yet
     * @see #analyze()
     */
    public Collection getOrphans() {
        ensureAnalyzed();
        Collection orphans = new HashSet(revisions.keySet());
        orphans.removeAll(dependers.keySet());
        return orphans;
    }

    private ModuleRevisionId[] searchModules() {
        ModuleRevisionId[] mrids = searchEngine.listModules(ModuleRevisionId.newInstance(
            PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION,
            PatternMatcher.ANY_EXPRESSION, PatternMatcher.ANY_EXPRESSION),
            RegexpPatternMatcher.INSTANCE);
        return mrids;
    }

    private ModuleRevisionId getDependency(DependencyDescriptor dd) {
        ModuleRevisionId askedMrid = dd.getDependencyRevisionId();
        VersionMatcher vmatcher = settings.getVersionMatcher();
        if (vmatcher.isDynamic(askedMrid)) {
            ModuleRevisionId mrid = (ModuleRevisionId) cache.get(askedMrid);
            if (mrid == null) {
                Collection revs = getAllRevisions(askedMrid);
                for (Iterator iterator = revs.iterator(); iterator.hasNext();) {
                    ModuleDescriptor md = (ModuleDescriptor) iterator.next();
                    if (vmatcher.needModuleDescriptor(askedMrid, md.getResolvedModuleRevisionId())) {
                        if (vmatcher.accept(askedMrid, md)) {
                            mrid = md.getResolvedModuleRevisionId();
                            break;
                        }
                    } else {
                        if (vmatcher.accept(askedMrid, md.getResolvedModuleRevisionId())) {
                            mrid = md.getResolvedModuleRevisionId();
                            break;
                        }
                    }
                }
                if (mrid == null) {
                    return null;
                } else {
                    cache.put(askedMrid, mrid);
                }
            }
            return mrid;
        } else {
            return askedMrid;
        }
    }

    private Collection getDependers(ModuleRevisionId id) {
        Collection depders = (Collection) dependers.get(id);
        if (depders == null) {
            depders = new ArrayList();
            dependers.put(id, depders);
        }
        return depders;
    }

    private void loadModuleRevision(ModuleRevisionId mrid) throws Exception {
        ResolvedModuleRevision module = settings.getResolver(mrid).getDependency(
            new DefaultDependencyDescriptor(mrid, false), newResolveData());
        if (module == null) {
            Message.warn("module not found while listed: " + mrid);
        } else {
            revisions.put(module.getId(), module.getDescriptor());
            getAllRevisions(module.getId()).add(module.getDescriptor());
        }
        Message.progress();
    }

    private Collection getAllRevisions(ModuleRevisionId id) {
        Collection revisions = (Collection) modules.get(id.getModuleId());
        if (revisions == null) {
            revisions = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    ModuleDescriptor md1 = (ModuleDescriptor) o1;
                    ModuleDescriptor md2 = (ModuleDescriptor) o2;
                    // we use reverse order compared to latest revision, to have latest revision
                    // first
                    return settings.getDefaultLatestStrategy().sort(new ArtifactInfo[] {md1, md2})
                            .get(0).equals(md1) ? 1 : -1;
                }
            });
            modules.put(id.getModuleId(), revisions);
        }
        return revisions;
    }

    private ResolveData newResolveData() {
        return new ResolveData(resolveEngine, new ResolveOptions());
    }

    private void ensureAnalyzed() {
        if (!analyzed) {
            throw new IllegalStateException(
                    "repository must have been analyzed to perform this method");
        }
    }

    private void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException("repository must have be loaded to perform this method");
        }
    }
}
