/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.tools.ant.BuildException;

public class IvyDependencyTree extends IvyPostResolveTask {

    private final Map<ModuleRevisionId, List<IvyNode>> dependencies = new HashMap<>();

    private boolean showEvicted = false;

    public void doExecute() throws BuildException {
        prepareAndCheck();
        ResolveReport report = getResolvedReport();
        if (report == null) {
            throw new BuildException("No resolution report was available to run the post-resolve task. Make sure resolve was done before this task");
        }
        log("Dependency tree for " + report.getResolveId());
        ModuleRevisionId mrid = report.getModuleDescriptor().getModuleRevisionId();
        // make dependency tree easier to fetch information
        for (IvyNode dependency : report.getDependencies()) {
            populateDependencyTree(dependency);
        }
        final List<IvyNode> dependencyList = dependencies.get(mrid);
        if (dependencyList != null) {
            printDependencies(mrid, dependencyList, 0, new HashSet<ModuleRevisionId>());
        }
    }

    private void printDependencies(final ModuleRevisionId mrid, final List<IvyNode> dependencyList, final int indent,
                                   final Set<ModuleRevisionId> ancestors) {
        for (IvyNode dependency : dependencyList) {
            final Set<ModuleRevisionId> ancestorsForCurrentDep = new HashSet<>(ancestors);
            // previous ancestors plus the module to whom these dependencies belong to
            ancestorsForCurrentDep.add(mrid);
            final boolean evicted = dependency.isEvicted(getConf());
            if (evicted && !showEvicted) {
                continue;
            }
            final boolean isLastDependency = dependencyList.indexOf(dependency) == dependencyList.size() - 1;
            final StringBuilder sb = new StringBuilder();
            final ModuleRevisionId dependencyMrid = dependency.getId();
            final boolean circular = ancestorsForCurrentDep.contains(dependencyMrid);
            if (indent > 0) {
                for (int i = 0; i < indent; i++) {
                    if (i == indent - 1 && isLastDependency && !hasDependencies(dependency)) {
                        sb.append("   ");
                    } else {
                        sb.append("|  ");
                    }

                }
            }
            sb.append(isLastDependency ? "\\- " : "+- ");
            if (!evicted && circular) {
                // log and skip processing the (transitive) dependencies of this dependency
                sb.append("(circularly depends on) ").append(dependencyMrid);
                log(sb.toString());
                continue;
            } else {
                sb.append(dependencyMrid.toString());
            }
            if (evicted && showEvicted) {
                EvictionData evictedData = dependency.getEvictedData(getConf());
                if (evictedData.isTransitivelyEvicted()) {
                    sb.append(" transitively");
                } else {
                    sb.append(" evicted by ");
                    sb.append(evictedData.getSelected());
                    sb.append(" in ").append(evictedData.getParent());
                    if (evictedData.getDetail() != null) {
                        sb.append(" ").append(evictedData.getDetail());
                    }
                }
            }
            log(sb.toString());

            printDependencies(dependencyMrid, dependencies.get(dependencyMrid), indent + 1, ancestorsForCurrentDep);
        }
    }

    private boolean hasDependencies(final IvyNode module) {
        if (module == null) {
            return false;
        }
        final List<IvyNode> dependenciesForModule = dependencies.get(module.getId());
        return dependenciesForModule != null && !dependenciesForModule.isEmpty();
    }

    private void populateDependencyTree(IvyNode dependency) {
        registerNodeIfNecessary(dependency.getId());
        for (Caller caller : dependency.getAllCallers()) {
            addDependency(caller.getModuleRevisionId(), dependency);
        }
    }

    private void registerNodeIfNecessary(final ModuleRevisionId moduleRevisionId) {
        if (!dependencies.containsKey(moduleRevisionId)) {
            dependencies.put(moduleRevisionId, new ArrayList<IvyNode>());
        }
    }

    private void addDependency(final ModuleRevisionId moduleRevisionId, final IvyNode dependency) {
        registerNodeIfNecessary(moduleRevisionId);
        dependencies.get(moduleRevisionId).add(dependency);
    }

    public boolean isShowEvicted() {
        return showEvicted;
    }

    public void setShowEvicted(boolean showEvicted) {
        this.showEvicted = showEvicted;
    }

}
