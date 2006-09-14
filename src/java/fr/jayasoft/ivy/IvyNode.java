/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import fr.jayasoft.ivy.event.resolve.EndResolveDependencyEvent;
import fr.jayasoft.ivy.event.resolve.StartResolveDependencyEvent;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.matcher.MatcherHelper;
import fr.jayasoft.ivy.util.Message;

public class IvyNode {
    private static final Pattern FALLBACK_CONF_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    public static class EvictionData {
        private IvyNode _node; // can be null in case of transitive eviction
        private ConflictManager _conflictManager; // can be null in case of transitive eviction
        private Collection _selected; // can be null in case of transitive eviction
        private String _rootModuleConf;

        public EvictionData(String rootModuleConf, IvyNode node, ConflictManager conflictManager, Collection selected) {
            _rootModuleConf = rootModuleConf;
            _node = node;
            _conflictManager = conflictManager;
            _selected = selected;
        }
        
        public String toString() {
            if (_selected != null) {
                return _selected + " in "+ _node+" ("+_conflictManager+") ["+_rootModuleConf+"]";
            } else {
                return "transitively ["+_rootModuleConf+"]";
            }
        }

        public ConflictManager getConflictManager() {
            return _conflictManager;
        }
        

        public IvyNode getNode() {
            return _node;
        }

        public Collection getSelected() {
            return _selected;
        }
        

        public String getRootModuleConf() {
            return _rootModuleConf;
        }
        
        
    }

    public class Caller {
        private ModuleDescriptor _md;
        private ModuleRevisionId _mrid;
        private Map _confs = new HashMap(); // Map (String callerConf -> String[] dependencyConfs)
        private DependencyDescriptor _dd;
        private boolean _callerCanExclude;
        
        public Caller(ModuleDescriptor md, ModuleRevisionId mrid, DependencyDescriptor dd, boolean callerCanExclude) {
            _md = md;
            _mrid = mrid;
            _dd = dd;
            _callerCanExclude = callerCanExclude;
        }
        public void addConfiguration(String callerConf, String[] dependencyConfs) {
            String[] prevDepConfs = (String[])_confs.get(callerConf);
            if (prevDepConfs != null) {
                Set newDepConfs = new HashSet(Arrays.asList(prevDepConfs));
                newDepConfs.addAll(Arrays.asList(dependencyConfs));
                _confs.put(callerConf, (String[])newDepConfs.toArray(new String[newDepConfs.size()]));
            } else {
                _confs.put(callerConf, dependencyConfs);
            }
        }
        public String[] getCallerConfigurations() {
            return (String[])_confs.keySet().toArray(new String[_confs.keySet().size()]);
        }
        public ModuleRevisionId getModuleRevisionId() {
            return _mrid;
        }
        public boolean equals(Object obj) {
            if (! (obj instanceof Caller)) {
                return false;
            }
            Caller other = (Caller)obj;
            return other._confs.equals(_confs) 
                && _mrid.equals(other._mrid);
        }
        public int hashCode() {
            int hash = 31;
            hash = hash * 13 + _confs.hashCode();
            hash = hash * 13 + _mrid.hashCode();
            return hash;
        }
        public String toString() {
            return _mrid.toString();
        }
        public ModuleRevisionId getAskedDependencyId() {
            return _dd.getDependencyRevisionId();
        }
        public ModuleDescriptor getModuleDescriptor() {
            return _md;
        }
        public boolean canExclude() {
            return _callerCanExclude || _dd.canExclude();
        }
        public DependencyDescriptor getDependencyDescriptor() {
            return _dd;
        }
    }
    private static final class NodeConf {
        private IvyNode _node;
        private String _conf;

        public NodeConf(IvyNode node, String conf) {
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

    private static final class ModuleIdConf {
        private ModuleId _moduleId;
        private String _conf;

        public ModuleIdConf(ModuleId mid, String conf) {
            _moduleId = mid;
            _conf = conf;
        }

        public final String getConf() {
            return _conf;
        }
        
        public final ModuleId getModuleId() {
            return _moduleId;
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof ModuleIdConf)) {
                return false;
            }
            return getModuleId().equals(((ModuleIdConf)obj).getModuleId()) 
                && getConf().equals(((ModuleIdConf)obj).getConf());
        }
        public int hashCode() {
            int hash = 33;
            hash += getModuleId().hashCode() * 17;
            hash += getConf().hashCode() * 17;
            return hash;
        }
    }

    private ModuleRevisionId _id; // id as requested, i.e. may be with latest rev
    
