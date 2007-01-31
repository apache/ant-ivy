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

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;


/**
 * Convert a pom to an ivy file
 * 
 * @author Xavier Hanin
 *
 */
public class IvyConvertPom extends IvyTask {
    private File _pomFile = null;
    private File _ivyFile = null;
    
    public File getPomFile() {
        return _pomFile;
    }
    public void setPomFile(File file) {
        _pomFile = file;
    }
    public File getIvyFile() {
        return _ivyFile;
    }
    public void setIvyFile(File ivyFile) {
        _ivyFile = ivyFile;
    }
    
    
    public void execute() throws BuildException {
        try {
            if (_pomFile == null) {
                throw new BuildException("source pom file is required for convertpom task");
            }
            if (_ivyFile == null) {
                throw new BuildException("destination ivy file is required for convertpom task");
            }
            ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new IvySettings(), _pomFile.toURL(), false);
            PomModuleDescriptorParser.getInstance().toIvyFile(_pomFile.toURL().openStream(), new URLResource(_pomFile.toURL()), getIvyFile(), md);
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given pom file to url: "+_pomFile+": "+e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in pom file "+_pomFile+": "+e, e);
        } catch (Exception e) {
            throw new BuildException("impossible convert given pom file to ivy file: "+e+" from="+_pomFile+" to="+_ivyFile, e);
        }
    }
}
