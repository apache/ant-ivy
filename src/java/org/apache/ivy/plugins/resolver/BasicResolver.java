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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.ModuleDescriptorWriter;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.util.MDResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.ChecksumHelper;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;

/**
 *
 */
public abstract class BasicResolver extends AbstractResolver {
    public static final String DESCRIPTOR_OPTIONAL = "optional";

    public static final String DESCRIPTOR_REQUIRED = "required";

    /**
     * Exception thrown internally in getDependency to indicate a dependency is unresolved.
     * <p>
     * Due to the contract of getDependency, this exception is never thrown publicly, but rather
     * converted in a message (either error or verbose) and returning null
     * </p>
     */
    private static class UnresolvedDependencyException extends RuntimeException {
        private boolean error;

        /**
         * Dependency has not been resolved. This is not an error and won't log any message.
         */
        public UnresolvedDependencyException() {
            this("", false);
        }

        /**
         * Dependency has not been resolved. This is an error and will log a message.
         */
        public UnresolvedDependencyException(String message) {
            this(message, true);
        }

        /**
         * Dependency has not been resolved. The boolean tells if it is an error or not, a message
         * will be logged if non empty.
         */
        public UnresolvedDependencyException(String message, boolean error) {
            super(message);
            this.error = error;
        }

        public boolean isError() {
            return error;
        }
    }

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private String workspaceName;

    /**
     * True if the files resolved are dependent of the environment from which they have been
     * resolved, false otherwise. In general, relative paths are dependent of the environment, and
     * absolute paths including machine reference are not.
     */
    private boolean envDependent = true;

    private List<String> ivyattempts = new ArrayList<String>();

    private Map<Artifact, List<String>> artattempts = new HashMap<Artifact, List<String>>();

    private boolean checkconsistency = true;

    private boolean allownomd = true;

    private boolean force = false;

