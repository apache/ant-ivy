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
import java.util.List;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;

public class IvyDependency {

    private List<IvyDependencyConf> confs = new ArrayList<IvyDependencyConf>();

    private List<IvyDependencyArtifact> artifacts = new ArrayList<IvyDependencyArtifact>();

    private List<IvyDependencyExclude> excludes = new ArrayList<IvyDependencyExclude>();

    private List<IvyDependencyInclude> includes = new ArrayList<IvyDependencyInclude>();

    private String org;

    private String name;

    private String rev;

    private String branch;

    private String conf;

    private boolean changing;

    private boolean force;

    private boolean transitive = true;

    public IvyDependencyConf createConf() {
        IvyDependencyConf c = new IvyDependencyConf();
        confs.add(c);
        return c;
    }

    public IvyDependencyArtifact createArtifact() {
        IvyDependencyArtifact artifact = new IvyDependencyArtifact();
        artifacts.add(artifact);
        return artifact;
    }

    public IvyDependencyExclude createExclude() {
        IvyDependencyExclude exclude = new IvyDependencyExclude();
        excludes.add(exclude);
        return exclude;
    }

    public IvyDependencyInclude createInclude() {
        IvyDependencyInclude include = new IvyDependencyInclude();
        includes.add(include);
        return include;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public boolean isChanging() {
        return changing;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    DependencyDescriptor asDependencyDescriptor(ModuleDescriptor md, String masterConf,
            IvySettings settings) {
        if (org == null) {
            throw new BuildException("'org' is required on ");
        }
        if (name == null) {
            throw new BuildException("'name' is required when using inline mode");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, name, branch, rev);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, mrid, force, changing,
                transitive);
        if (conf != null) {
            dd.addDependencyConfiguration(masterConf, conf);
        } else {
            dd.addDependencyConfiguration(masterConf, "*");
        }

        for (IvyDependencyConf c : confs) {
            c.addConf(dd, masterConf);
        }

        for (IvyDependencyArtifact artifact : artifacts) {
            artifact.addArtifact(dd, masterConf);
        }

        for (IvyDependencyExclude exclude : excludes) {
            DefaultExcludeRule rule = exclude.asRule(settings);
            dd.addExcludeRule(masterConf, rule);
        }

        for (IvyDependencyInclude include : includes) {
            DefaultIncludeRule rule = include.asRule(settings);
            dd.addIncludeRule(masterConf, rule);
        }

        return dd;
    }

}
