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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.install.InstallOptions;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;

/**
 * Allow to install a module or a set of module from repository to another one.
 */
public class IvyInstall extends IvyTask {
    private String organisation;

    private String module;

    private String revision;

    private String branch;

    private String conf = "*";

    private boolean overwrite = false;

    private String from;

    private String to;

    private boolean transitive;

    private String type;

    private String matcher = PatternMatcher.EXACT;

    private boolean haltOnFailure = true;

    private boolean installOriginalMetadata = false;

    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'organisation' "
                    + "or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (module == null && PatternMatcher.EXACT.equals(matcher)) {
            throw new BuildException("no module name provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'module' "
                    + "or via 'ivy.module' property or a prior call to <resolve/>");
        } else if (module == null && !PatternMatcher.EXACT.equals(matcher)) {
            module = PatternMatcher.ANY_EXPRESSION;
        }
        if (revision == null && PatternMatcher.EXACT.equals(matcher)) {
            throw new BuildException("no module revision provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'revision' "
                    + "or via 'ivy.revision' property or a prior call to <resolve/>");
        } else if (revision == null && !PatternMatcher.EXACT.equals(matcher)) {
            revision = PatternMatcher.ANY_EXPRESSION;
        }
        if (branch == null && PatternMatcher.EXACT.equals(matcher)) {
            branch = settings.getDefaultBranch(ModuleId.newInstance(organisation, module));
        } else if (branch == null && !PatternMatcher.EXACT.equals(matcher)) {
            branch = PatternMatcher.ANY_EXPRESSION;
        }
        if (from == null) {
            throw new BuildException(
                    "no from resolver name: please provide it through parameter 'from'");
        }
        if (to == null) {
            throw new BuildException(
                    "no to resolver name: please provide it through parameter 'to'");
        }
        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance(organisation, module, branch, revision);

        ResolveReport report;
        try {
            report = ivy.install(
                mrid,
                from,
                to,
                new InstallOptions().setTransitive(transitive).setValidate(doValidate(settings))
                        .setOverwrite(overwrite).setConfs(conf.split(","))
                        .setArtifactFilter(FilterHelper.getArtifactTypeFilter(type))
                        .setMatcherName(matcher)
                        .setInstallOriginalMetadata(installOriginalMetadata));
        } catch (Exception e) {
            throw new BuildException("impossible to install " + mrid + ": " + e, e);
        }

        if (report.hasError() && isHaltonfailure()) {
            throw new BuildException(
                    "Problem happened while installing modules - see output for details");
        }
    }

    public boolean isHaltonfailure() {
        return haltOnFailure;
    }

    public void setHaltonfailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }

    public void setCache(File cache) {
        cacheAttributeNotSupported();
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public boolean isInstallOriginalMetadata() {
        return installOriginalMetadata;
    }

    public void setInstallOriginalMetadata(boolean installOriginalMetadata) {
        this.installOriginalMetadata = installOriginalMetadata;
    }
}
