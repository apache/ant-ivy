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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.util.JAXPUtils;

/**
 * This ant task let users generates reports (html, xml, graphml, ...) from the last resolve done.
 */
public class IvyReport extends IvyTask {
    private File todir;

    private String organisation;

    private String module;

    private String conf;

    private boolean graph = true;

    private boolean dot = false;

    private boolean xml = false;

    private boolean xsl = true;

    private File xslFile;

    private String outputpattern;

    private String xslext = "html";

    private List params = new ArrayList();

    private String resolveId;

    private ModuleRevisionId mRevId;

    public File getTodir() {
        return todir;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public void setCache(File cache) {
        cacheAttributeNotSupported();
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

    public File getXslfile() {
        return xslFile;
    }

    public void setXslfile(File xslFile) {
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

        conf = getProperty(conf, settings, "ivy.resolved.configurations", resolveId);
        if ("*".equals(conf)) {
            conf = getProperty(settings, "ivy.resolved.configurations", resolveId);
        }
        if (conf == null) {
            throw new BuildException("no conf provided for ivy report task: "
                    + "It can either be set explicitely via the attribute 'conf' or "
                    + "via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (todir == null) {
            String t = getProperty(settings, "ivy.report.todir");
            if (t != null) {
                todir = getProject().resolveFile(t);
            }
        }
        if (todir != null && todir.exists()) {
            todir.mkdirs();
        }
        outputpattern = getProperty(outputpattern, settings, "ivy.report.output.pattern");
        if (outputpattern == null) {
            outputpattern = "[organisation]-[module]-[conf].[ext]";
        }

        if (todir != null && todir.exists() && !todir.isDirectory()) {
            throw new BuildException("destination directory should be a directory !");
        }

        if (resolveId == null) {
            organisation = getProperty(organisation, settings, "ivy.organisation", resolveId);
            module = getProperty(module, settings, "ivy.module", resolveId);

            if (organisation == null) {
                throw new BuildException("no organisation provided for ivy report task: "
                        + "It can either be set explicitely via the attribute 'organisation' or "
                        + "via 'ivy.organisation' property or a prior call to <resolve/>");
            }
            if (module == null) {
                throw new BuildException("no module name provided for ivy report task: "
                        + "It can either be set explicitely via the attribute 'module' or "
                        + "via 'ivy.module' property or a prior call to <resolve/>");
            }

            resolveId = ResolveOptions.getDefaultResolveId(new ModuleId(organisation, module));
        }

        try {
            String[] confs = splitConfs(conf);
            if (xsl) {
                genreport(confs);
            }
            if (xml) {
                genxml(confs);
            }
            if (graph) {
                genStyled(confs, getStylePath("ivy-report-graph.xsl"), "graphml");
            }
            if (dot) {
                genStyled(confs, getStylePath("ivy-report-dot.xsl"), "dot");
            }
        } catch (IOException e) {
            throw new BuildException("impossible to generate report: " + e, e);
        }
    }

    private void genxml(String[] confs) throws IOException {
        ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
        for (int i = 0; i < confs.length; i++) {
            File xml = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);

            File out;
            if (todir != null) {
                out = new File(todir, getOutputPattern(confs[i], "xml"));
            } else {
                out = getProject().resolveFile(getOutputPattern(confs[i], "xml"));
            }

            FileUtil.copy(xml, out, null);
        }
    }

    private void genreport(String[] confs) throws IOException {
        genStyled(confs, getReportStylePath(), xslext);

        // copy the css if required
        if (xslFile == null) {
            File css;
            if (todir != null) {
                css = new File(todir, "ivy-report.css");
            } else {
                css = getProject().resolveFile("ivy-report.css");
            }

            if (!css.exists()) {
                Message.debug("copying report css to " + css.getAbsolutePath());
                FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), css,
                    null);
            }
        }
    }

    private File getReportStylePath() throws IOException {
        if (xslFile != null) {
            return xslFile;
        }
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
        File style = new File(cacheMgr.getResolutionCacheRoot(), "ivy-report.xsl");
        if (!style.exists()) {
            Message.debug("copying ivy-report.xsl to " + style.getAbsolutePath());
            FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.xsl"), style,
                null);
        }
        return style;
    }

    private String getOutputPattern(String conf, String ext) {
        if (mRevId == null) {
            ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();

            XmlReportParser parser = new XmlReportParser();
            File reportFile = cacheMgr.getConfigurationResolveReportInCache(resolveId, conf);

            try {
                parser.parse(reportFile);
            } catch (ParseException e) {
                throw new BuildException("Error occurred while parsing reportfile '"
                        + reportFile.getAbsolutePath() + "'", e);
            }

            // get the resolve module
            mRevId = parser.getResolvedModule();
        }

        return IvyPatternHelper.substitute(outputpattern, mRevId.getOrganisation(),
            mRevId.getName(), mRevId.getRevision(), "", "", ext, conf,
            mRevId.getQualifiedExtraAttributes(), null);
    }

    private void genStyled(String[] confs, File style, String ext) throws IOException {
        ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();

        // process the report with xslt to generate dot file
        File out;
        if (todir != null) {
            out = todir;
        } else {
            out = getProject().getBaseDir();
        }

        InputStream xsltStream = null;
        try {
            // create stream to stylesheet
            xsltStream = new BufferedInputStream(new FileInputStream(style));
            Source xsltSource = new StreamSource(xsltStream, JAXPUtils.getSystemId(style));

            // create transformer
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(xsltSource);

            // add standard parameters
            transformer.setParameter("confs", conf);
            transformer.setParameter("extension", xslext);

            // add the provided XSLT parameters
            for (Iterator it = params.iterator(); it.hasNext();) {
                XSLTProcess.Param param = (XSLTProcess.Param) it.next();
                transformer.setParameter(param.getName(), param.getExpression());
            }

            // create the report
            for (int i = 0; i < confs.length; i++) {
                File reportFile = cacheMgr
                        .getConfigurationResolveReportInCache(resolveId, confs[i]);
                File outFile = new File(out, getOutputPattern(confs[i], ext));

                log("Processing " + reportFile + " to " + outFile);

                // make sure the output directory exist
                File outFileDir = outFile.getParentFile();
                if (!outFileDir.exists()) {
                    if (!outFileDir.mkdirs()) {
                        throw new BuildException("Unable to create directory: "
                                + outFileDir.getAbsolutePath());
                    }
                }

                InputStream inStream = null;
                OutputStream outStream = null;
                try {
                    inStream = new BufferedInputStream(new FileInputStream(reportFile));
                    outStream = new BufferedOutputStream(new FileOutputStream(outFile));
                    StreamResult res = new StreamResult(outStream);
                    Source src = new StreamSource(inStream, JAXPUtils.getSystemId(style));
                    transformer.transform(src, res);
                } catch (TransformerException e) {
                    throw new BuildException(e);
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    if (outStream != null) {
                        try {
                            outStream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (TransformerConfigurationException e) {
            throw new BuildException(e);
        } finally {
            if (xsltStream != null) {
                try {
                    xsltStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private File getStylePath(String styleResourceName) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
        File style = new File(cacheMgr.getResolutionCacheRoot(), styleResourceName);
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream(styleResourceName), style, null);
        return style;
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
