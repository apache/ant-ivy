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
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.util.Checks;

/**
 * A visit node is an object used to represent one visit from one parent on an {@link IvyNode} of
 * the dependency graph. During dependency resolution, the {@link ResolveEngine} visits nodes of the
 * depency graph following the dependencies, thus the same node can be visited several times, if it
 * is requested from several module. In this case you will have one VisitNode per parent and per
 * root module configuration. Thus VisitNode stores data specific to the visit:
 * <ul>
 * <li>parent</li>
 * the node from which the visit is occuring
 * <li>parentConf</li>
 * the configuration of the parent in which this node is visited
 * <li>rootModuleConf</li>
 * the configuration of the root module which is currently resolved
 * </ul>
 */
public class VisitNode {
    /**
     * The node which is currently visited
     */
    private IvyNode node;

    /**
     * Represents the current parent of the node during ivy visit of dependency graph.
     */
    private VisitNode parent = null;

    /**
     * The root node of the current visit It is null until it is required, see getRoot
     */
    private VisitNode root = null;

    /**
     * Direct path from root to this node. Note that the colleciton is ordered but is not a list
     * implementation This collection is null until it is required, see getPath
     */
    private Collection<VisitNode> path = null;

    /**
     * The configuration of the parent module in the current visit
     */
    private String parentConf = null;

    /**
     * The configuration requested by the parent Note that this is the actual conf requested by the
     * parent, not a configuration extended by the requested conf which actually trigger the node
     * visit
     */
    private String requestedConf;

    /**
     * The root configuration which is currently visited
     */
    private String rootModuleConf;

    /**
     * Shared ResolveData instance, which can be used to get info on the current resolve process
     */
    private ResolveData data;

    /**
     * Boolean.TRUE if a node with a same module id as the one visited has already been visited in
     * the current path. null if not computed yet Boolean.FALSE otherwise
     */
    private Boolean isCircular;

    /**
     * IvyNode usage information to update when visiting the underlying IvyNode. This is usually the
     * main IvyNodeUsage of the underlying node, except when we are visiting it coming from an
     * evicted node replaced by the other one.
     */
    private IvyNodeUsage usage;

    public VisitNode(ResolveData data, IvyNode node, VisitNode parent, String rootModuleConf,
            String parentConf) {
        this(data, node, parent, rootModuleConf, parentConf, null);
    }

    public VisitNode(ResolveData data, IvyNode node, VisitNode parent, String rootModuleConf,
            String parentConf, IvyNodeUsage usage) {
        Checks.checkNotNull(data, "data");
        Checks.checkNotNull(node, "node");
        Checks.checkNotNull(rootModuleConf, "rootModuleConf");

        this.data = data;
        this.node = node;
        this.parent = parent;
        this.rootModuleConf = rootModuleConf;
        this.parentConf = parentConf;
        this.usage = usage;

        this.data.register(this);
    }

    public IvyNode getNode() {
        return node;
    }

    /**
     * @return Returns the configuration requested by the parent
     */
    public String getRequestedConf() {
        return requestedConf;
    }

    public void setRequestedConf(String requestedConf) {
        this.requestedConf = requestedConf;
    }

    public VisitNode getParent() {
        return parent;
    }

    public VisitNode getRoot() {
        if (root == null) {
            root = computeRoot();
        }
        return root;
    }

    /**
     * Get an ordered collection with the nodes from the root to this node
     * 
     * @return
     */
    public Collection<VisitNode> getPath() {
        if (path == null) {
            path = computePath();
        }
        return path;
    }

    private Collection<VisitNode> computePath() {
        if (parent != null) {
            Collection<VisitNode> p = new LinkedHashSet<VisitNode>(parent.getPath());
            p.add(this);
            return p;
        } else {
            return Collections.singletonList(this);
        }
    }

    private VisitNode computeRoot() {
        if (node.isRoot()) {
            return this;
        } else if (parent != null) {
            return parent.getRoot();
        } else {
            return null;
        }
    }

    public String getParentConf() {
        return parentConf;
    }

    public void setParentConf(String parentConf) {
        this.parentConf = parentConf;
    }

    public String getRootModuleConf() {
        return rootModuleConf;
    }

