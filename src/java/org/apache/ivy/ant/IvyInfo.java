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
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;


/**
 * Parses information about an ivy file and make them available in ant.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyInfo extends IvyTask {
    private File _file = null;
    
    public File getFile() {
        return _file;
    }
    public void setFile(File file) {
        _file = file;
    }

    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (_file == null) {
            _file = getProject().resolveFile(getProperty(settings, "ivy.dep.file"));
        }
        
        try {
			ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(settings, _file.toURL(), doValidate(settings));
            getProject().setProperty("ivy.organisation", md.getModuleRevisionId().getOrganisation());
            getProject().setProperty("ivy.module", md.getModuleRevisionId().getName());
            getProject().setProperty("ivy.revision", md.getModuleRevisionId().getRevision());
            getProject().setProperty("ivy.configurations", mergeConfs(md.getConfigurationsNames()));
            
            // store the public configurations in a separate property
            Configuration[] configs = md.getConfigurations();
            List publicConfigsList = new ArrayList();
            for (int i = 0; i < configs.length; i++) {
            	if (Visibility.PUBLIC.equals(configs[i].getVisibility())) {
            		publicConfigsList.add(configs[i].getName());
            	}
            }
            String[] publicConfigs = (String[]) publicConfigsList.toArray(new String[publicConfigsList.size()]);
            getProject().setProperty("ivy.public.configurations", mergeConfs(publicConfigs));
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: "+_file+": "+e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: "+e, e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies: "+e, e);
        }
    }
}
