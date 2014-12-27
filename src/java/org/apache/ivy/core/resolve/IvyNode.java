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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.LatestCompatibleConflictManager;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

public class IvyNode implements Comparable<IvyNode> {
    private static final Pattern FALLBACK_CONF_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    // //////// CONTEXT
    private ResolveData data;

    private ResolveEngineSettings settings;

    // //////// DELEGATES
    private IvyNodeCallers callers;

    private IvyNodeEviction eviction;

    // //////// MAIN DATA

    private IvyNode root;

    // id as requested, i.e. may be with latest rev
    private ModuleRevisionId id;

    // set only when node has been built or updated from a DependencyDescriptor
    private Map<IvyNode, DependencyDescriptor> dds = new HashMap<IvyNode, DependencyDescriptor>();

    // Set when data has been loaded only, or when constructed from a module descriptor
    private ModuleDescriptor md;

    private ResolvedModuleRevision module;

    // //////// LOADING METADATA
    private Exception problem = null;

    private boolean downloaded = false;

    private boolean searched = false;

    private Collection<String> confsToFetch = new HashSet<String>();

    private Collection<String> fetchedConfigurations = new HashSet<String>();

    private Collection<String> loadedRootModuleConfs = new HashSet<String>();

    // //////// USAGE DATA

    private IvyNodeUsage usage = new IvyNodeUsage(this);

    // usage information merged from evicted nodes this node is "replacing"
    private Map<ModuleRevisionId, IvyNodeUsage> mergedUsages = new LinkedHashMap<ModuleRevisionId, IvyNodeUsage>();

    public IvyNode(ResolveData data, IvyNode parent, DependencyDescriptor dd) {
        id = dd.getDependencyRevisionId();
        dds.put(parent, dd);
        root = parent.getRoot();
        init(data);
    }

    public IvyNode(ResolveData data, ModuleDescriptor md) {
        id = md.getModuleRevisionId();
        this.md = md;
        root = this;
        init(data);
    }

    private void init(ResolveData data) {
        this.data = data;
        settings = data.getSettings();
        eviction = new IvyNodeEviction(this);
        callers = new IvyNodeCallers(this);
    }

    /**
     * After the call node may be discarded. To avoid using discarded node, make sure to get the
     * real node after the call IvyNode node = ... node.loadData(); node = node.getRealNode(); ...
     */
    public boolean loadData(String rootModuleConf, IvyNode parent, String parentConf, String conf,
            boolean shouldBePublic, IvyNodeUsage usage) {
        Message.debug("loadData of " + this.toString() + " of rootConf=" + rootModuleConf);
        if (!isRoot() && (data.getReport() != null)) {
            data.getReport().addDependency(this);
        }

        boolean loaded = false;
        if (hasProblem()) {
            Message.debug("Node has problem.  Skip loading");
        } else if (isEvicted(rootModuleConf)) {
            Message.debug(rootModuleConf + " is evicted.  Skip loading");
        } else if (!hasConfigurationsToLoad() && isRootModuleConfLoaded(rootModuleConf)) {
            Message.debug(rootModuleConf + " is loaded and no conf to load.  Skip loading");
        } else {
            markRootModuleConfLoaded(rootModuleConf);
            if (md == null) {
                DependencyResolver resolver = data.getSettings().getResolver(getId());
                if (resolver == null) {
                    Message.error("no resolver found for " + getModuleId()
                            + ": check your configuration");
                    problem = new RuntimeException("no resolver found for " + getModuleId()
                            + ": check your configuration");
                    return false;
                }
                try {
                    Message.debug("\tusing " + resolver + " to resolve " + getId());
                    DependencyDescriptor dependencyDescriptor = getDependencyDescriptor(parent);
                    long start = System.currentTimeMillis();
                    ModuleRevisionId requestedRevisionId = dependencyDescriptor
                            .getDependencyRevisionId();
                    data.getEventManager().fireIvyEvent(
                        new StartResolveDependencyEvent(resolver, dependencyDescriptor,
                                requestedRevisionId));
                    module = resolver.getDependency(dependencyDescriptor, data);
                    data.getEventManager().fireIvyEvent(
                        new EndResolveDependencyEvent(resolver, dependencyDescriptor,
                                requestedRevisionId, module, System.currentTimeMillis() - start));

                    if (module != null) {
                        module.getResolver()
                                .getRepositoryCacheManager()
                                .saveResolvers(module.getDescriptor(),
                                    module.getResolver().getName(),
                                    module.getArtifactResolver().getName());
                        if (settings.logModuleWhenFound()
                                && LogOptions.LOG_DEFAULT.equals(getData().getOptions().getLog())) {
                            Message.info("\tfound " + module.getId() + " in "
                                    + module.getResolver().getName());
                        } else {
                            Message.verbose("\tfound " + module.getId() + " in "
                                    + module.getResolver().getName());
                        }

                        // IVY-56: check if revision has actually been resolved
                        if (settings.getVersionMatcher().isDynamic(getId())
                                && settings.getVersionMatcher().isDynamic(module.getId())) {
                            Message.error("impossible to resolve dynamic revision for " + getId()
                                    + ": check your configuration and "
                                    + "make sure revision is part of your pattern");
                            problem = new RuntimeException("impossible to resolve dynamic revision");
                            return false;
                        }
                        if (!getId().equals(module.getId())) {
                            IvyNode resolved = data.getNode(module.getId());
                            if (resolved != null) {
                                // found revision has already been resolved
                                // => update it and discard this node
                                md = module.getDescriptor(); // needed for handleConfiguration
                                if (!handleConfiguration(loaded, rootModuleConf, parent,
                                    parentConf, conf, shouldBePublic, usage)) {
                                    return false;
                                }

                                moveToRealNode(rootModuleConf, parent, parentConf, conf,
                                    shouldBePublic, resolved);

                                return true;
                            }
                            String log = "\t[" + module.getId().getRevision() + "] " + getId();
                            if (!settings.getVersionMatcher().isDynamic(getId())) {
                                log += " (forced)";
                            }
                            if (settings.logResolvedRevision()
                                    && LogOptions.LOG_DEFAULT.equals(getData().getOptions()
                                            .getLog())) {
                                Message.info(log);
                            } else {
                                Message.verbose(log);
                            }
                        }
                        downloaded = module.getReport().isDownloaded();
                        searched = module.getReport().isSearched();
                        loaded = true;

                        md = module.getDescriptor();
                        confsToFetch.remove("*");
                        updateConfsToFetch(Arrays
                                .asList(resolveSpecialConfigurations(getRequiredConfigurations(
                                    parent, parentConf))));
                    } else {
                        Message.warn("\tmodule not found: " + getId());
                        resolver.reportFailure();
                        problem = new RuntimeException("not found");
                        return false;
                    }
                } catch (ResolveProcessException e) {
                    throw e;
                } catch (Exception e) {
                    problem = e;
                    Message.debug("Unexpected error: " + problem.getMessage(), problem);
                    return false;
                }
            } else {
                loaded = true;
            }
        }
        handleConfiguration(loaded, rootModuleConf, parent, parentConf, conf, shouldBePublic, usage);
        if (hasProblem()) {
            Message.debug("problem : " + problem.getMessage());
            return false;
        } else {
            DependencyDescriptor dd = getDependencyDescriptor(parent);
            if (dd != null) {
                usage.addUsage(rootModuleConf, dd, parentConf);
            }
            return loaded;
        }
    }

