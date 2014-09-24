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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.NormalRelativeUrlResolver;
import org.apache.ivy.core.RelativeUrlResolver;
import org.apache.ivy.core.cache.CacheUtil;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.DefaultResolutionCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.check.CheckEngineSettings;
import org.apache.ivy.core.deliver.DeliverEngineSettings;
import org.apache.ivy.core.install.InstallEngineSettings;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.id.ModuleRules;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.core.pack.ArchivePacking;
import org.apache.ivy.core.pack.PackingRegistry;
import org.apache.ivy.core.publish.PublishEngineSettings;
import org.apache.ivy.core.repository.RepositoryManagementEngineSettings;
import org.apache.ivy.core.resolve.ResolveEngineSettings;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveEngineSettings;
import org.apache.ivy.core.sort.SortEngineSettings;
import org.apache.ivy.osgi.core.OsgiLatestStrategy;
import org.apache.ivy.plugins.IvySettingsAware;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.circular.ErrorCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.IgnoreCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.WarnCircularDependencyStrategy;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.LatestCompatibleConflictManager;
import org.apache.ivy.plugins.conflict.LatestConflictManager;
import org.apache.ivy.plugins.conflict.NoConflictManager;
import org.apache.ivy.plugins.conflict.StrictConflictManager;
import org.apache.ivy.plugins.latest.LatestLexicographicStrategy;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.ivy.plugins.lock.CreateFileLockStrategy;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.lock.NIOFileLockStrategy;
import org.apache.ivy.plugins.lock.NoLockStrategy;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.MapMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.matcher.RegexpPatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.report.LogReportOutputter;
import org.apache.ivy.plugins.report.ReportOutputter;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.DualResolver;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.plugins.signer.SignatureGenerator;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.plugins.version.ChainVersionMatcher;
import org.apache.ivy.plugins.version.ExactVersionMatcher;
import org.apache.ivy.plugins.version.LatestVersionMatcher;
import org.apache.ivy.plugins.version.SubVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.plugins.version.VersionRangeMatcher;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.url.URLHandlerRegistry;

