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
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;


public class IvyNode implements Comparable {
    private static final Pattern FALLBACK_CONF_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    private static final class NodeConf {
        private IvyNode _node;
        private String _conf;

        public NodeConf(IvyNode node, String conf) {
        	if (node == null) {
        		throw new NullPointerException("node must not null");
        	}
        	if (conf == null) {
        		throw new NullPointerException("conf must not null");
        	}
            _node = node;
            _conf = conf;
        }

        public final String getConf() {
            return _conf;
        }
        
        public final IvyNode getNode() {
            return _node;
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeConf)) {
                return false;
            }
            return getNode().equals(((NodeConf)obj).getNode()) 
                && getConf().equals(((NodeConf)obj).getConf());
        }
        public int hashCode() {
            int hash = 33;
            hash += getNode().hashCode() * 17;
            hash += getConf().hashCode() * 17;
            return hash;
        }
    }

    ////////// CONTEXT
    private ResolveData _data;
    private IvySettings _settings;
    
    
    ////////// DELEGATES
    private IvyNodeCallers _callers;
    private IvyNodeEviction _eviction;
    
    

    ////////// MAIN DATA
    
    private IvyNode _root;
    
    // id as requested, i.e. may be with latest rev
    private ModuleRevisionId _id; 

    // set only when node has been built or updated from a DependencyDescriptor
    // Map(IvyNode parent -> DependencyDescriptor)
    private Map _dds = new HashMap(); 
    
    
    // Set when data has been loaded only, or when constructed from a module descriptor
    private ModuleDescriptor _md;
    private ResolvedModuleRevision _module;
    
    
    
    ////////// LOADING METADATA
    private Exception _problem = null;
    
    private boolean _downloaded = false;
    private boolean _searched = false;
    private boolean _isCircular = false;

    private Collection _confsToFetch = new HashSet();
    private Collection _fetchedConfigurations = new HashSet();
    private Collection _loadedRootModuleConfs = new HashSet();

    
    ////////// USAGE DATA
    
    // Map (String rootConfName -> Set(String confName))
    // used to know which configurations of the dependency are required 
    // for each root module configuration
    private Map _rootModuleConfs = new HashMap();
    
    // Map (NodeConf in -> Set(String conf))
    private Map _requiredConfs = new HashMap(); 
    
    // Map (String rootModuleConf -> Set(DependencyArtifactDescriptor))
    private Map _dependencyArtifacts = new HashMap();

    // Map (String rootModuleConf -> Set(IncludeRule))
    private Map _dependencyIncludes = new HashMap();


    
    public IvyNode(ResolveData data, IvyNode parent, DependencyDescriptor dd) {
        _id = dd.getDependencyRevisionId();
        _dds.put(parent, dd);
        _root = parent.getRoot();
        init(data);
    }

    public IvyNode(ResolveData data, ModuleDescriptor md) {
        _id = md.getModuleRevisionId();
        _md = md;
        _root = this;
        init(data);
    }

	private void init(ResolveData data) {
		_data = data;
        _settings = data.getSettings();
        _eviction = new IvyNodeEviction(this);
        _callers = new IvyNodeCallers(this);
	}


    /**
     * After the call node may be discarded. To avoid using discarded node, make sure
     * to get the real node after the call
     * IvyNode node = ...
     * node.loadData();
     * node = node.getRealNode();
     * ...
     */
    public boolean loadData(String rootModuleConf, IvyNode parent, String parentConf, String conf, boolean shouldBePublic) {
        boolean loaded = false;
		if (!isEvicted(rootModuleConf) && (hasConfigurationsToLoad() || !isRootModuleConfLoaded(rootModuleConf)) && !hasProblem()) {
            markRootModuleConfLoaded(rootModuleConf);
            if (_md == null) {
                DependencyResolver resolver = _data.getSettings().getResolver(getModuleId());
                if (resolver == null) {
                    Message.error("no resolver found for "+getModuleId()+": check your configuration");
                    _problem = new RuntimeException("no resolver found for "+getModuleId()+": check your configuration");
                    _data.getReport().addDependency(this);
                    return false;
                }
                try {
                    Message.debug("\tusing "+resolver+" to resolve "+getId());
                    DependencyDescriptor dependencyDescriptor = getDependencyDescriptor(parent);
                    _data.getEventManager().fireIvyEvent(new StartResolveDependencyEvent(resolver, dependencyDescriptor));
                    _module = resolver.getDependency(dependencyDescriptor, _data);
                    _data.getEventManager().fireIvyEvent(new EndResolveDependencyEvent(resolver, dependencyDescriptor, _module));
                    if (_module != null) {
                        _data.getCacheManager().saveResolver(_module.getDescriptor(), _module.getResolver().getName());
                        _data.getCacheManager().saveArtResolver(_module.getDescriptor(), _module.getArtifactResolver().getName());
                        if (_settings.logModuleWhenFound()) {
                            Message.info("\tfound "+_module.getId()+" in "+_module.getResolver().getName());
                        } else {
                            Message.verbose("\tfound "+_module.getId()+" in "+_module.getResolver().getName());
                        }
                        
                        if (_data.getSettings().getVersionMatcher().isDynamic(getId())) {
                            // IVY-56: check if revision has actually been resolved
                            if (_data.getSettings().getVersionMatcher().isDynamic(_module.getId())) {
                                Message.error("impossible to resolve dynamic revision for "+getId()+": check your configuration and make sure revision is part of your pattern");
                                _problem = new RuntimeException("impossible to resolve dynamic revision");
                                _data.getReport().addDependency(this);
                                return false;
                            }
                            IvyNode resolved = _data.getNode(_module.getId());
                            if (resolved != null) {
                                // exact revision has already been resolved
                                // => update it and discard this node
                                _md = _module.getDescriptor(); // needed for handleConfiguration
                                if (!handleConfiguration(loaded, rootModuleConf, parent, parentConf, conf, shouldBePublic)) {
                                    return false;
                                }
                                
                                if (resolved._md == null) {
                                	resolved._md = _md;
                                }
                                if (resolved._module == null) {
                                	resolved._module = _module;
                                }
                                resolved._downloaded |= _module.isDownloaded();
                                resolved._searched |= _module.isSearched();  
                                resolved._dds.putAll(_dds);
                                resolved.updateDataFrom(this, rootModuleConf);
                                resolved.loadData(rootModuleConf, parent, parentConf, conf, shouldBePublic);
                                DependencyDescriptor dd = dependencyDescriptor;
                                if (dd != null) {
                                    resolved.addDependencyArtifacts(rootModuleConf, dd.getDependencyArtifacts(parentConf));
                                    resolved.addDependencyIncludes(rootModuleConf, dd.getIncludeRules(parentConf));
                                }
                                _data.replaceNode(getId(), resolved, rootModuleConf); // this actually discards the node

                                if (_settings.logResolvedRevision()) {
                                    Message.info("\t["+_module.getId().getRevision()+"] "+getId());
                                } else {
                                    Message.verbose("\t["+_module.getId().getRevision()+"] "+getId());
                                }
                                
                                return true;
                            }
                        }
                        _downloaded = _module.isDownloaded();
                        _searched = _module.isSearched();
                    } else {
                        Message.warn("\tmodule not found: "+getId());
                        resolver.reportFailure();
                        _problem = new RuntimeException("not found");
                    }
                } catch (Exception e) {
                    _problem = e;
                }
                
                // still not resolved, report error
                if (_module == null) {
                    _data.getReport().addDependency(this);
                    return false;
                } else {
                    loaded = true;
                    if (_settings.getVersionMatcher().isDynamic(getId())) {
                        if (_settings.logResolvedRevision()) {
                            Message.info("\t["+_module.getId().getRevision()+"] "+getId());
                        } else {
                            Message.verbose("\t["+_module.getId().getRevision()+"] "+getId());
                        }
                    }
                    _md = _module.getDescriptor();
                    _confsToFetch.remove("*");
                    updateConfsToFetch(
                    		Arrays.asList(
                    				resolveSpecialConfigurations(
                    						getRequiredConfigurations(parent, parentConf), this)));
                }  
            } else {
                loaded = true;
            }
        }
        if (hasProblem()) {
            _data.getReport().addDependency(this);
            return handleConfiguration(loaded, rootModuleConf, parent, parentConf, conf, shouldBePublic) && loaded;
        }
        if (!handleConfiguration(loaded, rootModuleConf, parent, parentConf, conf, shouldBePublic)) {
            return false;
        }
        DependencyDescriptor dd = getDependencyDescriptor(parent);
        if (dd != null) {
            addDependencyArtifacts(rootModuleConf, dd.getDependencyArtifacts(parentConf));
            addDependencyIncludes(rootModuleConf, dd.getIncludeRules(parentConf));
        }
        return loaded;
        
    }

    
    

    public Collection getDependencies(String rootModuleConf, String[] confs) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get dependencies when data has not been loaded");
        }
        if (Arrays.asList(confs).contains("*")) {
            confs = _md.getConfigurationsNames();
        }
        Collection deps = new HashSet();
        for (int i = 0; i < confs.length; i++) {
            deps.addAll(getDependencies(rootModuleConf, confs[i], confs[i]));
        }
        return deps;
    }
    
    public Collection getDependencies(String rootModuleConf, String conf, String requestedConf) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get dependencies when data has not been loaded");
        }
        DependencyDescriptor[] dds = _md.getDependencies();
        Collection dependencies = new LinkedHashSet(); // it's important to respect dependencies order
        for (int i = 0; i < dds.length; i++) {
            DependencyDescriptor dd = dds[i];
            String[] dependencyConfigurations = dd.getDependencyConfigurations(conf, requestedConf);
            if (dependencyConfigurations.length == 0) {
                // no configuration of the dependency is required for current confs : 
                // it is exactly the same as if there was no dependency at all on it
                continue;
            } 
            if (isDependencyModuleExcluded(rootModuleConf, dd.getDependencyRevisionId(), conf)) {
                // the whole module is excluded, it is considered as not being part of dependencies at all
                Message.verbose("excluding "+dd.getDependencyRevisionId()+" in "+conf);
                continue;
            }
            IvyNode depNode = _data.getNode(dd.getDependencyRevisionId());
            if (depNode == null) {
                depNode = new IvyNode(_data, this, dd);
            } else {
                depNode.addDependencyDescriptor(this, dd);
                if (depNode.hasProblem()) {
                    // dependency already tried to be resolved, but unsuccessfully
                    // nothing special to do
                }
                
            }
            Collection confs = Arrays.asList(resolveSpecialConfigurations(dependencyConfigurations, depNode));
            depNode.updateConfsToFetch(confs);
            depNode.setRequiredConfs(this, conf, confs);
            
            depNode.addCaller(rootModuleConf, this, conf, dependencyConfigurations, dd);
            dependencies.add(depNode);
        }
        return dependencies;
    }

    private void addDependencyDescriptor(IvyNode parent, DependencyDescriptor dd) {
        _dds.put(parent, dd);
    }

    public DependencyDescriptor getDependencyDescriptor(IvyNode parent) {
        return (DependencyDescriptor)_dds.get(parent);
    }
    
    private boolean isDependencyModuleExcluded(String rootModuleConf, ModuleRevisionId dependencyRevisionId, String conf) {
        return _callers.doesCallersExclude(rootModuleConf, DefaultArtifact.newIvyArtifact(dependencyRevisionId, null));
    }

    

    
    
    public boolean hasConfigurationsToLoad() {
        return !_confsToFetch.isEmpty();
    }

    private boolean markRootModuleConfLoaded(String rootModuleConf) {
        return _loadedRootModuleConfs.add(rootModuleConf);
    }
    
    private boolean isRootModuleConfLoaded(String rootModuleConf) {
        return _loadedRootModuleConfs.contains(rootModuleConf);
    }

    private boolean handleConfiguration(boolean loaded, String rootModuleConf, IvyNode parent, String parentConf, String conf, boolean shouldBePublic) {
        if (_md != null) {
            String[] confs = getRealConfs(conf);
            for (int i = 0; i < confs.length; i++) {
                Configuration c = _md.getConfiguration(confs[i]);
                if (c == null) {
                    _confsToFetch.remove(conf);
                    if (!conf.equals(confs[i])) {
                        _problem = new RuntimeException("configuration(s) not found in "+this+": "+conf+". Missing configuration: "+confs[i]+". It was required from "+parent+" "+parentConf);
                    } else {
                        _problem = new RuntimeException("configuration(s) not found in "+this+": "+confs[i]+". It was required from "+parent+" "+parentConf);
                    }
                    _data.getReport().addDependency(this);
                    return false;
                } else if (shouldBePublic && !isRoot() && c.getVisibility() != Configuration.Visibility.PUBLIC) {
                    _confsToFetch.remove(conf);
                    _problem = new RuntimeException("configuration not public in "+this+": "+c+". It was required from "+parent+" "+parentConf);
                    _data.getReport().addDependency(this);
                    return false;
                }
                if (loaded) {
                    _fetchedConfigurations.add(conf);
                    _confsToFetch.removeAll(Arrays.asList(confs));
                    _confsToFetch.remove(conf);
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
        _confsToFetch.addAll(confs);
        _confsToFetch.removeAll(_fetchedConfigurations);
    }
    
    /**
     * resolve the '*' special configurations if necessary and possible
     */
    private String[] resolveSpecialConfigurations(String[] dependencyConfigurations, IvyNode node) {
        if (dependencyConfigurations.length == 1
                && dependencyConfigurations[0].startsWith("*")
                && node != null
                && node.isLoaded()) {
            String conf = dependencyConfigurations[0];
            if ("*".equals(conf)) {
                return node.getDescriptor().getPublicConfigurationsNames();
            }
            // there are exclusions in the configuration
            List exclusions = Arrays.asList(conf.substring(2).split("\\!"));
            
            List ret = new ArrayList(Arrays.asList(node.getDescriptor().getPublicConfigurationsNames()));
            ret.removeAll(exclusions);
            
            return (String[])ret.toArray(new String[ret.size()]);
        }
        return dependencyConfigurations;
    }

    /**
     * returns the required configurations from the given node
     * @param in
     * @return
     */
    public String[] getRequiredConfigurations(IvyNode in, String inConf) {
        Collection req = (Collection)_requiredConfs.get(new NodeConf(in, inConf));
        return req == null?new String[0]:(String[])req.toArray(new String[req.size()]);
    }

    /**
     * returns all the current required configurations of the node
     * @return
     */
    public String[] getRequiredConfigurations() {
        Collection required = new ArrayList(_confsToFetch.size() + _fetchedConfigurations.size());
        required.addAll(_fetchedConfigurations);
        required.addAll(_confsToFetch);
        return (String[])required.toArray(new String[required.size()]);
    }
    
    private void setRequiredConfs(IvyNode parent, String parentConf, Collection confs) {
        _requiredConfs.put(new NodeConf(parent, parentConf), new HashSet(confs));
    }

    public Configuration getConfiguration(String conf) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get configuration when data has not been loaded");
        }
        String defaultConf = getDefaultConf(conf);
        conf = getMainConf(conf);
        Configuration configuration = _md.getConfiguration(conf);
        if (configuration == null) {
            configuration = _md.getConfiguration(defaultConf);
        }
        return configuration;
    }

    /**
     * Returns the configurations of the dependency required in a given 
     * root module configuration.
     * @param rootModuleConf
     * @return
     */
    public String[] getConfigurations(String rootModuleConf) {
        Set depConfs = (Set) _rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            return new String[0];
        }
        return (String[]) depConfs.toArray(new String[depConfs.size()]);
    }
    
    public void discardConf(String rootModuleConf, String conf) {
        Set depConfs = (Set) _rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            depConfs = new HashSet();
            _rootModuleConfs.put(rootModuleConf, depConfs);
        }
        if (_md != null) {
            // remove all given dependency configurations to the set + extended ones 
                Configuration c = _md.getConfiguration(conf);
                if (conf != null) {
                    String[] exts = c.getExtends();
                    for (int i = 0; i < exts.length; i++) {
                        discardConf(rootModuleConf, exts[i]); // recursive remove of extended configurations
                    }
                    depConfs.remove(c.getName());
                } else {
                    Message.warn("unknown configuration in "+getId()+": "+conf);
                }
        } else {
            depConfs.remove(conf);
        }
    }

    private void addRootModuleConfigurations(String rootModuleConf, String[] dependencyConfs) {
        Set depConfs = (Set) _rootModuleConfs.get(rootModuleConf);
        if (depConfs == null) {
            depConfs = new HashSet();
            _rootModuleConfs.put(rootModuleConf, depConfs);
        }
        if (_md != null) {
            // add all given dependency configurations to the set + extended ones 
            for (int i = 0; i < dependencyConfs.length; i++) {
                Configuration conf = _md.getConfiguration(dependencyConfs[i]);
                if (conf != null) {
                    String[] exts = conf.getExtends();
                    addRootModuleConfigurations(rootModuleConf, exts); // recursive add of extended configurations
                    depConfs.add(conf.getName());
                } else {
                    Message.warn("unknown configuration in "+getId()+": "+dependencyConfs[i]);
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
     * @return
     */
    public String[] getRootModuleConfigurations() {
        return (String[])_rootModuleConfs.keySet().toArray(new String[_rootModuleConfs.size()]);
    }


    public String[] getConfsToFetch() {
        return (String[])_confsToFetch.toArray(new String[_confsToFetch.size()]);
    }

    public String[] getRealConfs(String conf) {
        if (_md == null) {
            return new String[] {conf};
        }
        String defaultConf = getDefaultConf(conf);
        conf = getMainConf(conf);
        if (_md.getConfiguration(conf) == null) {
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
     * @param from the module id to start the path from
     * @return a collection representing the path, starting with the from node, followed by
     * the list of nodes being one path to the current node, excluded
     */
    private Collection findPath(ModuleId from) {
		return findPath(from, this, new LinkedList());
	}
    
    private Collection findPath(ModuleId from, IvyNode node, List path) {
    	IvyNode parent = (IvyNode) node.getDirectCallerFor(from);
    	if (parent == null) {
    		throw new IllegalArgumentException("no path from "+from+" to "+getId()+" found");
    	}
    	if (path.contains(parent)) {
        	path.add(0, parent);
    		Message.verbose("circular dependency found while looking for the path for another one: was looking for "+from+" as a caller of "+path.get(path.size()-1));
    		return path;
    	}
    	path.add(0, parent);
    	if (parent.getId().getModuleId().equals(from)) {
    		return path;
    	}
		return findPath(from, parent, path);
	}

    
    
    
    
    private void updateDataFrom(IvyNode node, String rootModuleConf) {
        // update callers
    	_callers.updateFrom(node._callers, rootModuleConf);
        
        // update requiredConfs
        updateMapOfSet(node._requiredConfs, _requiredConfs);
        
        // update rootModuleConfs
        updateMapOfSetForKey(node._rootModuleConfs, _rootModuleConfs, rootModuleConf);
        
        // update dependencyArtifactsIncludes
        updateMapOfSetForKey(node._dependencyArtifacts, _dependencyArtifacts, rootModuleConf);
        
        // update confsToFetch
        updateConfsToFetch(node._fetchedConfigurations);
        updateConfsToFetch(node._confsToFetch);
    }
    
    private void updateMapOfSet(Map from, Map to) {
        for (Iterator iter = from.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            updateMapOfSetForKey(from, to, key);
        }        
    }

    private void updateMapOfSetForKey(Map from, Map to, Object key) {
        Set set = (Set)from.get(key);
        if (set != null) {
            Set toupdate = (Set)to.get(key);
            if (toupdate != null) {
                toupdate.addAll(set);
            } else {
                to.put(key, new HashSet(set));
            }
        }
    }
    
    
    
    
    /**
     * Returns all the artifacts of this dependency required in all the
     * root module configurations
     * @return
     */
    public Artifact[] getAllArtifacts() {
        Set ret = new HashSet();
        for (Iterator it = _rootModuleConfs.keySet().iterator(); it.hasNext();) {
            String rootModuleConf = (String)it.next();
            ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
        }
        return (Artifact[])ret.toArray(new Artifact[ret.size()]);
    }

    /**
     * Returns all the artifacts of this dependency required in the
     * root module configurations in which the node is not evicted
     * @param artifactFilter 
     * @return
     */
    public Artifact[] getSelectedArtifacts(Filter artifactFilter) {
        Collection ret = new HashSet();
        for (Iterator it = _rootModuleConfs.keySet().iterator(); it.hasNext();) {
            String rootModuleConf = (String)it.next();
            if (!isEvicted(rootModuleConf)) {
                ret.addAll(Arrays.asList(getArtifacts(rootModuleConf)));
            }
        }
        ret = FilterHelper.filter(ret, artifactFilter);
        return (Artifact[])ret.toArray(new Artifact[ret.size()]);
    }

    /**
     * Returns the artifacts of this dependency required in the
     * configurations themselves required in the given root module configuration
     * @param rootModuleConf
     * @return
     */
    public Artifact[] getArtifacts(String rootModuleConf) {
        // first we look for the dependency configurations required
        // in the given root module configuration
        Set confs = (Set) _rootModuleConfs.get(rootModuleConf);
        if (confs == null) {
            // no configuration required => no artifact required
            return new Artifact[0];
        }
        
        Set artifacts = new HashSet(); // the set we fill before returning
        
        // we check if we have dependencyArtifacts includes description for this rootModuleConf
        Set dependencyArtifacts = (Set)_dependencyArtifacts.get(rootModuleConf);
        
        if (_md.isDefault() && dependencyArtifacts != null && !dependencyArtifacts.isEmpty()) {
            // the descriptor is a default one: it has been generated from nothing
            // moreover, we have dependency artifacts description
            // these descritions are thus used as if they were declared in the module
            // descriptor. If one is not really present, the error will be raised
            // at download time
            for (Iterator it = dependencyArtifacts.iterator(); it.hasNext();) {
                DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)it.next();
                artifacts.add(new MDArtifact(_md, dad.getName(), dad.getType(), dad.getExt(), dad.getUrl(), dad.getExtraAttributes()));
            }
        } else {
            Set includes = (Set)_dependencyIncludes.get(rootModuleConf);
            
            if ((dependencyArtifacts == null || dependencyArtifacts.isEmpty())
            		&& (includes == null || includes.isEmpty())) {
                // no artifacts / includes: we get all artifacts as defined by the descriptor
                for (Iterator iter = confs.iterator(); iter.hasNext();) {
                    String conf = (String) iter.next();
                    artifacts.addAll(Arrays.asList(_md.getArtifacts(conf)));
                }
            } else {
            	// we have to get only artifacts listed as "includes"
                
                // first we get all artifacts as defined by the module descriptor
                // and classify them by artifact id
                Map allArtifacts = new HashMap();
                for (Iterator iter = confs.iterator(); iter.hasNext();) {
                    String conf = (String) iter.next();
                    Artifact[] arts = _md.getArtifacts(conf);
                    for (int i = 0; i < arts.length; i++) {
                        allArtifacts.put(arts[i].getId().getArtifactId(), arts[i]);
                    }
                }
                
                // now we add caller defined ones
                for (Iterator it = dependencyArtifacts.iterator(); it.hasNext();) {
                    DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)it.next();
                    artifacts.add(new MDArtifact(_md, dad.getName(), dad.getType(), dad.getExt(), 
                    		dad.getUrl(), dad.getExtraAttributes()));
                }
                
                // and now we filter according to include rules
                for (Iterator it = includes.iterator(); it.hasNext();) {
                	IncludeRule dad = (IncludeRule)it.next();
                	Collection arts = findArtifactsMatching(dad, allArtifacts);
                	if (arts.isEmpty()) {
                		Message.error("a required artifact is not listed by module descriptor: "+dad.getId());
                		// we remove it from required list to prevent message to be displayed more than once
                		it.remove(); 
                	} else {
                		Message.debug(this+" in "+rootModuleConf+": including "+arts);
                		artifacts.addAll(arts);
                	}
                }
            }
        }
        
        
        // now excludes artifacts that aren't accepted by any caller
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact)iter.next();
            boolean excluded = _callers.doesCallersExclude(rootModuleConf, artifact);
            if (excluded) {
                Message.debug(this+" in "+rootModuleConf+": excluding "+artifact);
                iter.remove();
            }
        }
        return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
    }

    private static Collection findArtifactsMatching(IncludeRule rule, Map allArtifacts) {
        Collection ret = new ArrayList();
        for (Iterator iter = allArtifacts.keySet().iterator(); iter.hasNext();) {
            ArtifactId aid = (ArtifactId)iter.next();
            if (MatcherHelper.matches(rule.getMatcher(), rule.getId(), aid)) {
                ret.add(allArtifacts.get(aid));
            }
        }
        return ret;
    }

    private void addDependencyArtifacts(String rootModuleConf, DependencyArtifactDescriptor[] dependencyArtifacts) {
        addObjectsForConf(rootModuleConf, Arrays.asList(dependencyArtifacts), _dependencyArtifacts);
    }

    private void addDependencyIncludes(String rootModuleConf, IncludeRule[] rules) {
    	addObjectsForConf(rootModuleConf, Arrays.asList(rules), _dependencyIncludes);
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
        return _problem != null;
    }
    
    public Exception getProblem() {
        return _problem;
    }
    
    public String getProblemMessage() {
    	Exception e = _problem;
    	if (e == null) {
    		return "";
    	}
		String errMsg = e instanceof RuntimeException?e.getMessage():e.toString();
		if (errMsg == null || errMsg.length()==0 || "null".equals(errMsg)) {
			errMsg = e.getClass().getName() + " at "+e.getStackTrace()[0].toString();
		}
		return errMsg;
    }

    public boolean isDownloaded() {
        return _downloaded;
    }
    
    public boolean isSearched() {
        return _searched;
    }

    public boolean isLoaded() {
        return _md != null;
    }

    /**
     * Returns true if this node can already be found among its callers
     * @return
     */
    public boolean isCircular() {
        return _isCircular;
    }
    
    public boolean isFetched(String conf) {
        return _fetchedConfigurations.contains(conf);
    }

    public IvyNode findNode(ModuleRevisionId mrid) {
        return _data.getNode(mrid);
    }

    boolean isRoot() {
        return _root == this;
    }

    public IvyNode getRoot() {
		return _root;
	}

    public ConflictManager getConflictManager(ModuleId mid) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get conflict manager when data has not been loaded");
        }
        ConflictManager cm = _md.getConflictManager(mid);
        return cm == null ? _settings.getConflictManager(mid) : cm;
    }
    
    public IvyNode getRealNode() {
    	IvyNode real = _data.getNode(getId());
        return real != null?real:this;
    }

    public ModuleRevisionId getId() {
    	return _id;
    }

    public ModuleId getModuleId() {
        return _id.getModuleId();
    }

    public ModuleDescriptor getDescriptor() {
        return _md;
    }

	public ResolveData getData() {
		return _data;
	}

    public ResolvedModuleRevision getModuleRevision() {
        return _module;
    }

    public long getPublication() {
        if (_module != null) {
            return _module.getPublicationDate().getTime();
        }
        return 0;
    }

    /**
     * Returns the last modified timestamp of the module represented by this Node,
     * or 0 if the last modified timestamp is currently unkwown (module not loaded)
     * @return the last modified timestamp of the module represented by this Node
     */
	public long getLastModified() {
		if (_md != null) {
			return _md.getLastModified();
		}
		return 0;
	}

    public ModuleRevisionId getResolvedId() {
        if (_md != null && _md.getResolvedModuleRevisionId().getRevision() != null) {
            return _md.getResolvedModuleRevisionId();
        } else if (_module != null) {
            return _module.getId();
        } else {
            return getId();
        }
    }
    
	/**
	 * Clean data related to one root module configuration only
	 */
	public void clean() {
		_confsToFetch.clear();
	}

	///////////////////////////////////////////////////////////////////////////////
	//          CALLERS MANAGEMENT
	/////////////////////////////////////////////////////////////////////////////// 

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
		return _callers.getDirectCallerFor(from);
	}

    public Caller[] getCallers(String rootModuleConf) {
		return _callers.getCallers(rootModuleConf);
	}
    
	public Collection getAllCallersModuleIds() {
		return _callers.getAllCallersModuleIds();
	}

	public Caller[] getAllCallers() {
		return _callers.getAllCallers();
	}
	

    public void addCaller(String rootModuleConf, IvyNode callerNode, String callerConf, String[] dependencyConfs, DependencyDescriptor dd) {
    	_callers.addCaller(rootModuleConf, callerNode, callerConf, dependencyConfs, dd);
        _isCircular = _callers.getAllCallersModuleIds().contains(getId().getModuleId());
        if (_isCircular) {
        	IvyContext.getContext().getCircularDependencyStrategy().handleCircularDependency(
        			toMrids(findPath(getId().getModuleId()), this));
        }
    }

    
	public boolean doesCallersExclude(String rootModuleConf, Artifact artifact, Stack callersStack) {
		return _callers.doesCallersExclude(rootModuleConf, artifact, callersStack);
	}
	

	private ModuleRevisionId[] toMrids(Collection path, IvyNode depNode) {
    	ModuleRevisionId[] ret = new ModuleRevisionId[path.size()+1];
    	int i=0;
    	for (Iterator iter = path.iterator(); iter.hasNext(); i++) {
			IvyNode node = (IvyNode) iter.next();
			ret[i] = node.getId();
		}
    	ret[ret.length-1] = depNode.getId();
		return ret;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//          EVICTION MANAGEMENT
	/////////////////////////////////////////////////////////////////////////////// 

	public Collection getResolvedNodes(ModuleId moduleId, String rootModuleConf) {
		return _eviction.getResolvedNodes(moduleId, rootModuleConf);
	}

	public Collection getResolvedRevisions(ModuleId moduleId, String rootModuleConf) {
		return _eviction.getResolvedRevisions(moduleId, rootModuleConf);
	}
	
    public void markEvicted(EvictionData evictionData) {
        _eviction.markEvicted(evictionData);
        if (!_rootModuleConfs.keySet().contains(evictionData.getRootModuleConf())) {
            _rootModuleConfs.put(evictionData.getRootModuleConf(), null);
        }
        
        // bug 105: update selected data with evicted one
        if (evictionData.getSelected() != null) {
            for (Iterator iter = evictionData.getSelected().iterator(); iter.hasNext();) {
                IvyNode selected = (IvyNode)iter.next();
                selected.updateDataFrom(this, evictionData.getRootModuleConf());
            }
        }
    }

	public Collection getAllEvictingConflictManagers() {
		return _eviction.getAllEvictingConflictManagers();
	}

	public Collection getAllEvictingNodes() {
		return _eviction.getAllEvictingNodes();
	}

	public String[] getEvictedConfs() {
		return _eviction.getEvictedConfs();
	}

	public EvictionData getEvictedData(String rootModuleConf) {
		return _eviction.getEvictedData(rootModuleConf);
	}

	public Collection getEvictedNodes(ModuleId mid, String rootModuleConf) {
		return _eviction.getEvictedNodes(mid, rootModuleConf);
	}

	public Collection getEvictedRevisions(ModuleId mid, String rootModuleConf) {
		return _eviction.getEvictedRevisions(mid, rootModuleConf);
	}

	public EvictionData getEvictionDataInRoot(String rootModuleConf, IvyNode ancestor) {
		return _eviction.getEvictionDataInRoot(rootModuleConf, ancestor);
	}

	public boolean isCompletelyEvicted() {
		return _eviction.isCompletelyEvicted();
	}

	public boolean isEvicted(String rootModuleConf) {
		return _eviction.isEvicted(rootModuleConf);
	}

	public void markEvicted(String rootModuleConf, IvyNode node, ConflictManager conflictManager, Collection resolved) {
		_eviction.markEvicted(rootModuleConf, node, conflictManager, resolved);
	}

	public void setEvictedNodes(ModuleId moduleId, String rootModuleConf, Collection evicted) {
		_eviction.setEvictedNodes(moduleId, rootModuleConf, evicted);
	}

	public void setResolvedNodes(ModuleId moduleId, String rootModuleConf, Collection resolved) {
		_eviction.setResolvedNodes(moduleId, rootModuleConf, resolved);
	}


	public String toString() {
        return getResolvedId().toString();
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof IvyNode)) {
            return false;
        }
        IvyNode node = (IvyNode)obj;
        return node.getId().equals(getId());
    }

    public int compareTo(Object obj) {
        IvyNode that = (IvyNode)obj;
        return this.getModuleId().compareTo(that.getModuleId());
    }
    
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Returns a collection of Nodes in conflict for which conflict has been detected
     * but conflict resolution hasn't been done yet
     * @param rootModuleConf
     * @param mid the module id for which pending conflicts should be found
     * @return a Collection of IvyNode in pending conflict
     */
	public Collection getPendingConflicts(String rootModuleConf, ModuleId mid) {
		return _eviction.getPendingConflicts(rootModuleConf, mid);
	}

	public void setPendingConflicts(ModuleId moduleId, String rootModuleConf, Collection conflicts) {
		_eviction.setPendingConflicts(moduleId, rootModuleConf, conflicts);
	}
}
