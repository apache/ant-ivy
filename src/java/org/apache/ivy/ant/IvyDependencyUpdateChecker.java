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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
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

import static org.apache.ivy.util.StringUtils.splitToArray;

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
        for (Configuration configuration : originalModuleDescriptor.getConfigurations()) {
            latestModuleDescriptor.addConfiguration(configuration);
        }
        // clone dependency and add new one with the requested revisionToCheck
        for (DependencyDescriptor dependencyDescriptor : originalModuleDescriptor.getDependencies()) {
            ModuleRevisionId upToDateMrid = ModuleRevisionId.newInstance(
                dependencyDescriptor.getDependencyRevisionId(), revisionToCheck);
            latestModuleDescriptor.addDependency(dependencyDescriptor.clone(upToDateMrid));
        }

        // resolve
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setDownload(isDownload());
        resolveOptions.setLog(getLog());
        resolveOptions.setConfs(splitToArray(getConf()));
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

        } catch (ParseException | IOException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        }

    }

    private void displayDependencyUpdates(ResolveReport originalReport, ResolveReport latestReport) {
        log("Dependencies updates available :");
        boolean dependencyUpdateDetected = false;
        for (IvyNode latest : latestReport.getDependencies()) {
            for (IvyNode originalDependency : originalReport.getDependencies()) {
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    if (!originalDependency.getResolvedId().getRevision()
                            .equals(latest.getResolvedId().getRevision())) {
                        // is this dependency a transitive or a direct dependency?
                        // (unfortunately .isTransitive() methods do not have the same meaning)
                        boolean isTransitiveDependency = latest.getDependencyDescriptor(latest
                                .getRoot()) == null;
                        if (!isTransitiveDependency || showTransitive) {
                            log(String.format("\t%s#%s%s\t%s -> %s",
                                    originalDependency.getResolvedId().getOrganisation(),
                                    originalDependency.getResolvedId().getName(),
                                    isTransitiveDependency ? " (transitive)" : "",
                                    originalDependency.getResolvedId().getRevision(),
                                    latest.getResolvedId().getRevision()));
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
        List<ModuleRevisionId> listOfMissingDependencyOnLatest = new ArrayList<>();
        for (IvyNode originalDependency : originalReport.getDependencies()) {
            boolean dependencyFound = false;
            for (IvyNode latest : latestReport.getDependencies()) {
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
            for (ModuleRevisionId moduleRevisionId : listOfMissingDependencyOnLatest) {
                log("\t" + moduleRevisionId.toString());
            }
        }
    }

    private void displayNewDependencyOnLatest(ResolveReport originalReport,
            ResolveReport latestReport) {
        List<ModuleRevisionId> listOfNewDependencyOnLatest = new ArrayList<>();
        for (IvyNode latest : latestReport.getDependencies()) {
            boolean dependencyFound = false;
            for (IvyNode originalDependency : originalReport.getDependencies()) {
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
            for (ModuleRevisionId moduleRevisionId : listOfNewDependencyOnLatest) {
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

    public boolean isCheckIfChanged() {
        return checkIfChanged;
    }

    public void setCheckIfChanged(boolean checkIfChanged) {
        this.checkIfChanged = checkIfChanged;
    }
}
