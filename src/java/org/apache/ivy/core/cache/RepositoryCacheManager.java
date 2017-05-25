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
package org.apache.ivy.core.cache;

import java.text.ParseException;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

public interface RepositoryCacheManager {

    /**
     * Returns the name of the repository cache manager.
     * 
     * @return the name of the repository cache manager.
     */
    public abstract String getName();

    /**
     * Saves the information of which resolvers were used to resolve a module (both for metadata and
     * artifact), so that this info can be loaded later (even after a jvm restart) for the use of
     * {@link #findModuleInCache(DependencyDescriptor, CacheMetadataOptions, String)}.
     * 
     * @param md
     *            the module descriptor resolved
     * @param metadataResolverName
     *            metadata resolver name
     * @param artifactResolverName
     *            artifact resolver name
     */
    public abstract void saveResolvers(ModuleDescriptor descriptor, String metadataResolverName,
            String artifactResolverName);

    /**
     * Returns the artifact origin of the given artifact as saved in this cache.
     * <p>
     * If the origin is unknown, the returned ArtifactOrigin instance will return true when
     * {@link ArtifactOrigin#isUnknown(ArtifactOrigin)} is called.
     * 
     * @param artifact
     *            the artifact for which the saved artifact origin should be returned.
     * @return the artifact origin of the given artifact as saved in this cache
     */
    public abstract ArtifactOrigin getSavedArtifactOrigin(Artifact artifact);

    /**
     * Search a module descriptor in cache for a mrid
     * 
     * @param dd
     *            the dependency descriptor identifying the module to search
     * @param requestedRevisionId
     *            the requested dependency module revision id identifying the module to search
     * @param options
     *            options on how caching should be handled
     * @param expectedResolver
     *            the resolver with which the md in cache must have been resolved to be returned,
     *            null if this doesn't matter
     * @return the ResolvedModuleRevision corresponding to the module found, null if none correct
     *         has been found in cache
     */
    public abstract ResolvedModuleRevision findModuleInCache(DependencyDescriptor dd,
            ModuleRevisionId requestedRevisionId, CacheMetadataOptions options,
            String expectedResolver);

    /**
     * Downloads an artifact to this cache.
     * 
     * @param artifact
     *            the artifact to download
     * @param resourceResolver
     *            a resource resolver to use if the artifact needs to be resolved to a Resource for
     *            downloading
     * @param resourceDownloader
     *            a resource downloader to use if actual download of the resource is needed
     * @param options
     *            a set of options to adjust the download
     * @return a report indicating how the download was performed
     */
    public abstract ArtifactDownloadReport download(Artifact artifact,
            ArtifactResourceResolver resourceResolver, ResourceDownloader resourceDownloader,
            CacheDownloadOptions options);

    /**
     * Download some repository resource and put it in the cache.
     * <p>
     * If the cached version is considered enough up to date, no downloading is done.
     * 
     * @param resource
     *            the resource of the file to put in cache
     * @param name
     *            the descriptive name of the resource (helps while manually looking into the cache
     *            files)
     * @param type
     *            the type of the resource (helps while manually looking into the cache files)
     * @param extension
     *            the extension of the resource (helps while manually looking into the cache files)
     * @param options
     *            a set of options to adjust the download
     * @param repository
     *            the repository which resolve the content of the resource
     * @return a report indicating how the download was performed
     */
    public ArtifactDownloadReport downloadRepositoryResource(Resource resource, String name,
            String type, String extension, CacheResourceOptions options, Repository repository);

    /**
     * Caches an original module descriptor.
     * <p>
     * After this call, the original module descriptor file (with no modification nor conversion)
     * should be available as a local file.
     * </p>
     * 
     * @param resolver
     *            the dependency resolver from which the cache request comes from
     * @param orginalMetadataRef
     *            a resolved resource pointing to the remote original module descriptor
     * @param dd
     *            the dependency descriptor for which the module descriptor should be cached
     * @param requestedMetadataArtifact
     *            the module descriptor artifact as requested originally
     * @param downloader
     *            a ResourceDownloader able to download the original module descriptor resource if
     *            required by this cache implementation
     * @param options
     *            options to apply to cache this module descriptor
     * @return a {@link ResolvedModuleRevision} representing the local cached module descriptor, or
     *         null if it failed
     * @throws ParseException
     *             if an exception occurred while parsing the module descriptor
     */
    public ResolvedModuleRevision cacheModuleDescriptor(DependencyResolver resolver,
            ResolvedResource orginalMetadataRef, DependencyDescriptor dd,
            Artifact requestedMetadataArtifact, ResourceDownloader downloader,
            CacheMetadataOptions options) throws ParseException;

    /**
     * Stores a standardized version of an original module descriptor in the cache for later use.
     * 
     * @param resolver
     *            the dependency resolver from which the cache request comes from
     * @param orginalMetadataRef
     *            a resolved resource pointing to the remote original module descriptor
     * @param requestedMetadataArtifact
     *            the module descriptor artifact as requested originally
     * @param rmr
     *            the {@link ResolvedModuleRevision} representing the local cached module descriptor
     * @param writer
     *            a {@link ModuleDescriptorWriter} able to write the module descriptor to a stream.
     */
    public void originalToCachedModuleDescriptor(DependencyResolver resolver,
            ResolvedResource orginalMetadataRef, Artifact requestedMetadataArtifact,
            ResolvedModuleRevision rmr, ModuleDescriptorWriter writer);

    /**
     * Cleans the whole cache.
     */
    public void clean();

    /**
     * Caches a dynamic revision constraint resolution.
     * 
     * @param dynamicMrid
     *            the dynamic module revision id
     * @param revision
     *            the resolved revision
     * @deprecated See {@link #saveResolvedRevision(String, ModuleRevisionId, String)} which
     *             prevents cache + * thrashing when multiple resolvers store the same dynamicMrid
     */
    public void saveResolvedRevision(ModuleRevisionId dynamicMrid, String revision);

    /**
     * Caches a dynamic revision constraint resolution for a specific resolver.
     * 
     * @param resolverName
     *            the resolver in which this dynamic revision was resolved
     * @param dynamicMrid
     *            the dynamic module revision id
     * @param revision
     *            the resolved revision
     */
    public void saveResolvedRevision(String resolverName, ModuleRevisionId dynamicMrid,
            String revision);

}
