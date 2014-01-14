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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.circular.IgnoreCircularDependencyStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Checks;

public class SortEngine {

    private SortEngineSettings settings;

    public SortEngine(SortEngineSettings settings) {
        if (settings == null) {
            throw new NullPointerException("SortEngine.settings can not be null");
        }
        this.settings = settings;
    }

    /**
     * Same as {@link #sortModuleDescriptors(Collection, SortOptions)} but for <code>IvyNode</code>
     * s.
     * 
     * @param nodes
     *            a Collection of nodes to sort
     * @param options
     *            Options to use to sort the nodes.
     * @return a List of sorted IvyNode
     * @throws CircularDependencyException
     *             if a circular dependency exists and circular dependency strategy decide to throw
     *             an exception
     */
    public List<IvyNode> sortNodes(Collection<IvyNode> nodes, SortOptions options) {
        /*
         * here we want to use the sort algorithm which work on module descriptors : so we first put
         * dependencies on a map from descriptors to dependency, then we sort the keySet (i.e. a
         * collection of descriptors), then we replace in the sorted list each descriptor by the
         * corresponding dependency
         */

        Map<ModuleDescriptor, List<IvyNode>> dependenciesMap = new LinkedHashMap<ModuleDescriptor, List<IvyNode>>();
        List<IvyNode> nulls = new ArrayList<IvyNode>();
        for (IvyNode node : nodes) {
            if (node.getDescriptor() == null) {
                nulls.add(node);
            } else {
                List<IvyNode> n = dependenciesMap.get(node.getDescriptor());
                if (n == null) {
                    n = new ArrayList<IvyNode>();
                    dependenciesMap.put(node.getDescriptor(), n);
                }
                n.add(node);
            }
        }
        List<ModuleDescriptor> list = sortModuleDescriptors(dependenciesMap.keySet(), options);
        final double adjustFactor = 1.3;
        List<IvyNode> ret = new ArrayList<IvyNode>(
                (int) (list.size() * adjustFactor + nulls.size()));
        // attempt to adjust the size to avoid too much list resizing
        for (int i = 0; i < list.size(); i++) {
            ModuleDescriptor md = list.get(i);
            List<IvyNode> n = dependenciesMap.get(md);
            ret.addAll(n);
        }
        ret.addAll(0, nulls);
        return ret;
    }

    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent. This sort
     * ensures that a ModuleDescriptor is always found in the list before all ModuleDescriptors
     * depending directly on it.
     * 
     * @param moduleDescriptors
     *            a Collection of ModuleDescriptor to sort
     * @param options
     *            Options to use to sort the descriptors.
     * @return a List of sorted ModuleDescriptors
     * @throws CircularDependencyException
     *             if a circular dependency exists and circular dependency strategy decide to throw
     *             an exception
     */
    public List<ModuleDescriptor> sortModuleDescriptors(
            Collection<ModuleDescriptor> moduleDescriptors, SortOptions options)
            throws CircularDependencyException {
        Checks.checkNotNull(options, "options");
        ModuleDescriptorSorter sorter = new ModuleDescriptorSorter(moduleDescriptors,
                getVersionMatcher(), options.getNonMatchingVersionReporter(),
                options.isUseCircularDependencyStrategy() ? getCircularStrategy()
                        : IgnoreCircularDependencyStrategy.getInstance());
        return sorter.sortModuleDescriptors();
    }

    protected CircularDependencyStrategy getCircularStrategy() {
        return settings.getCircularDependencyStrategy();
    }

    protected VersionMatcher getVersionMatcher() {
        return settings.getVersionMatcher();
    }

}
