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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;

public class IvyDependencyUpdateChecker extends IvyPostResolveTask {

    private String revisionToCheck = "latest.integration";

    private boolean download = false;

    private boolean checkIfChanged = false;

    private boolean showTransitive = false;

    public void doExecute() throws BuildException {
        prepareAndCheck();

        ModuleDescriptor originalModuleDescriptor = getResolvedReport().getModuleDescriptor();
        // clone module descriptor
        DefaultModuleDescriptor latestModuleDescriptor = new DefaultModuleDescriptor(
                originalModuleDescriptor.getModuleRevisionId(),
                originalModuleDescriptor.getStatus(), originalModuleDescriptor.getPublicationDate());
        // copy configurations
        for (int i = 0; i < originalModuleDescriptor.getConfigurations().length; i++) {
            Configuration configuration = originalModuleDescriptor.getConfigurations()[i];
            latestModuleDescriptor.addConfiguration(configuration);
        }
        // clone dependency and add new one with the requested revisionToCheck
        for (int i = 0; i < originalModuleDescriptor.getDependencies().length; i++) {
            DependencyDescriptor dependencyDescriptor = originalModuleDescriptor.getDependencies()[i];
            ModuleRevisionId upToDateMrid = ModuleRevisionId.newInstance(
                dependencyDescriptor.getDependencyRevisionId(), revisionToCheck);
            latestModuleDescriptor.addDependency(dependencyDescriptor.clone(upToDateMrid));
        }

        // resolve
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setDownload(isDownload());
        resolveOptions.setLog(getLog());
        resolveOptions.setConfs(splitConfs(getConf()));
        resolveOptions.setCheckIfChanged(checkIfChanged);

        ResolveReport latestReport;
        try {
            latestReport = getIvyInstance().getResolveEngine().resolve(latestModuleDescriptor,
                resolveOptions);

            displayDependencyUpdates(getResolvedReport(), latestReport);
            if (showTransitive) {
                displayNewDependencyOnLatest(getResolvedReport(), latestReport);
                displayMissingDependencyOnLatest(getResolvedReport(), latestReport);
            }

        } catch (ParseException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        }

    }

    private void displayDependencyUpdates(ResolveReport originalReport, ResolveReport latestReport) {
        log("Dependencies updates available :");
        boolean dependencyUpdateDetected = false;
        for (Iterator iterator = latestReport.getDependencies().iterator(); iterator.hasNext();) {
            IvyNode latest = (IvyNode) iterator.next();
            for (Iterator iterator2 = originalReport.getDependencies().iterator(); iterator2
                    .hasNext();) {
                IvyNode originalDependency = (IvyNode) iterator2.next();
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    if (!originalDependency.getResolvedId().getRevision()
                            .equals(latest.getResolvedId().getRevision())) {
                        // is this dependency a transitive dependency ? or direct dependency
                        // (unfortunatly
                        // .isTranstive() methods doesn't have the same meaning)
                        boolean isTransitiveDependency = latest.getDependencyDescriptor(latest
                                .getRoot()) == null;
                        if ((!isTransitiveDependency) || (isTransitiveDependency && showTransitive)) {
                            StringBuffer sb = new StringBuffer();
                            sb.append("\t")//
                                    .append(originalDependency.getResolvedId().getOrganisation()) //
                                    .append('#')//
                                    .append(originalDependency.getResolvedId().getName())//
                                    .append(isTransitiveDependency ? " (transitive)" : "") //
                                    .append("\t")//
                                    .append(originalDependency.getResolvedId().getRevision())//
                                    .append(" -> ")//
                                    .append(latest.getResolvedId().getRevision());
                            log(sb.toString());
                            dependencyUpdateDetected = true;
                        }
                    }

                }
            }
        }
        if (!dependencyUpdateDetected) {
            log("\tAll dependencies are up to date");
        }
    }

    private void displayMissingDependencyOnLatest(ResolveReport originalReport,
            ResolveReport latestReport) {
        List/* <ModuleRevisionId> */listOfMissingDependencyOnLatest = new ArrayList/*
                                                                                    * <ModuleRevisionId
                                                                                    * >
                                                                                    */();
        for (Iterator iterator = originalReport.getDependencies().iterator(); iterator.hasNext();) {
            IvyNode originalDependency = (IvyNode) iterator.next();
            boolean dependencyFound = false;
            for (Iterator iterator2 = latestReport.getDependencies().iterator(); iterator2
                    .hasNext();) {
                IvyNode latest = (IvyNode) iterator2.next();
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    dependencyFound = true;
                }
            }
            if (!dependencyFound) {
                listOfMissingDependencyOnLatest.add(originalDependency.getId());
            }
        }

        if (listOfMissingDependencyOnLatest.size() > 0) {
            log("List of missing dependency on latest resolve :");
            for (Iterator iterator = listOfMissingDependencyOnLatest.iterator(); iterator.hasNext();) {
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) iterator.next();
                log("\t" + moduleRevisionId.toString());
            }
        }
    }

    private void displayNewDependencyOnLatest(ResolveReport originalReport,
            ResolveReport latestReport) {
        List/* <ModuleRevisionId> */listOfNewDependencyOnLatest = new ArrayList/* <ModuleRevisionId> */();
        for (Iterator iterator = latestReport.getDependencies().iterator(); iterator.hasNext();) {
            IvyNode latest = (IvyNode) iterator.next();

            boolean dependencyFound = false;
            for (Iterator iterator2 = originalReport.getDependencies().iterator(); iterator2
                    .hasNext();) {
                IvyNode originalDependency = (IvyNode) iterator2.next();
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    dependencyFound = true;
                }
            }
            if (!dependencyFound) {
                listOfNewDependencyOnLatest.add(latest.getId());
            }
        }
        if (listOfNewDependencyOnLatest.size() > 0) {
            log("List of new dependency on latest resolve :");
            for (Iterator iterator = listOfNewDependencyOnLatest.iterator(); iterator.hasNext();) {
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) iterator.next();
                log("\t" + moduleRevisionId.toString());
            }
        }
    }

    public String getRevisionToCheck() {
        return revisionToCheck;
    }

    public void setRevisionToCheck(String revisionToCheck) {
        this.revisionToCheck = revisionToCheck;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public boolean isShowTransitive() {
        return showTransitive;
    }

    public void setShowTransitive(boolean showTransitive) {
        this.showTransitive = showTransitive;
    }

}
