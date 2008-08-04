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
package org.apache.ivy.core.sort;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;

abstract class MessageBasedNonMatchingVersionReporter implements NonMatchingVersionReporter {

    public void reportNonMatchingVersion(DependencyDescriptor descriptor, ModuleDescriptor md) {
        ModuleRevisionId dependencyRevisionId = descriptor.getDependencyRevisionId();
        ModuleRevisionId parentRevisionId = descriptor.getParentRevisionId();
        if (parentRevisionId == null) {
            // There are some rare case where DependencyDescriptor have no parent.
            // This is should not be used in the SortEngine, but if it is, we
            // show a decent trace.
            reportMessage("Non matching revision detected when sorting.  Dependency "
                    + dependencyRevisionId + " doesn't match " + md.getModuleRevisionId());
        } else {
            ModuleId parentModuleId = parentRevisionId.getModuleId();
            reportMessage("Non matching revision detected when sorting.  " + parentModuleId
                    + " depends on " + dependencyRevisionId + ", doesn't match "
                    + md.getModuleRevisionId());
        }
    }

    protected abstract void reportMessage(String msg);

}
