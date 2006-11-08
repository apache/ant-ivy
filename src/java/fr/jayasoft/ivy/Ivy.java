/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.event.EventListenerList;

import org.xml.sax.SAXException;

import fr.jayasoft.ivy.IvyNode.EvictionData;
import fr.jayasoft.ivy.circular.CircularDependencyStrategy;
import fr.jayasoft.ivy.circular.ErrorCircularDependencyStrategy;
import fr.jayasoft.ivy.circular.IgnoreCircularDependencyStrategy;
import fr.jayasoft.ivy.circular.WarnCircularDependencyStrategy;
import fr.jayasoft.ivy.conflict.LatestConflictManager;
import fr.jayasoft.ivy.conflict.NoConflictManager;
import fr.jayasoft.ivy.conflict.StrictConflictManager;
import fr.jayasoft.ivy.event.FilteredIvyListener;
import fr.jayasoft.ivy.event.IvyEvent;
import fr.jayasoft.ivy.event.IvyEventFilter;
import fr.jayasoft.ivy.event.IvyListener;
import fr.jayasoft.ivy.event.Trigger;
import fr.jayasoft.ivy.event.download.PrepareDownloadEvent;
import fr.jayasoft.ivy.event.resolve.EndResolveEvent;
import fr.jayasoft.ivy.event.resolve.StartResolveEvent;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.latest.LatestLexicographicStrategy;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import fr.jayasoft.ivy.latest.LatestTimeStrategy;
import fr.jayasoft.ivy.matcher.ExactOrRegexpPatternMatcher;
import fr.jayasoft.ivy.matcher.ExactPatternMatcher;
import fr.jayasoft.ivy.matcher.GlobPatternMatcher;
import fr.jayasoft.ivy.matcher.Matcher;
import fr.jayasoft.ivy.matcher.MatcherHelper;
import fr.jayasoft.ivy.matcher.ModuleIdMatcher;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.matcher.RegexpPatternMatcher;
import fr.jayasoft.ivy.namespace.NameSpaceHelper;
import fr.jayasoft.ivy.namespace.Namespace;
import fr.jayasoft.ivy.parser.ModuleDescriptorParser;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.ConfigurationResolveReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.report.LogReportOutputter;
import fr.jayasoft.ivy.report.ReportOutputter;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.report.XmlReportOutputter;
import fr.jayasoft.ivy.repository.TransferEvent;
import fr.jayasoft.ivy.repository.TransferListener;
import fr.jayasoft.ivy.repository.url.URLResource;
import fr.jayasoft.ivy.resolver.AbstractResolver;
import fr.jayasoft.ivy.resolver.CacheResolver;
import fr.jayasoft.ivy.resolver.ChainResolver;
import fr.jayasoft.ivy.resolver.DualResolver;
import fr.jayasoft.ivy.resolver.ModuleEntry;
import fr.jayasoft.ivy.resolver.OrganisationEntry;
import fr.jayasoft.ivy.resolver.RevisionEntry;
import fr.jayasoft.ivy.status.StatusManager;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.PropertiesFile;
import fr.jayasoft.ivy.version.ChainVersionMatcher;
import fr.jayasoft.ivy.version.ExactVersionMatcher;
import fr.jayasoft.ivy.version.LatestVersionMatcher;
import fr.jayasoft.ivy.version.SubVersionMatcher;
import fr.jayasoft.ivy.version.VersionMatcher;
import fr.jayasoft.ivy.version.VersionRangeMatcher;
import fr.jayasoft.ivy.xml.XmlIvyConfigurationParser;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorUpdater;
import fr.jayasoft.ivy.xml.XmlReportParser;

/**
 * <a href="http://www.jayasoft.org/ivy">Ivy</a> is a free java based dependency manager.
 * 
 * This class is the main class of Ivy, which offers mainly dependency resolution.
 * 
 * Here is one typical usage:
 * Ivy ivy = new Ivy();
 * ivy.configure(new URL("ivyconf.xml"));
 * ivy.resolve(new URL("ivy.xml"));
 *  
 * @author Xavier Hanin
 *
 */
public class Ivy implements TransferListener {

	public static Ivy getCurrent() {
        Ivy cur = IvyContext.getContext().getIvy();
        if (cur == null) {
            cur = new Ivy();
            IvyContext.getContext().setIvy(cur);
        }
        return cur;
    }

    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

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
    private Map _moduleConfigurations = new LinkedHashMap(); // Map (ModuleIdMatcher -> ModuleSettings)
    
    private Map _conflictsManager = new HashMap(); // Map (String conflictManagerName -> ConflictManager)
    private Map _latestStrategies = new HashMap(); // Map (String latestStrategyName -> LatestStrategy)
    private Map _namespaces = new HashMap(); // Map (String namespaceName -> Namespace)
    private Map _matchers = new HashMap(); // Map (String matcherName -> Matcher)
    private Map _reportOutputters = new HashMap(); // Map (String outputterName -> ReportOutputter)
    private Map _versionMatchers = new HashMap(); // Map (String matcherName -> VersionMatcher)
    private Map _circularDependencyStrategies = new HashMap(); // Map (String name -> CircularDependencyStrategy)
    
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
    
    private Set _fetchedSet = new HashSet();

    private List _classpathURLs = new ArrayList();

    private ClassLoader _classloader;
    
    private StatusManager _statusManager;

	private long _interruptTimeout = 2000;

	private boolean _interrupted;
    
    public Ivy() {
        setVariable("ivy.default.conf.dir", Ivy.class.getResource("conf").toExternalForm(), true);
        
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
                typeDefs(Ivy.class.getResourceAsStream("typedef.properties"), true);
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
        
        addMatcher(ExactPatternMatcher.getInstance());
        addMatcher(RegexpPatternMatcher.getInstance());
        addMatcher(ExactOrRegexpPatternMatcher.getInstance());
        addMatcher(GlobPatternMatcher.getInstance());
        
        addReportOutputter(new XmlReportOutputter());
        addReportOutputter(new LogReportOutputter());
        
        configureDefaultCircularDependencyStrategies();
        
        _listingIgnore.add(".cvsignore");
        _listingIgnore.add("CVS");
        _listingIgnore.add(".svn");
        
        addSystemProperties();
        
        addTransferListener(new TransferListener() {
            public void transferProgress(TransferEvent evt) {
                switch (evt.getEventType()) {
                case TransferEvent.TRANSFER_PROGRESS:
                    Message.progress();
                    break;
                case TransferEvent.TRANSFER_COMPLETED:
                    Message.endProgress(" ("+(evt.getTotalLength() / 1024)+"kB)");
                    break;
                default:
                    break;
                }
            }
        });
        IvyContext.getContext().setIvy(this);
    }
    
    private void addSystemProperties() {
        addAllVariables(System.getProperties());
    }

