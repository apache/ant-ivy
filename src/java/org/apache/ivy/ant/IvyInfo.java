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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Parses information about an ivy file and make them available in ant.
 */
public class IvyInfo extends IvyTask {
    private File file = null;

    private String organisation;

    private String module;

    private String branch;

    private String revision;

    private String property = "ivy";

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
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
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        try {
            if (organisation != null || module != null || revision != null || branch != null) {
                if (organisation == null) {
                    throw new BuildException("no organisation provided for ivy info task");
                }
                if (module == null) {
                    throw new BuildException("no module name provided for ivy info task");
                }
                if (revision == null) {
                    throw new BuildException("no revision provided for ivy info task");
                }

                if (branch == null) {
                    settings.getDefaultBranch(new ModuleId(organisation, module));
                }
                ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(
                    organisation, module, branch, revision));
                if (rmr != null) {
                    ModuleDescriptor md = rmr.getDescriptor();
                    ModuleRevisionId mrid = rmr.getId();
                    setProperties(md, mrid);
                }
            } else {
                if (file == null) {
                    file = getProject().resolveFile(getProperty(settings, "ivy.dep.file"));
                }
                ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
                    settings, file.toURI().toURL(), doValidate(settings));
                ModuleRevisionId mrid = md.getModuleRevisionId();
                setProperties(md, mrid);
            }
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: " + file + ": " + e,
                    e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: " + e, e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies: " + e, e);
        }
    }

    private void setProperties(ModuleDescriptor md, ModuleRevisionId mrid) {
        getProject().setProperty(property + ".organisation", mrid.getOrganisation());
        getProject().setProperty(property + ".module", mrid.getName());
        if (mrid.getBranch() != null) {
            getProject().setProperty(property + ".branch", mrid.getBranch());
        }
        getProject().setProperty(property + ".revision", mrid.getRevision());
        getProject().setProperty(property + ".status", md.getStatus());
        if (md.getPublicationDate() != null) {
            getProject().setProperty(property + ".publication",
                Long.toString(md.getPublicationDate().getTime()));
        }

        Map extra = mrid.getExtraAttributes();
        for (Iterator iter = extra.entrySet().iterator(); iter.hasNext();) {
            Entry entry = (Entry) iter.next();
            getProject().setProperty(property + ".extra." + entry.getKey(),
                (String) entry.getValue());
        }

        getProject().setProperty(property + ".configurations",
            mergeConfs(md.getConfigurationsNames()));

        // store the public configurations in a separate property
        Configuration[] configs = md.getConfigurations();
        List publicConfigsList = new ArrayList();
        for (int i = 0; i < configs.length; i++) {
            String name = configs[i].getName();
            if (Visibility.PUBLIC.equals(configs[i].getVisibility())) {
                publicConfigsList.add(name);
            }

            if (configs[i].getDescription() != null) {
                getProject().setProperty(property + ".configuration." + name + ".desc",
                    configs[i].getDescription());
            }
        }
        String[] publicConfigs = (String[]) publicConfigsList.toArray(new String[publicConfigsList
                .size()]);
        getProject().setProperty(property + ".public.configurations", mergeConfs(publicConfigs));

        Artifact[] artifacts = md.getAllArtifacts();
        for (int i = 0; i < artifacts.length; i++) {
            int id = i + 1;
            getProject()
                    .setProperty(property + ".artifact." + id + ".name", artifacts[i].getName());
            getProject()
                    .setProperty(property + ".artifact." + id + ".type", artifacts[i].getType());
            getProject().setProperty(property + ".artifact." + id + ".ext", artifacts[i].getExt());
            getProject().setProperty(property + ".artifact." + id + ".conf",
                mergeConfs(artifacts[i].getConfigurations()));

            Map artiExtra = artifacts[i].getExtraAttributes();
            for (Iterator iter = artiExtra.entrySet().iterator(); iter.hasNext();) {
                Entry entry = (Entry) iter.next();
                getProject().setProperty(property + ".artifact." + id + ".extra." + entry.getKey(),
                    (String) entry.getValue());
            }
        }
    }
}
