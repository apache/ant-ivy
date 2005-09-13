/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;

import org.xml.sax.SAXException;

import fr.jayasoft.ivy.conflict.LatestConflictManager;
import fr.jayasoft.ivy.conflict.NoConflictManager;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.latest.LatestLexicographicStrategy;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import fr.jayasoft.ivy.latest.LatestTimeStrategy;
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
import fr.jayasoft.ivy.resolver.CacheResolver;
import fr.jayasoft.ivy.resolver.ChainResolver;
import fr.jayasoft.ivy.resolver.DualResolver;
import fr.jayasoft.ivy.resolver.ModuleEntry;
import fr.jayasoft.ivy.resolver.OrganisationEntry;
import fr.jayasoft.ivy.resolver.RevisionEntry;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlIvyConfigurationParser;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorUpdater;
import fr.jayasoft.ivy.xml.XmlReportParser;

/**
 * Ivy is a free java based dependency manager.
 * 
 * This class is the main class of Ivy, which offers mainly dependency resolution.
 * 
 * Here is one typical usage:
 * Ivy ivy = new Ivy();
 * ivy.configure(new URL("ivyconf.xml"));
 * ivy.resolve(new URL("ivy.xml"), null, new String[] {"*"}, null, null, true);
 *  
 * @author x.hanin
 *
 */
