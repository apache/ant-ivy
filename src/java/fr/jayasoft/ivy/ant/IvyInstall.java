/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.filter.FilterHelper;

/**
 * @author Hanin
 *
 */
public class IvyInstall extends IvyTask {
    private String  _organisation;
    private String  _module;
    private String  _revision;
    private File 	_cache; 
    private boolean _overwrite = false;
    private String _from;
    private String _to;
    private boolean _transitive;
    private String _type;
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy publish task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy publish task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_revision == null) {
            throw new BuildException("no module revision provided for ivy publish task: It can either be set explicitely via the attribute 'revision' or via 'ivy.revision' property or a prior call to <resolve/>");
        }
        if (_from == null) {
            throw new BuildException("no from resolver name: please provide it through parameter 'from'");
        }
        if (_to == null) {
            throw new BuildException("no to resolver name: please provide it through parameter 'to'");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        try {
            
            ivy.install(mrid, _from, _to, _transitive, doValidate(ivy), _overwrite, FilterHelper.getArtifactTypeFilter(_type), _cache);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException("impossible to install "+mrid+": "+e.getMessage(), e);
        }
    }

    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
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
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    
    public boolean isOverwrite() {
        return _overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        _overwrite = overwrite;
    }
    public String getFrom() {
        return _from;
    }
    public void setFrom(String from) {
        _from = from;
    }
    public String getTo() {
        return _to;
    }
    public void setTo(String to) {
        _to = to;
    }
    public boolean isTransitive() {
        return _transitive;
    }
    public void setTransitive(boolean transitive) {
        _transitive = transitive;
    }
    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }
}