    /** 
     * Represents the current parent of the node during ivy visit
     * of dependency graph.
     * Use callers on Dependency to know all the callers
     * of a dependency
     */
    private IvyNode _parent = null;
    private IvyNode _root = null;
    private Collection _path = null; // Collection(IvyNode): direct path from root to this node. Note that the colleciton is ordered but is not a list implementation
    private String _parentConf = null;
    private String _rootModuleConf;

    private Map _selectedDeps = new HashMap(); // Map (ModuleIdConf -> Set(Node)) // map indicating for each dependency which node has been selected

    private Map _evictedDeps = new HashMap(); // Map (ModuleIdConf -> Set(Node)) // map indicating for each dependency which node has been evicted
    private Map _evictedRevs = new HashMap(); // Map (ModuleIdConf -> Set(ModuleRevisionId)) // map indicating for each dependency which revision has been evicted
    
    private Map _evicted = new HashMap(); // Map (root module conf -> EvictionData) // indicates if the node is evicted in each root module conf

    // Map (String rootModuleConf -> Map (ModuleRevisionId -> Caller)): key in second map is used to easily get a caller by its mrid
    private Map _callersByRootConf = new HashMap(); 
    
    // Map (String rootConfName -> Set(String confName))
    // used to know which configurations of the dependency are required by root
    // module configuration
    private Map _rootModuleConfs = new HashMap(); 
        
    // Map (String rootModuleConf -> Set(DependencyArtifactDescriptor))
    private Map _dependencyArtifactsIncludes = new HashMap();

    // shared data
    private ResolveData _data;

    private Collection _confsToFetch = new HashSet();
    private Collection _fetchedConfigurations = new HashSet();

    // set only when node has been built or updated from a DependencyDescriptor
    private Map _dds = new HashMap(); // Map(IvyNode parent -> DependencyDescriptor)

    // Set when data has been loaded only
    private ModuleDescriptor _md;

    private ResolvedModuleRevision _module;

    private Exception _problem = null;
    
    private boolean _downloaded = false;
    private boolean _searched = false;


    private Map _requiredConfs = new HashMap(); // Map (NodeConf in -> Set(String conf))

    private boolean _isRoot = false;

    // this map contains all the module ids calling this one as keys
    // the mapped nodes correspond to a direct caller from which the transitive caller comes
    
    private Map _allCallers = new HashMap(); // Map (ModuleId -> IvyNode)

    private boolean _isCircular = false;

    private Collection _loadedRootModuleConfs = new HashSet();

    private Map _requestedConf = new HashMap(); // Maps of requested confs per root configuration
                                                // this data is contextual to the current step of the 
    											// resolve process

    
    public IvyNode(ResolveData data, IvyNode parent, DependencyDescriptor dd) {
        _id = dd.getDependencyRevisionId();
        _dds.put(parent, dd);
        _isRoot = false;

        init(data, true);
    }

    public IvyNode(ResolveData data, ModuleDescriptor md) {
        _id = md.getModuleRevisionId();
        _md = md;
        _isRoot = true;
        _root = this;
        
        // we do not register nodes created from ModuleDescriptor, cause they are
        // the root of resolve
        init(data, false);
    }

    private void init(ResolveData data, boolean register) {
        _data = data;
        if (register) {
            _data.register(this);
        }
    }

