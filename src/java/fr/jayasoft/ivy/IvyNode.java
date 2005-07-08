/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.util.Message;

public class IvyNode {
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

    public static class Caller {
        private ModuleRevisionId _mrid;
        private Map _confs = new HashMap(); // Map (String callerConf -> String[] dependencyConfs)
        private ModuleRevisionId _askedDependencyId;
        
        public Caller(ModuleRevisionId mrid, ModuleRevisionId askedDependencyId) {
            _mrid = mrid;
            _askedDependencyId = askedDependencyId;
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
            return _askedDependencyId;
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
    private String _parentConf = null;
    private String _rootModuleConf;

    private Map _selected = new HashMap(); // Map (ModuleIdConf -> Set(Node))
    private Map _evicted = new HashMap(); // Map (root module conf -> EvictionData)

    // Map (String rootModuleConf -> Map (ModuleRevisionId -> Caller)): key in second map is used to easily get a caller by its mrid
    private Map _callersByRootConf = new HashMap(); 
    
    // Map (String rootConfName -> Set(String confName))
    // used to know which configurations of the dependency are required by root
    // module configuration
    private Map _rootModuleConfs = new HashMap(); 
        
    // Map (String rootModuleConf -> Set(DependencyArtifactDescriptor))
    private Map _dependencyArtifactsIncludes = new HashMap();
    // Map (String rootModuleConf -> Set(DependencyArtifactDescriptor))
    private Map _dependencyArtifactsExcludes = new HashMap();


    // shared data
    private ResolveData _data;

    private Collection _confsToFetch = new HashSet();
    private Collection _fetchedConfigurations = new HashSet();

    // set only when node has been built from DependencyDescriptor
    private DependencyDescriptor _dd;

    // Set when data has been loaded only
    private ModuleDescriptor _md;

    private ResolvedModuleRevision _module;

    private Exception _problem = null;
    
    private boolean _downloaded = false;
    private boolean _searched = false;


    private Map _requiredConfs = new HashMap(); // Map (NodeConf in -> Set(String conf))

    
    public IvyNode(ResolveData data, DependencyDescriptor dd) {
        _id = dd.getDependencyRevisionId();
        _dd = dd;

        init(data, true);
    }

    public IvyNode(ResolveData data, ModuleDescriptor md, String conf) {
        _id = md.getModuleRevisionId();
        _md = md;
        _confsToFetch.add(conf);
        
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
        Collection resolved = (Collection)_selected.get(new ModuleIdConf(mid, rootModuleConf));
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
        Collection resolved = (Collection)_selected.get(new ModuleIdConf(mid, rootModuleConf));
        if (resolved == null) {
            return new HashSet();
        } else {
            Collection ret = new HashSet();
            for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                IvyNode node = (IvyNode)iter.next();
                ret.add(node.getRealNode().getId());
            }
            return ret;
        }
    }

    public void setResolvedNodes(ModuleId moduleId, String rootModuleConf, Collection resolved) {
        _selected.put(new ModuleIdConf(moduleId, rootModuleConf), new HashSet(resolved));
    }
    