    public static VisitNode getRoot(VisitNode parent) {
        VisitNode root = parent;
        Collection<VisitNode> path = new HashSet<VisitNode>();
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
     * Returns true if the current dependency descriptor is transitive and the parent configuration
     * is transitive. Otherwise returns false.
     * 
     * @return true if current node is transitive and the parent configuration is transitive.
     */
    public boolean isTransitive() {
        if (node.isRoot()) {
            // the root node is always considered transitive!
            return true;
        }

        if (!data.isTransitive()) {
            return false;
        }

        if (!isParentConfTransitive()) {
            return false;
        }

        DependencyDescriptor dd = node.getDependencyDescriptor(getParentNode());
        if ((dd != null) && dd.isTransitive()) {
            return true;
        }

        return node.hasAnyMergedUsageWithTransitiveDependency(rootModuleConf);
    }

    /**
     * Checks if the current node's parent configuration is transitive.
     * 
     * @param node
     *            current node
     * @return true if the node's parent configuration is transitive
     */
    protected boolean isParentConfTransitive() {
        String conf = getParent().getRequestedConf();
        if (conf == null) {
            return true;
        }
        Configuration parentConf = getParentNode().getConfiguration(conf);
        return parentConf.isTransitive();

    }

    /**
     * Returns the 'real' node currently visited. 'Real' means that if we are visiting a node
     * created originally with only a version constraint, and if this version constraint has been
     * resolved to an existing node in the graph, we will return the existing node, and not the one
     * originally used which is about to be discarded, since it's not possible to have in the graph
     * two nodes for the same ModuleRevisionId
     * 
     * @return the 'real' node currently visited.
     */
    public IvyNode getRealNode() {
        IvyNode node = this.node.getRealNode();
        if (node != null) {
            return node;
        } else {
            return this.node;
        }
    }

    /**
     * Ask to the current visited node to use a real node only, if one exist. See getRealNode for
     * details about what a 'real' node is.
     */
    public void useRealNode() {
        if (parent != null) { // use real node make sense only for non root module
            IvyNode node = data.getNode(this.node.getId());
            if (node != null && node != this.node) {
                this.node = node;
            }
        }
    }

    public boolean loadData(String conf, boolean shouldBePublic) {
        boolean loaded = node.loadData(rootModuleConf, getParentNode(), parentConf, conf,
            shouldBePublic, getUsage());
        if (loaded) {
            useRealNode();

            // if the loaded revision is different from original one
            // we now register this node on the new resolved id
            // this includes two cases:
            // - the id refers to a dynamic revision, which has been resolved by loadData
            // - the loaded module descriptor has extra attributes in his info tag which are not
            // used when declaring the dependency
            if (data.getNode(node.getResolvedId()) == null
                    || !data.getNode(node.getResolvedId()).getId().equals(node.getResolvedId())) {
                data.register(node.getResolvedId(), this);
            }
        }

        return loaded;
    }

    public Collection<VisitNode> getDependencies(String conf) {
        Collection<IvyNode> deps = node.getDependencies(rootModuleConf, conf, requestedConf);
        Collection<VisitNode> ret = new ArrayList<VisitNode>(deps.size());
        for (IvyNode depNode : deps) {
            ret.add(traverseChild(conf, depNode));
        }
        return ret;
    }

    /**
     * Returns a VisitNode for the given node. The given node must be a representation of the same
     * module (usually in another revision) as the one visited by this node.
     * 
     * @param node
     *            the node to visit
     * @return a VisitNode for the given node
     */
    VisitNode gotoNode(IvyNode node) {
        if (!getModuleId().equals(node.getModuleId())) {
            throw new IllegalArgumentException(
                    "You can't use gotoNode for a node which does not represent the same Module "
                            + "as the one represented by this node.\nCurrent node module id="
                            + getModuleId() + " Given node module id=" + node.getModuleId());
        }
        VisitData visitData = data.getVisitData(node.getId());
        if (visitData != null) {
            List<VisitNode> visitNodes = visitData.getVisitNodes(rootModuleConf);
            for (VisitNode vnode : visitNodes) {
                if ((parent == null && vnode.getParent() == null)
                        || (parent != null && parent.getId().equals(vnode.getParent().getId()))) {
                    vnode.parentConf = parentConf;
                    vnode.usage = getUsage();
                    return vnode;
                }
            }
        }
        // the node has not yet been visited from the current parent, we create a new visit node
        return traverse(parent, parentConf, node, getUsage());
    }

    private IvyNodeUsage getUsage() {
        return usage == null ? node.getMainUsage() : usage;
    }

    private VisitNode traverseChild(String parentConf, IvyNode child) {
        VisitNode parent = this;
        return traverse(parent, parentConf, child, null);
    }

    private VisitNode traverse(VisitNode parent, String parentConf, IvyNode node, IvyNodeUsage usage) {
        if (getPath().contains(node)) {
            IvyContext.getContext().getCircularDependencyStrategy()
                    .handleCircularDependency(toMrids(getPath(), node.getId()));
            // we do not use the new parent, but the first one, to always be able to go up to the
            // root
            // parent = getVisitNode(depNode).getParent();
        }
        return new VisitNode(data, node, parent, rootModuleConf, parentConf, usage);
    }

    private ModuleRevisionId[] toMrids(Collection<VisitNode> path, ModuleRevisionId last) {
        ModuleRevisionId[] ret = new ModuleRevisionId[path.size() + 1];
        int i = 0;
        for (VisitNode node : path) {
            ret[i] = node.getNode().getId();
        }
        ret[ret.length - 1] = last;
        return ret;
    }

    public ModuleRevisionId getResolvedId() {
        return node.getResolvedId();
    }

    public void updateConfsToFetch(Collection<String> confs) {
        node.updateConfsToFetch(confs);
    }

    public ModuleRevisionId getId() {
        return node.getId();
    }

    public boolean isEvicted() {
        return node.isEvicted(rootModuleConf);
    }

    public String[] getRealConfs(String conf) {
        return node.getRealConfs(conf);
    }

    public boolean hasProblem() {
        return node.hasProblem();
    }

    public Configuration getConfiguration(String conf) {
        return node.getConfiguration(conf);
    }

    public EvictionData getEvictedData() {
        return node.getEvictedData(rootModuleConf);
    }

    public DependencyDescriptor getDependencyDescriptor() {
        return node.getDependencyDescriptor(getParentNode());
    }

    private IvyNode getParentNode() {
        return parent == null ? null : parent.getNode();
    }

    /**
     * Returns true if this node can already be found in the path
     * 
     * @return
     */
    public boolean isCircular() {
        if (isCircular == null) {
            if (parent != null) {
                isCircular = Boolean.FALSE; // asumme it's false, and see if it isn't by checking
                // the parent path
                for (VisitNode ancestor : parent.getPath()) {
                    if (getId().getModuleId().equals(ancestor.getId().getModuleId())) {
                        isCircular = Boolean.TRUE;
                        break;
                    }
                }
            } else {
                isCircular = Boolean.FALSE;
            }
        }
        return isCircular.booleanValue();
    }

    public String[] getConfsToFetch() {
        return node.getConfsToFetch();
    }

    public String[] getRequiredConfigurations(VisitNode in, String inConf) {
        return node.getRequiredConfigurations(in.getNode(), inConf);
    }

    public ModuleId getModuleId() {
        return node.getModuleId();
    }

    public Collection<ModuleRevisionId> getResolvedRevisions(ModuleId mid) {
        return node.getResolvedRevisions(mid, rootModuleConf);
    }

    public void markEvicted(EvictionData evictionData) {
        node.markEvicted(evictionData);
    }

    public String[] getRequiredConfigurations() {
        return node.getRequiredConfigurations();
    }

    /**
     * Marks the current node as evicted by the the given selected IvyNodes, in the given parent and
     * root module configuration, with the given {@link ConflictManager}
     * 
     * @param parent
     *            the VisitNode in which eviction has been made
     * @param conflictMgr
     *            the conflict manager responsible for the eviction
     * @param selected
     *            a Collection of {@link IvyNode} which have been selected
     */
    public void markEvicted(VisitNode parent, ConflictManager conflictMgr,
            Collection<IvyNode> selected) {
        node.markEvicted(rootModuleConf, parent.getNode(), conflictMgr, selected);
    }

    public ModuleDescriptor getDescriptor() {
        return node.getDescriptor();
    }

    public EvictionData getEvictionDataInRoot(String rootModuleConf, VisitNode ancestor) {
        return node.getEvictionDataInRoot(rootModuleConf, ancestor.getNode());
    }

    public Collection<ModuleRevisionId> getEvictedRevisions(ModuleId moduleId) {
        return node.getEvictedRevisions(moduleId, rootModuleConf);
    }

    // public void setRootModuleConf(String rootModuleConf) {
    // if (rootModuleConf != null && !rootModuleConf.equals(rootModuleConf)) {
    // _confsToFetch.clear(); // we change of root module conf => we discard all confs to fetch
    // }
    // if (rootModuleConf != null && rootModuleConf.equals(rootModuleConf)) {
    // _selectedDeps.put(new ModuleIdConf(_id.getModuleId(), rootModuleConf),
    // Collections.singleton(this));
    // }
    // rootModuleConf = rootModuleConf;
    // }

    @Override
    public String toString() {
        return node.toString();
    }

    public boolean isConfRequiredByMergedUsageOnly(String conf) {
        return node.isConfRequiredByMergedUsageOnly(rootModuleConf, conf);
    }

}
