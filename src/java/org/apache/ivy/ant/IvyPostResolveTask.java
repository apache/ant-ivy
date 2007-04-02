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
import java.util.Arrays;
import java.util.HashSet;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;


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
    private String _resolveId;

    private String _type;
    private File _file;
    
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
        IvySettings settings = ivy.getSettings();
        
        boolean orgAndModSetManually = (_organisation != null) && (_module != null);
        
        _organisation = getProperty(_organisation, settings, "ivy.organisation");
        _module = getProperty(_module, settings, "ivy.module");
        
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        
        if (_file == null) {
        	String fileName = getProperty(settings, "ivy.resolved.file", _resolveId);
        	if (fileName != null) {
        		_file = new File(fileName);
        	}
        }

        if (isInline()) {
        	_conf = _conf == null ? "*" : _conf;
            if (_organisation == null) {
                throw new BuildException("no organisation provided for ivy cache task in inline mode: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property");
            }
            if (_module == null) {
                throw new BuildException("no module name provided for ivy cache task in inline mode: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property");
            }
            if (_resolveId == null) {
            	_resolveId = ResolveOptions.getDefaultResolveId(getResolvedModuleId());
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
        		resolve.setCache(_cache);
//        		resolve.setResolveId(_resolveId);  TODO
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
        		ensureResolved(isHaltonfailure(), isUseOrigin(), isTransitive(), getOrganisation(), getModule(), getProperty(_conf, settings, "ivy.resolved.configurations"), _resolveId, _cache);
        	}
        	
	        _conf = getProperty(_conf, settings, "ivy.resolved.configurations");
	        if ("*".equals(_conf)) {
	            _conf = getProperty(settings, "ivy.resolved.configurations");
	            if (_conf == null) {
	                throw new BuildException("bad conf provided for ivy cache task: * can only be used with a prior call to <resolve/>");
	            }
	        }
        }
        _organisation = getProperty(_organisation, settings, "ivy.organisation");
        _module = getProperty(_module, settings, "ivy.module");
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy cache task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy cache task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy cache task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (_resolveId == null) {
        	_resolveId = ResolveOptions.getDefaultResolveId(getResolvedModuleId());
        }
        
        _artifactFilter = FilterHelper.getArtifactTypeFilter(_type);
    }

    protected void ensureResolved(boolean haltOnFailure, boolean useOrigin, boolean transitive, String org, String module, String conf, String resolveId, File cache) {
        ensureMessageInitialised();
        
        String[] confs = null;
        if (resolveId != null) {
        	confs = getConfsToResolve(resolveId, conf);
        } else {
        	confs = getConfsToResolve(org, module, conf, false);
        }
        
        if (confs.length > 0)  {
        	IvyResolve resolve = createResolve(haltOnFailure, useOrigin);
    		resolve.setFile(_file);
        	resolve.setCache(cache);
        	resolve.setTransitive(transitive);
        	resolve.setConf(StringUtils.join(confs, ", "));
        	resolve.setResolveId(resolveId);
        	resolve.execute();
        } 
    }
    
    protected String[] getConfsToResolve(String org, String module, String conf, boolean strict) {
        ModuleDescriptor reference = (ModuleDescriptor) getResolvedDescriptor(org, module, strict);
        String[] rconfs = getResolvedConfigurations(org, module, strict);
        return getConfsToResolve(reference, conf, rconfs);
    }
    
    protected String[] getConfsToResolve(String resolveId, String conf) {
        ModuleDescriptor reference = (ModuleDescriptor) getResolvedDescriptor(resolveId, false);
        if (reference == null) {
        	// assume the module has been resolved outside this build, resolve the required
        	// configurations again
        	// TODO: find a way to discover which confs were resolved by that previous resolve
        	if (conf == null) {
        		return new String[] {"*"};
        	} else {
        		return splitConfs(conf);
        	}
        }
        String[] rconfs = (String[]) getProject().getReference("ivy.resolved.configurations.ref." + resolveId);
        return getConfsToResolve(reference, conf, rconfs);
    }

    private String[] getConfsToResolve(ModuleDescriptor reference, String conf, String[] rconfs) {
		Message.debug("calculating configurations to resolve");
        
        if (reference == null)  {
    		Message.debug("module not yet resolved, all confs still need to be resolved");
        	if (conf == null) {
        		return new String[] {"*"};
        	} else {
        		return splitConfs(conf);
        	}
        } else if (conf != null) {
        	String[] confs;
        	if ("*".equals(conf)) {
        		confs = reference.getConfigurationsNames();
        	} else {
        		confs = splitConfs(conf);
        	}
    		HashSet rconfsSet = new HashSet(Arrays.asList(rconfs));
			HashSet confsSet = new HashSet(Arrays.asList(confs));
			Message.debug("resolved configurations:   "+rconfsSet);
			Message.debug("asked configurations:      "+confsSet);
			confsSet.removeAll(rconfsSet);
			Message.debug("to resolve configurations: "+confsSet);
			return (String[]) confsSet.toArray(new String[confsSet.size()]);
        } else {
    		Message.debug("module already resolved, no configuration to resolve");
        	return new String[0];
        }
    	
    }

    protected IvyResolve createResolve(boolean haltOnFailure, boolean useOrigin) {
		Message.verbose("no resolved descriptor found: launching default resolve");
		IvyResolve resolve = new IvyResolve();
		resolve.setProject(getProject());
		resolve.setHaltonfailure(haltOnFailure);
		resolve.setUseOrigin(useOrigin);
		resolve.setValidate(isValidate());
		return resolve;
	}

    protected ModuleRevisionId getResolvedMrid() {
    	return new ModuleRevisionId(getResolvedModuleId(), getRevision() == null ?Ivy.getWorkingRevision():getRevision());
    }

    protected ModuleId getResolvedModuleId() {
    	return isInline()?new ModuleId(getOrganisation(), getModule()+"-caller"):new ModuleId(getOrganisation(), getModule());
    }
    
    protected ResolveReport getResolvedReport() {
        return getResolvedReport(getOrganisation(), isInline()?getModule()+"-caller":getModule(), _resolveId);
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
	
	public void setResolveId(String resolveId) {
		_resolveId = resolveId;
	}
	
	public String getResolveId() {
		return _resolveId;
	}
	
	public void setFile(File file) {
		_file = file;
	}
	
	public File getFile() {
		return _file;
	}

}