    public ConflictManager getConflictManager(ModuleId mid) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get conflict manager when data has not been loaded");
        }
        ConflictManager cm = _md.getConflictManager(mid);
        return cm == null ? _data.getIvy().getDefaultConflictManager() : cm;
    }
    
    public Collection getResolvedNodes(ModuleId mid, String rootModuleConf) {
        Collection resolved = (Collection)_selectedDeps.get(new ModuleIdConf(mid, rootModuleConf));
        Set ret = new HashSet();
        if (resolved != null) {
            for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                IvyNode node = (IvyNode)iter.next();
                ret.add(node.getRealNode());
            }
        }
        return ret;
    }
    public Collection getResolvedRevisions(ModuleId mid, String rootModuleConf) {
    	Collection resolved = (Collection)_selectedDeps.get(new ModuleIdConf(mid, rootModuleConf));
    	if (resolved == null) {
    		return new HashSet();
    	} else {
    		Collection resolvedRevs = new HashSet();
    		for (Iterator iter = resolved.iterator(); iter.hasNext();) {
    			IvyNode node = (IvyNode)iter.next();
    			resolvedRevs.add(node.getId());
    			resolvedRevs.add(node.getResolvedId());
    		}
    		return resolvedRevs;
    	}
    }

    public void setResolvedNodes(ModuleId moduleId, String rootModuleConf, Collection resolved) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        _selectedDeps.put(moduleIdConf, new HashSet(resolved));
    }
    
    public Collection getEvictedNodes(ModuleId mid, String rootModuleConf) {
        Collection resolved = (Collection)_evictedDeps.get(new ModuleIdConf(mid, rootModuleConf));
        Set ret = new HashSet();
        if (resolved != null) {
            for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                IvyNode node = (IvyNode)iter.next();
                ret.add(node.getRealNode());
            }
        }
        return ret;
    }
    public Collection getEvictedRevisions(ModuleId mid, String rootModuleConf) {
        Collection evicted = (Collection)_evictedRevs.get(new ModuleIdConf(mid, rootModuleConf));
        if (evicted == null) {
            return new HashSet();
        } else {
            return new HashSet(evicted);
        }
    }

    public void setEvictedNodes(ModuleId moduleId, String rootModuleConf, Collection evicted) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        _evictedDeps.put(moduleIdConf, new HashSet(evicted));
        Collection evictedRevs = new HashSet();
        for (Iterator iter = evicted.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            evictedRevs.add(node.getId());
            evictedRevs.add(node.getResolvedId());
        }
        _evictedRevs.put(moduleIdConf, evictedRevs);
    }
    

    public boolean isEvicted(String rootModuleConf) {
        cleanEvicted();
        return getRoot() != this && !getRoot().getResolvedRevisions(getId().getModuleId(), rootModuleConf).contains(getResolvedId());
    }

    public boolean isCompletelyEvicted() {
        cleanEvicted();
        if (getRoot() == this) {
        	return false;
        }
        for (Iterator iter = _rootModuleConfs.keySet().iterator(); iter.hasNext();) {
			String conf = (String) iter.next();
			if (!isEvicted(conf)) {
				return false;
			}
		}
        return true;
    }
    
    private void cleanEvicted() {
        // check if it was evicted by a node that we are now the real node for
        for (Iterator iter = _evicted.keySet().iterator(); iter.hasNext();) {
            String rootModuleConf = (String)iter.next();
            EvictionData ed = (EvictionData)_evicted.get(rootModuleConf);
            Collection sel = ed.getSelected();
            if (sel != null) {
                for (Iterator iterator = sel.iterator(); iterator.hasNext();) {
                    IvyNode n = (IvyNode)iterator.next();
                    if (n.getRealNode().equals(this)) {
                        // yes, we are the real node for a selected one !
                        // we are no more evicted in this conf !
                        iter.remove();                    
                    }
                }
            }
        }
    }

    public void markEvicted(String rootModuleConf, IvyNode node, ConflictManager conflictManager, Collection resolved) {
        EvictionData evictionData = new EvictionData(rootModuleConf, node, conflictManager, resolved);
        markEvicted(evictionData);
    }

    public void markEvicted(EvictionData evictionData) {
        _evicted.put(evictionData.getRootModuleConf(), evictionData);
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
    
    private void updateDataFrom(IvyNode node, String rootModuleConf) {
        // update callers
        Map nodecallers = (Map)node._callersByRootConf.get(rootModuleConf);
        if (nodecallers != null) {
            Map thiscallers = (Map)_callersByRootConf.get(rootModuleConf);
            if (thiscallers == null) {
                thiscallers = new HashMap();
                _callersByRootConf.put(rootModuleConf, thiscallers);
            }
            for (Iterator iter = nodecallers.values().iterator(); iter.hasNext();) {
                Caller caller = (Caller)iter.next();
                if (!thiscallers.containsKey(caller.getModuleRevisionId())) {
                    thiscallers.put(caller.getModuleRevisionId(), caller);
                }
            }
        }
        
        // update requiredConfs
        updateMapOfSet(node._requiredConfs, _requiredConfs);
        
        // update rootModuleConfs
        updateMapOfSetForKey(node._rootModuleConfs, _rootModuleConfs, rootModuleConf);
        
        // update dependencyArtifactsIncludes
        updateMapOfSetForKey(node._dependencyArtifactsIncludes, _dependencyArtifactsIncludes, rootModuleConf);
        
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

    public EvictionData getEvictedData(String rootModuleConf) {
        cleanEvicted();
        return (EvictionData)_evicted.get(rootModuleConf);
    }
    public String[] getEvictedConfs() {
        cleanEvicted();
        return (String[])_evicted.keySet().toArray(new String[_evicted.keySet().size()]);
    }

    /**
     * Returns null if this node has only be evicted transitively, or the the colletion of selected nodes
     * if it has been evicted by other selected nodes
     * @return
     */
    public Collection getAllEvictingNodes() {
        Collection allEvictingNodes = null;
        for (Iterator iter = _evicted.values().iterator(); iter.hasNext();) {
            EvictionData ed = (EvictionData)iter.next();
            Collection selected = ed.getSelected();
            if (selected != null) {
                if (allEvictingNodes == null) {
                    allEvictingNodes = new HashSet();
                }
                allEvictingNodes.addAll(selected);
            }
        }        
        return allEvictingNodes;
    }    

    public Collection getAllEvictingConflictManagers() {
        Collection ret = new HashSet();
        for (Iterator iter = _evicted.values().iterator(); iter.hasNext();) {
            EvictionData ed = (EvictionData)iter.next();
            ret.add(ed.getConflictManager());
        }        
        return ret;
    }    


    public IvyNode getParent() {
        return _parent;
    }    

    public void setParent(IvyNode parent) {
        _parent = parent;
        _root = null;
        _path = null;
    }

    public IvyNode getRoot() {
        if (_root == null) {
            _root = computeRoot();
        }
        return _root;
    }

    public Collection getPath() {
        if (_path == null) {
            _path = computePath();
        }
        return _path;
    }

    private Collection computePath() {
        if (_parent != null) {
            Collection p = new LinkedHashSet(_parent.getPath());
            p.add(this);
            return p;
        } else {
            return Collections.singletonList(this);
        }
    }

    private IvyNode computeRoot() {
        if (isRoot()) {
            return this;
        } else if (_parent != null) {
            return _parent.getRoot();
        } else {
            return null;
        }
//        IvyNode root = this;
//        Collection path = new HashSet();
//        path.add(root);
//        while (root.getParent() != null && !root.isRoot()) {
//            if (path.contains(root.getParent())) {
//                return root;
//            }
//            root = root.getParent();
//            path.add(root);
//        }
//        return root;
    }

    public String getParentConf() {
        return _parentConf;
    }

    public void setParentConf(String parentConf) {
        _parentConf = parentConf;
    }


    public boolean hasConfigurationsToLoad() {
        return !_confsToFetch.isEmpty();
    }

    /**
     * After the call node may be discarded. To avoid using discarded node, make sure
     * to get the real node after the call
     * IvyNode node = ...
     * node.loadData();
     * node = node.getRealNode();
     * ...
     */
    public boolean loadData(String conf, boolean shouldBePublic) {
        boolean loaded = false;
		if (!isEvicted(_rootModuleConf) && (hasConfigurationsToLoad() || !isRootModuleConfLoaded()) && !hasProblem()) {
            markRootModuleConfLoaded();
            if (_md == null) {
                DependencyResolver resolver = _data.getIvy().getResolver(getModuleId());
                if (resolver == null) {
                    Message.error("no resolver found for "+getModuleId()+": check your configuration");
                    _problem = new RuntimeException("no resolver found for "+getModuleId()+": check your configuration");
                    _data.getReport().addDependency(this);
                    return false;
                }
                try {
                    Message.debug("\tusing "+resolver+" to resolve "+getId());
                    DependencyDescriptor dependencyDescriptor = getDependencyDescriptor(getParent());
                    _data.getIvy().fireIvyEvent(new StartResolveDependencyEvent(_data.getIvy(), resolver, dependencyDescriptor));
                    _module = resolver.getDependency(dependencyDescriptor, _data);
                    _data.getIvy().fireIvyEvent(new EndResolveDependencyEvent(_data.getIvy(), resolver, dependencyDescriptor, _module));
                    if (_module != null) {
                        _data.getIvy().saveResolver(_data.getCache(), _module.getDescriptor(), _module.getResolver().getName());
                        _data.getIvy().saveArtResolver(_data.getCache(), _module.getDescriptor(), _module.getArtifactResolver().getName());
                        if (_data.getIvy().logModuleWhenFound()) {
                            Message.info("\tfound "+_module.getId()+" in "+_module.getResolver().getName());
                        } else {
                            Message.verbose("\tfound "+_module.getId()+" in "+_module.getResolver().getName());
                        }
                        
                        if (_data.getIvy().getVersionMatcher().isDynamic(getId())) {
                            // IVY-56: check if revision has actually been resolved
                            if (_data.getIvy().getVersionMatcher().isDynamic(_module.getId())) {
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
                                if (!handleConfiguration(loaded, conf, shouldBePublic)) {
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
                                resolved.updateDataFrom(this, _rootModuleConf);
                                resolved.loadData(conf, shouldBePublic);
                                DependencyDescriptor dd = dependencyDescriptor;
                                if (dd != null) {
                                    resolved.addDependencyArtifactsIncludes(_rootModuleConf, dd.getDependencyArtifactsIncludes(getParentConf()));
                                }
                                _data.register(getId(), resolved); // this actually discards the node

                                if (_data.getIvy().logResolvedRevision()) {
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
                    if (_data.getIvy().getVersionMatcher().isDynamic(getId())) {
                        if (_data.getIvy().logResolvedRevision()) {
                            Message.info("\t["+_module.getId().getRevision()+"] "+getId());
                        } else {
                            Message.verbose("\t["+_module.getId().getRevision()+"] "+getId());
                        }
                    }
                    _md = _module.getDescriptor();
                    // if the revision was a dynamic one (which has now be resolved)
                    // store also it to cache the result
                    if (_data.getIvy().getVersionMatcher().isDynamic(getId())) {
                        _data.register(_module.getId(), this);
                    }
                    _confsToFetch.remove("*");
                    updateConfsToFetch(Arrays.asList(resolveSpecialConfigurations(getRequiredConfigurations(getParent(), getParentConf()), this)));
                }  
            } else {
                loaded = true;
            }
        }
        if (hasProblem()) {
            _data.getReport().addDependency(this);
            return handleConfiguration(loaded, conf, shouldBePublic) && loaded;
        }
        if (!handleConfiguration(loaded, conf, shouldBePublic)) {
            return false;
        }
        DependencyDescriptor dd = getDependencyDescriptor(getParent());
        if (dd != null) {
            addDependencyArtifactsIncludes(_rootModuleConf, dd.getDependencyArtifactsIncludes(getParentConf()));
        }
        return loaded;
        
    }

    private boolean markRootModuleConfLoaded() {
        return _loadedRootModuleConfs.add(_rootModuleConf);
    }
    
    private boolean isRootModuleConfLoaded() {
        return _loadedRootModuleConfs.contains(_rootModuleConf);
    }

    private boolean handleConfiguration(boolean loaded, String conf, boolean shouldBePublic) {
        if (_md != null) {
            String[] confs = getRealConfs(conf);
            for (int i = 0; i < confs.length; i++) {
                Configuration c = _md.getConfiguration(confs[i]);
                if (c == null) {
                    _confsToFetch.remove(conf);
                    if (!conf.equals(confs[i])) {
                        _problem = new RuntimeException("configuration(s) not found in "+this+": "+conf+". Missing configuration: "+confs[i]+". It was required from "+getParent()+" "+getParentConf());
                    } else {
                        _problem = new RuntimeException("configuration(s) not found in "+this+": "+confs[i]+". It was required from "+getParent()+" "+getParentConf());
                    }
                    _data.getReport().addDependency(this);
                    return false;
                } else if (shouldBePublic && !isRoot() && c.getVisibility() != Configuration.Visibility.PUBLIC) {
                    _confsToFetch.remove(conf);
                    _problem = new RuntimeException("configuration not public in "+this+": "+c+". It was required from "+getParent()+" "+getParentConf());
                    _data.getReport().addDependency(this);
                    return false;
                }
                if (loaded) {
                    _fetchedConfigurations.add(conf);
                    _confsToFetch.removeAll(Arrays.asList(confs));
                    _confsToFetch.remove(conf);
                }
                addRootModuleConfigurations(_rootModuleConf, confs);
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

    private boolean isRoot() {
        return _isRoot ;
    }

    public IvyNode getRealNode() {
        return getRealNode(false);
    }


    public IvyNode getRealNode(boolean traverse) {
        IvyNode node = _data.getNode(getId());
        if (node != null) {
            if (traverse) {
            	node.setParent(getParent());
                node.setParentConf(getParentConf());
                node.setRootModuleConf(getRootModuleConf());
                node.setRequestedConf(getRequestedConf());
                node._data = _data;                
            }
            return node;
        } else {
            return this;
        }
    }

    public Collection getDependencies(String[] confs) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get dependencies when data has not been loaded");
        }
        if (Arrays.asList(confs).contains("*")) {
            confs = _md.getConfigurationsNames();
        }
        Collection deps = new HashSet();
        for (int i = 0; i < confs.length; i++) {
            deps.addAll(getDependencies(confs[i], false));
        }
        return deps;
    }
    
    public Collection getDependencies(String conf, boolean traverse) {
        if (_md == null) {
            throw new IllegalStateException("impossible to get dependencies when data has not been loaded");
        }
        DependencyDescriptor[] dds = _md.getDependencies();
        Collection dependencies = new LinkedHashSet(); // it's important to respect dependencies order
        for (int i = 0; i < dds.length; i++) {
            DependencyDescriptor dd = dds[i];
            String[] dependencyConfigurations = dd.getDependencyConfigurations(conf, getRequestedConf());
            if (dependencyConfigurations.length == 0) {
                // no configuration of the dependency is required for current confs : 
                // it is exactly the same as if there was no dependency at all on it
                continue;
            } 
            if (isDependencyModuleExcluded(dd.getDependencyRevisionId(), conf)) {
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
            
            depNode.addCaller(_rootModuleConf, this, conf, dependencyConfigurations, dd);
            dependencies.add(depNode);
            if (traverse) {
            	traverse(conf, depNode);
            }
        }
        return dependencies;
    }

	public void traverse(String conf, IvyNode depNode) {
		if (getPath().contains(depNode)) {
			IvyContext.getContext().getCircularDependencyStrategy().handleCircularDependency(toMrids(getPath(), depNode));
		} else {
		    depNode.setParent(this);
		}
		depNode.setParentConf(conf);
		depNode.setRootModuleConf(getRootModuleConf());
		depNode._data = _data;
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

	/**
     * @return Returns the requestedConf in the current root module configuration
     */
    public final String getRequestedConf() {
        return (String) _requestedConf.get(getRootModuleConf());
    }
    
    public final void setRequestedConf(String requestedConf) {
        _requestedConf.put(getRootModuleConf(), requestedConf);
    }

    private void addDependencyDescriptor(IvyNode parent, DependencyDescriptor dd) {
        _dds.put(parent, dd);
    }

    private boolean isDependencyModuleExcluded(ModuleRevisionId dependencyRevisionId, String conf) {
        return doesCallersExclude(getRootModuleConf(), DefaultArtifact.newIvyArtifact(dependencyRevisionId, null));
    }

    public ModuleRevisionId getId() {
        return _id;
    }

    public void updateConfsToFetch(Collection confs) {
        _confsToFetch.addAll(confs);
        _confsToFetch.removeAll(_fetchedConfigurations);
    }

    public ModuleId getModuleId() {
        return _id.getModuleId();
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

    public boolean isLoaded() {
        return _md != null;
    }

    public ModuleDescriptor getDescriptor() {
        return _md;
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

    public ResolvedModuleRevision getModuleRevision() {
        return _module;
    }


    /**
     * 
     * @param rootModuleConf
     * @param mrid
     * @param callerConf
     * @param dependencyConfs '*' must have been resolved
     * @param dd the dependency revision id asked by the caller
     */
    public void addCaller(String rootModuleConf, IvyNode node, String callerConf, String[] dependencyConfs, DependencyDescriptor dd) {
        ModuleDescriptor md = node.getDescriptor();
        ModuleRevisionId mrid = node.getId(); 
        if (mrid.getModuleId().equals(getId().getModuleId())) {
            throw new IllegalArgumentException("a module is not authorized to depend on itself: "+getId());
        }
        Map callers = (Map)_callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            callers = new HashMap();
            _callersByRootConf.put(rootModuleConf, callers);
        }
        Caller caller = (Caller)callers.get(mrid);
        if (caller == null) {
            caller = new Caller(md, mrid, dd, node.canExclude(rootModuleConf));
            callers.put(mrid, caller);
        }
        caller.addConfiguration(callerConf, dependencyConfs);

        IvyNode parent = node.getRealNode();
    	for (Iterator iter = parent._allCallers.keySet().iterator(); iter.hasNext();) {
			ModuleId mid = (ModuleId) iter.next();
			_allCallers.put(mid, parent);
		}
        _allCallers.put(mrid.getModuleId(), node);
        _isCircular = _allCallers.keySet().contains(getId().getModuleId());
        if (_isCircular) {
        	IvyContext.getContext().getCircularDependencyStrategy().handleCircularDependency(
        			toMrids(findPath(getId().getModuleId()), this));
        }
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
    	IvyNode parent = (IvyNode) node._allCallers.get(from);
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

	private boolean canExclude(String rootModuleConf) {
        DependencyDescriptor dd = getDependencyDescriptor(getParent());
        if (dd != null && dd.canExclude()) {
            return true;
        }
        Caller[] callers = getCallers(rootModuleConf);
        for (int i = 0; i < callers.length; i++) {
            if (callers[i].canExclude()) {
                return true;
            }
        }
        return false;
    }

    public Caller[] getCallers(String rootModuleConf) {
        Map callers = (Map)_callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            return new Caller[0];
        }
        return (Caller[])callers.values().toArray(new Caller[callers.values().size()]);
    }

    public Caller[] getAllCallers() {
        Set all = new HashSet();
        for (Iterator iter = _callersByRootConf.values().iterator(); iter.hasNext();) {
            Map callers = (Map)iter.next();
            all.addAll(callers.values());
        }
        return (Caller[])all.toArray(new Caller[all.size()]);
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
    
    public int hashCode() {
        return getId().hashCode();
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

    public void discardConf(String conf) {
        discardConf(_rootModuleConf, conf);
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
        Set includes = (Set)_dependencyArtifactsIncludes.get(rootModuleConf);
        
        if (_md.isDefault() && includes != null && !includes.isEmpty()) {
            // the descriptor is a default one: it has been generated from nothing
            // moreover, we have dependency artifacts description
            // these descritions are thus used as if they were declared in the module
            // descriptor. If one is not really present, the error will be raised
            // at download time
            for (Iterator it = includes.iterator(); it.hasNext();) {
                DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)it.next();
                artifacts.add(new MDArtifact(_md, dad.getName(), dad.getType(), dad.getExt(), dad.getUrl(), null));
            }
        } else {
            if (includes == null || includes.isEmpty()) {
                // no artifacts includes: we get all artifacts as defined by the descriptor
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
                
                // now we can keep only listed ones
                for (Iterator it = includes.iterator(); it.hasNext();) {
                    DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)it.next();
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
            boolean excluded = doesCallersExclude(rootModuleConf, artifact);
            if (excluded) {
                Message.debug(this+" in "+rootModuleConf+": excluding "+artifact);
                iter.remove();
            }
        }
        return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
    }

    /**
     * Returns true if ALL callers exclude the given artifact in the given root module conf
     * @param rootModuleConf
     * @param artifact
     * @return
     */
    private boolean doesCallersExclude(String rootModuleConf, Artifact artifact) {
        return doesCallersExclude(rootModuleConf, artifact, new Stack());
    }
    private boolean doesCallersExclude(String rootModuleConf, Artifact artifact, Stack callersStack) {
        if (callersStack.contains(getId())) {
            return false;
        }
        callersStack.push(getId());
        try {
            Caller[] callers = getCallers(rootModuleConf);
            if (callers.length == 0) {
                return false;
            }
            Collection callersNodes = new ArrayList();
            for (int i = 0; i < callers.length; i++) {
                if (!callers[i].canExclude()) {
                    return false;
                }
                ModuleDescriptor md = callers[i].getModuleDescriptor();
                if (!doesExclude(md, rootModuleConf, callers[i].getCallerConfigurations(), this, callers[i].getDependencyDescriptor(), artifact, callersStack)) {
                    return false;
                }
            }
            return true;
        } finally {
            callersStack.pop();
        }
    }

    private boolean doesExclude(ModuleDescriptor md, String rootModuleConf, String[] moduleConfs, IvyNode dependency, DependencyDescriptor dd, Artifact artifact, Stack callersStack) {
        // artifact is excluded if it match any of the exclude pattern for this dependency...
        if (dd != null) {
            if (dd.doesExclude(moduleConfs, artifact.getId().getArtifactId())) {
                return true;
            }
        }
        // ... or if it is excluded by all its callers
        IvyNode c = _data.getNode(md.getModuleRevisionId());
        if (c != null) {
            return c.doesCallersExclude(rootModuleConf, artifact, callersStack);
        } else {
            return false;
        }
    }

    private static DependencyDescriptor getDependencyDescriptor(ModuleDescriptor md, IvyNode dependency) {
        if (md != null) {
            DependencyDescriptor[] dds = md.getDependencies();
            for (int i = 0; i < dds.length; i++) {
                if (dds[i].getDependencyId().equals(dependency.getModuleId())) {
                    return dds[i];
                }
            }
        }
        return null;
    }

    private static Collection findArtifactsMatching(DependencyArtifactDescriptor dad, Map allArtifacts) {
        Collection ret = new ArrayList();
        for (Iterator iter = allArtifacts.keySet().iterator(); iter.hasNext();) {
            ArtifactId aid = (ArtifactId)iter.next();
            if (MatcherHelper.matches(dad.getMatcher(), dad.getId(), aid)) {
                ret.add(allArtifacts.get(aid));
            }
        }
        return ret;
    }

    private void addDependencyArtifactsIncludes(String rootModuleConf, DependencyArtifactDescriptor[] dependencyArtifacts) {
        addDependencyArtifacts(rootModuleConf, dependencyArtifacts, _dependencyArtifactsIncludes);
    }

    private void addDependencyArtifacts(String rootModuleConf, DependencyArtifactDescriptor[] dependencyArtifacts, Map artifactsMap) {
        Set depArtifacts = (Set) artifactsMap.get(rootModuleConf);
        if (depArtifacts == null) {
            depArtifacts = new HashSet();
            artifactsMap.put(rootModuleConf, depArtifacts);
        }
        depArtifacts.addAll(Arrays.asList(dependencyArtifacts));
    }

    public long getPublication() {
        if (_module != null) {
            return _module.getPublicationDate().getTime();
        }
        return 0;
    }

    public DependencyDescriptor getDependencyDescriptor(IvyNode parent) {
        return (DependencyDescriptor)_dds.get(parent);
    }

    public boolean hasProblem() {
        return _problem != null;
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
    
    public Exception getProblem() {
        return _problem;
    }

    public boolean isDownloaded() {
        return _downloaded;
    }
    
    public boolean isSearched() {
        return _searched;
    }

    public String getRootModuleConf() {
        return _rootModuleConf;
    }
    

    public void setRootModuleConf(String rootModuleConf) {
        if (_rootModuleConf != null && !_rootModuleConf.equals(rootModuleConf)) {
            _confsToFetch.clear(); // we change of root module conf => we discard all confs to fetch
        }
        if (rootModuleConf != null && rootModuleConf.equals(_rootModuleConf)) {
            _selectedDeps.put(new ModuleIdConf(_id.getModuleId(), rootModuleConf), Collections.singleton(this));
        }
        _rootModuleConf = rootModuleConf;
    }

    public String[] getConfsToFetch() {
        return (String[])_confsToFetch.toArray(new String[_confsToFetch.size()]);
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

    /**
     * Returns the eviction data for this node if it has been previously evicted in the most far parent
     * of the given node, null otherwise (if it hasn't been evicted in root) for the 
     * given rootModuleConf.
     * Note that this method only works if conflict resolution has already be done in all the ancestors.
     * 
     * @param rootModuleConf
     * @param parent
     * @return
     */
    public EvictionData getEvictionDataInRoot(String rootModuleConf, IvyNode parent) {
        IvyNode root = parent.getRoot();
        Collection selectedNodes = root.getResolvedNodes(getModuleId(), rootModuleConf);
        for (Iterator iter = selectedNodes.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.getResolvedId().equals(getResolvedId())) {
                // the node is part of the selected ones for the root: no eviction data to return
                return null;
            }
        }
        // we didn't find this mrid in the selected ones for the root: it has been previously evicted
        return new EvictionData(rootModuleConf, parent, root.getConflictManager(getModuleId()), selectedNodes);
    }

    public static IvyNode getRoot(IvyNode parent) {
        IvyNode root = parent;
        Collection path = new HashSet();
        path.add(root);
        while (root.getParent() != null && !root.isRoot()) {
            if (path.contains(root.getParent())) {
                return root;
            }
            root = root.getParent();
            path.add(root);
        }
        return root;
    }

    public IvyNode findNode(ModuleRevisionId mrid) {
        return _data.getNode(mrid);
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
     * Returns true if the current dependency descriptor is transitive
     * and the parent configuration is transitive.  Otherwise returns false.
     * @param node curent node
     * @return true if current node is transitive and the parent configuration is
     * transitive.
     */
    protected boolean isTransitive() {
        return (_data.isTransitive() &&
        		getDependencyDescriptor(getParent()).isTransitive() &&
                isParentConfTransitive() );
    }

    /**
     * Checks if the current node's parent configuration is transitive.
     * @param node current node
     * @return true if the node's parent configuration is transitive
     */
    protected boolean isParentConfTransitive() {
        String conf = getParent().getRequestedConf();
        if (conf==null) {
            return true;
        }
        Configuration parentConf = getParent().getConfiguration(conf);
        return parentConf.isTransitive();

    }

	public ResolveData getResolveData() {
		return _data;
	}

}
