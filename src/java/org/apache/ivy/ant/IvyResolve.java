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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;


/**
 * This task allow to call the Ivy dependency resolution from ant.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyResolve extends IvyTask {
    private File _file = null;
    private String _conf = null;
    private File _cache = null;
    private String _organisation = null;
    private String _module = null;
    private String _revision = null;
    private String _pubdate = null;
    private boolean _inline = false;
    private boolean _haltOnFailure = true;
    private boolean _useCacheOnly = false;
    private String _type = null;
	private boolean _transitive = true;
	private boolean _changing = false;
	private Boolean _keep = null;
	private String _failureProperty = null;
    private boolean _useOrigin = false;
    private String _resolveId = null;
    
    public boolean isUseOrigin() {
    	return _useOrigin;
    }
    
    public void setUseOrigin(boolean useOrigin) {
    	_useOrigin = useOrigin;
    }
    
    public String getDate() {
        return _pubdate;
    }
    public void setDate(String pubdate) {
        _pubdate = pubdate;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
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
    public File getFile() {
        return _file;
    }
    public void setFile(File file) {
        _file = file;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    public void setShowprogress(boolean show) {
        Message.setShowProgress(show);
    }
    public boolean isUseCacheOnly() {
        return _useCacheOnly;
    }
    public void setUseCacheOnly(boolean useCacheOnly) {
        _useCacheOnly = useCacheOnly;
    }
    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }
    /**
	 * @deprecated Use {@link #setFailureProperty(String)} instead
	 */
	public void setFailurePropery(String failureProperty) {
		log("The 'failurepropery' attribute is deprecated. " + 
				"Please use the 'failureproperty' attribute instead", Project.MSG_WARN);
		setFailureProperty(failureProperty);
	}
	public void setFailureProperty(String failureProperty) {
    	_failureProperty = failureProperty;
    }
    public String getFailureProperty() {
    	return _failureProperty;
    }
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        try {
            _conf = getProperty(_conf, settings, "ivy.configurations");
            _type = getProperty(_type, settings, "ivy.resolve.default.type.filter");
            if (_cache == null) {
                _cache = settings.getDefaultCache();
            }
            String[] confs = splitConfs(_conf);
            
            ResolveReport report;
            if (isInline()) {
            	if (_organisation == null) {
            		throw new BuildException("'organisation' is required when using inline mode");
            	}
            	if (_module == null) {
            		throw new BuildException("'module' is required when using inline mode");
            	}
            	if (_file != null) {
            		throw new BuildException("'file' not allowed when using inline mode");
            	}
            	if (_revision == null) {
            		_revision = "latest.integration";
            	}
	            report = ivy.resolve(
	            		ModuleRevisionId.newInstance(_organisation, _module, _revision),
	            		getResolveOptions(confs, settings),
	            		_changing);
            	
            } else {
            	if (_organisation != null) {
            		throw new BuildException("'organisation' not allowed when not using 'org' attribute");
            	}
            	if (_module != null) {
            		throw new BuildException("'module' not allowed when not using 'org' attribute");
            	}
	            if (_file == null) {
	                _file = getProject().resolveFile(getProperty(settings, "ivy.dep.file"));
	            }
	            report = ivy.resolve(
	                    _file.toURL(), 
	                    getResolveOptions(confs, settings));
            }
            if (report.hasError()) {
	            if (_failureProperty != null) {
	            	getProject().setProperty(_failureProperty, "true");
	            }
	            if (isHaltonfailure()) {
	                throw new BuildException("resolve failed - see output for details");
	            }
            }            
            setResolved(report, _resolveId, isKeep());
            
            if (isKeep()) {
            	ModuleDescriptor md = report.getModuleDescriptor();
	            // put resolved infos in ant properties and ivy variables
	            // putting them in ivy variables is important to be able to change from one resolve call to the other
	            getProject().setProperty("ivy.organisation", md.getModuleRevisionId().getOrganisation());
	            settings.setVariable("ivy.organisation", md.getModuleRevisionId().getOrganisation());
	            getProject().setProperty("ivy.module", md.getModuleRevisionId().getName());
	            settings.setVariable("ivy.module", md.getModuleRevisionId().getName());
	            getProject().setProperty("ivy.revision", md.getResolvedModuleRevisionId().getRevision());
	            settings.setVariable("ivy.revision", md.getResolvedModuleRevisionId().getRevision());
	            boolean hasChanged = report.hasChanged();
	            getProject().setProperty("ivy.deps.changed", String.valueOf(hasChanged));
	            settings.setVariable("ivy.deps.changed", String.valueOf(hasChanged));
	            if (_conf.trim().equals("*")) {
	                getProject().setProperty("ivy.resolved.configurations", mergeConfs(md.getConfigurationsNames()));
	                settings.setVariable("ivy.resolved.configurations", mergeConfs(md.getConfigurationsNames()));
	            } else {
	                getProject().setProperty("ivy.resolved.configurations", _conf);
	                settings.setVariable("ivy.resolved.configurations", _conf);
	            }
	            getProject().setProperty("ivy.resolved.file", _file.getAbsolutePath());
	            settings.setVariable("ivy.resolved.file", _file.getAbsolutePath());
	            if (_resolveId != null) {
		            getProject().setProperty("ivy.organisation." + _resolveId, md.getModuleRevisionId().getOrganisation());
		            settings.setVariable("ivy.organisation." + _resolveId, md.getModuleRevisionId().getOrganisation());
		            getProject().setProperty("ivy.module." + _resolveId, md.getModuleRevisionId().getName());
		            settings.setVariable("ivy.module." + _resolveId, md.getModuleRevisionId().getName());
		            getProject().setProperty("ivy.revision." + _resolveId, md.getResolvedModuleRevisionId().getRevision());
		            settings.setVariable("ivy.revision." + _resolveId, md.getResolvedModuleRevisionId().getRevision());
		            getProject().setProperty("ivy.deps.changed." + _resolveId, String.valueOf(hasChanged));
		            settings.setVariable("ivy.deps.changed." + _resolveId, String.valueOf(hasChanged));
		            if (_conf.trim().equals("*")) {
		                getProject().setProperty("ivy.resolved.configurations." + _resolveId, mergeConfs(md.getConfigurationsNames()));
		                settings.setVariable("ivy.resolved.configurations." + _resolveId, mergeConfs(md.getConfigurationsNames()));
		            } else {
		                getProject().setProperty("ivy.resolved.configurations." + _resolveId, _conf);
		                settings.setVariable("ivy.resolved.configurations." + _resolveId, _conf);
		            }
		            getProject().setProperty("ivy.resolved.file." + _resolveId, _file.getAbsolutePath());
		            settings.setVariable("ivy.resolved.file." + _resolveId, _file.getAbsolutePath());
	            }
            }
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: "+_file+": "+e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file: "+e, e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies: "+e, e);
        }
    }
	private ResolveOptions getResolveOptions(String[] confs, IvySettings settings) {
		return new ResolveOptions()
			.setConfs(confs)
			.setValidate(doValidate(settings))
			.setArtifactFilter(FilterHelper.getArtifactTypeFilter(_type))
			.setRevision(_revision)
			.setCache(CacheManager.getInstance(settings, _cache))
			.setDate(getPubDate(_pubdate, null))
			.setUseCacheOnly(_useCacheOnly)
			.setUseOrigin(_useOrigin)
			.setTransitive(_transitive)
			.setResolveId(_resolveId);
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
	public boolean isTransitive() {
		return _transitive;
	}
	public void setTransitive(boolean transitive) {
		_transitive = transitive;
	}
	public boolean isChanging() {
		return _changing;
	}
	public void setChanging(boolean changing) {
		_changing = changing;
	}
	public boolean isKeep() {
		return _keep == null ? _organisation == null : _keep.booleanValue();
	}
	public void setKeep(boolean keep) {
		_keep = Boolean.valueOf(keep);
	}
	public boolean isInline() {
		return _inline;
	}
	public void setInline(boolean inline) {
		_inline = inline;
	}
	public String getResolveId() {
		return _resolveId;
	}
	public void setResolveId(String resolveId) {
		_resolveId = resolveId;
	}
}
