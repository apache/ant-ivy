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

import java.util.Collection;
import java.util.Collections;

import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.version.VersionMatcher;

public class StrictConflictManager extends AbstractConflictManager {

    public StrictConflictManager() {
    }

    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        IvyNode lastNode = null;
        for (IvyNode node : conflicts) {
            if (versionMatcher.isDynamic(node.getResolvedId())) {
                // dynamic revision, not enough information to resolve conflict
                return null;
            }

            if (lastNode != null && !lastNode.equals(node)) {
                throw new StrictConflictException(lastNode, node);
            }
            lastNode = node;
        }

        return Collections.singleton(lastNode);
    }

}
