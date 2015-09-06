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
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.RelativeUrlResolver;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheDownloadOptions;
import org.apache.ivy.core.cache.CacheMetadataOptions;
import org.apache.ivy.core.cache.DownloadListener;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.NeedArtifactEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.Validatable;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.resolver.ChainResolver.ResolvedModuleRevisionArtifactInfo;
import org.apache.ivy.plugins.resolver.util.HasLatestStrategy;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Message;

/**
 * This abstract resolver only provides handling for resolver name
 */
public abstract class AbstractResolver implements DependencyResolver, HasLatestStrategy,
        Validatable {

    /**
     * True if parsed ivy files should be validated against xsd, false if they should not, null if
     * default behavior should be used
     */
    private Boolean validate = null;

    private String name;

    private ResolverSettings settings;

    private EventManager eventManager = null; // may remain null

    /**
     * The latest strategy to use to find latest among several artifacts
     */
    private LatestStrategy latestStrategy;

    private String latestStrategyName;

    /**
     * The namespace to which this resolver belongs
     */
    private Namespace namespace;

    private String namespaceName;

    private String cacheManagerName;

    private RepositoryCacheManager repositoryCacheManager;

    // used to store default values for nested cache
    private String changingMatcherName;

    private String changingPattern;

    private Boolean checkmodified;

    public ResolverSettings getSettings() {
        return settings;
    }

    public ParserSettings getParserSettings() {
        return new ResolverParserSettings();
    }

    public void setSettings(ResolverSettings ivy) {
        settings = ivy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * this method should remove sensitive information from a location to be displayed in a log
     * 
     * @param name
     *            location
     * @return location with sensitive data replaced by stars
     */
    public String hidePassword(String name) {
        return name;
    }

    protected boolean doValidate(ResolveData data) {
        if (validate != null) {
            return validate.booleanValue();
        } else {
            return data.isValidate();
        }
    }

    public boolean isValidate() {
        return validate == null ? true : validate.booleanValue();
    }

    public void setValidate(boolean validate) {
        this.validate = Boolean.valueOf(validate);
    }

    protected void checkInterrupted() {
        IvyContext.getContext().getIvy().checkInterrupted();
    }

    public void reportFailure() {
        Message.verbose("no failure report implemented by " + getName());
    }

    public void reportFailure(Artifact art) {
        Message.verbose("no failure report implemented by " + getName());
    }

    public String[] listTokenValues(String token, Map<String, String> otherTokenValues) {
        return new String[0];
    }

    public Map<String, String>[] listTokenValues(String[] tokens, Map<String, Object> criteria) {
        return new Map[0];
    }

    public OrganisationEntry[] listOrganisations() {
        return new OrganisationEntry[0];
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        return new ModuleEntry[0];
    }

    public RevisionEntry[] listRevisions(ModuleEntry module) {
        return new RevisionEntry[0];
    }

    @Override
    public String toString() {
        return getName();
    }

    public void dumpSettings() {
        Message.verbose("\t" + getName() + " [" + getTypeName() + "]");
        Message.debug("\t\tcache: " + cacheManagerName);
    }

    public String getTypeName() {
        return getClass().getName();
    }

    /**
     * Default implementation downloads the artifact without taking advantage of its location
     */
    public ArtifactDownloadReport download(ArtifactOrigin artifact, DownloadOptions options) {
        DownloadReport r = download(new Artifact[] {artifact.getArtifact()}, options);
        return r.getArtifactReport(artifact.getArtifact());
    }

    public boolean exists(Artifact artifact) {
        return locate(artifact) != null;
    }

    /**
     * Default implementation actually download the artifact Subclasses should overwrite this to
     * avoid the download
     */
    public ArtifactOrigin locate(Artifact artifact) {
        DownloadReport dr = download(new Artifact[] {artifact}, new DownloadOptions());
        if (dr == null) {
            /*
             * according to IVY-831, it seems that this actually happen sometime, while the contract
             * of DependencyResolver says that it should never return null
             */
            throw new IllegalStateException("null download report returned by " + getName() + " ("
                    + getClass().getName() + ")" + " when trying to download " + artifact);
        }
        ArtifactDownloadReport adr = dr.getArtifactReport(artifact);
        return adr.getDownloadStatus() == DownloadStatus.FAILED ? null : adr.getArtifactOrigin();
    }

    public LatestStrategy getLatestStrategy() {
        if (latestStrategy == null) {
            initLatestStrategyFromSettings();
        }
        return latestStrategy;
    }

    private void initLatestStrategyFromSettings() {
        if (getSettings() != null) {
            if (latestStrategyName != null && !"default".equals(latestStrategyName)) {
                latestStrategy = getSettings().getLatestStrategy(latestStrategyName);
                if (latestStrategy == null) {
                    throw new IllegalStateException("unknown latest strategy '"
                            + latestStrategyName + "'");
                }
            } else {
                latestStrategy = getSettings().getDefaultLatestStrategy();
                Message.debug(getName() + ": no latest strategy defined: using default");
            }
        } else {
            throw new IllegalStateException("no ivy instance found: "
                    + "impossible to get a latest strategy without ivy instance");
        }
    }

    public void setLatestStrategy(LatestStrategy latestStrategy) {
        this.latestStrategy = latestStrategy;
    }

    public void setLatest(String strategyName) {
        latestStrategyName = strategyName;
    }

    public String getLatest() {
        if (latestStrategyName == null) {
            latestStrategyName = "default";
        }
        return latestStrategyName;
    }

    public Namespace getNamespace() {
        if (namespace == null) {
            initNamespaceFromSettings();
        }
        return namespace;
    }

    private void initNamespaceFromSettings() {
        if (getSettings() != null) {
            if (namespaceName != null) {
                namespace = getSettings().getNamespace(namespaceName);
                if (namespace == null) {
                    throw new IllegalStateException("unknown namespace '" + namespaceName + "'");
                }
            } else {
                namespace = getSettings().getSystemNamespace();
                Message.debug(getName() + ": no namespace defined: using system");
            }
        } else {
            Message.verbose(getName()
                    + ": no namespace defined nor ivy instance: using system namespace");
            namespace = Namespace.SYSTEM_NAMESPACE;
        }
    }

    public void setNamespace(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    // Namespace conversion methods
    protected ModuleDescriptor toSystem(ModuleDescriptor md) {
        return NameSpaceHelper.toSystem(md, getNamespace());
    }

    protected Artifact fromSystem(Artifact artifact) {
        return NameSpaceHelper.transform(artifact, getNamespace().getFromSystemTransformer());
    }

    protected Artifact toSystem(Artifact artifact) {
        return NameSpaceHelper.transform(artifact, getNamespace().getToSystemTransformer());
    }

    protected MetadataArtifactDownloadReport toSystem(MetadataArtifactDownloadReport report) {
        return NameSpaceHelper.transform(report, getNamespace().getToSystemTransformer());
    }

    protected ResolvedModuleRevision toSystem(ResolvedModuleRevision rmr) {
        return NameSpaceHelper.toSystem(rmr, getNamespace());
    }

    protected ModuleRevisionId toSystem(ModuleRevisionId resolvedMrid) {
        return getNamespace().getToSystemTransformer().transform(resolvedMrid);
    }

    protected DependencyDescriptor fromSystem(DependencyDescriptor dd) {
        return NameSpaceHelper.transform(dd, getNamespace().getFromSystemTransformer(), true);
    }

    protected DependencyDescriptor toSystem(DependencyDescriptor dd) {
        return NameSpaceHelper.transform(dd, getNamespace().getToSystemTransformer(), true);
    }

    protected IvyNode getSystemNode(ResolveData data, ModuleRevisionId resolvedMrid) {
        return data.getNode(toSystem(resolvedMrid));
    }

    protected ResolvedModuleRevision findModuleInCache(DependencyDescriptor dd, ResolveData data) {
        return findModuleInCache(dd, data, false);
    }

    protected ResolvedModuleRevision findModuleInCache(DependencyDescriptor dd, ResolveData data,
            boolean anyResolver) {
        ResolvedModuleRevision rmr = getRepositoryCacheManager().findModuleInCache(dd,
            dd.getDependencyRevisionId(), getCacheOptions(data), anyResolver ? null : getName());
        if (rmr == null) {
            return null;
        }
        if (data.getReport() != null
                && data.isBlacklisted(data.getReport().getConfiguration(), rmr.getId())) {
            Message.verbose("\t" + getName() + ": found revision in cache: " + rmr.getId()
                    + " for " + dd + ", but it is blacklisted");
            return null;
        }
        return rmr;
    }

    public void setChangingMatcher(String changingMatcherName) {
        this.changingMatcherName = changingMatcherName;
    }

    protected String getChangingMatcherName() {
        return changingMatcherName;
    }

    public void setChangingPattern(String changingPattern) {
        this.changingPattern = changingPattern;
    }

    protected String getChangingPattern() {
        return changingPattern;
    }

    public void setCheckmodified(boolean check) {
        checkmodified = Boolean.valueOf(check);
    }

    public RepositoryCacheManager getRepositoryCacheManager() {
        if (repositoryCacheManager == null) {
            initRepositoryCacheManagerFromSettings();
        }
        return repositoryCacheManager;
    }

    private void initRepositoryCacheManagerFromSettings() {
        if (cacheManagerName == null) {
            repositoryCacheManager = settings.getDefaultRepositoryCacheManager();
            if (repositoryCacheManager == null) {
                throw new IllegalStateException(
                        "no default cache manager defined with current settings");
            }
        } else {
            repositoryCacheManager = settings.getRepositoryCacheManager(cacheManagerName);
            if (repositoryCacheManager == null) {
                throw new IllegalStateException("unknown cache manager '" + cacheManagerName
                        + "'. Available caches are "
                        + Arrays.asList(settings.getRepositoryCacheManagers()));
            }
        }
    }

    public void setRepositoryCacheManager(RepositoryCacheManager repositoryCacheManager) {
        this.cacheManagerName = repositoryCacheManager.getName();
        this.repositoryCacheManager = repositoryCacheManager;
    }

    public void setCache(String cacheName) {
        cacheManagerName = cacheName;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void validate() {
        initRepositoryCacheManagerFromSettings();
        initNamespaceFromSettings();
        initLatestStrategyFromSettings();
    }

    protected CacheMetadataOptions getCacheOptions(ResolveData data) {
        return (CacheMetadataOptions) new CacheMetadataOptions()
                .setChangingMatcherName(getChangingMatcherName())
                .setChangingPattern(getChangingPattern())
                .setCheckTTL(!data.getOptions().isUseCacheOnly())
                .setCheckmodified(
                    data.getOptions().isUseCacheOnly() ? Boolean.FALSE : checkmodified)
                .setValidate(doValidate(data)).setNamespace(getNamespace())
                .setUseCacheOnly(data.getOptions().isUseCacheOnly())
                .setForce(data.getOptions().isRefresh())
                .setListener(getDownloadListener(getDownloadOptions(data.getOptions())));
    }

    protected CacheDownloadOptions getCacheDownloadOptions(DownloadOptions options) {
        CacheDownloadOptions cacheDownloadOptions = new CacheDownloadOptions();
        cacheDownloadOptions.setListener(getDownloadListener(options));
        return cacheDownloadOptions;
    }

    protected DownloadOptions getDownloadOptions(ResolveOptions options) {
        return (DownloadOptions) new DownloadOptions().setLog(options.getLog());
    }

    public void abortPublishTransaction() throws IOException {
        /* Default implementation is a no-op */
    }

    public void commitPublishTransaction() throws IOException {
        /* Default implementation is a no-op */
    }

    public void beginPublishTransaction(ModuleRevisionId module, boolean overwrite)
            throws IOException {
        /* Default implementation is a no-op */
    }

    private DownloadListener getDownloadListener(final DownloadOptions options) {
        return new DownloadListener() {
            public void needArtifact(RepositoryCacheManager cache, Artifact artifact) {
                if (eventManager != null) {
                    eventManager
                            .fireIvyEvent(new NeedArtifactEvent(AbstractResolver.this, artifact));
                }
            }

            public void startArtifactDownload(RepositoryCacheManager cache, ResolvedResource rres,
                    Artifact artifact, ArtifactOrigin origin) {
                if (artifact.isMetadata() || LogOptions.LOG_QUIET.equals(options.getLog())) {
                    Message.verbose("downloading " + rres.getResource() + " ...");
                } else {
                    Message.info("downloading " + rres.getResource() + " ...");
                }
                if (eventManager != null) {
                    eventManager.fireIvyEvent(new StartArtifactDownloadEvent(AbstractResolver.this,
                            artifact, origin));
                }
            }

            public void endArtifactDownload(RepositoryCacheManager cache, Artifact artifact,
                    ArtifactDownloadReport adr, File archiveFile) {
                if (eventManager != null) {
                    eventManager.fireIvyEvent(new EndArtifactDownloadEvent(AbstractResolver.this,
                            artifact, adr, archiveFile));
                }
            }
        };
    }

    /**
     * Returns true if rmr1 is after rmr2, using the latest strategy to determine which is the
     * latest
     * 
     * @param rmr1
     * @param rmr2
     * @return
     */
    protected boolean isAfter(ResolvedModuleRevision rmr1, ResolvedModuleRevision rmr2, Date date) {
        ArtifactInfo[] ais = new ArtifactInfo[] {new ResolvedModuleRevisionArtifactInfo(rmr1),
                new ResolvedModuleRevisionArtifactInfo(rmr2)};
        return getLatestStrategy().findLatest(ais, date) == ais[0];
    }

    protected ResolvedModuleRevision checkLatest(DependencyDescriptor dd,
            ResolvedModuleRevision newModuleFound, ResolveData data) {
        Checks.checkNotNull(dd, "dd");
        Checks.checkNotNull(data, "data");

        // always cache dynamic mrids because we can store per-resolver values
        saveModuleRevisionIfNeeded(dd, newModuleFound);

        // check if latest is asked and compare to return the most recent
        ResolvedModuleRevision previousModuleFound = data.getCurrentResolvedModuleRevision();
        String newModuleDesc = describe(newModuleFound);
        Message.debug("\tchecking " + newModuleDesc + " against " + describe(previousModuleFound));
        if (previousModuleFound == null) {
            Message.debug("\tmodule revision kept as first found: " + newModuleDesc);
            return newModuleFound;
        } else if (isAfter(newModuleFound, previousModuleFound, data.getDate())) {
            Message.debug("\tmodule revision kept as younger: " + newModuleDesc);
            return newModuleFound;
        } else if (!newModuleFound.getDescriptor().isDefault()
                && previousModuleFound.getDescriptor().isDefault()) {
            Message.debug("\tmodule revision kept as better (not default): " + newModuleDesc);
            return newModuleFound;
        } else {
            Message.debug("\tmodule revision discarded as older: " + newModuleDesc);
            return previousModuleFound;
        }
    }

    protected void saveModuleRevisionIfNeeded(DependencyDescriptor dd,
            ResolvedModuleRevision newModuleFound) {
        if (newModuleFound != null
                && getSettings().getVersionMatcher().isDynamic(dd.getDependencyRevisionId())) {
            getRepositoryCacheManager().saveResolvedRevision(getName(),
                dd.getDependencyRevisionId(), newModuleFound.getId().getRevision());
        }
    }

    private String describe(ResolvedModuleRevision rmr) {
        if (rmr == null) {
            return "[none]";
        }
        return rmr.getId() + (rmr.getDescriptor().isDefault() ? "[default]" : "") + " from "
                + rmr.getResolver().getName();
    }

    private class ResolverParserSettings implements ParserSettings {

        public ConflictManager getConflictManager(String name) {
            return AbstractResolver.this.getSettings().getConflictManager(name);
        }

        public Namespace getContextNamespace() {
            return AbstractResolver.this.getNamespace();
        }

        public String getDefaultBranch(ModuleId moduleId) {
            return AbstractResolver.this.getSettings().getDefaultBranch(moduleId);
        }

        public PatternMatcher getMatcher(String matcherName) {
            return AbstractResolver.this.getSettings().getMatcher(matcherName);
        }

        public Namespace getNamespace(String namespace) {
            return AbstractResolver.this.getSettings().getNamespace(namespace);
        }

        public RelativeUrlResolver getRelativeUrlResolver() {
            return AbstractResolver.this.getSettings().getRelativeUrlResolver();
        }

        public ResolutionCacheManager getResolutionCacheManager() {
            return AbstractResolver.this.getSettings().getResolutionCacheManager();
        }

        public DependencyResolver getResolver(ModuleRevisionId mRevId) {
            return AbstractResolver.this.getSettings().getResolver(mRevId);
        }

        public StatusManager getStatusManager() {
            return AbstractResolver.this.getSettings().getStatusManager();
        }

        public File resolveFile(String filename) {
            return AbstractResolver.this.getSettings().resolveFile(filename);
        }

        public Map substitute(Map strings) {
            return AbstractResolver.this.getSettings().substitute(strings);
        }

        public String substitute(String value) {
            return AbstractResolver.this.getSettings().substitute(value);
        }

        public String getVariable(String value) {
            return AbstractResolver.this.getSettings().getVariable(value);
        }

    }
}