public class Ivy implements TransferListener {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private static final String DEFAULT_CACHE_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision].[ext]";
    private static final String DEFAULT_CACHE_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";
    
    private Map _typeDefs = new HashMap();
    private Map _resolversMap = new HashMap();
    private DependencyResolver _defaultResolver;
    private DependencyResolver _dictatorResolver = null;
    
    private String _defaultResolverName;
	private File _defaultCache;
    private boolean _checkUpToDate = true;
	private Map _moduleConfigurations = new LinkedHashMap(); // Map (ModuleId -> String resolverName)
    
    private Map _conflictsManager = new HashMap(); // Map (String conflictManagerName -> ConflictManager)
    private Map _latestStrategies = new HashMap(); // Map (String latestStrategyName -> LatestStrategy)
	
    private Map _variables = new HashMap();

    private ReportOutputter[] _reportOutputters = new ReportOutputter[] {new LogReportOutputter(), new XmlReportOutputter()};
    
    private String _cacheIvyPattern = DEFAULT_CACHE_IVY_PATTERN;
    private String _cacheArtifactPattern = DEFAULT_CACHE_ARTIFACT_PATTERN;

    private boolean _validate = true;

    private LatestStrategy _defaultLatestStrategy = null;
    private ConflictManager _defaultConflictManager = null;
    
    private List _listingIgnore = new ArrayList();

    private boolean _repositoriesConfigured;

    private boolean _useRemoteConfig = false;
    
    public Ivy() {
    	String ivyTypeDefs = System.getProperty("ivy.typedef.files");
    	if (ivyTypeDefs != null) {
    		String[] files = ivyTypeDefs.split("\\,");
    		for (int i = 0; i < files.length; i++) {
    			try {
					typeDefs(new FileInputStream(new File(files[i].trim())));
				} catch (FileNotFoundException e) {
					Message.warn("typedefs file not found: "+files[i].trim());
				} catch (IOException e) {
					Message.warn("problem with typedef file: "+files[i].trim()+": "+e.getMessage());
				}
			}
    	} else {
			try {
				typeDefs(Ivy.class.getResourceAsStream("typedef.properties"));
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
        
        _listingIgnore.add(".cvsignore");
        _listingIgnore.add("CVS");
        _listingIgnore.add(".svn");
        
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
        try {
            Properties p = new Properties();
            p.load(stream);
            typeDefs(p);
        } finally {
            stream.close();
        }
	}

    public void typeDefs(Properties p) {
        for (Iterator iter = p.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			try {
			    typeDef(name, Class.forName(p.getProperty(name)));
			} catch (ClassNotFoundException e) {
				Message.warn("impossible to define resolver "+name+": class not found: "+p.getProperty(name));
			}
		}
    }
    
    
    /////////////////////////////////////////////////////////////////////////
    //                         CONFIGURATION
    /////////////////////////////////////////////////////////////////////////
    public void configure(File configurationFile) throws ParseException, IOException {
        Message.info(":: configuring :: file = "+configurationFile);
        long start = System.currentTimeMillis();
        setVariable("ivy.conf.dir", new File(configurationFile.getAbsolutePath()).getParent());
        setVariable("ivy.conf.file", configurationFile.getAbsolutePath());
        setVariable("ivy.conf.url", configurationFile.toURL().toExternalForm());
        
        try {
            new XmlIvyConfigurationParser(this).parse(configurationFile.toURL());
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException("given file cannot be transformed to url: "+configurationFile);
            iae.initCause(e);
            throw iae;
        }
        Message.verbose("configuration done ("+(System.currentTimeMillis()-start)+"ms)");
        dumpConfig();
    }
    
    public void configure(URL configurationURL) throws ParseException, IOException {
        Message.info(":: configuring :: url = "+configurationURL);
        long start = System.currentTimeMillis();
        String confURL = configurationURL.toExternalForm();
        setVariable("ivy.conf.url", confURL);
        int slashIndex = confURL.lastIndexOf('/');
        if (slashIndex != -1) {
            setVariable("ivy.conf.dir", confURL.substring(0, slashIndex));
        } else {
            Message.warn("configuration url does not contain any slash (/): ivy.conf.dir variable not set");
        }
        
        new XmlIvyConfigurationParser(this).parse(configurationURL);
        Message.verbose("configuration done ("+(System.currentTimeMillis()-start)+"ms)");
        dumpConfig();
    }
    
    private void dumpConfig() {
        Message.verbose("\tdefault cache: "+getDefaultCache());
        Message.verbose("\tdefault resolver: "+getDefaultResolver());
        Message.debug("\tdefault latest strategy: "+getDefaultLatestStrategy());
        Message.debug("\tdefault conflict manager: "+getDefaultConflictManager());
        Message.debug("\tvalidate: "+doValidate());
        Message.debug("\tcheck up2date: "+isCheckUpToDate());
        Message.debug("\tcache ivy pattern: "+getCacheIvyPattern());
        Message.debug("\tcache artifact pattern: "+getCacheArtifactPattern());
        
        Message.verbose("\t-- "+_resolversMap.size()+" resolvers:");
        for (Iterator iter = _resolversMap.values().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            resolver.dumpConfig();
        }
        if (!_moduleConfigurations.isEmpty()) {
            Message.debug("\tmodule configurations:");
            for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
                ModuleId mid = (ModuleId)iter.next();
                String res = (String)_moduleConfigurations.get(mid);
                Message.debug("\t\t"+mid+" -> "+res);
            }
        }
    }

    public void loadProperties(URL url) throws IOException {
        Properties properties = new Properties();
        properties.load(url.openStream());
        addAllVariables(properties);
    }
    public void loadProperties(File file) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(file));
        addAllVariables(properties);
    }
    
    public void setVariable(String varName, String value) {
        Message.debug("setting '"+varName+"' to '"+value+"'");
        _variables.put(varName, substitute(value));
    }
    
    public void addAllVariables(Map variables) {
        addAllVariables(variables, true);
    }

    public void addAllVariables(Map variables, boolean overwrite) {
        for (Iterator iter = variables.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
            if (overwrite || !_variables.containsKey(key)) {
                String val = (String)variables.get(key);
                setVariable(key, val);
            }
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

    public void typeDef(String name, Class clazz) {
        _typeDefs.put(name, clazz);
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
        if (!_resolversMap.containsKey(resolverName)) {
            throw new IllegalArgumentException("no resolver found called "+resolverName+": check your configuration");
        }
    }

    /**
     * regular expressions as explained in Pattern class may be used in ModuleId
     * organisation and name
     * 
     * @param moduleId
     * @param resolverName
     */
    public void addModuleConfiguration(ModuleId moduleId, String resolverName) {
        checkResolverName(resolverName);
    	_moduleConfigurations.put(moduleId, resolverName);
    }
    
    public File getDefaultCache() {
        if (_defaultCache==null) {
            _defaultCache = new File(System.getProperty("user.home"), ".ivy-cache");
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
        String name = (String)_moduleConfigurations.get(moduleId);
        if (name != null) {
            return name;
        }
        for (Iterator iter = _moduleConfigurations.keySet().iterator(); iter.hasNext();) {
            ModuleId mid = (ModuleId)iter.next();
            if (Pattern.compile(mid.getOrganisation()).matcher(moduleId.getOrganisation()).matches()
                    && Pattern.compile(mid.getName()).matcher(moduleId.getName()).matches()) {
                return (String)_moduleConfigurations.get(mid);
            }
        }
        return _defaultResolverName;
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
            boolean result = true;
            // parse ivy file
            ModuleDescriptor md = XmlModuleDescriptorParser.parseDescriptor(this, ivyFile, doValidate());
            
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
            Message.info("parse problem on "+ivyFile+": "+e.getMessage());
            return false;
        } catch (IOException e) {
            Message.info("io problem on "+ivyFile+": "+e.getMessage());
            return false;
        } catch (Exception e) {
            Message.info("problem on "+ivyFile+": "+e.getMessage());
            return false;
        }
    }
    
    /////////////////////////////////////////////////////////////////////////
    //                         RESOLVE
    /////////////////////////////////////////////////////////////////////////

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
    public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
        DependencyResolver oldDictator = getDictatorResolver();
        if (useCacheOnly) {
            setDictatorResolver(new CacheResolver(this));
        }
        try {
            ModuleDescriptor md = XmlModuleDescriptorParser.parseDescriptor(this, ivySource, validate);
            if (cache==null) {  // ensure that a cache exists
                cache = getDefaultCache();
            }
            if (revision == null && md.getResolvedModuleRevisionId().getRevision() == null) {
                revision = "working@"+getLocalHostName();
            }
            if (revision != null) {
                md.setResolvedModuleRevisionId(new ModuleRevisionId(md.getModuleRevisionId().getModuleId(), revision));
            }
            if (confs.length == 1 && confs[0].equals("*")) {
                confs = md.getConfigurationsNames();
            }
            long start = System.currentTimeMillis();
            Message.info(":: resolving dependencies :: "+md.getResolvedModuleRevisionId());
            Message.info("\tconfs: "+Arrays.asList(confs));
            Message.verbose("\tvalidate = "+validate);
            ResolveReport report = new ResolveReport(md);

            // resolve dependencies
            IvyNode[] dependencies = getDependencies(md, confs, cache, date, report, validate);
            
            Message.verbose(":: downloading artifacts ::");
            Map resolvedRevisions = new HashMap(); // Map (ModuleId dependency -> String revision)
            for (int i = 0; i < dependencies.length; i++) {
                //download artifacts required in all asked configurations
                if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
                    DependencyResolver resolver = dependencies[i].getModuleRevision().getResolver();
                    DownloadReport dReport = resolver
                     .download(dependencies[i].getSelectedArtifacts(artifactFilter), this, cache);
                    ArtifactDownloadReport[] adrs = dReport.getArtifactsReports(DownloadStatus.FAILED);
                    for (int j = 0; j < adrs.length; j++) {
                        Message.warn("\t[NOT FOUND  ] "+adrs[j].getArtifact());
                        resolver.reportFailure(adrs[j].getArtifact());
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
                    
                    // update resolved dependencies map for resolved ivy file producing
                    resolvedRevisions.put(dependencies[i].getModuleId(), dependencies[i].getResolvedId().getRevision());
                } else if (dependencies[i].isCompletelyEvicted()) {
                    // dependencies has been evicted: it has not been added to the report yet
                    String[] dconfs = dependencies[i].getRootModuleConfigurations();
                    for (int j = 0; j < dconfs.length; j++) {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i]);
                    }
                }
            }
            
            // produce resolved ivy conf file in cache
            File ivyFileInCache = getIvyFileInCache(cache, md.getResolvedModuleRevisionId());
            try {
                XmlModuleDescriptorUpdater.update(
                        ivySource, 
                        ivyFileInCache, 
                        resolvedRevisions, 
                        null, 
                        md.getResolvedModuleRevisionId().getRevision(),
                        null);
            } catch (SAXException e) {
                ParseException ex = new ParseException(e.getMessage(), 0);
                ex.initCause(e);
                throw ex;
            }
            Message.verbose("\tresolved ivy file produced in "+ivyFileInCache);
            
            Message.info(":: resolution report ::");
            
            // output report
            report.output(getReportOutputters(), cache);
            
            Message.verbose("\tresolve done ("+(System.currentTimeMillis()-start)+"ms)");
            Message.sumupProblems();
            return report;
        } finally {
            setDictatorResolver(oldDictator);
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
        if (cache == null) {
            cache = getDefaultCache();
        }
        DependencyResolver resolver = getResolver(artifact.getModuleRevisionId().getModuleId());
        DownloadReport r = resolver.download(new Artifact[] {artifact}, this, cache);
        return r.getArtifactReport(artifact);
    }
    
    public ReportOutputter[] getReportOutputters() {
        return _reportOutputters;
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
        return getDependencies(XmlModuleDescriptorParser.parseDescriptor(this, ivySource, validate), confs, cache, date, null, validate);
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
        if (md == null) {
            throw new NullPointerException("module descriptor must not be null");
        }
        if (cache==null) {  // ensure that a cache exists
            cache = getDefaultCache();
        }
        if (confs.length == 1 && confs[0].equals("*")) {
            confs = md.getConfigurationsNames();
        }
        
        Map dependenciesMap = new HashMap();
        Date reportDate = new Date();
        for (int i = 0; i < confs.length; i++) {
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
				
				ResolveData data = new ResolveData(this, cache, date, confReport, validate, dependenciesMap);
                IvyNode node = new IvyNode(data, md, confs[i]);
                node.setRootModuleConf(confs[i]);
                fetchDependencies(node, confs[i], false);
            }
		}
        
        
        // prune and reverse sort fectched dependencies 
        Collection dependencies = new HashSet(dependenciesMap.size()); // use a Set to avoids duplicates
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
                    boolean allEvicted = callers.length > 0;
                    for (int j = 0; j < callers.length; j++) {
                        if (callers[j].getModuleRevisionId().equals(md.getModuleRevisionId())) {
                            // the caller is the root module itself, it can't be evicted
                            allEvicted = false;
                            break;                            
                        } else {
                            IvyNode callerNode = (IvyNode)dependenciesMap.get(callers[j].getModuleRevisionId());                            
                            if (callerNode != null && !callerNode.isEvicted(confs[i])) {
                                allEvicted = false;
                                break;
                            }
                        }
                    }
                    if (allEvicted) {
                        Message.verbose("all callers are evicted for "+node+": evicting too");
                        node.markEvicted(confs[i], null, null, null);
                    }
                }
            }
        }
        
        return (IvyNode[])sortedDependencies.toArray(new IvyNode[sortedDependencies.size()]);
    }


    
    
    private void fetchDependencies(IvyNode node, String conf, boolean shouldBePublic) {
        resolveConflict(node, node.getParent(), Collections.EMPTY_SET);
        if (node.loadData(conf)) {
            node = node.getRealNode(); // if data loading discarded the node, get the real one
            resolveConflict(node, node.getParent(), Collections.EMPTY_SET);
            if (!node.isEvicted(node.getRootModuleConf())) {
                if ("*".equals(conf)) {
                    String[] confs = node.getDescriptor().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        doFetchDependencies(node, confs[i], shouldBePublic);
                    }
                } else {
                    doFetchDependencies(node, conf, shouldBePublic);
                }
            }
        }
        if (node.isEvicted(node.getRootModuleConf())) {
            // update selected nodes with confs asked in evicted one
            IvyNode.EvictionData ed = node.getEvictedData(node.getRootModuleConf());
            for (Iterator iter = ed.getSelected().iterator(); iter.hasNext();) {
                IvyNode selected = (IvyNode)iter.next();
                fetchDependencies(selected, conf, shouldBePublic);
            }
        }
    }

    private void doFetchDependencies(IvyNode node, String conf, boolean shouldBePublic) {
        Configuration c = node.getConfiguration(conf);
        if (c == null) {
            Message.error("configuration not found in "+node+": "+conf+". It was required from "+node.getParent()+" "+node.getParentConf());
        } else if (c.getVisibility() != Configuration.Visibility.PUBLIC && shouldBePublic) {
            Message.error("configuration not public in "+node+": "+c+". It was required from "+node.getParent()+" "+node.getParentConf());
        } else {
            // we handle the case where the asked configuration extends others:
            // we have to first fetch the extended configurations
            String[] extendedConfs = c.getExtends();
            if (extendedConfs.length > 0) {
                node.updateConfsToFetch(Arrays.asList(extendedConfs));
            }
            for (int i = 0; i < extendedConfs.length; i++) {
                fetchDependencies(node, extendedConfs[i], false);
            }

            if (node.getDependencyDescriptor() == null || node.getDependencyDescriptor().isTransitive()) {
                Collection dependencies = node.getDependencies(conf, true);
                for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                    IvyNode dep = (IvyNode)iter.next();
                    if (dep.isCircular()) {
                        Message.warn("circular dependency found ! "+node.getId()+" depends on "+dep.getId()+" which is already on the same branch of dependency");
                        continue;
                    }
                    String[] confs = dep.getRequiredConfigurations(node, conf);
                    for (int i = 0; i < confs.length; i++) {
                        fetchDependencies(dep, confs[i], shouldBePublic);
                    }
                    // if there are still confs to fetch (usually because they have
                    // been updated when evicting another module), we fetch them now
                    confs = dep.getConfsToFetch();
                    for (int i = 0; i < confs.length; i++) {
                        fetchDependencies(dep, confs[i], shouldBePublic);
                    }
                }
            }
        }
    }


    private void resolveConflict(IvyNode node, IvyNode parent, Collection toevict) {
        if (parent == null) {
            return;
        }
        if (node.getId().equals(node.getResolvedId()) && parent.getResolvedRevisions(node.getModuleId(), node.getRootModuleConf()).contains(node.getId())) {
            // resolve conflict has already be done with node with the same id
            // => job already done
            return;
        }
        Collection conflicts = new HashSet();
        if (parent.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()).removeAll(toevict)) {
            // parent.resolved(node.mid) is not up to date:
            // recompute resolved from all sub nodes
            Collection deps = parent.getDependencies(parent.getRequiredConfigurations());
            for (Iterator iter = deps.iterator(); iter.hasNext();) {
                IvyNode dep = (IvyNode)iter.next();
                conflicts.addAll(dep.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()));
            }
        } else {
            conflicts.add(node);
            conflicts.addAll(parent.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()));
        }
        Collection resolved = parent.getConflictManager(node.getModuleId()).resolveConflicts(parent, conflicts);
        if (resolved.contains(node)) {
            // node has been selected for the current parent
            // we update its eviction... but it can still be evicted by parent !
            node.markSelected(node.getRootModuleConf());
            Message.debug("selecting "+node+" in "+parent);
            
            // handle previously selected nodes that are now evicted by this new node
            toevict = parent.getResolvedNodes(node.getModuleId(), node.getRootModuleConf());
            toevict.removeAll(resolved);
            
            for (Iterator iter = toevict.iterator(); iter.hasNext();) {
                IvyNode te = (IvyNode)iter.next();
                te.markEvicted(node.getRootModuleConf(), parent, parent.getConflictManager(node.getModuleId()), resolved);
                
                Message.debug("evicting "+te+" by "+te.getEvictedData(node.getRootModuleConf()));
            }
            
            // it's very important to update resolved BEFORE recompute parent call
            // to allow it to recompute its resolved collection with correct data
            // if necessary
            parent.setResolvedNodes(node.getModuleId(), node.getRootModuleConf(), resolved); 
            resolveConflict(node, parent.getParent(), toevict);
        } else {
            // node has been evicted for the current parent
            
            // it's time to update parent resolved with found resolved...
            // if they have not been recomputed, it does not change anything
            parent.setResolvedNodes(node.getModuleId(), node.getRootModuleConf(), resolved); 
            
            node.markEvicted(node.getRootModuleConf(), parent, parent.getConflictManager(node.getModuleId()), resolved);
            Message.debug("evicting "+node+" by "+node.getEvictedData(node.getRootModuleConf()));
        }
    }

    public ResolvedModuleRevision findModuleInCache(ModuleRevisionId mrid, File cache, boolean validate) {
        // first, check if it is in cache
        if (mrid.isExactRevision()) {
            File ivyFile = getIvyFileInCache(cache, mrid);
            if (ivyFile.exists()) {
                // found in cache !
                try {
                    ModuleDescriptor depMD = XmlModuleDescriptorParser.parseDescriptor(this, ivyFile.toURL(), validate);
                    DependencyResolver resolver = (DependencyResolver)_resolversMap.get(depMD.getResolverName());
                    if (resolver != null) {
                        Message.debug("\tfound ivy file in cache for "+mrid+": "+ivyFile);
                        return new DefaultModuleRevision(resolver, depMD, false, false);
                    } else {
                        Message.debug("\tresolver not found: "+depMD.getResolverName()+" => cannot use cached ivy file for "+mrid);                                    
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
    public void retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern) {
        Message.info(":: retrieving :: "+moduleId);
        Message.info("\tconfs: "+Arrays.asList(confs));
        long start = System.currentTimeMillis();
        
        destFilePattern = IvyPatternHelper.substituteVariables(destFilePattern, getVariables());
        try {
            // find what we must retrieve where
            final Map artifactsToCopy = new HashMap(); // Artifact source -> Set (String copyDestAbsolutePath)
            final Map conflictsMap = new HashMap(); // String copyDestAbsolutePath -> Set (Artifact source)
            final Map conflictsConfMap = new HashMap(); // String copyDestAbsolutePath -> Set (String conf)
            XmlReportParser parser = new XmlReportParser();
            for (int i = 0; i < confs.length; i++) {
                final String conf = confs[i];
                Artifact[] artifacts = parser.getArtifacts(moduleId, conf, cache);
                for (int j = 0; j < artifacts.length; j++) {
                    Artifact artifact = artifacts[j];
                    String destFileName = IvyPatternHelper.substitute(destFilePattern, artifact.getModuleRevisionId().getOrganisation(), artifact.getModuleRevisionId().getName(), artifact.getModuleRevisionId().getRevision(), artifact.getName(), artifact.getType(), artifact.getExt(), conf);
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
            
            // do retrieve
            int targetsCopied = 0;
            int targetsUpToDate = 0;
            for (Iterator iter = artifactsToCopy.keySet().iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                File archive = getArchiveFileInCache(cache, artifact);
                Set dest = (Set)artifactsToCopy.get(artifact);
                Message.verbose("\tretrieving "+archive);
                for (Iterator it2 = dest.iterator(); it2.hasNext();) {
                    File destFile = new File((String)it2.next());
                    if (!_checkUpToDate || !upToDate(archive, destFile)) {
                        Message.verbose("\t\tto "+destFile);
                        FileUtil.copy(archive, destFile, null);
                        targetsCopied++;
                    } else {
                        Message.verbose("\t\tto "+destFile+" [NOT REQUIRED]");
                        targetsUpToDate++;
                    }
                }
            }
            Message.info("\t"+targetsCopied+" artifacts copied, "+targetsUpToDate+" already retrieved");
            Message.verbose("\tretrieve done ("+(System.currentTimeMillis()-start)+"ms)");
        } catch (Exception ex) {
            IllegalStateException ise = new IllegalStateException("problem during retrieve of "+moduleId);
            ise.initCause(ex);
            throw ise;
        }
    }

    private boolean upToDate(File source, File target) {
        if (!target.exists()) {
            return false;
        }
        return source.lastModified() == target.lastModified();
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
            boolean validate) throws IOException, ParseException {
	    Message.info(":: delivering :: "+mrid+" :: "+revision+" :: "+status+" :: "+pubdate);
        Message.verbose("\tvalidate = "+validate);
	    long start = System.currentTimeMillis();
        destIvyPattern = substitute(destIvyPattern);
        
	    // 1) find the resolved module descriptor in cache
	    File ivyFile = getIvyFileInCache(cache, mrid);
	    if (!ivyFile.exists()) {
	        throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
	    }
	    ModuleDescriptor md = null;
	    URL ivyFileURL = null;
	    try {
	        ivyFileURL = ivyFile.toURL();
	        md = XmlModuleDescriptorParser.parseDescriptor(this, ivyFileURL, validate);
	        md.setResolvedModuleRevisionId(new ModuleRevisionId(mrid.getModuleId(), revision));
	        md.setResolvedPublicationDate(pubdate);
	    } catch (MalformedURLException e) {
	        throw new RuntimeException("malformed url obtained for file "+ivyFile);
	    } catch (ParseException e) {
	        throw new IllegalStateException("bad ivy file in cache for "+mrid+": please clean and resolve again");
	    }
	    // 2) use pdrResolver to resolve dependencies info
	    Map resolvedDependencies = new HashMap(); // Map (ModuleId -> String revision)
	    DependencyDescriptor[] dependencies = md.getDependencies();
	    for (int i = 0; i < dependencies.length; i++) {
            DependencyResolver resolver = getResolver(dependencies[i].getDependencyId());
            ResolvedModuleRevision dependency = resolver.getDependency(dependencies[i], new ResolveData(this, cache, pubdate, null, validate));            
            if (dependency == null) {
            	Message.warn(resolver.getName()+": unresolved dependency while publishing: "+dependencies[i].getDependencyRevisionId());
            } else {
            	resolvedDependencies.put(dependencies[i].getDependencyId(), pdrResolver.resolve(md, status, dependency.getDescriptor()));
            }
        }
	    
	    // 3) copy the source resolved ivy to the destination specified, 
	    //    updating status, revision and dependency revisions obtained by
	    //    PublishingDependencyRevisionResolver
	    String publishedIvy = IvyPatternHelper.substitute(destIvyPattern, md.getResolvedModuleRevisionId());
	    Message.info("\tdelivering ivy file to "+publishedIvy);
	    try {
		    XmlModuleDescriptorUpdater.update(ivyFileURL, 
		            new File(publishedIvy),
		            resolvedDependencies, status, revision, pubdate);
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
        Message.info(":: publishing :: "+mrid);
        Message.verbose("\tvalidate = "+validate);
        long start = System.currentTimeMillis();
        srcArtifactPattern = substitute(srcArtifactPattern);
        srcIvyPattern = substitute(srcIvyPattern);
        // 1) find the resolved module descriptor in cache
        File ivyFile = getIvyFileInCache(cache, mrid);
        if (!ivyFile.exists()) {
            throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        DependencyResolver resolver = getResolver(resolverName);
        if (resolver == null) {
            throw new IllegalArgumentException("unknown resolver "+resolverName);
        }
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
            ivyFileURL = ivyFile.toURL();
            md = XmlModuleDescriptorParser.parseDescriptor(this, ivyFileURL, false);
            md.setResolvedModuleRevisionId(new ModuleRevisionId(mrid.getModuleId(), pubrevision));
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url obtained for file "+ivyFile);
        } catch (ParseException e) {
            throw new IllegalStateException("bad ivy file in cache for "+mrid+": please clean cache and resolve again");
        }
        
        // collect all declared artifacts of this module
        Collection missing = new ArrayList();
        Set artifactsSet = new HashSet();
	    String[] confs = md.getConfigurationsNames();
	    for (int i = 0; i < confs.length; i++) {
            Artifact[] artifacts = md.getArtifacts(confs[i]);
            for (int j = 0; j < artifacts.length; j++) {
                artifactsSet.add(artifacts[j]);
            }
        }
        // for each declared published artifact in this descriptor, do:
	    for (Iterator iter = artifactsSet.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact) iter.next();
    	    //   1) copy the artifact using src pattern and resolver
            if (!publish(artifact, srcArtifactPattern, resolver)) {
                missing.add(artifact);
            }
        }
        if (srcIvyPattern != null) {
            Artifact artifact = new MDArtifact(md, "ivy", "ivy", "xml");
            if (!publish(artifact, srcIvyPattern, resolver)) {
                missing.add(artifact);
            }
        }
        Message.verbose("\tpublish done ("+(System.currentTimeMillis()-start)+"ms)");
        return missing;
    }

    private boolean publish(Artifact artifact, String srcArtifactPattern, DependencyResolver resolver) {
        File src = new File(IvyPatternHelper.substitute(srcArtifactPattern, artifact));
        if (src.exists()) {
            try {
                resolver.publish(artifact, src);
                return true;
            } catch (Exception ex) {
                Message.error("impossible to publish "+artifact+" with "+resolver.getName()+": "+ex.getMessage());
            }
        }
        return false;
    }

    /////////////////////////////////////////////////////////////////////////
    //                         SORT 
    /////////////////////////////////////////////////////////////////////////

    public static List sortNodes(Collection nodes) {
        return ModuleDescriptorSorter.sortNodes(nodes);
    }


    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent.
     * This sort ensures that a ModuleDescriptor is always found in the list before all 
     * ModuleDescriptors depending directly on it.
     * @param moduleDescriptors a Collection of ModuleDescriptor to sort
     * @return a List of sorted ModuleDescriptors
     */
    public static List sortModuleDescriptors(Collection moduleDescriptors) {
        return ModuleDescriptorSorter.sortModuleDescriptors(moduleDescriptors);   
    }
    
    /////////////////////////////////////////////////////////////////////////
    //                         CACHE
    /////////////////////////////////////////////////////////////////////////
    
    public File getIvyFileInCache(File cache, ModuleRevisionId mrid) {
        return new File(cache, IvyPatternHelper.substitute(_cacheIvyPattern, mrid.getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml"));
    }

    public File getArchiveFileInCache(File cache, Artifact artifact) {
        return new File(cache, getArchivePathInCache(artifact));
    }
    
    public File getArchiveFileInCache(File cache, String organisation, String module, String revision, String artifact, String type, String ext) {
        return new File(cache, getArchivePathInCache(organisation, module, revision, artifact, type, ext));
    }
    
    public String getArchivePathInCache(Artifact artifact) {
        return getArchivePathInCache( 
                artifact.getModuleRevisionId().getOrganisation(),
                artifact.getModuleRevisionId().getName(),
                artifact.getModuleRevisionId().getRevision(),
                artifact.getName(),
                artifact.getType(),
                artifact.getExt());
    }
    
    public String getArchivePathInCache(String organisation, String module, String revision, String artifact, String type, String ext) {
        return IvyPatternHelper.substitute(_cacheArtifactPattern, organisation, module, revision, artifact, type, ext);
    }
    
    public static String getLocalHostName() {
        try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
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

    public void transferProgress(TransferEvent evt) {
        fireTransferEvent(evt);
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

}
