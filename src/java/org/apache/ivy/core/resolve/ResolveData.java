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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.util.Message;

public class ResolveData {
    private ResolveEngine engine;

    // shared map of all visit data
    private Map<ModuleRevisionId, VisitData> visitData;

    private ConfigurationResolveReport report;

    private ResolveOptions options;

    private VisitNode currentVisitNode = null;

    private ResolvedModuleRevision currentResolvedModuleRevision;

    public ResolveData(ResolveData data, boolean validate) {
        this(data.engine, new ResolveOptions(data.options).setValidate(validate), data.report,
                data.visitData);
        setCurrentVisitNode(data.currentVisitNode);
        setCurrentResolvedModuleRevision(data.currentResolvedModuleRevision);
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options) {
        this(engine, options, null, new LinkedHashMap<ModuleRevisionId, VisitData>());
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options,
            ConfigurationResolveReport report) {
        this(engine, options, report, new LinkedHashMap<ModuleRevisionId, VisitData>());
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options,
            ConfigurationResolveReport report, Map<ModuleRevisionId, VisitData> visitData) {
        this.engine = engine;
        this.report = report;
        this.visitData = visitData;
        this.options = options;
    }

    public ConfigurationResolveReport getReport() {
        return report;
    }

    public IvyNode getNode(ModuleRevisionId mrid) {
        VisitData visitData = getVisitData(mrid);
        return visitData == null ? null : visitData.getNode();
    }

    public Collection<IvyNode> getNodes() {
        Collection<IvyNode> nodes = new ArrayList<IvyNode>();
        for (VisitData vdata : visitData.values()) {
            nodes.add(vdata.getNode());
        }
        return nodes;
    }

    public Collection<ModuleRevisionId> getNodeIds() {
        return visitData.keySet();
    }

