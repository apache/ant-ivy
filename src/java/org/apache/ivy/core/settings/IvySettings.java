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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.NormalRelativeUrlResolver;
import org.apache.ivy.core.RelativeUrlResolver;
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
import org.apache.ivy.core.publish.PublishEngineSettings;
import org.apache.ivy.core.repository.RepositoryManagementEngineSettings;
import org.apache.ivy.core.resolve.ResolveEngineSettings;
import org.apache.ivy.core.retrieve.RetrieveEngineSettings;
import org.apache.ivy.core.sort.SortEngineSettings;
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
import org.apache.ivy.plugins.lock.ArtifactLockStrategy;
import org.apache.ivy.plugins.lock.LockStrategy;
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
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.plugins.version.ChainVersionMatcher;
import org.apache.ivy.plugins.version.ExactVersionMatcher;
import org.apache.ivy.plugins.version.LatestVersionMatcher;
import org.apache.ivy.plugins.version.SubVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.plugins.version.VersionRangeMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.url.URLHandlerRegistry;

public class IvySettings implements SortEngineSettings, PublishEngineSettings, ParserSettings,
        DeliverEngineSettings, CheckEngineSettings, InstallEngineSettings, 
        ResolverSettings, ResolveEngineSettings, RetrieveEngineSettings, 
        RepositoryManagementEngineSettings {
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

    public IvySettings() {
        this(new IvyVariableContainerImpl());
    }

    public IvySettings(IvyVariableContainer variableContainer) {
        setVariableContainer(variableContainer);
        setVariable("ivy.default.settings.dir", getDefaultSettingsDir(), true);
        setDeprecatedVariable("ivy.default.conf.dir", "ivy.default.settings.dir");

        String ivyTypeDefs = System.getProperty("ivy.typedef.files");
        if (ivyTypeDefs != null) {
            String[] files = ivyTypeDefs.split("\\,");
            for (int i = 0; i < files.length; i++) {
                try {
                    typeDefs(new FileInputStream(new File(files[i].trim())), true);
                } catch (FileNotFoundException e) {
                    Message.warn("typedefs file not found: " + files[i].trim());
                } catch (IOException e) {
                    Message.warn("problem with typedef file: " + files[i].trim() + ": "
                            + e.getMessage());
                }
            }
        } else {
            try {
                typeDefs(getSettingsURL("typedef.properties").openStream(), true);
            } catch (IOException e) {
                Message.warn("impossible to load default type defs");
            }
        }
        LatestLexicographicStrategy latestLexicographicStrategy = new LatestLexicographicStrategy();
        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        LatestTimeStrategy latestTimeStrategy = new LatestTimeStrategy();

        addLatestStrategy("latest-revision", latestRevisionStrategy);
        addLatestStrategy("latest-lexico", latestLexicographicStrategy);
        addLatestStrategy("latest-time", latestTimeStrategy);
        
        addLockStrategy("no-lock", new NoLockStrategy());
        addLockStrategy("artifact-lock", new ArtifactLockStrategy(debugLocking()));

        addConflictManager("latest-revision", new LatestConflictManager("latest-revision",
            latestRevisionStrategy));
        addConflictManager("latest-compatible", 
            new LatestCompatibleConflictManager("latest-compatible", latestRevisionStrategy));
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
                + "org.apache.ivy.plugins.matcher.GlobPatternMatcher was not found.");
        }

        addReportOutputter(new LogReportOutputter());
        addReportOutputter(new XmlReportOutputter());

        configureDefaultCircularDependencyStrategies();

        listingIgnore.add(".cvsignore");
        listingIgnore.add("CVS");
        listingIgnore.add(".svn");

        addSystemProperties();
    }

    private void addSystemProperties() {
        addAllVariables(System.getProperties());
    }

    /**
     * Call this method to ask ivy to configure some variables using either a remote or a local
     * properties file
     */
    public void configureRepositories(boolean remote) {
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
                    Message.verbose("unable to use remote repository configuration: "
                            + ex.getMessage());
                    props = new Properties();
                }
            }
            if (!configured) {
                InputStream repositoryPropsStream = null;
                try {
                    repositoryPropsStream = getSettingsURL("repository.properties").openStream();
                    props.load(repositoryPropsStream);
                } catch (IOException e) {
                    Message.error("unable to use internal repository configuration: "
                            + e.getMessage());
                    if (repositoryPropsStream != null) {
                        try {
                            repositoryPropsStream.close();
                        } catch (Exception ex) {
                            //nothing to do
                        }
                    }
                }
            }
            addAllVariables(props, false);
            repositoriesConfigured = true;
        }
    }

    public void typeDefs(InputStream stream) throws IOException {
        typeDefs(stream, false);
    }

    public void typeDefs(InputStream stream, boolean silentFail) throws IOException {
        try {
            Properties p = new Properties();
            p.load(stream);
            typeDefs(p, silentFail);
        } finally {
            stream.close();
        }
    }

    public void typeDefs(Properties p) {
        typeDefs(p, false);
    }

    public void typeDefs(Properties p, boolean silentFail) {
        for (Iterator iter = p.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            typeDef(name, p.getProperty(name), silentFail);
        }
    }

    public void load(File settingsFile) throws ParseException, IOException {
        Message.info(":: loading settings :: file = " + settingsFile);
        long start = System.currentTimeMillis();
        setSettingsVariables(settingsFile);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(new File(getVariable("ivy.default.ivy.user.dir")));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();

        loadDefaultProperties();
        try {
            new XmlSettingsParser(this).parse(settingsFile.toURL());
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

    public void load(URL settingsURL) throws ParseException, IOException {
        Message.info(":: loading settings :: url = " + settingsURL);
        long start = System.currentTimeMillis();
        setSettingsVariables(settingsURL);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(new File(getVariable("ivy.default.ivy.user.dir")));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();

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
    public void defaultInit() throws IOException {
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(new File(getVariable("ivy.default.ivy.user.dir")));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();

        loadDefaultProperties();
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        dumpSettings();
    }

    public void loadDefault() throws ParseException, IOException {
        load(getDefaultSettingsURL());
    }

    public void loadDefault14() throws ParseException, IOException {
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
        return ivysettingsLocation.substring(0, ivysettingsLocation.length()
                - "ivysettings.xml".length() - 1);
    }

    private static URL getSettingsURL(String file) {
        return XmlSettingsParser.class.getResource(file);
    }

    public void setSettingsVariables(File settingsFile) {
        try {
            setVariable("ivy.settings.dir", new File(settingsFile.getAbsolutePath()).getParent());
            setDeprecatedVariable("ivy.conf.dir", "ivy.settings.dir");
            setVariable("ivy.settings.file", settingsFile.getAbsolutePath());
            setDeprecatedVariable("ivy.conf.file", "ivy.settings.file");
            setVariable("ivy.settings.url", settingsFile.toURL().toExternalForm());
            setDeprecatedVariable("ivy.conf.url", "ivy.settings.url");
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

    public void setSettingsVariables(URL settingsURL) {
        String settingsURLStr = settingsURL.toExternalForm();
        setVariable("ivy.settings.url", settingsURLStr);
        setDeprecatedVariable("ivy.conf.url", "ivy.settings.url");
        int slashIndex = settingsURLStr.lastIndexOf('/');
        if (slashIndex != -1) {
            setVariable("ivy.settings.dir", settingsURLStr.substring(0, slashIndex));
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

    public void loadProperties(URL url) throws IOException {
        loadProperties(url, true);
    }

    public void loadProperties(URL url, boolean overwrite) throws IOException {
        loadProperties(url.openStream(), overwrite);
    }

    public void loadProperties(File file) throws IOException {
        loadProperties(file, true);
    }

    public void loadProperties(File file, boolean overwrite) throws IOException {
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
                    //nothing
                }
            }
        }
    }

    public void setVariable(String varName, String value) {
        setVariable(varName, value, true);
    }

    public void setVariable(String varName, String value, boolean overwrite) {
        variableContainer.setVariable(varName, value, overwrite);
    }

    public void addAllVariables(Map variables) {
        addAllVariables(variables, true);
    }

    public void addAllVariables(Map variables, boolean overwrite) {
        for (Iterator iter = variables.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String val = (String) variables.get(key);
            setVariable(key, val, overwrite);
        }
    }

    /**
     * Substitute variables in the given string by their value found in the current set of variables
     * 
     * @param str
     *            the string in which substitution should be made
     * @return the string where all current ivy variables have been substituted by their value
     */
    public String substitute(String str) {
        return IvyPatternHelper.substituteVariables(str, variableContainer);
    }

    /**
     * Returns the variables loaded in configuration file. Those variables may better be seen as ant
     * properties
     * 
     * @return
     */
    public IvyVariableContainer getVariables() {
        return variableContainer;
    }

    public Class typeDef(String name, String className) {
        return typeDef(name, className, false);
    }

    public Class typeDef(String name, String className, boolean silentFail) {
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
                classloader = new URLClassLoader((URL[]) classpathURLs
                        .toArray(new URL[classpathURLs.size()]), Ivy.class.getClassLoader());
            }
        }
        return classloader;
    }

    public void addClasspathURL(URL url) {
        classpathURLs.add(url);
        classloader = null;
    }

    public Map getTypeDefs() {
        return typeDefs;
    }

    public Class getTypeDef(String name) {
        return (Class) typeDefs.get(name);
    }

    // methods which match ivy conf method signature specs
    public void addConfigured(DependencyResolver resolver) {
        addResolver(resolver);
    }

    public void addConfigured(ModuleDescriptorParser parser) {
        ModuleDescriptorParserRegistry.getInstance().addParser(parser);
    }

    public void addResolver(DependencyResolver resolver) {
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

    public void setDefaultCache(File cacheDirectory) {
        setVariable("ivy.cache.dir", cacheDirectory.getAbsolutePath(), false);
        defaultCache = cacheDirectory;
    }

    public void setDefaultResolver(String resolverName) {
        checkResolverName(resolverName);
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
    public void addModuleConfiguration(Map attributes, PatternMatcher matcher, String resolverName,
            String branch, String conflictManager) {
        checkResolverName(resolverName);
        moduleSettings.defineRule(
            new MapMatcher(attributes, matcher), 
            new ModuleSettings(resolverName, branch, conflictManager));
    }

    public File getDefaultIvyUserDir() {
        if (defaultUserDir == null) {
            if (getVariable("ivy.home") != null) {
                setDefaultIvyUserDir(new File(getVariable("ivy.home")));
                Message.verbose("using ivy.default.ivy.user.dir variable for default ivy user dir: "
                                + defaultUserDir);
            } else {
                setDefaultIvyUserDir(new File(System.getProperty("user.home"), ".ivy2"));
                Message.verbose("no default ivy user dir defined: set to " + defaultUserDir);
            }
        }
        return defaultUserDir;
    }

    public void setDefaultIvyUserDir(File defaultUserDir) {
        this.defaultUserDir = defaultUserDir;
        setVariable("ivy.default.ivy.user.dir", this.defaultUserDir.getAbsolutePath());
        setVariable("ivy.home", this.defaultUserDir.getAbsolutePath());
    }

    public File getDefaultCache() {
        if (defaultCache == null) {
            String cache = getVariable("ivy.cache.dir");
            if (cache != null) {
                defaultCache = new File(cache);
            } else {
                setDefaultCache(new File(getDefaultIvyUserDir(), "cache"));
                Message.verbose("no default cache defined: set to " + defaultCache);
            }
        }
        return defaultCache;
    }
    
    public void setDefaultRepositoryCacheBasedir(String repositoryCacheRoot) {
        setVariable("ivy.cache.repository", repositoryCacheRoot, true);
    }
    
    public void setDefaultResolutionCacheBasedir(String resolutionCacheRoot) {
        setVariable("ivy.cache.resolution", resolutionCacheRoot, true);
    }
    
    public File getDefaultRepositoryCacheBasedir() {
        String repositoryCacheRoot = getVariable("ivy.cache.repository");
        if (repositoryCacheRoot != null) {
            return new File(repositoryCacheRoot);
        } else {
            return getDefaultCache();
        }
    }

    public File getDefaultResolutionCacheBasedir() {
        String resolutionCacheRoot = getVariable("ivy.cache.resolution");
        if (resolutionCacheRoot != null) {
            return new File(resolutionCacheRoot);
        } else {
            return getDefaultCache();
        }
    }

    public void setDictatorResolver(DependencyResolver resolver) {
        dictatorResolver = resolver;
    }

    public DependencyResolver getResolver(ModuleRevisionId mrid) {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        String resolverName = getResolverName(mrid);
        return getResolver(resolverName);
    }

    public DependencyResolver getResolver(String resolverName) {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        DependencyResolver resolver = (DependencyResolver) resolversMap.get(resolverName);
        if (resolver == null) {
            Message.error("unknown resolver " + resolverName);
        }
        return resolver;
    }

    public DependencyResolver getDefaultResolver() {
        if (dictatorResolver != null) {
            return dictatorResolver;
        }
        if (defaultResolver == null) {
            defaultResolver = (DependencyResolver) resolversMap.get(defaultResolverName);
        }
        return defaultResolver;
    }

    public String getResolverName(ModuleRevisionId mrid) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(mrid, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getResolverName() != null;
            }
        });
        return ms == null ? defaultResolverName : ms.getResolverName();
    }

    public String getDefaultBranch(ModuleId moduleId) {
        ModuleSettings ms = (ModuleSettings) moduleSettings.getRule(moduleId, new Filter() {
            public boolean accept(Object o) {
                return ((ModuleSettings) o).getBranch() != null;
            }
        });
        return ms == null ? getDefaultBranch() : ms.getBranch();
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public ConflictManager getConflictManager(ModuleId moduleId) {
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
                throw new IllegalStateException(
                        "ivy badly configured: unknown conflict manager "
                                + ms.getConflictManager());
            }
            return cm;
        }
    }

    public void addConfigured(ConflictManager cm) {
        addConflictManager(cm.getName(), cm);
    }

    public ConflictManager getConflictManager(String name) {
        if ("default".equals(name)) {
            return getDefaultConflictManager();
        }
        return (ConflictManager) conflictsManager.get(name);
    }

    public void addConflictManager(String name, ConflictManager cm) {
        init(cm);
        conflictsManager.put(name, cm);
    }

    public void addConfigured(LatestStrategy latest) {
        addLatestStrategy(latest.getName(), latest);
    }

    public LatestStrategy getLatestStrategy(String name) {
        if ("default".equals(name)) {
            return getDefaultLatestStrategy();
        }
        return (LatestStrategy) latestStrategies.get(name);
    }

    public void addLatestStrategy(String name, LatestStrategy latest) {
        init(latest);
        latestStrategies.put(name, latest);
    }

    public void addConfigured(LockStrategy lockStrategy) {
        addLockStrategy(lockStrategy.getName(), lockStrategy);
    }

    public LockStrategy getLockStrategy(String name) {
        if ("default".equals(name)) {
            return getDefaultLockStrategy();
        }
        return (LockStrategy) lockStrategies.get(name);
    }

    public void addLockStrategy(String name, LockStrategy lockStrategy) {
        init(lockStrategy);
        lockStrategies.put(name, lockStrategy);
    }

    public void addConfigured(Namespace ns) {
        addNamespace(ns);
    }

    public Namespace getNamespace(String name) {
        if ("system".equals(name)) {
            return getSystemNamespace();
        }
        return (Namespace) namespaces.get(name);
    }

    public Namespace getSystemNamespace() {
        return Namespace.SYSTEM_NAMESPACE;
    }

    public void addNamespace(Namespace ns) {
        init(ns);
        namespaces.put(ns.getName(), ns);
    }

    public void addConfigured(PatternMatcher m) {
        addMatcher(m);
    }

    public PatternMatcher getMatcher(String name) {
        return (PatternMatcher) matchers.get(name);
    }

    public void addMatcher(PatternMatcher m) {
        init(m);
        matchers.put(m.getName(), m);
    }

    public void addConfigured(RepositoryCacheManager c) {
        addRepositoryCacheManager(c);
    }

    public RepositoryCacheManager getRepositoryCacheManager(String name) {
        return (RepositoryCacheManager) repositoryCacheManagers.get(name);
    }

    public void addRepositoryCacheManager(RepositoryCacheManager c) {
        init(c);
        repositoryCacheManagers.put(c.getName(), c);
    }

    public RepositoryCacheManager[] getRepositoryCacheManagers() {
        return (RepositoryCacheManager[]) repositoryCacheManagers.values().toArray(
            new RepositoryCacheManager[repositoryCacheManagers.size()]);
    }

    public void addConfigured(ReportOutputter outputter) {
        addReportOutputter(outputter);
    }

    public ReportOutputter getReportOutputter(String name) {
        return (ReportOutputter) reportOutputters.get(name);
    }

    public void addReportOutputter(ReportOutputter outputter) {
        init(outputter);
        reportOutputters.put(outputter.getName(), outputter);
    }

    public ReportOutputter[] getReportOutputters() {
        return (ReportOutputter[]) reportOutputters.values().toArray(
            new ReportOutputter[reportOutputters.size()]);
    }

    public void addConfigured(VersionMatcher vmatcher) {
        addVersionMatcher(vmatcher);
    }

    public VersionMatcher getVersionMatcher(String name) {
        return (VersionMatcher) versionMatchers.get(name);
    }

    public void addVersionMatcher(VersionMatcher vmatcher) {
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

    public VersionMatcher[] getVersionMatchers() {
        return (VersionMatcher[]) versionMatchers.values().toArray(
            new VersionMatcher[versionMatchers.size()]);
    }

    public VersionMatcher getVersionMatcher() {
        if (versionMatcher == null) {
            configureDefaultVersionMatcher();
        }
        return versionMatcher;
    }

    public void configureDefaultVersionMatcher() {
        addVersionMatcher(new LatestVersionMatcher());
        addVersionMatcher(new SubVersionMatcher());
        addVersionMatcher(new VersionRangeMatcher());
    }

    public CircularDependencyStrategy getCircularDependencyStrategy() {
        if (circularDependencyStrategy == null) {
            circularDependencyStrategy = getCircularDependencyStrategy("default");
        }
        return circularDependencyStrategy;
    }

    public CircularDependencyStrategy getCircularDependencyStrategy(String name) {
        if ("default".equals(name)) {
            name = "warn";
        }
        return (CircularDependencyStrategy) circularDependencyStrategies.get(name);
    }

    public void setCircularDependencyStrategy(CircularDependencyStrategy strategy) {
        circularDependencyStrategy = strategy;
    }

    public void addConfigured(CircularDependencyStrategy strategy) {
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

    public StatusManager getStatusManager() {
        if (statusManager == null) {
            statusManager = StatusManager.newDefaultInstance();
        }
        return statusManager;
    }

    public void setStatusManager(StatusManager statusManager) {
        this.statusManager = statusManager;
    }

    /**
     * Returns true if the name should be ignored in listing
     * 
     * @param name
     * @return
     */
    public boolean listingIgnore(String name) {
        return listingIgnore.contains(name);
    }

    /**
     * Filters the names list by removing all names that should be ignored as defined by the listing
     * ignore list
     * 
     * @param names
     */
    public void filterIgnore(Collection names) {
        names.removeAll(listingIgnore);
    }

    public boolean isCheckUpToDate() {
        return checkUpToDate;
    }

    public void setCheckUpToDate(boolean checkUpToDate) {
        this.checkUpToDate = checkUpToDate;
    }
    
    public boolean doValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public String getVariable(String name) {
        return variableContainer.getVariable(name);
    }

    public ConflictManager getDefaultConflictManager() {
        if (defaultConflictManager == null) {
            defaultConflictManager = new LatestConflictManager(getDefaultLatestStrategy());
        }
        return defaultConflictManager;
    }

    public void setDefaultConflictManager(ConflictManager defaultConflictManager) {
        this.defaultConflictManager = defaultConflictManager;
    }

    public LatestStrategy getDefaultLatestStrategy() {
        if (defaultLatestStrategy == null) {
            defaultLatestStrategy = new LatestRevisionStrategy();
        }
        return defaultLatestStrategy;
    }

    public void setDefaultLatestStrategy(LatestStrategy defaultLatestStrategy) {
        this.defaultLatestStrategy = defaultLatestStrategy;
    }

    public LockStrategy getDefaultLockStrategy() {
        if (defaultLockStrategy == null) {
            defaultLockStrategy = new NoLockStrategy();
        }
        return defaultLockStrategy;
    }

    public void setDefaultLockStrategy(LockStrategy defaultLockStrategy) {
        this.defaultLockStrategy = defaultLockStrategy;
    }

    public RepositoryCacheManager getDefaultRepositoryCacheManager() {
        if (defaultRepositoryCacheManager == null) {
            defaultRepositoryCacheManager 
                = new DefaultRepositoryCacheManager(
                    "default-cache", this, getDefaultRepositoryCacheBasedir());
            addRepositoryCacheManager(defaultRepositoryCacheManager);
        }
        return defaultRepositoryCacheManager;
    }

    public void setDefaultRepositoryCacheManager(RepositoryCacheManager cache) {
        this.defaultRepositoryCacheManager = cache;
    }

    public ResolutionCacheManager getResolutionCacheManager() {
        if (resolutionCacheManager == null) {
            resolutionCacheManager 
                = new DefaultResolutionCacheManager(getDefaultResolutionCacheBasedir());
        }
        return resolutionCacheManager;
    }
    
    public void setResolutionCacheManager(ResolutionCacheManager resolutionCacheManager) {
        this.resolutionCacheManager = resolutionCacheManager;
    }

    public void addTrigger(Trigger trigger) {
        init(trigger);
        triggers.add(trigger);
    }

    public List getTriggers() {
        return triggers;
    }

    public void addConfigured(Trigger trigger) {
        addTrigger(trigger);
    }

    public boolean isUseRemoteConfig() {
        return useRemoteConfig;
    }

    public void setUseRemoteConfig(boolean useRemoteConfig) {
        this.useRemoteConfig = useRemoteConfig;
    }

    public boolean logModulesInUse() {
        String var = getVariable("ivy.log.modules.in.use");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public boolean logModuleWhenFound() {
        String var = getVariable("ivy.log.module.when.found");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public boolean logResolvedRevision() {
        String var = getVariable("ivy.log.resolved.revision");
        return var == null || Boolean.valueOf(var).booleanValue();
    }

    public boolean debugConflictResolution() {
        if (debugConflictResolution == null) {
            String var = getVariable("ivy.log.conflict.resolution");
            debugConflictResolution = Boolean.valueOf(var != null
                    && Boolean.valueOf(var).booleanValue());
        }
        return debugConflictResolution.booleanValue();
    }

    public final boolean debugLocking() {
        if (debugLocking == null) {
            String var = getVariable("ivy.log.locking");
            debugLocking = Boolean.valueOf(var != null
                    && Boolean.valueOf(var).booleanValue());
        }
        return debugLocking.booleanValue();
    }

    public boolean dumpMemoryUsage() {
        if (dumpMemoryUsage == null) {
            String var = getVariable("ivy.log.memory");
            dumpMemoryUsage = Boolean.valueOf(var != null
                    && Boolean.valueOf(var).booleanValue());
        }
        return dumpMemoryUsage.booleanValue();
    }


    public boolean logNotConvertedExclusionRule() {
        return logNotConvertedExclusionRule;
    }

    public void setLogNotConvertedExclusionRule(boolean logNotConvertedExclusionRule) {
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

        public ModuleSettings(String resolver, String branchName, String conflictMgr) {
            resolverName = resolver;
            branch = branchName;
            conflictManager = conflictMgr;
        }

        public String toString() {
            return resolverName != null ? "resolver: " + resolverName
                    : "" + branch != null ? "branch: " + branch : "";
        }

        public String getBranch() {
            return branch;
        }

        public String getResolverName() {
            return resolverName;
        }

        protected String getConflictManager() {
            return conflictManager;
        }
    }

    public long getInterruptTimeout() {
        return INTERUPT_TIMEOUT;
    }

    public Collection getResolvers() {
        return resolversMap.values();
    }

    public Collection getResolverNames() {
        return resolversMap.keySet();
    }

    public Collection getMatcherNames() {
        return matchers.keySet();
    }

    public IvyVariableContainer getVariableContainer() {
        return variableContainer;
    }

    /**
     * Use a different variable container.
     * 
     * @param variables
     */
    public void setVariableContainer(IvyVariableContainer variables) {
        variableContainer = variables;
    }

    
    public RelativeUrlResolver getRelativeUrlResolver() {
        return new NormalRelativeUrlResolver();
    }

    public void setDefaultCacheIvyPattern(String defaultCacheIvyPattern) {
        this.defaultCacheIvyPattern = defaultCacheIvyPattern;
    }
    
    public String getDefaultCacheIvyPattern() {
        return defaultCacheIvyPattern;
    }

    public void setDefaultCacheArtifactPattern(String defaultCacheArtifactPattern) {
        this.defaultCacheArtifactPattern = defaultCacheArtifactPattern;
    }
    
    public String getDefaultCacheArtifactPattern() {
        return defaultCacheArtifactPattern;
    }

    public void setDefaultUseOrigin(boolean useOrigin) {
        defaultUseOrigin = useOrigin;
    }
    
    public boolean isDefaultUseOrigin() {
        return defaultUseOrigin;
    }
    
    public void useDeprecatedUseOrigin() {
        Message.deprecated("useOrigin option is deprecated when calling resolve, use useOrigin"
            + " setting on the cache implementation instead");
        setDefaultUseOrigin(true);
        
    }
}
