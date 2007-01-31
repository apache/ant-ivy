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
package org.apache.ivy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.deliver.PublishingDependencyRevisionResolver;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.conflict.NoConflictManager;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;
import org.xml.sax.SAXException;


/**
 * <a href="http://incubator.apache.org/ivy/">Ivy</a> is a free java based dependency manager.
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
public class Ivy {
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

	private IvySettings _settings;

	private boolean _interrupted;

	private EventManager _eventManager;
	private SortEngine _sortEngine;
	private ResolveEngine _resolveEngine;
    
    public Ivy() {
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
            ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(_settings, ivyFile, _settings.doValidate());
            
            // check publications if possible
            if (resolvername != null) {
                DependencyResolver resolver = _settings.getResolver(resolvername);
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
            ResolveData data = new ResolveData(_resolveEngine, _settings.getDefaultCache(), new Date(), null, true);
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
                DependencyResolver resolver = _settings.getResolver(dds[i].getDependencyId());
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
    

	/////////////////////////////////////////////////////////////////////////
    //                         RESOLVE
    /////////////////////////////////////////////////////////////////////////

    


    public ArtifactDownloadReport download(Artifact artifact, File cache, boolean useOrigin) {
		return _resolveEngine.download(artifact, cache, useOrigin);
	}

	public void downloadArtifacts(ResolveReport report, CacheManager cacheManager, boolean useOrigin, Filter artifactFilter) {
		_resolveEngine.downloadArtifacts(report, cacheManager, useOrigin, artifactFilter);
	}

	public ResolvedModuleRevision findModule(ModuleRevisionId id) {
		return _resolveEngine.findModule(id);
	}

	public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate, boolean transitive) {
		return _resolveEngine.getDependencies(md, confs, cache, date, report, validate, transitive);
	}

	public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate) {
		return _resolveEngine.getDependencies(md, confs, cache, date, report, validate);
	}

	public IvyNode[] getDependencies(URL ivySource, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
		return _resolveEngine.getDependencies(ivySource, confs, cache, date, validate);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _resolveEngine.resolve(md, confs, cache, date, validate, useCacheOnly, transitive, useOrigin, download, outputReport, artifactFilter);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _resolveEngine.resolve(md, confs, cache, date, validate, useCacheOnly, transitive, download, outputReport, artifactFilter);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _resolveEngine.resolve(md, confs, cache, date, validate, useCacheOnly, transitive, artifactFilter);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _resolveEngine.resolve(md, confs, cache, date, validate, useCacheOnly, artifactFilter);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
		return _resolveEngine.resolve(mrid, confs, transitive, changing, cache, date, validate, useCacheOnly, useOrigin, artifactFilter);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
		return _resolveEngine.resolve(mrid, confs, transitive, changing, cache, date, validate, useCacheOnly, artifactFilter);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs) throws ParseException, IOException {
		return _resolveEngine.resolve(mrid, confs);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, transitive, useOrigin, artifactFilter);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, transitive, artifactFilter);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly, artifactFilter);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, revision, confs, cache, date, validate, useCacheOnly);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, revision, confs, cache, date, validate);
	}

	public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource);
	}

	public ResolveReport resolve(File ivySource) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource);
	}

    /////////////////////////////////////////////////////////////////////////
    //                         INSTALL
    /////////////////////////////////////////////////////////////////////////
    
	public ResolveReport install(ModuleRevisionId mrid, String from, String to, boolean transitive, boolean validate, boolean overwrite, Filter artifactFilter, File cache, String matcherName) throws IOException {
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        if (cache == null) {
            cache = _settings.getDefaultCache();
        }
        if (artifactFilter == null) {
            artifactFilter = FilterHelper.NO_FILTER;
        }
        DependencyResolver fromResolver = _settings.getResolver(from);
        DependencyResolver toResolver = _settings.getResolver(to);
        if (fromResolver == null) {
            throw new IllegalArgumentException("unknown resolver "+from+". Available resolvers are: "+_settings.getResolverNames());
        }
        if (toResolver == null) {
            throw new IllegalArgumentException("unknown resolver "+to+". Available resolvers are: "+_settings.getResolverNames());
        }
        PatternMatcher matcher = _settings.getMatcher(matcherName);
        if (matcher == null) {
            throw new IllegalArgumentException("unknown matcher "+matcherName+". Available matchers are: "+_settings.getMatcherNames());
        }
        
        // build module file declaring the dependency
        Message.info(":: installing "+mrid+" ::");
        DependencyResolver oldDicator = getDictatorResolver();
        boolean log = _settings.logNotConvertedExclusionRule();
        try {
        	_settings.setLogNotConvertedExclusionRule(true);
            setDictatorResolver(fromResolver);
            
            DefaultModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("apache", "ivy-install", "1.0"), _settings.getStatusManager().getDefaultStatus(), new Date());
            md.addConfiguration(new Configuration("default"));
            md.addConflictManager(new ModuleId(ExactPatternMatcher.ANY_EXPRESSION, ExactPatternMatcher.ANY_EXPRESSION), ExactPatternMatcher.INSTANCE, new NoConflictManager());
            
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
            downloadArtifacts(report, getCacheManager(cache), false, artifactFilter);

            // now that everything is in cache, we can publish all these modules
            Message.info(":: installing in "+to+" ::");
            for (int i = 0; i < dependencies.length; i++) {
                ModuleDescriptor depmd = dependencies[i].getDescriptor();
                if (depmd != null) {
                    Message.verbose("installing "+depmd.getModuleRevisionId());
                    publish(depmd, 
                            toResolver, 
                            Collections.singleton(cache.getAbsolutePath()+"/"+_settings.getCacheArtifactPattern()), 
                            cache.getAbsolutePath()+"/"+_settings.getCacheIvyPattern(), 
                            null,
                            overwrite,
                            null);
                }
            }

            Message.info(":: install resolution report ::");
            
            // output report
            report.output(_settings.getReportOutputters(), cache);

            return report;
        } finally {
            setDictatorResolver(oldDicator);
            _settings.setLogNotConvertedExclusionRule(log);
        }
    }
    
	private DependencyResolver _dictatorResolver;
    public DependencyResolver getDictatorResolver() {
        return _dictatorResolver;
    }

    public void setDictatorResolver(DependencyResolver dictatorResolver) {
        _dictatorResolver = dictatorResolver;
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
    	return retrieve(moduleId, confs, cache, destFilePattern, destIvyPattern, artifactFilter, sync, useOrigin, false);
    }
    public int retrieve(ModuleId moduleId, String[] confs, final File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter, boolean sync, boolean useOrigin, boolean makeSymlinks) {
    	if (artifactFilter == null) {
    		artifactFilter = FilterHelper.NO_FILTER;
    	}
    	
        IvyContext.getContext().setIvy(this);
        IvyContext.getContext().setCache(cache);
        Message.info(":: retrieving :: "+moduleId+(sync?" [sync]":""));
        Message.info("\tconfs: "+Arrays.asList(confs));
        long start = System.currentTimeMillis();
        
        destFilePattern = IvyPatternHelper.substituteVariables(destFilePattern, _settings.getVariables());
        destIvyPattern = IvyPatternHelper.substituteVariables(destIvyPattern, _settings.getVariables());
        CacheManager cacheManager = getCacheManager(cache);
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
					archive = cacheManager.getIvyFileInCache(artifact.getModuleRevisionId());
				} else {
					archive = cacheManager.getArchiveFileInCache(artifact, cacheManager.getSavedArtifactOrigin(artifact), useOrigin);
					if (!useOrigin && !archive.exists()) {
						// file is not available in cache, maybe the last resolve was performed with useOrigin=true.
						// we try to use the best we can
						archive = cacheManager.getArchiveFileInCache(artifact, cacheManager.getSavedArtifactOrigin(artifact));
					}
				}
                Set dest = (Set)artifactsToCopy.get(artifact);
                Message.verbose("\tretrieving "+archive);
                for (Iterator it2 = dest.iterator(); it2.hasNext();) {
                	checkInterrupted();
                    File destFile = new File((String)it2.next());
                    if (!_settings.isCheckUpToDate() || !upToDate(archive, destFile)) {
                        Message.verbose("\t\tto "+destFile);
                        if (makeSymlinks) {
                            FileUtil.symlink(archive, destFile, null, false);
                        } else {
                            FileUtil.copy(archive, destFile, null);
                        }
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

    public CacheManager getCacheManager(File cache) {
    	// TODO reuse instance
		return new CacheManager(_settings, cache);
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
        destIvyPattern = _settings.substitute(destIvyPattern);
        CacheManager cacheManager = getCacheManager(cache);
        
        // 1) find the resolved module descriptor in cache
        File ivyFile = cacheManager.getResolvedIvyFileInCache(mrid);
        if (!ivyFile.exists()) {
            throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        }
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
            ivyFileURL = ivyFile.toURL();
            md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFileURL, validate);
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(mrid, revision));
            md.setResolvedPublicationDate(pubdate);
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url obtained for file "+ivyFile , e);
        } catch (ParseException e) {
            throw new RuntimeException("bad ivy file in cache for "+mrid+": please clean and resolve again" , e);
        }
        
        // 2) parse resolvedRevisions From properties file
        Map resolvedRevisions = new HashMap(); // Map (ModuleId -> String revision)
        Map dependenciesStatus = new HashMap(); // Map (ModuleId -> String status)
        File ivyProperties = cacheManager.getResolvedIvyPropertiesInCache(mrid);
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
            XmlModuleDescriptorUpdater.update(_settings, ivyFileURL, 
                    new File(publishedIvy),
                    resolvedDependencies, status, revision, pubdate, null, true);
        } catch (SAXException ex) {
            throw new RuntimeException("bad ivy file in cache for "+mrid+": please clean and resolve again" , ex);
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
        srcIvyPattern = _settings.substitute(srcIvyPattern);
        CacheManager cacheManager = getCacheManager(cache);
        // 1) find the resolved module descriptor
        ModuleRevisionId pubmrid = ModuleRevisionId.newInstance(mrid, pubrevision);
        File ivyFile;
        if (srcIvyPattern != null) {
        	ivyFile = new File(IvyPatternHelper.substitute(srcIvyPattern, DefaultArtifact.newIvyArtifact(pubmrid, new Date())));
        	if (!ivyFile.exists()) {
        		throw new IllegalArgumentException("ivy file to publish not found for "+mrid+": call deliver before ("+ivyFile+")");
        	}
        } else {
        	ivyFile = cacheManager.getResolvedIvyFileInCache(mrid);
        	if (!ivyFile.exists()) {
        		throw new IllegalStateException("ivy file not found in cache for "+mrid+": please resolve dependencies before publishing ("+ivyFile+")");
        	}
        }
        
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
        	ivyFileURL = ivyFile.toURL();
        	md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFileURL, false);
        	if (srcIvyPattern != null) {
            	if (!pubrevision.equals(md.getModuleRevisionId().getRevision())) {
            		if (update) {
            			File tmp = File.createTempFile("ivy", ".xml");
            			tmp.deleteOnExit();
            			try {
							XmlModuleDescriptorUpdater.update(_settings, ivyFileURL, tmp, new HashMap(), status==null?md.getStatus():status, pubrevision, pubdate==null?new Date():pubdate, null, true);
							ivyFile = tmp;
							// we parse the new file to get updated module descriptor
							md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_settings, ivyFile.toURL(), false);
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
        
        DependencyResolver resolver = _settings.getResolver(resolverName);
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
				published = publish(artifact, _settings.substitute(pattern), resolver, overwrite);
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
        return _sortEngine.sortNodes(nodes);
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
        return _sortEngine.sortModuleDescriptors(moduleDescriptors);   
    }
    
    /////////////////////////////////////////////////////////////////////////
    //                         CACHE
    /////////////////////////////////////////////////////////////////////////
    
    
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
    			operatingThread.join(_settings.getInterruptTimeout());
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
        

    /**
     * Returns an empty array when no token values are found.
     *  
     * @param token
     * @param otherTokenValues
     * @return
     */
	public String[] listTokenValues(String token, Map otherTokenValues) {
        List r = new ArrayList();
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            r.addAll(Arrays.asList(resolver.listTokenValues(token, otherTokenValues)));
        }
        return (String[])r.toArray(new String[r.size()]);
	}
    
    public OrganisationEntry[] listOrganisationEntries() {
        List entries = new ArrayList();
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listOrganisations()));
        }
        return (OrganisationEntry[])entries.toArray(new OrganisationEntry[entries.size()]);
    }
    public String[] listOrganisations() {
        Collection orgs = new HashSet();
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
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
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listModules(org)));
        }
        return (ModuleEntry[])entries.toArray(new ModuleEntry[entries.size()]);
    }
    public String[] listModules(String org) {
        List mods = new ArrayList();
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
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
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver)iter.next();
            entries.addAll(Arrays.asList(resolver.listRevisions(module)));
        }
        return (RevisionEntry[])entries.toArray(new RevisionEntry[entries.size()]);
    }
    public String[] listRevisions(String org, String module) {
        List revs = new ArrayList();
        for (Iterator iter = _settings.getResolvers().iterator(); iter.hasNext();) {
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
							branches = new String[]  {_settings.getDefaultBranch(new ModuleId(orgs[i], mods[j]))};
						}
						for (int k = 0; k < branches.length; k++) {
							if (branches[k] == null || branchMatcher.matches(branches[k])) {
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

    /**
     * Check if the current operation has been interrupted, and if it is the case, throw a runtime exception
     */
	public void checkInterrupted() {
		if (isInterrupted()) {
			Message.info("operation interrupted");
			throw new RuntimeException("operation interrupted");
		}
	}

	public static String getWorkingRevision() {
		return "working@" + HostUtil.getLocalHostName();
	}

	public IvySettings getSettings() {
		return _settings;
	}

	public static Ivy newInstance() {
		Ivy ivy = new Ivy();
		ivy.bind();
		return ivy;
	}

	/**
	 * This method is used to bind this Ivy instance to 
	 * required dependencies, i.e. instance of settings, engines, and so on.
	 * After thes call Ivy is still not configured, which means that the settings
	 * object is still empty.
	 */
	private void bind() {
        IvyContext.getContext().setIvy(this);
		_settings = new IvySettings();
		_sortEngine = new SortEngine(_settings);
		_eventManager = new EventManager();
		_resolveEngine = new ResolveEngine(_settings, _eventManager, _sortEngine);
		
		_eventManager.addTransferListener(new TransferListener() {
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

	public void configure(File settingsFile) throws ParseException, IOException {
		assertBound();
		_settings.load(settingsFile);
		postConfigure();
	}

	public void configure(URL settingsURL) throws ParseException, IOException {
		assertBound();
		_settings.load(settingsURL);
		postConfigure();
	}

	public void configureDefault() throws ParseException, IOException {
		assertBound();
		_settings.loadDefault();
		postConfigure();
	}

	private void assertBound() {
		if (_settings == null) {
			bind();
		}
	}

	private void postConfigure() {
		Collection triggers = _settings.getTriggers();
		for (Iterator iter = triggers.iterator(); iter.hasNext();) {
			Trigger trigger = (Trigger) iter.next();
			_eventManager.addIvyListener(trigger, trigger.getEventFilter());
		}
	}

	public EventManager getEventManager() {
		assertBound();
		return _eventManager;
	}

	public String getVariable(String name) {
		assertBound();
		return _settings.getVariable(name);
	}

	public String substitute(String str) {
		assertBound();
		return _settings.substitute(str);
	}

	public void setVariable(String varName, String value) {
		assertBound();
		_settings.setVariable(varName, value);
	}

}
