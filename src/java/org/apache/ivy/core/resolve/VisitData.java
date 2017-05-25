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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to store data related to one node of the dependency graph visit. It stores
 * both an {@link IvyNode} and related {@link VisitNode} objects. Indeed, during the visit of the
 * graph, the algorithm can visit the same node from several parents, thus requiring several
 * VisitNode.
 */
public class VisitData {
    /**
     * A node in the graph of module dependencies resolution
     */
    private IvyNode node;

    /**
     * The associated visit nodes, per rootModuleConf Note that the value is a List, because a node
     * can be visited from several parents during the resolution process
     */
    private Map<String, List<VisitNode>> visitNodes = new HashMap<String, List<VisitNode>>();

    public VisitData(IvyNode node) {
        this.node = node;
    }

    public void addVisitNode(VisitNode node) {
        String rootModuleConf = node.getRootModuleConf();
        getVisitNodes(rootModuleConf).add(node);
    }

    public List<VisitNode> getVisitNodes(String rootModuleConf) {
        List<VisitNode> visits = visitNodes.get(rootModuleConf);
        if (visits == null) {
            visits = new ArrayList<VisitNode>();
            visitNodes.put(rootModuleConf, visits);
        }
        return visits;
    }

    public IvyNode getNode() {
        return node;
    }

    public void setNode(IvyNode node) {
        this.node = node;
    }

    public void addVisitNodes(String rootModuleConf, List<VisitNode> visitNodes) {
        getVisitNodes(rootModuleConf).addAll(visitNodes);
    }
}
