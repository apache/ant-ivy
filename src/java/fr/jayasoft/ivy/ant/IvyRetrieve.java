/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;

import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * @author Hanin
 *
 */
public class IvyRetrieve extends IvyTask {
    private String _organisation;
    private String _module;
    private String _conf;
    private File _cache;
    private String _pattern;
    private String _ivypattern = null;
    private boolean _haltOnFailure = true;

    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getConf() {
        return _conf;
    }
    public void setConf(String conf) {
        _conf = conf;
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
    public String getPattern() {
        return _pattern;
    }
    public void setPattern(String pattern) {
        _pattern = pattern;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");

        ensureResolved(isHaltonfailure(), getOrganisation(), getModule());
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _pattern = getProperty(_pattern, ivy, "ivy.retrieve.pattern");
        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(ivy, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy retrieve task: * can only be used with a prior call to <resolve/>");
            }
        }
        
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy retrieve task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy retrieve task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy retrieve task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        try {
            ivy.retrieve(new ModuleId(_organisation, _module), splitConfs(_conf), _cache, _pattern);
        } catch (Exception ex) {
            throw new BuildException("impossible to ivy retrieve: "+ex.getMessage(), ex);
        }
    }
    public String getIvypattern() {
        return _ivypattern;
    }
    public void setIvypattern(String ivypattern) {
        _ivypattern = ivypattern;
    }
    
}
