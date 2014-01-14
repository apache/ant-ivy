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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.plugins.version.VersionMatcher;

/**
 * Wrap a collection of descriptores wrapped themself in ModuleInSort elements. It contains some
 * dedicated function to retrieve module descriptors based on dependencies descriptors.<br>
 * <i>This class is designed to be used internally by the ModuleDescriptorSorter.</i>
 */
class CollectionOfModulesToSort implements Iterable<ModuleInSort> {

    private final List<ModuleInSort> moduleDescriptors;

    private final VersionMatcher versionMatcher;

    private final Map<ModuleId, Collection<ModuleInSort>> modulesByModuleId;

    private final NonMatchingVersionReporter nonMatchingVersionReporter;

    /**
     * @param modulesToSort
     *            The collection of ModuleDescriptor to sort
     * @param matcher
     *            The matcher to used to check if dependencyDescriptor match a module in this
     *            collection
     * @param nonMatchingVersionReporter
     */
    public CollectionOfModulesToSort(Collection<ModuleDescriptor> modulesToSort,
            VersionMatcher matcher, NonMatchingVersionReporter nonMatchingVersionReporter) {
        this.versionMatcher = matcher;
        this.nonMatchingVersionReporter = nonMatchingVersionReporter;
        this.modulesByModuleId = new HashMap<ModuleId, Collection<ModuleInSort>>();
        moduleDescriptors = new ArrayList<ModuleInSort>(modulesToSort.size());
        for (ModuleDescriptor md : modulesToSort) {
            ModuleInSort mdInSort = new ModuleInSort(md);
            moduleDescriptors.add(mdInSort);
            addToModulesByModuleId(md, mdInSort);
        }
    }

    private void addToModulesByModuleId(ModuleDescriptor md, ModuleInSort mdInSort) {
        ModuleId mdId = md.getModuleRevisionId().getModuleId();
        List<ModuleInSort> mdInSortAsList = new LinkedList<ModuleInSort>();
        mdInSortAsList.add(mdInSort);
        Collection<ModuleInSort> previousList = modulesByModuleId.put(mdId, mdInSortAsList);
        if (previousList != null) {
            mdInSortAsList.addAll(previousList);
        }
    }

    public Iterator<ModuleInSort> iterator() {
        return moduleDescriptors.iterator();
    }

    public int size() {
        return moduleDescriptors.size();
    }

    /**
     * Find a matching module descriptor in the list of module to sort.
     * 
     * @param descriptor
     * @return a ModuleDescriptor from the collection of module descriptors to sort. If none exists
     *         returns null.
     */
    public ModuleInSort getModuleDescriptorDependency(DependencyDescriptor descriptor) {
        Collection<ModuleInSort> modulesOfSameId = modulesByModuleId.get(descriptor
                .getDependencyId());
        if (modulesOfSameId == null) {
            return null;
        }
        for (ModuleInSort mdInSort : modulesOfSameId) {
            if (mdInSort.match(descriptor, versionMatcher)) {
                return mdInSort;
            } else {
                nonMatchingVersionReporter.reportNonMatchingVersion(descriptor,
                    mdInSort.getSortedModuleDescriptor());
            }
        }
        return null;
    }

}
