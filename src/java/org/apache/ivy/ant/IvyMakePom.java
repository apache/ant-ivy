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
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions.ExtraDependency;
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

    public class Dependency {
        private String group = null;

        private String artifact = null;

        private String version = null;

        private String scope = null;

        private String type = null;

        private String classifier = null;

        private boolean optional = false;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getArtifact() {
            return artifact;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public boolean getOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }
    }

    private String artifactName;

    private String artifactPackaging;

    private File pomFile = null;

    private File headerFile = null;

    private File templateFile = null;

    private boolean printIvyInfo = true;

    private String conf;

    private File ivyFile = null;

    private String description;

    private List<Mapping> mappings = new ArrayList<Mapping>();

    private List<Dependency> dependencies = new ArrayList<Dependency>();

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

    public File getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(File templateFile) {
        this.templateFile = templateFile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrintIvyInfo() {
        return printIvyInfo;
    }

    public void setPrintIvyInfo(boolean printIvyInfo) {
        this.printIvyInfo = printIvyInfo;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactPackaging() {
        return artifactPackaging;
    }

    public void setArtifactPackaging(String artifactPackaging) {
        this.artifactPackaging = artifactPackaging;
    }

    public Mapping createMapping() {
        Mapping mapping = new Mapping();
        this.mappings.add(mapping);
        return mapping;
    }

    public Dependency createDependency() {
        Dependency dependency = new Dependency();
        this.dependencies.add(dependency);
        return dependency;
    }

    @Override
    public void doExecute() throws BuildException {
        try {
            if (ivyFile == null) {
                throw new BuildException("source ivy file is required for makepom task");
            }
            if (pomFile == null) {
                throw new BuildException("destination pom file is required for makepom task");
            }
            ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
                getSettings(), ivyFile.toURI().toURL(), false);
            PomModuleDescriptorWriter.write(md, pomFile, getPomWriterOptions());
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

    private PomWriterOptions getPomWriterOptions() throws IOException {
        PomWriterOptions options = new PomWriterOptions();
        options.setConfs(splitConfs(conf)).setArtifactName(getArtifactName())
                .setArtifactPackaging(getArtifactPackaging()).setPrintIvyInfo(isPrintIvyInfo())
                .setDescription(getDescription()).setExtraDependencies(getDependencies())
                .setTemplate(getTemplateFile());

        if (!mappings.isEmpty()) {
            options.setMapping(new PomWriterOptions.ConfigurationScopeMapping(getMappingsMap()));
        }

        if (headerFile != null) {
            options.setLicenseHeader(FileUtil.readEntirely(getHeaderFile()));
        }

        return options;
    }

    private Map<String, String> getMappingsMap() {
        Map<String, String> mappingsMap = new LinkedHashMap<String, String>();
        for (Mapping mapping : mappings) {
            String[] mappingConfs = splitConfs(mapping.getConf());
            for (int i = 0; i < mappingConfs.length; i++) {
                if (!mappingsMap.containsKey(mappingConfs[i])) {
                    mappingsMap.put(mappingConfs[i], mapping.getScope());
                }
            }
        }
        return mappingsMap;
    }

    private List<ExtraDependency> getDependencies() {
        List<ExtraDependency> result = new ArrayList<ExtraDependency>();
        for (Dependency dependency : dependencies) {
            result.add(new ExtraDependency(dependency.getGroup(), dependency.getArtifact(),
                    dependency.getVersion(), dependency.getScope(), dependency.getType(),
                    dependency.getClassifier(), dependency.getOptional()));
        }
        return result;
    }
}
