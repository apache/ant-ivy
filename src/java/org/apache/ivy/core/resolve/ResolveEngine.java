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
package org.apache.ivy.core.resolve;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.event.resolve.StartResolveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode.EvictionData;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.CacheResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

public class ResolveEngine {
	private IvySettings _settings;
	private EventManager _eventManager;
	private SortEngine _sortEngine;
	
	
    private Set _fetchedSet = new HashSet();
	private DependencyResolver _dictatorResolver;
	
	
    public ResolveEngine(IvySettings settings, EventManager eventManager, SortEngine sortEngine) {
		_settings = settings;
		_eventManager = eventManager;
		_sortEngine = sortEngine;
	}

	public DependencyResolver getDictatorResolver() {
        return _dictatorResolver;
    }

    public void setDictatorResolver(DependencyResolver dictatorResolver) {
        _dictatorResolver = dictatorResolver;
    }

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
        URLResource res = new URLResource(ivySource);
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
        Message.verbose("using "+parser+" to parse "+ivySource);
        ModuleDescriptor md = parser.parseDescriptor(_settings, ivySource, validate);
        if (revision == null && md.getResolvedModuleRevisionId().getRevision() == null) {
            revision = "working@"+HostUtil.getLocalHostName();
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
        DependencyResolver oldDictator = getDictatorResolver();
        if (useCacheOnly) {
        	setDictatorResolver(new CacheResolver(_settings));
        }
        try {
            if (cache==null) {  // ensure that a cache exists
                cache = _settings.getDefaultCache();
                IvyContext.getContext().setCache(cache);
            }
            CacheManager cacheManager = getCacheManager(cache);
            if (artifactFilter == null) {
            	artifactFilter = FilterHelper.NO_FILTER;
            }
            if (confs.length == 1 && confs[0].equals("*")) {
                confs = md.getConfigurationsNames();
            }
            _eventManager.fireIvyEvent(new StartResolveEvent(md, confs));
            
            long start = System.currentTimeMillis();
            Message.info(":: resolving dependencies :: "+md.getResolvedModuleRevisionId()+(transitive?"":" [not transitive]"));
            Message.info("\tconfs: "+Arrays.asList(confs));
            Message.verbose("\tvalidate = "+validate);
            ResolveReport report = new ResolveReport(md);

            // resolve dependencies
            IvyNode[] dependencies = getDependencies(md, confs, cache, date, report, validate, transitive);
            report.setDependencies(Arrays.asList(dependencies), artifactFilter);

            
            // produce resolved ivy file and ivy properties in cache
            File ivyFileInCache = cacheManager.getResolvedIvyFileInCache(md.getResolvedModuleRevisionId());
            md.toIvyFile(ivyFileInCache);

            // we store the resolved dependencies revisions and statuses per asked dependency revision id,
            // for direct dependencies only.
            // this is used by the deliver task to resolve dynamic revisions to static ones
            File ivyPropertiesInCache = cacheManager.getResolvedIvyPropertiesInCache(md.getResolvedModuleRevisionId());
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
	
	            downloadArtifacts(report, cacheManager, useOrigin, artifactFilter);
            }
            
            
            if (outputReport) {
            	outputReport(report, cache);
            }
            
            _eventManager.fireIvyEvent(new EndResolveEvent(md, confs, report));
            return report;
        } finally {
            setDictatorResolver(oldDictator);
        }
	}

	private CacheManager getCacheManager(File cache) {
		//TODO : reuse instance
		CacheManager cacheManager = new CacheManager(_settings, cache);
		return cacheManager;
	}

	public void outputReport(ResolveReport report, File cache) {
		Message.info(":: resolution report ::");
		report.setProblemMessages(Message.getProblems());
		// output report
		report.output(_settings.getReportOutputters(), cache);
		
		Message.verbose("\tresolve done ("+report.getResolveTime()+"ms resolve - "+report.getDownloadTime()+"ms download)");
		Message.sumupProblems();
	}

    public void downloadArtifacts(ResolveReport report, CacheManager cacheManager, boolean useOrigin, Filter artifactFilter) {
    	long start = System.currentTimeMillis();
    	IvyNode[] dependencies = (IvyNode[]) report.getDependencies().toArray(new IvyNode[report.getDependencies().size()]);
        
        _eventManager.fireIvyEvent(new PrepareDownloadEvent((Artifact[])report.getArtifacts().toArray(new Artifact[report.getArtifacts().size()])));
        
        for (int i = 0; i < dependencies.length; i++) {
        	checkInterrupted();
            //download artifacts required in all asked configurations
            if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
                DependencyResolver resolver = dependencies[i].getModuleRevision().getArtifactResolver();
                Artifact[] selectedArtifacts = dependencies[i].getSelectedArtifacts(artifactFilter);
                DownloadReport dReport = resolver.download(selectedArtifacts, new DownloadOptions(_settings, cacheManager, _eventManager, useOrigin));
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

	private void checkInterrupted() {
		IvyContext.getContext().getIvy().checkInterrupted();
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
    public ArtifactDownloadReport download(Artifact artifact, File cache, boolean useOrigin) {
        if (cache == null) {
            cache = _settings.getDefaultCache();
        }
        DependencyResolver resolver = _settings.getResolver(artifact.getModuleRevisionId().getModuleId());
        CacheManager cacheManager = getCacheManager(cache);
        DownloadReport r = resolver.download(new Artifact[] {artifact}, new DownloadOptions(_settings, cacheManager, _eventManager, useOrigin));
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
        return getDependencies(ModuleDescriptorParserRegistry.getInstance().parseDescriptor(_settings, ivySource, validate), confs, cache, date, null, validate);
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
        if (md == null) {
            throw new NullPointerException("module descriptor must not be null");
        }
        if (cache==null) {  // ensure that a cache exists
            cache = _settings.getDefaultCache();
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
        List sortedDependencies = _sortEngine.sortNodes(dependencies);
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
                    if (_settings.debugConflictResolution()) {
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
                                if (_settings.debugConflictResolution()) {
                                    Message.debug("caller "+callerNode.getId()+" of "+node.getId()+" is evicted");
                                }
                            }
                        }
                    }
                    if (allEvicted) {
                        Message.verbose("all callers are evicted for "+node+": evicting too");
                        node.markEvicted(confs[i], null, null, null);
                    } else {
                        if (_settings.debugConflictResolution()) {
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
        if (_settings.debugConflictResolution()) {
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
        if (_settings.debugConflictResolution()) {
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
        if (_settings.debugConflictResolution()) {
            Message.debug("found conflicting revisions for "+node+" in "+parent+": "+conflicts);
        }
        
        Collection resolved = parent.getConflictManager(node.getModuleId()).resolveConflicts(parent, conflicts);
        if (_settings.debugConflictResolution()) {
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
                
                if (_settings.debugConflictResolution()) {
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
                if (_settings.debugConflictResolution()) {
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
            if (_settings.debugConflictResolution()) {
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
            if (_settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for "+node+" in "+parent);
            }
            EvictionData evictionData = node.getEvictionDataInRoot(node.getRootModuleConf(), parent);
            if (evictionData != null) {
                // node has been previously evicted in an ancestor: we mark it as evicted
                if (_settings.debugConflictResolution()) {
                    Message.debug(node+" was previously evicted in root module conf "+node.getRootModuleConf());
                }

                node.markEvicted(evictionData);                
                if (_settings.debugConflictResolution()) {
                    Message.debug("evicting "+node+" by "+evictionData);
                }
            }
            return true;
        } else if (parent.getEvictedRevisions(node.getModuleId(), node.getRootModuleConf()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            // => job already done, we just have to check if the node wasn't previously selected in root ancestor
            if (_settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for "+node+" in "+parent);
            }
            return true;
        }
        return false;
    }

	public ResolvedModuleRevision findModule(ModuleRevisionId id) {
		DependencyResolver r = _settings.getResolver(id.getModuleId());
		if (r == null) {
			throw new IllegalStateException("no resolver found for "+id.getModuleId());
		}
        DefaultModuleDescriptor md = DefaultModuleDescriptor.newCallerInstance(id, new String[] {"*"}, false, false);
		try {
			return r.getDependency(new DefaultDependencyDescriptor(id, true), new ResolveData(this, _settings.getDefaultCache(), null, new ConfigurationResolveReport(this, md, "default", null, _settings.getDefaultCache()), false));
		} catch (ParseException e) {
			throw new RuntimeException("problem whle parsing repository module descriptor for "+id+": "+e, e);
		}
	}

	public EventManager getEventManager() {
		return _eventManager;
	}

	public IvySettings getSettings() {
		return _settings;
	}

	public SortEngine getSortEngine() {
		return _sortEngine;
	}

    
}
