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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.deliver.PublishingDependencyRevisionResolver;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;



/**
 * This class can be used for easy migration from Ivy 1.4 API.
 * 
 * Indeed, Ivy 2.0 API has changed substantially, so it can take time to
 * migrate existing code using Ivy 1.4 API to the new API.
 * 
 * Using this class it's really easy: replace your instance of Ivy by an 
 * instance of this class. For instance, where you were doing:
 * Ivy ivy = new Ivy();
 * 
 * do instead:
 * Ivy14 ivy = new Ivy14();
 * 
 * And that should be enough in most cases!
 *  
 * @author Xavier Hanin
 */
public class Ivy14 {
	private Ivy _ivy;
    
    public Ivy14() {
    	this(Ivy.newInstance());
    }

	public Ivy14(Ivy ivy) {
		_ivy = ivy;
	}

	public boolean check(URL ivyFile, String resolvername) {
		return _ivy.check(ivyFile, resolvername);
	}

	public void configure(File settingsFile) throws ParseException, IOException {
		_ivy.configure(settingsFile);
	}

	public void configure(URL settingsURL) throws ParseException, IOException {
		_ivy.configure(settingsURL);
	}

	public void configureDefault() throws ParseException, IOException {
		_ivy.configureDefault();
	}

	public void deliver(ModuleRevisionId mrid, String revision, File cache, String destIvyPattern, String status, Date pubdate, PublishingDependencyRevisionResolver pdrResolver, boolean validate, boolean resolveDynamicRevisions) throws IOException, ParseException {
		_ivy.deliver(mrid, revision, destIvyPattern, new DeliverOptions(status, pubdate, CacheManager.getInstance(_ivy.getSettings(), cache), pdrResolver, validate, resolveDynamicRevisions));
	}

	public void deliver(ModuleRevisionId mrid, String revision, File cache, String destIvyPattern, String status, Date pubdate, PublishingDependencyRevisionResolver pdrResolver, boolean validate) throws IOException, ParseException {
		deliver(mrid, revision, cache, destIvyPattern, status, pubdate, pdrResolver, validate, true);
	}

	public Map determineArtifactsToCopy(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.getRetrieveEngine().determineArtifactsToCopy(
				new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
				destFilePattern, 
				new RetrieveOptions()
					.setConfs(confs)
					.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
					.setDestIvyPattern(destIvyPattern)
					.setArtifactFilter(artifactFilter));
	}

	public Map determineArtifactsToCopy(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern) throws ParseException, IOException {
		return _ivy.getRetrieveEngine().determineArtifactsToCopy(
				new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
				destFilePattern, 
				new RetrieveOptions()
					.setConfs(confs)
					.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
					.setDestIvyPattern(destIvyPattern));
	}

	public ArtifactDownloadReport download(Artifact artifact, File cache, boolean useOrigin) {
		return _ivy.getResolveEngine().download(artifact, _ivy.getCacheManager(cache), useOrigin);
	}

	public void downloadArtifacts(ResolveReport report, CacheManager cacheManager, boolean useOrigin, Filter artifactFilter) {
		_ivy.getResolveEngine().downloadArtifacts(report, cacheManager, useOrigin, artifactFilter);
	}

	public ResolvedModuleRevision findModule(ModuleRevisionId id) {
    	ResolveOptions options = new ResolveOptions();
    	options.setValidate(false);
    	options.setCache(CacheManager.getInstance(_ivy.getSettings()));
		return _ivy.getResolveEngine().findModule(id, options);
	}