    /**
     * Call this method to ask ivy to configure some variables using either a remote or a local properties file
     */
    public void configureRepositories(boolean remote) {
        IvyContext.getContext().setIvy(this);
        if (!_repositoriesConfigured) {
            Properties props = new Properties();
            boolean configured = false;
            if (_useRemoteConfig && remote) {
                try {
                    URL url = new URL("http://www.jayasoft.org/ivy/repository.properties");
                    Message.verbose("configuring repositories with "+url);
                    props.load(URLHandlerRegistry.getDefault().openStream(url));
                    configured = true;
                } catch (Exception ex) {
                    Message.verbose("unable to use remote repository configuration: "+ex.getMessage());
                    props = new Properties();
                }
            }
            if (!configured) {
                try {
                    props.load(Ivy.class.getResourceAsStream("repository.properties"));
                } catch (IOException e) {
                    Message.error("unable to use internal repository configuration: "+e.getMessage());
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
        IvyContext.getContext().setIvy(this);
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
        IvyContext.getContext().setIvy(this);
        for (Iterator iter = p.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            typeDef(name, p.getProperty(name), silentFail);
        }
    }
    
    
    /////////////////////////////////////////////////////////////////////////
    //                         CONFIGURATION
    /////////////////////////////////////////////////////////////////////////
    public void configure(File configurationFile) throws ParseException, IOException {
        IvyContext.getContext().setIvy(this);
        Message.info(":: configuring :: file = "+configurationFile);
        long start = System.currentTimeMillis();
        setConfigurationVariables(configurationFile);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(new File(getVariable("ivy.default.ivy.user.dir")));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();
        
        loadDefaultProperties();
        try {
            new XmlIvyConfigurationParser(this).parse(configurationFile.toURL());
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException("given file cannot be transformed to url: "+configurationFile);
            iae.initCause(e);
            throw iae;
        }
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        Message.verbose("configuration done ("+(System.currentTimeMillis()-start)+"ms)");
        dumpConfig();
    }

    public void configure(URL configurationURL) throws ParseException, IOException {
        IvyContext.getContext().setIvy(this);
        Message.info(":: configuring :: url = "+configurationURL);
        long start = System.currentTimeMillis();
        setConfigurationVariables(configurationURL);
        if (getVariable("ivy.default.ivy.user.dir") != null) {
            setDefaultIvyUserDir(new File(getVariable("ivy.default.ivy.user.dir")));
        } else {
            getDefaultIvyUserDir();
        }
        getDefaultCache();
        
        loadDefaultProperties();
        new XmlIvyConfigurationParser(this).parse(configurationURL);
        setVariable("ivy.default.ivy.user.dir", getDefaultIvyUserDir().getAbsolutePath(), false);
        Message.verbose("configuration done ("+(System.currentTimeMillis()-start)+"ms)");
        dumpConfig();
    }

	private void loadDefaultProperties() throws IOException {
		loadProperties(Ivy.class.getResource("ivy.properties"), false);
	}

    public void configureDefault() throws ParseException, IOException {
        configure(getDefaultConfigurationURL());
    }

    public void setConfigurationVariables(File configurationFile) {
        IvyContext.getContext().setIvy(this);
        try {
            setVariable("ivy.conf.dir", new File(configurationFile.getAbsolutePath()).getParent());
            setVariable("ivy.conf.file", configurationFile.getAbsolutePath());
            setVariable("ivy.conf.url", configurationFile.toURL().toExternalForm());
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException("given file cannot be transformed to url: "+configurationFile);
            iae.initCause(e);
            throw iae;
        }
    }
    
    public void setConfigurationVariables(URL configurationURL) {
        IvyContext.getContext().setIvy(this);
        String confURL = configurationURL.toExternalForm();
        setVariable("ivy.conf.url", confURL);
        int slashIndex = confURL.lastIndexOf('/');
        if (slashIndex != -1) {
            setVariable("ivy.conf.dir", confURL.substring(0, slashIndex));
        } else {
            Message.warn("configuration url does not contain any slash (/): ivy.conf.dir variable not set");
        }
    }
    
    private void dumpConfig() {
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
            resolver.dumpConfig();
        }
        if (!_moduleConfigurations.isEmpty()) {
            Message.debug("\tmodule configurations:");
            for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
                ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
                ModuleSettings s = (ModuleSettings)_moduleConfigurations.get(midm);
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
        if (resolver instanceof IvyAware) {
            ((IvyAware)resolver).setIvy(this);
        }
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
            throw new IllegalArgumentException("no resolver found called "+resolverName+": check your configuration");
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
        _moduleConfigurations.put(new ModuleIdMatcher(mid, matcher), new ModuleSettings(resolverName, branch, conflictManager));
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
        for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleConfigurations.get(midm);
            	if (ms.getResolverName() != null) {
            		return ms.getResolverName();
            	}
            }
        }
        return _defaultResolverName;
    }
    
	public String getDefaultBranch(ModuleId moduleId) {
        for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleConfigurations.get(midm);
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
        for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
            ModuleIdMatcher midm = (ModuleIdMatcher)iter.next();
            if (midm.matches(moduleId)) {
            	ModuleSettings  ms = (ModuleSettings)_moduleConfigurations.get(midm);
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
        if (cm instanceof IvyAware) {
            ((IvyAware)cm).setIvy(this);
        }
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
        if (latest instanceof IvyAware) {
            ((IvyAware)latest).setIvy(this);
        }
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
        if (ns instanceof IvyAware) {
            ((IvyAware)ns).setIvy(this);
        }
        _namespaces.put(ns.getName(), ns);
    }
    
    public void addConfigured(PatternMatcher m) {
        addMatcher(m);
    }
    
    public PatternMatcher getMatcher(String name) {
        return (PatternMatcher)_matchers.get(name);
    }
    
    public void addMatcher(PatternMatcher m) {
        if (m instanceof IvyAware) {
            ((IvyAware)m).setIvy(this);
        }
        _matchers.put(m.getName(), m);
    }
    
    public void addConfigured(ReportOutputter outputter) {
        addReportOutputter(outputter);
     }
     
     public ReportOutputter getReportOutputter(String name) {
        return (ReportOutputter) _reportOutputters.get(name);
     }
     
     public void addReportOutputter(ReportOutputter outputter) {
        if (outputter instanceof IvyAware) {
            ((IvyAware) outputter).setIvy(this);
        }
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
         if (vmatcher instanceof IvyAware) {
             ((IvyAware) vmatcher).setIvy(this);
         }
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

	/////////////////////////////////////////////////////////////////////////
    //                         CHECK
    /////////////////////////////////////////////////////////////////////////
    /**
     * Checks the given ivy file using current configuration to see if all dependencies
     * are available, with good confs. If a resolver name is given, it also checks that the declared
     * publications are available in the corresponding resolver.
     * Note that the check is not performed recursively, i.e. if a dependency has itself dependencies
     * badly described or not available, this check will not discover it. 
     */
    public boolean check(URL ivyFile, String resolvername) {
        try {
            IvyContext.getContext().setIvy(this);
            boolean result = true;
            // parse ivy file
            ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(this, ivyFile, doValidate());
            
            // check publications if possible
            if (resolvername != null) {
                DependencyResolver resolver = getResolver(resolvername);
                String[] confs = md.getConfigurationsNames();
                Set artifacts = new HashSet();
                for (int i = 0; i < confs.length; i++) {
                    artifacts.addAll(Arrays.asList(md.getArtifacts(confs[i])));
                }
                for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
                    Artifact art = (Artifact)iter.next();
                    if (!resolver.exists(art)) {
                        Message.info("declared publication not found: "+art);
                        result = false;
                    }
                }
            }
            
            // check dependencies
            DependencyDescriptor[] dds = md.getDependencies();
            ResolveData data = new ResolveData(this, getDefaultCache(), new Date(), null, true);
            for (int i = 0; i < dds.length; i++) {
                // check master confs
                String[] masterConfs = dds[i].getModuleConfigurations();
                for (int j = 0; j < masterConfs.length; j++) {
                    if (!"*".equals(masterConfs[j].trim()) && md.getConfiguration(masterConfs[j]) == null) {
                        Message.info("dependency required in non existing conf for "+ivyFile+" \n\tin "+dds[i].getDependencyRevisionId()+": "+masterConfs[j]);
                        result = false;
                    }
                }
                // resolve
                DependencyResolver resolver = getResolver(dds[i].getDependencyId());
                ResolvedModuleRevision rmr = resolver.getDependency(dds[i], data);
                if (rmr == null) {
                    Message.info("dependency not found in "+ivyFile+":\n\t"+dds[i]);
                    result = false;
                } else {
                    String[] depConfs = dds[i].getDependencyConfigurations(md.getConfigurationsNames());
                    for (int j = 0; j < depConfs.length; j++) {
                        if (!Arrays.asList(rmr.getDescriptor().getConfigurationsNames()).contains(depConfs[j])) {
                            Message.info("dependency configuration is missing for "+ivyFile+"\n\tin "+dds[i].getDependencyRevisionId()+": "+depConfs[j]);
                            result = false;
                        }
                        Artifact[] arts = rmr.getDescriptor().getArtifacts(depConfs[j]);
                        for (int k = 0; k < arts.length; k++) {
                            if (!resolver.exists(arts[k])) {
                                Message.info("dependency artifact is missing for "+ivyFile+"\n\t in "+dds[i].getDependencyRevisionId()+": "+arts[k]);
                                result = false;
                            }
                        }
                    }
                }
            }
            return result;
        } catch (ParseException e) {
            Message.info("parse problem on "+ivyFile+": "+e);
            return false;
        } catch (IOException e) {
            Message.info("io problem on "+ivyFile+": "+e);
            return false;
        } catch (Exception e) {
            Message.info("problem on "+ivyFile+": "+e);
            return false;
        }
    }
    

	public ResolvedModuleRevision findModule(ModuleRevisionId id) {
		DependencyResolver r = getResolver(id.getModuleId());
		if (r == null) {
			throw new IllegalStateException("no resolver found for "+id.getModuleId());
		}
        DefaultModuleDescriptor md = DefaultModuleDescriptor.newCallerInstance(id, new String[] {"*"}, false, false);
		try {
			return r.getDependency(new DefaultDependencyDescriptor(id, true), new ResolveData(this, getDefaultCache(), null, new ConfigurationResolveReport(this, md, "default", null, getDefaultCache()), false));
		} catch (ParseException e) {
			throw new RuntimeException("problem whle parsing repository module descriptor for "+id+": "+e, e);
		}
	}

	/////////////////////////////////////////////////////////////////////////
    //                         RESOLVE
    /////////////////////////////////////////////////////////////////////////

    
    public ResolveReport resolve(File ivySource) throws ParseException, IOException {
    	return resolve(ivySource.toURL());
    }
    public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
    	return resolve(ivySource, null, new String[] {"*"}, null, null, true);
    }
    /**
     * 
     * @param ivySource the url to the descriptor of the module for which dependencies should be resolved
     * @param revision the revision of the module for which dependencies should be resolved.
     * This revision is considered as the resolved revision of the module, unless it is null.
     * If it is null, then a default revision is given if necessary (no revision found in ivy file)
     * @param confs the configurations for which dependencies should be resolved
     * @param cache the directory where to place resolved dependencies
     * @param date the date for which the dependencies should be resolved. All obtained artifacts 
     * should have a publication date which is before or equal to the given date
     * @throws ParseException
     * @throws IOException
     * @throws NullPointerException if any parameter is null except cache or date
     */
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
        return resolve(ivySource, revision, confs, cache, date, validate, false);
    }
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly) throws ParseException, IOException {
        return resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, FilterHelper.NO_FILTER);
    }

    /**
     * Resolves the module identified by the given mrid with its dependencies. 
     */
	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs) throws ParseException, IOException {
        return resolve(mrid, confs, true, false, null, null, true, false, FilterHelper.NO_FILTER);
	}

	public ResolveReport resolve(final ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
		return resolve(mrid, confs, transitive, changing, cache, date, validate, useCacheOnly, false, artifactFilter);
	}

    /**
     * Resolves the module identified by the given mrid with its dependencies if transitive is set to true. 
     */
	public ResolveReport resolve(final ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
		DefaultModuleDescriptor md;
		if (confs.length == 1 && confs[0].equals("*")) {
			ResolvedModuleRevision rmr = findModule(mrid);
			if (rmr == null) {
				md = DefaultModuleDescriptor.newCallerInstance(mrid, confs, transitive, changing);
				return new ResolveReport(md){
					public boolean hasError() {
						return true;
					}
					public List getProblemMessages() {
						return Arrays.asList(new String[] {"module not found: "+mrid});
					}
				};
			} else {
				confs = rmr.getDescriptor().getConfigurationsNames();
				md = DefaultModuleDescriptor.newCallerInstance(ModuleRevisionId.newInstance(mrid, rmr.getId().getRevision()), confs, transitive, changing);
			}
		} else {
			md = DefaultModuleDescriptor.newCallerInstance(mrid, confs, transitive, changing);
		}
		
		return resolve(md, new String[] {"*"}, cache, date, validate, useCacheOnly, true, useOrigin, true, true, artifactFilter);
	}
	
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
    	return resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, true, artifactFilter);
    }
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException {
    	return resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, transitive, false, artifactFilter);
    }
    /**
     * Resolve dependencies of a module described by an ivy file.
     * 
     * Note: the method signature is way too long, we should use a class to store the settings of the resolve.
     */
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        
        URLResource res = new URLResource(ivySource);
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
        Message.verbose("using "+parser+" to parse "+ivySource);
        ModuleDescriptor md = parser.parseDescriptor(this, ivySource, validate);
        if (revision == null && md.getResolvedModuleRevisionId().getRevision() == null) {
            revision = "working@"+getLocalHostName();
        }
        if (revision != null) {
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(md.getModuleRevisionId(), revision));
        }

        return resolve(md, confs, cache, date, validate, useCacheOnly, transitive, useOrigin, true, true, artifactFilter);
    }

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return resolve(md, confs, cache, date, validate, useCacheOnly, true, artifactFilter);
	}
	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return resolve(md, confs, cache, date, validate, useCacheOnly, transitive, true, true, artifactFilter);
	}
	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return resolve(md, confs, cache, date, validate, useCacheOnly, transitive, false, download, outputReport, artifactFilter);
	}
    /**
     * Resolve dependencies of a module described by a module descriptor
     * 
     * Note: the method signature is way too long, we should use a class to store the settings of the resolve.
     */
	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		IvyContext.getContext().setIvy(this);
        DependencyResolver oldDictator = getDictatorResolver();
        if (useCacheOnly) {
        	setDictatorResolver(new CacheResolver(this));
        }
        try {
            if (cache==null) {  // ensure that a cache exists
                cache = getDefaultCache();
                IvyContext.getContext().setCache(cache);
            }
            if (artifactFilter == null) {
            	artifactFilter = FilterHelper.NO_FILTER;
            }
            if (confs.length == 1 && confs[0].equals("*")) {
                confs = md.getConfigurationsNames();
            }
            fireIvyEvent(new StartResolveEvent(this, md, confs));
            
            long start = System.currentTimeMillis();
            Message.info(":: resolving dependencies :: "+md.getResolvedModuleRevisionId()+(transitive?"":" [not transitive]"));
            Message.info("\tconfs: "+Arrays.asList(confs));
            Message.verbose("\tvalidate = "+validate);
            ResolveReport report = new ResolveReport(md);

            // resolve dependencies
            IvyNode[] dependencies = getDependencies(md, confs, cache, date, report, validate, transitive);
            report.setDependencies(Arrays.asList(dependencies), artifactFilter);

            
            // produce resolved ivy file and ivy properties in cache
            File ivyFileInCache = getResolvedIvyFileInCache(cache, md.getResolvedModuleRevisionId());
            md.toIvyFile(ivyFileInCache);

            // we store the resolved dependencies revisions and statuses per asked dependency revision id,
            // for direct dependencies only.
            // this is used by the deliver task to resolve dynamic revisions to static ones
            File ivyPropertiesInCache = getResolvedIvyPropertiesInCache(cache, md.getResolvedModuleRevisionId());
            Properties props = new Properties();
            if (dependencies.length > 0) {
            	IvyNode root = dependencies[0].getRoot();
            	for (int i = 0; i < dependencies.length; i++) {
            		if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
            			DependencyDescriptor dd = dependencies[i].getDependencyDescriptor(root);
            			if (dd != null) {
            				String rev = dependencies[i].getResolvedId().getRevision();
            				String status = dependencies[i].getDescriptor().getStatus();
            				props.put(dd.getDependencyRevisionId().encodeToString(), rev+" "+status);
            			}
            		}
            	}
            }
            props.store(new FileOutputStream(ivyPropertiesInCache), md.getResolvedModuleRevisionId()+ " resolved revisions");
            Message.verbose("\tresolved ivy file produced in "+ivyFileInCache);
            
            report.setResolveTime(System.currentTimeMillis()-start);

            if (download) {
	            Message.verbose(":: downloading artifacts ::");
	
	            downloadArtifacts(report, cache, useOrigin, artifactFilter);
            }
            
            
            if (outputReport) {
            	outputReport(report, cache);
            }
            
            fireIvyEvent(new EndResolveEvent(this, md, confs, report));
            return report;
        } finally {
            setDictatorResolver(oldDictator);
        }
	}

	public void outputReport(ResolveReport report, File cache) {
		Message.info(":: resolution report ::");
		report.setProblemMessages(Message.getProblems());
		// output report
		report.output(getReportOutputters(), cache);
		
		Message.verbose("\tresolve done ("+report.getResolveTime()+"ms resolve - "+report.getDownloadTime()+"ms download)");
		Message.sumupProblems();
	}

    public void downloadArtifacts(ResolveReport report, File cache, boolean useOrigin, Filter artifactFilter) {
    	long start = System.currentTimeMillis();
    	IvyNode[] dependencies = (IvyNode[]) report.getDependencies().toArray(new IvyNode[report.getDependencies().size()]);
        
        fireIvyEvent(new PrepareDownloadEvent(this, (Artifact[])report.getArtifacts().toArray(new Artifact[report.getArtifacts().size()])));
        
        for (int i = 0; i < dependencies.length; i++) {
        	checkInterrupted();
            //download artifacts required in all asked configurations
            if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
                DependencyResolver resolver = dependencies[i].getModuleRevision().getArtifactResolver();
                Artifact[] selectedArtifacts = dependencies[i].getSelectedArtifacts(artifactFilter);
                DownloadReport dReport = resolver.download(selectedArtifacts, this, cache, useOrigin);
                ArtifactDownloadReport[] adrs = dReport.getArtifactsReports();
                for (int j = 0; j < adrs.length; j++) {
                    if (adrs[j].getDownloadStatus() == DownloadStatus.FAILED) {
                        Message.warn("\t[NOT FOUND  ] "+adrs[j].getArtifact());
                        resolver.reportFailure(adrs[j].getArtifact());
                    }
                }
                // update concerned reports
                String[] dconfs = dependencies[i].getRootModuleConfigurations();
                for (int j = 0; j < dconfs.length; j++) {
                    // the report itself is responsible to take into account only
                    // artifacts required in its corresponding configuration
                    // (as described by the Dependency object)
                    if (dependencies[i].isEvicted(dconfs[j])) {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i]);
                    } else {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i], dReport);
                    }
                }
            }
        }
        report.setDownloadTime(System.currentTimeMillis() - start);
    }

    /**
     * Check if the current operation has been interrupted, and if it is the case, throw a runtime exception
     */
	public void checkInterrupted() {
		if (isInterrupted()) {
			Message.info("operation interrupted");
			throw new RuntimeException("operation interrupted");
		}
	}
    
    /**
     * Download an artifact to the cache.
     * Not used internally, useful especially for IDE plugins
     * needing to download artifact one by one (for source or javadoc artifact,
     * for instance).
     * 
     * Downloaded artifact file can be accessed using getArchiveFileInCache method.
     * 
     * It is possible to track the progression of the download using classical ivy 
     * progress monitoring feature (see addTransferListener).
     * 
     * @param artifact the artifact to download
     * @param cache the cache to use. If null, will use default cache
     * @return a report concerning the download
     */
    public ArtifactDownloadReport download(Artifact artifact, File cache) {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        if (cache == null) {
            cache = getDefaultCache();
        }
        DependencyResolver resolver = getResolver(artifact.getModuleRevisionId().getModuleId());
        DownloadReport r = resolver.download(new Artifact[] {artifact}, this, cache, false);
        return r.getArtifactReport(artifact);
    }
    
    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts.
     * The module to resolve is given by its ivy file URL. This method requires
     * appropriate configuration of the ivy instance, especially resolvers.
     * 
     * @param ivySource url of the ivy file to use for dependency resolving
     * @param confs an array of configuration names to resolve - must not be null nor empty
     * @param cache the cache to use - default cache is used if null
     * @param date the date to which resolution must be done - may be null
     * @return an array of the resolved dependencies
     * @throws ParseException if a parsing problem occured in the ivy file
     * @throws IOException if an IO problem was raised during ivy file parsing
     */
    public IvyNode[] getDependencies(URL ivySource, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
        return getDependencies(ModuleDescriptorParserRegistry.getInstance().parseDescriptor(this, ivySource, validate), confs, cache, date, null, validate);
    }
    
    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts.
     * The module to resolve is given by its module descriptor.This method requires
     * appropriate configuration of the ivy instance, especially resolvers.
     * 
     * @param md the descriptor of the module for which we want to get dependencies - must not be null
     * @param confs an array of configuration names to resolve - must not be null nor empty
     * @param cache the cache to use - default cache is used if null
     * @param date the date to which resolution must be done - may be null
     * @param report a resolve report to fill during resolution - may be null
     * @return an array of the resolved Dependencies
     */
    public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate) {
    	return getDependencies(md, confs, cache, date, report, validate, true);
    }
    public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate, boolean transitive) {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        if (md == null) {
            throw new NullPointerException("module descriptor must not be null");
        }
        if (cache==null) {  // ensure that a cache exists
            cache = getDefaultCache();
        }
        if (confs.length == 1 && confs[0].equals("*")) {
            confs = md.getConfigurationsNames();
        }
        
        Map dependenciesMap = new LinkedHashMap();
        Date reportDate = new Date();
        ResolveData data = new ResolveData(this, cache, date, null, validate, transitive, dependenciesMap);
        IvyNode rootNode = new IvyNode(data, md);
        
        for (int i = 0; i < confs.length; i++) {
            // for each configuration we clear the cache of what's been fetched
            _fetchedSet.clear();     
            
            Configuration configuration = md.getConfiguration(confs[i]);
            if (configuration == null) {
                Message.error("asked configuration not found in "+md.getModuleRevisionId()+": "+confs[i]);
            } else {
                ConfigurationResolveReport confReport = null;
                if (report != null) {
                    confReport = report.getConfigurationReport(confs[i]);
                    if (confReport == null) {
                        confReport = new ConfigurationResolveReport(this, md, confs[i], reportDate, cache);
                        report.addReport(confs[i], confReport);
                    }
                }
                // we reuse the same resolve data with a new report for each conf
                data.setReport(confReport); 
                
                // update the root module conf we are about to fetch
                rootNode.setRootModuleConf(confs[i]); 
                rootNode.setRequestedConf(confs[i]);
                rootNode.updateConfsToFetch(Collections.singleton(confs[i]));
                
                // go fetch !
                fetchDependencies(rootNode, confs[i], false);
            }
        }
        
        // prune and reverse sort fectched dependencies 
        Collection dependencies = new LinkedHashSet(dependenciesMap.size()); // use a Set to avoids duplicates
        for (Iterator iter = dependenciesMap.values().iterator(); iter.hasNext();) {
            IvyNode dep = (IvyNode) iter.next();
            if (dep != null) {
                dependencies.add(dep);
            }
        }
        List sortedDependencies = sortNodes(dependencies);
        Collections.reverse(sortedDependencies);

        // handle transitive eviction now:
        // if a module has been evicted then all its dependencies required 
        // only by it should be evicted too. Since nodes are now sorted from the more dependent to 
        // the less one, we can traverse the list and check only the direct parent and not all
        // the ancestors
        for (ListIterator iter = sortedDependencies.listIterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (!node.isCompletelyEvicted()) {
                for (int i = 0; i < confs.length; i++) {
                    IvyNode.Caller[] callers = node.getCallers(confs[i]);
                    if (debugConflictResolution()) {
                        Message.debug("checking if "+node.getId()+" is transitively evicted in "+confs[i]);
                    }
                    boolean allEvicted = callers.length > 0;
                    for (int j = 0; j < callers.length; j++) {
                        if (callers[j].getModuleRevisionId().equals(md.getModuleRevisionId())) {
                            // the caller is the root module itself, it can't be evicted
                            allEvicted = false;
                            break;                            
                        } else {
                            IvyNode callerNode = (IvyNode)dependenciesMap.get(callers[j].getModuleRevisionId());
                            if (callerNode == null) {
                                Message.warn("ivy internal error: no node found for "+callers[j].getModuleRevisionId()+": looked in "+dependenciesMap.keySet()+" and root module id was "+md.getModuleRevisionId());
                            } else if (!callerNode.isEvicted(confs[i])) {
                                allEvicted = false;
                                break;
                            } else {
                                if (debugConflictResolution()) {
                                    Message.debug("caller "+callerNode.getId()+" of "+node.getId()+" is evicted");
                                }
                            }
                        }
                    }
                    if (allEvicted) {
                        Message.verbose("all callers are evicted for "+node+": evicting too");
                        node.markEvicted(confs[i], null, null, null);
                    } else {
                        if (debugConflictResolution()) {
                            Message.debug(node.getId()+" isn't transitively evicted, at least one caller was not evicted");
                        }
                    }
                }
            }
        }
        
        return (IvyNode[]) dependencies.toArray(new IvyNode[dependencies.size()]);
    }


    
    
    private void fetchDependencies(IvyNode node, String conf, boolean shouldBePublic) {
    	checkInterrupted();
        long start = System.currentTimeMillis();
        if (debugConflictResolution()) {
            Message.debug(node.getId()+" => resolving dependencies in "+conf);
        }
        resolveConflict(node, node.getParent());
        
        if (node.loadData(conf, shouldBePublic)) {
            node = node.getRealNode(true); // if data loading discarded the node, get the real one
            
            resolveConflict(node, node.getParent());
            if (!node.isEvicted(node.getRootModuleConf())) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        } else if (!node.hasProblem()) {
            // the node has not been loaded but hasn't problem: it was already loaded 
            // => we just have to update its dependencies data
            if (!node.isEvicted(node.getRootModuleConf())) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        }
        if (node.isEvicted(node.getRootModuleConf())) {
            // update selected nodes with confs asked in evicted one
            IvyNode.EvictionData ed = node.getEvictedData(node.getRootModuleConf());
            if (ed.getSelected() != null) {
            	for (Iterator iter = ed.getSelected().iterator(); iter.hasNext();) {
            		IvyNode selected = (IvyNode)iter.next();
            		fetchDependencies(selected, conf, true);
            	}
            }
        }
        if (debugConflictResolution()) {
            Message.debug(node.getId()+" => dependencies resolved in "+conf+" ("+(System.currentTimeMillis()-start)+"ms)");
        }
    }

    private void doFetchDependencies(IvyNode node, String conf) {
        Configuration c = node.getConfiguration(conf);
        if (c == null) {
            Message.warn("configuration not found '"+conf+"' in "+node.getResolvedId()+": ignoring");
            if (node.getParent() != null) {
                Message.warn("it was required from "+node.getParent().getResolvedId());
            }
            return;
        }
        // we handle the case where the asked configuration extends others:
        // we have to first fetch the extended configurations
        
        // first we check if this is the actual requested conf (not an extended one)
        boolean requestedConfSet = false;
        if (node.getRequestedConf()==null) {
            node.setRequestedConf(conf);
            requestedConfSet = true;
        }
        // now let's recurse in extended confs
        String[] extendedConfs = c.getExtends();
        if (extendedConfs.length > 0) {
            node.updateConfsToFetch(Arrays.asList(extendedConfs));
        }
        for (int i = 0; i < extendedConfs.length; i++) {
            fetchDependencies(node, extendedConfs[i], false);
        }
        
        // now we can actually resolve this configuration dependencies
        DependencyDescriptor dd = node.getDependencyDescriptor(node.getParent());
        if (!isDependenciesFetched(node, conf) && (dd == null || node.isTransitive())) {
            Collection dependencies = node.getDependencies(conf, true);
            for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                IvyNode dep = (IvyNode)iter.next();
                dep = dep.getRealNode(); // the node may have been resolved to another real one while resolving other deps
                node.traverse(conf, dep); // dependency traversal data may have been changed while resolving other deps, we update it
                if (dep.isCircular()) {
                    continue;
                }
                String[] confs = dep.getRequiredConfigurations(node, conf);
                for (int i = 0; i < confs.length; i++) {
                    fetchDependencies(dep, confs[i], true);
                }
                // if there are still confs to fetch (usually because they have
                // been updated when evicting another module), we fetch them now
                confs = dep.getConfsToFetch();
                for (int i = 0; i < confs.length; i++) {
                    fetchDependencies(dep, confs[i], true);
                }
            }
        }
        // we have finiched with this configuration, if it was the original requested conf
        // we can clean it now
        if (requestedConfSet) {
        	node.setRequestedConf(null);
        }
        
    }


    /**
     * Returns true if we've already fetched the dependencies for this node and configuration
     * @param node node to check
     * @param conf configuration to check
     * @return true if we've already fetched this dependency
     */
    private boolean isDependenciesFetched(IvyNode node, String conf) {
        ModuleId moduleId = node.getModuleId();
        ModuleRevisionId moduleRevisionId = node.getResolvedId();
        String key = moduleId.getOrganisation()+"|"+moduleId.getName()+"|"+moduleRevisionId.getRevision() +
            "|" + conf;
        if (_fetchedSet.contains(key)) {
            return true;
        }
        _fetchedSet.add(key);
        return false;
    }    

    private void resolveConflict(IvyNode node, IvyNode parent) {
        resolveConflict(node, parent, Collections.EMPTY_SET);
    }
    private void resolveConflict(IvyNode node, IvyNode parent, Collection toevict) {
        if (parent == null || node == parent) {
            return;
        }
        // check if job is not already done
        if (checkConflictSolved(node, parent)) {
            return;
        }
        
        // compute conflicts
        Collection resolvedNodes = new HashSet(parent.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()));
        Collection conflicts = computeConflicts(node, parent, toevict, resolvedNodes);
        if (debugConflictResolution()) {
            Message.debug("found conflicting revisions for "+node+" in "+parent+": "+conflicts);
        }
        
        Collection resolved = parent.getConflictManager(node.getModuleId()).resolveConflicts(parent, conflicts);
        if (debugConflictResolution()) {
            Message.debug("selected revisions for "+node+" in "+parent+": "+resolved);
        }
        if (resolved.contains(node)) {
            // node has been selected for the current parent
            
            // handle previously selected nodes that are now evicted by this new node
            toevict = resolvedNodes;
            toevict.removeAll(resolved);
            
            for (Iterator iter = toevict.iterator(); iter.hasNext();) {
                IvyNode te = (IvyNode)iter.next();
                te.markEvicted(node.getRootModuleConf(), parent, parent.getConflictManager(node.getModuleId()), resolved);
                
                if (debugConflictResolution()) {
                    Message.debug("evicting "+te+" by "+te.getEvictedData(node.getRootModuleConf()));
                }
            }
            
            // it's very important to update resolved and evicted nodes BEFORE recompute parent call
            // to allow it to recompute its resolved collection with correct data
            // if necessary            
            parent.setResolvedNodes(node.getModuleId(), node.getRootModuleConf(), resolved); 

            Collection evicted = new HashSet(parent.getEvictedNodes(node.getModuleId(), node.getRootModuleConf()));
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            parent.setEvictedNodes(node.getModuleId(), node.getRootModuleConf(), evicted);
            
            resolveConflict(node, parent.getParent(), toevict);
        } else {
            // node has been evicted for the current parent
            if (resolved.isEmpty()) {
                if (debugConflictResolution()) {
                    Message.verbose("conflict manager '"+parent.getConflictManager(node.getModuleId())+"' evicted all revisions among "+conflicts);
                }
            }
            
            
            // it's time to update parent resolved and evicted with what was found 
            
            Collection evicted = new HashSet(parent.getEvictedNodes(node.getModuleId(), node.getRootModuleConf()));
            toevict.removeAll(resolved);
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            evicted.add(node);
            parent.setEvictedNodes(node.getModuleId(), node.getRootModuleConf(), evicted);

            
            node.markEvicted(node.getRootModuleConf(), parent, parent.getConflictManager(node.getModuleId()), resolved);
            if (debugConflictResolution()) {
                Message.debug("evicting "+node+" by "+node.getEvictedData(node.getRootModuleConf()));
            }

            // if resolved changed we have to go up in the graph
            Collection prevResolved = parent.getResolvedNodes(node.getModuleId(), node.getRootModuleConf());
            if (!prevResolved.equals(resolved)) {                
                parent.setResolvedNodes(node.getModuleId(), node.getRootModuleConf(), resolved);
                for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                    IvyNode sel = (IvyNode)iter.next();
                    if (!prevResolved.contains(sel)) {
                        resolveConflict(sel, parent.getParent(), toevict);
                    }
                }
            }

        }
    }

    private Collection computeConflicts(IvyNode node, IvyNode parent, Collection toevict, Collection resolvedNodes) {
        Collection conflicts = new HashSet();
        if (resolvedNodes.removeAll(toevict)) {
            // parent.resolved(node.mid) is not up to date:
            // recompute resolved from all sub nodes
            conflicts.add(node);
            Collection deps = parent.getDependencies(parent.getRequiredConfigurations());
            for (Iterator iter = deps.iterator(); iter.hasNext();) {
                IvyNode dep = (IvyNode)iter.next();
                conflicts.addAll(dep.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()));
            }
        } else if (resolvedNodes.isEmpty() && node.getParent() != parent) {
            conflicts.add(node);
            DependencyDescriptor[] dds = parent.getDescriptor().getDependencies();
            for (int i = 0; i < dds.length; i++) {
                if (dds[i].getDependencyId().equals(node.getModuleId())) {
                    IvyNode n = node.findNode(dds[i].getDependencyRevisionId());
                    if (n != null) {
                        conflicts.add(n);
                        break;
                    }
                }
            }
        } else {
            conflicts.add(node);
            conflicts.addAll(resolvedNodes);
        }
        return conflicts;
    }

    private boolean checkConflictSolved(IvyNode node, IvyNode parent) {
        if (parent.getResolvedRevisions(node.getModuleId(), node.getRootModuleConf()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            // => job already done, we just have to check if the node wasn't previously evicted in root ancestor
            if (debugConflictResolution()) {
                Message.debug("conflict resolution already done for "+node+" in "+parent);
            }
            EvictionData evictionData = node.getEvictionDataInRoot(node.getRootModuleConf(), parent);
            if (evictionData != null) {
                // node has been previously evicted in an ancestor: we mark it as evicted
                if (debugConflictResolution()) {
                    Message.debug(node+" was previously evicted in root module conf "+node.getRootModuleConf());
                }

                node.markEvicted(evictionData);                
                if (debugConflictResolution()) {
                    Message.debug("evicting "+node+" by "+evictionData);
                }
            }
            return true;
        } else if (parent.getEvictedRevisions(node.getModuleId(), node.getRootModuleConf()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            // => job already done, we just have to check if the node wasn't previously selected in root ancestor
            if (debugConflictResolution()) {
                Message.debug("conflict resolution already done for "+node+" in "+parent);
            }
            return true;
        }
        return false;
    }

    public ResolvedModuleRevision findModuleInCache(ModuleRevisionId mrid, File cache, boolean validate) {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        // first, check if it is in cache
        if (!getVersionMatcher().isDynamic(mrid)) {
            File ivyFile = getIvyFileInCache(cache, mrid);
            if (ivyFile.exists()) {
                // found in cache !
                try {
                    ModuleDescriptor depMD = XmlModuleDescriptorParser.getInstance().parseDescriptor(this, ivyFile.toURL(), validate);
                    String resolverName = getSavedResolverName(cache, depMD);
                    String artResolverName = getSavedArtResolverName(cache, depMD);
                    DependencyResolver resolver = (DependencyResolver)_resolversMap.get(resolverName);
                    if (resolver == null) {
                        Message.debug("\tresolver not found: "+resolverName+" => trying to use the one configured for "+mrid);                                    
                        resolver = getResolver(depMD.getResolvedModuleRevisionId().getModuleId());
                        if (resolver != null) {
                            Message.debug("\tconfigured resolver found for "+depMD.getResolvedModuleRevisionId()+": "+resolver.getName()+": saving this data");                                    
                            saveResolver(cache, depMD, resolver.getName());
                        }
                    }
                    DependencyResolver artResolver = (DependencyResolver)_resolversMap.get(artResolverName);
                    if (artResolver == null) {
                        artResolver = resolver;
                    }
                    if (resolver != null) {
                        Message.debug("\tfound ivy file in cache for "+mrid+" (resolved by "+resolver.getName()+"): "+ivyFile);
                        return new DefaultModuleRevision(resolver, artResolver, depMD, false, false, ivyFile.toURL());
                    } else {
                        Message.debug("\tresolver not found: "+resolverName+" => cannot use cached ivy file for "+mrid);                                    
                    }
                } catch (Exception e) {
                    // will try with resolver
                    Message.debug("\tproblem while parsing cached ivy file for: "+mrid+": "+e.getMessage());                                    
                }
            } else {
                Message.debug("\tno ivy file in cache for "+mrid+": tried "+ivyFile);
            }
        }
        return null;
    }


    /////////////////////////////////////////////////////////////////////////
    //                         INSTALL
    /////////////////////////////////////////////////////////////////////////
    
    public ResolveReport install(ModuleRevisionId mrid, String from, String to, boolean transitive, boolean validate, boolean overwrite, Filter artifactFilter, File cache, String matcherName) throws IOException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        if (cache == null) {
            cache = getDefaultCache();
        }
        if (artifactFilter == null) {
            artifactFilter = FilterHelper.NO_FILTER;
        }
        DependencyResolver fromResolver = getResolver(from);
        DependencyResolver toResolver = getResolver(to);
        if (fromResolver == null) {
            throw new IllegalArgumentException("unknown resolver "+from+". Available resolvers are: "+_resolversMap.keySet());
        }
        if (toResolver == null) {
            throw new IllegalArgumentException("unknown resolver "+to+". Available resolvers are: "+_resolversMap.keySet());
        }
        PatternMatcher matcher = getMatcher(matcherName);
        if (matcher == null) {
            throw new IllegalArgumentException("unknown matcher "+matcherName+". Available matchers are: "+_matchers.keySet());
        }
        
        // build module file declaring the dependency
        Message.info(":: installing "+mrid+" ::");
        DependencyResolver oldDicator = getDictatorResolver();
        boolean log = logNotConvertedExclusionRule();
        try {
            setLogNotConvertedExclusionRule(true);
            setDictatorResolver(fromResolver);
            
            DefaultModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("jayasoft", "ivy-install", "1.0"), getStatusManager().getDefaultStatus(), new Date());
            md.addConfiguration(new Configuration("default"));
            md.addConflictManager(new ModuleId(ExactPatternMatcher.ANY_EXPRESSION, ExactPatternMatcher.ANY_EXPRESSION), ExactPatternMatcher.getInstance(), new NoConflictManager());
            
            if (MatcherHelper.isExact(matcher, mrid)) {
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, mrid, false, false, transitive);
                dd.addDependencyConfiguration("default", "*");
                md.addDependency(dd);
            } else {
                Collection mrids = findModuleRevisionIds(fromResolver, mrid, matcher); 
                                
                for (Iterator iter = mrids.iterator(); iter.hasNext();) {
                    ModuleRevisionId foundMrid = (ModuleRevisionId)iter.next();
                    Message.info("\tfound "+foundMrid+" to install: adding to the list");
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, foundMrid, false, false, transitive);
                    dd.addDependencyConfiguration("default", "*");
                    md.addDependency(dd);
                }
            }                       
            
            // resolve using appropriate resolver
            ResolveReport report = new ResolveReport(md);
            
            Message.info(":: resolving dependencies ::");
            IvyNode[] dependencies = getDependencies(md, new String[] {"default"}, cache, null, report, validate);
            report.setDependencies(Arrays.asList(dependencies), artifactFilter);
            
            Message.info(":: downloading artifacts to cache ::");
            downloadArtifacts(report, cache, false, artifactFilter);

            // now that everything is in cache, we can publish all these modules
            Message.info(":: installing in "+to+" ::");
            for (int i = 0; i < dependencies.length; i++) {
                ModuleDescriptor depmd = dependencies[i].getDescriptor();
                if (depmd != null) {
                    Message.verbose("installing "+depmd.getModuleRevisionId());
                    publish(depmd, 
                            toResolver, 
                            Collections.singleton(cache.getAbsolutePath()+"/"+getCacheArtifactPattern()), 
                            cache.getAbsolutePath()+"/"+getCacheIvyPattern(), 
                            null,
                            overwrite,
                            null);
                }
            }

            Message.info(":: install resolution report ::");
            
            // output report
            report.output(getReportOutputters(), cache);

            return report;
        } finally {
            setDictatorResolver(oldDicator);
            setLogNotConvertedExclusionRule(log);
        }
    }

    public Collection findModuleRevisionIds(DependencyResolver resolver, ModuleRevisionId pattern, PatternMatcher matcher) {
        IvyContext.getContext().setIvy(this);
        Collection mrids = new ArrayList();
        String resolverName = resolver.getName();
        
        Message.verbose("looking for modules matching "+pattern+" using "+matcher.getName());
        Namespace fromNamespace = resolver instanceof AbstractResolver ? ((AbstractResolver)resolver).getNamespace() : null;
        
        Collection modules = new ArrayList();
        
        OrganisationEntry[] orgs = resolver.listOrganisations();
        if (orgs == null || orgs.length == 0) {
            // hack for resolvers which are not able to list organisation, we try to see if the asked organisation is not an exact one:
            String org = pattern.getOrganisation();
            if (fromNamespace != null) {
                org = NameSpaceHelper.transform(pattern.getModuleId(), fromNamespace.getFromSystemTransformer()).getOrganisation();
            }
            modules.addAll(Arrays.asList(resolver.listModules(new OrganisationEntry(resolver, org))));                    
        } else {
            Matcher orgMatcher = matcher.getMatcher(pattern.getOrganisation());
            for (int i = 0; i < orgs.length; i++) {
                String org = orgs[i].getOrganisation();
                String systemOrg = org;
                if (fromNamespace != null) {
                    systemOrg = NameSpaceHelper.transformOrganisation(org, fromNamespace.getToSystemTransformer());
                }
                if (orgMatcher.matches(systemOrg)) {
                    modules.addAll(Arrays.asList(resolver.listModules(new OrganisationEntry(resolver, org))));                    
                }
            }
        }                
        Message.debug("found " + modules.size() + " modules for "+pattern.getOrganisation()+" on " + resolverName);
        boolean foundModule = false;
        for (Iterator iter = modules.iterator(); iter.hasNext();) {
            ModuleEntry mEntry = (ModuleEntry)iter.next();
            
            ModuleId foundMid = new ModuleId(mEntry.getOrganisation(), mEntry.getModule());
            ModuleId systemMid = foundMid;
            if (fromNamespace != null) {
                systemMid = NameSpaceHelper.transform(foundMid, fromNamespace.getToSystemTransformer());
            }
            
            if (MatcherHelper.matches(matcher, pattern.getModuleId(), systemMid)) {
                // The module corresponds to the searched module pattern
                foundModule = true;
                RevisionEntry[] rEntries = resolver.listRevisions(mEntry);
                Message.debug("found " + rEntries.length + " revisions for [" + mEntry.getOrganisation() + ", "+ mEntry.getModule() + "] on " + resolverName);

                boolean foundRevision = false;
                for (int j = 0; j < rEntries.length; j++) {
                    RevisionEntry rEntry = rEntries[j];
                    
                    ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mEntry.getOrganisation(), mEntry.getModule(), rEntry.getRevision());
                    ModuleRevisionId systemMrid = foundMrid;
                    if (fromNamespace != null) {
                        systemMrid = fromNamespace.getToSystemTransformer().transform(foundMrid);
                    }
                    
                    if (MatcherHelper.matches(matcher, pattern, systemMrid)) {
                        // We have a matching module revision
                        foundRevision = true;
                        mrids.add(systemMrid);
                    }
                }
                if (!foundRevision) {
                    Message.debug("no revision found matching "+pattern+" in [" + mEntry.getOrganisation() + "," + mEntry.getModule()+ "] using " + resolverName);                            
                }
            }
        }
        if (!foundModule) {
            Message.debug("no module found matching "+pattern+" using " + resolverName);                            
        }
        return mrids;
    }

    /////////////////////////////////////////////////////////////////////////
    //                         RETRIEVE
    /////////////////////////////////////////////////////////////////////////

    
    /**
     * example of destFilePattern :
     * - lib/[organisation]/[module]/[artifact]-[revision].[type]
     * - lib/[artifact].[type] : flatten with no revision
     * moduleId is used with confs and localCacheDirectory to determine
     * an ivy report file, used as input for the copy
     * If such a file does not exist for any conf (resolve has not been called before ?)
     * then an IllegalStateException is thrown and nothing is copied.
     */
    public int retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern) {
        return retrieve(moduleId, confs, cache, destFilePattern, null);
    }
    /**
     * If destIvyPattern is null no ivy files will be copied.
     */
    public int retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern) {
    	return retrieve(moduleId, confs, cache, destFilePattern, destIvyPattern, FilterHelper.NO_FILTER);
    }
    
    public int retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter) {
    	return retrieve(moduleId, confs, cache, destFilePattern, destIvyPattern, artifactFilter, false, false);
    }
    public int retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter, boolean sync, boolean useOrigin) {
    	if (artifactFilter == null) {
    		artifactFilter = FilterHelper.NO_FILTER;
    	}
    	
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        Message.info(":: retrieving :: "+moduleId+(sync?" [sync]":""));
        Message.info("\tconfs: "+Arrays.asList(confs));
        long start = System.currentTimeMillis();
        
        destFilePattern = IvyPatternHelper.substituteVariables(destFilePattern, getVariables());
        destIvyPattern = IvyPatternHelper.substituteVariables(destIvyPattern, getVariables());
        try {
            Map artifactsToCopy = determineArtifactsToCopy(moduleId, confs, cache, destFilePattern, destIvyPattern, artifactFilter);
            File fileRetrieveRoot = new File(IvyPatternHelper.getTokenRoot(destFilePattern));
            File ivyRetrieveRoot = destIvyPattern == null ? null : new File(IvyPatternHelper.getTokenRoot(destIvyPattern));
            Collection targetArtifactsStructure = new HashSet(); // Set(File) set of all paths which should be present at then end of retrieve (useful for sync) 
            Collection targetIvysStructure = new HashSet(); // same for ivy files
            
            // do retrieve
            int targetsCopied = 0;
            int targetsUpToDate = 0;
            for (Iterator iter = artifactsToCopy.keySet().iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                File archive;
				if ("ivy".equals(artifact.getType())) {
					archive = getIvyFileInCache(cache, artifact.getModuleRevisionId());
				} else {
					archive = getArchiveFileInCache(cache, artifact, getSavedArtifactOrigin(cache, artifact), useOrigin);
					if (!useOrigin && !archive.exists()) {
						// file is not available in cache, maybe the last resolve was performed with useOrigin=true.
						// we try to use the best we can
						archive = getArchiveFileInCache(cache, artifact, getSavedArtifactOrigin(cache, artifact));
					}
				}
                Set dest = (Set)artifactsToCopy.get(artifact);
                Message.verbose("\tretrieving "+archive);
                for (Iterator it2 = dest.iterator(); it2.hasNext();) {
                	checkInterrupted();
                    File destFile = new File((String)it2.next());
                    if (!_checkUpToDate || !upToDate(archive, destFile)) {
                        Message.verbose("\t\tto "+destFile);
                        FileUtil.copy(archive, destFile, null);
                        targetsCopied++;
                    } else {
                        Message.verbose("\t\tto "+destFile+" [NOT REQUIRED]");
                        targetsUpToDate++;
                    }
                    if ("ivy".equals(artifact.getType())) {
                    	targetIvysStructure.addAll(FileUtil.getPathFiles(ivyRetrieveRoot, destFile));
                    } else {
                    	targetArtifactsStructure.addAll(FileUtil.getPathFiles(fileRetrieveRoot, destFile));
                    }
                }
            }
            
            if (sync) {
				Message.verbose("\tsyncing...");
                Collection existingArtifacts = FileUtil.listAll(fileRetrieveRoot);
                Collection existingIvys = ivyRetrieveRoot == null ? null : FileUtil.listAll(ivyRetrieveRoot);

                if (fileRetrieveRoot.equals(ivyRetrieveRoot)) {
                	Collection target = targetArtifactsStructure;
                	target.addAll(targetIvysStructure);
                	Collection existing = existingArtifacts;
                	existing.addAll(existingIvys);
                	sync(target, existing);
                } else {
                	sync(targetArtifactsStructure, existingArtifacts);
                	if (existingIvys != null) {
                		sync(targetIvysStructure, existingIvys);
                	}
                }
            }
            Message.info("\t"+targetsCopied+" artifacts copied, "+targetsUpToDate+" already retrieved");
            Message.verbose("\tretrieve done ("+(System.currentTimeMillis()-start)+"ms)");
            
            return targetsCopied;
        } catch (Exception ex) {
            throw new RuntimeException("problem during retrieve of "+moduleId+": "+ex, ex);
        }
    }

    private void sync(Collection target, Collection existing) {
		Collection toRemove = new HashSet();
		for (Iterator iter = existing.iterator(); iter.hasNext();) {
			File file = (File) iter.next();
			toRemove.add(file.getAbsoluteFile());
		}
		for (Iterator iter = target.iterator(); iter.hasNext();) {
			File file = (File) iter.next();
			toRemove.remove(file.getAbsoluteFile());
		}
		for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
			File file = (File) iter.next();
			if (file.exists()) {
				Message.verbose("\t\tdeleting "+file);
				FileUtil.forceDelete(file);
			}
		}
	}

	public Map determineArtifactsToCopy(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern) throws ParseException, IOException {
    	return determineArtifactsToCopy(moduleId, confs, cache, destFilePattern, destIvyPattern, FilterHelper.NO_FILTER);
    }
    
    public Map determineArtifactsToCopy(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter) throws ParseException, IOException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        
        if (artifactFilter == null) {
        	artifactFilter = FilterHelper.NO_FILTER;
        }
        
        // find what we must retrieve where
        final Map artifactsToCopy = new HashMap(); // Artifact source -> Set (String copyDestAbsolutePath)
        final Map conflictsMap = new HashMap(); // String copyDestAbsolutePath -> Set (Artifact source)
        final Map conflictsConfMap = new HashMap(); // String copyDestAbsolutePath -> Set (String conf)
        XmlReportParser parser = new XmlReportParser();
        for (int i = 0; i < confs.length; i++) {
            final String conf = confs[i];
            Collection artifacts = new ArrayList(Arrays.asList(parser.getArtifacts(moduleId, conf, cache)));
            if (destIvyPattern != null) {
                ModuleRevisionId[] mrids = parser.getRealDependencyRevisionIds(moduleId, conf, cache);
                for (int j = 0; j < mrids.length; j++) {
                    artifacts.add(DefaultArtifact.newIvyArtifact(mrids[j], null));
                }
            }
            for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                String destPattern = "ivy".equals(artifact.getType()) ? destIvyPattern: destFilePattern;
                
                if (!"ivy".equals(artifact.getType()) && !artifactFilter.accept(artifact)) {
                	continue;	// skip this artifact, the filter didn't accept it!
                }
                
                String destFileName = IvyPatternHelper.substitute(destPattern, artifact, conf);
                
                Set dest = (Set)artifactsToCopy.get(artifact);
                if (dest == null) {
                    dest = new HashSet();
                    artifactsToCopy.put(artifact, dest);
                }
                String copyDest = new File(destFileName).getAbsolutePath();
                dest.add(copyDest);
                
                Set conflicts = (Set)conflictsMap.get(copyDest);
                Set conflictsConf = (Set)conflictsConfMap.get(copyDest);
                if (conflicts == null) {
                    conflicts = new HashSet();
                    conflictsMap.put(copyDest, conflicts);
                }
                if (conflictsConf == null) {
                    conflictsConf = new HashSet();
                    conflictsConfMap.put(copyDest, conflictsConf);
                }
                conflicts.add(artifact);
                conflictsConf.add(conf);
            }
        }
        
        // resolve conflicts if any
        for (Iterator iter = conflictsMap.keySet().iterator(); iter.hasNext();) {
            String copyDest = (String)iter.next();
            Set artifacts = (Set)conflictsMap.get(copyDest);
            Set conflictsConfs = (Set)conflictsConfMap.get(copyDest);
            if (artifacts.size() > 1) {
                List artifactsList = new ArrayList(artifacts);
                // conflicts battle is resolved by a sort using a conflict resolving policy comparator
                // which consider as greater a winning artifact
                Collections.sort(artifactsList, getConflictResolvingPolicy());
                // after the sort, the winning artifact is the greatest one, i.e. the last one
                Message.info("\tconflict on "+copyDest+" in "+conflictsConfs+": "+((Artifact)artifactsList.get(artifactsList.size() -1)).getModuleRevisionId().getRevision()+" won");
                
                // we now iterate over the list beginning with the artifact preceding the winner,
                // and going backward to the least artifact
                for (int i=artifactsList.size() - 2; i >=0; i--) {
                    Artifact looser = (Artifact)artifactsList.get(i);
                    Message.verbose("\t\tremoving conflict looser artifact: "+looser);
                    // for each loser, we remove the pair (loser - copyDest) in the artifactsToCopy map
                    Set dest = (Set)artifactsToCopy.get(looser);
                    dest.remove(copyDest);
                    if (dest.isEmpty()) {
                        artifactsToCopy.remove(looser);
                    }
                }
            }
        }
        return artifactsToCopy;
    }
    
    private boolean upToDate(File source, File target) {
        if (!target.exists()) {
            return false;
        }
        return source.lastModified() <= target.lastModified();
    }

    /**
     * The returned comparator should consider greater the artifact which
     * gains the conflict battle.
     * This is used only during retrieve... prefer resolve conflict manager
     * to resolve conflicts.
     * @return
     */
    private Comparator getConflictResolvingPolicy() {
        return new Comparator() {
            // younger conflict resolving policy
            public int compare(Object o1, Object o2) {
                Artifact a1 = (Artifact)o1;
                Artifact a2 = (Artifact)o2;
                if (a1.getPublicationDate().after(a2.getPublicationDate())) {
                    // a1 is after a2 <=> a1 is younger than a2 <=> a1 wins the conflict battle
                    return +1;
                } else if (a1.getPublicationDate().before(a2.getPublicationDate())) {
                    // a1 is before a2 <=> a2 is younger than a1 <=> a2 wins the conflict battle
                    return -1;
                } else {
                    return 0;
                }
            }
        };
    }

    /////////////////////////////////////////////////////////////////////////
    //                         PUBLISH
    /////////////////////////////////////////////////////////////////////////
    public void deliver(ModuleRevisionId mrid,
            String revision,
            File cache, 
            String destIvyPattern, 
            String status,
            Date pubdate,
            PublishingDependencyRevisionResolver pdrResolver, 
            boolean validate
            ) throws IOException, ParseException {
        deliver(mrid, revision, cache, destIvyPattern, status, pubdate, pdrResolver, validate, true);
    }
    
    /**
     * delivers a resolved ivy file based upon last resolve call status and
     * the given PublishingDependencyRevisionResolver.
     * If resolve report file cannot be found in cache, then it throws 
     * an IllegalStateException (maybe resolve has not been called before ?)
     * Moreover, the given PublishingDependencyRevisionResolver is used for each 
     * dependency to get its published information. This can particularly useful
     * when the publish is made for a delivery, and when we wish to deliver each
     * dependency which is still in integration. The PublishingDependencyRevisionResolver
     * can then do the delivering work for the dependency and return the new (delivered)
     * dependency info (with the delivered revision). Note that 
     * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
     * 
     * @param status the new status, null to keep the old one
     * @throws ParseException
     */
    public void deliver(ModuleRevisionId mrid,
            String revision,
            File cache, 
            String destIvyPattern, 
            String status,
            Date pubdate,
            PublishingDependencyRevisionResolver pdrResolver, 
            boolean validate,
            boolean resolveDynamicRevisions) throws IOException, ParseException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        Message.info(":: delivering :: "+mrid+" :: "+revision+" :: "+status+" :: "+pubdate);
        Message.verbose("\tvalidate = "+validate);
        long start = System.currentTimeMillis();
        destIvyPattern = substitute(destIvyPattern);
        
        // 1) find the resolved module descriptor in cache
        File ivyFile = getResolvedIvyFileInCache(cache, mrid);
        if (!ivyFile.exists()) {
            throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
            ivyFileURL = ivyFile.toURL();
            md = XmlModuleDescriptorParser.getInstance().parseDescriptor(this, ivyFileURL, validate);
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(mrid, revision));
            md.setResolvedPublicationDate(pubdate);
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url obtained for file "+ivyFile);
        } catch (ParseException e) {
            throw new IllegalStateException("bad ivy file in cache for "+mrid+": please clean and resolve again");
        }
        
        // 2) parse resolvedRevisions From properties file
        Map resolvedRevisions = new HashMap(); // Map (ModuleId -> String revision)
        Map dependenciesStatus = new HashMap(); // Map (ModuleId -> String status)
        File ivyProperties = getResolvedIvyPropertiesInCache(cache, mrid);
        if (!ivyProperties.exists()) {
            throw new IllegalStateException("ivy properties not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        Properties props = new Properties();
        props.load(new FileInputStream(ivyProperties));
        
        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String depMridStr = (String)iter.next();
            String[] parts = props.getProperty(depMridStr).split(" ");
            ModuleRevisionId decodedMrid = ModuleRevisionId.decode(depMridStr);
            if (resolveDynamicRevisions) {
                resolvedRevisions.put(decodedMrid, parts[0]);
            }
            dependenciesStatus.put(decodedMrid, parts[1]);
        }
        
        // 3) use pdrResolver to resolve dependencies info
        Map resolvedDependencies = new HashMap(); // Map (ModuleRevisionId -> String revision)
        DependencyDescriptor[] dependencies = md.getDependencies();
        for (int i = 0; i < dependencies.length; i++) {
            String rev = (String)resolvedRevisions.get(dependencies[i].getDependencyRevisionId());
            if (rev == null) {
                rev = dependencies[i].getDependencyRevisionId().getRevision();
            }
            String depStatus = (String)dependenciesStatus.get(dependencies[i].getDependencyRevisionId());
            resolvedDependencies.put(dependencies[i].getDependencyRevisionId(), 
                    pdrResolver.resolve(md, status, 
                            ModuleRevisionId.newInstance(dependencies[i].getDependencyRevisionId(), rev), 
                            depStatus));
        }
        
        // 4) copy the source resolved ivy to the destination specified, 
        //    updating status, revision and dependency revisions obtained by
        //    PublishingDependencyRevisionResolver
        String publishedIvy = IvyPatternHelper.substitute(destIvyPattern, md.getResolvedModuleRevisionId());
        Message.info("\tdelivering ivy file to "+publishedIvy);
        try {
            XmlModuleDescriptorUpdater.update(this, ivyFileURL, 
                    new File(publishedIvy),
                    resolvedDependencies, status, revision, pubdate, null, true);
        } catch (SAXException ex) {
            throw new IllegalStateException("bad ivy file in cache for "+mrid+": please clean and resolve again");
        }
        
        Message.verbose("\tdeliver done ("+(System.currentTimeMillis()-start)+"ms)");
    }

    /**
     * 
     * @param pubrevision 
     * @param resolverName the name of a resolver to use for publication
     * @param srcArtifactPattern a pattern to find artifacts to publish with the given resolver
     * @param srcIvyPattern a pattern to find ivy file to publish, null if ivy file should not be published
     * @return a collection of missing artifacts (those that are not published)
     * @throws ParseException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate) throws IOException {
        return publish(mrid, pubrevision, cache, srcArtifactPattern, resolverName, srcIvyPattern, validate, false);
    }
    /**
     * 
     * @param pubrevision 
     * @param resolverName the name of a resolver to use for publication
     * @param srcArtifactPattern a pattern to find artifacts to publish with the given resolver
     * @param srcIvyPattern a pattern to find ivy file to publish, null if ivy file should not be published
     * @return a collection of missing artifacts (those that are not published)
     * @throws ParseException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate, boolean overwrite) throws IOException {
    	return publish(mrid, pubrevision, cache, srcArtifactPattern, resolverName, srcIvyPattern, null, null, null, validate, overwrite, false, null);
    }
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
    	return publish(mrid, pubrevision, cache, Collections.singleton(srcArtifactPattern), resolverName, srcIvyPattern, status, pubdate, extraArtifacts, validate, overwrite, update, conf);
    }
    /**
     * Publishes a module to the repository.
     * 
     * The publish can update the ivy file to publish if update is set to true. In this case it will use
     * the given pubrevision, pubdate and status. If pudate is null it will default to the current date.
     * If status is null it will default to the current ivy file status (which itself defaults to integration if none is found).
     * If update is false, then if the revision is not the same in the ivy file than the one expected (given as parameter),
     * this method will fail with an  IllegalArgumentException.
     * pubdate and status are not used if update is false.
     * extra artifacts can be used to publish more artifacts than actually declared in the ivy file.
     * This can be useful to publish additional metadata or reports.
     * The extra artifacts array can be null (= no extra artifacts), and if non null only the name, type, ext url 
     * and extra attributes of the artifacts are really used. Other methods can return null safely. 
     * 
     * @param mrid
     * @param pubrevision
     * @param cache
     * @param srcArtifactPattern
     * @param resolverName
     * @param srcIvyPattern
     * @param status
     * @param pubdate
     * @param validate
     * @param overwrite
     * @param update
     * @return
     * @throws IOException
     */
    public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, Collection srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        Message.info(":: publishing :: "+mrid.getModuleId());
        Message.verbose("\tvalidate = "+validate);
        long start = System.currentTimeMillis();
        srcIvyPattern = substitute(srcIvyPattern);
        // 1) find the resolved module descriptor
        ModuleRevisionId pubmrid = ModuleRevisionId.newInstance(mrid, pubrevision);
        File ivyFile;
        if (srcIvyPattern != null) {
        	ivyFile = new File(IvyPatternHelper.substitute(srcIvyPattern, DefaultArtifact.newIvyArtifact(pubmrid, new Date())));
        	if (!ivyFile.exists()) {
        		throw new IllegalArgumentException("ivy file to publish not found for "+mrid+": call deliver before ("+ivyFile+")");
        	}
        } else {
        	ivyFile = getResolvedIvyFileInCache(cache, mrid);
        	if (!ivyFile.exists()) {
        		throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        	}
        }
        
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
        	ivyFileURL = ivyFile.toURL();
        	md = XmlModuleDescriptorParser.getInstance().parseDescriptor(this, ivyFileURL, false);
        	if (srcIvyPattern != null) {
            	if (!pubrevision.equals(md.getModuleRevisionId().getRevision())) {
            		if (update) {
            			File tmp = File.createTempFile("ivy", ".xml");
            			tmp.deleteOnExit();
            			try {
							XmlModuleDescriptorUpdater.update(this, ivyFileURL, tmp, new HashMap(), status==null?md.getStatus():status, pubrevision, pubdate==null?new Date():pubdate, null, true);
							ivyFile = tmp;
							// we parse the new file to get updated module descriptor
							md = XmlModuleDescriptorParser.getInstance().parseDescriptor(this, ivyFile.toURL(), false);
							srcIvyPattern = ivyFile.getAbsolutePath();
						} catch (SAXException e) {
				        	throw new IllegalStateException("bad ivy file for "+mrid+": "+ivyFile+": "+e);
						}
            		} else {
            			throw new IllegalArgumentException("cannot publish "+ivyFile+" as "+pubrevision+": bad revision found in ivy file. Use deliver before.");
            		}
            	}
        	} else {
				md.setResolvedModuleRevisionId(pubmrid);
        	}
        } catch (MalformedURLException e) {
        	throw new RuntimeException("malformed url obtained for file "+ivyFile);
        } catch (ParseException e) {
        	throw new IllegalStateException("bad ivy file for "+mrid+": "+ivyFile+": "+e);
        }
        
        DependencyResolver resolver = getResolver(resolverName);
        if (resolver == null) {
            throw new IllegalArgumentException("unknown resolver "+resolverName);
        }
        
        // collect all declared artifacts of this module
        Collection missing = publish(md, resolver, srcArtifactPattern, srcIvyPattern, extraArtifacts, overwrite, conf);
        Message.verbose("\tpublish done ("+(System.currentTimeMillis()-start)+"ms)");
        return missing;
    }

    private Collection publish(ModuleDescriptor md, DependencyResolver resolver, Collection srcArtifactPattern, String srcIvyPattern, Artifact[] extraArtifacts, boolean overwrite, String conf) throws IOException {
        Collection missing = new ArrayList();
        Set artifactsSet = new HashSet();
		String[] confs;
		if (null == conf || "".equals(conf)) {
			confs = md.getConfigurationsNames();
		} else {
			StringTokenizer st = new StringTokenizer(conf, ",");
			confs = new String[st.countTokens()];
			int counter = 0;
			while (st.hasMoreTokens()) {
				confs[counter] = st.nextToken().trim();
				counter++;
			}
		}

		for (int i = 0; i < confs.length; i++) {
            Artifact[] artifacts = md.getArtifacts(confs[i]);
            for (int j = 0; j < artifacts.length; j++) {
                artifactsSet.add(artifacts[j]);
            }
        }
        if (extraArtifacts != null) {
        	for (int i = 0; i < extraArtifacts.length; i++) {
				artifactsSet.add(new MDArtifact(md, extraArtifacts[i].getName(), extraArtifacts[i].getType(), extraArtifacts[i].getExt(), extraArtifacts[i].getUrl(), extraArtifacts[i].getExtraAttributes()));
			}
        }
        // for each declared published artifact in this descriptor, do:
        for (Iterator iter = artifactsSet.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
            //   1) copy the artifact using src patterns and resolver
            boolean published = false;
            for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext() && !published;) {
				String pattern = (String) iterator.next();
				published = publish(artifact, substitute(pattern), resolver, overwrite);
			}
            if (!published) {
            	Message.info("missing artifact "+artifact+":");
                for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext();) {
    				String pattern = (String) iterator.next();
                	Message.info("\t"+new File(IvyPatternHelper.substitute(pattern, artifact))+" file does not exist");
                }
                missing.add(artifact);
            }
        }
        if (srcIvyPattern != null) {
            Artifact artifact = MDArtifact.newIvyArtifact(md);
            if (!publish(artifact, srcIvyPattern, resolver, overwrite)) {
                Message.info("missing ivy file for "+md.getModuleRevisionId()+": "+new File(IvyPatternHelper.substitute(srcIvyPattern, artifact))+" file does not exist");
                missing.add(artifact);
            }
        }
        return missing;
    }

    private boolean publish(Artifact artifact, String srcArtifactPattern, DependencyResolver resolver, boolean overwrite) throws IOException {
    	checkInterrupted();
        File src = new File(IvyPatternHelper.substitute(srcArtifactPattern, artifact));
        if (src.exists()) {
            resolver.publish(artifact, src, overwrite);
            return true;
        } else {
            return false;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //                         SORT 
    /////////////////////////////////////////////////////////////////////////

    /**
     * Sorts the collection of IvyNode from the less dependent to the more dependent
     */
    public List sortNodes(Collection nodes) {
        IvyContext.getContext().setIvy(this);
        return ModuleDescriptorSorter.sortNodes(getVersionMatcher(), nodes);
    }


    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent.
     * This sort ensures that a ModuleDescriptor is always found in the list before all 
     * ModuleDescriptors depending directly on it.
     * @param moduleDescriptors a Collection of ModuleDescriptor to sort
     * @return a List of sorted ModuleDescriptors
     */
    public List sortModuleDescriptors(Collection moduleDescriptors) {
        IvyContext.getContext().setIvy(this);
        return ModuleDescriptorSorter.sortModuleDescriptors(getVersionMatcher(), moduleDescriptors);   
    }
    
    /////////////////////////////////////////////////////////////////////////
    //                         CACHE
    /////////////////////////////////////////////////////////////////////////
    
    public File getResolvedIvyFileInCache(File cache, ModuleRevisionId mrid) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
        return new File(cache, IvyPatternHelper.substitute(_cacheResolvedIvyPattern, mrid.getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml"));
    }

    public File getResolvedIvyPropertiesInCache(File cache, ModuleRevisionId mrid) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
        return new File(cache, IvyPatternHelper.substitute(_cacheResolvedIvyPropertiesPattern, mrid.getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml"));
    }

    public File getIvyFileInCache(File cache, ModuleRevisionId mrid) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
        return new File(cache, IvyPatternHelper.substitute(_cacheIvyPattern, DefaultArtifact.newIvyArtifact(mrid, null)));
    }

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local
     * and if the resolve has been done with useOrigin = true
     * 
     */
    public File getArchiveFileInCache(File cache, Artifact artifact) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
    	ArtifactOrigin origin = getSavedArtifactOrigin(cache, artifact);
		return getArchiveFileInCache(cache, artifact, origin);
    }
    
    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local
     * and if the resolve has been done with useOrigin = true
     * 
     */
    public File getArchiveFileInCache(File cache, Artifact artifact, ArtifactOrigin origin) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
    	File archive = new File(cache, getArchivePathInCache(artifact, origin));
    	if (!archive.exists() && origin != null && origin.isLocal()) {
    		File original = new File(origin.getLocation());
    		if (original.exists()) {
    			return original;
    		}
    	}
    	return archive;
    }
    /**
     * Returns a File object pointing to where the artifact can be found on the local file system,
     * using or not the original location depending on the availability of origin information provided
     * as parameter and the setting of useOrigin.
     * 
     * If useOrigin is false, this method will always return the file in the cache.
     * 
     */
    public File getArchiveFileInCache(File cache, Artifact artifact, ArtifactOrigin origin, boolean useOrigin) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
    	if (useOrigin && origin != null && origin.isLocal()) {
    		return new File(origin.getLocation());
    	} else {
    		return new File(cache, getArchivePathInCache(artifact, origin));
    	}
    }
    
    /**
     * deprecated: use getArchiveFileInCache(File cache, Artifact artifact) instead
     */
    public File getArchiveFileInCache(File cache, String organisation, String module, String revision, String artifact, String type, String ext) {
        IvyContext.getContext().setIvy(this);
    	IvyContext.getContext().setCache(cache);
        return new File(cache, getArchivePathInCache(organisation, module, revision, artifact, type, ext));
    }
    
    /**
     * @deprecated use getArchivePathInCache(Artifact artifact, ArtifactOrigin origin) instead.
     * @param artifact
     * @return
     */
    public String getArchivePathInCache(Artifact artifact) {
        IvyContext.getContext().setIvy(this);
        return IvyPatternHelper.substitute(_cacheArtifactPattern, artifact);
    }
    
    public String getArchivePathInCache(Artifact artifact, ArtifactOrigin origin) {
        IvyContext.getContext().setIvy(this);
        return IvyPatternHelper.substitute(_cacheArtifactPattern, artifact, origin);
    }
    
    /**
     * @deprecated
     */
    public String getArchivePathInCache(String organisation, String module, String revision, String artifact, String type, String ext) {
        IvyContext.getContext().setIvy(this);
        return getArchivePathInCache(new DefaultArtifact(ModuleRevisionId.newInstance(organisation, module, revision), new Date(), artifact, type, ext));
    }
    
