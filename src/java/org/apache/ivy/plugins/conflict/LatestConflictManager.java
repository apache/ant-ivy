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
package org.apache.ivy.plugins.conflict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.util.Message;


public class LatestConflictManager extends AbstractConflictManager {
    private static class IvyNodeArtifactInfo implements ArtifactInfo {
        private final IvyNode _node;

        private IvyNodeArtifactInfo(IvyNode dep) {
            _node = dep;
        }

        public long getLastModified() {
            return _node.getPublication();
        }

        public String getRevision() {
            return _node.getId().getRevision();
        }
        
        public IvyNode getNode() {
            return _node;
        }
    }



    private LatestStrategy _strategy;
    private String _strategyName;

    public LatestConflictManager() {
    }

    public LatestConflictManager(LatestStrategy strategy) {
        _strategy = strategy;
    }

    public LatestConflictManager(String name, LatestStrategy strategy) {
        setName(name);
        _strategy = strategy;
    }

    public Collection resolveConflicts(IvyNode parent, Collection conflicts) {
        if (conflicts.size() < 2) {
            return conflicts;
        }
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            DependencyDescriptor dd = node.getDependencyDescriptor(parent);
            if (dd != null && dd.isForce() && parent.getResolvedId().equals(dd.getParentRevisionId())) {
                return Collections.singleton(node);
            }
        }
        ArtifactInfo latest = getStrategy().findLatest(toArtifactInfo(conflicts), null);
        if (latest != null) {
            return Collections.singleton(((IvyNodeArtifactInfo)latest).getNode());
        } else {
            return conflicts;
        }
    }

    private ArtifactInfo[] toArtifactInfo(Collection conflicts) {
        List artifacts = new ArrayList(conflicts.size());
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            artifacts.add(new IvyNodeArtifactInfo(node));
        }
        return (ArtifactInfo[])artifacts.toArray(new ArtifactInfo[artifacts.size()]);
    }

    public LatestStrategy getStrategy() {
        if (_strategy == null) {
            if (_strategyName != null) {
                _strategy = getIvy().getLatestStrategy(_strategyName);
                if (_strategy == null) {
                    Message.error("unknown latest strategy: "+_strategyName);
                    _strategy = getIvy().getDefaultLatestStrategy();
                }
            } else {
                _strategy = getIvy().getDefaultLatestStrategy();
            }
        }
        return _strategy;
    }
    

    /**
     * To conform to configurator API
     * @param latestStrategy
     */
    public void setLatest(String strategyName) {
        _strategyName = strategyName;
    }
    
    public void setStrategy(LatestStrategy strategy) {
        _strategy = strategy;
    }
    

    public String toString() {
        return String.valueOf(_strategy);
    }
}
