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

import org.apache.ivy.core.module.id.ModuleRevisionId;

public class SubVersionMatcher extends AbstractVersionMatcher {
    public SubVersionMatcher() {
        super("sub-version");
    }

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        return askedMrid.getRevision().endsWith("+");
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        String prefix = askedMrid.getRevision().substring(0, askedMrid.getRevision().length() - 1);
        return foundMrid.getRevision().startsWith(prefix);
    }

    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid,
            Comparator staticComparator) {
        if (foundMrid.getRevision().startsWith(
            askedMrid.getRevision().substring(0, askedMrid.getRevision().length() - 1))) {
            return 1;
        }
        return staticComparator.compare(askedMrid, foundMrid);
    }
}
