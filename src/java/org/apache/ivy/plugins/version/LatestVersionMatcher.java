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
package org.apache.ivy.plugins.version;

import java.util.Comparator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.Status;
import org.apache.ivy.core.module.status.StatusManager;

public class LatestVersionMatcher extends AbstractVersionMatcher {
    public LatestVersionMatcher() {
        super("latest");
    }

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        return askedMrid.getRevision().startsWith("latest.");
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return true;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        List<Status> statuses = StatusManager.getCurrent().getStatuses();
        Status lowest = (Status) statuses.get(statuses.size() - 1);
        String latestLowest = "latest." + lowest.getName();
        return !latestLowest.equals(askedMrid.getRevision());
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        String askedBranch = askedMrid.getBranch();
        String foundBranch = foundMD.getModuleRevisionId().getBranch();
        boolean sameBranch = (askedBranch == null) ? foundBranch == null
                : askedBranch.equals(foundBranch);
        if (!sameBranch) {
            return false;
        }
        String askedStatus = askedMrid.getRevision().substring("latest.".length());
        return StatusManager.getCurrent().getPriority(askedStatus) >= StatusManager.getCurrent()
                .getPriority(foundMD.getStatus());
    }

    /**
     * If we don't need a module descriptor we can consider the dynamic revision to be greater. If
     * we need a module descriptor then we can't know which one is greater and return 0.
     */
    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid,
            Comparator staticComparator) {
        return needModuleDescriptor(askedMrid, foundMrid) ? 0 : 1;
    }
}
