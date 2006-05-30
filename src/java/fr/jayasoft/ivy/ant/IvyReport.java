/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.report.XmlReportOutputter;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;

public class IvyReport extends IvyTask {
    private File _todir;
    private String _organisation;
    private String _module;
    private String _conf;
    private File _cache;
    private boolean _graph = true;
    private boolean _xml = false;
    private boolean _xsl = true;
    private String _xslFile;
    private String _outputpattern;
    private String _xslext = "html";
    private List _params = new ArrayList();

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


    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
        if (_conf.equals("*")) {
            _conf = getProperty(ivy, "ivy.resolved.configurations");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy report task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (_todir == null) {
            String t = getProperty(ivy, "ivy.report.todir");
            if (t != null) {
                _todir = new File(t);
            }
        }
        _outputpattern = getProperty(_outputpattern, ivy, "ivy.report.output.pattern");
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
        try {
            String[] confs = splitConfs(_conf);
            for (int i = 0; i < confs.length; i++) {
                if (_xsl) {
                    genreport(ivy, _cache, _organisation, _module, confs[i]);
                }
                if (_xml) {
                    genxml(ivy, _cache, _organisation, _module, confs[i]);
                }
                if (_graph) {
                    gengraph(ivy, _cache, _organisation, _module, confs[i]);
                }
            }
        } catch (IOException e) {
            throw new BuildException("impossible to generate report", e);
        }
    }
    
    private void genxml(Ivy ivy, File cache, String organisation, String module, String conf) throws IOException {
        File xml = new File(cache, XmlReportOutputter.getReportFileName(new ModuleId(organisation, module), conf));

        File out;
        if (_todir != null) {
            out = new File(_todir, IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "xml", conf));
        } else {
            out = new File(IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "xml", conf));
        }
        
        FileUtil.copy(xml, out, null);
    }
    private void genreport(Ivy ivy, File cache, String organisation, String module, String conf) throws IOException {        
        // first process the report with xslt
        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.init();
        
        xslt.setIn(new File(cache, XmlReportOutputter.getReportFileName(new ModuleId(organisation, module), conf)));
        File out;
        if (_todir != null) {
            out = new File(_todir, IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", _xslext, conf));
        } else {
            out = new File(IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", _xslext, conf));
        }
        if (out.getParentFile() != null && !out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        } else if(out.getParentFile() == null) {
            out = new File("./"+out.getPath());
        }
        xslt.setOut(out);
        xslt.setStyle(getReportStylePath(cache));
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

        // then copy the css if required
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
    
    
    private void gengraph(Ivy ivy, File cache, String organisation, String module, String conf) throws IOException {        
        // process the report with xslt to generate graphml
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
        xslt.setExtension(".graphml");
        xslt.setIncludes(XmlReportOutputter.getReportFileName(new ModuleId(organisation, module), conf));
        xslt.setStyle(getGraphStylePath(cache));
        xslt.execute();
    }
    
    private String getGraphStylePath(File cache) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "ivy-report-graph.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report-graph.xsl"), style, null);
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
    
}
