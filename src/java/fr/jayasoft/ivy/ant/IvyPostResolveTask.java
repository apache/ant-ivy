package fr.jayasoft.ivy.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.StringUtils;

/**
 * Base class for tasks needing to be performed after a resolve. 
 * 
 * 
 * @author Xavier Hanin
 *
 */
public abstract class IvyPostResolveTask extends IvyTask {
    private String _conf;
    private boolean _haltOnFailure = true;
    private boolean _transitive = true;
    private boolean _inline = false;
    private File _cache;

    private String _organisation;
    private String _module;
    private String _revision = "latest.integration";

    private String _type;
    
    
    private Filter _artifactFilter = null;
    private boolean useOrigin = false;
    
    public boolean isUseOrigin() {
    	return useOrigin;
    }
    
    public void setUseOrigin(boolean useOrigin) {
    	this.useOrigin = useOrigin;
    }
    
    protected void prepareAndCheck() {
        Ivy ivy = getIvyInstance();
        
        boolean orgAndModSetManually = (_organisation != null) && (_module != null);
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");

        if (isInline()) {
        	_conf = _conf == null ? "*" : _conf;
            if (_organisation == null) {
                throw new BuildException("no organisation provided for ivy cache task in inline mode: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property");
            }
            if (_module == null) {
                throw new BuildException("no module name provided for ivy cache task in inline mode: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property");
            }
        	String[] toResolve = getConfsToResolve(getOrganisation(), getModule()+"-caller", _conf, true);
        	if (toResolve.length > 0) {        		
        		Message.verbose("using inline mode to resolve "+getOrganisation()+" "+getModule()+" "+getRevision()+" ("+StringUtils.join(toResolve, ", ")+")");
        		IvyResolve resolve = createResolve(isHaltonfailure(), isUseOrigin());
        		resolve.setOrganisation(getOrganisation());
        		resolve.setModule(getModule());
        		resolve.setRevision(getRevision());
        		resolve.setInline(true);
        		resolve.setConf(_conf);
        		resolve.execute();
        	} else {
        		Message.verbose("inline resolve already done for "+getOrganisation()+" "+getModule()+" "+getRevision()+" ("+_conf+")");
        	}
        	if ("*".equals(_conf)) {
        		_conf = StringUtils.join(getResolvedConfigurations(getOrganisation(), getModule()+"-caller", true), ", ");
        	}
        } else {        
        	Message.debug("using standard ensure resolved");
        	
        	// if the organization and module has been manually specified, we'll reuse the resolved
        	// data from another build (there is no way to know which configurations were resolved
        	// there (TODO: maybe we can check which reports exist and extract the configurations
        	// from these report names?)
        	if (!orgAndModSetManually) {
        		ensureResolved(isHaltonfailure(), isUseOrigin(), isTransitive(), getOrganisation(), getModule(), getProperty(_conf, ivy, "ivy.resolved.configurations"));
        	}
        	
	        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
	        if ("*".equals(_conf)) {
	            _conf = getProperty(ivy, "ivy.resolved.configurations");
	            if (_conf == null) {
	                throw new BuildException("bad conf provided for ivy cache task: * can only be used with a prior call to <resolve/>");
	            }
	        }
        }
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy cache task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy cache task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy cache task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        
        _artifactFilter = FilterHelper.getArtifactTypeFilter(_type);
    }

    protected ModuleId getResolvedModuleId() {
    	return isInline()?new ModuleId(getOrganisation(), getModule()+"-caller"):new ModuleId(getOrganisation(), getModule());
    }
    
    protected ResolveReport getResolvedReport() {
        return getResolvedReport(getOrganisation(), isInline()?getModule()+"-caller":getModule());
    }
    
    public String getType() {
    	return _type;
    }
    public void setType(String type) {
    	_type = type;
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

	public String getRevision() {
		return _revision;
	}

	public void setRevision(String rev) {
		_revision = rev;
	}



	public Filter getArtifactFilter() {
		return _artifactFilter;
	}

	public boolean isTransitive() {
		return _transitive;
	}

	public void setTransitive(boolean transitive) {
		_transitive = transitive;
	}

	public boolean isInline() {
		return _inline;
	}

	public void setInline(boolean inline) {
		_inline = inline;
	}

}
