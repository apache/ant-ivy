/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.IOException;

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
    private String _xslFile;
    private String _outputpattern;

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
            _outputpattern = "[organisation]-[module]-[conf].html";
        }
        
        if (!_todir.isDirectory()) {
            throw new BuildException("destination directory should be a directory !");
        }
        if (_organisation == null || _module == null) {
            throw new BuildException("no module id provided for retrieve: either call resolve, give paramaters to ivy:retrieve, or provide ivy.module and ivy.organisation properties");
        }
        try {
            String[] confs = splitConfs(_conf);
            for (int i = 0; i < confs.length; i++) {
                genreport(ivy, _cache, _organisation, _module, confs[i]);
                if (_graph) {
                    gengraph(ivy, _cache, _organisation, _module, confs[i]);
                }
            }
        } catch (IOException e) {
            throw new BuildException("impossible to generate report", e);
        }
    }
    
    private void genreport(Ivy ivy, File cache, String organisation, String module, String conf) throws IOException {        
        // first process the report with xslt
        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.setIn(new File(cache, XmlReportOutputter.getReportFileName(new ModuleId(organisation, module), conf)));
        File out;
        if (_todir != null) {
            out = new File(_todir, IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "", conf));
        } else {
            out = new File(IvyPatternHelper.substitute(_outputpattern, organisation, module, "", "", "", "", conf));
        }
        if (out.getParentFile() != null && !out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        xslt.setOut(out);
        xslt.setStyle(getReportStylePath(cache));
        XSLTProcess.Param param = xslt.createParam();
        param.setName("confs");
        param.setExpression(_conf);
        param = xslt.createParam();
        param.setName("extension");
        param.setExpression("html");
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
        XSLTProcess xslt = new XSLTProcess();
        xslt.setTaskName(getTaskName());
        xslt.setProject(getProject());
        xslt.setDestdir(_todir);
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
    
    
    
    
}
