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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.plugins.conflict.ConflictManager;

public class IvyNodeEviction {
    public static class EvictionData {
        private IvyNode _parent; // can be null in case of transitive eviction
        private ConflictManager _conflictManager; // can be null in case of transitive eviction
        private Collection _selected; // Collection(IvyNode); can be null in case of transitive eviction
        private String _rootModuleConf;

        public EvictionData(String rootModuleConf, IvyNode parent, ConflictManager conflictManager, Collection selected) {
            _rootModuleConf = rootModuleConf;
            _parent = parent;
            _conflictManager = conflictManager;
            _selected = selected;
            for (Iterator iter = selected.iterator(); iter.hasNext();) {
				Object o = (Object) iter.next();
				if (! (o instanceof IvyNode)) {
					throw new IllegalArgumentException("selected nodes must be instance of IvyNode. Found: "+o.getClass().getName());
				}
			}
        }
        
        public String toString() {
            if (_selected != null) {
                return _selected + " in "+ _parent+" ("+_conflictManager+") ["+_rootModuleConf+"]";
            } else {
                return "transitively ["+_rootModuleConf+"]";
            }
        }

        public ConflictManager getConflictManager() {
            return _conflictManager;
        }
        

        public IvyNode getParent() {
            return _parent;
        }

        public Collection getSelected() {
            return _selected;
        }
        

        public String getRootModuleConf() {
            return _rootModuleConf;
        }
    }

    private static final class ModuleIdConf {
        private ModuleId _moduleId;
        private String _conf;

        public ModuleIdConf(ModuleId mid, String conf) {
        	if (mid == null) {
        		throw new NullPointerException("mid cannot be null");
        	}
        	if (conf == null) {
        		throw new NullPointerException("conf cannot be null");
        	}
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
    
    private IvyNode _node;

    private Map _selectedDeps = new HashMap(); // Map (ModuleIdConf -> Set(Node)) // map indicating for each dependency which node has been selected
    private Map _pendingConflicts = new HashMap(); // Map (ModuleIdConf -> Set(Node)) // map indicating for each dependency which nodes are in pending conflict (conflict detected but not yet resolved)

    private Map _evictedDeps = new HashMap(); // Map (ModuleIdConf -> Set(Node)) // map indicating for each dependency which node has been evicted
    private Map _evictedRevs = new HashMap(); // Map (ModuleIdConf -> Set(ModuleRevisionId)) // map indicating for each dependency which revision has been evicted
    
    private Map _evicted = new HashMap(); // Map (root module conf -> EvictionData) // indicates if the node is evicted in each root module conf
    
    public IvyNodeEviction(IvyNode node) {
    	if (node == null) {
    		throw new NullPointerException("node must not be null");
    	}
		_node = node;
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
        IvyNode root = _node.getRoot();
        return root != _node 
        	&& !root.getResolvedRevisions(
        			_node.getId().getModuleId(), 
        			rootModuleConf)
        				.contains(_node.getResolvedId())
        	&& getEvictedData(rootModuleConf) != null;
    }

    public boolean isCompletelyEvicted() {
        cleanEvicted();
        if (_node.isRoot()) {
        	return false;
        }
        String[] rootModuleConfigurations = _node.getRootModuleConfigurations();
		for (int i = 0; i < rootModuleConfigurations.length; i++) {
			if (!isEvicted(rootModuleConfigurations[i])) {
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


    /**
     * Returns the eviction data for this node if it has been previously evicted in the root,
     * null otherwise (if it hasn't been evicted in root) for the 
     * given rootModuleConf.
     * Note that this method only works if conflict resolution has already be done in all the ancestors.
     * 
     * @param rootModuleConf
     * @param ancestor
     * @return
     */
    public EvictionData getEvictionDataInRoot(String rootModuleConf, IvyNode ancestor) {
        Collection selectedNodes = _node.getRoot().getResolvedNodes(_node.getModuleId(), rootModuleConf);
        for (Iterator iter = selectedNodes.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.getResolvedId().equals(_node.getResolvedId())) {
                // the node is part of the selected ones for the root: no eviction data to return
                return null;
            }
        }
        // we didn't find this mrid in the selected ones for the root: it has been previously evicted
        return new EvictionData(rootModuleConf, ancestor, _node.getRoot().getConflictManager(_node.getModuleId()), selectedNodes);
    }

	public Collection getPendingConflicts(String rootModuleConf, ModuleId mid) {
        Collection resolved = (Collection)_pendingConflicts.get(new ModuleIdConf(mid, rootModuleConf));
        Set ret = new HashSet();
        if (resolved != null) {
            for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                IvyNode node = (IvyNode)iter.next();
                ret.add(node.getRealNode());
            }
        }
        return ret;
	}

	public void setPendingConflicts(ModuleId moduleId, String rootModuleConf, Collection conflicts) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        _pendingConflicts.put(moduleIdConf, new HashSet(conflicts));
	}

}
