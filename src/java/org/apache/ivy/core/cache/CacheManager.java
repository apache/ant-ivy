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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DefaultModuleRevision;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.repository.ResourceHelper;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.PropertiesFile;

public class CacheManager implements RepositoryCacheManager, ResolutionCacheManager {
    public static CacheManager getInstance(CacheSettings settings, File cache) {
        return new CacheManager(settings, cache);
    }

    public static CacheManager getInstance(CacheSettings settings) {
        return getInstance(settings, settings.getDefaultCache());
    }

    private CacheSettings settings;
    
    private File cache;

    private LockStrategy lockStrategy;

    // TODO: use different names when we'll be able to have multiple cache managers
    private String name = "cache"; 

    public CacheManager(CacheSettings settings, File cache) {
        this.settings = settings == null ? IvyContext.getContext().getSettings() : settings;
        if (cache == null) {
            this.cache = settings.getDefaultCache();
        } else {
            this.cache = cache;
        }
    }

    public File getResolvedIvyFileInCache(ModuleRevisionId mrid) {
        String file = IvyPatternHelper.substitute(settings.getCacheResolvedIvyPattern(), mrid
                .getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml");
        return new File(getResolutionCacheRoot(), file);
    }

    public File getResolvedIvyPropertiesInCache(ModuleRevisionId mrid) {
        String file = IvyPatternHelper.substitute(settings.getCacheResolvedIvyPropertiesPattern(),
            mrid.getOrganisation(), mrid.getName(), mrid.getRevision(), "ivy", "ivy", "xml");
        return new File(getResolutionCacheRoot(), file);
    }

    public File getConfigurationResolveReportInCache(String resolveId, String conf) {
        return new File(getResolutionCacheRoot(), resolveId + "-" + conf + ".xml");
    }

    public File[] getConfigurationResolveReportsInCache(final String resolveId) {
        final String prefix = resolveId + "-";
        final String suffix = ".xml";
        return getResolutionCacheRoot().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.startsWith(prefix) && name.endsWith(suffix));
            }
        });
    }

    public File getIvyFileInCache(ModuleRevisionId mrid) {
        String file = IvyPatternHelper.substitute(settings.getCacheIvyPattern(), DefaultArtifact
                .newIvyArtifact(mrid, null));
        return new File(getRepositoryCacheRoot(), file);
    }

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local and if
     * the resolve has been done with useOrigin = true
     */
    public File getArchiveFileInCache(Artifact artifact) {
        ArtifactOrigin origin = getSavedArtifactOrigin(artifact);
        return getArchiveFileInCache(artifact, origin);
    }

    /**
     * Returns a File object pointing to where the artifact can be found on the local file system.
     * This is usually in the cache, but it can be directly in the repository if it is local and if
     * the resolve has been done with useOrigin = true
     */
    public File getArchiveFileInCache(Artifact artifact, ArtifactOrigin origin) {
        File archive = new File(getRepositoryCacheRoot(), getArchivePathInCache(artifact, origin));
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
     * using or not the original location depending on the availability of origin information
     * provided as parameter and the setting of useOrigin. If useOrigin is false, this method will
     * always return the file in the cache.
     */
    public File getArchiveFileInCache(Artifact artifact, ArtifactOrigin origin, boolean useOrigin) {
        if (useOrigin && origin != null && origin.isLocal()) {
            return new File(origin.getLocation());
        } else {
            return new File(getRepositoryCacheRoot(), getArchivePathInCache(artifact, origin));
        }
    }

    public String getArchivePathInCache(Artifact artifact) {
        return IvyPatternHelper.substitute(settings.getCacheArtifactPattern(), artifact);
    }

    public String getArchivePathInCache(Artifact artifact, ArtifactOrigin origin) {
        return IvyPatternHelper.substitute(settings.getCacheArtifactPattern(), artifact, origin);
    }

    /**
     * Saves the information of which resolver was used to resolve a md, so that this info can be
     * retrieve later (even after a jvm restart) by getSavedResolverName(ModuleDescriptor md)
     * 
     * @param md
     *            the module descriptor resolved
     * @param name
     *            resolver name
     */
    private void saveResolver(ModuleDescriptor md, String name) {
        // should always be called with a lock on module metadata artifact
        PropertiesFile cdf = getCachedDataFile(md);
        cdf.setProperty("resolver", name);
        cdf.save();
    }

    /**
     * Saves the information of which resolver was used to resolve a md, so that this info can be
     * retrieve later (even after a jvm restart) by getSavedArtResolverName(ModuleDescriptor md)
     * 
     * @param md
     *            the module descriptor resolved
     * @param name
     *            artifact resolver name
     */
    public void saveResolvers(
            ModuleDescriptor md, String metadataResolverName, String artifactResolverName) {
        ModuleRevisionId mrid = md.getResolvedModuleRevisionId();
        if (!lockMetadataArtifact(mrid)) {
            Message.error("impossible to acquire lock for " + mrid);
            return;
        }
        try {
            PropertiesFile cdf = getCachedDataFile(md);
            cdf.setProperty("resolver", metadataResolverName);
            cdf.setProperty("artifact.resolver", artifactResolverName);
            cdf.save();
        } finally {
            unlockMetadataArtifact(mrid);
        }
    }

    private String getSavedResolverName(ModuleDescriptor md) {
        // should always be called with a lock on module metadata artifact
        PropertiesFile cdf = getCachedDataFile(md);
        return cdf.getProperty("resolver");
    }

    private String getSavedArtResolverName(ModuleDescriptor md) {
        // should always be called with a lock on module metadata artifact
        PropertiesFile cdf = getCachedDataFile(md);
        return cdf.getProperty("artifact.resolver");
    }

    void saveArtifactOrigin(Artifact artifact, ArtifactOrigin origin) {
        // should always be called with a lock on module metadata artifact
        PropertiesFile cdf = getCachedDataFile(artifact.getModuleRevisionId());
        cdf.setProperty(getIsLocalKey(artifact), String.valueOf(origin.isLocal()));
        cdf.setProperty(getLocationKey(artifact), origin.getLocation());
        cdf.save();
    }

    private void removeSavedArtifactOrigin(Artifact artifact) {
        // should always be called with a lock on module metadata artifact
        PropertiesFile cdf = getCachedDataFile(artifact.getModuleRevisionId());
        cdf.remove(getLocationKey(artifact));
        cdf.remove(getIsLocalKey(artifact));
        cdf.save();
    }

    public ArtifactOrigin getSavedArtifactOrigin(Artifact artifact) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (!lockMetadataArtifact(mrid)) {
            Message.error("impossible to acquire lock for " + mrid);
            return null;
        }
        try {
            PropertiesFile cdf = getCachedDataFile(artifact.getModuleRevisionId());
            String location = cdf.getProperty(getLocationKey(artifact));
            String local = cdf.getProperty(getIsLocalKey(artifact));
            boolean isLocal = Boolean.valueOf(local).booleanValue();

            if (location == null) {
                // origin has not been specified, return null
                return null;
            }

            return new ArtifactOrigin(isLocal, location);
        } finally {
            unlockMetadataArtifact(mrid);
        }
    }

    /**
     * Creates the unique prefix key that will reference the artifact within the properties.
     * 
     * @param artifact
     *            the artifact to create the unique key from. Cannot be null.
     * @return the unique prefix key as a string.
     */
    private String getPrefixKey(Artifact artifact) {
        // use the hashcode as a uuid for the artifact (fingers crossed)
        int hashCode = artifact.getId().hashCode();
        // use just some visual cue
        return "artifact:" + artifact.getName() + "#" + artifact.getType() + "#"
                + artifact.getExt() + "#" + hashCode;
    }

    /**
     * Returns the key used to identify the location of the artifact.
     * 
     * @param artifact
     *            the artifact to generate the key from. Cannot be null.
     * @return the key to be used to reference the artifact location.
     */
    private String getLocationKey(Artifact artifact) {
        String prefix = getPrefixKey(artifact);
        return prefix + ".location";
    }

    /**
     * Returns the key used to identify if the artifact is local.
     * 
     * @param artifact
     *            the artifact to generate the key from. Cannot be null.
     * @return the key to be used to reference the artifact location.
     */
    private String getIsLocalKey(Artifact artifact) {
        String prefix = getPrefixKey(artifact);
        return prefix + ".is-local";
    }

    private PropertiesFile getCachedDataFile(ModuleDescriptor md) {
        return getCachedDataFile(md.getResolvedModuleRevisionId());
    }

    private PropertiesFile getCachedDataFile(ModuleRevisionId mRevId) {
        return new PropertiesFile(new File(getRepositoryCacheRoot(), 
            IvyPatternHelper.substitute(settings
                .getCacheDataFilePattern(), mRevId)), "ivy cached data file for " + mRevId);
    }

    public ResolvedModuleRevision findModuleInCache(
            ModuleRevisionId mrid, boolean validate, String expectedResolver) {
        if (!settings.getVersionMatcher().isDynamic(mrid)) {
            if (!lockMetadataArtifact(mrid)) {
                Message.error("impossible to acquire lock for " + mrid);
                return null;
            }
            try {
                // first, check if it is in cache
                File ivyFile = getIvyFileInCache(mrid);
                if (ivyFile.exists()) {
                    // found in cache !
                    try {
                        ModuleDescriptor depMD = XmlModuleDescriptorParser.getInstance()
                        .parseDescriptor(settings, ivyFile.toURL(), validate);
                        String resolverName = getSavedResolverName(depMD);
                        String artResolverName = getSavedArtResolverName(depMD);
                        DependencyResolver resolver = settings.getResolver(resolverName);
                        if (resolver == null) {
                            Message.debug("\tresolver not found: " + resolverName
                                + " => trying to use the one configured for " + mrid);
                            resolver = settings.getResolver(depMD.getResolvedModuleRevisionId()
                                .getModuleId());
                            if (resolver != null) {
                                Message.debug("\tconfigured resolver found for "
                                    + depMD.getResolvedModuleRevisionId() + ": "
                                    + resolver.getName() + ": saving this data");
                                saveResolver(depMD, resolver.getName());
                            }
                        }
                        DependencyResolver artResolver = settings.getResolver(artResolverName);
                        if (artResolver == null) {
                            artResolver = resolver;
                        }
                        if (resolver != null) {
                            Message.debug("\tfound ivy file in cache for " + mrid + " (resolved by "
                                + resolver.getName() + "): " + ivyFile);
                            if (expectedResolver != null 
                                    && expectedResolver.equals(resolver.getName())) {
                                return new DefaultModuleRevision(
                                    resolver, artResolver, depMD, false, false);
                            } else {
                                Message.debug(
                                    "found module in cache but with a different resolver: "
                                    + "discarding: " + mrid 
                                    + "; expected resolver=" + expectedResolver 
                                    + "; resolver=" + resolver.getName());
                            }
                        } else {
                            Message.debug("\tresolver not found: " + resolverName
                                + " => cannot use cached ivy file for " + mrid);
                        }
                    } catch (Exception e) {
                        // will try with resolver
                        Message.debug("\tproblem while parsing cached ivy file for: " + mrid + ": "
                            + e.getMessage());
                    }
                } else {
                    Message.debug("\tno ivy file in cache for " + mrid + ": tried " + ivyFile);
                }
            } finally {
                unlockMetadataArtifact(mrid);
            }
        }
        return null;
    }

    public String toString() {
        return "cache: " + String.valueOf(cache);
    }

    public File getRepositoryCacheRoot() {
        return settings.getRepositoryCacheRoot(cache);
    }
    
    public File getResolutionCacheRoot() {
        return settings.getResolutionCacheRoot(cache);
    }

    public LockStrategy getLockStrategy() {
        if (lockStrategy == null) {
            lockStrategy = settings.getDefaultLockStrategy();
        }
        return lockStrategy;
    }
    
    public void setLockStrategy(LockStrategy lockStrategy) {
        this.lockStrategy = lockStrategy;
    }
    
    public ArtifactDownloadReport download(
            Artifact artifact, 
            ArtifactResourceResolver resourceResolver, 
            ResourceDownloader resourceDownloader, 
            CacheDownloadOptions options) {
        final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifact);
        boolean useOrigin = options.isUseOrigin();
        
        // TODO: see if we could lock on the artifact to download only, instead of the module
        // metadata artifact. We'd need to store artifact origin and is local in artifact specific
        // file to do so, or lock the metadata artifact only to update artifact origin, which would
        // mean acquiring nested locks, which can be a dangerous thing
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (!lockMetadataArtifact(mrid)) {
            adr.setDownloadStatus(DownloadStatus.FAILED);
            adr.setDownloadDetails("impossible to get lock for " + mrid);
            return adr;
        }
        try {
            DownloadListener listener = options.getListener();
            if (listener != null) {
                listener.needArtifact(this, artifact);
            }
            ArtifactOrigin origin = getSavedArtifactOrigin(artifact);
            // if we can use origin file, we just ask ivy for the file in cache, and it will
            // return the original one if possible. If we are not in useOrigin mode, we use the
            // getArchivePath method which always return a path in the actual cache
            File archiveFile = getArchiveFileInCache(artifact, origin, useOrigin);

            if (archiveFile.exists() && !options.isForce()) {
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(archiveFile.length());
                adr.setArtifactOrigin(origin);
                adr.setLocalFile(archiveFile);
            } else {
                long start = System.currentTimeMillis();
                try {
                    ResolvedResource artifactRef = resourceResolver.resolve(artifact);
                    if (artifactRef != null) {
                        origin = new ArtifactOrigin(artifactRef.getResource().isLocal(),
                            artifactRef.getResource().getName());
                        if (useOrigin && artifactRef.getResource().isLocal()) {
                            saveArtifactOrigin(artifact, origin);
                            archiveFile = getArchiveFileInCache(artifact,
                                origin);
                            adr.setDownloadStatus(DownloadStatus.NO);
                            adr.setSize(archiveFile.length());
                            adr.setArtifactOrigin(origin);
                            adr.setLocalFile(archiveFile);
                        } else {
                            // refresh archive file now that we better now its origin
                            archiveFile = getArchiveFileInCache(artifact,
                                origin, useOrigin);
                            if (ResourceHelper.equals(artifactRef.getResource(), archiveFile)) {
                                throw new IllegalStateException("invalid settings for '"
                                    + resourceResolver
                                    + "': pointing repository to ivy cache is forbidden !");
                            } 
                            if (listener != null) {
                                listener.startArtifactDownload(this, artifactRef, artifact, origin);
                            }

                            resourceDownloader.download(
                                artifact, artifactRef.getResource(), archiveFile);
                            adr.setSize(archiveFile.length());
                            saveArtifactOrigin(artifact, origin);
                            adr.setDownloadTimeMillis(System.currentTimeMillis() - start);
                            adr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
                            adr.setArtifactOrigin(origin);
                            adr.setLocalFile(archiveFile);
                        }
                    } else {
                        adr.setDownloadStatus(DownloadStatus.FAILED);
                        adr.setDownloadDetails(ArtifactDownloadReport.MISSING_ARTIFACT);
                        adr.setDownloadTimeMillis(System.currentTimeMillis() - start);
                    }
                } catch (Exception ex) {
                    adr.setDownloadStatus(DownloadStatus.FAILED);
                    adr.setDownloadDetails(ex.getMessage());
                    adr.setDownloadTimeMillis(System.currentTimeMillis() - start);
                }
            }
            if (listener != null) {
                listener.endArtifactDownload(this, artifact, adr, archiveFile);
            }
            return adr;
        } finally {
            unlockMetadataArtifact(mrid);
        }
    }
    
    public void originalToCachedModuleDescriptor(
            DependencyResolver resolver, ResolvedResource orginalMetadataRef,
            Artifact requestedMetadataArtifact,
            ModuleDescriptor md, ModuleDescriptorWriter writer) {
        Artifact originalMetadataArtifact = getOriginalMetadataArtifact(requestedMetadataArtifact);
        File mdFileInCache = getIvyFileInCache(md.getResolvedModuleRevisionId());

        ModuleRevisionId mrid = requestedMetadataArtifact.getModuleRevisionId();
        if (!lockMetadataArtifact(mrid)) {
            Message.warn("impossible to acquire lock for: " + mrid);
            return;
        }
        try {
            writer.write(orginalMetadataRef, md, 
                getArchiveFileInCache(originalMetadataArtifact), 
                mdFileInCache);

            saveResolvers(md, resolver.getName(), resolver.getName());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Message.warn("impossible to put metadata file in cache: " 
                + (orginalMetadataRef == null 
                        ? String.valueOf(md.getResolvedModuleRevisionId()) 
                        : String.valueOf(orginalMetadataRef))
                + ". " + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            unlockMetadataArtifact(mrid);
        }
    }

    public ResolvedModuleRevision cacheModuleDescriptor(
            DependencyResolver resolver, final ResolvedResource mdRef, Artifact moduleArtifact, 
            ResourceDownloader downloader, CacheMetadataOptions options) throws ParseException {
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry
            .getInstance().getParser(mdRef.getResource());
        Date cachedPublicationDate = null;
        ArtifactDownloadReport report;
        ModuleRevisionId mrid = moduleArtifact.getModuleRevisionId();
        Artifact originalMetadataArtifact = getOriginalMetadataArtifact(moduleArtifact);
        if (!lockMetadataArtifact(mrid)) {
            Message.error("impossible to acquire lock for " + mrid);
            return null;
        }
        try {
            // now let's see if we can find it in cache and if it is up to date
            ResolvedModuleRevision rmr = findModuleInCache(
                mrid, options.isValidate(), resolver.getName());
            if (rmr != null) {
                if (rmr.getDescriptor().isDefault() && rmr.getResolver() != this) {
                    Message.verbose("\t" + getName() + ": found revision in cache: " + mrid
                        + " (resolved by " + rmr.getResolver().getName()
                        + "): but it's a default one, maybe we can find a better one");
                } else {
                    if (!options.isCheckmodified() && !options.isChanging()) {
                        Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
                        return DefaultModuleRevision.searchedRmr(rmr);
                    }
                    long repLastModified = mdRef.getLastModified();
                    long cacheLastModified = rmr.getDescriptor().getLastModified();
                    if (!rmr.getDescriptor().isDefault() && repLastModified <= cacheLastModified) {
                        Message.verbose("\t" + getName() + ": revision in cache (not updated): "
                            + mrid);
                        return DefaultModuleRevision.searchedRmr(rmr);
                    } else {
                        Message.verbose("\t" + getName() + ": revision in cache is not up to date: "
                            + mrid);
                        if (options.isChanging()) {
                            // ivy file has been updated, we should see if it has a new publication
                            // date to see if a new download is required (in case the dependency is
                            // a changing one)
                            cachedPublicationDate = 
                                rmr.getDescriptor().getResolvedPublicationDate();
                        }
                    }
                }
            }

            // now download module descriptor and parse it
            report = download(
                originalMetadataArtifact, 
                new ArtifactResourceResolver() {
                    public ResolvedResource resolve(Artifact artifact) {
                        return mdRef;
                    }
                }, downloader, 
                new CacheDownloadOptions().setListener(options.getListener()).setForce(true));
            Message.verbose("\t" + report); 

            if (report.getDownloadStatus() == DownloadStatus.FAILED) {
                Message.warn("problem while downloading module descriptor: " + mdRef.getResource() 
                    + ": " + report.getDownloadDetails() 
                    + " (" + report.getDownloadTimeMillis() + "ms)");
                return null;
            }

            URL cachedMDURL = null;
            try {
                cachedMDURL = report.getLocalFile().toURL();
            } catch (MalformedURLException ex) {
                Message.warn("malformed url exception for original in cache file: " 
                    + report.getLocalFile() + ": " + ex.getMessage());
                return null;
            }
            try {
                ModuleDescriptor md = parser.parseDescriptor(
                    settings, cachedMDURL, mdRef.getResource(), options.isValidate());
                Message.debug("\t" + getName() + ": parsed downloaded md file for " + mrid 
                    + "; parsed=" + md.getModuleRevisionId());

                // check if we should delete old artifacts
                boolean deleteOldArtifacts = false;
                if (cachedPublicationDate != null
                        && !cachedPublicationDate.equals(md.getResolvedPublicationDate())) {
                    // artifacts have changed, they should be downloaded again
                    Message.verbose(mrid + " has changed: deleting old artifacts");
                    deleteOldArtifacts = true;
                }
                if (deleteOldArtifacts) {
                    String[] confs = md.getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        Artifact[] arts = md.getArtifacts(confs[i]);
                        for (int j = 0; j < arts.length; j++) {
                            Artifact transformedArtifact = NameSpaceHelper.transform(
                                arts[j], options.getNamespace().getToSystemTransformer());
                            ArtifactOrigin origin = getSavedArtifactOrigin(
                                transformedArtifact);
                            File artFile = getArchiveFileInCache(
                                transformedArtifact, origin, false);
                            if (artFile.exists()) {
                                Message.debug("deleting " + artFile);
                                artFile.delete();
                            }
                            removeSavedArtifactOrigin(transformedArtifact);
                        }
                    }
                } else if (options.isChanging()) {
                    Message.verbose(mrid
                        + " is changing, but has not changed: will trust cached artifacts if any");
                }
                return new DefaultModuleRevision(resolver, resolver, md, true, true);
            } catch (IOException ex) {
                Message.warn("io problem while parsing ivy file: " + mdRef.getResource() + ": "
                    + ex.getMessage());
                return null;
            }
        } finally {
            unlockMetadataArtifact(mrid);
        }
        
    }

    // lock used to lock all metadata related information access
    private boolean lockMetadataArtifact(ModuleRevisionId mrid) {
        Artifact artifact = getDefaultMetadataArtifact(mrid);
        try {
            // we need to provide an artifact origin to be sure we do not end up in a stack overflow
            // if the cache pattern is using original name, and the substitution thus trying to get
            // the saved artifact origin value which in turns calls this method
            return getLockStrategy().lockArtifact(artifact, 
                getArchiveFileInCache(artifact, getDefaultMetadataArtifactOrigin(mrid)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // reset interrupt status 
            throw new RuntimeException("operation interrupted");
        }
    }

    private void unlockMetadataArtifact(ModuleRevisionId mrid) {
        Artifact artifact = getDefaultMetadataArtifact(mrid);
        getLockStrategy().unlockArtifact(artifact, 
            getArchiveFileInCache(artifact, getDefaultMetadataArtifactOrigin(mrid)));
    }
    
    
    private ArtifactOrigin getDefaultMetadataArtifactOrigin(ModuleRevisionId mrid) {
        // it's important to say the origin is not local to make sure it won't ever be used for
        // anything else than original token
        return new ArtifactOrigin(false, getIvyFileInCache(mrid).getPath());
    }
    
    private Artifact getDefaultMetadataArtifact(ModuleRevisionId mrid) {
        return new DefaultArtifact(mrid, new Date(), "metadata", "metadata", "ivy", true);
    }

    // not used any more, but maybe useful for finer grain locking when downloading artifacts
//    private boolean lockArtifact(Artifact artifact) {
//        try {
//            return getLockStrategy().lockArtifact(artifact, 
//                getArchiveFileInCache(artifact, null));
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // reset interrupt status 
//            throw new RuntimeException("operation interrupted");
//        }
//    }
//    
//    private void unlockArtifact(Artifact artifact) {
//        getLockStrategy().unlockArtifact(artifact, getArchiveFileInCache(artifact, null));
//    }
    
    public Artifact getOriginalMetadataArtifact(Artifact moduleArtifact) {
        return DefaultArtifact.cloneWithAnotherName(moduleArtifact, 
            moduleArtifact.getName() + ".original");
    }


    private String getName() {
        return name;
    }
    
}
