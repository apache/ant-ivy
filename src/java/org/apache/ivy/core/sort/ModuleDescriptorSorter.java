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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

/**
 * Inner helper class for sorting ModuleDescriptors.<br>
 * ModuleDescriptorSorter use CollectionOfModulesToSort to find the dependencies of the modules, and
 * use ModuleInSort to store some temporary values attached to the modules to sort.
 * 
 * @see ModuleInSort
 * @see CollectionOfModulesToSort
 */
public class ModuleDescriptorSorter {

    private final CollectionOfModulesToSort moduleDescriptors;

    private final List<ModuleDescriptor> sorted = new LinkedList<ModuleDescriptor>();

    private final CircularDependencyStrategy circularDepStrategy;

    public ModuleDescriptorSorter(Collection<ModuleDescriptor> modulesDescriptorsToSort,
            VersionMatcher matcher, NonMatchingVersionReporter nonMatchingVersionReporter,
            CircularDependencyStrategy circularDepStrategy) {
        this.circularDepStrategy = circularDepStrategy;
        moduleDescriptors = new CollectionOfModulesToSort(modulesDescriptorsToSort, matcher,
                nonMatchingVersionReporter);
    }

    /**
     * Iterates over all modules calling sortModuleDescriptorsHelp.
     * 
     * @return sorted module
     * @throws CircularDependencyException
     */
    public List<ModuleDescriptor> sortModuleDescriptors() throws CircularDependencyException {
        Message.debug("Nbr of module to sort : " + moduleDescriptors.size());
        for (ModuleInSort m : moduleDescriptors) {
            sortModuleDescriptorsHelp(m, m);
        }
        return sorted;
    }

    /**
     * If current module has already been added to list, returns, Otherwise invokes
     * sortModuleDescriptorsHelp for all dependencies contained within set of moduleDescriptors.
     * Then finally adds self to list of sorted.<br/>
     * When a loop is detected by a recursive call, the moduleDescriptors are not added immediately
     * added to the sorted list. They are added as loop dependencies of the root, and will be added
     * to the sorted list only when the root itself will be added.
     * 
     * @param current
     *            Current module to add to sorted list.
     * @throws CircularDependencyException
     */
    private void sortModuleDescriptorsHelp(ModuleInSort current, ModuleInSort caller)
            throws CircularDependencyException {
        // if already sorted return
        if (current.isProcessed()) {
            return;
        }
        if (current.checkLoop(caller, circularDepStrategy)) {
            return;
        }
        DependencyDescriptor[] descriptors = current.getDependencies();
        Message.debug("Sort dependencies of : " + current.toString()
                + " / Number of dependencies = " + descriptors.length);
        current.setCaller(caller);
        for (int i = 0; i < descriptors.length; i++) {
            ModuleInSort child = moduleDescriptors.getModuleDescriptorDependency(descriptors[i]);
            if (child != null) {
                sortModuleDescriptorsHelp(child, current);
            }
        }
        current.endOfCall();
        Message.debug("Sort done for : " + current.toString());
        current.addToSortedListIfRequired(sorted);
    }

}
