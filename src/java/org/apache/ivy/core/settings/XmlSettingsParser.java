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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Configurator;
import org.apache.ivy.util.FileResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 */
public class XmlSettingsParser extends DefaultHandler {
    /**
     * Wraps an {@link IvyVariableContainer} delegating most method calls to the wrapped instance,
     * except for a set of variables which are only stored locally in the wrapper, and not
     * propagated to the wrapped instance.
     */
    private static final class IvyVariableContainerWrapper implements IvyVariableContainer {
        private static final Collection SETTINGS_VARIABLES = Arrays.asList(new String[] {
                "ivy.settings.dir", "ivy.settings.url", "ivy.settings.file", "ivy.conf.dir",
                "ivy.conf.url", "ivy.conf.file"});

        private final IvyVariableContainer variables;

        private Map localVariables = new HashMap();

        private IvyVariableContainerWrapper(IvyVariableContainer variables) {
            this.variables = variables;
        }

        public void setVariable(String varName, String value, boolean overwrite) {
            if (SETTINGS_VARIABLES.contains(varName)) {
                if (!localVariables.containsKey(varName) || overwrite) {
                    localVariables.put(varName, value);
                }
            } else {
                variables.setVariable(varName, value, overwrite);
            }
        }

        public void setEnvironmentPrefix(String prefix) {
            variables.setEnvironmentPrefix(prefix);
        }

        public String getVariable(String name) {
            if (localVariables.containsKey(name)) {
                return (String) localVariables.get(name);
            }
            return variables.getVariable(name);
        }

        public Object clone() {
            throw new UnsupportedOperationException();
        }
    }

    private Configurator configurator;