public class IvySettings implements SortEngineSettings, PublishEngineSettings, ParserSettings,
        DeliverEngineSettings, CheckEngineSettings, InstallEngineSettings, ResolverSettings,
        ResolveEngineSettings, RetrieveEngineSettings, RepositoryManagementEngineSettings {
    private static final long INTERUPT_TIMEOUT = 2000;

    private Map typeDefs = new HashMap();

    private Map resolversMap = new HashMap();

    private DependencyResolver defaultResolver;

    private DependencyResolver dictatorResolver = null;

    private String defaultResolverName;

    private File defaultCache;

    private String defaultBranch = null;

    private boolean checkUpToDate = true;

    private ModuleRules moduleSettings = new ModuleRules();

    // Map (String conflictManagerName -> ConflictManager)
    private Map conflictsManager = new HashMap();

    // Map (String latestStrategyName -> LatestStrategy)
    private Map latestStrategies = new HashMap();

    // Map (String name -> LockStrategy)
    private Map lockStrategies = new HashMap();

    // Map (String namespaceName -> Namespace)
    private Map namespaces = new HashMap();

    // Map (String matcherName -> Matcher)
    private Map matchers = new HashMap();

    // Map (String outputterName -> ReportOutputter)
    private Map reportOutputters = new HashMap();

    // Map (String matcherName -> VersionMatcher)
    private Map versionMatchers = new HashMap();

    // Map (String name -> CircularDependencyStrategy)
    private Map circularDependencyStrategies = new HashMap();

    // Map (String name -> RepositoryCacheManager)
    private Map repositoryCacheManagers = new HashMap();

    // Map (String name -> SignatureGenerator)
    private Map signatureGenerators = new HashMap();

    // List (Trigger)
    private List triggers = new ArrayList();

    private IvyVariableContainer variableContainer = new IvyVariableContainerImpl();

    private boolean validate = true;

    private LatestStrategy defaultLatestStrategy = null;

    private LockStrategy defaultLockStrategy = null;

    private ConflictManager defaultConflictManager = null;

    private CircularDependencyStrategy circularDependencyStrategy = null;

    private RepositoryCacheManager defaultRepositoryCacheManager = null;

    private ResolutionCacheManager resolutionCacheManager = null;

    private List listingIgnore = new ArrayList();

    private boolean repositoriesConfigured;

    private boolean useRemoteConfig = false;

    private File defaultUserDir;

    private File baseDir = new File(".").getAbsoluteFile();

    private List classpathURLs = new ArrayList();

    private ClassLoader classloader;

    private Boolean debugConflictResolution;

    private boolean logNotConvertedExclusionRule;

    private VersionMatcher versionMatcher;

    private StatusManager statusManager;

    private Boolean debugLocking;

    private Boolean dumpMemoryUsage;

    private String defaultCacheIvyPattern;

    private String defaultCacheArtifactPattern;

    private boolean defaultUseOrigin;

    private String defaultResolveMode = ResolveOptions.RESOLVEMODE_DEFAULT;

    private PackingRegistry packingRegistry = new PackingRegistry();

    public IvySettings() {
        this(new IvyVariableContainerImpl());
    }

    public IvySettings(IvyVariableContainer variableContainer) {
        setVariableContainer(variableContainer);
        setVariable("ivy.default.settings.dir", getDefaultSettingsDir(), true);
        setVariable("ivy.basedir", getBaseDir().getAbsolutePath());
        setDeprecatedVariable("ivy.default.conf.dir", "ivy.default.settings.dir");

        String ivyTypeDefs = System.getProperty("ivy.typedef.files");
        if (ivyTypeDefs != null) {
            String[] files = ivyTypeDefs.split("\\,");
            for (int i = 0; i < files.length; i++) {
                try {
                    typeDefs(
                        new FileInputStream(Checks.checkAbsolute(files[i].trim(),
                            "ivy.typedef.files")), true);
                } catch (FileNotFoundException e) {
                    Message.warn("typedefs file not found: " + files[i].trim());
                } catch (IOException e) {
                    Message.warn("problem with typedef file: " + files[i].trim(), e);
                }
            }
        } else {
            try {
                typeDefs(getSettingsURL("typedef.properties").openStream(), true);
            } catch (IOException e) {
                Message.warn("impossible to load default type defs", e);
            }
        }
        LatestLexicographicStrategy latestLexicographicStrategy = new LatestLexicographicStrategy();
        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        LatestTimeStrategy latestTimeStrategy = new LatestTimeStrategy();
        OsgiLatestStrategy osgiLatestStrategy = new OsgiLatestStrategy();

        addLatestStrategy("latest-revision", latestRevisionStrategy);
        addLatestStrategy("latest-lexico", latestLexicographicStrategy);
        addLatestStrategy("latest-time", latestTimeStrategy);
        addLatestStrategy("latest-osgi", osgiLatestStrategy);

        addLockStrategy("no-lock", new NoLockStrategy());
        addLockStrategy("artifact-lock", new CreateFileLockStrategy(debugLocking()));
        addLockStrategy("artifact-lock-nio", new NIOFileLockStrategy(debugLocking()));

        addConflictManager("latest-revision", new LatestConflictManager("latest-revision",
                latestRevisionStrategy));
        addConflictManager("latest-compatible", new LatestCompatibleConflictManager(
                "latest-compatible", latestRevisionStrategy));
        addConflictManager("latest-time", new LatestConflictManager("latest-time",
                latestTimeStrategy));
        addConflictManager("all", new NoConflictManager());
        addConflictManager("strict", new StrictConflictManager());

        addMatcher(ExactPatternMatcher.INSTANCE);
        addMatcher(RegexpPatternMatcher.INSTANCE);
        addMatcher(ExactOrRegexpPatternMatcher.INSTANCE);

        try {
            // GlobPatternMatcher is optional. Only add it when available.
            Class globClazz = IvySettings.class.getClassLoader().loadClass(
                "org.apache.ivy.plugins.matcher.GlobPatternMatcher");
            Field instanceField = globClazz.getField("INSTANCE");
            addMatcher((PatternMatcher) instanceField.get(null));
        } catch (Exception e) {
            // ignore: the matcher isn't on the classpath
            Message.info("impossible to define glob matcher: "
                    + "org.apache.ivy.plugins.matcher.GlobPatternMatcher was not found", e);
        }

        addReportOutputter(new LogReportOutputter());
        addReportOutputter(new XmlReportOutputter());

        configureDefaultCircularDependencyStrategies();

        listingIgnore.add(".cvsignore");
        listingIgnore.add("CVS");
        listingIgnore.add(".svn");
        listingIgnore.add("maven-metadata.xml");
        listingIgnore.add("maven-metadata.xml.md5");
        listingIgnore.add("maven-metadata.xml.sha1");

        addSystemProperties();
    }

    private synchronized void addSystemProperties() {
        try {
            addAllVariables((Map) System.getProperties().clone());
        } catch (AccessControlException ex) {
            Message.verbose("access denied to getting all system properties: they won't be available as Ivy variables."
                    + "\nset " + ex.getPermission() + " permission if you want to access them");
        }
    }

    /**
     * Call this method to ask ivy to configure some variables using either a remote or a local
     * properties file
     */
    public synchronized void configureRepositories(boolean remote) {
        if (!repositoriesConfigured) {
            Properties props = new Properties();
            boolean configured = false;
            if (useRemoteConfig && remote) {
                try {
                    URL url = new URL("http://ant.apache.org/ivy/repository.properties");
                    Message.verbose("configuring repositories with " + url);
                    props.load(URLHandlerRegistry.getDefault().openStream(url));
                    configured = true;
                } catch (Exception ex) {
                    Message.verbose("unable to use remote repository configuration", ex);
                    props = new Properties();
                }
            }
            if (!configured) {
                InputStream repositoryPropsStream = null;
                try {
                    repositoryPropsStream = getSettingsURL("repository.properties").openStream();
                    props.load(repositoryPropsStream);
                } catch (IOException e) {
                    Message.error("unable to use internal repository configuration", e);
                    if (repositoryPropsStream != null) {
                        try {
                            repositoryPropsStream.close();
                        } catch (Exception ex) {
                            // nothing to do
                        }
                    }
                }
            }
            addAllVariables(props, false);
            repositoriesConfigured = true;
        }
    }

    public synchronized void typeDefs(InputStream stream) throws IOException {
        typeDefs(stream, false);
    }

    public synchronized void typeDefs(InputStream stream, boolean silentFail) throws IOException {
        try {
            Properties p = new Properties();
            p.load(stream);
            typeDefs(p, silentFail);
        } finally {
            stream.close();
        }
    }

    public synchronized void typeDefs(Properties p) {
        typeDefs(p, false);
    }

    public synchronized void typeDefs(Properties p, boolean silentFail) {
        for (Iterator iter = p.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            typeDef(name, p.getProperty(name), silentFail);
        }
    }

    public synchronized void load(File settingsFile) throws ParseException, IOException {
        Message.info(":: loading settings :: file = " + settingsFile);
        long start = System.currentTimeMillis();
        setSettingsVariables(settingsFile);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(Checks.checkAbsolute(getVariable("ivy.default.ivy.user.dir"),
                "ivy.default.ivy.user.dir"));
        } else {
            getDefaultIvyUserDir();
        }

        loadDefaultProperties();
        try {
            new XmlSettingsParser(this).parse(settingsFile.toURI().toURL());
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "given file cannot be transformed to url: " + settingsFile);
            iae.initCause(e);
            throw iae;
        }
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        Message.verbose("settings loaded (" + (System.currentTimeMillis() - start) + "ms)");
        dumpSettings();
    }

    public synchronized void load(URL settingsURL) throws ParseException, IOException {
        Message.info(":: loading settings :: url = " + settingsURL);
        long start = System.currentTimeMillis();
        setSettingsVariables(settingsURL);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(Checks.checkAbsolute(getVariable("ivy.default.ivy.user.dir"),
                "ivy.default.ivy.user.dir"));
        } else {
            getDefaultIvyUserDir();
        }

        loadDefaultProperties();
        new XmlSettingsParser(this).parse(settingsURL);
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        Message.verbose("settings loaded (" + (System.currentTimeMillis() - start) + "ms)");
        dumpSettings();
    }

    /**
     * Default initialization of settings, useful when you don't want to load your settings from a
     * settings file or URL, but prefer to set them manually. By calling this method you will still
     * have the basic initialization done when loading settings.
     * 
     * @throws IOException
     */
    public synchronized void defaultInit() throws IOException {
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(Checks.checkAbsolute(getVariable("ivy.default.ivy.user.dir"),
                "ivy.default.ivy.user.dir"));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();

        loadDefaultProperties();
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        dumpSettings();
    }

    public synchronized void loadDefault() throws ParseException, IOException {
        load(getDefaultSettingsURL());
    }

    public synchronized void loadDefault14() throws ParseException, IOException {
        load(getDefault14SettingsURL());
    }

    private void loadDefaultProperties() throws IOException {
        loadProperties(getDefaultPropertiesURL(), false);
    }

    public static URL getDefaultPropertiesURL() {
        return getSettingsURL("ivy.properties");
    }

    public static URL getDefaultSettingsURL() {
        return getSettingsURL("ivysettings.xml");
    }

    public static URL getDefault14SettingsURL() {
        return getSettingsURL("ivysettings-1.4.xml");
    }

    private String getDefaultSettingsDir() {
        String ivysettingsLocation = getDefaultSettingsURL().toExternalForm();
        return ivysettingsLocation.substring(0,
            ivysettingsLocation.length() - "ivysettings.xml".length() - 1);
    }

    private static URL getSettingsURL(String file) {
        return XmlSettingsParser.class.getResource(file);
    }

    public synchronized void setSettingsVariables(File settingsFile) {
        try {
            setVariable("ivy.settings.dir", new File(settingsFile.getAbsolutePath()).getParent());
            setDeprecatedVariable("ivy.conf.dir", "ivy.settings.dir");
            setVariable("ivy.settings.file", settingsFile.getAbsolutePath());
            setDeprecatedVariable("ivy.conf.file", "ivy.settings.file");
            setVariable("ivy.settings.url", settingsFile.toURI().toURL().toExternalForm());
            setDeprecatedVariable("ivy.conf.url", "ivy.settings.url");
            setVariable("ivy.settings.dir.url", new File(settingsFile.getAbsolutePath())
                    .getParentFile().toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "given file cannot be transformed to url: " + settingsFile);
            iae.initCause(e);
            throw iae;
        }
    }

    /**
     * Sets a deprecated variable with the value of the new variable
     * 
     * @param deprecatedKey
     *            the deprecated variable name
     * @param newKey
     *            the new variable name
     */
    private void setDeprecatedVariable(String deprecatedKey, String newKey) {
        setVariable(deprecatedKey, getVariable(newKey));
    }

    public synchronized void setSettingsVariables(URL settingsURL) {
        String settingsURLStr = settingsURL.toExternalForm();
        setVariable("ivy.settings.url", settingsURLStr);
        setDeprecatedVariable("ivy.conf.url", "ivy.settings.url");
        int slashIndex = settingsURLStr.lastIndexOf('/');
        if (slashIndex != -1) {
            String dirUrl = settingsURLStr.substring(0, slashIndex);
            setVariable("ivy.settings.dir", dirUrl);
            setVariable("ivy.settings.dir.url", dirUrl);
            setDeprecatedVariable("ivy.conf.dir", "ivy.settings.dir");
        } else {
            Message.warn("settings url does not contain any slash (/): "
                    + "ivy.settings.dir variable not set");
        }
    }

    private void dumpSettings() {
        Message.verbose("\tdefault cache: " + getDefaultCache());
        Message.verbose("\tdefault resolver: " + getDefaultResolver());
        Message.debug("\tdefault latest strategy: " + getDefaultLatestStrategy());
        Message.debug("\tdefault conflict manager: " + getDefaultConflictManager());
        Message.debug("\tcircular dependency strategy: " + getCircularDependencyStrategy());
        Message.debug("\tvalidate: " + doValidate());
        Message.debug("\tcheck up2date: " + isCheckUpToDate());

        if (!classpathURLs.isEmpty()) {
            Message.verbose("\t-- " + classpathURLs.size() + " custom classpath urls:");
            for (Iterator iter = classpathURLs.iterator(); iter.hasNext();) {
                Message.debug("\t\t" + iter.next());
            }
        }
        Message.verbose("\t-- " + resolversMap.size() + " resolvers:");
        for (Iterator iter = resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            resolver.dumpSettings();
        }
        Message.debug("\tmodule settings:");
        moduleSettings.dump("\t\t");
    }

    public synchronized void loadProperties(URL url) throws IOException {
        loadProperties(url, true);
    }

    public synchronized void loadProperties(URL url, boolean overwrite) throws IOException {
        loadProperties(url.openStream(), overwrite);
    }

    public synchronized void loadProperties(File file) throws IOException {
        loadProperties(file, true);
    }

    public synchronized void loadProperties(File file, boolean overwrite) throws IOException {
        loadProperties(new FileInputStream(file), overwrite);
    }

    private void loadProperties(InputStream stream, boolean overwrite) throws IOException {
        try {
            Properties properties = new Properties();
            properties.load(stream);
            addAllVariables(properties, overwrite);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // nothing
                }
            }
        }
    }

    public synchronized void setVariable(String varName, String value) {
        setVariable(varName, value, true);
    }

    public synchronized void setVariable(String varName, String value, boolean overwrite) {
        setVariable(varName, value, overwrite, null, null);
    }

    public synchronized void setVariable(String varName, String value, boolean overwrite,
            String ifSetVar, String unlessSetVar) {
        if (ifSetVar != null && variableContainer.getVariable(ifSetVar) == null) {
            Message.verbose("Not setting '" + varName + "' to '" + value + "' since '" + ifSetVar
                    + "' is not set.");
            return;
        }
        if (unlessSetVar != null && variableContainer.getVariable(unlessSetVar) != null) {
            Message.verbose("Not setting '" + varName + "' to '" + value + "' since '"
                    + unlessSetVar + "' is set.");
            return;
        }
        variableContainer.setVariable(varName, value, overwrite);
    }

    public synchronized void addAllVariables(Map variables) {
        addAllVariables(variables, true);
    }

    public synchronized void addAllVariables(Map<?, ?> variables, boolean overwrite) {
        for (Map.Entry<?, ?> entry : variables.entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (val == null || val instanceof String) {
                setVariable(key, (String) val, overwrite);
            }
        }
    }

    /**
     * Substitute variables in the given string by their value found in the current set of variables
     * 
     * @param str
     *            the string in which substitution should be made
     * @return the string where all current ivy variables have been substituted by their value If
     *         the input str doesn't use any variable, the same object is returned
     */
    public synchronized String substitute(String str) {
        return IvyPatternHelper.substituteVariables(str, variableContainer);
    }

    /**
     * Substitute variables in the given map values by their value found in the current set of
     * variables
     * 
     * @param strings
     *            the map of strings in which substitution should be made
     * @return a new map of strings in which all current ivy variables in values have been
     *         substituted by their value
     */
    public synchronized Map/* <String, String> */substitute(Map/* <String, String> */strings) {
        Map substituted = new LinkedHashMap();
        for (Iterator it = strings.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            substituted.put(entry.getKey(), substitute((String) entry.getValue()));
        }
        return substituted;
    }

    /**
     * Returns the variables loaded in configuration file. Those variables may better be seen as ant
     * properties
     * 
     * @return
     */
    public synchronized IvyVariableContainer getVariables() {
        return variableContainer;
    }

    public synchronized Class typeDef(String name, String className) {
        return typeDef(name, className, false);
    }

    public synchronized Class typeDef(String name, String className, boolean silentFail) {
        Class clazz = classForName(className, silentFail);
        if (clazz != null) {
            typeDefs.put(name, clazz);
        }
        return clazz;
    }

    private Class classForName(String className, boolean silentFail) {
        try {
            return getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            if (silentFail) {
                Message.info("impossible to define new type: class not found: " + className
                        + " in " + classpathURLs + " nor Ivy classloader");
                return null;
            } else {
                throw new RuntimeException("impossible to define new type: class not found: "
                        + className + " in " + classpathURLs + " nor Ivy classloader");
            }
        }
    }

    private ClassLoader getClassLoader() {
        if (classloader == null) {
            if (classpathURLs.isEmpty()) {
                classloader = Ivy.class.getClassLoader();
            } else {
                classloader = new URLClassLoader(
                        (URL[]) classpathURLs.toArray(new URL[classpathURLs.size()]),
                        Ivy.class.getClassLoader());
            }
        }
        return classloader;
    }

    public synchronized void addClasspathURL(URL url) {
        classpathURLs.add(url);
        classloader = null;
    }

    public synchronized Map getTypeDefs() {
        return typeDefs;
    }

    public synchronized Class getTypeDef(String name) {
        return (Class) typeDefs.get(name);
    }

    // methods which match ivy conf method signature specs
    public synchronized void addConfigured(DependencyResolver resolver) {
        addResolver(resolver);
    }

    public synchronized void addConfigured(ModuleDescriptorParser parser) {
        ModuleDescriptorParserRegistry.getInstance().addParser(parser);
    }

    public synchronized void addConfigured(SignatureGenerator generator) {
        addSignatureGenerator(generator);
    }

    public synchronized void addSignatureGenerator(SignatureGenerator generator) {
        init(generator);
        signatureGenerators.put(generator.getName(), generator);
    }

    public synchronized SignatureGenerator getSignatureGenerator(String name) {
        return (SignatureGenerator) signatureGenerators.get(name);
    }

    public synchronized void addResolver(DependencyResolver resolver) {
        if (resolver == null) {
            throw new NullPointerException("null resolver");
        }
        init(resolver);
        resolversMap.put(resolver.getName(), resolver);
        if (resolver instanceof ChainResolver) {
            List subresolvers = ((ChainResolver) resolver).getResolvers();
            for (Iterator iter = subresolvers.iterator(); iter.hasNext();) {
                DependencyResolver dr = (DependencyResolver) iter.next();
                addResolver(dr);
            }
        } else if (resolver instanceof DualResolver) {
            DependencyResolver ivyResolver = ((DualResolver) resolver).getIvyResolver();
            if (ivyResolver != null) {
                addResolver(ivyResolver);
            }
            DependencyResolver artifactResolver = ((DualResolver) resolver).getArtifactResolver();
            if (artifactResolver != null) {
                addResolver(artifactResolver);
            }
        }
    }

    public synchronized void setDefaultCache(File cacheDirectory) {
        setVariable("ivy.cache.dir", cacheDirectory.getAbsolutePath(), false);
        defaultCache = cacheDirectory;
        if (defaultRepositoryCacheManager != null) {
            if ("default-cache".equals(defaultRepositoryCacheManager.getName())
                    && defaultRepositoryCacheManager instanceof DefaultRepositoryCacheManager) {
                ((DefaultRepositoryCacheManager) defaultRepositoryCacheManager)
                        .setBasedir(defaultCache);
            }
        }
    }

    public synchronized void setDefaultResolver(String resolverName) {
        checkResolverName(resolverName);
        if (resolverName != null && !resolverName.equals(defaultResolverName)) {
            defaultResolver = null;
        }
        defaultResolverName = resolverName;
    }

    private void checkResolverName(String resolverName) {
        if (resolverName != null && !resolversMap.containsKey(resolverName)) {
            throw new IllegalArgumentException("no resolver found called " + resolverName
                    + ": check your settings");
        }
    }

    /**
     * regular expressions as explained in Pattern class may be used in attributes
     */
    public synchronized void addModuleConfiguration(Map attributes, PatternMatcher matcher,
            String resolverName, String branch, String conflictManager, String resolveMode) {
        checkResolverName(resolverName);
        moduleSettings.defineRule(new MapMatcher(attributes, matcher), new ModuleSettings(
                resolverName, branch, conflictManager, resolveMode));
    }

    /**
     * Return the canonical form of a filename.
     * <p>
     * If the specified file name is relative it is resolved with respect to the settings's base
     * directory.
     * 
     * @param fileName
     *            The name of the file to resolve. Must not be <code>null</code>.
     * 
     * @return the resolved File.
     * 
     */
    public synchronized File resolveFile(String fileName) {
        return FileUtil.resolveFile(baseDir, fileName);
    }

    public synchronized void setBaseDir(File baseDir) {
        this.baseDir = baseDir.getAbsoluteFile();
        setVariable("ivy.basedir", this.baseDir.getAbsolutePath());
        setVariable("basedir", this.baseDir.getAbsolutePath(), false);
    }

    public synchronized File getBaseDir() {
        return baseDir;
    }

    public synchronized File getDefaultIvyUserDir() {
        if (defaultUserDir == null) {
            if (getVariable("ivy.home") != null) {
                setDefaultIvyUserDir(Checks.checkAbsolute(getVariable("ivy.home"), "ivy.home"));
                Message.verbose("using ivy.default.ivy.user.dir variable for default ivy user dir: "
                        + defaultUserDir);
            } else {
                setDefaultIvyUserDir(new File(System.getProperty("user.home"), ".ivy2"));
                Message.verbose("no default ivy user dir defined: set to " + defaultUserDir);
            }
        }
        return defaultUserDir;
    }

    public synchronized void setDefaultIvyUserDir(File defaultUserDir) {
        this.defaultUserDir = defaultUserDir;
        setVariable("ivy.default.ivy.user.dir", this.defaultUserDir.getAbsolutePath());
        setVariable("ivy.home", this.defaultUserDir.getAbsolutePath());
    }

    public synchronized File getDefaultCache() {
        if (defaultCache == null) {
            String cache = getVariable("ivy.cache.dir");
            if (cache != null) {
                defaultCache = Checks.checkAbsolute(cache, "ivy.cache.dir");
            } else {
                setDefaultCache(new File(getDefaultIvyUserDir(), "cache"));
                Message.verbose("no default cache defined: set to " + defaultCache);
            }
        }
        return defaultCache;
    }

    public synchronized void setDefaultRepositoryCacheBasedir(String repositoryCacheRoot) {
        setVariable("ivy.cache.repository", repositoryCacheRoot, true);
        if (defaultRepositoryCacheManager != null
                && "default-cache".equals(defaultRepositoryCacheManager.getName())
                && defaultRepositoryCacheManager instanceof DefaultRepositoryCacheManager) {
            ((DefaultRepositoryCacheManager) defaultRepositoryCacheManager)
                    .setBasedir(getDefaultRepositoryCacheBasedir());
        }
    }

    public synchronized void setDefaultResolutionCacheBasedir(String resolutionCacheRoot) {
        setVariable("ivy.cache.resolution", resolutionCacheRoot, true);
        if (resolutionCacheManager != null
                && resolutionCacheManager instanceof DefaultResolutionCacheManager) {
            ((DefaultResolutionCacheManager) resolutionCacheManager)
                    .setBasedir(getDefaultResolutionCacheBasedir());
        }
    }

    public synchronized File getDefaultRepositoryCacheBasedir() {
        String repositoryCacheRoot = getVariable("ivy.cache.repository");
        if (repositoryCacheRoot != null) {
            return Checks.checkAbsolute(repositoryCacheRoot, "ivy.cache.repository");
        } else {
            return getDefaultCache();
        }
    }

    public synchronized File getDefaultResolutionCacheBasedir() {
        String resolutionCacheRoot = getVariable("ivy.cache.resolution");
        if (resolutionCacheRoot != null) {
            return Checks.checkAbsolute(resolutionCacheRoot, "ivy.cache.resolution");
        } else {
            return getDefaultCache();
        }
    }

    public synchronized void setDictatorResolver(DependencyResolver resolver) {
        dictatorResolver = resolver;
    }

    public synchronized DependencyResolver getResolver(ModuleRevisionId mrid) {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        String resolverName = getResolverName(mrid);
        return getResolver(resolverName);
    }

    public synchronized boolean hasResolver(String resolverName) {
        return resolversMap.containsKey(resolverName);
    }

    public synchronized DependencyResolver getResolver(String resolverName) {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        DependencyResolver resolver = (DependencyResolver) resolversMap.get(resolverName);
        if (resolver == null) {
            Message.error("unknown resolver " + resolverName);
        }
        return resolver;
    }

    public synchronized DependencyResolver getDefaultResolver() {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        if (defaultResolver == null) {
            defaultResolver = (DependencyResolver) resolversMap.get(defaultResolverName);
        }
        return defaultResolver;
    }

    public synchronized String getResolverName(ModuleRevisionId mrid) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(mrid, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getResolverName() != null;
            }
        });
        return ms == null ? defaultResolverName : ms.getResolverName();
    }

    public synchronized String getDefaultBranch(ModuleId moduleId) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(moduleId, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getBranch() != null;
            }
        });
        return ms == null ? getDefaultBranch() : ms.getBranch();
    }

    public synchronized String getDefaultBranch() {
        return defaultBranch;
    }

    public synchronized void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public synchronized ConflictManager getConflictManager(ModuleId moduleId) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(moduleId, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getConflictManager() != null;
            }
        });
        if (ms == null) {
            return getDefaultConflictManager();
        } else {
            ConflictManager cm = getConflictManager(ms.getConflictManager());
            if (cm == null) {
                throw new IllegalStateException("ivy badly configured: unknown conflict manager "
                        + ms.getConflictManager());
            }
            return cm;
        }
    }

    public synchronized String getResolveMode(ModuleId moduleId) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(moduleId, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getResolveMode() != null;
            }
        });
        return ms == null ? getDefaultResolveMode() : ms.getResolveMode();
    }

    public synchronized String getDefaultResolveMode() {
        return defaultResolveMode;
    }

    public synchronized void setDefaultResolveMode(String defaultResolveMode) {
        this.defaultResolveMode = defaultResolveMode;
    }

    public synchronized void addConfigured(ConflictManager cm) {
        addConflictManager(cm.getName(), cm);
    }

    public synchronized ConflictManager getConflictManager(String name) {
        if ("default".equals(name)) {
            return getDefaultConflictManager();
        }
        return (ConflictManager) conflictsManager.get(name);
    }

    public synchronized void addConflictManager(String name, ConflictManager cm) {
        init(cm);
        conflictsManager.put(name, cm);
    }

    public synchronized void addConfigured(LatestStrategy latest) {
        addLatestStrategy(latest.getName(), latest);
    }

    public synchronized LatestStrategy getLatestStrategy(String name) {
        if ("default".equals(name)) {
            return getDefaultLatestStrategy();
        }
        return (LatestStrategy) latestStrategies.get(name);
    }

    public synchronized void addLatestStrategy(String name, LatestStrategy latest) {
        init(latest);
        latestStrategies.put(name, latest);
    }

    public synchronized void addConfigured(LockStrategy lockStrategy) {
        addLockStrategy(lockStrategy.getName(), lockStrategy);
    }

    public synchronized LockStrategy getLockStrategy(String name) {
        if ("default".equals(name)) {
            return getDefaultLockStrategy();
        }
        return (LockStrategy) lockStrategies.get(name);
    }

    public synchronized void addLockStrategy(String name, LockStrategy lockStrategy) {
        init(lockStrategy);
        lockStrategies.put(name, lockStrategy);
    }

    public synchronized void addConfigured(Namespace ns) {
        addNamespace(ns);
    }

    public synchronized Namespace getNamespace(String name) {
        if ("system".equals(name)) {
            return getSystemNamespace();
        }
        return (Namespace) namespaces.get(name);
    }

    public final Namespace getSystemNamespace() {
        return Namespace.SYSTEM_NAMESPACE;
    }

    public synchronized void addNamespace(Namespace ns) {
        init(ns);
        namespaces.put(ns.getName(), ns);
    }

    public synchronized void addConfigured(PatternMatcher m) {
        addMatcher(m);
    }

    public synchronized PatternMatcher getMatcher(String name) {
        return (PatternMatcher) matchers.get(name);
    }

    public synchronized void addMatcher(PatternMatcher m) {
        init(m);
        matchers.put(m.getName(), m);
    }

    public synchronized void addConfigured(RepositoryCacheManager c) {
        addRepositoryCacheManager(c);
    }

    public synchronized RepositoryCacheManager getRepositoryCacheManager(String name) {
        return (RepositoryCacheManager) repositoryCacheManagers.get(name);
    }

    public synchronized void addRepositoryCacheManager(RepositoryCacheManager c) {
        init(c);
        repositoryCacheManagers.put(c.getName(), c);
    }

    public synchronized RepositoryCacheManager[] getRepositoryCacheManagers() {
        return (RepositoryCacheManager[]) repositoryCacheManagers.values().toArray(
            new RepositoryCacheManager[repositoryCacheManagers.size()]);
    }

    public synchronized void addConfigured(ReportOutputter outputter) {
        addReportOutputter(outputter);
    }

    public synchronized ReportOutputter getReportOutputter(String name) {
        return (ReportOutputter) reportOutputters.get(name);
    }

    public synchronized void addReportOutputter(ReportOutputter outputter) {
        init(outputter);
        reportOutputters.put(outputter.getName(), outputter);
    }

    public synchronized ReportOutputter[] getReportOutputters() {
        return (ReportOutputter[]) reportOutputters.values().toArray(
            new ReportOutputter[reportOutputters.size()]);
    }

    public synchronized void addConfigured(VersionMatcher vmatcher) {
        addVersionMatcher(vmatcher);
    }

    public synchronized VersionMatcher getVersionMatcher(String name) {
        return (VersionMatcher) versionMatchers.get(name);
    }

    public synchronized void addVersionMatcher(VersionMatcher vmatcher) {
        init(vmatcher);
        versionMatchers.put(vmatcher.getName(), vmatcher);

        if (versionMatcher == null) {
            versionMatcher = new ChainVersionMatcher();
            addVersionMatcher(new ExactVersionMatcher());
        }
        if (versionMatcher instanceof ChainVersionMatcher) {
            ChainVersionMatcher chain = (ChainVersionMatcher) versionMatcher;
            chain.add(vmatcher);
        }
    }

    public synchronized VersionMatcher[] getVersionMatchers() {
        return (VersionMatcher[]) versionMatchers.values().toArray(
            new VersionMatcher[versionMatchers.size()]);
    }

    public synchronized VersionMatcher getVersionMatcher() {
        if (versionMatcher == null) {
            configureDefaultVersionMatcher();
        }
        return versionMatcher;
    }

    public synchronized void configureDefaultVersionMatcher() {
        addVersionMatcher(new LatestVersionMatcher());
        addVersionMatcher(new SubVersionMatcher());
        addVersionMatcher(new VersionRangeMatcher());
    }

    public synchronized CircularDependencyStrategy getCircularDependencyStrategy() {
        if (circularDependencyStrategy == null) {
            circularDependencyStrategy = getCircularDependencyStrategy("default");
        }
        return circularDependencyStrategy;
    }

    public synchronized CircularDependencyStrategy getCircularDependencyStrategy(String name) {
        if ("default".equals(name)) {
            name = "warn";
        }
        return (CircularDependencyStrategy) circularDependencyStrategies.get(name);
    }

    public synchronized void setCircularDependencyStrategy(CircularDependencyStrategy strategy) {
        circularDependencyStrategy = strategy;
    }

    public synchronized void addConfigured(CircularDependencyStrategy strategy) {
        addCircularDependencyStrategy(strategy);
    }

    private void addCircularDependencyStrategy(CircularDependencyStrategy strategy) {
        circularDependencyStrategies.put(strategy.getName(), strategy);
    }

    private void configureDefaultCircularDependencyStrategies() {
        addCircularDependencyStrategy(WarnCircularDependencyStrategy.getInstance());
        addCircularDependencyStrategy(ErrorCircularDependencyStrategy.getInstance());
        addCircularDependencyStrategy(IgnoreCircularDependencyStrategy.getInstance());
    }

    public synchronized StatusManager getStatusManager() {
        if (statusManager == null) {
            statusManager = StatusManager.newDefaultInstance();
        }
        return statusManager;
    }

    public void setStatusManager(StatusManager statusManager) {
        this.statusManager = statusManager;
    }

    /**
     * Returns the file names of the files that should be ignored when creating a file listing.
     */
    public synchronized String[] getIgnorableFilenames() {
        return (String[]) listingIgnore.toArray(new String[listingIgnore.size()]);
    }

    /**
     * Filters the names list by removing all names that should be ignored as defined by the listing
     * ignore list
     * 
     * @param names
     */
    public synchronized void filterIgnore(Collection names) {
        names.removeAll(listingIgnore);
    }

    public synchronized boolean isCheckUpToDate() {
        return checkUpToDate;
    }

    public synchronized void setCheckUpToDate(boolean checkUpToDate) {
        this.checkUpToDate = checkUpToDate;
    }

    public synchronized boolean doValidate() {
        return validate;
    }

    public synchronized void setValidate(boolean validate) {
        this.validate = validate;
    }

    public synchronized String getVariable(String name) {
        return variableContainer.getVariable(name);
    }

    public synchronized ConflictManager getDefaultConflictManager() {
        if (defaultConflictManager == null) {
            defaultConflictManager = new LatestConflictManager(getDefaultLatestStrategy());
            ((LatestConflictManager) defaultConflictManager).setSettings(this);
        }
        return defaultConflictManager;
    }

    public synchronized void setDefaultConflictManager(ConflictManager defaultConflictManager) {
        this.defaultConflictManager = defaultConflictManager;
    }

    public synchronized LatestStrategy getDefaultLatestStrategy() {
        if (defaultLatestStrategy == null) {
            defaultLatestStrategy = new LatestRevisionStrategy();
        }
        return defaultLatestStrategy;
    }

    public synchronized void setDefaultLatestStrategy(LatestStrategy defaultLatestStrategy) {
        this.defaultLatestStrategy = defaultLatestStrategy;
    }

    public synchronized LockStrategy getDefaultLockStrategy() {
        if (defaultLockStrategy == null) {
            defaultLockStrategy = new NoLockStrategy();
        }
        return defaultLockStrategy;
    }

    public synchronized void setDefaultLockStrategy(LockStrategy defaultLockStrategy) {
        this.defaultLockStrategy = defaultLockStrategy;
    }

    public synchronized RepositoryCacheManager getDefaultRepositoryCacheManager() {
        if (defaultRepositoryCacheManager == null) {
            defaultRepositoryCacheManager = new DefaultRepositoryCacheManager("default-cache",
                    this, getDefaultRepositoryCacheBasedir());
            addRepositoryCacheManager(defaultRepositoryCacheManager);
        }
        return defaultRepositoryCacheManager;
    }

    public synchronized void setDefaultRepositoryCacheManager(RepositoryCacheManager cache) {
        this.defaultRepositoryCacheManager = cache;
    }

    public synchronized ResolutionCacheManager getResolutionCacheManager() {
        if (resolutionCacheManager == null) {
            resolutionCacheManager = new DefaultResolutionCacheManager(
                    getDefaultResolutionCacheBasedir());
            init(resolutionCacheManager);
        }
        return resolutionCacheManager;
    }

    public synchronized void setResolutionCacheManager(ResolutionCacheManager resolutionCacheManager) {
        this.resolutionCacheManager = resolutionCacheManager;
    }

    public synchronized void addTrigger(Trigger trigger) {
        init(trigger);
        triggers.add(trigger);
    }

    public synchronized List getTriggers() {
        return triggers;
    }

    public synchronized void addConfigured(Trigger trigger) {
        addTrigger(trigger);
    }

    public synchronized boolean isUseRemoteConfig() {
        return useRemoteConfig;
    }

    public synchronized void setUseRemoteConfig(boolean useRemoteConfig) {
        this.useRemoteConfig = useRemoteConfig;
    }

    public synchronized boolean logModulesInUse() {
        String var = getVariable("ivy.log.modules.in.use");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public synchronized boolean logModuleWhenFound() {
        String var = getVariable("ivy.log.module.when.found");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public synchronized boolean logResolvedRevision() {
        String var = getVariable("ivy.log.resolved.revision");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public synchronized boolean debugConflictResolution() {
        if (debugConflictResolution == null) {
            String var = getVariable("ivy.log.conflict.resolution");
            debugConflictResolution = Boolean.valueOf(var != null
                    && Boolean.valueOf(var).booleanValue());
        }
        return debugConflictResolution.booleanValue();
    }

    public synchronized boolean debugLocking() {
        if (debugLocking == null) {
            String var = getVariable("ivy.log.locking");
            debugLocking = Boolean.valueOf(var != null && Boolean.valueOf(var).booleanValue());
        }
        return debugLocking.booleanValue();
    }

    public synchronized boolean dumpMemoryUsage() {
        if (dumpMemoryUsage == null) {
            String var = getVariable("ivy.log.memory");
            dumpMemoryUsage = Boolean.valueOf(var != null && Boolean.valueOf(var).booleanValue());
        }
        return dumpMemoryUsage.booleanValue();
    }

    public synchronized boolean logNotConvertedExclusionRule() {
        return logNotConvertedExclusionRule;
    }

    public synchronized void setLogNotConvertedExclusionRule(boolean logNotConvertedExclusionRule) {
        this.logNotConvertedExclusionRule = logNotConvertedExclusionRule;
    }

    private void init(Object obj) {
        if (obj instanceof IvySettingsAware) {
            ((IvySettingsAware) obj).setSettings(this);
        } else if (obj instanceof DependencyResolver) {
            ((DependencyResolver) obj).setSettings(this);
        }
    }

    private static class ModuleSettings {
        private String resolverName;

        private String branch;

        private String conflictManager;

        private String resolveMode;

        public ModuleSettings(String resolver, String branchName, String conflictMgr,
                String resolveMode) {
            this.resolverName = resolver;
            this.branch = branchName;
            this.conflictManager = conflictMgr;
            this.resolveMode = resolveMode;
        }

        public String toString() {
            return (resolverName != null ? "resolver: " + resolverName : "")
                    + (branch != null ? "branch: " + branch : "")
                    + (conflictManager != null ? "conflictManager: " + conflictManager : "")
                    + (resolveMode != null ? "resolveMode: " + resolveMode : "");
        }

        public String getBranch() {
            return branch;
        }

        public String getResolverName() {
            return resolverName;
        }

        public String getConflictManager() {
            return conflictManager;
        }

        public String getResolveMode() {
            return resolveMode;
        }
    }

    public final long getInterruptTimeout() {
        return INTERUPT_TIMEOUT;
    }

    public synchronized Collection getResolvers() {
        return resolversMap.values();
    }

    public synchronized Collection getResolverNames() {
        return resolversMap.keySet();
    }

    public synchronized Collection getMatcherNames() {
        return matchers.keySet();
    }

    public synchronized IvyVariableContainer getVariableContainer() {
        return variableContainer;
    }

    /**
     * Use a different variable container.
     * 
     * @param variables
     */
    public synchronized void setVariableContainer(IvyVariableContainer variables) {
        variableContainer = variables;
    }

    public synchronized RelativeUrlResolver getRelativeUrlResolver() {
        return new NormalRelativeUrlResolver();
    }

    public synchronized void setDefaultCacheIvyPattern(String defaultCacheIvyPattern) {
        CacheUtil.checkCachePattern(defaultCacheIvyPattern);
        this.defaultCacheIvyPattern = defaultCacheIvyPattern;
    }

    public synchronized String getDefaultCacheIvyPattern() {
        return defaultCacheIvyPattern;
    }

    public synchronized void setDefaultCacheArtifactPattern(String defaultCacheArtifactPattern) {
        CacheUtil.checkCachePattern(defaultCacheArtifactPattern);
        this.defaultCacheArtifactPattern = defaultCacheArtifactPattern;
    }

    public synchronized String getDefaultCacheArtifactPattern() {
        return defaultCacheArtifactPattern;
    }

    public synchronized void setDefaultUseOrigin(boolean useOrigin) {
        defaultUseOrigin = useOrigin;
    }

    public synchronized boolean isDefaultUseOrigin() {
        return defaultUseOrigin;
    }

    public synchronized void useDeprecatedUseOrigin() {
        Message.deprecated("useOrigin option is deprecated when calling resolve, use useOrigin"
                + " setting on the cache implementation instead");
        setDefaultUseOrigin(true);

    }

    /**
     * Validates the settings, throwing an {@link IllegalStateException} if the current state is not
     * valid.
     * 
     * @throws IllegalStateException
     *             if the settings is not valid.
     */
    public synchronized void validate() {
        validateAll(resolversMap.values());
        validateAll(conflictsManager.values());
        validateAll(latestStrategies.values());
        validateAll(lockStrategies.values());
        validateAll(repositoryCacheManagers.values());
        validateAll(reportOutputters.values());
        validateAll(circularDependencyStrategies.values());
        validateAll(versionMatchers.values());
        validateAll(namespaces.values());
    }

    /**
     * Validates all {@link Validatable} objects in the collection.
     * 
     * @param objects
     *            the collection of objects to validate.
     * @throws IllegalStateException
     *             if any of the objects is not valid.
     */
    private void validateAll(Collection values) {
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if (object instanceof Validatable) {
                ((Validatable) object).validate();
            }
        }
    }

    public Namespace getContextNamespace() {
        return Namespace.SYSTEM_NAMESPACE;
    }

    public synchronized void addConfigured(ArchivePacking packing) {
        init(packing);
        packingRegistry.register(packing);
    }

    public PackingRegistry getPackingRegistry() {
        return packingRegistry;
    }
}
