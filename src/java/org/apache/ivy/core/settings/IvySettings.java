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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.IvyAware;
import org.apache.ivy.plugins.IvySettingsAware;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.circular.ErrorCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.IgnoreCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.WarnCircularDependencyStrategy;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.conflict.LatestConflictManager;
import org.apache.ivy.plugins.conflict.NoConflictManager;
import org.apache.ivy.plugins.conflict.StrictConflictManager;
import org.apache.ivy.plugins.latest.LatestLexicographicStrategy;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.GlobPatternMatcher;
import org.apache.ivy.plugins.matcher.ModuleIdMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.matcher.RegexpPatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.report.LogReportOutputter;
import org.apache.ivy.plugins.report.ReportOutputter;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.DualResolver;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.plugins.version.ChainVersionMatcher;
import org.apache.ivy.plugins.version.ExactVersionMatcher;
import org.apache.ivy.plugins.version.LatestVersionMatcher;
import org.apache.ivy.plugins.version.SubVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.plugins.version.VersionRangeMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.URLHandlerRegistry;

public class IvySettings {
    private static final String DEFAULT_CACHE_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision](.[ext])";
    private static final String DEFAULT_CACHE_DATA_FILE_PATTERN = "[organisation]/[module]/ivydata-[revision].properties";
    private static final String DEFAULT_CACHE_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";
    private static final String DEFAULT_CACHE_RESOLVED_IVY_PATTERN = "resolved-[organisation]-[module]-[revision].xml";
    private static final String DEFAULT_CACHE_RESOLVED_IVY_PROPERTIES_PATTERN = "resolved-[organisation]-[module]-[revision].properties";

    private Map _typeDefs = new HashMap();
    private Map _resolversMap = new HashMap();
    private DependencyResolver _defaultResolver;
    private DependencyResolver _dictatorResolver = null;
    
    private String _defaultResolverName;
    private File _defaultCache;

	private String _defaultBranch = null;

    private boolean _checkUpToDate = true;
    private Map _moduleSettings = new LinkedHashMap(); // Map (ModuleIdMatcher -> ModuleSettings)
    
    private Map _conflictsManager = new HashMap(); // Map (String conflictManagerName -> ConflictManager)
    private Map _latestStrategies = new HashMap(); // Map (String latestStrategyName -> LatestStrategy)
    private Map _namespaces = new HashMap(); // Map (String namespaceName -> Namespace)
    private Map _matchers = new HashMap(); // Map (String matcherName -> Matcher)
    private Map _reportOutputters = new HashMap(); // Map (String outputterName -> ReportOutputter)
    private Map _versionMatchers = new HashMap(); // Map (String matcherName -> VersionMatcher)
    private Map _circularDependencyStrategies = new HashMap(); // Map (String name -> CircularDependencyStrategy)
    private List _triggers = new ArrayList(); // List (Trigger)
    
    private Map _variables = new HashMap();

    private String _cacheIvyPattern = DEFAULT_CACHE_IVY_PATTERN;
    private String _cacheResolvedIvyPattern = DEFAULT_CACHE_RESOLVED_IVY_PATTERN;
    private String _cacheResolvedIvyPropertiesPattern = DEFAULT_CACHE_RESOLVED_IVY_PROPERTIES_PATTERN;
    private String _cacheArtifactPattern = DEFAULT_CACHE_ARTIFACT_PATTERN;
    private String _cacheDataFilePattern = DEFAULT_CACHE_DATA_FILE_PATTERN;

    private boolean _validate = true;

    private LatestStrategy _defaultLatestStrategy = null;
    private ConflictManager _defaultConflictManager = null;
    private CircularDependencyStrategy _circularDependencyStrategy = null;
    
    private List _listingIgnore = new ArrayList();

    private boolean _repositoriesConfigured;

    private boolean _useRemoteConfig = false;

    private File _defaultUserDir;
    

    private List _classpathURLs = new ArrayList();

    private ClassLoader _classloader;
    
	private long _interruptTimeout = 2000;
	private Boolean _debugConflictResolution;
	private boolean _logNotConvertedExclusionRule;
	private VersionMatcher _versionMatcher;
	private StatusManager _statusManager;
	
	public IvySettings() {
        setVariable("ivy.default.settings.dir", getDefaultSettingsDir(), true);
        setDeprecatedVariable("ivy.default.conf.dir", "ivy.default.settings.dir");
        
        String ivyTypeDefs = System.getProperty("ivy.typedef.files");
        if (ivyTypeDefs != null) {
            String[] files = ivyTypeDefs.split("\\,");
            for (int i = 0; i < files.length; i++) {
                try {
                    typeDefs(new FileInputStream(new File(files[i].trim())), true);
                } catch (FileNotFoundException e) {
                    Message.warn("typedefs file not found: "+files[i].trim());
                } catch (IOException e) {
                    Message.warn("problem with typedef file: "+files[i].trim()+": "+e.getMessage());
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

        addConflictManager("latest-revision", new LatestConflictManager("latest-revision", latestRevisionStrategy));
        addConflictManager("latest-time", new LatestConflictManager("latest-time", latestTimeStrategy));
        addConflictManager("all", new NoConflictManager());    
        addConflictManager("strict", new StrictConflictManager());
        
        addMatcher(ExactPatternMatcher.INSTANCE);
        addMatcher(RegexpPatternMatcher.INSTANCE);
        addMatcher(ExactOrRegexpPatternMatcher.INSTANCE);
        addMatcher(GlobPatternMatcher.INSTANCE);
        
        addReportOutputter(new XmlReportOutputter());
        addReportOutputter(new LogReportOutputter());
        
        configureDefaultCircularDependencyStrategies();
        
        _listingIgnore.add(".cvsignore");
        _listingIgnore.add("CVS");
        _listingIgnore.add(".svn");
        
        addSystemProperties();
	}

    private void addSystemProperties() {
        addAllVariables(System.getProperties());
    }

    /**
     * Call this method to ask ivy to configure some variables using either a remote or a local properties file
     */
    public void configureRepositories(boolean remote) {
        if (!_repositoriesConfigured) {
            Properties props = new Properties();
            boolean configured = false;
            if (_useRemoteConfig && remote) {
                try {
                    URL url = new URL("http://incubator.apache.org/ivy/repository.properties");
                    Message.verbose("configuring repositories with "+url);
                    props.load(URLHandlerRegistry.getDefault().openStream(url));
                    configured = true;
                } catch (Exception ex) {
                    Message.verbose("unable to use remote repository configuration: "+ex.getMessage());
                    props = new Properties();
                }
            }
            if (!configured) {
            	InputStream repositoryPropsStream = null;
                try {
                    repositoryPropsStream = getSettingsURL("repository.properties").openStream();
					props.load(repositoryPropsStream);
                } catch (IOException e) {
                    Message.error("unable to use internal repository configuration: "+e.getMessage());
                    if (repositoryPropsStream != null) {
                    	try {repositoryPropsStream.close();} catch(Exception ex) {};
                    }
                }
            }
            addAllVariables(props, false);
            _repositoriesConfigured = true;
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
        Message.info(":: loading settings :: file = "+settingsFile);
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
            IllegalArgumentException iae = new IllegalArgumentException("given file cannot be transformed to url: "+settingsFile);
            iae.initCause(e);
            throw iae;
        }
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        Message.verbose("settings loaded ("+(System.currentTimeMillis()-start)+"ms)");
        dumpSettings();
    }

    public void load(URL settingsURL) throws ParseException, IOException {
        Message.info(":: loading settings :: url = "+settingsURL);
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
        Message.verbose("settings loaded ("+(System.currentTimeMillis()-start)+"ms)");
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
		return ivysettingsLocation.substring(0, ivysettingsLocation.length() - "ivysettings.xml".length() - 1);
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
            IllegalArgumentException iae = new IllegalArgumentException("given file cannot be transformed to url: "+settingsFile);
            iae.initCause(e);
            throw iae;
        }
    }
    
    /**
     * Sets a deprecated variable with the value of the new variable
     * @param deprecatedKey the deprecated variable name
     * @param newKey the new variable name
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
            Message.warn("settings url does not contain any slash (/): ivy.settings.dir variable not set");
        }
    }
    
    private void dumpSettings() {
        Message.verbose("\tdefault cache: "+getDefaultCache());
        Message.verbose("\tdefault resolver: "+getDefaultResolver());
        Message.debug("\tdefault latest strategy: "+getDefaultLatestStrategy());
        Message.debug("\tdefault conflict manager: "+getDefaultConflictManager());
        Message.debug("\tcircular dependency strategy: "+getCircularDependencyStrategy());
        Message.debug("\tvalidate: "+doValidate());
        Message.debug("\tcheck up2date: "+isCheckUpToDate());
        Message.debug("\tcache ivy pattern: "+getCacheIvyPattern());
        Message.debug("\tcache artifact pattern: "+getCacheArtifactPattern());
        
        if (!_classpathURLs.isEmpty()) {
            Message.verbose("\t-- "+_classpathURLs.size()+" custom classpath urls:");
            for (Iterator iter = _classpathURLs.iterator(); iter.hasNext();) {
                Message.debug("\t\t"+iter.next());
            }
        }
        Message.verbose("\t-- "+_resolversMap.size()+" resolvers:");
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            resolver.dumpSettings();
        }
        if (!_moduleSettings.isEmpty()) {
            Message.debug("\tmodule settings:");
            for (Iterator iter = _moduleSettings.keySet().iterator(); iter.hasNext();) {
                ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
                ModuleSettings s = (ModuleSettings)_moduleSettings.get(midm);
                Message.debug("\t\t"+midm+" -> "+s);
            }
        }
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
    			} catch (IOException e) {}
    		}
    	}
    }
    
    public void setVariable(String varName, String value) {
        setVariable(varName, value, true);
    }
    
    public void setVariable(String varName, String value, boolean overwrite) {
        if (overwrite || !_variables.containsKey(varName)) {
            Message.debug("setting '"+varName+"' to '"+value+"'");
            _variables.put(varName, substitute(value));
        } else {
            Message.debug("'"+varName+"' already set: discarding '"+value+"'");
        }
    }
    
    public void addAllVariables(Map variables) {
        addAllVariables(variables, true);
    }

    public void addAllVariables(Map variables, boolean overwrite) {
        for (Iterator iter = variables.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
            String val = (String)variables.get(key);
            setVariable(key, val, overwrite);
        }
    }

    /**
     * Substitute variables in the given string by their value found in the current 
     * set of variables
     * 
     * @param str the string in which substitution should be made
     * @return the string where all current ivy variables have been substituted by their value
     */
    public String substitute(String str) {
        return IvyPatternHelper.substituteVariables(str, getVariables());
    }

    /**
     * Returns the variables loaded in configuration file. Those variables
     * may better be seen as ant properties 
     * 
     * @return
     */
    public Map getVariables() {
        return _variables;
    }

    public Class typeDef(String name, String className) {
        return typeDef(name, className, false);
    }
    
    public Class typeDef(String name, String className, boolean silentFail) {
        Class clazz = classForName(className, silentFail);
        if (clazz != null) {
        	_typeDefs.put(name, clazz);
        }
        return clazz;
    }
    
    private Class classForName(String className, boolean silentFail) {
        try {
            return getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
        	if (silentFail) {
        		Message.info("impossible to define new type: class not found: "+className+" in "+_classpathURLs+" nor Ivy classloader");
        		return null;
        	} else {
        		throw new RuntimeException("impossible to define new type: class not found: "+className+" in "+_classpathURLs+" nor Ivy classloader");
        	}
        }
    }

    private ClassLoader getClassLoader() {
        if (_classloader == null) {
            if (_classpathURLs.isEmpty()) {
                _classloader = Ivy.class.getClassLoader();   
            } else {
                _classloader = new URLClassLoader(
                        (URL[])_classpathURLs.toArray(new URL[_classpathURLs.size()]), 
                        Ivy.class.getClassLoader());
            }
        }
        return _classloader;
    }


    public void addClasspathURL(URL url) {
        _classpathURLs.add(url);
        _classloader = null;
    }

    public Map getTypeDefs() {
        return _typeDefs;
    }

    public Class getTypeDef(String name) {
        return (Class)_typeDefs.get(name);
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
        _resolversMap.put(resolver.getName(), resolver);
        if (resolver instanceof ChainResolver) {
            List subresolvers = ((ChainResolver)resolver).getResolvers();
            for (Iterator iter = subresolvers.iterator(); iter.hasNext();) {
                DependencyResolver dr = (DependencyResolver)iter.next();
                addResolver(dr);
            }
        } else if (resolver instanceof DualResolver) {
            DependencyResolver ivyResolver = ((DualResolver)resolver).getIvyResolver();
            if (ivyResolver != null) {
                addResolver(ivyResolver);
            }
            DependencyResolver artifactResolver = ((DualResolver)resolver).getArtifactResolver();
            if (artifactResolver != null) {
                addResolver(artifactResolver);
            }
        }
    }

    public void setDefaultCache(File cacheDirectory) {
        _defaultCache = cacheDirectory;
    }
    
    public void setDefaultResolver(String resolverName) {
        checkResolverName(resolverName);
        _defaultResolverName = resolverName;
    }
    
    private void checkResolverName(String resolverName) {
        if (resolverName != null && !_resolversMap.containsKey(resolverName)) {
            throw new IllegalArgumentException("no resolver found called "+resolverName+": check your settings");
        }
    }

    /**
     * regular expressions as explained in Pattern class may be used in ModuleId
     * organisation and name
     * 
     * @param moduleId
     * @param resolverName
     * @param branch 
     */
    public void addModuleConfiguration(ModuleId mid, PatternMatcher matcher, String resolverName, String branch, String conflictManager) {
        checkResolverName(resolverName);
        _moduleSettings.put(new ModuleIdMatcher(mid, matcher), new ModuleSettings(resolverName, branch, conflictManager));
    }
    
    public File getDefaultIvyUserDir() {
        if (_defaultUserDir==null) {
        	if (getVariable("ivy.home") != null) {
        		setDefaultIvyUserDir(new File(getVariable("ivy.home")));
        		Message.verbose("using ivy.default.ivy.user.dir variable for default ivy user dir: "+_defaultUserDir);
        	} else {
        		setDefaultIvyUserDir(new File(System.getProperty("user.home"), ".ivy"));
        		Message.verbose("no default ivy user dir defined: set to "+_defaultUserDir);
        	}
        }
        return _defaultUserDir;
    }
    
    public void setDefaultIvyUserDir(File defaultUserDir) {
        _defaultUserDir = defaultUserDir;
        setVariable("ivy.default.ivy.user.dir", _defaultUserDir.getAbsolutePath());
        setVariable("ivy.home", _defaultUserDir.getAbsolutePath());
    }    

    public File getDefaultCache() {
        if (_defaultCache==null) {
            _defaultCache = new File(getDefaultIvyUserDir(), "cache");
            Message.verbose("no default cache defined: set to "+_defaultCache);
        }
        return _defaultCache;
    }

    public DependencyResolver getResolver(ModuleId moduleId) {
        if (_dictatorResolver != null) {
            return _dictatorResolver;
        }
        String resolverName = getResolverName(moduleId);
        return getResolver(resolverName);
    }

    public DependencyResolver getResolver(String resolverName) {
        if (_dictatorResolver != null) {
            return _dictatorResolver;
        }
        DependencyResolver resolver = (DependencyResolver)_resolversMap.get(resolverName);
        if (resolver == null) {
            Message.error("unknown resolver "+resolverName);
        }
        return resolver;
    }

    public DependencyResolver getDefaultResolver() {
        if (_dictatorResolver != null) {
            return _dictatorResolver;
        }
        if (_defaultResolver == null) {
            _defaultResolver = (DependencyResolver)_resolversMap.get(_defaultResolverName);
        }
        return _defaultResolver;
    }

    public String getResolverName(ModuleId moduleId) {
        for (Iterator iter = _moduleSettings.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleSettings.get(midm);
            	if (ms.getResolverName() != null) {
            		return ms.getResolverName();
            	}
            }
        }
        return _defaultResolverName;
    }
    
	public String getDefaultBranch(ModuleId moduleId) {
        for (Iterator iter = _moduleSettings.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleSettings.get(midm);
            	if (ms.getBranch() != null) {
            		return ms.getBranch();
            	}
            }
        }
		return getDefaultBranch();
	}

	public String getDefaultBranch() {
		return _defaultBranch;
	}
	public void setDefaultBranch(String defaultBranch) {
		_defaultBranch = defaultBranch;
	}

	public ConflictManager getConflictManager(ModuleId moduleId) {
        for (Iterator iter = _moduleSettings.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleSettings.get(midm);
            	if (ms.getConflictManager() != null) {
            		ConflictManager cm = getConflictManager(ms.getConflictManager());
            		if (cm == null) {
            			throw new IllegalStateException("ivy badly configured: unknown conflict manager "+ms.getConflictManager());
            		}
					return cm;
            	}
            }
        }
		return getDefaultConflictManager();
	}

    public void addConfigured(ConflictManager cm) {
        addConflictManager(cm.getName(), cm);
    }
    
    public ConflictManager getConflictManager(String name) {
        if ("default".equals(name)) {
            return getDefaultConflictManager();
        }
        return (ConflictManager)_conflictsManager.get(name);
    }
    public void addConflictManager(String name, ConflictManager cm) {
        init(cm);
        _conflictsManager.put(name, cm);
    }
    
    public void addConfigured(LatestStrategy latest) {
        addLatestStrategy(latest.getName(), latest);
    }
    
    public LatestStrategy getLatestStrategy(String name) {
        if ("default".equals(name)) {
            return getDefaultLatestStrategy();
        }
        return (LatestStrategy)_latestStrategies.get(name);
    }
    public void addLatestStrategy(String name, LatestStrategy latest) {
        init(latest);
        _latestStrategies.put(name, latest);
    }
    
    public void addConfigured(Namespace ns) {
        addNamespace(ns);
    }
    
    public Namespace getNamespace(String name) {
        if ("system".equals(name)) {
            return getSystemNamespace();
        }
        return (Namespace)_namespaces.get(name);
    }
    
    public Namespace getSystemNamespace() {
        return Namespace.SYSTEM_NAMESPACE;
    }

    public void addNamespace(Namespace ns) {
        init(ns);
        _namespaces.put(ns.getName(), ns);
    }
    
    public void addConfigured(PatternMatcher m) {
        addMatcher(m);
    }
    
    public PatternMatcher getMatcher(String name) {
        return (PatternMatcher)_matchers.get(name);
    }
    
    public void addMatcher(PatternMatcher m) {
        init(m);
        _matchers.put(m.getName(), m);
    }
    
    public void addConfigured(ReportOutputter outputter) {
        addReportOutputter(outputter);
     }
     
     public ReportOutputter getReportOutputter(String name) {
        return (ReportOutputter) _reportOutputters.get(name);
     }
     
     public void addReportOutputter(ReportOutputter outputter) {
         init(outputter);
        _reportOutputters.put(outputter.getName(), outputter);
     }
     
     public ReportOutputter[] getReportOutputters() {
        return (ReportOutputter[]) _reportOutputters.values().toArray(new ReportOutputter[_reportOutputters.size()]);
     }
     
     public void addConfigured(VersionMatcher vmatcher) {
         addVersionMatcher(vmatcher);
      }
      
      public VersionMatcher getVersionMatcher(String name) {
         return (VersionMatcher) _versionMatchers.get(name);
      }
      
      public void addVersionMatcher(VersionMatcher vmatcher) {
          init(vmatcher);
         _versionMatchers.put(vmatcher.getName(), vmatcher);
         
         if (_versionMatcher == null) {
        	 _versionMatcher = new ChainVersionMatcher();
        	 addVersionMatcher(new ExactVersionMatcher());
         }
         if (_versionMatcher instanceof ChainVersionMatcher) {
			ChainVersionMatcher chain = (ChainVersionMatcher) _versionMatcher;
			chain.add(vmatcher);
		}
      }
      
      public VersionMatcher[] getVersionMatchers() {
         return (VersionMatcher[]) _versionMatchers.values().toArray(new VersionMatcher[_versionMatchers.size()]);
      }

      public VersionMatcher getVersionMatcher() {
          if (_versionMatcher == null) {
              configureDefaultVersionMatcher();
          }
          return _versionMatcher;
      }

      public void configureDefaultVersionMatcher() {
          addVersionMatcher(new LatestVersionMatcher());
          addVersionMatcher(new SubVersionMatcher());
          addVersionMatcher(new VersionRangeMatcher());
      }


  	public CircularDependencyStrategy getCircularDependencyStrategy() {
  		if (_circularDependencyStrategy == null) {
  			_circularDependencyStrategy = getCircularDependencyStrategy("default");
  		}
  		return _circularDependencyStrategy;
  	}

  	public CircularDependencyStrategy getCircularDependencyStrategy(String name) {
  		if ("default".equals(name)) {
  			name = "warn";
  		}
		return (CircularDependencyStrategy)_circularDependencyStrategies.get(name);
	}

	public void setCircularDependencyStrategy(CircularDependencyStrategy strategy) {
  		_circularDependencyStrategy = strategy;
  	}
	
	public void addConfigured(CircularDependencyStrategy strategy) {
		addCircularDependencyStrategy(strategy);
	}

    private void addCircularDependencyStrategy(CircularDependencyStrategy strategy) {
		_circularDependencyStrategies.put(strategy.getName(), strategy);
	}
    
    private void configureDefaultCircularDependencyStrategies() {
    	addCircularDependencyStrategy(WarnCircularDependencyStrategy.getInstance());
    	addCircularDependencyStrategy(ErrorCircularDependencyStrategy.getInstance());
    	addCircularDependencyStrategy(IgnoreCircularDependencyStrategy.getInstance());
    }

    public StatusManager getStatusManager() {
        if (_statusManager == null) {
            _statusManager = StatusManager.newDefaultInstance();
        }
        return _statusManager;
    }
    public void setStatusManager(StatusManager statusManager) {
        _statusManager = statusManager;
    }


    /**
     * Returns true if the name should be ignored in listing
     * @param name
     * @return
     */
    public boolean listingIgnore(String name) {
        return _listingIgnore.contains(name);
    }
    
    /**
     * Filters the names list by removing all names that should be ignored
     * as defined by the listing ignore list
     * @param names
     */
    public void filterIgnore(Collection names) {
        names.removeAll(_listingIgnore);
    }
    
    public boolean isCheckUpToDate() {
        return _checkUpToDate;
    }
    public void setCheckUpToDate(boolean checkUpToDate) {
        _checkUpToDate = checkUpToDate;
    }

    public String getCacheArtifactPattern() {
        return _cacheArtifactPattern;
    }
    

    public void setCacheArtifactPattern(String cacheArtifactPattern) {
        _cacheArtifactPattern = cacheArtifactPattern;
    }
    

    public String getCacheIvyPattern() {
        return _cacheIvyPattern;
    }
    

    public void setCacheIvyPattern(String cacheIvyPattern) {
        _cacheIvyPattern = cacheIvyPattern;
    }
    

    public String getCacheDataFilePattern() {
        return _cacheDataFilePattern;
    }


    public boolean doValidate() {
        return _validate;
    }

    public void setValidate(boolean validate) {
        _validate = validate;
    }

    public String getVariable(String name) {
        String val = (String)_variables.get(name);
        return val==null?val:substitute(val);
    }

    public ConflictManager getDefaultConflictManager() {
        if (_defaultConflictManager == null) {
            _defaultConflictManager = new LatestConflictManager(getDefaultLatestStrategy());
        }
        return _defaultConflictManager;
    }
    

    public void setDefaultConflictManager(ConflictManager defaultConflictManager) {
        _defaultConflictManager = defaultConflictManager;
    }
    

    public LatestStrategy getDefaultLatestStrategy() {
        if (_defaultLatestStrategy == null) {
            _defaultLatestStrategy = new LatestRevisionStrategy();
        }
        return _defaultLatestStrategy;
    }
    

    public void setDefaultLatestStrategy(LatestStrategy defaultLatestStrategy) {
        _defaultLatestStrategy = defaultLatestStrategy;
    }
    
    public void addTrigger(Trigger trigger) {
    	init(trigger);
    	_triggers.add(trigger);
    }
    
    public List getTriggers() {
    	return _triggers;
    }

    public void addConfigured(Trigger trigger) {
    	addTrigger(trigger);
    }
    
    public boolean isUseRemoteConfig() {
        return _useRemoteConfig;
    }

    public void setUseRemoteConfig(boolean useRemoteConfig) {
        _useRemoteConfig = useRemoteConfig;
    }

    /** 
     * WARNING: Replace all current ivy variables by the given Map.
     * Should be used only when restoring variables.
     * 
     *  Thr given Map is not copied, but stored by reference.
     * @param variables
     */
    public void setVariables(Map variables) {
        if (variables == null) {
            throw new NullPointerException("variables shouldn't be null");
        }
        _variables = variables;
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
        if (_debugConflictResolution == null) {
            String var = getVariable("ivy.log.conflict.resolution");
            _debugConflictResolution =  Boolean.valueOf(var != null && Boolean.valueOf(var).booleanValue());
        }
        return _debugConflictResolution.booleanValue();
    }

    public boolean logNotConvertedExclusionRule() {
        return _logNotConvertedExclusionRule;
    }
    public void setLogNotConvertedExclusionRule(boolean logNotConvertedExclusionRule) {
        _logNotConvertedExclusionRule = logNotConvertedExclusionRule;
    }

    private void init(Object obj) {
		if (obj instanceof IvySettingsAware) {
            ((IvySettingsAware)obj).setSettings(this);
        }
		if (obj instanceof IvyAware) {
			//TODO
//            ((IvyAware)obj).setIvy(this);
        }
	}
    
    
	private static class ModuleSettings {
		private String _resolverName;
		private String _branch;
		private String _conflictManager;
		public ModuleSettings(String resolverName, String branch, String conflictManager) {
			_resolverName = resolverName;
			_branch = branch;
			_conflictManager = conflictManager;
		}
		public String toString() {
			return _resolverName != null ? "resolver: "+_resolverName:""
					+_branch != null ? "branch: "+_branch:"";
		}
		public String getBranch() {
			return _branch;
		}
		public String getResolverName() {
			return _resolverName;
		}
		protected String getConflictManager() {
			return _conflictManager;
		}
	}


	public String getCacheResolvedIvyPattern() {
		return _cacheResolvedIvyPattern;
	}

	public String getCacheResolvedIvyPropertiesPattern() {
		return _cacheResolvedIvyPropertiesPattern;
	}

	public long getInterruptTimeout() {
		return _interruptTimeout;
	}

	public Collection getResolvers() {
		return _resolversMap.values();
	}

	public Collection getResolverNames() {
		return _resolversMap.keySet();
	}

	public Collection getMatcherNames() {
		return _matchers.keySet();
	}

}
