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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter.ConfigurationScopeMapping;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Convert an ivy file to a pom
 */
public class IvyMakePom extends IvyTask {
    public class Mapping {
        private String conf;
        private String scope;
        public String getConf() {
            return conf;
        }
        public void setConf(String conf) {
            this.conf = conf;
        }
        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    private File pomFile = null;

    private File headerFile = null;

    private File ivyFile = null;

    private Collection mappings = new ArrayList();

    public File getPomFile() {
        return pomFile;
    }

    public void setPomFile(File file) {
        pomFile = file;
    }

    public File getIvyFile() {
        return ivyFile;
    }

    public void setIvyFile(File ivyFile) {
        this.ivyFile = ivyFile;
    }

    public File getHeaderFile() {
        return headerFile;
    }

    public void setHeaderFile(File headerFile) {
        this.headerFile = headerFile;
    }
    
    public Mapping createMapping() {
        Mapping mapping = new Mapping();
        this.mappings.add(mapping);
        return mapping;
    }
    
    public void doExecute() throws BuildException {
        try {
            if (ivyFile == null) {
                throw new BuildException("source ivy file is required for makepom task");
            }
            if (pomFile == null) {
                throw new BuildException("destination pom file is required for makepom task");
            }
            ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
                getSettings(), ivyFile.toURL(), false);
            PomModuleDescriptorWriter.write(md,
                headerFile == null ? null : FileUtil.readEntirely(getHeaderFile()),
                mappings.isEmpty() 
                    ? PomModuleDescriptorWriter.DEFAULT_MAPPING
                    : new ConfigurationScopeMapping(getMappingsMap()), pomFile);
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: " + ivyFile + ": "
                    + e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file " + ivyFile + ": " + e, e);
        } catch (Exception e) {
            throw new BuildException("impossible convert given ivy file to pom file: " + e
                    + " from=" + ivyFile + " to=" + pomFile, e);
        }
    }

    private Map getMappingsMap() {
        Map mappingsMap = new HashMap();
        for (Iterator iter = mappings.iterator(); iter.hasNext();) {
            Mapping mapping = (Mapping) iter.next();
            mappingsMap.put(mapping.getConf(), mapping.getScope());
        }
        return mappingsMap;
    }
}