    public VisitData getVisitData(ModuleRevisionId mrid) {
        VisitData result = visitData.get(mrid);

        if (result == null) {
            // search again, now ignore the missing extra attributes
            for (Entry<ModuleRevisionId, VisitData> entry : visitData.entrySet()) {
                ModuleRevisionId current = entry.getKey();

                if (isSubMap(mrid.getAttributes(), current.getAttributes())) {
                    result = entry.getValue();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Checks whether one map is a sub-map of the other.
     */
    private static <K, V> boolean isSubMap(Map<K, V> map1, Map<K, V> map2) {
        int map1Size = map1.size();
        int map2Size = map2.size();

        if (map1Size == map2Size) {
            return map1.equals(map2);
        }

        Map<K, V> smallest = map1Size < map2Size ? map1 : map2;
        Map<K, V> largest = map1Size < map2Size ? map2 : map1;

        for (Entry<K, V> entry : smallest.entrySet()) {

            if (!largest.containsKey(entry.getKey())) {
                return false;
            }

            Object map1Value = smallest.get(entry.getKey());
            Object map2Value = largest.get(entry.getKey());
            if (!isEqual(map1Value, map2Value)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }

        if (obj1 == null) {
            return obj2 == null;
        }

        if (obj2 == null) {
            return obj1 == null;
        }

        return obj1.equals(obj2);
    }

    /**
     * Returns the VisitNode currently visited, or <code>null</code> if there is no node currently
     * visited in this context.
     * 
     * @return the VisitNode currently visited
     */
    public VisitNode getCurrentVisitNode() {
        return currentVisitNode;
    }

    /**
     * Sets the currently visited node. WARNING: This should only be called by Ivy core
     * ResolveEngine!
     * 
     * @param currentVisitNode
     */
    void setCurrentVisitNode(VisitNode currentVisitNode) {
        this.currentVisitNode = currentVisitNode;
    }

    public void register(VisitNode node) {
        register(node.getId(), node);
    }

    public void register(ModuleRevisionId mrid, VisitNode node) {
        VisitData visitData = getVisitData(mrid);
        if (visitData == null) {
            visitData = new VisitData(node.getNode());
            visitData.addVisitNode(node);
            this.visitData.put(mrid, visitData);
        } else {
            visitData.setNode(node.getNode());
            visitData.addVisitNode(node);
        }
    }

    /**
     * Updates the visit data currently associated with the given mrid with the given node and the
     * visit nodes of the old visitData for the given rootModuleConf
     * 
     * @param mrid
     *            the module revision id for which the update should be done
     * @param node
     *            the IvyNode to associate with the visit data to update
     * @param rootModuleConf
     *            the root module configuration in which the update is made
     */
    void replaceNode(ModuleRevisionId mrid, IvyNode node, String rootModuleConf) {
        VisitData visitData = getVisitData(mrid);
        if (visitData == null) {
            throw new IllegalArgumentException("impossible to replace node for id " + mrid
                    + ". No registered node found.");
        }
        VisitData keptVisitData = getVisitData(node.getId());
        if (keptVisitData == null) {
            throw new IllegalArgumentException("impossible to replace node with " + node
                    + ". No registered node found for " + node.getId() + ".");
        }
        // replace visit data in Map (discards old one)
        this.visitData.put(mrid, keptVisitData);
        // update visit data with discarde visit nodes
        keptVisitData.addVisitNodes(rootModuleConf, visitData.getVisitNodes(rootModuleConf));

        report.updateDependency(mrid, node);
    }

    public void setReport(ConfigurationResolveReport report) {
        this.report = report;
    }

    public Date getDate() {
        return options.getDate();
    }

    public boolean isValidate() {
        return options.isValidate();
    }

    public boolean isTransitive() {
        return options.isTransitive();
    }

    public ResolveOptions getOptions() {
        return options;
    }

    public ResolveEngineSettings getSettings() {
        return engine.getSettings();
    }

    public EventManager getEventManager() {
        return engine.getEventManager();
    }

    public ResolveEngine getEngine() {
        return engine;
    }

    void blacklist(IvyNode node) {
        for (Iterator<Entry<ModuleRevisionId, VisitData>> iter = visitData.entrySet().iterator(); iter
                .hasNext();) {
            Entry<ModuleRevisionId, VisitData> entry = iter.next();
            VisitData vdata = entry.getValue();
            if (vdata.getNode() == node && !node.getResolvedId().equals(entry.getKey())) {
                // this visit data was associated with the blacklisted node,
                // we discard this association
                iter.remove();
            }
        }
    }

    public boolean isBlacklisted(String rootModuleConf, ModuleRevisionId mrid) {
        IvyNode node = getNode(mrid);

        // if (node == null) {
        // // search again, now ignore the extra attributes
        // // TODO: maybe we should search the node that has at least the
        // // same attributes as mrid
        // for (Iterator it = visitData.entrySet().iterator(); it.hasNext();) {
        // Map.Entry entry = (Entry) it.next();
        // ModuleRevisionId current = (ModuleRevisionId) entry.getKey();
        // if (current.getModuleId().equals(mrid.getModuleId())
        // && current.getRevision().equals(mrid.getRevision())) {
        // VisitData data = (VisitData) entry.getValue();
        // node = data.getNode();
        // break;
        // }
        // }
        // }
        //
        return node != null && node.isBlacklisted(rootModuleConf);
    }

    public DependencyDescriptor mediate(DependencyDescriptor dd) {
        DependencyDescriptor originalDD = dd;
        dd = getEngine().mediate(dd, getOptions());

        VisitNode current = getCurrentVisitNode();
        if (current != null) {
            // mediating dd through dependers stack
            List<VisitNode> dependers = new ArrayList<VisitNode>(current.getPath());
            // the returned path contains the currently visited node, we are only interested in
            // the dependers, so we remove the currently visted node from the end
            dependers.remove(dependers.size() - 1);
            // we want to apply mediation going up in the dependers stack, not the opposite
            Collections.reverse(dependers);
            for (VisitNode n : dependers) {
                ModuleDescriptor md = n.getDescriptor();
                if (md != null) {
                    dd = md.mediate(dd);
                }
            }
        }

        if (originalDD != dd) {
            Message.verbose("dependency descriptor has been mediated: " + originalDD + " => " + dd);
        }

        return dd;
    }

    /**
     * Sets the last {@link ResolvedModuleRevision} which has been currently resolved.
     * <p>
     * This can be used especially in dependency resolvers, to know if another dependency resolver
     * has already resolved the requested dependency, to take a decision if the resolver should try
     * to resolve it by itself or not. Indeed, the dependency resolver is responsible for taking
     * this decision, even when included in a chain. The chain responsibility is only to set this
     * current resolved module revision to enable the resolver to take the decision.
     * </p>
     * 
     * @param mr
     *            the last {@link ResolvedModuleRevision} which has been currently resolved.
     */
    public void setCurrentResolvedModuleRevision(ResolvedModuleRevision mr) {
        this.currentResolvedModuleRevision = mr;
    }

    /**
     * Returns the last {@link ResolvedModuleRevision} which has been currently resolved.
     * <p>
     * It can be <code>null</code>.
     * </p>
     * 
     * @return the last {@link ResolvedModuleRevision} which has been currently resolved.
     */
    public ResolvedModuleRevision getCurrentResolvedModuleRevision() {
        return currentResolvedModuleRevision;
    }
}
