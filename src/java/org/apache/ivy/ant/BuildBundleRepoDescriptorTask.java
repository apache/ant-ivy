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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.ivy.Ivy;
import org.apache.ivy.ant.AntMessageLogger;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.obr.xml.OBRXMLWriter;
import org.apache.ivy.osgi.repo.FSManifestIterable;
import org.apache.ivy.osgi.repo.ResolverManifestIterable;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class BuildBundleRepoDescriptorTask extends IvyTask {

    private String resolverName = null;

    private File file = null;

    private String cacheName = null;

    private String encoding = "UTF-8";

    private boolean indent = true;

    private File baseDir;

    private String basePath = "";

    private boolean quiet;

    public void setResolver(String resolverName) {
        this.resolverName = resolverName;
    }

    public void setCache(String cacheName) {
        this.cacheName = cacheName;
    }

    public void setOut(File file) {
        this.file = file;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setIndent(boolean indent) {
        this.indent = indent;
    }

    public void setBaseDir(File dir) {
        this.baseDir = dir;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    protected void prepareTask() {
        if (baseDir == null) {
            super.prepareTask();
        }
    }

    public void doExecute() throws BuildException {
        if (file == null) {
            throw new BuildException("No output file specified: use the attribute 'out'");
        }

        Iterator/* <ManifestAndLocation> */it;
        if (resolverName != null) {
            if (baseDir != null) {
                throw new BuildException("specify only one of 'resolver' or 'baseDir'");
            }
            if (cacheName != null) {
                throw new BuildException("specify only one of 'resolver' or 'cache'");
            }
            if (basePath != null) {
                log("'basePath' is only usefull with 'baseDir'", Project.MSG_WARN);
            }
            Ivy ivy = getIvyInstance();
            IvySettings settings = ivy.getSettings();
            DependencyResolver resolver = settings.getResolver(resolverName);
            if (resolver == null) {
                throw new BuildException("the resolver '" + resolverName + "' was not found");
            }
            if (!(resolver instanceof BasicResolver)) {
                throw new BuildException("the type of resolver '"
                        + resolver.getClass().getName() + "' is not supported.");
            }
            it = new ResolverManifestIterable((BasicResolver) resolver).iterator();
        } else if (baseDir != null) {
            if (cacheName != null) {
                throw new BuildException("specify only one of 'baseDir' or 'cache'");
            }
            if (!baseDir.isDirectory()) {
                throw new BuildException(baseDir + " is not a directory");
            }
            it = new FSManifestIterable(baseDir, basePath).iterator();
        } else if (cacheName != null) {
            Ivy ivy = getIvyInstance();
            RepositoryCacheManager cacheManager = ivy.getSettings().getRepositoryCacheManager(
                cacheName);
            if (!(cacheManager instanceof DefaultRepositoryCacheManager)) {
                throw new BuildException("the type of cache '"
                        + cacheManager.getClass().getName() + "' is not supported.");
            }
            File basedir = ((DefaultRepositoryCacheManager) cacheManager).getBasedir();
            it = new FSManifestIterable(basedir, basedir.getAbsolutePath() + File.separator).iterator();
        } else {
            throw new BuildException(
                    "No resolver, cache or basedir specified: "
                            + "please provide one of them through the attribute 'resolver', 'cache' or 'dir'");
        }

        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new BuildException(file + " was not found", e);
        }

        ContentHandler hd;
        try {
            hd = OBRXMLWriter.newHandler(out, encoding, indent);
        } catch (TransformerConfigurationException e) {
            throw new BuildException("Sax configuration error: " + e.getMessage(), e);
        }

        class AntMessageLogger2 extends AntMessageLogger {
            AntMessageLogger2() {
                super(BuildBundleRepoDescriptorTask.this);
            }
        }
        IvyContext.getContext().getMessageLogger();
        Message.setDefaultLogger(new AntMessageLogger2());

        try {
            OBRXMLWriter.writeManifests(it, hd, quiet);
        } catch (SAXException e) {
            throw new BuildException("Sax serialisation error: " + e.getMessage(), e);
        }

        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            // don't care
        }

        Message.sumupProblems();
    }

}
