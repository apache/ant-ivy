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
package org.apache.ivy.ant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.tools.ant.BuildException;

public class IvyDependencyTree extends IvyPostResolveTask {

    private Map/* <ModuleRevisionId, List<IvyNode>> */dependencies = new HashMap/*
                                                                                 * <ModuleRevisionId,
                                                                                 * List<IvyNode>>
                                                                                 */();

    private boolean showEvicted = false;

    public void doExecute() throws BuildException {
        prepareAndCheck();
        ResolveReport report = getResolvedReport();
        log("Dependency tree for " + report.getResolveId());
        ModuleRevisionId mrid = report.getModuleDescriptor().getModuleRevisionId();
        // make dependency tree easier to fetch informations
        for (Iterator iterator = report.getDependencies().iterator(); iterator.hasNext();) {
            IvyNode dependency = (IvyNode) iterator.next();
            populateDependencyTree(dependency, mrid, report);
        }
        printDependencies((List) dependencies.get(mrid), 0);
    }

    private void printDependencies(List/* <IvyNode> */dependencyList, int indent) {
        for (Iterator iterator = dependencyList.iterator(); iterator.hasNext();) {
            IvyNode dependency = (IvyNode) iterator.next();
            boolean evicted = dependency.isEvicted(getConf());
            if (evicted && !showEvicted) {
                continue;
            }
            StringBuffer sb = new StringBuffer();
            if (indent > 0) {
                for (int i = 0; i < indent; i++) {
                    if (i == indent - 1 && !iterator.hasNext() && !hasDependencies(dependency)) {
                        sb.append("   ");
                    } else {
                        sb.append("|  ");
                    }

                }
            }
            if (iterator.hasNext()) {
                sb.append("+- ");
            } else {
                sb.append("\\- ");
            }
            sb.append(dependency.getId().toString());
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

            printDependencies((List) dependencies.get(dependency.getId()), indent + 1);
        }
    }

    private boolean hasDependencies(IvyNode dependency) {
        List dependencyList = (List) dependencies.get(dependency.getId());
        return dependencyList.size() > 0;
    }

    private void populateDependencyTree(IvyNode dependency, ModuleRevisionId currentMrid,
            ResolveReport report) {
        registerNodeIfNecessary(dependency.getId());
        for (int i = 0; i < dependency.getAllCallers().length; i++) {
            Caller caller = dependency.getAllCallers()[i];
            addDependency(caller.getModuleRevisionId(), dependency);
        }
    }

    private void registerNodeIfNecessary(ModuleRevisionId moduleRevisionId) {
        if (!dependencies.containsKey(moduleRevisionId)) {
            dependencies.put(moduleRevisionId, new ArrayList/* <IvyNode> */());
        }
    }

    private void addDependency(ModuleRevisionId moduleRevisionId, IvyNode dependency) {
        registerNodeIfNecessary(moduleRevisionId);
        List/* <IvyNode> */list = (List) dependencies.get(moduleRevisionId);
        list.add(dependency);
    }

    public boolean isShowEvicted() {
        return showEvicted;
    }

    public void setShowEvicted(boolean showEvicted) {
        this.showEvicted = showEvicted;
    }

}
