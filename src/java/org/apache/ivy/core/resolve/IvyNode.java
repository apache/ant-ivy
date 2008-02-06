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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

public class IvyNode implements Comparable {
    private static final Pattern FALLBACK_CONF_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    private static final class NodeConf {
        private IvyNode node;

        private String conf;

        public NodeConf(IvyNode node, String conf) {
            if (node == null) {
                throw new NullPointerException("node must not null");
            }
            if (conf == null) {
                throw new NullPointerException("conf must not null");
            }
            this.node = node;
            this.conf = conf;
        }

        public final String getConf() {
            return conf;
        }

        public final IvyNode getNode() {
            return node;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof NodeConf)) {
                return false;
            }
            return getNode().equals(((NodeConf) obj).getNode())
                    && getConf().equals(((NodeConf) obj).getConf());
        }

        public int hashCode() {
            //CheckStyle:MagicNumber| OFF
            int hash = 33;
            hash += getNode().hashCode() * 17;
            hash += getConf().hashCode() * 17;
            //CheckStyle:MagicNumber| OFF
            return hash;
        }
        
        public String toString() {
            return "NodeConf(" + conf + ")";
        }
    }
    
    

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
    // Map(IvyNode parent -> DependencyDescriptor)
    private Map dds = new HashMap();

    // Set when data has been loaded only, or when constructed from a module descriptor
    private ModuleDescriptor md;

    private ResolvedModuleRevision module;

    // //////// LOADING METADATA
    private Exception problem = null;

    private boolean downloaded = false;

    private boolean searched = false;
    
    private Collection confsToFetch = new HashSet();

    private Collection fetchedConfigurations = new HashSet();

    private Collection loadedRootModuleConfs = new HashSet();

    // //////// USAGE DATA

    // Map (String rootConfName -> Set(String confName))
    // used to know which configurations of the dependency are required
    // for each root module configuration
    private Map rootModuleConfs = new HashMap();

    // Map (NodeConf in -> Set(String conf))
    private Map requiredConfs = new HashMap();

    // Map (String rootModuleConf -> Set(DependencyArtifactDescriptor))
    private Map dependencyArtifacts = new HashMap();

    // Map (String rootModuleConf -> Set(IncludeRule))
    private Map dependencyIncludes = new HashMap();
    
    // Map (String rootModuleConf -> IvyNodeBlacklist)
    private Map blacklisted = new HashMap();

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
            boolean shouldBePublic) {
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
                    data.getEventManager().fireIvyEvent(
                        new StartResolveDependencyEvent(resolver, dependencyDescriptor));
                    module = resolver.getDependency(dependencyDescriptor, data);
                    data.getEventManager().fireIvyEvent(
                        new EndResolveDependencyEvent(resolver, dependencyDescriptor, module,
                            System.currentTimeMillis() - start));
                    if (module != null) {
                        module.getResolver().getRepositoryCacheManager().saveResolvers(
                            module.getDescriptor(),
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

                        if (settings.getVersionMatcher().isDynamic(getId())) {
                            // IVY-56: check if revision has actually been resolved
                            if (settings.getVersionMatcher().isDynamic(module.getId())) {
                                Message
                                        .error("impossible to resolve dynamic revision for "
                                                + getId()
                                                + ": check your configuration and make sure revision is part of your pattern");
                                problem = new RuntimeException(
                                        "impossible to resolve dynamic revision");
                                return false;
                            }
                            IvyNode resolved = data.getNode(module.getId());
                            if (resolved != null) {
                                // exact revision has already been resolved
                                // => update it and discard this node
                                md = module.getDescriptor(); // needed for handleConfiguration
                                if (!handleConfiguration(loaded, rootModuleConf, parent,
                                    parentConf, conf, shouldBePublic)) {
                                    return false;
                                }

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
                                resolved.loadData(rootModuleConf, parent, parentConf, conf,
                                    shouldBePublic);
                                DependencyDescriptor dd = dependencyDescriptor;
                                if (dd != null) {
                                    resolved.addDependencyArtifacts(rootModuleConf, dd
                                            .getDependencyArtifacts(parentConf));
                                    resolved.addDependencyIncludes(rootModuleConf, dd
                                            .getIncludeRules(parentConf));
                                }
                                
                                data.replaceNode(getId(), resolved, rootModuleConf); 
                                // this actually discards the node

                                if (settings.logResolvedRevision()) {
                                    Message.info("\t[" + module.getId().getRevision() + "] "
                                            + getId());
                                } else {
                                    Message.verbose("\t[" + module.getId().getRevision() + "] "
                                            + getId());
                                }

                                return true;
                            }
                        }
                        downloaded = module.getReport().isDownloaded();
                        searched = module.getReport().isSearched();
                    } else {
                        Message.warn("\tmodule not found: " + getId());
                        resolver.reportFailure();
                        problem = new RuntimeException("not found");
                    }
                } catch (ResolveProcessException e) {
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                    problem = e;
                }

                // still not resolved, report error
                if (module == null) {
                    return false;
                } else {
                    loaded = true;
                    if (settings.getVersionMatcher().isDynamic(getId())) {
                        if (settings.logResolvedRevision()) {
                            Message.info("\t[" + module.getId().getRevision() + "] " + getId());
                        } else {
                            Message.verbose("\t[" + module.getId().getRevision() + "] " + getId());
                        }
                    }
                    md = module.getDescriptor();
                    confsToFetch.remove("*");
                    updateConfsToFetch(Arrays.asList(resolveSpecialConfigurations(
                        getRequiredConfigurations(parent, parentConf), this)));
                }
            } else {
                loaded = true;
            }
        }
        handleConfiguration(loaded, rootModuleConf, parent, parentConf, conf, shouldBePublic);
        if (hasProblem()) {
            Message.debug("problem : " + problem.getMessage());
            return false;
        } else {
            DependencyDescriptor dd = getDependencyDescriptor(parent);
            if (dd != null) {
                addDependencyArtifacts(rootModuleConf, dd.getDependencyArtifacts(parentConf));
                addDependencyIncludes(rootModuleConf, dd.getIncludeRules(parentConf));
            }
            return loaded;
        }
    }

    public Collection getDependencies(String rootModuleConf, String[] confs) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get dependencies when data has not been loaded");
        }
        if (Arrays.asList(confs).contains("*")) {
            confs = md.getConfigurationsNames();
        }
        Collection deps = new HashSet();
        for (int i = 0; i < confs.length; i++) {
            deps.addAll(getDependencies(rootModuleConf, confs[i], confs[i]));
        }
        return deps;
    }

    public Collection getDependencies(String rootModuleConf, String conf, String requestedConf) {
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get dependencies when data has not been loaded");
        }
        DependencyDescriptor[] dds = md.getDependencies();
        Collection dependencies = new LinkedHashSet(); // it's important to respect order
        for (int i = 0; i < dds.length; i++) {
            DependencyDescriptor dd = dds[i];
            String[] dependencyConfigurations = dd.getDependencyConfigurations(conf, requestedConf);
            if (dependencyConfigurations.length == 0) {
                // no configuration of the dependency is required for current confs :
                // it is exactly the same as if there was no dependency at all on it
                continue;
            }
            if (isDependencyModuleExcluded(rootModuleConf, dd.getDependencyRevisionId(), conf)) {
                // the whole module is excluded, it is considered as not being part of dependencies
                // at all
                Message.verbose("excluding " + dd.getDependencyRevisionId() + " in " + conf);
                continue;
            }
            IvyNode depNode = data.getNode(dd.getDependencyRevisionId());
            if (depNode == null) {
                depNode = new IvyNode(data, this, dd);
            } else {
                depNode.addDependencyDescriptor(this, dd);
                if (depNode.hasProblem()) {
                    // dependency already tried to be resolved, but unsuccessfully
                    // nothing special to do
                }

            }
            Collection confs = Arrays.asList(resolveSpecialConfigurations(dependencyConfigurations,
                depNode));
            depNode.updateConfsToFetch(confs);
            depNode.setRequiredConfs(this, conf, confs);

            depNode.addCaller(rootModuleConf, this, conf, dependencyConfigurations, dd);
            dependencies.add(depNode);
        }
        return dependencies;
    }

    private void addDependencyDescriptor(IvyNode parent, DependencyDescriptor dd) {
        dds.put(parent, dd);
    }

    public DependencyDescriptor getDependencyDescriptor(IvyNode parent) {
        return (DependencyDescriptor) dds.get(parent);
    }

    private boolean isDependencyModuleExcluded(String rootModuleConf,
            ModuleRevisionId dependencyRevisionId, String conf) {
        return callers.doesCallersExclude(rootModuleConf, DefaultArtifact.newIvyArtifact(
            dependencyRevisionId, null));
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
            String parentConf, String conf, boolean shouldBePublic) {
        if (md != null) {
            String[] confs = getRealConfs(conf);
            for (int i = 0; i < confs.length; i++) {
                Configuration c = md.getConfiguration(confs[i]);
                if (c == null) {
                    confsToFetch.remove(conf);
                    if (!conf.equals(confs[i])) {
                        problem = new RuntimeException("configuration(s) not found in " + this
                                + ": " + conf + ". Missing configuration: " + confs[i]
                                + ". It was required from " + parent + " " + parentConf);
                    } else {
                        problem = new RuntimeException("configuration(s) not found in " + this
                                + ": " + confs[i] + ". It was required from " + parent + " "
                                + parentConf);
                    }
                    return false;
                } else if (shouldBePublic && !isRoot()
                        && c.getVisibility() != Configuration.Visibility.PUBLIC) {
                    confsToFetch.remove(conf);
                    problem = new RuntimeException("configuration not public in " + this + ": " + c
                            + ". It was required from " + parent + " " + parentConf);
                    return false;
                }
                if (loaded) {
                    fetchedConfigurations.add(conf);
                    confsToFetch.removeAll(Arrays.asList(confs));
                    confsToFetch.remove(conf);
                }
                addRootModuleConfigurations(rootModuleConf, confs);
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

    public void updateConfsToFetch(Collection confs) {
        confsToFetch.addAll(confs);
        confsToFetch.removeAll(fetchedConfigurations);
    }

    /**
     * resolve the '*' special configurations if necessary and possible
     */
    private String[] resolveSpecialConfigurations(String[] dependencyConfigurations, IvyNode node) {
        if (dependencyConfigurations.length == 1 && dependencyConfigurations[0].startsWith("*")
                && node != null && node.isLoaded()) {
            String conf = dependencyConfigurations[0];
            if ("*".equals(conf)) {
                return node.getDescriptor().getPublicConfigurationsNames();
            }
            // there are exclusions in the configuration
            List exclusions = Arrays.asList(conf.substring(2).split("\\!"));

            List ret = new ArrayList(Arrays.asList(node.getDescriptor()
                    .getPublicConfigurationsNames()));
            ret.removeAll(exclusions);

            return (String[]) ret.toArray(new String[ret.size()]);
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
        Collection req = (Collection) requiredConfs.get(new NodeConf(in, inConf));
        return req == null ? new String[0] : (String[]) req.toArray(new String[req.size()]);
    }

    /**
     * returns all the current required configurations of the node
     * 
     * @return
     */
    public String[] getRequiredConfigurations() {
        Collection required = new ArrayList(confsToFetch.size() + fetchedConfigurations.size());
        required.addAll(fetchedConfigurations);
        required.addAll(confsToFetch);
        return (String[]) required.toArray(new String[required.size()]);
    }

    private void setRequiredConfs(IvyNode parent, String parentConf, Collection confs) {
        requiredConfs.put(new NodeConf(parent, parentConf), new HashSet(confs));
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
        Set depConfs = (Set) rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            return new String[0];
        }
        return (String[]) depConfs.toArray(new String[depConfs.size()]);
    }

    //This is never called.  Could we remove it?
    public void discardConf(String rootModuleConf, String conf) {
        Set depConfs = (Set) rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            depConfs = new HashSet();
            rootModuleConfs.put(rootModuleConf, depConfs);
        }
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

    private void addRootModuleConfigurations(String rootModuleConf, String[] dependencyConfs) {
        Set depConfs = (Set) rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            depConfs = new HashSet();
            rootModuleConfs.put(rootModuleConf, depConfs);
        }
        if (md != null) {
            // add all given dependency configurations to the set + extended ones
            for (int i = 0; i < dependencyConfs.length; i++) {
                Configuration conf = md.getConfiguration(dependencyConfs[i]);
                if (conf != null) {
                    String[] exts = conf.getExtends();
                    addRootModuleConfigurations(rootModuleConf, exts); // recursive add of extended
                    // configurations
                    depConfs.add(conf.getName());
                } else {
                    Message.warn("unknown configuration in " + getId() + ": " + dependencyConfs[i]);
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
        return (String[]) rootModuleConfs.keySet().toArray(new String[rootModuleConfs.size()]);
    }

    public String[] getConfsToFetch() {
        return (String[]) confsToFetch.toArray(new String[confsToFetch.size()]);
    }

    public String[] getRealConfs(String conf) {
        if (md == null) {
            return new String[] {conf};
        }
        String defaultConf = getDefaultConf(conf);
        conf = getMainConf(conf);
        if (md.getConfiguration(conf) == null) {
            if ("".equals(defaultConf)) {
                return new String[0];
            }
            conf = defaultConf;
        }
        if (conf.startsWith("*")) {
            return resolveSpecialConfigurations(new String[] {conf}, this);
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
    private Collection findPath(ModuleId from) {
        return findPath(from, this, new LinkedList());
    }

    private Collection findPath(ModuleId from, IvyNode node, List path) {
        IvyNode parent = (IvyNode) node.getDirectCallerFor(from);
        if (parent == null) {
            throw new IllegalArgumentException("no path from " + from + " to " + getId() + " found");
        }
        if (path.contains(parent)) {
            path.add(0, parent);
            Message
                    .verbose("circular dependency found while looking for the path for another one: was looking for "
                            + from + " as a caller of " + path.get(path.size() - 1));
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

        // update requiredConfs
        updateMapOfSet(node.requiredConfs, requiredConfs);

        // update rootModuleConfs
        updateMapOfSetForKey(node.rootModuleConfs, rootModuleConfs, rootModuleConf);

        // update dependencyArtifactsIncludes
        updateMapOfSetForKey(node.dependencyArtifacts, dependencyArtifacts, rootModuleConf);

        // update confsToFetch
        updateConfsToFetch(node.fetchedConfigurations);
        updateConfsToFetch(node.confsToFetch);
    }

    private void updateMapOfSet(Map from, Map to) {
        for (Iterator iter = from.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            updateMapOfSetForKey(from, to, key);
        }
    }

    private void updateMapOfSetForKey(Map from, Map to, Object key) {
        Set set = (Set) from.get(key);
        if (set != null) {
            Set toupdate = (Set) to.get(key);
            if (toupdate != null) {
                toupdate.addAll(set);
            } else {
                to.put(key, new HashSet(set));
            }
        }
    }

    /**
     * Returns all the artifacts of this dependency required in all the root module configurations
     * 
     * @return
     */
    public Artifact[] getAllArtifacts() {
        Set ret = new HashSet();
        for (Iterator it = rootModuleConfs.keySet().iterator(); it.hasNext();) {
            String rootModuleConf = (String) it.next();
            ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
        }
        return (Artifact[]) ret.toArray(new Artifact[ret.size()]);
    }

    /**
     * Returns all the artifacts of this dependency required in the root module configurations in
     * which the node is not evicted nor blacklisted
     * 
     * @param artifactFilter
     * @return
     */
    public Artifact[] getSelectedArtifacts(Filter artifactFilter) {
        Collection ret = new HashSet();
        for (Iterator it = rootModuleConfs.keySet().iterator(); it.hasNext();) {
            String rootModuleConf = (String) it.next();
            if (!isEvicted(rootModuleConf) && !isBlacklisted(rootModuleConf)) {
                ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
            }
        }
        ret = FilterHelper.filter(ret, artifactFilter);
        return (Artifact[]) ret.toArray(new Artifact[ret.size()]);
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
        Set confs = (Set) rootModuleConfs.get(rootModuleConf);
        if (confs == null) {
            // no configuration required => no artifact required
            return new Artifact[0];
        }
        if (md == null) {
            throw new IllegalStateException(
                    "impossible to get artefacts when data has not been loaded. IvyNode = "
                    + this.toString());
        }

        
        Set artifacts = new HashSet(); // the set we fill before returning

        // we check if we have dependencyArtifacts includes description for this rootModuleConf
        Set dependencyArtifacts = (Set) this.dependencyArtifacts.get(rootModuleConf);

        if (md.isDefault() && dependencyArtifacts != null && !dependencyArtifacts.isEmpty()) {
            // the descriptor is a default one: it has been generated from nothing
            // moreover, we have dependency artifacts description
            // these descritions are thus used as if they were declared in the module
            // descriptor. If one is not really present, the error will be raised
            // at download time
            for (Iterator it = dependencyArtifacts.iterator(); it.hasNext();) {
                DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor) it.next();
                artifacts.add(new MDArtifact(md, dad.getName(), dad.getType(), dad.getExt(), dad
                        .getUrl(), dad.getExtraAttributes()));
            }
        } else {
            Set includes = (Set) dependencyIncludes.get(rootModuleConf);

            if ((dependencyArtifacts == null || dependencyArtifacts.isEmpty())
                    && (includes == null || includes.isEmpty())) {
                // no artifacts / includes: we get all artifacts as defined by the descriptor
                for (Iterator iter = confs.iterator(); iter.hasNext();) {
                    String conf = (String) iter.next();
                    artifacts.addAll(Arrays.asList(md.getArtifacts(conf)));
                }
            } else {
                // we have to get only artifacts listed as "includes"

                // first we get all artifacts as defined by the module descriptor
                // and classify them by artifact id
                Map allArtifacts = new HashMap();
                for (Iterator iter = confs.iterator(); iter.hasNext();) {
                    String conf = (String) iter.next();
                    Artifact[] arts = md.getArtifacts(conf);
                    for (int i = 0; i < arts.length; i++) {
                        allArtifacts.put(arts[i].getId().getArtifactId(), arts[i]);
                    }
                }

                // now we add caller defined ones
                for (Iterator it = dependencyArtifacts.iterator(); it.hasNext();) {
                    DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor) it.next();
                    artifacts.add(new MDArtifact(md, dad.getName(), dad.getType(), dad.getExt(),
                            dad.getUrl(), dad.getExtraAttributes()));
                }

                // and now we filter according to include rules
                for (Iterator it = includes.iterator(); it.hasNext();) {
                    IncludeRule dad = (IncludeRule) it.next();
                    Collection arts = findArtifactsMatching(dad, allArtifacts);
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
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            boolean excluded = callers.doesCallersExclude(rootModuleConf, artifact);
            if (excluded) {
                Message.debug(this + " in " + rootModuleConf + ": excluding " + artifact);
                iter.remove();
            }
        }
        return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
    }

    private static Collection findArtifactsMatching(IncludeRule rule, Map allArtifacts) {
        Collection ret = new ArrayList();
        for (Iterator iter = allArtifacts.keySet().iterator(); iter.hasNext();) {
            ArtifactId aid = (ArtifactId) iter.next();
            if (MatcherHelper.matches(rule.getMatcher(), rule.getId(), aid)) {
                ret.add(allArtifacts.get(aid));
            }
        }
        return ret;
    }

    private void addDependencyArtifacts(String rootModuleConf,
            DependencyArtifactDescriptor[] dependencyArtifacts) {
        addObjectsForConf(rootModuleConf, Arrays.asList(dependencyArtifacts),
            this.dependencyArtifacts);
    }

    private void addDependencyIncludes(String rootModuleConf, IncludeRule[] rules) {
        addObjectsForConf(rootModuleConf, Arrays.asList(rules), dependencyIncludes);
    }

    private void addObjectsForConf(String rootModuleConf, Collection objectsToAdd, Map map) {
        Set set = (Set) map.get(rootModuleConf);
        if (set == null) {
            set = new HashSet();
            map.put(rootModuleConf, set);
        }
        set.addAll(objectsToAdd);
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

    public Collection getAllCallersModuleIds() {
        return callers.getAllCallersModuleIds();
    }

    public Caller[] getAllCallers() {
        return callers.getAllCallers();
    }

    public Caller[] getAllRealCallers() {
        return callers.getAllRealCallers();
    }

    public void addCaller(String rootModuleConf, IvyNode callerNode, String callerConf,
            String[] dependencyConfs, DependencyDescriptor dd) {
        callers.addCaller(rootModuleConf, callerNode, callerConf, dependencyConfs, dd);
        boolean isCircular = callers.getAllCallersModuleIds().contains(getId().getModuleId());
        if (isCircular) {
            IvyContext.getContext().getCircularDependencyStrategy().handleCircularDependency(
                toMrids(findPath(getId().getModuleId()), this));
        }
    }

    public boolean doesCallersExclude(String rootModuleConf, Artifact artifact, Stack callersStack) {
        return callers.doesCallersExclude(rootModuleConf, artifact, callersStack);
    }

    private ModuleRevisionId[] toMrids(Collection path, IvyNode depNode) {
        ModuleRevisionId[] ret = new ModuleRevisionId[path.size() + 1];
        int i = 0;
        for (Iterator iter = path.iterator(); iter.hasNext(); i++) {
            IvyNode node = (IvyNode) iter.next();
            ret[i] = node.getId();
        }
        ret[ret.length - 1] = depNode.getId();
        return ret;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // EVICTION MANAGEMENT
    // /////////////////////////////////////////////////////////////////////////////

    public Collection getResolvedNodes(ModuleId moduleId, String rootModuleConf) {
        return eviction.getResolvedNodes(moduleId, rootModuleConf);
    }

    public Collection getResolvedRevisions(ModuleId moduleId, String rootModuleConf) {
        return eviction.getResolvedRevisions(moduleId, rootModuleConf);
    }

    public void markEvicted(EvictionData evictionData) {
        eviction.markEvicted(evictionData);
        if (!rootModuleConfs.keySet().contains(evictionData.getRootModuleConf())) {
            rootModuleConfs.put(evictionData.getRootModuleConf(), null);
        }

        // bug 105: update selected data with evicted one
        if (evictionData.getSelected() != null) {
            for (Iterator iter = evictionData.getSelected().iterator(); iter.hasNext();) {
                IvyNode selected = (IvyNode) iter.next();
                selected.updateDataFrom(this, evictionData.getRootModuleConf(), false);
            }
        }
    }

    public Collection getAllEvictingConflictManagers() {
        return eviction.getAllEvictingConflictManagers();
    }

    public Collection getAllEvictingNodes() {
        return eviction.getAllEvictingNodes();
    }

    public Collection/*<String>*/ getAllEvictingNodesDetails() {
        return eviction.getAllEvictingNodesDetails();
    }

    public String[] getEvictedConfs() {
        return eviction.getEvictedConfs();
    }

    public EvictionData getEvictedData(String rootModuleConf) {
        return eviction.getEvictedData(rootModuleConf);
    }

    public Collection getEvictedNodes(ModuleId mid, String rootModuleConf) {
        return eviction.getEvictedNodes(mid, rootModuleConf);
    }

    public Collection getEvictedRevisions(ModuleId mid, String rootModuleConf) {
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
            Collection resolved) {
        EvictionData evictionData = new EvictionData(rootModuleConf, node, conflictManager,
                resolved);
        markEvicted(evictionData);
    }

    public void setEvictedNodes(ModuleId moduleId, String rootModuleConf, Collection evicted) {
        eviction.setEvictedNodes(moduleId, rootModuleConf, evicted);
    }

    public void setResolvedNodes(ModuleId moduleId, String rootModuleConf, Collection resolved) {
        eviction.setResolvedNodes(moduleId, rootModuleConf, resolved);
    }

    public String toString() {
        return getResolvedId().toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IvyNode)) {
            return false;
        }
        IvyNode node = (IvyNode) obj;
        return node.getId().equals(getId());
    }

    public int compareTo(Object obj) {
        IvyNode that = (IvyNode) obj;
        return this.getModuleId().compareTo(that.getModuleId());
    }

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
    public Collection getPendingConflicts(String rootModuleConf, ModuleId mid) {
        return eviction.getPendingConflicts(rootModuleConf, mid);
    }

    public void setPendingConflicts(ModuleId moduleId, String rootModuleConf, Collection conflicts) {
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
     * @param rootModuleConf the root module configuration in which the node should be blacklisted
     */
    public void blacklist(IvyNodeBlacklist bdata) {
        if (data.getSettings().logResolvedRevision()) {
            Message.info("BLACKLISTING " + bdata);
        } else {
            Message.verbose("BLACKLISTING " + bdata);
        }
        
        Stack callerStack = new Stack();
        callerStack.push(this);
        clearEvictionDataInAllCallers(bdata.getRootModuleConf(), callerStack);
        
        blacklisted.put(bdata.getRootModuleConf(), bdata);
        data.blacklist(this);
    }


    private void clearEvictionDataInAllCallers(String rootModuleConf, Stack/*<IvyNode>*/ callerStack) {
        IvyNode node = (IvyNode) callerStack.peek();
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
        return blacklisted.containsKey(rootModuleConf);
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
        return (IvyNodeBlacklist) blacklisted.get(rootModuleConf);
    }

}
