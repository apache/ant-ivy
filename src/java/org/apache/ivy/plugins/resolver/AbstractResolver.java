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
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheDownloadOptions;
import org.apache.ivy.core.cache.CacheMetadataOptions;
import org.apache.ivy.core.cache.DownloadListener;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.NeedArtifactEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.resolver.util.HasLatestStrategy;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;

/**
 * This abstract resolver only provides handling for resolver name
 */
public abstract class AbstractResolver implements DependencyResolver, HasLatestStrategy {

    /**
     * True if parsed ivy files should be validated against xsd, false if they should not, null if
     * default behaviur should be used
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

    public String[] listTokenValues(String token, Map otherTokenValues) {
        return new String[0];
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
     * Default implementation actually download the artifact Subclasses should overwrite this to
     * avoid the download
     */
    public boolean exists(Artifact artifact) {
        DownloadReport dr = download(new Artifact[] {artifact}, new DownloadOptions());
        ArtifactDownloadReport adr = dr.getArtifactReport(artifact);
        return adr.getDownloadStatus() != DownloadStatus.FAILED;
    }

    public LatestStrategy getLatestStrategy() {
        if (latestStrategy == null) {
            if (getSettings() != null) {
                if (latestStrategyName != null && !"default".equals(latestStrategyName)) {
                    latestStrategy = getSettings().getLatestStrategy(latestStrategyName);
                    if (latestStrategy == null) {
                        Message.error("unknown latest strategy: " + latestStrategyName);
                        latestStrategy = getSettings().getDefaultLatestStrategy();
                    }
                } else {
                    latestStrategy = getSettings().getDefaultLatestStrategy();
                    Message.debug(getName() + ": no latest strategy defined: using default");
                }
            } else {
                throw new IllegalStateException(
                    "no ivy instance found: "
                    + "impossible to get a latest strategy without ivy instance");
            }
        }
        return latestStrategy;
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
            if (getSettings() != null) {
                if (namespaceName != null) {
                    namespace = getSettings().getNamespace(namespaceName);
                    if (namespace == null) {
                        Message.error("unknown namespace: " + namespaceName);
                        namespace = getSettings().getSystemNamespace();
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
        return namespace;
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

    protected ResolvedModuleRevision findModuleInCache(
            DependencyDescriptor dd, CacheMetadataOptions options) {
        return findModuleInCache(dd, options, false);
    }

    protected ResolvedModuleRevision findModuleInCache(
            DependencyDescriptor dd, CacheMetadataOptions options, boolean anyResolver) {
        return getRepositoryCacheManager().findModuleInCache(
            dd, options, anyResolver ? null : getName());
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
            if (cacheManagerName == null) {
                repositoryCacheManager = settings.getDefaultRepositoryCacheManager();
            } else {
                repositoryCacheManager = settings.getRepositoryCacheManager(cacheManagerName);
            }
        }
        return repositoryCacheManager;
    }
    
    public void setRepositoryCacheManager(RepositoryCacheManager repositoryCacheManager) {
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

    protected CacheMetadataOptions getCacheOptions(ResolveData data) {
        return (CacheMetadataOptions) new CacheMetadataOptions()
            .setChangingMatcherName(getChangingMatcherName())
            .setChangingPattern(getChangingPattern())
            .setCheckmodified(checkmodified)
            .setValidate(doValidate(data))
            .setNamespace(getNamespace())
            .setForce(data.getOptions().isRefresh())
            .setListener(getDownloadListener(getDownloadOptions(data.getOptions())));
    }

    protected CacheDownloadOptions getCacheDownloadOptions(DownloadOptions options) {
        return new CacheDownloadOptions().setListener(getDownloadListener(options));
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

    public void beginPublishTransaction(
            ModuleRevisionId module, boolean overwrite) throws IOException {
        /* Default implementation is a no-op */
    }

    private DownloadListener getDownloadListener(final DownloadOptions options) {
        return new DownloadListener() {
            public void needArtifact(RepositoryCacheManager cache, Artifact artifact) {
                if (eventManager != null) {
                    eventManager.fireIvyEvent(
                        new NeedArtifactEvent(AbstractResolver.this, artifact));
                }
            }
            public void startArtifactDownload(
                    RepositoryCacheManager cache, ResolvedResource rres, 
                    Artifact artifact, ArtifactOrigin origin) {
                if (artifact.isMetadata() || LogOptions.LOG_QUIET.equals(options.getLog())) {
                    Message.verbose("downloading " + rres.getResource() + " ...");
                } else {
                    Message.info("downloading " + rres.getResource() + " ...");
                }
                if (eventManager != null) {
                    eventManager.fireIvyEvent(
                        new StartArtifactDownloadEvent(
                            AbstractResolver.this, artifact, origin));
                }            
            }
            public void endArtifactDownload(
                    RepositoryCacheManager cache, Artifact artifact, 
                    ArtifactDownloadReport adr, File archiveFile) {
                if (eventManager != null) {
                    eventManager.fireIvyEvent(
                        new EndArtifactDownloadEvent(
                            AbstractResolver.this, artifact, adr, archiveFile));
                }
            }
        };
    }

}