    private String checksums = null;

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

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        IvyContext context = IvyContext.pushNewCopyContext();
        try {
            ResolvedModuleRevision mr = data.getCurrentResolvedModuleRevision();
            if (mr != null) {
                if (shouldReturnResolvedModule(dd, mr)) {
                    return mr;
                }
            }

            if (isForce()) {
                dd = dd.clone(ModuleRevisionId.newInstance(dd.getDependencyRevisionId(),
                    "latest.integration"));
            }
            DependencyDescriptor systemDd = dd;
            DependencyDescriptor nsDd = fromSystem(dd);
            context.setDependencyDescriptor(systemDd);
            context.setResolveData(data);

            clearIvyAttempts();
            clearArtifactAttempts();
            ModuleRevisionId systemMrid = systemDd.getDependencyRevisionId();
            ModuleRevisionId nsMrid = nsDd.getDependencyRevisionId();

            checkRevision(systemMrid);

            boolean isDynamic = getAndCheckIsDynamic(systemMrid);

            // we first search for the dependency in cache
            ResolvedModuleRevision rmr = null;
            rmr = findModuleInCache(systemDd, data);
            if (rmr != null) {
                if (rmr.getDescriptor().isDefault() && rmr.getResolver() != this) {
                    Message.verbose("\t" + getName() + ": found revision in cache: " + systemMrid
                            + " (resolved by " + rmr.getResolver().getName()
                            + "): but it's a default one, maybe we can find a better one");
                } else if (isForce() && rmr.getResolver() != this) {
                    Message.verbose("\t" + getName() + ": found revision in cache: " + systemMrid
                            + " (resolved by " + rmr.getResolver().getName()
                            + "): but we are in force mode, let's try to find one ourself");
                } else {
                    Message.verbose("\t" + getName() + ": revision in cache: " + systemMrid);
                    return checkLatest(systemDd, checkForcedResolvedModuleRevision(rmr), data);
                }
            }
            if (data.getOptions().isUseCacheOnly()) {
                throw new UnresolvedDependencyException("\t" + getName()
                        + " (useCacheOnly) : no ivy file found for " + systemMrid, false);
            }

            checkInterrupted();

            ResolvedResource ivyRef = findIvyFileRef(nsDd, data);
            checkInterrupted();

            // get module descriptor
            ModuleDescriptor nsMd;
            ModuleDescriptor systemMd = null;
            if (ivyRef == null) {
                if (!isAllownomd()) {
                    throw new UnresolvedDependencyException("\t" + getName()
                            + ": no ivy file found for " + systemMrid, false);
                }
                nsMd = DefaultModuleDescriptor.newDefaultInstance(nsMrid,
                    nsDd.getAllDependencyArtifacts());
                ResolvedResource artifactRef = findFirstArtifactRef(nsMd, nsDd, data);
                checkInterrupted();
                if (artifactRef == null) {
                    throw new UnresolvedDependencyException("\t" + getName()
                            + ": no ivy file nor artifact found for " + systemMrid, false);
                } else {
                    long lastModified = artifactRef.getLastModified();
                    if (lastModified != 0 && nsMd instanceof DefaultModuleDescriptor) {
                        ((DefaultModuleDescriptor) nsMd).setLastModified(lastModified);
                    }
                    Message.verbose("\t" + getName() + ": no ivy file found for " + systemMrid
                            + ": using default data");
                    if (isDynamic) {
                        nsMd.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(nsMrid,
                            artifactRef.getRevision()));
                    }
                    systemMd = toSystem(nsMd);
                    MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(
                            systemMd.getMetadataArtifact());
                    madr.setDownloadStatus(DownloadStatus.NO);
                    madr.setSearched(true);
                    rmr = new ResolvedModuleRevision(this, this, systemMd, madr, isForce());
                    getRepositoryCacheManager().cacheModuleDescriptor(this, artifactRef,
                        toSystem(dd), systemMd.getAllArtifacts()[0], null, getCacheOptions(data));
                }
            } else {
                if (ivyRef instanceof MDResolvedResource) {
                    rmr = ((MDResolvedResource) ivyRef).getResolvedModuleRevision();
                }
                if (rmr == null) {
                    rmr = parse(ivyRef, systemDd, data);
                    if (rmr == null) {
                        throw new UnresolvedDependencyException();
                    }
                }
                if (!rmr.getReport().isDownloaded() && rmr.getReport().getLocalFile() != null) {
                    return checkLatest(systemDd, checkForcedResolvedModuleRevision(rmr), data);
                } else {
                    nsMd = rmr.getDescriptor();

                    // check descriptor data is in sync with resource revision and names
                    systemMd = toSystem(nsMd);
                    if (isCheckconsistency()) {
                        checkDescriptorConsistency(systemMrid, systemMd, ivyRef);
                        checkDescriptorConsistency(nsMrid, nsMd, ivyRef);
                    } else {
                        if (systemMd instanceof DefaultModuleDescriptor) {
                            DefaultModuleDescriptor defaultMd = (DefaultModuleDescriptor) systemMd;
                            ModuleRevisionId revision = getRevision(ivyRef, systemMrid, systemMd);
                            defaultMd.setModuleRevisionId(revision);
                            defaultMd.setResolvedModuleRevisionId(revision);
                        } else {
                            Message.warn("consistency disabled with instance of non DefaultModuleDescriptor..."
                                    + " module info can't be updated, so consistency check will be done");
                            checkDescriptorConsistency(nsMrid, nsMd, ivyRef);
                            checkDescriptorConsistency(systemMrid, systemMd, ivyRef);
                        }
                    }
                    rmr = new ResolvedModuleRevision(this, this, systemMd,
                            toSystem(rmr.getReport()), isForce());
                }
            }

            resolveAndCheckRevision(systemMd, systemMrid, ivyRef, isDynamic);
            resolveAndCheckPublicationDate(systemDd, systemMd, systemMrid, data);
            checkNotConvertedExclusionRule(systemMd, ivyRef, data);

            if (ivyRef == null || ivyRef.getResource() != null) {
                cacheModuleDescriptor(systemMd, systemMrid, ivyRef, rmr);
            }

