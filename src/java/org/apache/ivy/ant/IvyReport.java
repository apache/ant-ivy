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
 */
public class IvyReport extends IvyTask {
    private File todir;

    private String organisation;

    private String module;

    private String conf;

    private File cache;

    private boolean graph = true;

    private boolean dot = false;

    private boolean xml = false;

    private boolean xsl = true;

    private String xslFile;

    private String outputpattern;

    private String xslext = "html";

    private List params = new ArrayList();

    private String resolveId;

    public File getTodir() {
        return todir;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public File getCache() {
        return cache;
    }

    public void setCache(File cache) {
        this.cache = cache;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public boolean isGraph() {
        return graph;
    }

    public void setGraph(boolean graph) {
        this.graph = graph;
    }

    public String getXslfile() {
        return xslFile;
    }

    public void setXslfile(String xslFile) {
        this.xslFile = xslFile;
    }

    public String getOutputpattern() {
        return outputpattern;
    }

    public void setOutputpattern(String outputpattern) {
        this.outputpattern = outputpattern;
    }

    public String getResolveId() {
        return resolveId;
    }

    public void setResolveId(String resolveId) {
        this.resolveId = resolveId;
    }

    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        organisation = getProperty(organisation, settings, "ivy.organisation", resolveId);
        module = getProperty(module, settings, "ivy.module", resolveId);
        if (cache == null) {
            cache = settings.getDefaultCache();
        }
        conf = getProperty(conf, settings, "ivy.resolved.configurations", resolveId);
        if ("*".equals(conf)) {
            conf = getProperty(settings, "ivy.resolved.configurations", resolveId);
        }
        if (conf == null) {
            throw new BuildException(
                    "no conf provided for ivy report task: "
                    + "It can either be set explicitely via the attribute 'conf' or"
                    + "via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (todir == null) {
            String t = getProperty(settings, "ivy.report.todir");
            if (t != null) {
                todir = new File(t);
            }
        }
        outputpattern = getProperty(outputpattern, settings, "ivy.report.output.pattern");
        if (todir != null && todir.exists()) {
            todir.mkdirs();
        }
        if (outputpattern == null) {
            outputpattern = "[organisation]-[module]-[conf].[ext]";
        }

        if (todir != null && todir.exists() && !todir.isDirectory()) {
            throw new BuildException("destination directory should be a directory !");
        }
        if (organisation == null) {
            throw new BuildException(
                    "no organisation provided for ivy report task: "
                    + "It can either be set explicitely via the attribute 'organisation' or "
                    + "via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (module == null) {
            throw new BuildException(
                    "no module name provided for ivy report task: "
                    + "It can either be set explicitely via the attribute 'module' or "
                    + "via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (resolveId == null) {
            resolveId = ResolveOptions.getDefaultResolveId(new ModuleId(organisation, module));
        }

        try {
            String[] confs = splitConfs(conf);
            if (xsl) {
                genreport(cache, organisation, module, confs);
            }
            if (xml) {
                genxml(cache, organisation, module, confs);
            }
            if (graph) {
                genStyled(cache, organisation, module, confs, getStylePath(cache,
                    "ivy-report-graph.xsl"), "graphml");
            }
            if (dot) {
                genStyled(cache, organisation, module, confs, getStylePath(cache,
                    "ivy-report-dot.xsl"), "dot");
            }
        } catch (IOException e) {
            throw new BuildException("impossible to generate report: " + e, e);
        }
    }

    private void genxml(File cache, String organisation, String module, String[] confs)
            throws IOException {
        CacheManager cacheMgr = getIvyInstance().getCacheManager(cache);
        for (int i = 0; i < confs.length; i++) {
            File xml = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);

            File out;
            if (todir != null) {
                out = new File(todir, IvyPatternHelper.substitute(outputpattern, organisation,
                    module, "", "", "", "xml", confs[i]));
            } else {
                out = new File(IvyPatternHelper.substitute(outputpattern, organisation, module,
                    "", "", "", "xml", confs[i]));
            }

            FileUtil.copy(xml, out, null);
        }
    }

    private void genreport(File cache, String organisation, String module, String[] confs)
            throws IOException {
        genStyled(cache, organisation, module, confs, getReportStylePath(cache), xslext);

        // copy the css if required
        if (todir != null && xslFile == null) {
            File css = new File(todir, "ivy-report.css");
            if (!css.exists()) {
                Message.debug("copying report css to " + todir);
                FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), css,
                    null);
            }
            FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), new File(
                    cache, "ivy-report.css"), null);
        }
    }

    private String getReportStylePath(File cache) throws IOException {
        if (xslFile != null) {
            return xslFile;
        }
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "ivy-report.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.xsl"), style, null);
        return style.getAbsolutePath();
    }

    private void genStyled(File cache, String organisation, String module, String[] confs,
            String style, String ext) throws IOException {
        // process the report with xslt to generate dot file
        File out;
        if (todir != null) {
            out = todir;
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
            File reportFile = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
            xslt.setIncludes(reportFile.getName());

            FileNameMapper reportMapper = new GlobPatternMapper();
            reportMapper.setFrom(reportFile.getName());
            reportMapper.setTo(IvyPatternHelper.substitute(outputpattern, organisation, module,
                "", "", "", ext, confs[i]));
            mapper.add(reportMapper);
        }
        xslt.setStyle(style);

        XSLTProcess.Param param = xslt.createParam();
        param.setName("confs");
        param.setExpression(conf);
        param = xslt.createParam();
        param.setName("extension");
        param.setExpression(xslext);

        // add the provided XSLT parameters
        for (Iterator it = params.iterator(); it.hasNext();) {
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
        return xml;
    }

    public void setXml(boolean xml) {
        this.xml = xml;
    }

    public boolean isXsl() {
        return xsl;
    }

    public void setXsl(boolean xsl) {
        this.xsl = xsl;
    }

    public String getXslext() {
        return xslext;
    }

    public void setXslext(String xslext) {
        this.xslext = xslext;
    }

    public XSLTProcess.Param createParam() {
        XSLTProcess.Param result = new XSLTProcess.Param();
        params.add(result);
        return result;
    }

    public boolean isDot() {
        return dot;
    }

    public void setDot(boolean dot) {
        this.dot = dot;
    }

}