    public boolean isEvicted(String rootModuleConf) {
        cleanEvicted();
        return _evicted.containsKey(rootModuleConf);
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

    public void markSelected(String rootModuleConf) {
        _evicted.remove(rootModuleConf);
    }

    public void markEvicted(String rootModuleConf, IvyNode node, ConflictManager conflictManager, Collection resolved) {
        _evicted.put(rootModuleConf, new EvictionData(rootModuleConf, node, conflictManager, resolved));
        if (!_rootModuleConfs.keySet().contains(rootModuleConf)) {
            _rootModuleConfs.put(rootModuleConf, null);
        }
        
        // bug 105: update selected data with evicted one
        if (resolved != null) {
            for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                IvyNode selected = (IvyNode)iter.next();
                selected.updateDataFrom(this, rootModuleConf);
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
        
        // update dependencyArtifactsExcludes
        updateMapOfSetForKey(node._dependencyArtifactsExcludes, _dependencyArtifactsExcludes, rootModuleConf);
        
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

    public boolean isCompletelyEvicted() {
        cleanEvicted();
        return _evicted.keySet().containsAll(_rootModuleConfs.keySet());
    }

    public Collection getAllEvictingNodes() {
        Collection allEvictingNodes = new HashSet();
        for (Iterator iter = _evicted.values().iterator(); iter.hasNext();) {
            EvictionData ed = (EvictionData)iter.next();
            Collection selected = ed.getSelected();
            if (selected != null) {
                allEvictingNodes.addAll(selected);
            }
        }        
        return allEvictingNodes;
    }    


    public IvyNode getParent() {
        return _parent;
    }    

    public void setParent(IvyNode parent) {
        _parent = parent;
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
    public boolean loadData(String conf) {
        boolean loaded = _md != null;
        if (!isEvicted(_rootModuleConf) && hasConfigurationsToLoad() && !hasProblem()) {
            if (_md == null) {
                DependencyResolver resolver = _data.getIvy().getResolver(getModuleId());
                try {
                    Message.debug("\tusing "+resolver+" to resolve "+getId());
                    _module = resolver.getDependency(_dd, _data);
                    if (_module != null) {
                        if (!getId().isExactRevision()) { 
                            IvyNode resolved = _data.getNode(_module.getId());
                            if (resolved != null) {
                                // exact revision has already been resolved
                                // => update it and discard this node
                                resolved._downloaded |= _module.isDownloaded();
                                resolved._searched |= _module.isSearched();
                                resolved.markSelected(_rootModuleConf);
                                resolved.updateDataFrom(this, _rootModuleConf);
                                resolved.loadData(conf);
                                if (_dd != null) {
                                    resolved.addDependencyArtifactsIncludes(_rootModuleConf, _dd.getDependencyArtifactsIncludes(getParentConf()));
                                    resolved.addDependencyArtifactsExcludes(_rootModuleConf, _dd.getDependencyArtifactsExcludes(getParentConf()));
                                }
                                _data.register(getId(), resolved); // this actually discards the node
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
                } catch (ParseException e) {
                    _problem = e;
                }
                
                // still not resolved, report error
                if (_module == null) {
                    _data.getReport().addDependency(this);
                    return false;
                } else {
                    loaded = true;
                    if (!getId().isExactRevision()) {                
                        Message.info("\t["+_module.getId().getRevision()+"] "+getId());
                    }
                    _md = _module.getDescriptor();
                    // if the revision was a latest one (which has now be resolved)
                    // store also it to cache the result
                    if (!getId().isExactRevision()) {
                        _data.register(_module.getId(), this);
                    }
                    _confsToFetch.remove("*");
                    updateConfsToFetch(Arrays.asList(resolveSpecialConfigurations(getRequiredConfigurations(getParent(), getParentConf()), this)));
                }  
            }
        }
        if (hasProblem()) {
            _data.getReport().addDependency(this);
            return loaded;
        }
        if ("*".equals(conf)) {
            if (_md != null) {
                _fetchedConfigurations.addAll(Arrays.asList(_md.getConfigurationsNames()));
                _confsToFetch.clear();
                addRootModuleConfigurations(_rootModuleConf, _md.getConfigurationsNames());
            }
        } else {
            if (loaded) {
                _fetchedConfigurations.add(conf);
                _confsToFetch.remove(conf);
            }
            addRootModuleConfigurations(_rootModuleConf, new String[] {conf});
        }
        if (_dd != null) {
            addDependencyArtifactsIncludes(_rootModuleConf, _dd.getDependencyArtifactsIncludes(getParentConf()));
            addDependencyArtifactsExcludes(_rootModuleConf, _dd.getDependencyArtifactsExcludes(getParentConf()));
        }
        return loaded;
        
    }

    public IvyNode getRealNode() {
        IvyNode node = _data.getNode(getId());
        return node == null ? this : node;
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
            String[] dependencyConfigurations = dd.getDependencyConfigurations(conf);
            if (dependencyConfigurations.length == 0) {
                // no configuration of the dependency is required for current confs : 
                // it is exactly the same as if there was no dependency at all on it
                continue;
            } 
            IvyNode depNode = _data.getNode(dd.getDependencyRevisionId());
            if (depNode == null) {
                depNode = new IvyNode(_data, dd);
            } else if (depNode.hasProblem()) {
                // dependency already tried to be resolved, but unsuccessfully
                // nothing special to do
                
            }
            Collection confs = Arrays.asList(resolveSpecialConfigurations(dependencyConfigurations, depNode));
            depNode.updateConfsToFetch(confs);
            depNode.setRequiredConfs(this, conf, confs);
            
            depNode.addCaller(_rootModuleConf, _md.getModuleRevisionId(), conf, dependencyConfigurations, dd.getDependencyRevisionId());
            dependencies.add(depNode);

            if (traverse) {
                depNode.setParent(this);
                depNode.setParentConf(conf);
                depNode.setRootModuleConf(getRootModuleConf());
                depNode._data = _data;
            }
        }
        return dependencies;
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
                && "*".equals(dependencyConfigurations[0])
                && node != null
                && node.isLoaded()) {
            return node.getDescriptor().getConfigurationsNames();
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
        return _md.getConfiguration(conf);
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
     * @param askedDependencyId the dependency revision id asked by the caller
     */
    public void addCaller(String rootModuleConf, ModuleRevisionId mrid, String callerConf, String[] dependencyConfs, ModuleRevisionId askedDependencyId) {
        Map callers = (Map)_callersByRootConf.get(rootModuleConf);
        if (callers == null) {
            callers = new HashMap();
            _callersByRootConf.put(rootModuleConf, callers);
        }
        Caller caller = (Caller)callers.get(mrid);
        if (caller == null) {
            caller = new Caller(mrid, askedDependencyId);
            callers.put(mrid, caller);
        }
        caller.addConfiguration(callerConf, dependencyConfs);
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
                artifacts.add(new MDArtifact(_md, dad.getName(), dad.getType(), dad.getExt()));
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
                    Collection arts = findArtifactsMatching(dad.getId(), allArtifacts);
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
        
        // now excludes artifacts in the exclude list
        Set excludes = (Set)_dependencyArtifactsExcludes.get(rootModuleConf);
        if (excludes != null) {
            for (Iterator it = excludes.iterator(); it.hasNext();) {
                DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)it.next();
                for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
                    Artifact artifact = (Artifact)iter.next();
                    if (artifactIdMatch(dad.getId(), artifact.getId().getArtifactId())) {
                        Message.debug(this+" in "+rootModuleConf+": excluding "+artifact);
                        iter.remove();
                    }
                }
            }
        }
        return (Artifact[]) artifacts.toArray(new Artifact[artifacts.size()]);
    }

    private Collection findArtifactsMatching(ArtifactId id, Map allArtifacts) {
        Artifact art = (Artifact)allArtifacts.get(id);
        if (art != null) {
            return Collections.singleton(art);
        }
        Collection ret = new ArrayList();
        for (Iterator iter = allArtifacts.keySet().iterator(); iter.hasNext();) {
            ArtifactId aid = (ArtifactId)iter.next();
            if (artifactIdMatch(id, aid)) {
                ret.add(allArtifacts.get(aid));
            }
        }
        return ret;
    }


    private boolean artifactIdMatch(ArtifactId id, ArtifactId aid) {
        if (aid.equals(id)) {
            return true;
        }
        return stringMatch(id.getModuleId().getOrganisation(), aid.getModuleId().getOrganisation())
            && stringMatch(id.getModuleId().getName(), aid.getModuleId().getName())
            && stringMatch(id.getName(), aid.getName())
            && stringMatch(id.getExt(), aid.getExt())
            && stringMatch(id.getType(), aid.getType())
            ;
    }

    private boolean stringMatch(String pattern, String test) {
        if (test.equals(pattern)) {
            return true;
        }
        return Pattern.matches(pattern, test);
    }

    private void addDependencyArtifactsIncludes(String rootModuleConf, DependencyArtifactDescriptor[] dependencyArtifacts) {
        addDependencyArtifacts(rootModuleConf, dependencyArtifacts, _dependencyArtifactsIncludes);
    }

    private void addDependencyArtifactsExcludes(String rootModuleConf, DependencyArtifactDescriptor[] dependencyArtifacts) {
        addDependencyArtifacts(rootModuleConf, dependencyArtifacts, _dependencyArtifactsExcludes);
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

    public DependencyDescriptor getDependencyDescriptor() {
        return _dd;
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
        if (rootModuleConf != null && rootModuleConf.equals(_rootModuleConf)) {
            _selected.put(new ModuleIdConf(_id.getModuleId(), rootModuleConf), Collections.singleton(this));
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
        return isCircular(this);
    }
    
    private boolean isCircular(IvyNode node) {
        boolean isCircular = false;
        IvyNode.Caller[] callers = node.getCallers(getRootModuleConf());
        for (int i = 0; i < callers.length && !isCircular; i++) {
            ModuleRevisionId mrid = callers[i].getModuleRevisionId();
            if (getId().equals(mrid)) {
                return true;
            }
            IvyNode parent = _data.getNode(mrid);
            if (parent != null) {
                isCircular = isCircular(parent);
            }
        }
        return isCircular;
    }
}
