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

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.tools.ant.BuildException;


/**
 * Set a set of ant properties according to the last artifact resolved 
 * 
 * @author Xavier Hanin
 */
public class IvyArtifactProperty extends IvyTask {
    private String _conf;
    private String _name;
    private String _value;
    
    private String _organisation;
    private String _module;
    private boolean _haltOnFailure = true;
    private File _cache;

    public String getConf() {
        return _conf;
    }
    public void setConf(String conf) {
        _conf = conf;
    }
    public String getName() {
        return _name;
    }
    public void setName(String name) {
        _name = name;
    }
    public String getValue() {
        return _value;
    }
    public void setValue(String value) {
        _value = value;
    }
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }

    public void execute() throws BuildException {
        IvySettings settings = getSettings(); 
        
        _organisation = getProperty(_organisation, settings, "ivy.organisation");
        _module = getProperty(_module, settings, "ivy.module");

        ensureResolved(isHaltonfailure(), false, getOrganisation(), getModule());
        
        _conf = getProperty(_conf, settings, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(settings, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy artifactproperty: * can only be used with a prior call to <resolve/>");
            }
        }
        _organisation = getProperty(_organisation, settings, "ivy.organisation");
        _module = getProperty(_module, settings, "ivy.module");
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy artifactproperty: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy artifactproperty: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy artifactproperty: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        try {
            XmlReportParser parser = new XmlReportParser();
            String[] confs = splitConfs(_conf);
            for (int i = 0; i < confs.length; i++) {
                Artifact[] artifacts = parser.getArtifacts(new ModuleId(_organisation, _module), confs[i], _cache);
                for (int j = 0; j < artifacts.length; j++) {
                    Artifact artifact = artifacts[j];
                    String name = IvyPatternHelper.substitute(settings.substitute(getName()), artifact, confs[i]);
                    String value = IvyPatternHelper.substitute(settings.substitute(getValue()), artifact, confs[i]);
                    getProject().setProperty(name, value);
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to add artifact properties: "+ex, ex);
        }
    }
}
