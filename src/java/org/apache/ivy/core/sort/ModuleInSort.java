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

import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.circular.CircularDependencyHelper;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

/**
 * Decorates a ModuleDescriptor with some attributes used during the sort. Thus every instance of a
 * ModuleInSort can be used in only one ModuleDescriptorSorter at a time.<br>
 * The added fields are : <br>
 * <ul>
 * <li><code>isSorted</code> : is true iff this module has already been added to the sorted list.</li>
 * <li><code>loopElements</code> : When the module is the root of a loop (=the first element of a
 * loop met during the sort), <code>loopElements</code> contains all ModuleInSort of the loop
 * (excluding the root itself.</li>
 * <li><code>isLoopIntermediateElement</code> : When a loop is detected, all modules included in the
 * loop (except the root) have <code>isLoopIntermediateElement</code> set to true.</li>
 * <li><code>caller</code> : During the sort, we traverse recursively the graph. When doing that,
 * caller point to the parent element.
 */
class ModuleInSort {

    private final ModuleDescriptor module;

    private boolean isSorted = false;

    private List<ModuleInSort> loopElements = new LinkedList<ModuleInSort>();

    private boolean isLoopIntermediateElement = false;

    private ModuleInSort caller;

    public ModuleInSort(ModuleDescriptor moduleToSort) {
        module = moduleToSort;
    }

    public boolean isInLoop() {
        return isLoopIntermediateElement;
    }

    /** This ModuleInSort has been placed on the sorted list */
    public boolean isSorted() {
        if (isSorted) {
            Message.debug("Module descriptor already sorted : "
                    + module.getModuleRevisionId().toString());
            return true;
        } else {
            return false;
        }
    }

    /**
     * This ModuleInSort has already been analyzed. It is either already added to the sorted list,
     * either it is included in a loop and will be added when the root of the loop will be added to
     * the list.
     */
    public boolean isProcessed() {
        if (isSorted || isLoopIntermediateElement) {
            Message.debug("Module descriptor is processed : "
                    + module.getModuleRevisionId().toString());
            return true;
        } else {
            return false;
        }
    }

    public void setCaller(ModuleInSort caller) {
        this.caller = caller;
    }

    public void endOfCall() {
        caller = null;
    }

    /**
     * Check if a adding this element as a dependency of caller will introduce a circular
     * dependency. If it is, all the elements of the loop are flaged as 'loopIntermediateElement',
     * and the loopElements of this module (which is the root of the loop) is updated. The
     * depStrategy is invoked on order to report a correct circular loop message.
     * 
     * @param futurCaller
     * @param depStrategy
     * @return true if a loop is detected.
     */
    public boolean checkLoop(ModuleInSort futurCaller, CircularDependencyStrategy depStrategy) {
        if (caller != null) {
            LinkedList<ModuleRevisionId> elemOfLoop = new LinkedList<ModuleRevisionId>();
            elemOfLoop.add(this.module.getModuleRevisionId());
            for (ModuleInSort stackEl = futurCaller; stackEl != this; stackEl = stackEl.caller) {
                elemOfLoop.add(stackEl.module.getModuleRevisionId());
                stackEl.isLoopIntermediateElement = true;
                loopElements.add(stackEl);
            }
            elemOfLoop.add(this.module.getModuleRevisionId());
            ModuleRevisionId[] mrids = elemOfLoop.toArray(new ModuleRevisionId[elemOfLoop.size()]);
            depStrategy.handleCircularDependency(mrids);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add this module to the sorted list except if this module is an intermediary element of a
     * loop. If this module is the 'root' of a loop, then all elements of that loops are added
     * before.
     * 
     * @param sorted
     *            The list of sorted elements on which this module will be added
     */
    public void addToSortedListIfRequired(List<ModuleDescriptor> sorted) {
        if (!isLoopIntermediateElement) {
            addToSortList(sorted);
        }
    }

    /**
     * Add this module to the sorted list. If current is the 'root' of a loop, then all elements of
     * that loops are added before.
     */
    private void addToSortList(List<ModuleDescriptor> sortedList) {
        for (ModuleInSort moduleInLoop : loopElements) {
            moduleInLoop.addToSortList(sortedList);
        }
        if (!this.isSorted()) {
            sortedList.add(module);
            this.isSorted = true;
        }
    }

    public String toString() {
        return module.getModuleRevisionId().toString();
    }

    public DependencyDescriptor[] getDependencies() {
        return module.getDependencies();
    }

    /** Log a warning saying that a loop is detected */
    public static void logLoopWarning(List loopElement) {
        Message.warn("circular dependency detected during sort: "
                + CircularDependencyHelper.formatMessageFromDescriptors(loopElement));
    }

    /**
     * Return true if this module match the DependencyDescriptor with the given versionMatcher. If
     * this module has no version defined, then true is always returned.
     */
    public boolean match(DependencyDescriptor descriptor, VersionMatcher versionMatcher) {
        ModuleDescriptor md = module;
        return md.getResolvedModuleRevisionId().getRevision() == null
                || md.getResolvedModuleRevisionId().getRevision().equals(Ivy.getWorkingRevision())
                || versionMatcher.accept(descriptor.getDependencyRevisionId(), md);
        // Checking md.getResolvedModuleRevisionId().getRevision().equals(Ivy.getWorkingRevision()
        // allow to consider any local non resolved ivy.xml
        // as a valid module.
    }

    public ModuleDescriptor getSortedModuleDescriptor() {
        return module;
    }
}