    private List configuratorTags = Arrays.asList(new String[] {"resolvers", "namespaces",
            "parsers", "latest-strategies", "conflict-managers", "outputters", "version-matchers",
            "statuses", "circular-dependency-strategies", "triggers", "lock-strategies", "caches",
            "signers"});

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
        configurator.setFileResolver(new FileResolver() {
            public File resolveFile(String path, String filename) {
                return Checks.checkAbsolute(path, filename);
            }
        });
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
            InputSource inSrc = new InputSource(stream);
            inSrc.setSystemId(settingsUrl.toExternalForm());
            SAXParserFactory.newInstance().newSAXParser().parse(settingsUrl.toExternalForm(), this);
            ivy.validate();
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
                    // ignored
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
                inConfiguratorStarted(qName, attributes);
            } else if ("classpath".equals(qName)) {
                classpathStarted(attributes);
            } else if ("typedef".equals(qName)) {
                typedefStarted(attributes);
            } else if ("property".equals(qName)) {
                propertyStarted(attributes);
            } else if ("properties".equals(qName)) {
                propertiesStarted(attributes);
            } else if ("include".equals(qName)) {
                includeStarted(attributes);
            } else if ("settings".equals(qName) || "conf".equals(qName)) {
                settingsStarted(qName, attributes);
            } else if ("caches".equals(qName)) {
                cachesStarted(qName, attributes);
            } else if ("version-matchers".equals(qName)) {
                versionMatchersStarted(qName, attributes);
            } else if ("statuses".equals(qName)) {
                statusesStarted(qName, attributes);
            } else if (configuratorTags.contains(qName)) {
                anyConfiguratorStarted(qName);
            } else if ("macrodef".equals(qName)) {
                macrodefStarted(qName, attributes);
            } else if ("module".equals(qName)) {
                moduleStarted(attributes);
            } else if ("credentials".equals(qName)) {
                credentialsStarted(attributes);
            }
        } catch (ParseException ex) {
            SAXException sax = new SAXException("problem in config file: " + ex.getMessage(), ex);
            sax.initCause(ex);
            throw sax;
        } catch (IOException ex) {
            SAXException sax = new SAXException("io problem while parsing config file: "
                    + ex.getMessage(), ex);
            sax.initCause(ex);
            throw sax;
        }
    }

    private void credentialsStarted(Map attributes) {
        String realm = (String) attributes.remove("realm");
        String host = (String) attributes.remove("host");
        String userName = (String) attributes.remove("username");
        String passwd = (String) attributes.remove("passwd");
        CredentialsStore.INSTANCE.addCredentials(realm, host, userName, passwd);
    }

    private void moduleStarted(Map attributes) {
        attributes.put(IvyPatternHelper.MODULE_KEY, attributes.remove("name"));
        String resolver = (String) attributes.remove("resolver");
        String branch = (String) attributes.remove("branch");
        String cm = (String) attributes.remove("conflict-manager");
        String resolveMode = (String) attributes.remove("resolveMode");
        String matcher = (String) attributes.remove("matcher");
        matcher = matcher == null ? PatternMatcher.EXACT_OR_REGEXP : matcher;
        ivy.addModuleConfiguration(attributes, ivy.getMatcher(matcher), resolver, branch, cm,
            resolveMode);
    }

    private void macrodefStarted(String qName, Map attributes) {
        currentConfiguratorTag = qName;
        Configurator.MacroDef macrodef = configurator
                .startMacroDef((String) attributes.get("name"));
        macrodef.addAttribute("name", null);
    }

    private void anyConfiguratorStarted(String qName) {
        currentConfiguratorTag = qName;
        configurator.setRoot(ivy);
    }

    private void statusesStarted(String qName, Map attributes) {
        currentConfiguratorTag = qName;
        StatusManager m = new StatusManager();
        String defaultStatus = (String) attributes.get("default");
        if (defaultStatus != null) {
            m.setDefaultStatus(defaultStatus);
        }
        ivy.setStatusManager(m);
        configurator.setRoot(m);
    }

    private void versionMatchersStarted(String qName, Map attributes) {
        anyConfiguratorStarted(qName);
        if ("true".equals(attributes.get("usedefaults"))) {
            ivy.configureDefaultVersionMatcher();
        }
    }

    private void cachesStarted(String qName, Map attributes) {
        anyConfiguratorStarted(qName);
        defaultLock = (String) attributes.get("lockStrategy");
        defaultCacheManager = (String) attributes.get("default");

        String cache = (String) attributes.get("defaultCacheDir");
        if (cache != null) {
            ivy.setDefaultCache(Checks.checkAbsolute(cache, "defaultCacheDir"));
        }
        String up2d = (String) attributes.get("checkUpToDate");
        if (up2d != null) {
            Message.deprecated("'checkUpToDate' is deprecated, "
                    + "use the 'overwriteMode' on the 'ivy:retrieve' task instead (" + settings
                    + ")");
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
    }

    private void settingsStarted(String qName, Map attributes) {
        if ("conf".equals(qName) && !deprecatedMessagePrinted) {
            Message.deprecated("'conf' is deprecated, use 'settings' instead (" + settings + ")");
        }
        String cache = (String) attributes.get("defaultCache");
        if (cache != null) {
            Message.deprecated("'defaultCache' is deprecated, "
                    + "use 'caches[@defaultCacheDir]' instead (" + settings + ")");
            ivy.setDefaultCache(Checks.checkAbsolute(cache, "defaultCache"));
        }
        String defaultBranch = (String) attributes.get("defaultBranch");
        if (defaultBranch != null) {
            ivy.setDefaultBranch(defaultBranch);
        }
        String defaultResolveMode = (String) attributes.get("defaultResolveMode");
        if (defaultResolveMode != null) {
            ivy.setDefaultResolveMode(defaultResolveMode);
        }
        String validate = (String) attributes.get("validate");
        if (validate != null) {
            ivy.setValidate(Boolean.valueOf(validate).booleanValue());
        }
        String up2d = (String) attributes.get("checkUpToDate");
        if (up2d != null) {
            Message.deprecated("'checkUpToDate' is deprecated, "
                    + "use the 'overwriteMode' on the 'ivy:retrieve' task instead (" + settings
                    + ")");
            ivy.setCheckUpToDate(Boolean.valueOf(up2d).booleanValue());
        }
        String useRemoteConfig = (String) attributes.get("useRemoteConfig");
        if (useRemoteConfig != null) {
            ivy.setUseRemoteConfig(Boolean.valueOf(useRemoteConfig).booleanValue());
        }
        String cacheIvyPattern = (String) attributes.get("cacheIvyPattern");
        if (cacheIvyPattern != null) {
            Message.deprecated("'cacheIvyPattern' is deprecated, use 'caches[@ivyPattern]' instead"
                    + " (" + settings + ")");
            ivy.setDefaultCacheIvyPattern(cacheIvyPattern);
        }
        String cacheArtPattern = (String) attributes.get("cacheArtifactPattern");
        if (cacheArtPattern != null) {
            Message.deprecated("'cacheArtifactPattern' is deprecated, "
                    + "use 'caches[@artifactPattern]' instead (" + settings + ")");
            ivy.setDefaultCacheArtifactPattern(cacheArtPattern);
        }

        // we do not set following defaults here since no instances has been registered yet
        defaultResolver = (String) attributes.get("defaultResolver");
        defaultCM = (String) attributes.get("defaultConflictManager");
        defaultLatest = (String) attributes.get("defaultLatestStrategy");
        defaultCircular = (String) attributes.get("circularDependencyStrategy");

        String requestMethod = (String) attributes.get("httpRequestMethod");
        if ("head".equalsIgnoreCase(requestMethod)) {
            URLHandlerRegistry.getHttp().setRequestMethod(URLHandler.REQUEST_METHOD_HEAD);
        } else if ("get".equalsIgnoreCase(requestMethod)) {
            URLHandlerRegistry.getHttp().setRequestMethod(URLHandler.REQUEST_METHOD_GET);
        } else if ((requestMethod != null) && (requestMethod.trim().length() > 0)) {
            throw new IllegalArgumentException("Invalid httpRequestMethod specified, must be "
                    + "one of {'HEAD', 'GET'}");
        }
    }

    private void includeStarted(Map attributes) throws IOException, ParseException {
        final IvyVariableContainer variables = ivy.getVariableContainer();
        ivy.setVariableContainer(new IvyVariableContainerWrapper(variables));
        try {
            String propFilePath = (String) attributes.get("file");
            URL settingsURL = null;
            if (propFilePath == null) {
                propFilePath = (String) attributes.get("url");
                if (propFilePath == null) {
                    throw new IllegalArgumentException(
                            "bad include tag: specify file or url to include");
                } else {
                    try {
                        // First asume that it is an absolute URL
                        settingsURL = new URL(propFilePath);
                    } catch (MalformedURLException e) {
                        // If that fail, it may be because it is a relative one.
                        settingsURL = new URL(this.settings, propFilePath);
                    }
                    Message.verbose("including url: " + settingsURL.toString());
                    ivy.setSettingsVariables(settingsURL);
                }
            } else {
                settingsURL = urlFromFileAttribute(propFilePath);
                Message.verbose("including file: " + settingsURL);
                if ("file".equals(settingsURL.getProtocol())) {
                    try {
                        File settingsFile = new File(new URI(settingsURL.toExternalForm()));
                        String optional = (String) attributes.get("optional");
                        if ("true".equals(optional) && !settingsFile.exists()) {
                            return;
                        }

                        ivy.setSettingsVariables(Checks.checkAbsolute(settingsFile,
                            "settings include path"));
                    } catch (URISyntaxException e) {
                        // try to make the best of it...
                        ivy.setSettingsVariables(Checks.checkAbsolute(settingsURL.getPath(),
                            "settings include path"));
                    }
                } else {
                    ivy.setSettingsVariables(settingsURL);
                }
            }
            new XmlSettingsParser(ivy).parse(configurator, settingsURL);
        } finally {
            ivy.setVariableContainer(variables);
        }
    }

    /**
     * Provide an URL referencing the given filepath. If filePath is an absolute path, then the
     * resulting URL point to a local file, otherwise, the filepath is evaluated relatively to the
     * URL of the current settings file (can be local file or remote URL).
     */
    private URL urlFromFileAttribute(String filePath) throws IOException {
        try {
            return new URL(filePath);
        } catch (MalformedURLException e) {
            // ignore, we'll try to create a correct URL below
        }

        File incFile = new File(filePath);
        if (incFile.isAbsolute()) {
            if (!incFile.exists()) {
                throw new FileNotFoundException(incFile.getAbsolutePath());
            }
            return incFile.toURI().toURL();
        } else if ("file".equals(this.settings.getProtocol())) {
            try {
                File settingsFile = new File(new URI(this.settings.toExternalForm()));
                if (!settingsFile.exists()) {
                    throw new FileNotFoundException(settingsFile.getAbsolutePath());
                }
                return new File(settingsFile.getParentFile(), filePath).toURI().toURL();
            } catch (URISyntaxException e) {
                return new URL(this.settings, filePath);
            }
        } else {
            return new URL(this.settings, filePath);
        }
    }

    private void propertiesStarted(Map attributes) throws IOException {
        String propFilePath = (String) attributes.get("file");
        String environmentPrefix = (String) attributes.get("environment");
        if (propFilePath != null) {
            String overrideStr = (String) attributes.get("override");
            boolean override = overrideStr == null ? true : Boolean.valueOf(overrideStr)
                    .booleanValue();
            Message.verbose("loading properties: " + propFilePath);
            try {
                URL fileUrl = urlFromFileAttribute(propFilePath);
                ivy.loadProperties(fileUrl, override);
            } catch (FileNotFoundException e) {
                Message.verbose("Unable to find property file: " + propFilePath);
            }
        } else if (environmentPrefix != null) {
            ivy.getVariableContainer().setEnvironmentPrefix(environmentPrefix);
        } else {
            throw new IllegalArgumentException("Didn't find a 'file' or 'environment' attribute "
                    + "on the 'properties' element");
        }
    }

    private void propertyStarted(Map attributes) {
        String name = (String) attributes.get("name");
        String value = (String) attributes.get("value");
        String override = (String) attributes.get("override");
        String isSetVar = (String) attributes.get("ifset");
        String unlessSetVar = (String) attributes.get("unlessset");
        if (name == null) {
            throw new IllegalArgumentException("missing attribute name on property tag");
        }
        if (value == null) {
            throw new IllegalArgumentException("missing attribute value on property tag");
        }
        ivy.setVariable(name, value, override == null ? true : Boolean.valueOf(override)
                .booleanValue(), isSetVar, unlessSetVar);
    }

    private void typedefStarted(Map attributes) {
        String name = (String) attributes.get("name");
        String className = (String) attributes.get("classname");
        Class clazz = ivy.typeDef(name, className);
        configurator.typeDef(name, clazz);
    }

    private void classpathStarted(Map attributes) throws IOException {
        String urlStr = (String) attributes.get("url");
        URL url = null;
        if (urlStr == null) {
            String file = (String) attributes.get("file");
            if (file == null) {
                throw new IllegalArgumentException(
                        "either url or file should be given for classpath element");
            } else {
                url = urlFromFileAttribute(file);
            }
        } else {
            url = new URL(urlStr);
        }
        ivy.addClasspathURL(url);
    }

    private void inConfiguratorStarted(String qName, Map attributes) {
        if ("macrodef".equals(currentConfiguratorTag) && configurator.getTypeDef(qName) != null) {
            String name = (String) attributes.get("name");
            if (name == null) {
                attributes.put("name", "@{name}");
            } else if (name.indexOf("@{name}") != -1) {
                attributes.put("name", name);
            } else {
                attributes.put("name", "@{name}-" + name);
            }
        }
        if (attributes.get("ref") != null) {
            if (attributes.size() != 1) {
                throw new IllegalArgumentException("ref attribute should be the only one ! found "
                        + attributes.size() + " in " + qName);
            }
            String name = (String) attributes.get("ref");
            Object child = null;
            if ("resolvers".equals(currentConfiguratorTag) || "resolver".equals(qName)) {
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
                    throw new IllegalArgumentException("unknown conflict manager " + name
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
                configurator.setAttribute(attName, (String) attributes.get(attName));
            }
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
            ConflictManager conflictManager = ivy.getConflictManager(ivy.substitute(defaultCM));
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
            RepositoryCacheManager cache = ivy.getRepositoryCacheManager(ivy
                    .substitute(defaultCacheManager));
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
