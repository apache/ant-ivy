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
package org.apache.ivy.core.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.Configurator;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 */
public class XmlSettingsParser extends DefaultHandler {
    private Configurator configurator;

    private List configuratorTags = Arrays.asList(new String[] {"resolvers", "namespaces",
            "parsers", "latest-strategies", "conflict-managers", "outputters", "version-matchers",
            "statuses", "circular-dependency-strategies", "triggers", "lock-strategies",
            "caches"});

    private IvySettings ivy;

    private String defaultResolver;

    private String defaultCM;

    private String defaultLatest;

    private String defaultCacheManager;

    private String defaultCircular;

    private String defaultLock;

    private String currentConfiguratorTag;

    private URL settings;

    private boolean deprecatedMessagePrinted = false;

    public XmlSettingsParser(IvySettings ivy) {
        this.ivy = ivy;
    }

    public void parse(URL settings) throws ParseException, IOException {
        configurator = new Configurator();
        // put every type definition from ivy to configurator
        Map typeDefs = ivy.getTypeDefs();
        for (Iterator iter = typeDefs.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            configurator.typeDef(name, (Class) typeDefs.get(name));
        }

        doParse(settings);
    }

    private void doParse(URL settingsUrl) throws IOException, ParseException {
        this.settings = settingsUrl;
        InputStream stream = null;
        try {
            stream = URLHandlerRegistry.getDefault().openStream(settingsUrl);
            SAXParserFactory.newInstance().newSAXParser().parse(stream, this);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            ParseException pe = new ParseException("failed to load settings from " + settingsUrl
                    + ": " + e.getMessage(), 0);
            pe.initCause(e);
            throw pe;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void parse(Configurator configurator, URL configuration) throws IOException,
            ParseException {
        this.configurator = configurator;
        doParse(configuration);
    }

    public void startElement(String uri, String localName, String qName, Attributes att)
            throws SAXException {
        // we first copy attributes in a Map to be able to modify them
        Map attributes = new HashMap();
        for (int i = 0; i < att.getLength(); i++) {
            attributes.put(att.getQName(i), ivy.substitute(att.getValue(i)));
        }

        try {
            if ("ivyconf".equals(qName)) {
                deprecatedMessagePrinted = true;
                Message.deprecated("'ivyconf' element is deprecated, use 'ivysettings' instead ("
                        + settings + ")");
            }
            if (configurator.getCurrent() != null) {
                if ("macrodef".equals(currentConfiguratorTag)
                        && configurator.getTypeDef(qName) != null) {
                    String name = (String) attributes.get("name");
                    if (name == null) {
                        attributes.put("name", "@{name}");
                    } else if (configurator.isTopLevelMacroRecord()
                            && name.indexOf("@{name}") != -1) {
                        attributes.put("name", name);
                    } else {
                        attributes.put("name", "@{name}-" + name);
                    }
                }
                if (attributes.get("ref") != null) {
                    if (attributes.size() != 1) {
                        throw new IllegalArgumentException(
                                "ref attribute should be the only one ! found " + attributes.size()
                                        + " in " + qName);
                    }
                    String name = (String) attributes.get("ref");
                    Object child = null;
                    if ("resolvers".equals(currentConfiguratorTag)) {
                        child = ivy.getResolver(name);
                        if (child == null) {
                            throw new IllegalArgumentException("unknown resolver " + name
                                    + ": resolver should be defined before being referenced");
                        }
                    } else if ("latest-strategies".equals(currentConfiguratorTag)) {
                        child = ivy.getLatestStrategy(name);
                        if (child == null) {
                            throw new IllegalArgumentException("unknown latest strategy " + name
                                   + ": latest strategy should be defined before being referenced");
                        }
                    } else if ("conflict-managers".equals(currentConfiguratorTag)) {
                        child = ivy.getConflictManager(name);
                        if (child == null) {
                            throw new IllegalArgumentException(
                                 "unknown conflict manager " + name
                                 + ": conflict manager should be defined before being referenced");
                        }
                    }
                    if (child == null) {
                        throw new IllegalArgumentException("bad reference " + name);
                    }
                    configurator.addChild(qName, child);
                } else {
                    configurator.startCreateChild(qName);
                    for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
                        String attName = (String) iter.next();
                        configurator.setAttribute(attName, (String) attributes
                                .get(attName));
                    }
                }
            } else if ("classpath".equals(qName)) {
                String urlStr = (String) attributes.get("url");
                URL url = null;
                if (urlStr == null) {
                    String file = (String) attributes.get("file");
                    if (file == null) {
                        throw new IllegalArgumentException(
                                "either url or file should be given for classpath element");
                    } else {
                        url = new File(file).toURL();
                    }
                } else {
                    url = new URL(urlStr);
                }
                ivy.addClasspathURL(url);
            } else if ("typedef".equals(qName)) {
                String name = (String) attributes.get("name");
                String className = (String) attributes.get("classname");
                Class clazz = ivy.typeDef(name, className);
                configurator.typeDef(name, clazz);
            } else if ("property".equals(qName)) {
                String name = (String) attributes.get("name");
                String value = (String) attributes.get("value");
                String override = (String) attributes.get("override");
                if (name == null) {
                    throw new IllegalArgumentException("missing attribute name on property tag");
                }
                if (value == null) {
                    throw new IllegalArgumentException("missing attribute value on property tag");
                }
                ivy.setVariable(name, value, override == null ? true : Boolean.valueOf(override)
                        .booleanValue());
            } else if ("properties".equals(qName)) {
                String propFilePath = (String) attributes.get("file");
                String environmentPrefix = (String) attributes.get("environment");
                if (propFilePath != null) {
                    String override = (String) attributes.get("override");
                    try {
                        Message.verbose("loading properties: " + propFilePath);
                        ivy.loadProperties(new File(propFilePath), override == null ? true : Boolean
                                .valueOf(override).booleanValue());
                    } catch (Exception fileEx) {
                        Message.verbose("failed to load properties as file: trying as url: "
                                + propFilePath);
                        try {
                            ivy.loadProperties(new URL(propFilePath), override == null ? true : Boolean
                                    .valueOf(override).booleanValue());
                        } catch (Exception urlEx) {
                            throw new IllegalArgumentException(
                                    "unable to load properties from "
                                            + propFilePath
                                            + ". Tried both as an url and a file, with no success. File exception: "
                                            + fileEx + ". URL exception: " + urlEx);
                        }
                    }
                } else if (environmentPrefix != null) {
                    ivy.getVariableContainer().setEnvironmentPrefix(environmentPrefix);
                } else {
                    throw new IllegalArgumentException("Didn't find a 'file' or 'environment' attribute " +
                    		"on the 'properties' element");
                }
            } else if ("include".equals(qName)) {
                IvyVariableContainer variables = (IvyVariableContainer) ivy.getVariableContainer()
                        .clone();
                try {
                    String propFilePath = (String) attributes.get("file");
                    URL settingsURL = null;
                    if (propFilePath == null) {
                        propFilePath = (String) attributes.get("url");
                        if (propFilePath == null) {
                            Message.error("bad include tag: specify file or url to include");
                            return;
                        } else {
                            Message.verbose("including url: " + propFilePath);
                            settingsURL = new URL(propFilePath);
                            ivy.setSettingsVariables(settingsURL);
                        }
                    } else {
                        File incFile = new File(propFilePath);
                        if (!incFile.exists()) {
                            Message.error("impossible to include " + incFile
                                    + ": file does not exist");
                            return;
                        } else {
                            Message.verbose("including file: " + propFilePath);
                            ivy.setSettingsVariables(incFile);
                            settingsURL = incFile.toURL();
                        }
                    }
                    new XmlSettingsParser(ivy).parse(configurator, settingsURL);
                } finally {
                    ivy.setVariableContainer(variables);
                }
            } else if ("settings".equals(qName) || "conf".equals(qName)) {
                if ("conf".equals(qName) && !deprecatedMessagePrinted) {
                    Message.deprecated(
                        "'conf' is deprecated, use 'settings' instead (" + settings + ")");
                }
                String cache = (String) attributes.get("defaultCache");
                if (cache != null) {
                    Message.deprecated("'defaultCache' is deprecated, "
                        + "use 'caches[@defaultCacheDir]' instead (" + settings + ")");
                    ivy.setDefaultCache(new File(cache));
                }
                String defaultBranch = (String) attributes.get("defaultBranch");
                if (defaultBranch != null) {
                    ivy.setDefaultBranch(defaultBranch);
                }
                String validate = (String) attributes.get("validate");
                if (validate != null) {
                    ivy.setValidate(Boolean.valueOf(validate).booleanValue());
                }
                String up2d = (String) attributes.get("checkUpToDate");
                if (up2d != null) {
                    Message.deprecated("'checkUpToDate' is deprecated, "
                        + "use 'caches[@checkUpToDate]' instead (" + settings + ")");
                    ivy.setCheckUpToDate(Boolean.valueOf(up2d).booleanValue());
                }
                String useRemoteConfig = (String) attributes.get("useRemoteConfig");
                if (useRemoteConfig != null) {
                    ivy.setUseRemoteConfig(Boolean.valueOf(useRemoteConfig)
                            .booleanValue());
                }
                String cacheIvyPattern = (String) attributes.get("cacheIvyPattern");
                if (cacheIvyPattern != null) {
                    Message.deprecated(
                        "'cacheIvyPattern' is deprecated, use 'caches[@ivyPattern]' instead"
                        + " (" + settings + ")");
                    ivy.setDefaultCacheIvyPattern(cacheIvyPattern);
                }
                String cacheArtPattern = (String) attributes.get("cacheArtifactPattern");
                if (cacheArtPattern != null) {
                    Message.deprecated(
                        "'cacheArtifactPattern' is deprecated, "
                        + "use 'caches[@artifactPattern]' instead (" + settings + ")");
                    ivy.setDefaultCacheArtifactPattern(cacheArtPattern);
                }

                // we do not set following defaults here since no instances has been registered yet
                defaultResolver = (String) attributes.get("defaultResolver");
                defaultCM = (String) attributes.get("defaultConflictManager");
                defaultLatest = (String) attributes.get("defaultLatestStrategy");
                defaultCircular = (String) attributes.get("circularDependencyStrategy");

            } else if ("caches".equals(qName)) {
                currentConfiguratorTag = qName;
                configurator.setRoot(ivy);
                defaultLock = (String) attributes.get("lockStrategy");
                defaultCacheManager = (String) attributes.get("default");
                
                String cache = (String) attributes.get("defaultCacheDir");
                if (cache != null) {
                    ivy.setDefaultCache(new File(cache));
                }
                String up2d = (String) attributes.get("checkUpToDate");
                if (up2d != null) {
                    ivy.setCheckUpToDate(Boolean.valueOf(up2d).booleanValue());
                }
                String resolutionDir = (String) attributes.get("resolutionCacheDir");
                if (resolutionDir != null) {
                    ivy.setDefaultResolutionCacheBasedir(resolutionDir);
                }
                String useOrigin = (String) attributes.get("useOrigin");
                if (useOrigin != null) {
                    ivy.setDefaultUseOrigin(Boolean.valueOf(useOrigin).booleanValue());
                }
                String cacheIvyPattern = (String) attributes.get("ivyPattern");
                if (cacheIvyPattern != null) {
                    ivy.setDefaultCacheIvyPattern(cacheIvyPattern);
                }
                String cacheArtPattern = (String) attributes.get("artifactPattern");
                if (cacheArtPattern != null) {
                    ivy.setDefaultCacheArtifactPattern(cacheArtPattern);
                }
                String repositoryDir = (String) attributes.get("repositoryCacheDir");
                if (repositoryDir != null) {
                    ivy.setDefaultRepositoryCacheBasedir(repositoryDir); 
                }
           } else if ("version-matchers".equals(qName)) {
                currentConfiguratorTag = qName;
                configurator.setRoot(ivy);
                if ("true".equals((String) attributes.get("usedefaults"))) {
                    ivy.configureDefaultVersionMatcher();
                }
            } else if ("statuses".equals(qName)) {
                currentConfiguratorTag = qName;
                StatusManager m = new StatusManager();
                String defaultStatus = (String) attributes.get("default");
                if (defaultStatus != null) {
                    m.setDefaultStatus(defaultStatus);
                }
                ivy.setStatusManager(m);
                configurator.setRoot(m);
            } else if (configuratorTags.contains(qName)) {
                currentConfiguratorTag = qName;
                configurator.setRoot(ivy);
            } else if ("macrodef".equals(qName)) {
                currentConfiguratorTag = qName;
                Configurator.MacroDef macrodef = configurator.startMacroDef((String) attributes
                        .get("name"));
                macrodef.addAttribute("name", null);
            } else if ("module".equals(qName)) {
                attributes.put(IvyPatternHelper.MODULE_KEY, attributes.remove("name"));
                String resolver = (String) attributes.remove("resolver");
                String branch = (String) attributes.remove("branch");
                String cm = (String) attributes.remove("conflict-manager");
                String matcher = (String) attributes.remove("matcher");
                matcher = matcher == null ? PatternMatcher.EXACT_OR_REGEXP : matcher;
                ivy.addModuleConfiguration(attributes, ivy
                        .getMatcher(matcher), resolver, branch, cm);
            }
        } catch (ParseException ex) {
            throw new SAXException("problem in config file: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new SAXException("io problem while parsing config file: " + ex.getMessage(), ex);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (configurator.getCurrent() != null) {
            if (configuratorTags.contains(qName) && configurator.getDepth() == 1) {
                configurator.clear();
                currentConfiguratorTag = null;
            } else if ("macrodef".equals(qName) && configurator.getDepth() == 1) {
                configurator.endMacroDef();
                currentConfiguratorTag = null;
            } else {
                configurator.endCreateChild();
            }
        }
    }

    public void endDocument() throws SAXException {
        if (defaultResolver != null) {
            ivy.setDefaultResolver(ivy.substitute(defaultResolver));
        }
        if (defaultCM != null) {
            ConflictManager conflictManager = ivy.getConflictManager(defaultCM);
            if (conflictManager == null) {
                throw new IllegalArgumentException("unknown conflict manager "
                        + ivy.substitute(defaultCM));
            }
            ivy.setDefaultConflictManager(conflictManager);
        }
        if (defaultLatest != null) {
            LatestStrategy latestStrategy = ivy.getLatestStrategy(ivy.substitute(defaultLatest));
            if (latestStrategy == null) {
                throw new IllegalArgumentException("unknown latest strategy "
                        + ivy.substitute(defaultLatest));
            }
            ivy.setDefaultLatestStrategy(latestStrategy);
        }
        if (defaultCacheManager != null) {
            RepositoryCacheManager cache = ivy.getRepositoryCacheManager(
                                                ivy.substitute(defaultCacheManager));
            if (cache == null) {
                throw new IllegalArgumentException("unknown cache manager "
                        + ivy.substitute(defaultCacheManager));
            }
            ivy.setDefaultRepositoryCacheManager(cache);
        }
        if (defaultCircular != null) {
            CircularDependencyStrategy strategy = ivy.getCircularDependencyStrategy(ivy
                    .substitute(defaultCircular));
            if (strategy == null) {
                throw new IllegalArgumentException("unknown circular dependency strategy "
                        + ivy.substitute(defaultCircular));
            }
            ivy.setCircularDependencyStrategy(strategy);
        }
        if (defaultLock != null) {
            LockStrategy strategy = ivy.getLockStrategy(ivy.substitute(defaultLock));
            if (strategy == null) {
                throw new IllegalArgumentException("unknown lock strategy "
                        + ivy.substitute(defaultLock));
            }
            ivy.setDefaultLockStrategy(strategy);
        }
    }
}
