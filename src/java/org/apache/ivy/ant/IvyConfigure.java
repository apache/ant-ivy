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
import java.util.Properties;

import org.apache.ivy.ant.IvyAntSettings.Credentials;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;

/**
 * Configure Ivy with an ivysettings.xml file
 * 
 * @deprecated Use the IvyAntSettings instead.
 */
public class IvyConfigure extends IvyTask {

    private IvyAntSettings antSettings = new IvyAntSettings();

    public void doExecute() throws BuildException {
        ensureMessageInitialised();
        log("ivy:configure is deprecated, please use the data type ivy:settings instead",
            Project.MSG_WARN);
        // ivyConfigure used to export properties in the ant script.
        // ivy:settings doesn't.
        try {
            loadDefaultProperties();
        } catch (Exception ex) {
            throw new BuildException("impossible to load ivy default properties file: " + ex, ex);
        }
        antSettings.registerAsDefault();
    }

    private void loadDefaultProperties() {
        Property prop = new Property() {
            public void execute() throws BuildException {
                Properties props = antSettings.getDefaultProperties();
                addProperties(props);
            }
        };
        prop.setProject(getProject());
        prop.execute();
    }

    public void addConfiguredCredentials(Credentials c) {
        antSettings.addConfiguredCredentials(c);
    }

    public void setFile(File file) {
        antSettings.setFile(file);
    }

    public void setHost(String host) {
        antSettings.setHost(host);
    }

    public void setPasswd(String passwd) {
        antSettings.setPasswd(passwd);
    }

    public void setProject(Project prj) {
        super.setProject(prj);
        antSettings.setProject(prj);
    }

    public void setRealm(String realm) {
        antSettings.setRealm(realm);
    }

    public void setUrl(String confUrl) throws MalformedURLException {
        antSettings.setUrl(confUrl);
    }

    public void setUsername(String userName) {
        antSettings.setUsername(userName);
    }

}
