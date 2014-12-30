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
import java.util.Arrays;
import java.util.Collection;

import org.apache.ivy.core.resolve.IvyNode;

public class FixedConflictManager extends AbstractConflictManager {

    private Collection<String> revisions;

    public FixedConflictManager(String[] revs) {
        revisions = Arrays.asList(revs);
        setName("fixed" + revisions);
    }

    public Collection<IvyNode> resolveConflicts(IvyNode parent, Collection<IvyNode> conflicts) {
        Collection<IvyNode> resolved = new ArrayList<IvyNode>(conflicts.size());
        for (IvyNode node : conflicts) {
            String revision = node.getResolvedId().getRevision();
            if (revisions.contains(revision)) {
                resolved.add(node);
            }
        }
        return resolved;
    }

    public Collection<String> getRevs() {
        return revisions;
    }

}
