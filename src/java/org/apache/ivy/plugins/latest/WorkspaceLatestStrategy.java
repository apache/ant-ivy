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
package org.apache.ivy.plugins.latest;

import java.util.ArrayList;
import java.util.List;

/**
 * A strategy which delegate to another strategy, unless for the latest and working revisions which
 * are considered as superior to any other revision. < br/>
 * NB : it is for internal usage of Ivy only!
 */
public class WorkspaceLatestStrategy extends AbstractLatestStrategy {

    private LatestStrategy delegate;

    public WorkspaceLatestStrategy(LatestStrategy delegate) {
        this.delegate = delegate;
        setName("workspace-" + delegate.getName());
    }

    public List<ArtifactInfo> sort(ArtifactInfo[] infos) {
        List<ArtifactInfo> sorted = delegate.sort(infos);

        List<ArtifactInfo> head = new ArrayList<ArtifactInfo>();
        List<ArtifactInfo> tail = new ArrayList<ArtifactInfo>();

        for (ArtifactInfo ai : sorted) {
            String rev = ai.getRevision();
            boolean latestRev = rev.startsWith("latest") || rev.startsWith("working");
            if (latestRev) {
                head.add(ai);
            } else {
                tail.add(ai);
            }
        }

        head.addAll(tail);
        return head;
    }

}