            return checkLatest(systemDd, checkForcedResolvedModuleRevision(rmr), data);
        } catch (UnresolvedDependencyException ex) {
            if (ex.getMessage().length() > 0) {
                if (ex.isError()) {
                    Message.error(ex.getMessage());
                } else {
                    Message.verbose(ex.getMessage());
                }
            }
            return data.getCurrentResolvedModuleRevision();
        } finally {
            IvyContext.popContext();
        }
    }

    protected boolean shouldReturnResolvedModule(DependencyDescriptor dd, ResolvedModuleRevision mr) {
        // a resolved module revision has already been found by a prior dependency resolver
        // let's see if it should be returned and bypass this resolver

        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        boolean isDynamic = getSettings().getVersionMatcher().isDynamic(mrid);
        boolean shouldReturn = mr.isForce();
        shouldReturn |= !isDynamic && !mr.getDescriptor().isDefault();
        shouldReturn &= !isForce();

        return shouldReturn;
    }

    private ResolvedModuleRevision checkForcedResolvedModuleRevision(ResolvedModuleRevision rmr) {
        if (rmr == null) {
            return null;
        }
        if (!isForce() || rmr.isForce()) {
            return rmr;
        }
        return new ResolvedModuleRevision(rmr.getResolver(), rmr.getArtifactResolver(),
                rmr.getDescriptor(), rmr.getReport(), true);
    }

    private void cacheModuleDescriptor(ModuleDescriptor systemMd, ModuleRevisionId systemMrid,
            ResolvedResource ivyRef, ResolvedModuleRevision rmr) {
        RepositoryCacheManager cacheManager = getRepositoryCacheManager();

        final ModuleDescriptorParser parser = systemMd.getParser();

        // the metadata artifact which was used to cache the original metadata file
        Artifact requestedMetadataArtifact = ivyRef == null ? systemMd.getMetadataArtifact()
                : parser.getMetadataArtifact(
                    ModuleRevisionId.newInstance(systemMrid, systemMd.getRevision()),
                    ivyRef.getResource());

        cacheManager.originalToCachedModuleDescriptor(this, ivyRef, requestedMetadataArtifact, rmr,
            new ModuleDescriptorWriter() {
                public void write(ResolvedResource originalMdResource, ModuleDescriptor md,
                        File src, File dest) throws IOException, ParseException {
                    if (originalMdResource == null) {
                        // a basic ivy file is written containing default data
                        XmlModuleDescriptorWriter.write(md, dest);
                    } else {
                        // copy and update ivy file from source to cache
                        parser.toIvyFile(new FileInputStream(src),
                            originalMdResource.getResource(), dest, md);
                        long repLastModified = originalMdResource.getLastModified();
                        if (repLastModified > 0) {
                            dest.setLastModified(repLastModified);
                        }
                    }
                }
            });
    }

    private void checkNotConvertedExclusionRule(ModuleDescriptor systemMd, ResolvedResource ivyRef,
            ResolveData data) {
        if (!getNamespace().equals(Namespace.SYSTEM_NAMESPACE) && !systemMd.isDefault()
                && data.getSettings().logNotConvertedExclusionRule()
                && systemMd instanceof DefaultModuleDescriptor) {
            DefaultModuleDescriptor dmd = (DefaultModuleDescriptor) systemMd;
            if (dmd.isNamespaceUseful()) {
                Message.warn("the module descriptor " + ivyRef.getResource()
                        + " has information which can't be converted into "
                        + "the system namespace. "
                        + "It will require the availability of the namespace '"
                        + getNamespace().getName() + "' to be fully usable.");
            }
        }
    }

    private void resolveAndCheckPublicationDate(DependencyDescriptor systemDd,
            ModuleDescriptor systemMd, ModuleRevisionId systemMrid, ResolveData data) {
        // resolve and check publication date
        if (data.getDate() != null) {
            long pubDate = getPublicationDate(systemMd, systemDd, data);
            if (pubDate > data.getDate().getTime()) {
                throw new UnresolvedDependencyException("\t" + getName()
                        + ": unacceptable publication date => was=" + new Date(pubDate)
                        + " required=" + data.getDate());
            } else if (pubDate == -1) {
                throw new UnresolvedDependencyException("\t" + getName()
                        + ": impossible to guess publication date: artifact missing for "
                        + systemMrid);
            }
            systemMd.setResolvedPublicationDate(new Date(pubDate));
        }
    }

    protected void checkModuleDescriptorRevision(ModuleDescriptor systemMd,
            ModuleRevisionId systemMrid) {
        if (!getSettings().getVersionMatcher().accept(systemMrid, systemMd)) {
            throw new UnresolvedDependencyException("\t" + getName()
                    + ": unacceptable revision => was="
                    + systemMd.getResolvedModuleRevisionId().getRevision() + " required="
                    + systemMrid.getRevision());
        }
    }

    private boolean getAndCheckIsDynamic(ModuleRevisionId systemMrid) {
        boolean isDynamic = getSettings().getVersionMatcher().isDynamic(systemMrid);
        if (isDynamic && !acceptLatest()) {
            throw new UnresolvedDependencyException("dynamic revisions not handled by "
                    + getClass().getName() + ". impossible to resolve " + systemMrid);
        }
        return isDynamic;
    }

    private void checkRevision(ModuleRevisionId systemMrid) {
        // check revision
        int index = systemMrid.getRevision().indexOf("@");
        if (index != -1 && !systemMrid.getRevision().substring(index + 1).equals(workspaceName)) {
            throw new UnresolvedDependencyException("\t" + getName() + ": unhandled revision => "
                    + systemMrid.getRevision());
        }
    }

    private void resolveAndCheckRevision(ModuleDescriptor systemMd,
            ModuleRevisionId dependencyConstraint, ResolvedResource ivyRef, boolean isDynamic) {
        // we get the resolved module revision id from the descriptor: it may contain extra
        // attributes that were not included in the dependency constraint
        ModuleRevisionId resolvedMrid = systemMd.getResolvedModuleRevisionId();
        if (resolvedMrid.getRevision() == null || resolvedMrid.getRevision().length() == 0
                || resolvedMrid.getRevision().startsWith("working@")) {
            if (!isDynamic) {
                resolvedMrid = ModuleRevisionId.newInstance(resolvedMrid,
                    dependencyConstraint.getRevision());
            } else if (ivyRef == null) {
                resolvedMrid = systemMd.getMetadataArtifact().getModuleRevisionId();
            } else if (ivyRef.getRevision() == null || ivyRef.getRevision().length() == 0) {
                resolvedMrid = ModuleRevisionId.newInstance(resolvedMrid, "working@" + getName());
            } else {
                resolvedMrid = ModuleRevisionId.newInstance(resolvedMrid, ivyRef.getRevision());
            }
        }
        if (isDynamic) {
            Message.verbose("\t\t[" + toSystem(resolvedMrid).getRevision() + "] "
                    + dependencyConstraint.getModuleId());
        }
        systemMd.setResolvedModuleRevisionId(resolvedMrid);
        checkModuleDescriptorRevision(systemMd, dependencyConstraint);
    }

    private ModuleRevisionId getRevision(ResolvedResource ivyRef, ModuleRevisionId askedMrid,
            ModuleDescriptor md) {
        Map<String, String> allAttributes = new HashMap<String, String>();
        allAttributes.putAll(md.getQualifiedExtraAttributes());
        allAttributes.putAll(askedMrid.getQualifiedExtraAttributes());

        String revision = ivyRef.getRevision();
        if (revision == null) {
            Message.debug("no revision found in reference for " + askedMrid);
            if (getSettings().getVersionMatcher().isDynamic(askedMrid)) {
                if (md.getModuleRevisionId().getRevision() == null) {
                    revision = "working@" + getName();
                } else {
                    Message.debug("using " + askedMrid);
                    revision = askedMrid.getRevision();
                }
            } else {
                Message.debug("using " + askedMrid);
                revision = askedMrid.getRevision();
            }
        }

        return ModuleRevisionId.newInstance(askedMrid.getOrganisation(), askedMrid.getName(),
            askedMrid.getBranch(), revision, allAttributes);
    }

    public ResolvedModuleRevision parse(final ResolvedResource mdRef, DependencyDescriptor dd,
            ResolveData data) throws ParseException {

        DependencyDescriptor nsDd = dd;
        dd = toSystem(nsDd);

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

        ModuleRevisionId resolvedMrid = mrid;

        // first check if this dependency has not yet been resolved
        if (getSettings().getVersionMatcher().isDynamic(mrid)) {
            resolvedMrid = ModuleRevisionId.newInstance(mrid, mdRef.getRevision());
            IvyNode node = data.getNode(resolvedMrid);
            if (node != null && node.getModuleRevision() != null) {
                // this revision has already be resolved : return it
                if (node.getDescriptor() != null && node.getDescriptor().isDefault()) {
                    Message.verbose("\t" + getName() + ": found already resolved revision: "
                            + resolvedMrid
                            + ": but it's a default one, maybe we can find a better one");
                } else {
                    Message.verbose("\t" + getName() + ": revision already resolved: "
                            + resolvedMrid);
                    node.getModuleRevision().getReport().setSearched(true);
                    return node.getModuleRevision();
                }
            }
        }

        Artifact moduleArtifact = parser.getMetadataArtifact(resolvedMrid, mdRef.getResource());
        return getRepositoryCacheManager().cacheModuleDescriptor(this, mdRef, dd, moduleArtifact,
            downloader, getCacheOptions(data));
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
                    Message.warn("Failed to parse the file '" + resource + "'", e);
                    return null;
                }
            }

        };
    }

    protected ResourceMDParser getDefaultRMDParser(final ModuleId mid) {
        return new ResourceMDParser() {
            public MDResolvedResource parse(Resource resource, String rev) {
                DefaultModuleDescriptor md = DefaultModuleDescriptor
                        .newDefaultInstance(new ModuleRevisionId(mid, rev));
                MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(
                        md.getMetadataArtifact());
                madr.setDownloadStatus(DownloadStatus.NO);
                madr.setSearched(true);
                return new MDResolvedResource(resource, rev, new ResolvedModuleRevision(
                        BasicResolver.this, BasicResolver.this, md, madr, isForce()));
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
        if (mrid.getBranch() != null
                && !mrid.getBranch().equals(md.getModuleRevisionId().getBranch())) {
            Message.error("\t" + getName() + ": bad branch name found in " + ivyRef.getResource()
                    + ": expected='" + mrid.getBranch() + " found='"
                    + md.getModuleRevisionId().getBranch() + "'");
            errors.append("bad branch name: expected='" + mrid.getBranch() + "' found='"
                    + md.getModuleRevisionId().getBranch() + "'; ");
            ok = false;
        }
        if (ivyRef.getRevision() != null && !ivyRef.getRevision().startsWith("working@")
                && !mrid.getRevision().equals(md.getModuleRevisionId().getRevision())) {
            ModuleRevisionId expectedMrid = ModuleRevisionId.newInstance(mrid, mrid.getRevision());
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
        for (Entry<String, String> extra : mrid.getExtraAttributes().entrySet()) {
            if (extra.getValue() != null
                    && !extra.getValue().equals(md.getExtraAttribute(extra.getKey()))) {
                String errorMsg = "bad " + extra.getKey() + " found in " + ivyRef.getResource()
                        + ": expected='" + extra.getValue() + "' found='"
                        + md.getExtraAttribute(extra.getKey()) + "'";
                Message.error("\t" + getName() + ": " + errorMsg);
                errors.append(errorMsg + ";");
                ok = false;
            }
        }
        if (!ok) {
            throw new ParseException("inconsistent module descriptor file found in '"
                    + ivyRef.getResource() + "': " + errors, 0);
        }
    }

    /**
     * When the resolver has many choices, this function helps choosing one
     * 
     * @param rress
     *            the list of resolved resource which the resolver found to fit the requirement
     * @param rmdparser
     *            the parser of module descriptor
     * @param mrid
     *            the module being resolved
     * @param date
     *            the current date
     * @return the selected resource
     */
    public ResolvedResource findResource(ResolvedResource[] rress, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        String name = getName();
        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        ResolvedResource found = null;
        List<ArtifactInfo> sorted = getLatestStrategy().sort(rress);
        List<String> rejected = new ArrayList<String>();
        List<ModuleRevisionId> foundBlacklisted = new ArrayList<ModuleRevisionId>();
        IvyContext context = IvyContext.getContext();

        for (ListIterator<ArtifactInfo> iter = sorted.listIterator(sorted.size()); iter
                .hasPrevious();) {
            ResolvedResource rres = (ResolvedResource) iter.previous();
            // we start by filtering based on information already available,
            // even though we don't even know if the resource actually exist.
            // But checking for existence is most of the time more costly than checking
            // name, blacklisting and first level version matching
            if (filterNames(new ArrayList<String>(Collections.singleton(rres.getRevision())))
                    .isEmpty()) {
                Message.debug("\t" + name + ": filtered by name: " + rres);
                continue;
            }
            ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mrid, rres.getRevision());

            ResolveData data = context.getResolveData();
            if (data != null && data.getReport() != null
                    && data.isBlacklisted(data.getReport().getConfiguration(), foundMrid)) {
                Message.debug("\t" + name + ": blacklisted: " + rres);
                rejected.add(rres.getRevision() + " (blacklisted)");
                foundBlacklisted.add(foundMrid);
                continue;
            }

            if (!versionMatcher.accept(mrid, foundMrid)) {
                Message.debug("\t" + name + ": rejected by version matcher: " + rres);
                rejected.add(rres.getRevision());
                continue;
            }
            if (rres.getResource() != null && !rres.getResource().exists()) {
                Message.debug("\t" + name + ": unreachable: " + rres + "; res="
                        + rres.getResource());
                rejected.add(rres.getRevision() + " (unreachable)");
                continue;
            }
            if ((date != null && rres.getLastModified() > date.getTime())) {
                Message.verbose("\t" + name + ": too young: " + rres);
                rejected.add(rres.getRevision() + " (" + rres.getLastModified() + ")");
                continue;
            }
            if (versionMatcher.needModuleDescriptor(mrid, foundMrid)) {
                ResolvedResource r = rmdparser.parse(rres.getResource(), rres.getRevision());
                if (r == null) {
                    Message.debug("\t" + name + ": impossible to get module descriptor resource: "
                            + rres);
                    rejected.add(rres.getRevision() + " (no or bad MD)");
                    continue;
                }
                ModuleDescriptor md = ((MDResolvedResource) r).getResolvedModuleRevision()
                        .getDescriptor();
                if (md.isDefault()) {
                    Message.debug("\t" + name + ": default md rejected by version matcher"
                            + "requiring module descriptor: " + rres);
                    rejected.add(rres.getRevision() + " (MD)");
                    continue;
                } else if (!versionMatcher.accept(mrid, md)) {
                    Message.debug("\t" + name + ": md rejected by version matcher: " + rres);
                    rejected.add(rres.getRevision() + " (MD)");
                    continue;
                } else {
                    found = r;
                }
            } else {
                found = rres;
            }

            if (found != null) {
                break;
            }
        }
        if (found == null && !rejected.isEmpty()) {
            logAttempt(rejected.toString());
        }
        if (found == null && !foundBlacklisted.isEmpty()) {
            // all acceptable versions have been blacklisted, this means that an unsolvable conflict
            // has been found
            DependencyDescriptor dd = context.getDependencyDescriptor();
            IvyNode parentNode = context.getResolveData().getNode(dd.getParentRevisionId());
            ConflictManager cm = parentNode.getConflictManager(mrid.getModuleId());
            cm.handleAllBlacklistedRevisions(dd, foundBlacklisted);
        }

        return found;
    }

    /**
     * Filters names before returning them in the findXXXNames or findTokenValues method.
     * <p>
     * Remember to call the super implementation when overriding this method.
     * </p>
     * 
     * @param names
     *            the list to filter.
     * @return the filtered list
     */
    protected Collection<String> filterNames(Collection<String> names) {
        getSettings().filterIgnore(names);
        return names;
    }

    protected void clearIvyAttempts() {
        ivyattempts.clear();
        clearArtifactAttempts();
    }

    protected void logIvyAttempt(String attempt) {
        ivyattempts.add(attempt);
        Message.verbose("\t\ttried " + attempt);
    }

    protected void logArtifactAttempt(Artifact art, String attempt) {
        List<String> attempts = artattempts.get(art);
        if (attempts == null) {
            attempts = new ArrayList<String>();
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

    @Override
    public void reportFailure() {
        Message.warn("==== " + getName() + ": tried");
        for (String m : ivyattempts) {
            Message.warn("  " + m);
        }
        for (Entry<Artifact, List<String>> entry : artattempts.entrySet()) {
            Artifact art = entry.getKey();
            List<String> attempts = entry.getValue();
            if (attempts != null) {
                Message.warn("  -- artifact " + art + ":");
                for (String m : attempts) {
                    Message.warn("  " + m);
                }
            }
        }
    }

    @Override
    public void reportFailure(Artifact art) {
        Message.warn("==== " + getName() + ": tried");
        List<String> attempts = artattempts.get(art);
        if (attempts != null) {
            for (String m : attempts) {
                Message.warn("  " + m);
            }
        }
    }

    protected boolean acceptLatest() {
        return true;
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        RepositoryCacheManager cacheManager = getRepositoryCacheManager();

        clearArtifactAttempts();
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactDownloadReport adr = cacheManager.download(artifacts[i],
                artifactResourceResolver, downloader, getCacheDownloadOptions(options));
            if (DownloadStatus.FAILED == adr.getDownloadStatus()) {
                if (!ArtifactDownloadReport.MISSING_ARTIFACT.equals(adr.getDownloadDetails())) {
                    Message.warn("\t" + adr);
                }
            } else if (DownloadStatus.NO == adr.getDownloadStatus()) {
                Message.verbose("\t" + adr);
            } else if (LogOptions.LOG_QUIET.equals(options.getLog())) {
                Message.verbose("\t" + adr);
            } else {
                Message.info("\t" + adr);
            }
            dr.addArtifactReport(adr);
            checkInterrupted();
        }
        return dr;
    }

    protected void clearArtifactAttempts() {
        artattempts.clear();
    }

    @Override
    public ArtifactDownloadReport download(final ArtifactOrigin origin, DownloadOptions options) {
        Checks.checkNotNull(origin, "origin");
        return getRepositoryCacheManager().download(origin.getArtifact(),
            new ArtifactResourceResolver() {
                public ResolvedResource resolve(Artifact artifact) {
                    try {
                        Resource resource = getResource(origin.getLocation());
                        if (resource == null) {
                            return null;
                        }
                        String revision = origin.getArtifact().getModuleRevisionId().getRevision();
                        return new ResolvedResource(resource, revision);
                    } catch (IOException e) {
                        Message.debug(e);
                        return null;
                    }
                }
            }, downloader, getCacheDownloadOptions(options));
    }

    protected abstract Resource getResource(String source) throws IOException;

    @Override
    public boolean exists(Artifact artifact) {
        ResolvedResource artifactRef = getArtifactRef(artifact, null);
        if (artifactRef != null) {
            return artifactRef.getResource().exists();
        }
        return false;
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        ArtifactOrigin origin = getRepositoryCacheManager().getSavedArtifactOrigin(
            toSystem(artifact));
        if (!ArtifactOrigin.isUnknown(origin)) {
            return origin;
        }
        ResolvedResource artifactRef = getArtifactRef(artifact, null);
        if (artifactRef != null && artifactRef.getResource().exists()) {
            return new ArtifactOrigin(artifact, artifactRef.getResource().isLocal(), artifactRef
                    .getResource().getName());
        }
        return null;
    }

    protected long getPublicationDate(ModuleDescriptor md, DependencyDescriptor dd, ResolveData data) {
        if (md.getPublicationDate() != null) {
            return md.getPublicationDate().getTime();
        }
        ResolvedResource artifactRef = findFirstArtifactRef(md, dd, data);
        if (artifactRef != null) {
            return artifactRef.getLastModified();
        }
        return -1;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String[] listTokenValues(String token, Map<String, String> otherTokenValues) {
        Collection<String> ret = findNames(otherTokenValues, token);
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public OrganisationEntry[] listOrganisations() {
        Collection<String> names = findNames(Collections.<String, String> emptyMap(),
            IvyPatternHelper.ORGANISATION_KEY);
        OrganisationEntry[] ret = new OrganisationEntry[names.size()];
        int i = 0;
        for (String org : names) {
            ret[i++] = new OrganisationEntry(this, org);
        }
        return ret;
    }

    @Override
    public ModuleEntry[] listModules(OrganisationEntry org) {
        Map<String, String> tokenValues = new HashMap<String, String>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection<String> names = findNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i = 0;
        for (String name : names) {
            ret[i++] = new ModuleEntry(org, name);
        }
        return ret;
    }

    @Override
    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        Map<String, String> tokenValues = new HashMap<String, String>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, mod.getOrganisation());
        tokenValues.put(IvyPatternHelper.MODULE_KEY, mod.getModule());
        Collection<String> names = findNames(tokenValues, IvyPatternHelper.REVISION_KEY);
        RevisionEntry[] ret = new RevisionEntry[names.size()];
        int i = 0;
        for (String name : names) {
            ret[i++] = new RevisionEntry(mod, name);
        }
        return ret;
    }

    protected abstract Collection<String> findNames(Map<String, String> tokenValues, String token);

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
        if (!ChecksumHelper.isKnownAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unknown checksum algorithm: " + algorithm);
        }

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
                Resource resource;
                if ("file".equals(url.getProtocol())) {
                    File f;
                    try {
                        f = new File(new URI(url.toExternalForm()));
                    } catch (URISyntaxException e) {
                        // unexpected, try to get the best of it
                        f = new File(url.getPath());
                    }
                    resource = new FileResource(new FileRepository(), f);
                } else {
                    resource = new URLResource(url);
                }
                ret = new ResolvedResource(resource, artifact.getModuleRevisionId().getRevision());
            }
            return ret;
        } finally {
            IvyContext.getContext().set(getName() + ".artifact", null);
        }
    }

    public ResolvedResource doFindArtifactRef(Artifact artifact, Date date) {
        return findArtifactRef(artifact, date);
    }

    protected abstract ResolvedResource findArtifactRef(Artifact artifact, Date date);

    protected abstract long get(Resource resource, File dest) throws IOException;

    public boolean isCheckconsistency() {
        return checkconsistency;
    }

    public void setCheckconsistency(boolean checkConsitency) {
        checkconsistency = checkConsitency;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isAllownomd() {
        return allownomd;
    }

    public void setAllownomd(boolean b) {
        Message.deprecated("allownomd is deprecated, please use descriptor=\""
                + (b ? DESCRIPTOR_OPTIONAL : DESCRIPTOR_REQUIRED) + "\" instead");
        allownomd = b;
    }

    /**
     * Sets the module descriptor presence rule. Should be one of {@link #DESCRIPTOR_REQUIRED} or
     * {@link #DESCRIPTOR_OPTIONAL}.
     * 
     * @param descriptorRule
     *            the descriptor rule to use with this resolver.
     */
    public void setDescriptor(String descriptorRule) {
        if (DESCRIPTOR_REQUIRED.equals(descriptorRule)) {
            allownomd = false;
        } else if (DESCRIPTOR_OPTIONAL.equals(descriptorRule)) {
            allownomd = true;
        } else {
            throw new IllegalArgumentException("unknown descriptor rule '" + descriptorRule
                    + "'. Allowed rules are: "
                    + Arrays.asList(new String[] {DESCRIPTOR_REQUIRED, DESCRIPTOR_OPTIONAL}));
        }
    }

    public String[] getChecksumAlgorithms() {
        String csDef = checksums == null ? getSettings().getVariable("ivy.checksums") : checksums;
        if (csDef == null) {
            return new String[0];
        }
        // csDef is a comma separated list of checksum algorithms to use with this resolver
        // we parse and return it as a String[]
        String[] checksums = csDef.split(",");
        List<String> algos = new ArrayList<String>();
        for (int i = 0; i < checksums.length; i++) {
            String cs = checksums[i].trim();
            if (!"".equals(cs) && !"none".equals(cs)) {
                algos.add(cs);
            }
        }
        return algos.toArray(new String[algos.size()]);
    }

    public void setChecksums(String checksums) {
        this.checksums = checksums;
    }

    private final ArtifactResourceResolver artifactResourceResolver = new ArtifactResourceResolver() {
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
            if (resource.getName().equals(String.valueOf(artifact.getUrl()))) {
                if (part.getParentFile() != null) {
                    part.getParentFile().mkdirs();
                }
                extartifactrep.get(resource.getName(), part);
            } else {
                getAndCheck(resource, part);
            }
            if (!part.renameTo(dest)) {
                throw new IOException("impossible to move part file to definitive one: " + part
                        + " -> " + dest);
            }

        }
    };

}
