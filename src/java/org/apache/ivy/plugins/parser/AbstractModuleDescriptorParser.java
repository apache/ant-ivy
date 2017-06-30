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
package org.apache.ivy.plugins.parser;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceHelper;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractModuleDescriptorParser implements ModuleDescriptorParser {
    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            boolean validate) throws ParseException, IOException {
        return parseDescriptor(ivySettings, descriptorURL, new URLResource(descriptorURL), validate);
    }

    public String getType() {
        return "ivy";
    }

    public Artifact getMetadataArtifact(ModuleRevisionId mrid, Resource res) {
        return DefaultArtifact.newIvyArtifact(mrid, new Date(res.getLastModified()));
    }

    protected abstract static class AbstractParser extends DefaultHandler {
        private static final String DEFAULT_CONF_MAPPING = "*->*";

        private String defaultConf; // used only as defaultconf, not used for

        // guessing right side part of a mapping
        private String defaultConfMapping; // same as default conf but is used

        // for guessing right side part of a mapping
        private DefaultDependencyDescriptor defaultConfMappingDescriptor;

        private Resource res;

        private List<String> errors = new ArrayList<>();

        private DefaultModuleDescriptor md;

        private ModuleDescriptorParser parser;

        protected AbstractParser(ModuleDescriptorParser parser) {
            this.parser = parser;
        }

        public ModuleDescriptorParser getModuleDescriptorParser() {
            return parser;
        }

        protected void checkErrors() throws ParseException {
            if (!errors.isEmpty()) {
                throw new ParseException(errors.toString(), 0);
            }
        }

        public void setResource(Resource res) {
            this.res = res; // used for log and date only
            md = new DefaultModuleDescriptor(parser, res);
            md.setLastModified(ResourceHelper.getLastModifiedOrDefault(res));
        }

        protected Resource getResource() {
            return res;
        }

        protected String getDefaultConfMapping() {
            return defaultConfMapping;
        }

        protected void setDefaultConfMapping(String defaultConf) {
            defaultConfMapping = defaultConf;
            getMd().setDefaultConfMapping(defaultConf);
        }

        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd) {
            parseDepsConfs(confs, dd, defaultConfMapping != null);
        }

        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand) {
            parseDepsConfs(confs, dd, useDefaultMappingToGuessRightOperand, true);
        }

        protected void parseDepsConfs(String confs, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand, boolean evaluateConditions) {
            if (confs == null) {
                return;
            }

            String[] conf = confs.split(";");
            parseDepsConfs(conf, dd, useDefaultMappingToGuessRightOperand, evaluateConditions);
        }

        protected void parseDepsConfs(String[] conf, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand) {
            parseDepsConfs(conf, dd, useDefaultMappingToGuessRightOperand, true);
        }

        protected void parseDepsConfs(String[] confs, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand, boolean evaluateConditions) {
            replaceConfigurationWildcards(md);
            for (String conf : confs) {
                String[] ops = conf.split("->");
                if (ops.length == 1) {
                    String[] modConfs = ops[0].split(",");
                    if (!useDefaultMappingToGuessRightOperand) {
                        for (String modConf : modConfs) {
                            dd.addDependencyConfiguration(modConf.trim(), modConf.trim());
                        }
                    } else {
                        for (String modConf : modConfs) {
                            String[] depConfs = getDefaultConfMappingDescriptor()
                                    .getDependencyConfigurations(modConf);
                            if (depConfs.length > 0) {
                                for (String depConf : depConfs) {
                                    String mappedDependency = evaluateConditions ? evaluateCondition(
                                            depConf.trim(), dd) : depConf.trim();
                                    if (mappedDependency != null) {
                                        dd.addDependencyConfiguration(modConf.trim(),
                                                mappedDependency);
                                    }
                                }
                            } else {
                                // no default mapping found for this configuration, map
                                // configuration to itself
                                dd.addDependencyConfiguration(modConf.trim(),
                                        modConf.trim());
                            }
                        }
                    }
                } else if (ops.length == 2) {
                    for (String modConf : ops[0].split(",")) {
                        for (String depConf : ops[1].split(",")) {
                            String mappedDependency = evaluateConditions ? evaluateCondition(
                                    depConf.trim(), dd) : depConf.trim();
                            if (mappedDependency != null) {
                                dd.addDependencyConfiguration(modConf.trim(), mappedDependency);
                            }
                        }
                    }
                } else {
                    addError("invalid conf " + conf + " for " + dd);
                }
            }

            if (md.isMappingOverride()) {
                addExtendingConfigurations(confs, dd, useDefaultMappingToGuessRightOperand);
            }
        }

        /**
         * Evaluate the optional condition in the given configuration, like "[org=MYORG]confX". If
         * the condition evaluates to true, the configuration is returned, if the condition
         * evaluates to false, null is returned. If there are no conditions, the configuration
         * itself is returned.
         *
         * @param conf
         *            the configuration to evaluate
         * @param dd
         *            the dependency descriptor to which the configuration will be added
         * @return the evaluated condition
         */
        private String evaluateCondition(String conf, DefaultDependencyDescriptor dd) {
            if (conf.charAt(0) != '[') {
                return conf;
            }

            int endConditionIndex = conf.indexOf(']');
            if (endConditionIndex == -1) {
                addError("invalid conf " + conf + " for " + dd);
                return null;
            }

            String condition = conf.substring(1, endConditionIndex);

            int notEqualIndex = condition.indexOf("!=");
            if (notEqualIndex == -1) {
                int equalIndex = condition.indexOf('=');
                if (equalIndex == -1) {
                    addError("invalid conf " + conf + " for " + dd.getDependencyRevisionId());
                    return null;
                }

                String leftOp = condition.substring(0, equalIndex).trim();
                String rightOp = condition.substring(equalIndex + 1).trim();

                // allow organisation synonyms, like 'org' or 'organization'
                if (leftOp.equals("org") || leftOp.equals("organization")) {
                    leftOp = "organisation";
                }

                String attrValue = dd.getAttribute(leftOp);
                if (!rightOp.equals(attrValue)) {
                    return null;
                }
            } else {
                String leftOp = condition.substring(0, notEqualIndex).trim();
                String rightOp = condition.substring(notEqualIndex + 2).trim();

                // allow organisation synonyms, like 'org' or 'organization'
                if (leftOp.equals("org") || leftOp.equals("organization")) {
                    leftOp = "organisation";
                }

                String attrValue = dd.getAttribute(leftOp);
                if (rightOp.equals(attrValue)) {
                    return null;
                }
            }

            return conf.substring(endConditionIndex + 1);
        }

        private void addExtendingConfigurations(String[] confs, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand) {
            for (String conf : confs) {
                addExtendingConfigurations(conf, dd, useDefaultMappingToGuessRightOperand);
            }
        }

        private void addExtendingConfigurations(String conf, DefaultDependencyDescriptor dd,
                boolean useDefaultMappingToGuessRightOperand) {
            Set<String> configsToAdd = new HashSet<>();
            for (Configuration config : md.getConfigurations()) {
                for (String ext : config.getExtends()) {
                    if (conf.equals(ext)) {
                        String configName = config.getName();
                        configsToAdd.add(configName);
                        addExtendingConfigurations(configName, dd,
                                useDefaultMappingToGuessRightOperand);
                    }
                }
            }

            parseDepsConfs(configsToAdd.toArray(new String[configsToAdd.size()]),
                    dd, useDefaultMappingToGuessRightOperand);
        }

        protected DependencyDescriptor getDefaultConfMappingDescriptor() {
            if (defaultConfMappingDescriptor == null) {
                defaultConfMappingDescriptor = new DefaultDependencyDescriptor(
                        ModuleRevisionId.newInstance("", "", ""), false);
                parseDepsConfs(defaultConfMapping, defaultConfMappingDescriptor, false, false);
            }
            return defaultConfMappingDescriptor;
        }

        protected void addError(String msg) {
            if (res != null) {
                errors.add(msg + " in " + res + "\n");
            } else {
                errors.add(msg + "\n");
            }
        }

        @Override
        public void warning(SAXParseException ex) {
            Message.warn("xml parsing: " + getLocationString(ex) + ": " + ex.getMessage());
        }

        @Override
        public void error(SAXParseException ex) {
            addError("xml parsing: " + getLocationString(ex) + ": " + ex.getMessage());
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            addError("[Fatal Error] " + getLocationString(ex) + ": " + ex.getMessage());
        }

        /** Returns a string of the location. */
        private String getLocationString(SAXParseException ex) {
            StringBuilder str = new StringBuilder();

            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1) {
                    systemId = systemId.substring(index + 1);
                }
                str.append(systemId);
            } else if (getResource() != null) {
                str.append(getResource().toString());
            }
            str.append(':');
            str.append(ex.getLineNumber());
            str.append(':');
            str.append(ex.getColumnNumber());

            return str.toString();

        } // getLocationString(SAXParseException):String

        protected String getDefaultConf() {
            return defaultConf != null ? defaultConf
                    : (defaultConfMapping != null ? defaultConfMapping : DEFAULT_CONF_MAPPING);
        }

        protected void setDefaultConf(String defaultConf) {
            this.defaultConf = defaultConf;
            getMd().setDefaultConf(defaultConf);
        }

        public ModuleDescriptor getModuleDescriptor() throws ParseException {
            checkErrors();
            return md;
        }

        protected Date getDefaultPubDate() {
            return new Date(md.getLastModified());
        }

        private void replaceConfigurationWildcards(ModuleDescriptor md) {
            for (Configuration config : md.getConfigurations()) {
                config.replaceWildcards(md);
            }
        }

        protected void setMd(DefaultModuleDescriptor md) {
            this.md = md;
        }

        protected DefaultModuleDescriptor getMd() {
            return md;
        }
    }

}
