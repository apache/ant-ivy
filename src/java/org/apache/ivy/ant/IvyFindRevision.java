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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;

/**
 * Look for the latest module in the repository matching the given criteria, and sets a set of
 * properties according to what was found.
 */
public class IvyFindRevision extends IvyTask {
    private String organisation;

    private String module;

    private String branch;

    private String revision;

    private String property = "ivy.revision";

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

    public String getProperty() {
        return property;
    }

    public void setProperty(String prefix) {
        this.property = prefix;
    }

    public void doExecute() throws BuildException {
        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy findrevision task");
        }
        if (module == null) {
            throw new BuildException("no module name provided for ivy findrevision task");
        }
        if (revision == null) {
            throw new BuildException("no revision provided for ivy findrevision task");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (branch == null) {
            branch = settings.getDefaultBranch(new ModuleId(organisation, module));
        }
        ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(organisation,
            module, branch, revision));
        if (rmr != null) {
            getProject().setProperty(property, rmr.getId().getRevision());
        }
    }
}
