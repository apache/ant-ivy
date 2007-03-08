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

import java.util.Date;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

/**
 * A set of options used during resolve related tasks
 * 
 * @see ResolveEngine
 * @author Xavier Hanin
 */
public class ResolveOptions {
	/**
	 * an array of configuration names to resolve - must not be null nor empty
	 */
	private String[] confs = new String[] {"*"};
	/**
	 * the revision of the module for which dependencies should be resolved.
     * This revision is considered as the resolved revision of the module, unless it is null.
     * If it is null, then a default revision is given if necessary (no revision found in ivy file)
	 */
	private String revision = null;
	/**
	 * The cache manager to use during resolve.
	 * If it is null, default cache manager will be used. 
	 */
	private CacheManager cache = null;
	/**
	 * the date for which the dependencies should be resolved. 
	 * All obtained artifacts should have a publication date which is before 
	 * or equal to the given date.
	 * The date can be null, in which case all artifacts will be considered
	 */
	private Date date = null;
	/**
	 * True if validation of module descriptors should done, false otherwise
	 */
	private boolean validate = true;
	/**
	 * True if only the cache should be used for resolve, false
	 * if a real resolve with dependency resolvers should be done
	 */
	private boolean useCacheOnly = false;
	/**
	 * True if the dependencies should be resolved transitively, false
	 * if only direct dependencies should be resolved
	 */
	private boolean transitive = true;
	/**
	 * True if the resolve should also download artifacts, false
	 * if only dependency resolution with module descriptors should be done
	 */
	private boolean download = true;
	/**
	 * True if a report of the resolve process should be output at the end of the 
	 * process, false otherwise
	 */
	private boolean outputReport = true;
	/**
	 * True if the original files from the repositories should be used instead 
	 * of downloaded ones, false otherwise.
	 * This means that artifacts which can be used directory in their original location
	 * won't be downloaded if this option is set to true 
	 */
	private boolean useOrigin = false;
	/**
	 * A filter to use to avoid downloading all artifacts.
	 */
	private Filter artifactFilter = FilterHelper.NO_FILTER;
	/**
	 * The id used to store the resolve information.
	 */
	private String resolveId;
	
	public ResolveOptions() {
	}
	
	public ResolveOptions(ResolveOptions options) {
		confs = options.confs;
		revision = options.revision;
		cache = options.cache;
		date = options.date;
		validate = options.validate;
		useCacheOnly = options.useCacheOnly;
		transitive = options.transitive;
		download = options.download;
		outputReport = options.outputReport;
		useOrigin = options.useOrigin;
		artifactFilter = options.artifactFilter;
		resolveId = options.resolveId;
	}
	
	public Filter getArtifactFilter() {
		return artifactFilter;
	}
	public ResolveOptions setArtifactFilter(Filter artifactFilter) {
		this.artifactFilter = artifactFilter;
		return this;
	}
	public CacheManager getCache() {
		return cache;
	}
	public ResolveOptions setCache(CacheManager cache) {
		this.cache = cache;
		return this;
	}
	public String[] getConfs() {
		return confs;
	}
	public ResolveOptions setConfs(String[] confs) {
		this.confs = confs;
		return this;
	}
	public Date getDate() {
		return date;
	}
	public ResolveOptions setDate(Date date) {
		this.date = date;
		return this;
	}
	public boolean isDownload() {
		return download;
	}
	public ResolveOptions setDownload(boolean download) {
		this.download = download;
		return this;
	}
	public boolean isOutputReport() {
		return outputReport;
	}
	public ResolveOptions setOutputReport(boolean outputReport) {
		this.outputReport = outputReport;
		return this;
	}
	public boolean isTransitive() {
		return transitive;
	}
	public ResolveOptions setTransitive(boolean transitive) {
		this.transitive = transitive;
		return this;
	}
	public boolean isUseCacheOnly() {
		return useCacheOnly;
	}
	public ResolveOptions setUseCacheOnly(boolean useCacheOnly) {
		this.useCacheOnly = useCacheOnly;
		return this;
	}
	public boolean isValidate() {
		return validate;
	}
	public ResolveOptions setValidate(boolean validate) {
		this.validate = validate;
		return this;
	}
	public String getRevision() {
		return revision;
	}
	public ResolveOptions setRevision(String revision) {
		this.revision = revision;
		return this;
	}
	public boolean isUseOrigin() {
		return useOrigin;
	}
	public ResolveOptions setUseOrigin(boolean useOrigin) {
		this.useOrigin = useOrigin;
		return this;
	}
	public String getResolveId() {
		return resolveId;
	}
	public ResolveOptions setResolveId(String resolveId) {
		this.resolveId = resolveId;
		return this;
	}
	public static String getDefaultResolveId(ModuleDescriptor md) {
    	ModuleId module = md.getModuleRevisionId().getModuleId();
    	return module.getOrganisation() + "-" + module.getName();
	}
	public static String getDefaultResolveId(ModuleId moduleId) {
    	return moduleId.getOrganisation() + "-" + moduleId.getName();
	}
}
