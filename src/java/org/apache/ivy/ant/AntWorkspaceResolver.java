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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.resolver.AbstractWorkspaceResolver;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;

public class AntWorkspaceResolver extends DataType {

    public static final class WorkspaceArtifact {

        private String name;

        private String type;

        private String ext;

        private String path;

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private List<ResourceCollection> allResources = new ArrayList<ResourceCollection>();

    private boolean haltOnError = true;

    private Resolver resolver;

    private String name;

    private List<WorkspaceArtifact> artifacts = new ArrayList<WorkspaceArtifact>();

    public void setName(String name) {
        this.name = name;
    }

    public void setHaltonerror(boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    public void addConfigured(ResourceCollection resources) {
        if (!resources.isFilesystemOnly()) {
            throw new BuildException("Only filesystem resource collection is supported");
        }
        allResources.add(resources);
    }

    public WorkspaceArtifact createArtifact() {
        WorkspaceArtifact a = new WorkspaceArtifact();
        artifacts.add(a);
        return a;
    }

    public Resolver getResolver() {
        if (resolver == null) {
            if (name == null) {
                throw new BuildException("A name is required");
            }
            resolver = new Resolver();
            resolver.setName(name);
        }
        return resolver;
    }

    private String getProjectName(File ivyFile) {
        return ivyFile.getParentFile().getName();
    }

    private class Resolver extends AbstractWorkspaceResolver {

        private Map<ModuleDescriptor, File> md2IvyFile;

        private synchronized Map<ModuleDescriptor, File> getModuleDescriptors() {
            if (md2IvyFile == null) {
                md2IvyFile = new HashMap<ModuleDescriptor, File>();
                for (ResourceCollection resources : allResources) {
                    for (Iterator it = resources.iterator(); it.hasNext();) {
                        File ivyFile = ((FileResource) it.next()).getFile();
                        try {
                            ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance()
                                    .parseDescriptor(getParserSettings(), ivyFile.toURI().toURL(),
                                        isValidate());
                            md2IvyFile.put(md, ivyFile);
                            Message.debug("Add " + md.getModuleRevisionId().getModuleId());
                        } catch (Exception ex) {
                            if (haltOnError) {
                                throw new BuildException("impossible to parse ivy file " + ivyFile
                                        + " exception=" + ex, ex);
                            } else {
                                Message.warn("impossible to parse ivy file " + ivyFile
                                        + " exception=" + ex.getMessage());
                            }
                        }
                    }
                }
            }
            return md2IvyFile;
        }

        public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
                throws ParseException {
            Map<ModuleDescriptor, File> mds = getModuleDescriptors();
            for (Entry<ModuleDescriptor, File> md : mds.entrySet()) {
                ResolvedModuleRevision rmr = checkCandidate(dd, md.getKey(),
                    getProjectName(md.getValue()));
                if (rmr != null) {
                    return rmr;
                }
            }
            return null;
        }

        @Override
        protected List<Artifact> createWorkspaceArtifacts(ModuleDescriptor md) {
            List<Artifact> res = new ArrayList<Artifact>();

            for (WorkspaceArtifact wa : artifacts) {
                String name = wa.name;
                String type = wa.type;
                String ext = wa.ext;
                String path = wa.path;
                if (name == null) {
                    name = md.getModuleRevisionId().getName();
                }
                if (type == null) {
                    type = "jar";
                }
                if (ext == null) {
                    ext = "jar";
                }
                if (path == null) {
                    path = "target" + File.separator + "dist" + File.separator + type + "s"
                            + File.separator + name + "." + ext;
                }

                URL url;
                File ivyFile = md2IvyFile.get(md);
                File artifactFile = new File(ivyFile.getParentFile(), path);
                try {
                    url = artifactFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Unsupported file path : " + artifactFile, e);
                }

                res.add(new DefaultArtifact(md.getModuleRevisionId(), new Date(), name, type, ext,
                        url, null));
            }

            return res;
        }

        public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
            // Not much to do here - downloads are not required for workspace projects.
            DownloadReport dr = new DownloadReport();
            for (int i = 0; i < artifacts.length; i++) {
                ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
                dr.addArtifactReport(adr);
                URL url = artifacts[i].getUrl();
                if (url == null || !url.getProtocol().equals("file")) {
                    // this is not an artifact managed by this resolver
                    adr.setDownloadStatus(DownloadStatus.FAILED);
                    return dr;
                }
                File f;
                try {
                    f = new File(url.toURI());
                } catch (URISyntaxException e) {
                    f = new File(url.getPath());
                }
                adr.setLocalFile(f);
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(0);
                Message.verbose("\t[IN WORKSPACE] " + artifacts[i]);
            }
            return dr;
        }
    }

}