	public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate, boolean transitive) {
		return _ivy.getResolveEngine().getDependencies(md, newResolveOptions(confs, null, cache, date, validate, false, transitive, false, true, true, FilterHelper.NO_FILTER), report);
	}

	public IvyNode[] getDependencies(ModuleDescriptor md, String[] confs, File cache, Date date, ResolveReport report, boolean validate) {
		return _ivy.getResolveEngine().getDependencies(md, newResolveOptions(confs, null, cache, date, validate, false, true, false, true, true, FilterHelper.NO_FILTER), report);
	}

	public IvyNode[] getDependencies(URL ivySource, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
		return _ivy.getResolveEngine().getDependencies(ivySource, newResolveOptions(confs, null, cache, date, validate, false, true, false, true, true, FilterHelper.NO_FILTER));
	}

	public String getVariable(String name) {
		return _ivy.getVariable(name);
	}

	public ResolveReport install(ModuleRevisionId mrid, String from, String to, boolean transitive, boolean validate, boolean overwrite, Filter artifactFilter, File cache, String matcherName) throws IOException {
		return _ivy.install(mrid, from, to, transitive, validate, overwrite, artifactFilter, cache, matcherName);
	}

	public void interrupt() {
		_ivy.interrupt();
	}

	public void interrupt(Thread operatingThread) {
		_ivy.interrupt(operatingThread);
	}

	public boolean isInterrupted() {
		return _ivy.isInterrupted();
	}

	public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
		return _ivy.listModuleEntries(org);
	}

	public ModuleId[] listModules(ModuleId criteria, PatternMatcher matcher) {
		return _ivy.listModules(criteria, matcher);
	}

	public ModuleRevisionId[] listModules(ModuleRevisionId criteria, PatternMatcher matcher) {
		return _ivy.listModules(criteria, matcher);
	}

	public String[] listModules(String org) {
		return _ivy.listModules(org);
	}

	public OrganisationEntry[] listOrganisationEntries() {
		return _ivy.listOrganisationEntries();
	}

	public String[] listOrganisations() {
		return _ivy.listOrganisations();
	}

	public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
		return _ivy.listRevisionEntries(module);
	}

	public String[] listRevisions(String org, String module) {
		return _ivy.listRevisions(org, module);
	}

	public String[] listTokenValues(String token, Map otherTokenValues) {
		return _ivy.listTokenValues(token, otherTokenValues);
	}

	public Collection publish(ModuleDescriptor md, DependencyResolver resolver, Collection srcArtifactPattern, String srcIvyPattern, Artifact[] extraArtifacts, boolean overwrite, String conf) throws IOException {
		return _ivy.getPublishEngine().publish(md, srcArtifactPattern, resolver, 
				new PublishOptions()
					.setSrcIvyPattern(srcIvyPattern)
					.setExtraArtifacts(extraArtifacts)
					.setOverwrite(overwrite)
					.setConfs(splitConfs(conf)));
	}

	public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, Collection srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
		return _ivy.publish(mrid, srcArtifactPattern, resolverName, 
				new PublishOptions()
					.setCache(_ivy.getCacheManager(cache == null?_ivy.getSettings().getDefaultCache():cache))
					.setStatus(status)
					.setPubdate(pubdate)
					.setPubrevision(pubrevision)
					.setSrcIvyPattern(srcIvyPattern)
					.setExtraArtifacts(extraArtifacts)
					.setUpdate(update)
					.setValidate(validate)
					.setOverwrite(overwrite)
					.setConfs(splitConfs(conf))
				);
	}

	public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate, boolean overwrite) throws IOException {
		return _ivy.publish(mrid, Collections.singleton(srcArtifactPattern), resolverName, 
				new PublishOptions()
					.setCache(_ivy.getCacheManager(cache == null?_ivy.getSettings().getDefaultCache():cache))
					.setPubrevision(pubrevision)
					.setSrcIvyPattern(srcIvyPattern)
					.setValidate(validate)
					.setOverwrite(overwrite)
				);
	}

	public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, boolean validate) throws IOException {
		return _ivy.publish(mrid, Collections.singleton(srcArtifactPattern), resolverName, 
				new PublishOptions()
					.setCache(_ivy.getCacheManager(cache == null?_ivy.getSettings().getDefaultCache():cache))
					.setPubrevision(pubrevision)
					.setSrcIvyPattern(srcIvyPattern)
					.setValidate(validate)
				);
	}

	public Collection publish(ModuleRevisionId mrid, String pubrevision, File cache, String srcArtifactPattern, String resolverName, String srcIvyPattern, String status, Date pubdate, Artifact[] extraArtifacts, boolean validate, boolean overwrite, boolean update, String conf) throws IOException {
		return _ivy.publish(mrid, Collections.singleton(srcArtifactPattern), resolverName, 
				new PublishOptions()
					.setCache(_ivy.getCacheManager(cache == null?_ivy.getSettings().getDefaultCache():cache))
					.setStatus(status)
					.setPubdate(pubdate)
					.setPubrevision(pubrevision)
					.setSrcIvyPattern(srcIvyPattern)
					.setExtraArtifacts(extraArtifacts)
					.setUpdate(update)
					.setValidate(validate)
					.setOverwrite(overwrite)
					.setConfs(splitConfs(conf))
				);
	}

	public ResolveReport resolve(File ivySource) throws ParseException, IOException {
		return _ivy.resolve(ivySource);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _ivy.resolve(md, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, transitive, useOrigin, download, outputReport, artifactFilter));
	}

	private ResolveOptions newResolveOptions(String[] confs, String revision, File cache, Date date, boolean validate, 
			boolean useCacheOnly, boolean transitive, boolean useOrigin, boolean download, boolean outputReport, Filter artifactFilter) {
		return new ResolveOptions()
					.setConfs(confs)
					.setRevision(revision)
					.setCache(_ivy.getCacheManager(cache == null?_ivy.getSettings().getDefaultCache():cache))
					.setValidate(validate)
					.setUseCacheOnly(useCacheOnly)
					.setTransitive(transitive)
					.setUseOrigin(useOrigin)
					.setDownload(download)
					.setOutputReport(outputReport)
					.setArtifactFilter(artifactFilter);
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean download, boolean outputReport, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _ivy.resolve(md, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, transitive, false, download, outputReport, artifactFilter));
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _ivy.resolve(md, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, transitive, false, true, true, artifactFilter));
	}

	public ResolveReport resolve(ModuleDescriptor md, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException, FileNotFoundException {
		return _ivy.resolve(md, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, true, false, true, true, artifactFilter));
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.resolve(mrid, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, transitive, useOrigin, true, true, artifactFilter), changing);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs, boolean transitive, boolean changing, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.resolve(mrid, newResolveOptions(confs, null, cache, date, validate, useCacheOnly, transitive, false, true, true, artifactFilter), changing);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, String[] confs) throws ParseException, IOException {
		return _ivy.resolve(mrid, newResolveOptions(confs, null, _ivy.getSettings().getDefaultCache(), null, true, false, true, false, true, true, FilterHelper.NO_FILTER), false);
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, boolean useOrigin, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.resolve(ivySource, newResolveOptions(confs, revision, cache, date, validate, useCacheOnly, transitive, useOrigin, true, true, artifactFilter));
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, boolean transitive, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.resolve(ivySource, newResolveOptions(confs, revision, cache, date, validate, useCacheOnly, transitive, false, true, true, artifactFilter));
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly, Filter artifactFilter) throws ParseException, IOException {
		return _ivy.resolve(ivySource, newResolveOptions(confs, revision, cache, date, validate, useCacheOnly, true, false, true, true, artifactFilter));
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate, boolean useCacheOnly) throws ParseException, IOException {
		return _ivy.resolve(ivySource, newResolveOptions(confs, revision, cache, date, validate, useCacheOnly, true, false, true, true, FilterHelper.NO_FILTER));
	}

	public ResolveReport resolve(URL ivySource, String revision, String[] confs, File cache, Date date, boolean validate) throws ParseException, IOException {
		return _ivy.resolve(ivySource, newResolveOptions(confs, revision, cache, date, validate, false, true, false, true, true, FilterHelper.NO_FILTER));
	}

	public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
		return _ivy.resolve(ivySource);
	}

	public int retrieve(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter, boolean sync, boolean useOrigin, boolean makeSymlinks) {
		try {
			return _ivy.retrieve(
					new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
					destFilePattern, 
					new RetrieveOptions()
						.setConfs(confs)
						.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
						.setDestIvyPattern(destIvyPattern)
						.setArtifactFilter(artifactFilter)
						.setSync(sync)
						.setUseOrigin(useOrigin)
						.setMakeSymlinks(makeSymlinks));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int retrieve(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter, boolean sync, boolean useOrigin) {
		try {
			return _ivy.retrieve(
					new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
					destFilePattern, 
					new RetrieveOptions()
						.setConfs(confs)
						.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
						.setDestIvyPattern(destIvyPattern)
						.setArtifactFilter(artifactFilter)
						.setSync(sync)
						.setUseOrigin(useOrigin));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int retrieve(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern, Filter artifactFilter) {
		try {
			return _ivy.retrieve(
					new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
					destFilePattern, 
					new RetrieveOptions()
						.setConfs(confs)
						.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
						.setDestIvyPattern(destIvyPattern)
						.setArtifactFilter(artifactFilter));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int retrieve(ModuleId moduleId, String[] confs, File cache, String destFilePattern, String destIvyPattern) {
		try {
			return _ivy.retrieve(
					new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
					destFilePattern, 
					new RetrieveOptions()
						.setConfs(confs)
						.setCache(CacheManager.getInstance(_ivy.getSettings(), cache))
						.setDestIvyPattern(destIvyPattern));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int retrieve(ModuleId moduleId, String[] confs, File cache, String destFilePattern) {
		try {
			return _ivy.retrieve(
					new ModuleRevisionId(moduleId, Ivy.getWorkingRevision()), 
					destFilePattern, 
					new RetrieveOptions()
						.setConfs(confs)
						.setCache(CacheManager.getInstance(_ivy.getSettings(), cache)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setVariable(String varName, String value) {
		_ivy.setVariable(varName, value);
	}

	public List sortModuleDescriptors(Collection moduleDescriptors) {
		return _ivy.sortModuleDescriptors(moduleDescriptors);
	}

	public List sortNodes(Collection nodes) {
		return _ivy.sortNodes(nodes);
	}

	public String substitute(String str) {
		return _ivy.substitute(str);
	}
    

	private String[] splitConfs(String conf) {
		if (conf == null || "".equals(conf)) {
			return null;
		}
        String[] confs = conf.split(",");
        for (int i = 0; i < confs.length; i++) {
            confs[i] = confs[i].trim();
        }
        return confs;
	}
	
}
