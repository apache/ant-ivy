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
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.conflict.ConflictManager;

public class IvyNodeEviction {
    /**
     * This class contains data about the eviction of an {@link IvyNode}.
     */
    public static class EvictionData {
        /**
         * Can be null in case of transitive eviction.
         */
        private IvyNode parent;

        /**
         * Can be null in case of transitive eviction.
         */
        private ConflictManager conflictManager;

        /**
         * Can be null in case of transitive eviction.
         */
        private Collection<IvyNode> selected;

        private String rootModuleConf;

        private String detail;

        /**
         * Creates a new object containing the eviction data of an {@link IvyNode}.
         * 
         * @param rootModuleConf
         *            the root module configuration
         * @param parent
         *            the parent node (or <tt>null</tt> in case of transitive eviction)
         * @param conflictManager
         *            the conflict manager which evicted the node (or <tt>null</tt> in case of
         *            transitive eviction)
         * @param selected
         *            a collection of {@link IvyNode}s which evict the evicted node (or
         *            <tt>null</tt> in case of transitive eviction)
         */
        public EvictionData(String rootModuleConf, IvyNode parent, ConflictManager conflictManager,
                Collection<IvyNode> selected) {
            this(rootModuleConf, parent, conflictManager, selected, null);
        }

        /**
         * Creates a new object containing the eviction data of an {@link IvyNode}.
         * 
         * @param rootModuleConf
         *            the root module configuration
         * @param parent
         *            the parent node (or <tt>null</tt> in case of transitive eviction)
         * @param conflictManager
         *            the conflict manager which evicted the node (or <tt>null</tt> in case of
         *            transitive eviction)
         * @param selected
         *            a collection of {@link IvyNode}s which evict the evicted node (or
         *            <tt>null</tt> in case of transitive eviction)
         * @param detail
         *            a String detailing the reason why the node was evicted
         */
        public EvictionData(String rootModuleConf, IvyNode parent, ConflictManager conflictManager,
                Collection<IvyNode> selected, String detail) {
            this.rootModuleConf = rootModuleConf;
            this.parent = parent;
            this.conflictManager = conflictManager;
            this.selected = selected;
            this.detail = detail;
        }

        @Override
        public String toString() {
            if (selected != null) {
                return selected + " in " + parent + (detail == null ? "" : " " + detail) + " ("
                        + conflictManager + ") [" + rootModuleConf + "]";
            } else {
                return "transitively [" + rootModuleConf + "]";
            }
        }

        public ConflictManager getConflictManager() {
            return conflictManager;
        }

        public IvyNode getParent() {
            return parent;
        }

        public Collection<IvyNode> getSelected() {
            return selected;
        }

        public String getRootModuleConf() {
            return rootModuleConf;
        }

        public boolean isTransitivelyEvicted() {
            return parent == null;
        }

        public String getDetail() {
            return detail;
        }
    }

    private static final class ModuleIdConf {
        private ModuleId moduleId;

        private String conf;

        public ModuleIdConf(ModuleId mid, String conf) {
            if (mid == null) {
                throw new NullPointerException("mid cannot be null");
            }
            if (conf == null) {
                throw new NullPointerException("conf cannot be null");
            }
            moduleId = mid;
            this.conf = conf;
        }

        public final String getConf() {
            return conf;
        }

