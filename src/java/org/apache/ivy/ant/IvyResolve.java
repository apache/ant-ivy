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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExtendsDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolveProcessException;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * This task allow to call the Ivy dependency resolution from ant.
 */
public class IvyResolve extends IvyTask {
    private File file = null;

    private String conf = null;

    private String organisation = null;

    private String module = null;

    private String branch = null;

    private String revision = null;

    private String pubdate = null;

    private boolean inline = false;

    private boolean haltOnFailure = true;

    private boolean showProgress = true;

    private boolean useCacheOnly = false;

    private String type = null;

    private boolean transitive = true;

    private boolean refresh = false;

    private boolean changing = false;

    private Boolean keep = null;

    private String failureProperty = null;

    private boolean useOrigin = false;

    private String resolveMode = null;

    private String resolveId = null;

    private String log = ResolveOptions.LOG_DEFAULT;

    private boolean checkIfChanged = true; // for backward compatibility

    private List<IvyDependency> dependencies = new ArrayList<IvyDependency>();

    private List<IvyExclude> excludes = new ArrayList<IvyExclude>();

    private List<IvyConflict> conflicts = new ArrayList<IvyConflict>();

    public boolean isUseOrigin() {
        return useOrigin;
    }

    public void setUseOrigin(boolean useOrigin) {
        this.useOrigin = useOrigin;
    }

    public String getDate() {
        return pubdate;
    }