    private void moveToRealNode(String rootModuleConf, IvyNode parent, String parentConf,
            String conf, boolean shouldBePublic, IvyNode resolved) {
        if (resolved.md == null) {
            resolved.md = md;
        }
        if (resolved.module == null) {
            resolved.module = module;
        }
        resolved.downloaded |= module.getReport().isDownloaded();
        resolved.searched |= module.getReport().isSearched();
        resolved.dds.putAll(dds);
        resolved.updateDataFrom(this, rootModuleConf, true);
        resolved.loadData(rootModuleConf, parent, parentConf, conf, shouldBePublic, usage);
        resolved.usage.updateDataFrom(getAllUsages(), rootModuleConf);
        usage = resolved.usage;

        data.replaceNode(getId(), resolved, rootModuleConf); // this actually discards the node

        if (settings.logResolvedRevision()
                && LogOptions.LOG_DEFAULT.equals(getData().getOptions().getLog())) {
            Message.info("\t[" + module.getId().getRevision() + "] " + getId());
        } else {
            Message.verbose("\t[" + module.getId().getRevision() + "] " + getId());
        }
    }

    public Collection<IvyNode> getDependencies(String rootModuleConf, String[] confs,
            String requestedConf) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get dependencies when data has not been loaded");
        }
        if (Arrays.asList(confs).contains("*")) {
            if (isRoot()) {
                confs = md.getConfigurationsNames();
            } else {
                confs = md.getPublicConfigurationsNames();
            }
        }
        Collection<IvyNode> deps = new HashSet<IvyNode>();
        for (int i = 0; i < confs.length; i++) {
            deps.addAll(getDependencies(rootModuleConf, confs[i], requestedConf));
        }
        return deps;
    }

    /**
     * Load the dependencies of the current node
     * <p>
     * The resulting collection of nodes may have some configuration to load
     * 
     * @param rootModuleConf
     *            the requested configuration of the root module
     * @param conf
     *            the configuration to load of this node
     * @param requestedConf
     *            the actual node conf requested, possibly extending the <code>conf</code> one.
     * @return
     */
    public Collection<IvyNode> getDependencies(String rootModuleConf, String conf,
            String requestedConf) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get dependencies when data has not been loaded");
        }
        DependencyDescriptor[] dds = md.getDependencies();
        // it's important to respect order => LinkedHashMap
        Map<ModuleRevisionId, IvyNode> dependencies = new LinkedHashMap<ModuleRevisionId, IvyNode>();
        for (int i = 0; i < dds.length; i++) {
            DependencyDescriptor dd = data.mediate(dds[i]);
            String[] dependencyConfigurations = dd.getDependencyConfigurations(conf, requestedConf);
            if (dependencyConfigurations.length == 0) {
                // no configuration of the dependency is required for current confs :
                // it is exactly the same as if there was no dependency at all on it
                continue;
            }
            ModuleRevisionId requestedDependencyRevisionId = dd.getDependencyRevisionId();
            if (isDependencyModuleExcluded(dd, rootModuleConf, requestedDependencyRevisionId, conf)) {
                // the whole module is excluded, it is considered as not being part of dependencies
                // at all
                Message.verbose("excluding " + dd + " in " + conf);
                continue;
            }

            // check if not already loaded here
            IvyNode depNode = dependencies.get(requestedDependencyRevisionId);
            if (depNode == null) {
                // check if not already loaded during the resolve session
                depNode = data.getNode(requestedDependencyRevisionId);
            }

            if (depNode == null) {
                depNode = new IvyNode(data, this, dd);
            } else {
                depNode.addDependencyDescriptor(this, dd);
                if (depNode.hasProblem()) {
                    // dependency already tried to be resolved, but unsuccessfully
                    // nothing special to do
                }

            }
            String[] confsArray = depNode.resolveSpecialConfigurations(dependencyConfigurations);
            Collection<String> confs = Arrays.asList(confsArray);
            depNode.updateConfsToFetch(confs);
            depNode.addRootModuleConfigurations(depNode.usage, rootModuleConf, confsArray);
            depNode.usage.setRequiredConfs(this, conf, confs);

            depNode.addCaller(rootModuleConf, this, conf, requestedConf, dependencyConfigurations,
                dd);
            dependencies.put(requestedDependencyRevisionId, depNode);
        }
        return dependencies.values();
    }

    private void addDependencyDescriptor(IvyNode parent, DependencyDescriptor dd) {
        dds.put(parent, dd);
    }

    public DependencyDescriptor getDependencyDescriptor(IvyNode parent) {
        return dds.get(parent);
    }

    private boolean isDependencyModuleExcluded(DependencyDescriptor dd, String rootModuleConf,
            ModuleRevisionId dependencyRevisionId, String conf) {
        Artifact a = DefaultArtifact.newIvyArtifact(dependencyRevisionId, null);
        if (isRoot()) {
            // no callers, but maybe some exclude
            Boolean exclude = doesExclude(md, rootModuleConf, new String[] {rootModuleConf}, dd, a,
                new Stack<ModuleRevisionId>());
            return exclude == null ? false : exclude.booleanValue();
        }
        return callers.doesCallersExclude(rootModuleConf, a);
    }

    Boolean doesExclude(ModuleDescriptor md, String rootModuleConf, String[] moduleConfs,
            DependencyDescriptor dd, Artifact artifact, Stack<ModuleRevisionId> callersStack) {
        // artifact is excluded if it match any of the exclude pattern for this dependency...
        if (dd != null) {
            if (dd.doesExclude(moduleConfs, artifact.getId().getArtifactId())) {
                return Boolean.TRUE;
            }
        }
        if (md.doesExclude(moduleConfs, artifact.getId().getArtifactId())) {
            return Boolean.TRUE;
        }
        // ... or if it is excluded by all its callers
        IvyNode c = getData().getNode(md.getModuleRevisionId());
        if (c != null) {
            if (callersStack.contains(c.getId())) {
                // a circular dependency, we cannot be conclusive here
                return null;
            }
            return Boolean.valueOf(c.doesCallersExclude(rootModuleConf, artifact, callersStack));
        } else {
            return Boolean.FALSE;
        }
    }

    public boolean hasConfigurationsToLoad() {
        return !confsToFetch.isEmpty();
    }

    private boolean markRootModuleConfLoaded(String rootModuleConf) {
        return loadedRootModuleConfs.add(rootModuleConf);
    }

    private boolean isRootModuleConfLoaded(String rootModuleConf) {
        return loadedRootModuleConfs.contains(rootModuleConf);
    }

    private boolean handleConfiguration(boolean loaded, String rootModuleConf, IvyNode parent,
            String parentConf, String conf, boolean shouldBePublic, IvyNodeUsage usage) {
        if (md != null) {
            String[] confs = getRealConfs(conf);
            addRootModuleConfigurations(usage, rootModuleConf, confs);
            for (int i = 0; i < confs.length; i++) {
                Configuration c = md.getConfiguration(confs[i]);
                if (c == null) {
                    confsToFetch.remove(conf);
                    if (isConfRequiredByMergedUsageOnly(rootModuleConf, conf)) {
                        Message.verbose("configuration required by evicted revision is not available in "
                                + "selected revision. skipping " + conf + " in " + this);
                    } else if (!conf.equals(confs[i])) {
                        problem = new RuntimeException("configuration not found in " + this + ": '"
                                + conf + "'. Missing configuration: '" + confs[i]
                                + "'. It was required from " + parent + " " + parentConf);
                    } else {
                        problem = new RuntimeException("configuration not found in " + this + ": '"
                                + confs[i] + "'. It was required from " + parent + " " + parentConf);
                    }
                    return false;
                } else if (shouldBePublic && !isRoot()
                        && c.getVisibility() != Configuration.Visibility.PUBLIC) {
                    confsToFetch.remove(conf);
                    if (isConfRequiredByMergedUsageOnly(rootModuleConf, conf)) {
                        Message.verbose("configuration required by evicted revision is not visible in "
                                + "selected revision. skipping " + conf + " in " + this);
                    } else {
                        problem = new RuntimeException("configuration not public in " + this
                                + ": '" + c + "'. It was required from " + parent + " "
                                + parentConf);
                    }
                    return false;
                }
            }
            if (loaded) {
                fetchedConfigurations.add(conf);
                confsToFetch.removeAll(Arrays.asList(confs));
                confsToFetch.remove(conf);
            }
        }
        return true;
    }

    private String getDefaultConf(String conf) {
        Matcher m = FALLBACK_CONF_PATTERN.matcher(conf);
        if (m.matches()) {
            return m.group(2);
        } else {
            return conf;
        }
    }

    private String getMainConf(String conf) {
        Matcher m = FALLBACK_CONF_PATTERN.matcher(conf);
        if (m.matches()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    public void updateConfsToFetch(Collection<String> confs) {
        confsToFetch.addAll(confs);
        confsToFetch.removeAll(fetchedConfigurations);
    }

    /**
     * resolve the '*' special configurations if necessary and possible
     */
    private String[] resolveSpecialConfigurations(String[] dependencyConfigurations) {
        if (dependencyConfigurations.length == 1 && dependencyConfigurations[0].startsWith("*")
                && isLoaded()) {
            String conf = dependencyConfigurations[0];
            if ("*".equals(conf)) {
                return getDescriptor().getPublicConfigurationsNames();
            }
            // there are exclusions in the configuration
            List<String> exclusions = Arrays.asList(conf.substring(2).split("\\!"));

            List<String> ret = new ArrayList<String>(Arrays.asList(getDescriptor()
                    .getPublicConfigurationsNames()));
            ret.removeAll(exclusions);

            return ret.toArray(new String[ret.size()]);
        }
        return dependencyConfigurations;
    }

    /**
     * returns the required configurations from the given node
     * 
     * @param in
     * @return
     */
    public String[] getRequiredConfigurations(IvyNode in, String inConf) {
        Collection<String> req = new LinkedHashSet<String>();
        addAllIfNotNull(req, usage.getRequiredConfigurations(in, inConf));
        for (IvyNodeUsage usage : mergedUsages.values()) {
            addAllIfNotNull(req, usage.getRequiredConfigurations(in, inConf));
        }
        return req == null ? new String[0] : req.toArray(new String[req.size()]);
    }

    private <T> void addAllIfNotNull(Collection<T> into, Collection<T> col) {
        if (col != null) {
            into.addAll(col);
        }
    }

    /**
     * returns all the current required configurations of the node
     * 
     * @return
     */
    public String[] getRequiredConfigurations() {
        Collection<String> required = new ArrayList<String>(confsToFetch.size()
                + fetchedConfigurations.size());
        required.addAll(fetchedConfigurations);
        required.addAll(confsToFetch);
        return required.toArray(new String[required.size()]);
    }

    public Configuration getConfiguration(String conf) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get configuration when data has not been loaded");
        }
        String defaultConf = getDefaultConf(conf);
        conf = getMainConf(conf);
        Configuration configuration = md.getConfiguration(conf);
        if (configuration == null) {
            configuration = md.getConfiguration(defaultConf);
        }
        return configuration;
    }

    /**
     * Returns the configurations of the dependency required in a given root module configuration.
     * 
     * @param rootModuleConf
     * @return
     */
    public String[] getConfigurations(String rootModuleConf) {
        Set<String> depConfs = new LinkedHashSet<String>();
        addAllIfNotNull(depConfs, usage.getConfigurations(rootModuleConf));
        for (IvyNodeUsage usage : mergedUsages.values()) {
            addAllIfNotNull(depConfs, usage.getConfigurations(rootModuleConf));
        }
        return depConfs.toArray(new String[depConfs.size()]);
    }

    protected boolean isConfRequiredByMergedUsageOnly(String rootModuleConf, String conf) {
        Set<String> confs = usage.getConfigurations(rootModuleConf);
        return confs == null || !confs.contains(conf);
    }

    // This is never called. Could we remove it?
    public void discardConf(String rootModuleConf, String conf) {
        Set<String> depConfs = usage.addAndGetConfigurations(rootModuleConf);
        if (md != null) {
            // remove all given dependency configurations to the set + extended ones
            Configuration c = md.getConfiguration(conf);
            if (conf != null) {
                String[] exts = c.getExtends();
                for (int i = 0; i < exts.length; i++) {
                    discardConf(rootModuleConf, exts[i]); // recursive remove of extended
                    // configurations
                }
                depConfs.remove(c.getName());
            } else {
                Message.warn("unknown configuration in " + getId() + ": " + conf);
            }
        } else {
            depConfs.remove(conf);
        }
    }

    private void addRootModuleConfigurations(IvyNodeUsage usage, String rootModuleConf,
            String[] dependencyConfs) {
        Set<String> depConfs = usage.addAndGetConfigurations(rootModuleConf);
        if (md != null) {
            // add all given dependency configurations to the set + extended ones
            for (int i = 0; i < dependencyConfs.length; i++) {
                depConfs.add(dependencyConfs[i]);
                Configuration conf = md.getConfiguration(dependencyConfs[i]);
                if (conf != null) {
                    String[] exts = conf.getExtends();
                    // recursive add of extended
                    addRootModuleConfigurations(usage, rootModuleConf, exts);
                }
            }
        } else {
            for (int i = 0; i < dependencyConfs.length; i++) {
                depConfs.add(dependencyConfs[i]);
            }
        }
    }

    /**
     * Returns the root module configurations in which this dependency is required
     * 
     * @return
     */
    public String[] getRootModuleConfigurations() {
        Set<String> confs = getRootModuleConfigurationsSet();
        return confs.toArray(new String[confs.size()]);
    }

    /**
     * Returns the root module configurations in which this dependency is required
     * 
     * @return
     */
    public Set<String> getRootModuleConfigurationsSet() {
        Set<String> confs = new LinkedHashSet<String>();
        addAllIfNotNull(confs, usage.getRootModuleConfigurations());
        for (IvyNodeUsage usage : mergedUsages.values()) {
            addAllIfNotNull(confs, usage.getRootModuleConfigurations());
        }
        return confs;
    }

    public String[] getConfsToFetch() {
        return confsToFetch.toArray(new String[confsToFetch.size()]);
    }

    public String[] getRealConfs(String conf) {
        if (md == null) {
            return new String[] {conf};
        }
        String defaultConf = getDefaultConf(conf);
        conf = getMainConf(conf);
        if ((md.getConfiguration(conf) == null)
                || Configuration.Visibility.PRIVATE.equals(md.getConfiguration(conf)
                        .getVisibility())) {
            if ("".equals(defaultConf)) {
                return new String[0];
            }
            conf = defaultConf;
        }
        if (conf.startsWith("*")) {
            return resolveSpecialConfigurations(new String[] {conf});
        } else if (conf.indexOf(',') != -1) {
            String[] confs = conf.split(",");
            for (int i = 0; i < confs.length; i++) {
                confs[i] = confs[i].trim();
            }
        }
        return new String[] {conf};

    }

    /**
     * Finds and returns a path in callers from the given module id to the current node
     * 
     * @param from
     *            the module id to start the path from
     * @return a collection representing the path, starting with the from node, followed by the list
     *         of nodes being one path to the current node, excluded
     */
    private Collection<IvyNode> findPath(ModuleId from) {
        return findPath(from, this, new LinkedList<IvyNode>());
    }

    private Collection<IvyNode> findPath(ModuleId from, IvyNode node, List<IvyNode> path) {
        IvyNode parent = node.getDirectCallerFor(from);
        if (parent == null) {
            throw new IllegalArgumentException("no path from " + from + " to " + getId() + " found");
        }
        if (path.contains(parent)) {
            path.add(0, parent);
            Message.verbose("circular dependency found while looking for the path for another one: "
                    + "was looking for " + from + " as a caller of " + path.get(path.size() - 1));
            return path;
        }
        path.add(0, parent);
        if (parent.getId().getModuleId().equals(from)) {
            return path;
        }
        return findPath(from, parent, path);
    }

    /**
     * Update data in this node from data of the given node, for the given root module
     * configuration.
     * 
     * @param node
     *            the source node from which data should be copied
     * @param rootModuleConf
     *            the root module configuration for which data should be updated
     * @param real
     *            true if the node to update from actually corresponds to the same real node
     *            (usually updated because of dynamic revision resolution), false if it's not the
     *            same real node (usually updated because of node eviction)
     */
    private void updateDataFrom(IvyNode node, String rootModuleConf, boolean real) {
        // update callers
        callers.updateFrom(node.callers, rootModuleConf, real);

        if (real) {
            usage.updateDataFrom(node.getAllUsages(), rootModuleConf);
        } else {
            // let's copy usage information for the given rootModuleConf, into a separate usage
            // object to keep detailed data about where usage comes from
            IvyNodeUsage mergedUsage = mergedUsages.get(node.getId());
            if (mergedUsage == null) {
                mergedUsage = new IvyNodeUsage(node);
                mergedUsages.put(node.getId(), mergedUsage);
            }
            mergedUsage.updateDataFrom(node.getAllUsages(), rootModuleConf);
        }

        // update confsToFetch
        updateConfsToFetch(node.fetchedConfigurations);
        updateConfsToFetch(node.confsToFetch);
    }

    private Collection<IvyNodeUsage> getAllUsages() {
        Collection<IvyNodeUsage> usages = new ArrayList<IvyNodeUsage>();
        usages.add(usage);
        usages.addAll(mergedUsages.values());
        return usages;
    }

    /**
     * Returns all the artifacts of this dependency required in all the root module configurations
     * 
     * @return
     */
    public Artifact[] getAllArtifacts() {
        Set<Artifact> ret = new HashSet<Artifact>();
        for (String rootModuleConf : getRootModuleConfigurationsSet()) {
            ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
        }
        return ret.toArray(new Artifact[ret.size()]);
    }

    /**
     * Returns all the artifacts of this dependency required in the root module configurations in
     * which the node is not evicted nor blacklisted
     * 
     * @param artifactFilter
     * @return
     */
    public Artifact[] getSelectedArtifacts(Filter<Artifact> artifactFilter) {
        Collection<Artifact> ret = new HashSet<Artifact>();
        for (String rootModuleConf : getRootModuleConfigurationsSet()) {
            if (!isEvicted(rootModuleConf) && !isBlacklisted(rootModuleConf)) {
                ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
            }
        }
        ret = FilterHelper.filter(ret, artifactFilter);
        return ret.toArray(new Artifact[ret.size()]);
    }

    /**
     * Returns the artifacts of this dependency required in the configurations themselves required
     * in the given root module configuration
     * 
     * @param rootModuleConf
     * @return
     */
    public Artifact[] getArtifacts(String rootModuleConf) {
        // first we look for the dependency configurations required
        // in the given root module configuration
        String[] confs = getConfigurations(rootModuleConf);
        if (confs == null || confs.length == 0) {
            // no configuration required => no artifact required
            return new Artifact[0];
        }
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get artifacts when data has not been loaded. IvyNode = "
                            + this.toString());
        }

        Set<Artifact> artifacts = new HashSet<Artifact>(); // the set we fill before returning

        // we check if we have dependencyArtifacts includes description for this rootModuleConf
        Set<DependencyArtifactDescriptor> dependencyArtifacts = usage
                .getDependencyArtifactsSet(rootModuleConf);

        if (md.isDefault() && dependencyArtifacts != null && !dependencyArtifacts.isEmpty()) {
            addArtifactsFromOwnUsage(artifacts, dependencyArtifacts);
            addArtifactsFromMergedUsage(rootModuleConf, artifacts);
        } else {
            Set<IncludeRule> includes = new LinkedHashSet<IncludeRule>();
            addAllIfNotNull(includes, usage.getDependencyIncludesSet(rootModuleConf));
            for (IvyNodeUsage usage : mergedUsages.values()) {
                addAllIfNotNull(includes, usage.getDependencyIncludesSet(rootModuleConf));
            }

            if ((dependencyArtifacts == null || dependencyArtifacts.isEmpty())
                    && (includes.isEmpty())) {
                // no artifacts / includes: we get all artifacts as defined by the descriptor
                for (int i = 0; i < confs.length; i++) {
                    artifacts.addAll(Arrays.asList(md.getArtifacts(confs[i])));
                }
            } else {
                // we have to get only artifacts listed as "includes"

                // first we get all artifacts as defined by the module descriptor
                // and classify them by artifact id
                Map<ArtifactId, Artifact> allArtifacts = new HashMap<ArtifactId, Artifact>();
                for (int i = 0; i < confs.length; i++) {
                    Artifact[] arts = md.getArtifacts(confs[i]);
                    for (int j = 0; j < arts.length; j++) {
                        allArtifacts.put(arts[j].getId().getArtifactId(), arts[j]);
                    }
                }

                // now we add caller defined ones
                if (dependencyArtifacts != null) {
                    addArtifactsFromOwnUsage(artifacts, dependencyArtifacts);
                }
                addArtifactsFromMergedUsage(rootModuleConf, artifacts);

                // and now we filter according to include rules
                for (Iterator<IncludeRule> it = includes.iterator(); it.hasNext();) {
                    IncludeRule dad = it.next();
                    Collection<Artifact> arts = findArtifactsMatching(dad, allArtifacts);
                    if (arts.isEmpty()) {
                        Message.error("a required artifact is not listed by module descriptor: "
                                + dad.getId());
                        // we remove it from required list to prevent message to be displayed more
                        // than once
                        it.remove();
                    } else {
                        Message.debug(this + " in " + rootModuleConf + ": including " + arts);
                        artifacts.addAll(arts);
                    }
                }
            }
        }

        // now excludes artifacts that aren't accepted by any caller
        for (Iterator<Artifact> iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = iter.next();
            boolean excluded = callers.doesCallersExclude(rootModuleConf, artifact);
            if (excluded) {
                Message.debug(this + " in " + rootModuleConf + ": excluding " + artifact);
                iter.remove();
            }
        }
        return artifacts.toArray(new Artifact[artifacts.size()]);
    }

    private void addArtifactsFromOwnUsage(Set<Artifact> artifacts,
            Set<DependencyArtifactDescriptor> dependencyArtifacts) {
        for (DependencyArtifactDescriptor dad : dependencyArtifacts) {
            artifacts.add(new MDArtifact(md, dad.getName(), dad.getType(), dad.getExt(), dad
                    .getUrl(), dad.getQualifiedExtraAttributes()));
        }
    }

    private void addArtifactsFromMergedUsage(String rootModuleConf, Set<Artifact> artifacts) {
        for (IvyNodeUsage usage : mergedUsages.values()) {
            Set<DependencyArtifactDescriptor> mergedDependencyArtifacts = usage
                    .getDependencyArtifactsSet(rootModuleConf);
            if (mergedDependencyArtifacts != null) {
                for (DependencyArtifactDescriptor dad : mergedDependencyArtifacts) {
                    Map<String, String> extraAttributes = new HashMap<String, String>(
                            dad.getQualifiedExtraAttributes());
                    MDArtifact artifact = new MDArtifact(md, dad.getName(), dad.getType(),
                            dad.getExt(), dad.getUrl(), extraAttributes);

                    if (!artifacts.contains(artifact)) {
                        // this is later used to know that this is a merged artifact
                        extraAttributes.put("ivy:merged", dad.getDependencyDescriptor()
                                .getParentRevisionId() + " -> " + usage.getNode().getId());
                        artifacts.add(artifact);
                    }
                }
            }
        }
    }

    private static Collection<Artifact> findArtifactsMatching(IncludeRule rule,
            Map<ArtifactId, Artifact> allArtifacts) {
        Collection<Artifact> ret = new ArrayList<Artifact>();
        for (Entry<ArtifactId, Artifact> entry : allArtifacts.entrySet()) {
            if (MatcherHelper.matches(rule.getMatcher(), rule.getId(), entry.getKey())) {
                ret.add(entry.getValue());
            }
        }
        return ret;
    }

    public boolean hasProblem() {
        return problem != null;
    }

    public Exception getProblem() {
        return problem;
    }

    public String getProblemMessage() {
        return StringUtils.getErrorMessage(problem);
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public boolean isSearched() {
        return searched;
    }

    public boolean isLoaded() {
        return md != null;
    }

    public boolean isFetched(String conf) {
        return fetchedConfigurations.contains(conf);
    }

    public IvyNode findNode(ModuleRevisionId mrid) {
        return data.getNode(mrid);
    }

    boolean isRoot() {
        return root == this;
    }

    public IvyNode getRoot() {
        return root;
    }

    public ConflictManager getConflictManager(ModuleId mid) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get conflict manager when data has not been loaded. IvyNode = "
                            + this.toString());
        }
        ConflictManager cm = md.getConflictManager(mid);
        return cm == null ? settings.getConflictManager(mid) : cm;
    }

    public IvyNode getRealNode() {
        IvyNode real = data.getNode(getId());
        return real != null ? real : this;
    }

    public ModuleRevisionId getId() {
        return id;
    }

    public ModuleId getModuleId() {
        return id.getModuleId();
    }

    public ModuleDescriptor getDescriptor() {
        return md;
    }

    public ResolveData getData() {
        return data;
    }

    public ResolvedModuleRevision getModuleRevision() {
        return module;
    }

    public long getPublication() {
        if (module != null) {
            return module.getPublicationDate().getTime();
        }
        return 0;
    }

    /**
     * Returns the last modified timestamp of the module represented by this Node, or 0 if the last
     * modified timestamp is currently unkwown (module not loaded)
     * 
     * @return the last modified timestamp of the module represented by this Node
     */
    public long getLastModified() {
        if (md != null) {
            return md.getLastModified();
        }
        return 0;
    }

    public ModuleRevisionId getResolvedId() {
        if (md != null && md.getResolvedModuleRevisionId().getRevision() != null) {
            return md.getResolvedModuleRevisionId();
        } else if (module != null) {
            return module.getId();
        } else {
            return getId();
        }
    }

    /**
     * Clean data related to one root module configuration only
     */
    public void clean() {
        confsToFetch.clear();
    }

    // /////////////////////////////////////////////////////////////////////////////
    // CALLERS MANAGEMENT
    // /////////////////////////////////////////////////////////////////////////////

    boolean canExclude(String rootModuleConf) {
        Caller[] callers = getCallers(rootModuleConf);
        for (int i = 0; i < callers.length; i++) {
            if (callers[i].canExclude()) {
                return true;
            }
        }
        return false;
    }

    private IvyNode getDirectCallerFor(ModuleId from) {
        return callers.getDirectCallerFor(from);
    }

    public Caller[] getCallers(String rootModuleConf) {
        return callers.getCallers(rootModuleConf);
    }

    public Collection<ModuleId> getAllCallersModuleIds() {
        return callers.getAllCallersModuleIds();
    }

    public Caller[] getAllCallers() {
        return callers.getAllCallers();
    }

    public Caller[] getAllRealCallers() {
        return callers.getAllRealCallers();
    }

    public void addCaller(String rootModuleConf, IvyNode callerNode, String callerConf,
            String requestedConf, String[] dependencyConfs, DependencyDescriptor dd) {
        callers.addCaller(rootModuleConf, callerNode, callerConf, requestedConf, dependencyConfs,
            dd);
        boolean isCircular = callers.getAllCallersModuleIds().contains(getId().getModuleId());
        if (isCircular) {
            IvyContext.getContext().getCircularDependencyStrategy()
                    .handleCircularDependency(toMrids(findPath(getId().getModuleId()), this));
        }
    }

    public boolean doesCallersExclude(String rootModuleConf, Artifact artifact,
            Stack<ModuleRevisionId> callersStack) {
        return callers.doesCallersExclude(rootModuleConf, artifact, callersStack);
    }

    private ModuleRevisionId[] toMrids(Collection<IvyNode> path, IvyNode depNode) {
        ModuleRevisionId[] ret = new ModuleRevisionId[path.size() + 1];
        int i = 0;
        for (IvyNode node : path) {
            ret[i++] = node.getId();
        }
        ret[ret.length - 1] = depNode.getId();
        return ret;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // EVICTION MANAGEMENT
    // /////////////////////////////////////////////////////////////////////////////

    /** A copy of the set of resolved nodes (real nodes) */
    public Set<IvyNode> getResolvedNodes(ModuleId moduleId, String rootModuleConf) {
        return eviction.getResolvedNodes(moduleId, rootModuleConf);
    }

    public Collection<ModuleRevisionId> getResolvedRevisions(ModuleId moduleId,
            String rootModuleConf) {
        return eviction.getResolvedRevisions(moduleId, rootModuleConf);
    }

    public void markEvicted(EvictionData evictionData) {
        eviction.markEvicted(evictionData);
        String rootModuleConf = evictionData.getRootModuleConf();

        // bug 105: update selected data with evicted one
        if (evictionData.getSelected() != null) {
            for (IvyNode selected : evictionData.getSelected()) {
                selected.updateDataFrom(this, rootModuleConf, false);
            }
        }
    }

    public Collection<ConflictManager> getAllEvictingConflictManagers() {
        return eviction.getAllEvictingConflictManagers();
    }

    public Collection<IvyNode> getAllEvictingNodes() {
        return eviction.getAllEvictingNodes();
    }

    public Collection<String> getAllEvictingNodesDetails() {
        return eviction.getAllEvictingNodesDetails();
    }

    public String[] getEvictedConfs() {
        return eviction.getEvictedConfs();
    }

    public EvictionData getEvictedData(String rootModuleConf) {
        return eviction.getEvictedData(rootModuleConf);
    }

    public Collection<IvyNode> getEvictedNodes(ModuleId mid, String rootModuleConf) {
        return eviction.getEvictedNodes(mid, rootModuleConf);
    }

    public Collection<ModuleRevisionId> getEvictedRevisions(ModuleId mid, String rootModuleConf) {
        return eviction.getEvictedRevisions(mid, rootModuleConf);
    }

    public EvictionData getEvictionDataInRoot(String rootModuleConf, IvyNode ancestor) {
        return eviction.getEvictionDataInRoot(rootModuleConf, ancestor);
    }

    public boolean isCompletelyEvicted() {
        return eviction.isCompletelyEvicted();
    }

    public boolean isEvicted(String rootModuleConf) {
        return eviction.isEvicted(rootModuleConf);
    }

    public void markEvicted(String rootModuleConf, IvyNode node, ConflictManager conflictManager,
            Collection<IvyNode> resolved) {
        EvictionData evictionData = new EvictionData(rootModuleConf, node, conflictManager,
                resolved);
        markEvicted(evictionData);
    }

    public void setEvictedNodes(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> evicted) {
        eviction.setEvictedNodes(moduleId, rootModuleConf, evicted);
    }

    public void setResolvedNodes(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> resolved) {
        eviction.setResolvedNodes(moduleId, rootModuleConf, resolved);
    }

    @Override
    public String toString() {
        return getResolvedId().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IvyNode)) {
            return false;
        }
        IvyNode node = (IvyNode) obj;
        return node.getId().equals(getId());
    }

    public int compareTo(IvyNode that) {
        return this.getModuleId().compareTo(that.getModuleId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Returns a collection of Nodes in conflict for which conflict has been detected but conflict
     * resolution hasn't been done yet
     * 
     * @param rootModuleConf
     * @param mid
     *            the module id for which pending conflicts should be found
     * @return a Collection of IvyNode in pending conflict
     */
    public Collection<IvyNode> getPendingConflicts(String rootModuleConf, ModuleId mid) {
        return eviction.getPendingConflicts(rootModuleConf, mid);
    }

    public void setPendingConflicts(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> conflicts) {
        eviction.setPendingConflicts(moduleId, rootModuleConf, conflicts);
    }

    // /////////////////////////////////////////////////////////////////////////////
    // BLACKLISTING MANAGEMENT
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * Blacklists the current node, so that a new resolve process won't ever consider this node as
     * available in the repository.
     * <p>
     * This is useful in combination with {@link RestartResolveProcess} for conflict manager
     * implementation which use a best effort strategy to find compatible dependency set, like
     * {@link LatestCompatibleConflictManager}
     * </p>
     * 
     * @param rootModuleConf
     *            the root module configuration in which the node should be blacklisted
     */
    public void blacklist(IvyNodeBlacklist bdata) {
        if (data.getSettings().logResolvedRevision()) {
            Message.info("BLACKLISTING " + bdata);
        } else {
            Message.verbose("BLACKLISTING " + bdata);
        }

        Stack<IvyNode> callerStack = new Stack<IvyNode>();
        callerStack.push(this);
        clearEvictionDataInAllCallers(bdata.getRootModuleConf(), callerStack);

        usage.blacklist(bdata);
        data.blacklist(this);
    }

    private void clearEvictionDataInAllCallers(String rootModuleConf, Stack<IvyNode> callerStack) {
        IvyNode node = callerStack.peek();
        Caller[] callers = node.getCallers(rootModuleConf);
        for (int i = 0; i < callers.length; i++) {
            IvyNode callerNode = findNode(callers[i].getModuleRevisionId());
            if (callerNode != null) {
                callerNode.eviction = new IvyNodeEviction(callerNode);
                if (!callerStack.contains(callerNode)) {
                    callerStack.push(callerNode);
                    clearEvictionDataInAllCallers(rootModuleConf, callerStack);
                    callerStack.pop();
                }
            }
        }
    }

    /**
     * Indicates if this node has been blacklisted in the given root module conf.
     * <p>
     * A blacklisted node should be considered as if it doesn't even exist on the repository.
     * </p>
     * 
     * @param rootModuleConf
     *            the root module conf for which we'd like to know if the node is blacklisted
     * 
     * @return true if this node is blacklisted int he given root module conf, false otherwise
     * @see #blacklist(String)
     */
    public boolean isBlacklisted(String rootModuleConf) {
        return usage.isBlacklisted(rootModuleConf);
    }

    /**
     * Indicates if this node has been blacklisted in all root module configurations.
     * 
     * @return true if this node is blacklisted in all root module configurations, false otherwise
     * @see #blacklist(String)
     */
    public boolean isCompletelyBlacklisted() {
        if (isRoot()) {
            return false;
        }
        String[] rootModuleConfigurations = getRootModuleConfigurations();
        for (int i = 0; i < rootModuleConfigurations.length; i++) {
            if (!isBlacklisted(rootModuleConfigurations[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the blacklist data of this node in the given root module conf, or <code>null</code>
     * if this node is not blacklisted in this root module conf.
     * 
     * @param rootModuleConf
     *            the root module configuration to consider
     * @return the blacklist data if any
     */
    public IvyNodeBlacklist getBlacklistData(String rootModuleConf) {
        return usage.getBlacklistData(rootModuleConf);
    }

    public IvyNodeUsage getMainUsage() {
        return usage;
    }

    /**
     * Indicates if there is any of the merged usages of this node which has a depender with
     * transitive dependency descriptor.
     * <p>
     * If at there is at least one usage from the merged usages for which there is a depender in the
     * given root module conf which has a dependency descriptor with transitive == true, then it
     * returns true. Otherwise it returns false.
     * </p>
     * 
     * @param rootModuleConf
     *            the root module configuration to consider
     * @return true if there is any merged usage with transitive dd, false otherwise.
     */
    public boolean hasAnyMergedUsageWithTransitiveDependency(String rootModuleConf) {
        if (mergedUsages == null) {
            return false;
        }
        for (IvyNodeUsage usage : mergedUsages.values()) {
            if (usage.hasTransitiveDepender(rootModuleConf)) {
                return true;
            }
        }
        return false;
    }

}
