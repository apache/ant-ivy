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

import java.io.File;
import java.text.ParseException;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

public interface RepositoryCacheManager {
    public abstract File getIvyFileInCache(ModuleRevisionId mrid);

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local and if
     * the resolve has been done with useOrigin = true
     */
    public abstract File getArchiveFileInCache(Artifact artifact);

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local and if
     * the resolve has been done with useOrigin = true
     */
    public abstract File getArchiveFileInCache(Artifact artifact, ArtifactOrigin origin);

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system,
     * using or not the original location depending on the availability of origin information
     * provided as parameter and the setting of useOrigin. If useOrigin is false, this method will
     * always return the file in the cache.
     */
    public abstract File getArchiveFileInCache(Artifact artifact, ArtifactOrigin origin,
            boolean useOrigin);

    public abstract String getArchivePathInCache(Artifact artifact);

    public abstract String getArchivePathInCache(Artifact artifact, ArtifactOrigin origin);

    /**
     * Saves the information of which resolvers were used to resolve a module (both for metadata and
     * artifact), so that this info can be loaded later (even after a jvm restart) for the use of
     * {@link #findModuleInCache(ModuleRevisionId, boolean, String)}.
     * 
     * @param md
     *            the module descriptor resolved
     * @param metadataResolverName
     *            metadata resolver name
     * @param artifactResolverName
     *            artifact resolver name
     */
    public abstract void saveResolvers(
            ModuleDescriptor descriptor, String metadataResolverName, String artifactResolverName);

    public abstract ArtifactOrigin getSavedArtifactOrigin(Artifact artifact);

    /**
     * Search a module descriptor in cache for a mrid
     * 
     * @param mrid
     *            the id of the module to search
     * @param validate
     *            true to validate ivy file found in cache before returning
     * @param expectedResolver
     *            the resolver with which the md in cache must have been resolved to be returned,
     *            null if this doesn't matter
     * @return the ResolvedModuleRevision corresponding to the module found, null if none correct
     *         has been found in cache
     */
    public abstract ResolvedModuleRevision findModuleInCache(
            ModuleRevisionId mrid, boolean validate, String expectedResolver);
    
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
    public abstract ArtifactDownloadReport download(
            Artifact artifact, 
            ArtifactResourceResolver resourceResolver, 
            ResourceDownloader resourceDownloader, 
            CacheDownloadOptions options);

    public ResolvedModuleRevision cacheModuleDescriptor(
            DependencyResolver resolver, ResolvedResource orginalMetadataRef, 
            Artifact requestedMetadataArtifact, 
            ResourceDownloader downloader, CacheMetadataOptions options) throws ParseException;

    public void originalToCachedModuleDescriptor(
            DependencyResolver resolver, ResolvedResource orginalMetadataRef, 
            Artifact requestedMetadataArtifact, 
            ModuleDescriptor md, ModuleDescriptorWriter writer);
}