    public void setDate(String pubdate) {
        this.pubdate = pubdate;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setCache(File cache) {
        cacheAttributeNotSupported();
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isHaltonfailure() {
        return haltOnFailure;
    }

    public void setHaltonfailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }

    public void setShowprogress(boolean show) {
        this.showProgress = show;
    }

    public boolean isUseCacheOnly() {
        return useCacheOnly;
    }

    public void setUseCacheOnly(boolean useCacheOnly) {
        this.useCacheOnly = useCacheOnly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    /**
     * @deprecated Use {@link #setFailureProperty(String)} instead
     */
    @Deprecated
    public void setFailurePropery(String failureProperty) {
        log("The 'failurepropery' attribute is deprecated. "
                + "Please use the 'failureproperty' attribute instead", Project.MSG_WARN);
        setFailureProperty(failureProperty);
    }

    public void setFailureProperty(String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public String getFailureProperty() {
        return failureProperty;
    }

    public IvyDependency createDependency() {
        IvyDependency dep = new IvyDependency();
        dependencies.add(dep);
        return dep;
    }

    public IvyExclude createExclude() {
        IvyExclude ex = new IvyExclude();
        excludes.add(ex);
        return ex;
    }

    public IvyConflict createConflict() {
        IvyConflict c = new IvyConflict();
        conflicts.add(c);
        return c;
    }

    @Override
    protected void prepareTask() {
        super.prepareTask();
        Message.setShowProgress(showProgress);
    }

    @Override
    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        try {
            conf = getProperty(conf, settings, "ivy.configurations");
            type = getProperty(type, settings, "ivy.resolve.default.type.filter");
            String[] confs = splitConfs(conf);

            boolean childs = !dependencies.isEmpty() || !excludes.isEmpty() || !conflicts.isEmpty();

            ResolveReport report;
            if (childs) {
                if (isInline()) {
                    throw new BuildException("the inline mode is incompatible with child elements");
                }
                if (organisation != null) {
                    throw new BuildException("'organisation' is not allowed with child elements");
                }
                if (module != null) {
                    throw new BuildException("'module' is not allowed with child elements");
                }
                if (file != null) {
                    throw new BuildException("'file' not allowed with child elements");
                }
                if (!getAllowedLogOptions().contains(log)) {
                    throw new BuildException("invalid option for 'log': " + log
                            + ". Available options are " + getAllowedLogOptions());
                }

                ModuleRevisionId mrid = ModuleRevisionId.newInstance("", "",
                    Ivy.getWorkingRevision());
                DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(mrid, null);

                for (IvyDependency dep : dependencies) {
                    DependencyDescriptor dd = dep.asDependencyDescriptor(md, "default", settings);
                    md.addDependency(dd);
                }

                for (IvyExclude exclude : excludes) {
                    DefaultExcludeRule rule = exclude.asRule(settings);
                    rule.addConfiguration("default");
                    md.addExcludeRule(rule);
                }

                for (IvyConflict conflict : conflicts) {
                    conflict.addConflict(md, settings);
                }

                report = ivy
                        .resolve(md, getResolveOptions(ivy, new String[] {"default"}, settings));
            } else if (isInline()) {
                if (organisation == null) {
                    throw new BuildException("'organisation' is required when using inline mode");
                }
                if (module == null) {
                    throw new BuildException("'module' is required when using inline mode");
                }
                if (file != null) {
                    throw new BuildException("'file' not allowed when using inline mode");
                }
                if (!getAllowedLogOptions().contains(log)) {
                    throw new BuildException("invalid option for 'log': " + log
                            + ". Available options are " + getAllowedLogOptions());
                }
                for (int i = 0; i < confs.length; i++) {
                    if ("*".equals(confs[i])) {
                        confs[i] = "*(public)";
                    }
                }
                if (revision == null) {
                    revision = "latest.integration";
                }
                report = ivy.resolve(
                    ModuleRevisionId.newInstance(organisation, module, branch, revision),
                    getResolveOptions(ivy, confs, settings), changing);

            } else {
                if (organisation != null) {
                    throw new BuildException(
                            "'organisation' not allowed when not using 'org' attribute");
                }
                if (module != null) {
                    throw new BuildException("'module' not allowed when not using 'org' attribute");
                }
                if (file == null) {
                    file = getProject().resolveFile(getProperty(settings, "ivy.dep.file"));
                }
                report = ivy.resolve(file.toURI().toURL(), getResolveOptions(ivy, confs, settings));
            }
            if (report.hasError()) {
                if (failureProperty != null) {
                    getProject().setProperty(failureProperty, "true");
                }
                if (isHaltonfailure()) {
                    throw new BuildException("resolve failed - see output for details");
                }
            }
            setResolved(report, resolveId, isKeep());
            confs = report.getConfigurations();

            if (isKeep()) {
                ModuleDescriptor md = report.getModuleDescriptor();
                // put resolved infos in ant properties and ivy variables
                // putting them in ivy variables is important to be able to change from one resolve
                // call to the other
                String mdOrg = md.getModuleRevisionId().getOrganisation();
                String mdName = md.getModuleRevisionId().getName();
                String mdRev = md.getResolvedModuleRevisionId().getRevision();
                getProject().setProperty("ivy.organisation", mdOrg);
                settings.setVariable("ivy.organisation", mdOrg);
                getProject().setProperty("ivy.module", mdName);
                settings.setVariable("ivy.module", mdName);
                getProject().setProperty("ivy.revision", mdRev);
                settings.setVariable("ivy.revision", mdRev);
                for (int i = 0; i < md.getInheritedDescriptors().length; i++) {
                    ExtendsDescriptor parent = md.getInheritedDescriptors()[i];
                    String parentOrg = parent.getResolvedParentRevisionId().getOrganisation();
                    String parentModule = parent.getResolvedParentRevisionId().getName();
                    String parentRevision = parent.getResolvedParentRevisionId().getRevision();
                    String parentBranch = parent.getResolvedParentRevisionId().getBranch();
                    getProject().setProperty("ivy.parent[" + i + "].organisation", parentOrg);
                    settings.setVariable("ivy.parent[" + i + "].organisation", parentOrg);
                    getProject().setProperty("ivy.parent[" + i + "].module", parentModule);
                    settings.setVariable("ivy.parent[" + i + "].module", parentModule);
                    getProject().setProperty("ivy.parent[" + i + "].revision", parentRevision);
                    settings.setVariable("ivy.parent[" + i + "].revision", parentRevision);
                    if (parentBranch != null) {
                        getProject().setProperty("ivy.parent[" + i + "].branch", parentBranch);
                        settings.setVariable("ivy.parent[" + i + "].branch", parentBranch);
                    }
                }
                getProject().setProperty("ivy.parents.count",
                    String.valueOf(md.getInheritedDescriptors().length));
                settings.setVariable("ivy.parents.count",
                    String.valueOf(md.getInheritedDescriptors().length));

                Boolean hasChanged = null;
                if (getCheckIfChanged()) {
                    hasChanged = Boolean.valueOf(report.hasChanged());
                    getProject().setProperty("ivy.deps.changed", hasChanged.toString());
                    settings.setVariable("ivy.deps.changed", hasChanged.toString());
                }
                getProject().setProperty("ivy.resolved.configurations", mergeConfs(confs));
                settings.setVariable("ivy.resolved.configurations", mergeConfs(confs));
                if (file != null) {
                    getProject().setProperty("ivy.resolved.file", file.getAbsolutePath());
                    settings.setVariable("ivy.resolved.file", file.getAbsolutePath());
                }
                if (resolveId != null) {
                    getProject().setProperty("ivy.organisation." + resolveId, mdOrg);
                    settings.setVariable("ivy.organisation." + resolveId, mdOrg);
                    getProject().setProperty("ivy.module." + resolveId, mdName);
                    settings.setVariable("ivy.module." + resolveId, mdName);
                    getProject().setProperty("ivy.revision." + resolveId, mdRev);
                    settings.setVariable("ivy.revision." + resolveId, mdRev);
                    if (getCheckIfChanged()) {
                        // hasChanged has already been set earlier
                        getProject().setProperty("ivy.deps.changed." + resolveId,
                            hasChanged.toString());
                        settings.setVariable("ivy.deps.changed." + resolveId, hasChanged.toString());
                    }
                    getProject().setProperty("ivy.resolved.configurations." + resolveId,
                        mergeConfs(confs));
                    settings.setVariable("ivy.resolved.configurations." + resolveId,
                        mergeConfs(confs));
                    if (file != null) {
                        getProject().setProperty("ivy.resolved.file." + resolveId,
                            file.getAbsolutePath());
                        settings.setVariable("ivy.resolved.file." + resolveId,
                            file.getAbsolutePath());
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: " + file + ": " + e,
                    e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: " + e, e);
        } catch (ResolveProcessException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e.getMessage(), e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        }
    }

    protected Collection<String> getAllowedLogOptions() {
        return Arrays.asList(LogOptions.LOG_DEFAULT, LogOptions.LOG_DOWNLOAD_ONLY,
            LogOptions.LOG_QUIET);
    }

    private ResolveOptions getResolveOptions(Ivy ivy, String[] confs, IvySettings settings) {
        if (useOrigin) {
            settings.useDeprecatedUseOrigin();
        }
        return ((ResolveOptions) new ResolveOptions().setLog(log)).setConfs(confs)
                .setValidate(doValidate(settings))
                .setArtifactFilter(FilterHelper.getArtifactTypeFilter(type)).setRevision(revision)
                .setDate(getPubDate(pubdate, null)).setUseCacheOnly(useCacheOnly)
                .setRefresh(refresh).setTransitive(transitive).setResolveMode(resolveMode)
                .setResolveId(resolveId).setCheckIfChanged(checkIfChanged);
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public boolean isChanging() {
        return changing;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

    public boolean isKeep() {
        return keep == null ? organisation == null : keep.booleanValue();
    }

    public void setKeep(boolean keep) {
        this.keep = Boolean.valueOf(keep);
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getResolveId() {
        return resolveId;
    }

    public void setResolveId(String resolveId) {
        this.resolveId = resolveId;
    }

    public String getResolveMode() {
        return resolveMode;
    }

    public void setResolveMode(String resolveMode) {
        this.resolveMode = resolveMode;
    }

    public boolean getCheckIfChanged() {
        return checkIfChanged;
    }

    public void setCheckIfChanged(boolean checkIfChanged) {
        this.checkIfChanged = checkIfChanged;
    }
}
