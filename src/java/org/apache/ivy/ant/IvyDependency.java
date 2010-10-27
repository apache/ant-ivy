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

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.tools.ant.BuildException;

public class IvyDependency {

    private String org;

    private String name;

    private String rev;

    private String branch;

    private String conf;

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

    DependencyDescriptor asDependencyDescriptor(ModuleDescriptor md, String c) {
        if (org == null) {
            throw new BuildException("'org' is required on ");
        }
        if (name == null) {
            throw new BuildException("'name' is required when using inline mode");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, name, branch, rev);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, mrid, false, false, true);
        if (conf != null) {
            dd.addDependencyConfiguration(c, conf);
        } else {
            dd.addDependencyConfiguration(c, "*");            
        }
        return dd;
    }

}