        public final ModuleId getModuleId() {
            return moduleId;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ModuleIdConf)) {
                return false;
            }
            return getModuleId().equals(((ModuleIdConf) obj).getModuleId())
                    && getConf().equals(((ModuleIdConf) obj).getConf());
        }

        @Override
        public int hashCode() {
            // CheckStyle:MagicNumber| OFF
            int hash = 33;
            hash += getModuleId().hashCode() * 17;
            hash += getConf().hashCode() * 17;
            // CheckStyle:MagicNumber| ON
            return hash;
        }
    }

    private IvyNode node;

    // map indicating for each dependency which node has been selected
    private Map<ModuleIdConf, Set<IvyNode>> selectedDeps = new HashMap<ModuleIdConf, Set<IvyNode>>();

    // map indicating for each dependency which nodes are in pending conflict (conflict detected but
    // not yet resolved)
    private Map<ModuleIdConf, Set<IvyNode>> pendingConflicts = new HashMap<ModuleIdConf, Set<IvyNode>>();

    // map indicating for each dependency which node has been evicted
    private Map<ModuleIdConf, Set<IvyNode>> evictedDeps = new HashMap<ModuleIdConf, Set<IvyNode>>();

    // map indicating for each dependency which revision has been evicted
    private Map<ModuleIdConf, Collection<ModuleRevisionId>> evictedRevs = new HashMap<ModuleIdConf, Collection<ModuleRevisionId>>();

    // indicates if the node is evicted in each root module conf
    private Map<String, EvictionData> evicted = new HashMap<String, EvictionData>();

    public IvyNodeEviction(IvyNode node) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        this.node = node;
    }

    /**
     * @return A copy of the set of resolved nodes (real nodes)
     */
    public Set<IvyNode> getResolvedNodes(ModuleId mid, String rootModuleConf) {
        Collection<IvyNode> resolved = selectedDeps.get(new ModuleIdConf(mid, rootModuleConf));
        Set<IvyNode> ret = new HashSet<IvyNode>();
        if (resolved != null) {
            for (IvyNode node : resolved) {
                ret.add(node.getRealNode());
            }
        }
        return ret;
    }

    public Collection<ModuleRevisionId> getResolvedRevisions(ModuleId mid, String rootModuleConf) {
        Collection<IvyNode> resolved = selectedDeps.get(new ModuleIdConf(mid, rootModuleConf));
        if (resolved == null) {
            return new HashSet<ModuleRevisionId>();
        } else {
            Collection<ModuleRevisionId> resolvedRevs = new HashSet<ModuleRevisionId>();
            for (IvyNode node : resolved) {
                ModuleRevisionId resolvedId = node.getResolvedId();
                resolvedRevs.add(node.getId());
                resolvedRevs.add(resolvedId);

                // in case there are extra attributes on the resolved module we also add the
                // the module without these extra attributes (cfr. IVY-1236)
                if (!resolvedId.getExtraAttributes().isEmpty()) {
                    resolvedRevs.add(ModuleRevisionId.newInstance(resolvedId.getOrganisation(),
                        resolvedId.getName(), resolvedId.getBranch(), resolvedId.getRevision()));
                }
            }
            return resolvedRevs;
        }
    }

    public void setResolvedNodes(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> resolved) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        selectedDeps.put(moduleIdConf, new HashSet<IvyNode>(resolved));
    }

    public Collection<IvyNode> getEvictedNodes(ModuleId mid, String rootModuleConf) {
        Collection<IvyNode> resolved = evictedDeps.get(new ModuleIdConf(mid, rootModuleConf));
        Set<IvyNode> ret = new HashSet<IvyNode>();
        if (resolved != null) {
            for (IvyNode node : resolved) {
                ret.add(node.getRealNode());
            }
        }
        return ret;
    }

    public Collection<ModuleRevisionId> getEvictedRevisions(ModuleId mid, String rootModuleConf) {
        Collection<ModuleRevisionId> evicted = evictedRevs
                .get(new ModuleIdConf(mid, rootModuleConf));
        if (evicted == null) {
            return new HashSet<ModuleRevisionId>();
        } else {
            return new HashSet<ModuleRevisionId>(evicted);
        }
    }

    public void setEvictedNodes(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> evicted) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        evictedDeps.put(moduleIdConf, new HashSet<IvyNode>(evicted));
        Collection<ModuleRevisionId> evictedRevs = new HashSet<ModuleRevisionId>();
        for (IvyNode node : evicted) {
            evictedRevs.add(node.getId());
            evictedRevs.add(node.getResolvedId());
        }
        this.evictedRevs.put(moduleIdConf, evictedRevs);
    }

    public boolean isEvicted(String rootModuleConf) {
        cleanEvicted();
        if (node.isRoot()) {
            return false;
        }
        EvictionData evictedData = getEvictedData(rootModuleConf);
        if (evictedData == null) {
            return false;
        }
        IvyNode root = node.getRoot();
        ModuleId moduleId = node.getId().getModuleId();
        Collection<ModuleRevisionId> resolvedRevisions = root.getResolvedRevisions(moduleId,
            rootModuleConf);
        return !resolvedRevisions.contains(node.getResolvedId())
                || evictedData.isTransitivelyEvicted();
    }

    public boolean isCompletelyEvicted() {
        cleanEvicted();
        if (node.isRoot()) {
            return false;
        }
        String[] rootModuleConfigurations = node.getRootModuleConfigurations();
        for (int i = 0; i < rootModuleConfigurations.length; i++) {
            if (!isEvicted(rootModuleConfigurations[i])) {
                return false;
            }
        }
        return true;
    }

    private void cleanEvicted() {
        // check if it was evicted by a node that we are now the real node for
        for (Iterator<String> iter = evicted.keySet().iterator(); iter.hasNext();) {
            String rootModuleConf = iter.next();
            EvictionData ed = evicted.get(rootModuleConf);
            Collection<IvyNode> sel = ed.getSelected();
            if (sel != null) {
                for (IvyNode n : sel) {
                    if (n.getRealNode().equals(node)) {
                        // yes, we are the real node for a selected one !
                        // we are no more evicted in this conf !
                        iter.remove();
                    }
                }
            }
        }
    }

    public void markEvicted(EvictionData evictionData) {
        evicted.put(evictionData.getRootModuleConf(), evictionData);
    }

    public EvictionData getEvictedData(String rootModuleConf) {
        cleanEvicted();
        return evicted.get(rootModuleConf);
    }

    public String[] getEvictedConfs() {
        cleanEvicted();
        return evicted.keySet().toArray(new String[evicted.keySet().size()]);
    }

    /**
     * Returns null if this node has only be evicted transitively, or the the collection of selected
     * nodes if it has been evicted by other selected nodes
     * 
     * @return
     */
    public Collection<IvyNode> getAllEvictingNodes() {
        Collection<IvyNode> allEvictingNodes = null;
        for (EvictionData ed : evicted.values()) {
            Collection<IvyNode> selected = ed.getSelected();
            if (selected != null) {
                if (allEvictingNodes == null) {
                    allEvictingNodes = new HashSet<IvyNode>();
                }
                allEvictingNodes.addAll(selected);
            }
        }
        return allEvictingNodes;
    }

    public Collection<String> getAllEvictingNodesDetails() {
        Collection<String> ret = null;
        for (EvictionData ed : evicted.values()) {
            Collection<IvyNode> selected = ed.getSelected();
            if (selected != null) {
                if (ret == null) {
                    ret = new HashSet<String>();
                }
                if (selected.size() == 1) {
                    ret.add(selected.iterator().next()
                            + (ed.getDetail() == null ? "" : " " + ed.getDetail()));
                } else if (selected.size() > 1) {
                    ret.add(selected + (ed.getDetail() == null ? "" : " " + ed.getDetail()));
                }
            }
        }
        return ret;
    }

    public Collection<ConflictManager> getAllEvictingConflictManagers() {
        Collection<ConflictManager> ret = new HashSet<ConflictManager>();
        for (EvictionData ed : evicted.values()) {
            ret.add(ed.getConflictManager());
        }
        return ret;
    }

    /**
     * Returns the eviction data for this node if it has been previously evicted in the root, null
     * otherwise (if it hasn't been evicted in root) for the given rootModuleConf. Note that this
     * method only works if conflict resolution has already be done in all the ancestors.
     * 
     * @param rootModuleConf
     * @param ancestor
     * @return
     */
    public EvictionData getEvictionDataInRoot(String rootModuleConf, IvyNode ancestor) {
        Collection<IvyNode> selectedNodes = node.getRoot().getResolvedNodes(node.getModuleId(),
            rootModuleConf);
        for (IvyNode node : selectedNodes) {
            if (node.getResolvedId().equals(this.node.getResolvedId())) {
                // the node is part of the selected ones for the root: no eviction data to return
                return null;
            }
        }
        // we didn't find this mrid in the selected ones for the root: it has been previously
        // evicted
        return new EvictionData(rootModuleConf, ancestor, node.getRoot().getConflictManager(
            node.getModuleId()), selectedNodes);
    }

    public Collection<IvyNode> getPendingConflicts(String rootModuleConf, ModuleId mid) {
        Collection<IvyNode> resolved = pendingConflicts.get(new ModuleIdConf(mid, rootModuleConf));
        Set<IvyNode> ret = new HashSet<IvyNode>();
        if (resolved != null) {
            for (IvyNode node : resolved) {
                ret.add(node.getRealNode());
            }
        }
        return ret;
    }

    public void setPendingConflicts(ModuleId moduleId, String rootModuleConf,
            Collection<IvyNode> conflicts) {
        ModuleIdConf moduleIdConf = new ModuleIdConf(moduleId, rootModuleConf);
        pendingConflicts.put(moduleIdConf, new HashSet<IvyNode>(conflicts));
    }

}
