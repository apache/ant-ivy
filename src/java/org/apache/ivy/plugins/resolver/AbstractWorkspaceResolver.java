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
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultWorkspaceModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.WorkspaceModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ManifestHeaderElement;
import org.apache.ivy.osgi.core.ManifestHeaderValue;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

public abstract class AbstractWorkspaceResolver extends AbstractResolver {

    private boolean ignoreBranch;

    private boolean ignoreVersion;

    public void setIgnoreBranch(boolean ignoreBranch) {
        this.ignoreBranch = ignoreBranch;
    }

    public void setIgnoreVersion(boolean ignoreVersion) {
        this.ignoreVersion = ignoreVersion;
    }

    protected ResolvedModuleRevision checkCandidate(DependencyDescriptor dd, ModuleDescriptor md,
            String workspaceModuleName) {

        if (workspaceModuleName == null) {
            workspaceModuleName = dd.getDependencyId().toString();
        }

        ModuleRevisionId dependencyMrid = dd.getDependencyRevisionId();
        String org = dependencyMrid.getModuleId().getOrganisation();
        String module = dependencyMrid.getModuleId().getName();

        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        ModuleRevisionId candidateMrid = md.getModuleRevisionId();

        // search a match on the organization and the module name

        if (org.equals(BundleInfo.BUNDLE_TYPE)) {
            // looking for an OSGi bundle via its symbolic name
            String sn = md.getExtraInfoContentByTagName("Bundle-SymbolicName");
            if (sn == null || !module.equals(sn)) {
                // not found, skip to next
                return null;
            }
        } else if (org.equals(BundleInfo.PACKAGE_TYPE)) {
            // looking for an OSGi bundle via its exported package
            String exportedPackages = md.getExtraInfoContentByTagName("Export-Package");
            if (exportedPackages == null) {
                // not found, skip to next
                return null;
            }
            boolean found = false;
            String version = null;
            ManifestHeaderValue exportElements;
            try {
                exportElements = new ManifestHeaderValue(exportedPackages);
            } catch (ParseException e) {
                // wrong OSGi header: skip it
                return null;
            }
            for (ManifestHeaderElement exportElement : exportElements.getElements()) {
                if (exportElement.getValues().contains(module)) {
                    found = true;
                    version = exportElement.getAttributes().get("version");
                    break;
                }
            }
            if (!found) {
                // not found, skip to next
                return null;
            }
            if (version == null) {
                // no version means anything can match. Let's trick the version matcher by
                // setting the exact expected version
                version = dependencyMrid.getRevision();
            }
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(org, module, version));
        } else {
            if (!candidateMrid.getModuleId().equals(dependencyMrid.getModuleId())) {
                // it doesn't match org#module, skip to next
                return null;
            }
        }

        Message.verbose("Workspace resolver found potential matching workspace module "
                + workspaceModuleName + " with module " + candidateMrid + " for module "
                + dependencyMrid);

        if (!ignoreBranch) {
            ModuleId mid = dependencyMrid.getModuleId();
            String defaultBranch = getSettings().getDefaultBranch(mid);
            String dependencyBranch = dependencyMrid.getBranch();
            String candidateBranch = candidateMrid.getBranch();
            if (dependencyBranch == null) {
                dependencyBranch = defaultBranch;
            }
            if (candidateBranch == null) {
                candidateBranch = defaultBranch;
            }
            if (dependencyBranch != candidateBranch) {
                // Both cannot be null
                if (dependencyBranch == null || candidateBranch == null) {
                    Message.verbose("\t\trejected since branches doesn't match (one is set, the other isn't)");
                    return null;
                }
                if (!dependencyBranch.equals(candidateBranch)) {
                    Message.verbose("\t\trejected since branches doesn't match");
                    return null;
                }
            }
        }

        // Found one; check if it is for the module we need
        if (!ignoreVersion
                && !md.getModuleRevisionId().getRevision().equals(Ivy.getWorkingRevision())
                && !versionMatcher.accept(dd.getDependencyRevisionId(), md)) {
            Message.verbose("\t\treject as version didn't match");
            return null;
        }

        if (ignoreVersion) {
            Message.verbose("\t\tmatched (version are ignored)");
        } else {
            Message.verbose("\t\tversion matched");
        }

        WorkspaceModuleDescriptor workspaceMd = createWorkspaceMd(md);

        Artifact mdaf = md.getMetadataArtifact();
        if (mdaf == null) {
            mdaf = new DefaultArtifact(md.getModuleRevisionId(), md.getPublicationDate(),
                    workspaceModuleName, "ivy", "");
        }
        MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(mdaf);
        madr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
        madr.setSearched(true);

        return new ResolvedModuleRevision(this, this, workspaceMd, madr);
    }

    protected WorkspaceModuleDescriptor createWorkspaceMd(ModuleDescriptor md) {
        DefaultWorkspaceModuleDescriptor newMd = new DefaultWorkspaceModuleDescriptor(
                md.getModuleRevisionId(), "release", null, true);
        newMd.addConfiguration(new Configuration(ModuleDescriptor.DEFAULT_CONFIGURATION));
        newMd.setLastModified(System.currentTimeMillis());

        newMd.setDescription(md.getDescription());
        newMd.setHomePage(md.getHomePage());
        newMd.setLastModified(md.getLastModified());
        newMd.setPublicationDate(md.getPublicationDate());
        newMd.setResolvedPublicationDate(md.getResolvedPublicationDate());
        newMd.setStatus(md.getStatus());

        List<Artifact> artifacts = createWorkspaceArtifacts(md);

        Configuration[] allConfs = md.getConfigurations();
        for (Artifact af : artifacts) {
            if (allConfs.length == 0) {
                newMd.addArtifact(ModuleDescriptor.DEFAULT_CONFIGURATION, af);
            } else {
                for (int k = 0; k < allConfs.length; k++) {
                    newMd.addConfiguration(allConfs[k]);
                    newMd.addArtifact(allConfs[k].getName(), af);
                }
            }
        }

        DependencyDescriptor[] dependencies = md.getDependencies();
        for (int k = 0; k < dependencies.length; k++) {
            newMd.addDependency(dependencies[k]);
        }

        ExcludeRule[] allExcludeRules = md.getAllExcludeRules();
        for (int k = 0; k < allExcludeRules.length; k++) {
            newMd.addExcludeRule(allExcludeRules[k]);
        }

        newMd.getExtraInfos().addAll(md.getExtraInfos());

        License[] licenses = md.getLicenses();
        for (int k = 0; k < licenses.length; k++) {
            newMd.addLicense(licenses[k]);
        }

        return newMd;
    }

    abstract protected List<Artifact> createWorkspaceArtifacts(ModuleDescriptor md);

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException("publish not supported by " + getName());
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        return null;
    }

}
