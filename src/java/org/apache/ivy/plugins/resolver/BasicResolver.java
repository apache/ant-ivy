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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheDownloadOptions;
import org.apache.ivy.core.cache.DownloadListener;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.NeedArtifactEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DefaultModuleRevision;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.util.MDResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.util.ChecksumHelper;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;

/**
 *
 */
public abstract class BasicResolver extends AbstractResolver {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private String workspaceName;

    /**
     * True if the files resolved are dependent of the environment from which they have been
     * resolved, false otherwise. In general, relative paths are dependent of the environment, and
     * absolute paths including machine reference are not.
     */
    private boolean envDependent = true;

    private List ivyattempts = new ArrayList();

    private Map artattempts = new HashMap();

    private Boolean checkmodified = null;

    private boolean checkconsistency = true;

    private boolean allownomd = true;

    private String checksums = null;
    
    private EventManager eventManager = null; // may remain null

    private URLRepository extartifactrep = new URLRepository(); // used only to download

    // external artifacts

    public BasicResolver() {
        workspaceName = HostUtil.getLocalHostName();
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public boolean isEnvDependent() {
        return envDependent;
    }

    public void setEnvDependent(boolean envDependent) {
        this.envDependent = envDependent;
    }
    
    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * True if this resolver should check lastmodified date to know if ivy files are up to date.
     * 
     * @return
     */
    public boolean isCheckmodified() {
        if (checkmodified == null) {
            if (getSettings() != null) {
                String check = getSettings().getVariable("ivy.resolver.default.check.modified");
                return check != null ? Boolean.valueOf(check).booleanValue() : false;
            } else {
                return false;
            }
        } else {
            return checkmodified.booleanValue();
        }
    }

    public void setCheckmodified(boolean check) {
        checkmodified = Boolean.valueOf(check);
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        IvyContext context = IvyContext.pushNewCopyContext();
        context.setDependencyDescriptor(dd);
        context.setResolveData(data);
        try {
            DependencyDescriptor systemDd = dd;
            dd = fromSystem(dd);

            clearIvyAttempts();
            clearArtifactAttempts();
            boolean downloaded = false;
            boolean searched = false;
            ModuleRevisionId mrid = dd.getDependencyRevisionId();
            // check revision
            int index = mrid.getRevision().indexOf("@");
            if (index != -1 && !mrid.getRevision().substring(index + 1).equals(workspaceName)) {
                Message.verbose("\t" + getName() + ": unhandled revision => " + mrid.getRevision());
                return null;
            }

            boolean isDynamic = getSettings().getVersionMatcher().isDynamic(mrid);
            if (isDynamic && !acceptLatest()) {
                Message.error("dynamic revisions not handled by " + getClass().getName()
                    + ". impossible to resolve " + mrid);
                return null;
            }

            boolean isChangingRevision = getChangingMatcher().matches(mrid.getRevision());
            boolean isChangingDependency = isChangingRevision || dd.isChanging();

            // if we do not have to check modified and if the revision is exact and not changing,
            // we first search for it in cache
            ResolvedModuleRevision cachedRmr = null;
            boolean checkedCache = false;
            if (!isDynamic && !isCheckmodified() && !isChangingDependency) {
                cachedRmr = findModuleInCache(data, mrid);
                checkedCache = true;
                if (cachedRmr != null) {
                    if (cachedRmr.getDescriptor().isDefault() && cachedRmr.getResolver() != this) {
                        Message.verbose("\t" + getName() + ": found revision in cache: " + mrid
                            + " (resolved by " + cachedRmr.getResolver().getName()
                            + "): but it's a default one, maybe we can find a better one");
                    } else {
                        Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
                        return toSystem(cachedRmr);
                    }
                }
            }
            checkInterrupted();
            ResolvedResource ivyRef = findIvyFileRef(dd, data);
            checkInterrupted();
            searched = true;

            // get module descriptor
            ModuleDescriptorParser parser;
            ModuleDescriptor md;
            ModuleDescriptor systemMd = null;
            if (ivyRef == null) {
                if (!isAllownomd()) {
                    Message.verbose("\t" + getName() + ": no ivy file found for " + mrid);
                    return null;
                }
                parser = XmlModuleDescriptorParser.getInstance();
                md = DefaultModuleDescriptor.newDefaultInstance(mrid, dd
                    .getAllDependencyArtifacts());
                ResolvedResource artifactRef = findFirstArtifactRef(md, dd, data);
                checkInterrupted();
                if (artifactRef == null) {
                    Message.verbose("\t" + getName() + ": no ivy file nor artifact found for "
                        + mrid);
                    if (!checkedCache) {
                        cachedRmr = findModuleInCache(data, mrid);
                    }
                    if (cachedRmr != null) {
                        Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
                        return toSystem(cachedRmr);
                    }
                    return null;
                } else {
                    long lastModified = artifactRef.getLastModified();
                    if (lastModified != 0 && md instanceof DefaultModuleDescriptor) {
                        ((DefaultModuleDescriptor) md).setLastModified(lastModified);
                    }
                    Message.verbose("\t" + getName() + ": no ivy file found for " + mrid
                        + ": using default data");
                    if (isDynamic) {
                        md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(mrid,
                            artifactRef.getRevision()));
                    }
                }
            } else {
                ResolvedModuleRevision rmr = null;
                if (ivyRef instanceof MDResolvedResource) {
                    rmr = ((MDResolvedResource) ivyRef).getResolvedModuleRevision();
                }
                if (rmr == null) {
                    rmr = parse(ivyRef, dd, data);
                    if (rmr == null) {
                        return null;
                    }
                }
                if (!rmr.isDownloaded()) {
                    return toSystem(rmr);
                } else {
                    md = rmr.getDescriptor();
                    parser = ModuleDescriptorParserRegistry.getInstance().getParser(
                        ivyRef.getResource());

                    // check descriptor data is in sync with resource revision and names
                    systemMd = toSystem(md);
                    if (checkconsistency) {
                        checkDescriptorConsistency(mrid, md, ivyRef);
                        checkDescriptorConsistency(systemDd.getDependencyRevisionId(), systemMd,
                            ivyRef);
                    } else {
                        if (md instanceof DefaultModuleDescriptor) {
                            String revision = getRevision(ivyRef, mrid, md);
                            ((DefaultModuleDescriptor) md).setModuleRevisionId(ModuleRevisionId
                                .newInstance(mrid, revision));
                        } else {
                            Message.warn(
                                "consistency disabled with instance of non DefaultModuleDescriptor..."
                                + " module info can't be updated, so consistency check will be done");
                            checkDescriptorConsistency(mrid, md, ivyRef);
                            checkDescriptorConsistency(systemDd.getDependencyRevisionId(),
                                systemMd, ivyRef);
                        }
                    }
                }
            }

            if (systemMd == null) {
                systemMd = toSystem(md);
            }

            // resolve revision
            ModuleRevisionId resolvedMrid = mrid;
            if (isDynamic) {
                resolvedMrid = md.getResolvedModuleRevisionId();
                if (resolvedMrid.getRevision() == null 
                        || resolvedMrid.getRevision().length() == 0) {
                    if (ivyRef.getRevision() == null || ivyRef.getRevision().length() == 0) {
                        resolvedMrid = ModuleRevisionId.newInstance(resolvedMrid, "working@"
                            + getName());
                    } else {
                        resolvedMrid = ModuleRevisionId.newInstance(resolvedMrid, ivyRef
                            .getRevision());
                    }
                }
                Message.verbose("\t\t[" + resolvedMrid.getRevision() + "] " + mrid.getModuleId());
            }
            md.setResolvedModuleRevisionId(resolvedMrid);
            systemMd.setResolvedModuleRevisionId(toSystem(resolvedMrid)); // keep system md in
            // sync with md

            // check module descriptor revision
            if (!getSettings().getVersionMatcher().accept(mrid, md)) {
                Message.info("\t" + getName() + ": unacceptable revision => was="
                    + md.getModuleRevisionId().getRevision() + " required="
                    + mrid.getRevision());
                return null;
            }

            // resolve and check publication date
            if (data.getDate() != null) {
                long pubDate = getPublicationDate(md, dd, data);
                if (pubDate > data.getDate().getTime()) {
                    Message.info("\t" + getName() + ": unacceptable publication date => was="
                        + new Date(pubDate) + " required=" + data.getDate());
                    return null;
                } else if (pubDate == -1) {
                    Message.info("\t" + getName()
                        + ": impossible to guess publication date: artifact missing for "
                        + mrid);
                    return null;
                }
                md.setResolvedPublicationDate(new Date(pubDate));
                systemMd.setResolvedPublicationDate(new Date(pubDate)); // keep system md in sync
                // with md
            }
            
            if (!md.isDefault() 
                    && data.getSettings().logNotConvertedExclusionRule() 
                    && md instanceof DefaultModuleDescriptor) {
                DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) md;
                if (dmd.isNamespaceUseful()) {
                    Message.warn(
                        "the module descriptor "
                        + ivyRef.getResource()
                        + " has information which can't be converted into "
                        + "the system namespace. "
                        + "It will require the availability of the namespace '"
                        + getNamespace().getName() + "' to be fully usable.");
                }
            }

            RepositoryCacheManager cacheManager = data.getCacheManager();
            // TODO: this should be done with a lock on the metadata artifact (moved to the cache manager)
           try {
                File ivyFile = cacheManager.getIvyFileInCache(
                    systemMd.getResolvedModuleRevisionId());
                if (ivyRef == null) {
                    // a basic ivy file is written containing default data
                    XmlModuleDescriptorWriter.write(systemMd, ivyFile);
                } else {
                    // copy and update ivy file from source to cache
                    parser.toIvyFile(
                        new FileInputStream(
                            cacheManager.getArchiveFileInCache(
                                getOriginalMetadataArtifact(
                                    parser.getMetadataArtifact(
                                        resolvedMrid, ivyRef.getResource())))), 
                        ivyRef.getResource(), ivyFile,
                        systemMd);
                    long repLastModified = ivyRef.getLastModified();
                    if (repLastModified > 0) {
                        ivyFile.setLastModified(repLastModified);
                    }
                }
            } catch (Exception e) {
                if (ivyRef == null) {
                    Message.warn("impossible to create ivy file in cache for module : "
                        + resolvedMrid);
                } else {
                    Message.warn("impossible to copy ivy file to cache: " + ivyRef.getResource()
                        + ". " + e.getClass().getName() + ": " + e.getMessage());
                }
            }

            cacheManager.saveResolver(systemMd, getName());
            cacheManager.saveArtResolver(systemMd, getName());
            
            
            return new DefaultModuleRevision(this, this, systemMd, searched, downloaded);
        } finally {
            IvyContext.popContext();
        }
    }

    private String getRevision(ResolvedResource ivyRef, ModuleRevisionId askedMrid,
            ModuleDescriptor md) throws ParseException {
        String revision = ivyRef.getRevision();
        if (revision == null) {
            Message.debug("no revision found in reference for " + askedMrid);
            if (getSettings().getVersionMatcher().isDynamic(askedMrid)) {
                if (md.getModuleRevisionId().getRevision() == null) {
                    return "working@" + getName();
                } else {
                    Message.debug("using  " + askedMrid);
                    revision = md.getModuleRevisionId().getRevision();
                }
            } else {
                Message.debug("using  " + askedMrid);
                revision = askedMrid.getRevision();
            }
        }
        return revision;
    }

    public ResolvedModuleRevision parse(final ResolvedResource mdRef, DependencyDescriptor dd,
            ResolveData data) throws ParseException {

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry.getInstance().getParser(
            mdRef.getResource());
        if (parser == null) {
            Message.warn("no module descriptor parser available for " + mdRef.getResource());
            return null;
        }
        Message.verbose("\t" + getName() + ": found md file for " + mrid);
        Message.verbose("\t\t=> " + mdRef);
        Message.debug("\tparser = " + parser);

        boolean isChangingRevision = getChangingMatcher().matches(mrid.getRevision());
        boolean isChangingDependency = isChangingRevision || dd.isChanging();
        Date cachedPublicationDate = null;
        ModuleRevisionId resolvedMrid = mrid;

        // first check if this dependency has not yet been resolved
        if (getSettings().getVersionMatcher().isDynamic(mrid)) {
            resolvedMrid = ModuleRevisionId.newInstance(mrid, mdRef.getRevision());
            IvyNode node = getSystemNode(data, resolvedMrid);
            if (node != null && node.getModuleRevision() != null) {
                // this revision has already be resolved : return it
                if (node.getDescriptor() != null && node.getDescriptor().isDefault()) {
                    Message.verbose("\t" + getName() + ": found already resolved revision: "
                            + resolvedMrid
                            + ": but it's a default one, maybe we can find a better one");
                } else {
                    Message.verbose("\t" + getName() + ": revision already resolved: "
                            + resolvedMrid);
                    return searchedRmr(node.getModuleRevision());
                }
            }
        }

        // TODO: this should be done while the lock on the original artifact is acquired (in cache manager #download)
        // now let's see if we can find it in cache and if it is up to date
        ResolvedModuleRevision rmr = findModuleInCache(data, resolvedMrid);
        if (rmr != null) {
            if (rmr.getDescriptor().isDefault() && rmr.getResolver() != this) {
                Message.verbose("\t" + getName() + ": found revision in cache: " + mrid
                        + " (resolved by " + rmr.getResolver().getName()
                        + "): but it's a default one, maybe we can find a better one");
            } else {
                if (!isCheckmodified() && !isChangingDependency) {
                    Message.verbose("\t" + getName() + ": revision in cache: " + mrid);
                    return searchedRmr(rmr);
                }
                long repLastModified = mdRef.getLastModified();
                long cacheLastModified = rmr.getDescriptor().getLastModified();
                if (!rmr.getDescriptor().isDefault() && repLastModified <= cacheLastModified) {
                    Message.verbose("\t" + getName() + ": revision in cache (not updated): "
                            + resolvedMrid);
                    return searchedRmr(rmr);
                } else {
                    Message.verbose("\t" + getName() + ": revision in cache is not up to date: "
                            + resolvedMrid);
                    if (isChangingDependency) {
                        // ivy file has been updated, we should see if it has a new publication date
                        // to see if a new download is required (in case the dependency is a
                        // changing one)
                        cachedPublicationDate = rmr.getDescriptor().getResolvedPublicationDate();
                    }
                }
            }
        }

        // now download module descriptor and parse it
        Artifact moduleArtifact = parser.getMetadataArtifact(resolvedMrid, mdRef.getResource());
        ArtifactDownloadReport report = data.getCacheManager().download(
            getOriginalMetadataArtifact(moduleArtifact), 
            new ArtifactResourceResolver() {
                public ResolvedResource resolve(Artifact artifact) {
                    return mdRef;
                }
            }, downloader, 
            new CacheDownloadOptions().setListener(downloadListener).setForce(true));
        Message.verbose("\t" + report);            

        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            Message.warn("problem while downloading module descriptor: " + mdRef.getResource() 
                + ": " + report.getDownloadDetails() 
                + " (" + report.getDownloadTimeMillis() + "ms)");
            return null;
        }

        File originalMDCacheFile = data.getCacheManager().getArchiveFileInCache(
            report.getArtifact(), report.getArtifactOrigin(), false);
        URL cachedMDURL = null;
        try {
            cachedMDURL = originalMDCacheFile.toURL();
        } catch (MalformedURLException ex) {
            Message.warn("malformed url exception for original in cache file: " 
                + originalMDCacheFile + ": " + ex.getMessage());
            return null;
        }
        try {
            ModuleDescriptor md = parser.parseDescriptor(
                data.getSettings(), cachedMDURL, mdRef.getResource(), doValidate(data));
            Message.debug("\t" + getName() + ": parsed downloaded md file for " + mrid + " parsed="
                    + md.getModuleRevisionId());

            // check if we should delete old artifacts
            boolean deleteOldArtifacts = false;
            if (cachedPublicationDate != null
                    && !cachedPublicationDate.equals(md.getResolvedPublicationDate())) {
                // artifacts have changed, they should be downloaded again
                Message.verbose(dd + " has changed: deleting old artifacts");
                deleteOldArtifacts = true;
            }
            if (deleteOldArtifacts) {
                String[] confs = rmr.getDescriptor().getConfigurationsNames();
                for (int i = 0; i < confs.length; i++) {
                    Artifact[] arts = rmr.getDescriptor().getArtifacts(confs[i]);
                    for (int j = 0; j < arts.length; j++) {
                        Artifact transformedArtifact = toSystem(arts[j]);
                        ArtifactOrigin origin = data.getCacheManager().getSavedArtifactOrigin(
                            transformedArtifact);
                        File artFile = data.getCacheManager().getArchiveFileInCache(
                            transformedArtifact, origin, false);
                        if (artFile.exists()) {
                            Message.debug("deleting " + artFile);
                            artFile.delete();
                        }
                        data.getCacheManager().removeSavedArtifactOrigin(transformedArtifact);
                    }
                }
            } else if (isChangingDependency) {
                Message.verbose(dd
                        + " is changing, but has not changed: will trust cached artifacts if any");
            }
            return new DefaultModuleRevision(this, this, md, true, true);
        } catch (IOException ex) {
            Message.warn("io problem while parsing ivy file: " + mdRef.getResource() + ": "
                    + ex.getMessage());
            return null;
        }

    }

    private Artifact getOriginalMetadataArtifact(Artifact moduleArtifact) {
        return DefaultArtifact.cloneWithAnotherName(moduleArtifact, 
            moduleArtifact.getName() + ".original");
    }

    protected ResourceMDParser getRMDParser(final DependencyDescriptor dd, final ResolveData data) {
        return new ResourceMDParser() {
            public MDResolvedResource parse(Resource resource, String rev) {
                try {
                    ResolvedModuleRevision rmr = BasicResolver.this.parse(new ResolvedResource(
                            resource, rev), dd, data);
                    if (rmr == null) {
                        return null;
                    } else {
                        return new MDResolvedResource(resource, rev, rmr);
                    }
                } catch (ParseException e) {
                    return null;
                }
            }

        };
    }

    protected ResourceMDParser getDefaultRMDParser(final ModuleId mid) {
        return new ResourceMDParser() {
            public MDResolvedResource parse(Resource resource, String rev) {
                return new MDResolvedResource(resource, rev, new DefaultModuleRevision(
                        BasicResolver.this, BasicResolver.this, DefaultModuleDescriptor
                                .newDefaultInstance(new ModuleRevisionId(mid, rev)), false, false));
            }
        };
    }

    // private boolean isResolved(ResolveData data, ModuleRevisionId mrid) {
    // IvyNode node = getSystemNode(data, mrid);
    // return node != null && node.getModuleRevision() != null;
    // }
    //
    private void checkDescriptorConsistency(ModuleRevisionId mrid, ModuleDescriptor md,
            ResolvedResource ivyRef) throws ParseException {
        boolean ok = true;
        StringBuffer errors = new StringBuffer();
        if (!mrid.getOrganisation().equals(md.getModuleRevisionId().getOrganisation())) {
            Message.error("\t" + getName() + ": bad organisation found in " + ivyRef.getResource()
                    + ": expected='" + mrid.getOrganisation() + "' found='"
                    + md.getModuleRevisionId().getOrganisation() + "'");
            errors.append("bad organisation: expected='" + mrid.getOrganisation() + "' found='"
                    + md.getModuleRevisionId().getOrganisation() + "'; ");
            ok = false;
        }
        if (!mrid.getName().equals(md.getModuleRevisionId().getName())) {
            Message.error("\t" + getName() + ": bad module name found in " + ivyRef.getResource()
                    + ": expected='" + mrid.getName() + " found='"
                    + md.getModuleRevisionId().getName() + "'");
            errors.append("bad module name: expected='" + mrid.getName() + "' found='"
                    + md.getModuleRevisionId().getName() + "'; ");
            ok = false;
        }
        if (ivyRef.getRevision() != null && !ivyRef.getRevision().startsWith("working@")) {
            ModuleRevisionId expectedMrid = ModuleRevisionId
                    .newInstance(mrid, ivyRef.getRevision());
            if (!getSettings().getVersionMatcher().accept(expectedMrid, md)) {
                Message.error("\t" + getName() + ": bad revision found in " + ivyRef.getResource()
                        + ": expected='" + ivyRef.getRevision() + " found='"
                        + md.getModuleRevisionId().getRevision() + "'");
                errors.append("bad revision: expected='" + ivyRef.getRevision() + "' found='"
                        + md.getModuleRevisionId().getRevision() + "'; ");
                ok = false;
            }
        }
        if (!getSettings().getStatusManager().isStatus(md.getStatus())) {
            Message.error("\t" + getName() + ": bad status found in " + ivyRef.getResource()
                    + ": '" + md.getStatus() + "'");
            errors.append("bad status: '" + md.getStatus() + "'; ");
            ok = false;
        }
        if (!ok) {
            throw new ParseException("inconsistent module descriptor file found in '"
                    + ivyRef.getResource() + "': " + errors, 0);
        }
    }

    protected void clearIvyAttempts() {
        ivyattempts.clear();
        clearArtifactAttempts();
    }

    protected ResolvedModuleRevision searchedRmr(final ResolvedModuleRevision rmr) {
        // delegate all to previously found except isSearched
        return new ResolvedModuleRevision() {
            public boolean isSearched() {
                return true;
            }

            public boolean isDownloaded() {
                return rmr.isDownloaded();
            }

            public ModuleDescriptor getDescriptor() {
                return rmr.getDescriptor();
            }

            public Date getPublicationDate() {
                return rmr.getPublicationDate();
            }

            public ModuleRevisionId getId() {
                return rmr.getId();
            }

            public DependencyResolver getResolver() {
                return rmr.getResolver();
            }

            public DependencyResolver getArtifactResolver() {
                return rmr.getArtifactResolver();
            }
        };
    }

    protected void logIvyAttempt(String attempt) {
        ivyattempts.add(attempt);
        Message.verbose("\t\ttried " + attempt);
    }

    protected void logArtifactAttempt(Artifact art, String attempt) {
        List attempts = (List) artattempts.get(art);
        if (attempts == null) {
            attempts = new ArrayList();
            artattempts.put(art, attempts);
        }
        attempts.add(attempt);
        Message.verbose("\t\ttried " + attempt);
    }

    protected void logAttempt(String attempt) {
        Artifact currentArtifact = (Artifact) IvyContext.getContext().get(getName() + ".artifact");
        if (currentArtifact != null) {
            logArtifactAttempt(currentArtifact, attempt);
        } else {
            logIvyAttempt(attempt);
        }
    }

    public void reportFailure() {
        Message.warn("==== " + getName() + ": tried");
        for (ListIterator iter = ivyattempts.listIterator(); iter.hasNext();) {
            String m = (String) iter.next();
            Message.warn("  " + m);
        }
        for (Iterator iter = artattempts.keySet().iterator(); iter.hasNext();) {
            Artifact art = (Artifact) iter.next();
            List attempts = (List) artattempts.get(art);
            if (attempts != null) {
                Message.warn("  -- artifact " + art + ":");
                for (ListIterator iterator = attempts.listIterator(); iterator.hasNext();) {
                    String m = (String) iterator.next();
                    Message.warn("  " + m);
                }
            }
        }
    }

    public void reportFailure(Artifact art) {
        Message.warn("==== " + getName() + ": tried");
        List attempts = (List) artattempts.get(art);
        if (attempts != null) {
            for (ListIterator iter = attempts.listIterator(); iter.hasNext();) {
                String m = (String) iter.next();
                Message.warn("  " + m);
            }
        }
    }

    protected boolean acceptLatest() {
        return true;
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        RepositoryCacheManager cacheManager = options.getCacheManager();

        clearArtifactAttempts();
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactDownloadReport adr = cacheManager.download(
                artifacts[i], artifactResourceResolver, downloader, 
                new CacheDownloadOptions().setListener(downloadListener)
                    .setUseOrigin(options.isUseOrigin()));
            Message.info("\t" + adr);
            dr.addArtifactReport(adr);
            checkInterrupted();
        }
        return dr;
    }

    protected void clearArtifactAttempts() {
        artattempts.clear();
    }

    public boolean exists(Artifact artifact) {
        ResolvedResource artifactRef = getArtifactRef(artifact, null);
        if (artifactRef != null) {
            return artifactRef.getResource().exists();
        }
        return false;
    }

    protected long getPublicationDate(ModuleDescriptor md, DependencyDescriptor dd, 
            ResolveData data) {
        if (md.getPublicationDate() != null) {
            return md.getPublicationDate().getTime();
        }
        ResolvedResource artifactRef = findFirstArtifactRef(md, dd, data);
        if (artifactRef != null) {
            return artifactRef.getLastModified();
        }
        return -1;
    }

    public String toString() {
        return getName();
    }

    public String[] listTokenValues(String token, Map otherTokenValues) {
        Collection ret = findNames(otherTokenValues, token);
        return (String[]) ret.toArray(new String[ret.size()]);
    }

    public OrganisationEntry[] listOrganisations() {
        Collection names = findNames(Collections.EMPTY_MAP, IvyPatternHelper.ORGANISATION_KEY);
        OrganisationEntry[] ret = new OrganisationEntry[names.size()];
        int i = 0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String org = (String) iter.next();
            ret[i] = new OrganisationEntry(this, org);
        }
        return ret;
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection names = findNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i = 0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String) iter.next();
            ret[i] = new ModuleEntry(org, name);
        }
        return ret;
    }

    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, mod.getOrganisation());
        tokenValues.put(IvyPatternHelper.MODULE_KEY, mod.getModule());
        Collection names = findNames(tokenValues, IvyPatternHelper.REVISION_KEY);
        RevisionEntry[] ret = new RevisionEntry[names.size()];
        int i = 0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String) iter.next();
            ret[i] = new RevisionEntry(mod, name);
        }
        return ret;
    }

    protected abstract Collection findNames(Map tokenValues, String token);

    protected ResolvedResource findFirstArtifactRef(ModuleDescriptor md, DependencyDescriptor dd,
            ResolveData data) {
        ResolvedResource ret = null;
        String[] conf = md.getConfigurationsNames();
        for (int i = 0; i < conf.length; i++) {
            Artifact[] artifacts = md.getArtifacts(conf[i]);
            for (int j = 0; j < artifacts.length; j++) {
                ret = getArtifactRef(artifacts[j], data.getDate());
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    protected long getAndCheck(Resource resource, File dest) throws IOException {
        long size = get(resource, dest);
        String[] checksums = getChecksumAlgorithms();
        boolean checked = false;
        for (int i = 0; i < checksums.length && !checked; i++) {
            checked = check(resource, dest, checksums[i]);
        }
        return size;
    }

    /**
     * Checks the given resource checksum if a checksum resource exists.
     * 
     * @param resource
     *            the resource to check
     * @param dest
     *            the file where the resource has been downloaded
     * @param algorithm
     *            the checksum algorithm to use
     * @return true if the checksum has been successfully checked, false if the checksum wasn't
     *         available
     * @throws IOException
     *             if a checksum exist but do not match the downloaded file checksum
     */
    private boolean check(Resource resource, File dest, String algorithm) throws IOException {
        Resource csRes = resource.clone(resource.getName() + "." + algorithm);
        if (csRes.exists()) {
            Message.debug(algorithm + " file found for " + resource + ": checking...");
            File csFile = File.createTempFile("ivytmp", algorithm);
            try {
                get(csRes, csFile);
                try {
                    ChecksumHelper.check(dest, csFile, algorithm);
                    Message.verbose(algorithm + " OK for " + resource);
                    return true;
                } catch (IOException ex) {
                    dest.delete();
                    throw ex;
                }
            } finally {
                csFile.delete();
            }
        } else {
            return false;
        }
    }

    protected ResolvedResource getArtifactRef(Artifact artifact, Date date) {
        IvyContext.getContext().set(getName() + ".artifact", artifact);
        try {
            ResolvedResource ret = findArtifactRef(artifact, date);
            if (ret == null && artifact.getUrl() != null) {
                URL url = artifact.getUrl();
                Message.verbose("\tusing url for " + artifact + ": " + url);
                logArtifactAttempt(artifact, url.toExternalForm());
                ret = new ResolvedResource(new URLResource(url), artifact.getModuleRevisionId()
                        .getRevision());
            }
            return ret;
        } finally {
            IvyContext.getContext().set(getName() + ".artifact", null);
        }
    }

    protected abstract ResolvedResource findArtifactRef(Artifact artifact, Date date);

    protected abstract long get(Resource resource, File dest) throws IOException;

    public boolean isCheckconsistency() {
        return checkconsistency;
    }

    public void setCheckconsistency(boolean checkConsitency) {
        checkconsistency = checkConsitency;
    }

    public boolean isAllownomd() {
        return allownomd;
    }

    public void setAllownomd(boolean b) {
        allownomd = b;
    }

    public String[] getChecksumAlgorithms() {
        String csDef = checksums == null ? getSettings().getVariable("ivy.checksums") : checksums;
        if (csDef == null) {
            return new String[0];
        }
        // csDef is a comma separated list of checksum algorithms to use with this resolver
        // we parse and return it as a String[]
        String[] checksums = csDef.split(",");
        List algos = new ArrayList();
        for (int i = 0; i < checksums.length; i++) {
            String cs = checksums[i].trim();
            if (!"".equals(cs) && !"none".equals(cs)) {
                algos.add(cs);
            }
        }
        return (String[]) algos.toArray(new String[algos.size()]);
    }

    public void setChecksums(String checksums) {
        this.checksums = checksums;
    }
    
    private final ArtifactResourceResolver artifactResourceResolver 
                                        = new ArtifactResourceResolver() {
        public ResolvedResource resolve(Artifact artifact) {
            artifact = fromSystem(artifact);
            return getArtifactRef(artifact, null);
        }
    };

    private final ResourceDownloader downloader = new ResourceDownloader() {
        public void download(Artifact artifact, Resource resource, File dest) throws IOException {
            if (dest.exists()) {
                dest.delete();
            }
            File part = new File(dest.getAbsolutePath() + ".part");
            if (resource.getName().equals(
                String.valueOf(artifact.getUrl()))) {
                if (part.getParentFile() != null) {
                    part.getParentFile().mkdirs();
                }
                extartifactrep.get(resource.getName(), part);
            } else {
                getAndCheck(resource, part);
            }
            if (!part.renameTo(dest)) {
                throw new IOException(
                    "impossible to move part file to definitive one: " 
                    + part + " -> " + dest);
            }
            
        }
    };

    private final DownloadListener downloadListener = new DownloadListener() {
        public void needArtifact(RepositoryCacheManager cache, Artifact artifact) {
            if (eventManager != null) {
                eventManager.fireIvyEvent(new NeedArtifactEvent(BasicResolver.this, artifact));
            }
        }
        public void startArtifactDownload(
                RepositoryCacheManager cache, ResolvedResource rres, 
                Artifact artifact, ArtifactOrigin origin) {
            if (artifact.isMetadata()) {
                Message.verbose("downloading " + rres.getResource() + " ...");
            } else {
                Message.info("downloading " + rres.getResource() + " ...");
            }
            if (eventManager != null) {
                eventManager.fireIvyEvent(
                    new StartArtifactDownloadEvent(
                        BasicResolver.this, artifact, origin));
            }            
        }
        public void endArtifactDownload(
                RepositoryCacheManager cache, Artifact artifact, 
                ArtifactDownloadReport adr, File archiveFile) {
            if (eventManager != null) {
                eventManager.fireIvyEvent(
                    new EndArtifactDownloadEvent(
                        BasicResolver.this, artifact, adr, archiveFile));
            }
        }
    };
}
