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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResolverHelper;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.ChecksumHelper;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 *
 */
public class RepositoryResolver extends AbstractResourceResolver {

    private Repository repository;

    private Boolean alwaysCheckExactRevision = null;

    public RepositoryResolver() {
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setName(String name) {
        super.setName(name);
        if (repository instanceof AbstractRepository) {
            ((AbstractRepository) repository).setName(name);
        }
    }

    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern,
            Artifact artifact, ResourceMDParser rmdparser, Date date) {
        return findResourceUsingPattern(getName(), getRepository(), getLatestStrategy(),
            getSettings().getVersionMatcher(), rmdparser, mrid, pattern, artifact, date,
            isAlwaysCheckExactRevision());
    }

    public ResolvedResource findResourceUsingPattern(String name, Repository repository,
            LatestStrategy strategy, VersionMatcher versionMatcher, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, String pattern, Artifact artifact, Date date,
            boolean alwaysCheckExactRevision) {
        try {
            if (!versionMatcher.isDynamic(mrid) || alwaysCheckExactRevision) {
                String resourceName = IvyPatternHelper.substitute(pattern, mrid, artifact);
                Message.debug("\t trying " + resourceName);
                logAttempt(resourceName);
                Resource res = repository.getResource(resourceName);
                boolean reachable = res.exists();
                if (reachable) {
                    String revision = pattern.indexOf(IvyPatternHelper.REVISION_KEY) == -1 ?
                            "working@" + name : mrid.getRevision();
                    return new ResolvedResource(res, revision);
                } else if (versionMatcher.isDynamic(mrid)) {
                    return findDynamicResourceUsingPattern(name, repository, strategy,
                        versionMatcher, rmdparser, mrid, pattern, artifact, date);
                } else {
                    Message.debug("\t" + name + ": resource not reachable for " + mrid + ": res="
                            + res);
                    return null;
                }
            } else {
                return findDynamicResourceUsingPattern(name, repository, strategy, versionMatcher,
                    rmdparser, mrid, pattern, artifact, date);
            }
        } catch (IOException ex) {
            throw new RuntimeException(name + ": unable to get resource for " + mrid + ": res="
                    + IvyPatternHelper.substitute(pattern, mrid, artifact) + ": " + ex, ex);
        }
    }

    private ResolvedResource findDynamicResourceUsingPattern(String name,
            Repository repository, LatestStrategy strategy, VersionMatcher versionMatcher,
            ResourceMDParser rmdparser, ModuleRevisionId mrid, String pattern, Artifact artifact,
            Date date) {
        logAttempt(IvyPatternHelper.substitute(pattern, ModuleRevisionId.newInstance(mrid,
            IvyPatternHelper.getTokenString(IvyPatternHelper.REVISION_KEY)), artifact));
        ResolvedResource[] rress = listResources(repository, mrid, pattern, artifact);
        if (rress == null) {
            Message.debug("\t" + name + ": unable to list resources for " + mrid + ": pattern="
                    + pattern);
            return null;
        } else {
            ResolvedResource found = findResource(rress, name, strategy, versionMatcher, rmdparser,
                mrid, date);
            if (found == null) {
                Message.debug("\t" + name + ": no resource found for " + mrid + ": pattern="
                        + pattern);
            }
            return found;
        }
    }

    /**
     * List all revisions as resolved resources for the given artifact in the given repository using
     * the given pattern, and using the given mrid except its revision.
     * 
     * @param repository
     *            the repository in which revisions should be located
     * @param mrid
     *            the module revision id to look for (except revision)
     * @param pattern
     *            the pattern to use to locate the revisions
     * @param artifact
     *            the artifact to find
     * @return an array of ResolvedResource, all pointing to a different revision of the given
     *         Artifact.
     */
    protected ResolvedResource[] listResources(
            Repository repository, ModuleRevisionId mrid, String pattern, Artifact artifact) {
        return ResolverHelper.findAll(repository, mrid, pattern, artifact);
    }

    protected long get(Resource resource, File dest) throws IOException {
        Message.verbose("\t" + getName() + ": downloading " + resource.getName());
        Message.debug("\t\tto " + dest);
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        repository.get(resource.getName(), dest);
        return dest.length();
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        String destPattern;
        if ("ivy".equals(artifact.getType()) && !getIvyPatterns().isEmpty()) {
            destPattern = (String) getIvyPatterns().get(0);
        } else if (!getArtifactPatterns().isEmpty()) {
            destPattern = (String) getArtifactPatterns().get(0);
        } else {
            throw new IllegalStateException("impossible to publish " + artifact + " using " + this
                    + ": no artifact pattern defined");
        }
        // Check for m2 compatibility
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }

        String dest = getDestination(destPattern, artifact, mrid);

        put(artifact, src, dest, overwrite);
        Message.info("\tpublished " + artifact.getName() + " to " + hidePassword(dest));
    }

    protected String getDestination(String pattern, Artifact artifact, ModuleRevisionId mrid) {
        return IvyPatternHelper.substitute(pattern, mrid, artifact);
    }

    private void put(Artifact artifact, File src, String dest, boolean overwrite)
            throws IOException {
        repository.put(artifact, src, dest, overwrite);
        String[] checksums = getChecksumAlgorithms();
        for (int i = 0; i < checksums.length; i++) {
            putChecksum(artifact, src, dest, overwrite, checksums[i]);
        }
    }

    private void putChecksum(Artifact artifact, File src, String dest, boolean overwrite,
            String algorithm) throws IOException {
        File csFile = File.createTempFile("ivytemp", algorithm);
        try {
            FileUtil.copy(new ByteArrayInputStream(ChecksumHelper.computeAsString(src, algorithm)
                    .getBytes()), csFile, null);
            repository.put(DefaultArtifact.cloneWithAnotherTypeAndExt(artifact, algorithm,
                artifact.getExt() + "." + algorithm), csFile, dest + "." + algorithm, overwrite);
        } finally {
            csFile.delete();
        }
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        EventManager eventManager = getEventManager();
        try {
            if (eventManager != null) {
                repository.addTransferListener(eventManager);
            }
            return super.download(artifacts, options);
        } finally {
            if (eventManager != null) {
                repository.removeTransferListener(eventManager);
            }
        }
    }

    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
        for (Iterator iter = patterns.iterator(); iter.hasNext();) {
            String pattern = (String) iter.next();
            String partiallyResolvedPattern = IvyPatternHelper.substituteTokens(pattern,
                tokenValues);
            String[] values = ResolverHelper.listTokenValues(repository, partiallyResolvedPattern,
                token);
            if (values != null) {
                names.addAll(filterNames(new ArrayList(Arrays.asList(values))));
            }
        }
    }
    
    public String getTypeName() {
        return "repository";
    }

    public void dumpSettings() {
        super.dumpSettings();
        Message.debug("\t\trepository: " + getRepository());
    }

    public void setSettings(ResolverSettings settings) {
        super.setSettings(settings);
        if (settings != null) {
            if (alwaysCheckExactRevision == null) {
                alwaysCheckExactRevision = Boolean.valueOf(settings
                        .getVariable("ivy.default.always.check.exact.revision"));
            }
        }
    }

    public boolean isAlwaysCheckExactRevision() {
        return alwaysCheckExactRevision == null ? true : alwaysCheckExactRevision.booleanValue();
    }

    public void setAlwaysCheckExactRevision(boolean alwaysCheckExactRevision) {
        this.alwaysCheckExactRevision = Boolean.valueOf(alwaysCheckExactRevision);
    }

}
