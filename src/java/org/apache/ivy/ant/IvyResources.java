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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.BaseResourceCollectionWrapper;
import org.apache.tools.ant.types.resources.FileResource;

public class IvyResources extends BaseResourceCollectionWrapper {

    private List/* <IvyDependency> */dependencies = new ArrayList();

    private String type = null;

    private String pubdate = null;

    private boolean useCacheOnly = false;

    private boolean transitive = true;

    private boolean refresh = false;

    private String resolveMode = null;

    private String resolveId = null;

    private String log = ResolveOptions.LOG_DEFAULT;

    private Reference antIvyEngineRef;

    public void setType(String type) {
        this.type = type;
    }

    public void setDate(String pubdate) {
        this.pubdate = pubdate;
    }

    public void setPubdate(String pubdate) {
        this.pubdate = pubdate;
    }

    public void setUseCacheOnly(boolean useCacheOnly) {
        this.useCacheOnly = useCacheOnly;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public void setResolveMode(String resolveMode) {
        this.resolveMode = resolveMode;
    }

    public void setResolveId(String resolveId) {
        this.resolveId = resolveId;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public void setSettingsRef(Reference ref) {
        antIvyEngineRef = ref;
    }

    public void addDependency(IvyDependency dependency) {
        dependencies.add(dependency);
    }

    public boolean isFilesystemOnly() {
        return true;
    }

    protected Collection/* <String> */getAllowedLogOptions() {
        return Arrays.asList(new String[] {LogOptions.LOG_DEFAULT, LogOptions.LOG_DOWNLOAD_ONLY,
                LogOptions.LOG_QUIET});
    }

    protected Ivy getIvyInstance() {
        Object antIvyEngine;
        if (antIvyEngineRef != null) {
            antIvyEngine = antIvyEngineRef.getReferencedObject(getProject());
            if (!antIvyEngine.getClass().getName().equals(IvyAntSettings.class.getName())) {
                throw new BuildException(antIvyEngineRef.getRefId()
                        + " doesn't reference an ivy:settings", getLocation());
            }
            if (!(antIvyEngine instanceof IvyAntSettings)) {
                throw new BuildException(antIvyEngineRef.getRefId()
                        + " has been defined in a different classloader.  "
                        + "Please use the same loader when defining your task, or "
                        + "redeclare your ivy:settings in this classloader", getLocation());
            }
        } else {
            antIvyEngine = IvyAntSettings.getDefaultInstance(this);
        }
        Ivy ivy = ((IvyAntSettings) antIvyEngine).getConfiguredIvyInstance(this);
        AntMessageLogger.register(this, ivy);
        return ivy;
    }

    protected Collection getCollection() {
        if (!getAllowedLogOptions().contains(log)) {
            throw new BuildException("invalid option for 'log': " + log
                    + ". Available options are " + getAllowedLogOptions());
        }

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("", "", "");
        DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(mrid, null);

        Iterator itDeps = dependencies.iterator();
        while (itDeps.hasNext()) {
            IvyDependency dep = (IvyDependency) itDeps.next();
            DependencyDescriptor dd = dep.asDependencyDescriptor(md, "default");
            md.addDependency(dd);
        }

        Ivy ivy = getIvyInstance();

        ResolveOptions options = new ResolveOptions();
        options.setConfs(new String[] {"default"});
        options.setDate(IvyTask.getPubDate(pubdate, null));
        options.setUseCacheOnly(useCacheOnly);
        options.setRefresh(refresh);
        options.setTransitive(transitive);
        options.setResolveMode(resolveMode);
        options.setResolveId(resolveId);

        ResolveReport report;
        try {
            report = ivy.resolve(md, options);
        } catch (ParseException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        }

        List/* <> */resources = new ArrayList();

        if (report.hasError()) {
            throw new BuildException("resolve failed - see output for details");
        } else {
            Filter artifactTypeFilter = FilterHelper.getArtifactTypeFilter(type);

            ConfigurationResolveReport configurationReport = report
                    .getConfigurationReport("default");
            Set revisions = configurationReport.getModuleRevisionIds();
            for (Iterator it = revisions.iterator(); it.hasNext();) {
                ModuleRevisionId revId = (ModuleRevisionId) it.next();
                ArtifactDownloadReport[] aReports = configurationReport.getDownloadReports(revId);
                for (int i = 0; i < aReports.length; i++) {
                    if (artifactTypeFilter.accept(aReports[i].getArtifact())) {
                        resources.add(new FileResource(aReports[i].getLocalFile()));
                    }
                }
            }
        }

        return resources;
    }

}
