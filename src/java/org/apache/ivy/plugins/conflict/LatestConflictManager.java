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
import java.util.List;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.util.Message;

public class LatestConflictManager extends AbstractConflictManager {
    public static class NoConflictResolvedYetException extends RuntimeException {
    }

    protected static final class IvyNodeArtifactInfo implements ArtifactInfo {
        private final IvyNode node;

        private IvyNodeArtifactInfo(IvyNode dep) {
            node = dep;
        }

        public long getLastModified() {
            long lastModified = node.getLastModified();
            if (lastModified == 0) {
                // if the last modified timestamp is unknown, we can't resolve
                // the conflicts now, and trigger an exception which will be catched
                // in the main resolveConflicts method
                throw new NoConflictResolvedYetException();
            } else {
                return lastModified;
            }
        }

        public String getRevision() {
            return node.getResolvedId().getRevision();
        }

        public IvyNode getNode() {
            return node;
        }
    }

    private LatestStrategy strategy;

    private String strategyName;

    public LatestConflictManager() {
    }

    public LatestConflictManager(LatestStrategy strategy) {
        this.strategy = strategy;
    }

    public LatestConflictManager(String name, LatestStrategy strategy) {
        setName(name);
        this.strategy = strategy;
    }

    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        if (conflicts.size() < 2) {
            return conflicts;
        }
        for (IvyNode node : conflicts) {
            DependencyDescriptor dd = node.getDependencyDescriptor(parent);
            if (dd != null && dd.isForce()
                    && parent.getResolvedId().equals(dd.getParentRevisionId())) {
                return Collections.singleton(node);
            }
        }

        /*
         * If the list of conflicts contains dynamic revisions, delay the conflict calculation until
         * they are resolved. TODO: we probably could already evict some of the dynamic revisions!
         */
        for (IvyNode node : conflicts) {
            ModuleRevisionId modRev = node.getResolvedId();
            if (getSettings().getVersionMatcher().isDynamic(modRev)) {
                return null;
            }
        }

        ArrayList<IvyNode> unevicted = new ArrayList<IvyNode>();
        for (IvyNode node : conflicts) {
            if (!node.isCompletelyEvicted())
                unevicted.add(node);
        }
        if (unevicted.size() > 0) {
            conflicts = unevicted;
        }

        try {
            IvyNodeArtifactInfo latest = (IvyNodeArtifactInfo) getStrategy().findLatest(
                toArtifactInfo(conflicts), null);
            if (latest != null) {
                return Collections.singleton(latest.getNode());
            } else {
                return conflicts;
            }
        } catch (NoConflictResolvedYetException ex) {
            // we have not enough informations in the nodes to resolve conflict
            // according to the resolveConflicts contract, we must return null
            return null;
        }
    }

    protected ArtifactInfo[] toArtifactInfo(Collection<IvyNode> conflicts) {
        List<ArtifactInfo> artifacts = new ArrayList<ArtifactInfo>(conflicts.size());
        for (IvyNode node : conflicts) {
            artifacts.add(new IvyNodeArtifactInfo(node));
        }
        return artifacts.toArray(new ArtifactInfo[artifacts.size()]);
    }

    public LatestStrategy getStrategy() {
        if (strategy == null) {
            if (strategyName != null) {
                strategy = getSettings().getLatestStrategy(strategyName);
                if (strategy == null) {
                    Message.error("unknown latest strategy: " + strategyName);
                    strategy = getSettings().getDefaultLatestStrategy();
                }
            } else {
                strategy = getSettings().getDefaultLatestStrategy();
            }
        }
        return strategy;
    }

    /**
     * To conform to configurator API
     * 
     * @param latestStrategy
     */
    public void setLatest(String strategyName) {
        this.strategyName = strategyName;
    }

    public void setStrategy(LatestStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public String toString() {
        return strategy != null ? String.valueOf(strategy) : strategyName;
    }
}
