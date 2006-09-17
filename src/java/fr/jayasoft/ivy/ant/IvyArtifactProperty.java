/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.xml.XmlReportParser;

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
        Ivy ivy = getIvyInstance();
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");

        ensureResolved(isHaltonfailure(), false, getOrganisation(), getModule());
        
        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(ivy, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy artifactproperty: * can only be used with a prior call to <resolve/>");
            }
        }
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
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
                    String name = IvyPatternHelper.substitute(ivy.substitute(getName()), artifact, confs[i]);
                    String value = IvyPatternHelper.substitute(ivy.substitute(getValue()), artifact, confs[i]);
                    getProject().setProperty(name, value);
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to add artifact properties: "+ex, ex);
        }
    }
}
