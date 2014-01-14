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

/**
 * Information about a blacklisted module, providing context information in which it has been
 * blacklisted
 */
public class IvyNodeBlacklist {
    private IvyNode conflictParent;

    private IvyNode selectedNode;

    private IvyNode evictedNode;

    private IvyNode blacklistedNode;

    private String rootModuleConf;

    public IvyNodeBlacklist(IvyNode conflictParent, IvyNode selectedNode, IvyNode evictedNode,
            IvyNode blacklistedNode, String rootModuleConf) {
        this.conflictParent = conflictParent;
        this.selectedNode = selectedNode;
        this.evictedNode = evictedNode;
        this.blacklistedNode = blacklistedNode;
        this.rootModuleConf = rootModuleConf;
    }

    public IvyNode getConflictParent() {
        return conflictParent;
    }

    public IvyNode getSelectedNode() {
        return selectedNode;
    }

    public IvyNode getEvictedNode() {
        return evictedNode;
    }

    public IvyNode getBlacklistedNode() {
        return blacklistedNode;
    }

    public String getRootModuleConf() {
        return rootModuleConf;
    }

    public String toString() {
        return "[" + blacklistedNode + " blacklisted to evict " + evictedNode + " in favor of "
                + selectedNode + " in " + conflictParent + " for " + rootModuleConf + "]";
    }
}
