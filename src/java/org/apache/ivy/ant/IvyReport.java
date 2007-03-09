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
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;


/**
 * This ant task let users generates reports (html, xml, graphml, ...) from the last resolve done.
 * 
 * @author Xavier Hanin
 */
public class IvyReport extends IvyTask {
    private File _todir;
    private String _organisation;
    private String _module;
    private String _conf;
    private File _cache;
    private boolean _graph = true;
    private boolean _dot = false;
    private boolean _xml = false;
    private boolean _xsl = true;
    private String _xslFile;
    private String _outputpattern;
    private String _xslext = "html";
    private List _params = new ArrayList();
    private String _resolveId;

    public File getTodir() {
        return _todir;
    }
    public void setTodir(File todir) {
        _todir = todir;
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
    public String getOutputpattern() {
        return _outputpattern;
    }
    
    public void setOutputpattern(String outputpattern) {
        _outputpattern = outputpattern;
    }
    public String getResolveId() {
    	return _resolveId;
    }
    public void setResolveId(String resolveId) {
    	_resolveId = resolveId;
    }

    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        
        _organisation = getProperty(_organisation, settings, "ivy.organisation", _resolveId);
        _module = getProperty(_module, settings, "ivy.module", _resolveId);
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        _conf = getProperty(_conf, settings, "ivy.resolved.configurations", _resolveId);
        if ("*".equals(_conf)) {
            _conf = getProperty(settings, "ivy.resolved.configurations", _resolveId);
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy report task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (_todir == null) {
            String t = getProperty(settings, "ivy.report.todir");
            if (t != null) {
                _todir = new File(t);
            }
        }
        _outputpattern = getProperty(_outputpattern, settings, "ivy.report.output.pattern");
        if (_todir != null && _todir.exists()) {
            _todir.mkdirs();
        }
        if (_outputpattern == null) {
            _outputpattern = "[organisation]-[module]-[conf].[ext]";
        }
        
        if (_todir != null && _todir.exists() && !_todir.isDirectory()) {
            throw new BuildException("destination directory should be a directory !");
        }
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy report task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy report task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_resolveId == null) {
        	_resolveId = ResolveOptions.getDefaultResolveId(new ModuleId(_organisation, _module));
        }
        
        try {
            String[] confs = splitConfs(_conf);
            if (_xsl) {
                genreport(_cache, _organisation, _module, confs);
            }
            if (_xml) {
                genxml(_cache, _organisation, _module, confs);
            }
            if (_graph) {
            	genStyled(_cache, _organisation, _module, confs, getStylePath(_cache, "ivy-report-graph.xsl"), "graphml");
            }
            if (_dot) {
            	genStyled(_cache, _organisation, _module, confs, getStylePath(_cache, "ivy-report-dot.xsl"), "dot");
            }
        } catch (IOException e) {
            throw new BuildException("impossible to generate report: "+e, e);
        }
    }
    
    private void genxml(File cache, String organisation, String module, String[] confs) throws IOException {
    	CacheManager cacheMgr = getIvyInstance().getCacheManager(_cache);
    	for (int i = 0; i < confs.length; i++) {
	        File xml = cacheMgr.getConfigurationResolveReportInCache(_resolveId, confs[i]);

            File out;
            if (_todir != null) {
	            out = new File(_todir, IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "xml", confs[i]));
            } else {
	            out = new File(IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "xml", confs[i]));
            }
        
            FileUtil.copy(xml, out, null);
        }
    }
    private void genreport(File cache, String organisation, String module, String[] confs) throws IOException {        
        genStyled(cache, organisation, module, confs, getReportStylePath(cache), _xslext);

        // copy the css if required
        if (_todir != null && _xslFile == null) {
            File css = new File(_todir, "ivy-report.css");
            if (!css.exists()) {
                Message.debug("copying report css to "+_todir);
                FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), css, null);
            }
            FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), new File(cache, "ivy-report.css"), null);
        }
    }
    
    private String getReportStylePath(File cache) throws IOException {
        if (_xslFile != null) {
            return _xslFile;
        }
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "ivy-report.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.xsl"), style, null);
        return style.getAbsolutePath();
    }
    
    private void genStyled(File cache, String organisation, String module, String[] confs, String style, String ext) throws IOException {        
        // process the report with xslt to generate dot file
        File out;
        if (_todir != null) {
            out = _todir;
        } else {
            out = new File(".");
        }

        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.init();
        
        xslt.setDestdir(out);
        xslt.setBasedir(cache);

        Mapper mapper = new Mapper(getProject());
    	xslt.addMapper(mapper);
        
    	CacheManager cacheMgr = getIvyInstance().getCacheManager(cache);
        for (int i = 0; i < confs.length; i++) {
        	File reportFile = cacheMgr.getConfigurationResolveReportInCache(_resolveId, confs[i]);
        	xslt.setIncludes(reportFile.getName());
        	
        	FileNameMapper reportMapper = new GlobPatternMapper();
			reportMapper.setFrom(reportFile.getName());
        	reportMapper.setTo(IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", ext, confs[i]));
        	mapper.add(reportMapper);
        }
        xslt.setStyle(style);
        
        XSLTProcess.Param param = xslt.createParam();
        param.setName("confs");
        param.setExpression(_conf);
        param = xslt.createParam();
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
    
    private String getStylePath(File cache, String styleResourceName) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, styleResourceName);
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream(styleResourceName), style, null);
        return style.getAbsolutePath();
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
	public boolean isDot() {
		return _dot;
	}
	public void setDot(boolean dot) {
		_dot = dot;
	}
    
}
