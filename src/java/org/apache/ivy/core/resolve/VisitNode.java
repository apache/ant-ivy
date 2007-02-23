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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.plugins.conflict.ConflictManager;

/**
 * A visit node is an object used to represent one visit from one parent on 
 * an {@link IvyNode} of the dependency graph.
 * 
 * During dependency resolution, the {@link ResolveEngine} visits nodes of the 
 * depency graph following the dependencies, thus the same node can be visited
 * several times, if it is requested from several module.
 * 
 * In this case you will have one VisitNode per parent and per root module 
 * configuration.
 * 
 * Thus VisitNode stores data specific to the visit:
 * <ul>
 * <li>parent</li> the node from which the visit is occuring
 * <li>parentConf</li> the configuration of the parent in which this node is visited
 * <li>rootModuleConf</li> the configuration of the root module which is currently resolved
 * </ul>
 * 
 * @author Xavier Hanin
 */
public class VisitNode {
	/**
	 * The node which is currently visited 
	 */
	private IvyNode _node;
    /** 
     * Represents the current parent of the node during ivy visit
     * of dependency graph.
     */
    private VisitNode _parent = null;
    /**
     * The root node of the current visit
     * It is null until it is required, see getRoot
     */
    private VisitNode _root = null;
    /**
     * Direct path from root to this node. 
     * Note that the colleciton is ordered but is not a list implementation 
     * This collection is null until it is required, see getPath
     */
    private Collection _path = null; // Collection(VisitNode)
    
    
    /**
     * The configuration of the parent module in the current visit 
     */
    private String _parentConf = null;
    /**
     * The configuration requested by the parent
     * Note that this is the actual conf requested by the parent, not 
     * a configuration extended by the requested conf which actually 
     * trigger the node visit
     */
    private String _requestedConf; 
    /**
     * The root configuration which is currently visited
     */
    private String _rootModuleConf;
    
    /**
     * Shared ResolveData instance, which can be used
     * to get info on the current resolve process
     */
    private ResolveData _data;

    
    public VisitNode(ResolveData data, IvyNode node, VisitNode parent, String rootModuleConf, String parentConf) {
    	if (data == null) {
    		throw new NullPointerException("data must not be null");
    	}
    	if (node == null) {
    		throw new NullPointerException("node must not be null");
    	}
    	if (rootModuleConf == null) {
    		throw new NullPointerException("rootModuleConf must not be null");
    	}
    	_data = data;
    	_node = node;
    	_parent = parent;
    	_rootModuleConf = rootModuleConf;
    	_parentConf = parentConf;

        // we do not register if this is a root module (root == no parent)
        init(data, _parent != null);
    }

    private void init(ResolveData data, boolean register) {
        _data = data;
        if (register) {
            _data.register(this);
        }
    }
    
    
	public IvyNode getNode() {
		return _node;
	}

	/**
     * @return Returns the configuration requested by the parent
     */
    public String getRequestedConf() {
        return _requestedConf;
    }
    
    public void setRequestedConf(String requestedConf) {
        _requestedConf = requestedConf;
    }
    

    public VisitNode getParent() {
        return _parent;
    }    

