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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;

public class ResolveData {
    private ResolveEngine engine;

    private Map visitData; // shared map of all visit data: Map (ModuleRevisionId -> VisitData)
    
    private ConfigurationResolveReport report;

    private ResolveOptions options;

    public ResolveData(ResolveData data, boolean validate) {
        this(data.engine, new ResolveOptions(data.options).setValidate(validate), data.report,
                data.visitData);
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options) {
        this(engine, options, null, new LinkedHashMap());
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options,
            ConfigurationResolveReport report) {
        this(engine, options, report, new LinkedHashMap());
    }

    public ResolveData(ResolveEngine engine, ResolveOptions options,
            ConfigurationResolveReport report, Map visitData) {
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

    public Collection getNodes() {
        Collection nodes = new ArrayList();
        for (Iterator iter = visitData.values().iterator(); iter.hasNext();) {
            VisitData vdata = (VisitData) iter.next();
            nodes.add(vdata.getNode());
        }
        return nodes;
    }

    public Collection getNodeIds() {
        return visitData.keySet();
    }

    public VisitData getVisitData(ModuleRevisionId mrid) {
        return (VisitData) visitData.get(mrid);
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
        for (Iterator iter = visitData.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            VisitData vdata = (VisitData) entry.getValue();
            if (vdata.getNode() == node && !node.getResolvedId().equals(entry.getKey())) {
                // this visit data was associated with the blacklisted node, 
                // we discard this association
                iter.remove();
            }
        }
    }

    public boolean isBlacklisted(String rootModuleConf, ModuleRevisionId mrid) {
        IvyNode node = getNode(mrid);
        return node != null && node.isBlacklisted(rootModuleConf);
    }

}
