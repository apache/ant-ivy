package fr.jayasoft.ivy.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.report.ResolveReport;

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
    private File _cache;

    private String _organisation;
    private String _module;

    private String _org = null;
    private String _name = null;
    private String _rev = null;
    private String _type;
    
    
    private Filter _artifactFilter = null;
    
    protected void prepareAndCheck() {
        Ivy ivy = getIvyInstance();
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");

        if (_org != null && getResolvedDescriptor(_org, _name+"-caller", true) == null) {
        	IvyResolve resolve = createResolve(isHaltonfailure());
        	resolve.setOrg(_org);
        	resolve.setName(_name);
        	resolve.setRev(_rev);
        	String conf = _conf == null ? "*" : _conf;
        	resolve.setConf(conf);
        	resolve.execute();
        	
    		_conf = "default"; // this will now be used as the configurations needed to be put in the path.
        } else {        
        	ensureResolved(isHaltonfailure(), isTransitive(), getOrganisation(), getModule());
        	
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
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        
        if (_organisation == null && _org == null) {
            throw new BuildException("no organisation provided for ivy cache task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null && _org == null) {
            throw new BuildException("no module name provided for ivy cache task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy cache task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        _artifactFilter = FilterHelper.getArtifactTypeFilter(_type);
    }

    protected ModuleId getResolvedModuleId() {
        String org = _org != null?_org:_organisation; 
        String mod = _name != null?_name+"-caller":_module; 
    	return new ModuleId(org, mod);
    }
    
    protected ResolveReport getResolvedReport() {
        String org = _org != null?_org:_organisation; 
        String mod = _name != null?_name+"-caller":_module; 
        return getResolvedReport(org, mod);
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
	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getOrg() {
		return _org;
	}

	public void setOrg(String org) {
		_org = org;
	}

	public String getRev() {
		return _rev;
	}

	public void setRev(String rev) {
		_rev = rev;
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

}