    public VisitNode getRoot() {
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

    private VisitNode computeRoot() {
        if (_node.isRoot()) {
            return this;
        } else if (_parent != null) {
            return _parent.getRoot();
        } else {
            return null;
        }
    }

    public String getParentConf() {
        return _parentConf;
    }

    public void setParentConf(String parentConf) {
        _parentConf = parentConf;
    }

    public String getRootModuleConf() {
        return _rootModuleConf;
    }

    public static VisitNode getRoot(VisitNode parent) {
    	VisitNode root = parent;
        Collection path = new HashSet();
        path.add(root);
        while (root.getParent() != null && !root.getNode().isRoot()) {
            if (path.contains(root.getParent())) {
                return root;
            }
            root = root.getParent();
            path.add(root);
        }
        return root;
    }
    

    /**
     * Returns true if the current dependency descriptor is transitive
     * and the parent configuration is transitive.  Otherwise returns false.
     * @param node curent node
     * @return true if current node is transitive and the parent configuration is
     * transitive.
     */
    public boolean isTransitive() {
        return (_data.isTransitive() &&
        		_node.getDependencyDescriptor(getParentNode()).isTransitive() &&
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
        Configuration parentConf = getParentNode().getConfiguration(conf);
        return parentConf.isTransitive();

    }

    /**
     * Returns the 'real' node currently visited.
     * 'Real' means that if we are visiting a node created originally with only a version
     * constraint, and if this version constraint has been resolved to an existing node
     * in the graph, we will return the existing node, and not the one originally used
     * which is about to be discarded, since it's not possible to have in the graph
     * two nodes for the same ModuleRevisionId
     * @return the 'real' node currently visited.
     */
    public IvyNode getRealNode() {
        IvyNode node = _node.getRealNode();
        if (node != null) {
            return node;
        } else {
            return _node;
        }
    }

    /**
     * Ask to the current visited node to use a real node only, if one exist. 
     * See getRealNode for details about what a 'real' node is.
     */
    public void useRealNode() {
        IvyNode node = _data.getNode(_node.getId());
        if (node != null && node != _node) {
        	_node = node;
        }
    }

    public boolean loadData(String conf, boolean shouldBePublic) {
        boolean loaded = _node.loadData(_rootModuleConf, getParentNode(), _parentConf, conf, shouldBePublic);
        if (loaded) {
	        useRealNode();
	
	        // if the revision was a dynamic one (which has now be resolved)
	        // we now register this node on the resolved id
	        if (_data.getSettings().getVersionMatcher().isDynamic(getId())) {
	            _data.register(_node.getResolvedId(), this);
	        }
        }

        return loaded;
    }

    public Collection getDependencies(String conf) {
    	Collection deps = _node.getDependencies(_rootModuleConf, conf, _requestedConf);
    	Collection ret = new ArrayList(deps.size());
    	for (Iterator iter = deps.iterator(); iter.hasNext();) {
			IvyNode depNode = (IvyNode) iter.next();
			ret.add(traverseChild(conf, depNode));
		}
    	return ret;
    }
    
    /**
     * Returns a VisitNode for the given node.
     * The given node must be a representation of the same module 
     * (usually in another revision) as the one visited by this node.
     * 
     * The given node must also have been already visited.
     * 
     * @param node the node to visit
     * @return a VisitNode for the given node
     */
	VisitNode gotoNode(IvyNode node) {
		if (!getModuleId().equals(node.getModuleId())) {
			throw new IllegalArgumentException("you can't use gotoNode for a node which does not represent the same Module as the one represented by this node.\nCurrent node module id="+getModuleId()+" Given node module id="+node.getModuleId());
		}
		VisitData visitData = _data.getVisitData(node.getId());
		if (visitData == null) {
			throw new IllegalArgumentException("you can't use gotoNode with a node which has not been visited yet.\nGiven node id="+node.getId());
		}
		for (Iterator iter = visitData.getVisitNodes(_rootModuleConf).iterator(); iter.hasNext();) {
			VisitNode vnode = (VisitNode) iter.next();
			if ((_parent == null && vnode.getParent() == null) || 
					(_parent != null && _parent.getId().equals(vnode.getParent().getId()))) {
				return vnode;
			}
		}
		// the node has not yet been visited from the current parent, we create a new visit node
		return traverse(_parent, _parentConf, node);
	}

	private VisitNode traverseChild(String parentConf, IvyNode child) {
		VisitNode parent = this;
		return traverse(parent, parentConf, child);
	}

	private VisitNode traverse(VisitNode parent, String parentConf, IvyNode node) {
		if (getPath().contains(node)) {
			IvyContext.getContext().getCircularDependencyStrategy()
				.handleCircularDependency(toMrids(getPath(), node.getId()));
			// we do not use the new parent, but the first one, to always be able to go up to the root
//			parent = getVisitNode(depNode).getParent(); 
		}
		return new VisitNode(_data, node, parent, _rootModuleConf, parentConf);
	}

	private ModuleRevisionId[] toMrids(Collection path, ModuleRevisionId last) {
    	ModuleRevisionId[] ret = new ModuleRevisionId[path.size()+1];
    	int i=0;
    	for (Iterator iter = path.iterator(); iter.hasNext(); i++) {
    		VisitNode node = (VisitNode) iter.next();
			ret[i] = node.getNode().getId();
		}
    	ret[ret.length-1] = last;
		return ret;
	}

	public ModuleRevisionId getResolvedId() {
		return _node.getResolvedId();
	}

	public void updateConfsToFetch(Collection confs) {
		_node.updateConfsToFetch(confs);
	}

	public ModuleRevisionId getId() {
		return _node.getId();
	}

	public boolean isEvicted() {
		return _node.isEvicted(_rootModuleConf);
	}

	public String[] getRealConfs(String conf) {
		return _node.getRealConfs(conf);
	}

	public boolean hasProblem() {
		return _node.hasProblem();
	}

	public Configuration getConfiguration(String conf) {
		return _node.getConfiguration(conf);
	}

	public EvictionData getEvictedData() {
		return _node.getEvictedData(_rootModuleConf);
	}

	public DependencyDescriptor getDependencyDescriptor() {
		return _node.getDependencyDescriptor(getParentNode());
	}

	private IvyNode getParentNode() {
		return _parent==null?null:_parent.getNode();
	}

	public boolean isCircular() {
		return _node.isCircular();
	}

	public String[] getConfsToFetch() {
		return _node.getConfsToFetch();
	}

	public String[] getRequiredConfigurations(VisitNode in, String inConf) {
		return _node.getRequiredConfigurations(in.getNode(), inConf);
	}

	public ModuleId getModuleId() {
		return _node.getModuleId();
	}

	public Collection getResolvedRevisions(ModuleId mid) {
		return _node.getResolvedRevisions(mid, _rootModuleConf);
	}

	public void markEvicted(EvictionData evictionData) {
		_node.markEvicted(evictionData);
	}

	public String[] getRequiredConfigurations() {
		return _node.getRequiredConfigurations();
	}

	/**
	 * Marks the current node as evicted by the the given selected IvyNodes, 
	 * in the given parent and root module configuration, with the given
	 * {@link ConflictManager}
	 * 
	 * @param parent the VisitNode in which eviction has been made
	 * @param conflictManager the conflict manager responsible for the eviction
	 * @param selected a Collection of {@link IvyNode} which have been selected 
	 */
	public void markEvicted(VisitNode parent, ConflictManager conflictManager, Collection selected) {
		_node.markEvicted(_rootModuleConf, parent.getNode(), conflictManager, selected);
	}

	public ModuleDescriptor getDescriptor() {
		return _node.getDescriptor();
	}

	public EvictionData getEvictionDataInRoot(String rootModuleConf, VisitNode ancestor) {
		return _node.getEvictionDataInRoot(rootModuleConf, ancestor.getNode());
	}

	public Collection getEvictedRevisions(ModuleId moduleId) {
		return _node.getEvictedRevisions(moduleId, _rootModuleConf);
	}


//    public void setRootModuleConf(String rootModuleConf) {
//        if (_rootModuleConf != null && !_rootModuleConf.equals(rootModuleConf)) {
//            _confsToFetch.clear(); // we change of root module conf => we discard all confs to fetch
//        }
//        if (rootModuleConf != null && rootModuleConf.equals(_rootModuleConf)) {
//            _selectedDeps.put(new ModuleIdConf(_id.getModuleId(), rootModuleConf), Collections.singleton(this));
//        }
//        _rootModuleConf = rootModuleConf;
//    }

	public String toString() {
		return _node.toString();
	}

}
