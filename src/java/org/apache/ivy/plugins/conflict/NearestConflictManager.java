/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.conflict;

import java.util.*;

import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;

public class NearestConflictManager extends AbstractConflictManager {

    @Override
    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        if (conflicts.size() < 2) {
            return conflicts;
        }

        // list containing the direct children of the parent
        List<IvyNode> children = new ArrayList<>(conflicts.size());

        for (IvyNode node : conflicts) {
            DependencyDescriptor dd = node.getDependencyDescriptor(parent);
            if (dd != null && parent.getResolvedId().equals(dd.getParentRevisionId())) {
                if (dd.isForce()) {
                    return Collections.singleton(node);
                }
                children.add(node);
            }
        }

        if (!children.isEmpty()) {
            return children;
        }

        int distance = 0;
        IvyNode node = null;

        for (IvyNode child : conflicts) {
            if (node == null) {
                node = child;
                // the first child has path through VisitNode
                distance = node.getData().getCurrentVisitNode().getPath().size() - 2;
            } else {
                for (String conf : parent.getRequiredConfigurations()) {
                    int hops = calculateDistance(parent, conf, child);
                    if (hops <= distance) {
                        distance = hops;
                        node = child; // nearer or sooner
                    }
                }
            }
        }

        return Collections.singletonList(node);
    }

    private int calculateDistance(IvyNode parent, String conf, IvyNode child) {
        int min = Short.MAX_VALUE;

        for (IvyNodeCallers.Caller caller : child.getCallers(conf)) {
            if (caller.getModuleRevisionId().equals(parent.getResolvedId())) {
                return 0;
            }

            int distance = 1 + calculateDistance(parent, conf, child.findNode(caller.getModuleRevisionId()));
            if (distance < min) {
                min = distance;
            }
        }

        return min;
    }
}