//    public File getOriginFileInCache(File cache, Artifact artifact) {
//        return new File(cache, getOriginPathInCache(artifact));
//    }
//    
//    public String getOriginPathInCache(Artifact artifact) {
//        return getArchivePathInCache(artifact) + ".origin";
//    }

    /**
     * Interrupts the current running operation, no later than
     * interruptTimeout milliseconds after the call
     */
    public void interrupt() {
    	Thread operatingThread = IvyContext.getContext().getOperatingThread();
    	interrupt(operatingThread);
    }

    /**
     * Interrupts the current running operation in the given operating thread, 
     * no later than interruptTimeout milliseconds after the call
     */
	public void interrupt(Thread operatingThread) {
		if (operatingThread != null && operatingThread.isAlive()) {
    		if (operatingThread == Thread.currentThread()) {
    			throw new IllegalStateException("cannot call interrupt from ivy operating thread");
    		}
			Message.verbose("interrupting operating thread...");
    		operatingThread.interrupt();
    		synchronized (this) {
    			_interrupted = true;
    		}
    		try {
				Message.verbose("waiting clean interruption of operating thread");
    			operatingThread.join(_interruptTimeout);
			} catch (InterruptedException e) {
			}
			if (operatingThread.isAlive()) {
				Message.warn("waited clean interruption for too long: stopping operating thread");
				operatingThread.stop();
			}
    		synchronized (this) {
    			_interrupted = false;
    		}
    	}
	}
    
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
    

    /**
     * Returns an empty array when no token values are found.
     *  
     * @param token
     * @param otherTokenValues
     * @return
     */
	public String[] listTokenValues(String token, Map otherTokenValues) {
        List r = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            r.addAll(Arrays.asList(resolver.listTokenValues(token, otherTokenValues)));
        }
        return (String[])r.toArray(new String[r.size()]);
	}
    
    public OrganisationEntry[] listOrganisationEntries() {
        List entries = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listOrganisations()));
        }
        return (OrganisationEntry[])entries.toArray(new OrganisationEntry[entries.size()]);
    }
    public String[] listOrganisations() {
        Collection orgs = new HashSet();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            OrganisationEntry[] entries = resolver.listOrganisations();
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        orgs.add(entries[i].getOrganisation());
                    }
                }
            }
        }
        return (String[])orgs.toArray(new String[orgs.size()]);
    }
    public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
        List entries = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listModules(org)));
        }
        return (ModuleEntry[])entries.toArray(new ModuleEntry[entries.size()]);
    }
    public String[] listModules(String org) {
        List mods = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            ModuleEntry[] entries = resolver.listModules(new OrganisationEntry(resolver, org));
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        mods.add(entries[i].getModule());
                    }
                }
            }
        }
        return (String[])mods.toArray(new String[mods.size()]);
    }
    public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
        List entries = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listRevisions(module)));
        }
        return (RevisionEntry[])entries.toArray(new RevisionEntry[entries.size()]);
    }
    public String[] listRevisions(String org, String module) {
        List revs = new ArrayList();
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            RevisionEntry[] entries = resolver.listRevisions(new ModuleEntry(new OrganisationEntry(resolver, org), module));
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        revs.add(entries[i].getRevision());
                    }
                }
            }
        }
        return (String[])revs.toArray(new String[revs.size()]);
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

    private EventListenerList _listeners = new EventListenerList();

    private boolean _logNotConvertedExclusionRule;

    private Boolean _debugConflictResolution;

    private VersionMatcher _versionMatcher;

    public void addTransferListener(TransferListener listener) {
        _listeners.add(TransferListener.class, listener);
    }

    public void removeTransferListener(TransferListener listener) {
        _listeners.remove(TransferListener.class, listener);
    }

    public boolean hasTransferListener(TransferListener listener) {
        return Arrays.asList(_listeners.getListeners(TransferListener.class)).contains(listener);
    }
    protected void fireTransferEvent(TransferEvent evt) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TransferListener.class) {
                ((TransferListener)listeners[i+1]).transferProgress(evt);
            }
        }
    }
    
    public void addTrigger(Trigger trigger) {
    	addIvyListener(trigger, trigger.getEventFilter());
    }

    public void addConfigured(Trigger trigger) {
    	addTrigger(trigger);
    }

    public void addIvyListener(IvyListener listener) {
        _listeners.add(IvyListener.class, listener);
    }

    public void addIvyListener(IvyListener listener, String eventName) {
        addIvyListener(listener, new IvyEventFilter(eventName, null, null));
    }

    public void addIvyListener(IvyListener listener, Filter filter) {
        _listeners.add(IvyListener.class, new FilteredIvyListener(listener, filter));
    }

    public void removeIvyListener(IvyListener listener) {
        _listeners.remove(IvyListener.class, listener);
        IvyListener[] listeners = (IvyListener[])_listeners.getListeners(IvyListener.class);
        for (int i = 0; i < listeners.length; i++) {
			if (listeners[i] instanceof FilteredIvyListener) {
				if (listener.equals(((FilteredIvyListener)listeners[i]).getIvyListener())) {
					_listeners.remove(IvyListener.class, listeners[i]);
				}
			}
		}
    }

    public boolean hasIvyListener(IvyListener listener) {
        return Arrays.asList(_listeners.getListeners(IvyListener.class)).contains(listener);
    }
    public void fireIvyEvent(IvyEvent evt) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==IvyListener.class) {
                ((IvyListener)listeners[i+1]).progress(evt);
            }
        }
    }

    public void transferProgress(TransferEvent evt) {
        fireTransferEvent(evt);
        fireIvyEvent(evt);
    }

    public boolean isUseRemoteConfig() {
        return _useRemoteConfig;
    }

    public void setUseRemoteConfig(boolean useRemoteConfig) {
        _useRemoteConfig = useRemoteConfig;
    }

    public DependencyResolver getDictatorResolver() {
        return _dictatorResolver;
    }

    public void setDictatorResolver(DependencyResolver dictatorResolver) {
        _dictatorResolver = dictatorResolver;
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

    public static URL getDefaultConfigurationURL() {
        return Ivy.class.getResource("conf/ivyconf.xml");
    }

    /**
     * Saves the information of which resolver was used to resolve a md,
     * so that this info can be retrieve later (even after a jvm restart)
     * by getSavedResolverName(ModuleDescriptor md)
     * @param md the module descriptor resolved
     * @param name resolver name
     */
    public void saveResolver(File cache, ModuleDescriptor md, String name) {
        PropertiesFile cdf = getCachedDataFile(cache, md);
        cdf.setProperty("resolver", name);
        cdf.save();
    }

    /**
     * Saves the information of which resolver was used to resolve a md,
     * so that this info can be retrieve later (even after a jvm restart)
     * by getSavedArtResolverName(ModuleDescriptor md)
     * @param md the module descriptor resolved
     * @param name artifact resolver name
     */
    public void saveArtResolver(File cache, ModuleDescriptor md, String name) {
        PropertiesFile cdf = getCachedDataFile(cache, md);
        cdf.setProperty("artifact.resolver", name);
        cdf.save();
    }
    
    public void saveArtifactOrigin(File cache, Artifact artifact, ArtifactOrigin origin) {
       PropertiesFile cdf = getCachedDataFile(cache, artifact.getModuleRevisionId());
       cdf.setProperty("artifact." + artifact.getName() + "#" + artifact.getExt() + ".is-local", String.valueOf(origin.isLocal()));
       cdf.setProperty("artifact." + artifact.getName() + "#" + artifact.getExt() + ".location", origin.getLocation());
       cdf.save();
    }
    
    public ArtifactOrigin getSavedArtifactOrigin(File cache, Artifact artifact) {
        PropertiesFile cdf = getCachedDataFile(cache, artifact.getModuleRevisionId());
        String location = cdf.getProperty("artifact." + artifact.getName() + "#" + artifact.getExt() + ".location");
        boolean isLocal = Boolean.valueOf(cdf.getProperty("artifact." + artifact.getName() + "#" + artifact.getExt() + ".is-local")).booleanValue();
        
        if (location == null) {
           // origin has not been specified, return null
           return null;
        }
        
        return new ArtifactOrigin(isLocal, location);
    }
    
    public void removeSavedArtifactOrigin(File cache, Artifact artifact) {
        PropertiesFile cdf = getCachedDataFile(cache, artifact.getModuleRevisionId());
        cdf.remove("artifact." + artifact.getName() + "#" + artifact.getExt() + ".location");
        cdf.remove("artifact." + artifact.getName() + "#" + artifact.getExt() + ".is-local");
        cdf.save();
    }
    
    private String getSavedResolverName(File cache, ModuleDescriptor md) {
        PropertiesFile cdf = getCachedDataFile(cache, md);
        return cdf.getProperty("resolver");
    }

    private String getSavedArtResolverName(File cache, ModuleDescriptor md) {
        PropertiesFile cdf = getCachedDataFile(cache, md);
        return cdf.getProperty("artifact.resolver");
    }

    private PropertiesFile getCachedDataFile(File cache, ModuleDescriptor md) {
       return getCachedDataFile(cache, md.getResolvedModuleRevisionId());
    }
    
    private PropertiesFile getCachedDataFile(File cache, ModuleRevisionId mRevId) {
        return new PropertiesFile(new File(cache, IvyPatternHelper.substitute(getCacheDataFilePattern(),mRevId)), "ivy cached data file for "+mRevId);
    }

    public String getCacheDataFilePattern() {
        return _cacheDataFilePattern;
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
    public StatusManager getStatusManager() {
        if (_statusManager == null) {
            _statusManager = StatusManager.newDefaultInstance();
        }
        return _statusManager;
    }
    public void setStatusManager(StatusManager statusManager) {
        _statusManager = statusManager;
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


	public synchronized boolean isInterrupted() {
		return _interrupted;
	}


	/**
	 * List module ids of the module accessible through the current resolvers
	 * matching the given mid criteria according to the given matcher.
	 * 
	 * @param criteria
	 * @param matcher
	 * @return
	 */
	public ModuleId[] listModules(ModuleId criteria, PatternMatcher matcher) {
		List ret = new ArrayList();
		Matcher orgMatcher = matcher.getMatcher(criteria.getOrganisation());
		Matcher modMatcher = matcher.getMatcher(criteria.getName());
		Map tokenValues = new HashMap();
		String[] orgs = listTokenValues(IvyPatternHelper.ORGANISATION_KEY, tokenValues);
		for (int i = 0; i < orgs.length; i++) {
			if (orgMatcher.matches(orgs[i])) {
				tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, orgs[i]);
				String[] mods = listTokenValues(IvyPatternHelper.MODULE_KEY, tokenValues);
				for (int j = 0; j < mods.length; j++) {
					if (modMatcher.matches(mods[j])) {
						ret.add(new ModuleId(orgs[i], mods[j]));
					}
				}
			}
		}
		return (ModuleId[]) ret.toArray(new ModuleId[ret.size()]);
	}
	
	
	/**
	 * List module revision ids of the module accessible through the current resolvers
	 * matching the given mrid criteria according to the given matcher.
	 * 
	 * @param criteria
	 * @param matcher
	 * @return
	 */
	public ModuleRevisionId[] listModules(ModuleRevisionId criteria, PatternMatcher matcher) {
		List ret = new ArrayList();
		Matcher orgMatcher = matcher.getMatcher(criteria.getOrganisation());
		Matcher modMatcher = matcher.getMatcher(criteria.getName());
		Matcher branchMatcher = matcher.getMatcher(criteria.getBranch());
		Matcher revMatcher = matcher.getMatcher(criteria.getRevision());
		Map tokenValues = new HashMap();
		String[] orgs = listTokenValues(IvyPatternHelper.ORGANISATION_KEY, tokenValues);
		for (int i = 0; i < orgs.length; i++) {
			if (orgMatcher.matches(orgs[i])) {
				tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, orgs[i]);
				String[] mods = listTokenValues(IvyPatternHelper.MODULE_KEY, tokenValues);
				for (int j = 0; j < mods.length; j++) {
					if (modMatcher.matches(mods[j])) {
						tokenValues.put(IvyPatternHelper.MODULE_KEY, mods[j]);
						String[] branches = listTokenValues(IvyPatternHelper.BRANCH_KEY, tokenValues);
						if (branches == null || branches.length == 0) {
							branches = new String[]  {getDefaultBranch(new ModuleId(orgs[i], mods[j]))};
						}
						for (int k = 0; k < branches.length; k++) {
							if (branchMatcher.matches(branches[k])) {
								tokenValues.put(IvyPatternHelper.BRANCH_KEY, tokenValues);
								String[] revs = listTokenValues(IvyPatternHelper.REVISION_KEY, tokenValues);
								for (int l = 0; l < revs.length; l++) {
									if (revMatcher.matches(revs[l])) {
										ret.add(ModuleRevisionId.newInstance(orgs[i], mods[j], branches[k], revs[l]));
									}
								}
							}
						}
					}
				}
			}
		}
		return (ModuleRevisionId[]) ret.toArray(new ModuleRevisionId[ret.size()]);
	}

}
