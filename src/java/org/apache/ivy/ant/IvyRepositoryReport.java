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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;


/**
 * Generates a report of dependencies of a set of modules in the repository.
 * 
 * The set of modules is specified using organisation/module and matcher.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyRepositoryReport extends IvyTask {
    private String  _organisation = "*";
    private String  _module;
	private String _branch;
    private String  _revision = "latest.integration";
    private File 	_cache; 
    private String _matcher = PatternMatcher.EXACT_OR_REGEXP;
    
    private File _todir = new File(".");
    private boolean _graph = false;
    private boolean _dot = false;
    private boolean _xml = true;
    private boolean _xsl = false;
    private String _xslFile;
    private String _outputname = "ivy-repository-report";
    private String _xslext = "html";
    private List _params = new ArrayList();
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        if (_xsl && _xslFile == null) {
            throw new BuildException("xsl file is mandatory when using xsl generation");
        }
        if (_module == null && PatternMatcher.EXACT.equals(_matcher)) {
            throw new BuildException("no module name provided for ivy repository graph task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        } else if (_module == null && !PatternMatcher.EXACT.equals(_matcher)) {
        	_module = PatternMatcher.ANY_EXPRESSION;
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        
        try {
    		ModuleId[] mids = ivy.listModules(new ModuleId(_organisation, _module), settings.getMatcher(_matcher));
    		ModuleRevisionId[] mrids = new ModuleRevisionId[mids.length];
    		for (int i = 0; i < mrids.length; i++) {
    			if (_branch != null) {
    				mrids[i] = new ModuleRevisionId(mids[i], _branch, _revision);
    			} else {
    				mrids[i] = new ModuleRevisionId(mids[i], _revision);
    			}
			}
    		DefaultModuleDescriptor md = DefaultModuleDescriptor.newCallerInstance(mrids, true, false);
    		String resolveId = ResolveOptions.getDefaultResolveId(md);
    		ResolveReport report = ivy.resolve(
    				md, 
    				new ResolveOptions()
    					.setResolveId(resolveId)
			    		.setCache(CacheManager.getInstance(settings, _cache))
			    		.setValidate(doValidate(settings)));
    		
    		CacheManager cacheMgr = getIvyInstance().getCacheManager(_cache);
    		new XmlReportOutputter().output(report, _cache);
    		if (_graph) {
    			gengraph(cacheMgr, md.getModuleRevisionId().getOrganisation(), md.getModuleRevisionId().getName());
    		}
    		if (_dot) {
    			gendot(cacheMgr, md.getModuleRevisionId().getOrganisation(), md.getModuleRevisionId().getName());
    		}
    		if (_xml) {
    			
    			FileUtil.copy(cacheMgr.getConfigurationResolveReportInCache(resolveId, "default"), new File(_todir, _outputname+".xml"), null);
    		}
    		if (_xsl) {
    			genreport(cacheMgr, md.getModuleRevisionId().getOrganisation(), md.getModuleRevisionId().getName());
    		}
        } catch (Exception e) {
            throw new BuildException("impossible to generate graph for "+ mrid +": "+e, e);
        }
    }
    
    private void genreport(CacheManager cache, String organisation, String module) throws IOException {  
        // first process the report with xslt
        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.init();
        
        String resolveId = ResolveOptions.getDefaultResolveId(new ModuleId(organisation, module));
        xslt.setIn(cache.getConfigurationResolveReportInCache(resolveId, "default"));
        xslt.setOut(new File(_todir, _outputname+"."+_xslext));
        
        xslt.setStyle(_xslFile);
        
        XSLTProcess.Param param = xslt.createParam();
        param.setName("extension");
        param.setExpression(_xslext);

        // add the provided XSLT parameters
        for (Iterator it = _params.iterator(); it.hasNext(); ) {
            param = (XSLTProcess.Param) it.next();
            XSLTProcess.Param realParam = xslt.createParam();
            realParam.setName(param.getName());
            realParam.setExpression(param.getExpression());
        }
        
        xslt.execute();
    }

    private void gengraph(CacheManager cache, String organisation, String module) throws IOException {        
        gen(cache, organisation, module, getGraphStylePath(cache.getCache()), "graphml");
    }
    
    private String getGraphStylePath(File cache) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "ivy-report-graph-all.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report-graph-all.xsl"), style, null);
        return style.getAbsolutePath();
    }
    
    private void gendot(CacheManager cache, String organisation, String module) throws IOException {        
        gen(cache, organisation, module, getDotStylePath(cache.getCache()), "dot");
    }
    
    private String getDotStylePath(File cache) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "ivy-report-dot-all.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report-dot-all.xsl"), style, null);
        return style.getAbsolutePath();
    }
    
    private void gen(CacheManager cache, String organisation, String module, String style, String ext) throws IOException {        
        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.init();
        
        String resolveId = ResolveOptions.getDefaultResolveId(new ModuleId(organisation, module));
        xslt.setIn(cache.getConfigurationResolveReportInCache(resolveId, "default"));
        xslt.setOut(new File(_todir, _outputname+"."+ext));
        xslt.setBasedir(cache.getCache());
        xslt.setStyle(style);
        xslt.execute();
    }
    
    public File getTodir() {
        return _todir;
    }
    public void setTodir(File todir) {
        _todir = todir;
    }
    public boolean isGraph() {
        return _graph;
    }
    
    public void setGraph(boolean graph) {
        _graph = graph;
    }
    public String getXslfile() {
        return _xslFile;
    }
    
    public void setXslfile(String xslFile) {
        _xslFile = xslFile;
    }
    public boolean isXml() {
        return _xml;
    }
    public void setXml(boolean xml) {
        _xml = xml;
    }
    public boolean isXsl() {
        return _xsl;
    }
    public void setXsl(boolean xsl) {
        _xsl = xsl;
    }
    public String getXslext() {
        return _xslext;
    }
    public void setXslext(String xslext) {
        _xslext = xslext;
    }
    
    public XSLTProcess.Param createParam() {
        XSLTProcess.Param result = new XSLTProcess.Param();
        _params.add(result);
        return result;
    }
    
    public String getOutputname() {
        return _outputname;
    }
    
    public void setOutputname(String outputpattern) {
        _outputname = outputpattern;
    }

	public File getCache() {
		return _cache;
	}
	public void setCache(File cache) {
		_cache = cache;
	}
	public String getMatcher() {
		return _matcher;
	}
	public void setMatcher(String matcher) {
		_matcher = matcher;
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


	public String getBranch() {
		return _branch;
	}


	public void setBranch(String branch) {
		_branch = branch;
	}

	public boolean isDot() {
		return _dot;
	}

	public void setDot(boolean dot) {
		_dot = dot;
	}
}
