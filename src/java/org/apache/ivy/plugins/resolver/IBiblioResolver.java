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
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.ContextualSAXHandler;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.SAXException;

/**
 * IBiblioResolver is a resolver which can be used to resolve dependencies found in the ibiblio
 * maven repository, or similar repositories.
 * <p>
 * For more flexibility with url and patterns, see
 * {@link org.apache.ivy.plugins.resolver.URLResolver}.
 */
public class IBiblioResolver extends URLResolver {
    private static final String M2_PER_MODULE_PATTERN = "[revision]/[artifact]-[revision](-[classifier]).[ext]";

    private static final String M2_PATTERN = "[organisation]/[module]/" + M2_PER_MODULE_PATTERN;

    public static final String DEFAULT_PATTERN = "[module]/[type]s/[artifact]-[revision].[ext]";

    public static final String DEFAULT_ROOT = "http://www.ibiblio.org/maven/";

    public static final String DEFAULT_M2_ROOT = "https://repo1.maven.org/maven2/";

    private String root = null;

    private String pattern = null;

    // use poms if m2 compatible is true
    private boolean usepoms = true;

    // use maven-metadata.xml is exists to list revisions
    private boolean useMavenMetadata = true;

    public IBiblioResolver() {
        // SNAPSHOT revisions are changing revisions
        setChangingMatcher(PatternMatcher.REGEXP);
        setChangingPattern(".*-SNAPSHOT");
    }

    @Override
    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        if (isM2compatible() && isUsepoms()) {
            ModuleRevisionId mrid = dd.getDependencyRevisionId();
            mrid = convertM2IdForResourceSearch(mrid);

            ResolvedResource rres = null;
            if (dd.getDependencyRevisionId().getRevision().endsWith("SNAPSHOT")) {
                rres = findSnapshotDescriptor(dd, data, mrid);
                if (rres != null) {
                    return rres;
                }
            }

            rres = findResourceUsingPatterns(mrid, getIvyPatterns(),
                DefaultArtifact.newPomArtifact(mrid, data.getDate()), getRMDParser(dd, data),
                data.getDate());
            return rres;
        } else {
            return null;
        }
    }

    @Override
    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureConfigured(getSettings());
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        ResolvedResource rres = null;
        if (artifact.getId().getRevision().endsWith("SNAPSHOT") && isM2compatible()) {
            rres = findSnapshotArtifact(artifact, date, mrid);
            if (rres != null) {
                return rres;
            }
        }
        return findResourceUsingPatterns(mrid, getArtifactPatterns(), artifact,
            getDefaultRMDParser(artifact.getModuleRevisionId().getModuleId()), date);
    }

    private ResolvedResource findSnapshotArtifact(Artifact artifact, Date date,
            ModuleRevisionId mrid) {
        String rev = findSnapshotVersion(mrid);
        if (rev != null) {
            // replace the revision token in file name with the resolved revision
            String pattern = getWholePattern().replaceFirst("\\-\\[revision\\]", "-" + rev);
            return findResourceUsingPattern(mrid, pattern, artifact, getDefaultRMDParser(artifact
                    .getModuleRevisionId().getModuleId()), date);
        }
        return null;
    }

    private ResolvedResource findSnapshotDescriptor(DependencyDescriptor dd, ResolveData data,
            ModuleRevisionId mrid) {
        String rev = findSnapshotVersion(mrid);
        if (rev != null) {
            // here it would be nice to be able to store the resolved snapshot version, to avoid
            // having to follow the same process to download artifacts

            Message.verbose("[" + rev + "] " + mrid);

            // replace the revision token in file name with the resolved revision
            String pattern = getWholePattern().replaceFirst("\\-\\[revision\\]", "-" + rev);
            return findResourceUsingPattern(mrid, pattern,
                DefaultArtifact.newPomArtifact(mrid, data.getDate()), getRMDParser(dd, data),
                data.getDate());
        }
        return null;
    }

    private String findSnapshotVersion(ModuleRevisionId mrid) {
        if (!isM2compatible()) {
            return null;
        }

        if (shouldUseMavenMetadata(getWholePattern())) {
            InputStream metadataStream = null;
            try {
                String metadataLocation = IvyPatternHelper.substitute(root
                        + "[organisation]/[module]/[revision]/maven-metadata.xml", mrid);
                Resource metadata = getRepository().getResource(metadataLocation);
                if (metadata.exists()) {
                    metadataStream = metadata.openStream();
                    final StringBuffer timestamp = new StringBuffer();
                    final StringBuffer buildNumer = new StringBuffer();
                    XMLHelper.parse(metadataStream, null, new ContextualSAXHandler() {
                        @Override
                        public void endElement(String uri, String localName, String qName)
                                throws SAXException {
                            if ("metadata/versioning/snapshot/timestamp".equals(getContext())) {
                                timestamp.append(getText());
                            }
                            if ("metadata/versioning/snapshot/buildNumber".equals(getContext())) {
                                buildNumer.append(getText());
                            }
                            super.endElement(uri, localName, qName);
                        }
                    }, null);
                    if (timestamp.length() > 0) {
                        // we have found a timestamp, so this is a snapshot unique version
                        String rev = mrid.getRevision();
                        rev = rev.substring(0, rev.length() - "SNAPSHOT".length());
                        rev = rev + timestamp.toString() + "-" + buildNumer.toString();

                        return rev;
                    }
                } else {
                    Message.verbose("\tmaven-metadata not available: " + metadata);
                }
            } catch (IOException e) {
                Message.verbose("impossible to access maven metadata file, ignored", e);
            } catch (SAXException e) {
                Message.verbose("impossible to parse maven metadata file, ignored", e);
            } catch (ParserConfigurationException e) {
                Message.verbose("impossible to parse maven metadata file, ignored", e);
            } finally {
                if (metadataStream != null) {
                    try {
                        metadataStream.close();
                    } catch (IOException e) {
                        // ignored
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void setM2compatible(boolean m2compatible) {
        super.setM2compatible(m2compatible);
        if (m2compatible) {
            if (root == null) {
                root = DEFAULT_M2_ROOT;
            }
            if (pattern == null) {
                pattern = M2_PATTERN;
            }
            updateWholePattern();
        }
    }

    public void ensureConfigured(ResolverSettings settings) {
        if (settings != null && (root == null || pattern == null)) {
            if (root == null) {
                String root = settings.getVariable("ivy.ibiblio.default.artifact.root");
                if (root != null) {
                    this.root = root;
                } else {
                    settings.configureRepositories(true);
                    this.root = settings.getVariable("ivy.ibiblio.default.artifact.root");
                }
            }
            if (pattern == null) {
                String pattern = settings.getVariable("ivy.ibiblio.default.artifact.pattern");
                if (pattern != null) {
                    this.pattern = pattern;
                } else {
                    settings.configureRepositories(false);
                    this.pattern = settings.getVariable("ivy.ibiblio.default.artifact.pattern");
                }
            }
            updateWholePattern();
        }
    }

    @Override
    protected String getModuleDescriptorExtension() {
        return "pom";
    }

    private String getWholePattern() {
        return root + pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        this.pattern = pattern;
        ensureConfigured(getSettings());
        updateWholePattern();
    }

    public String getRoot() {
        return root;
    }

    /**
     * Sets the root of the maven like repository. The maven like repository is necessarily an http
     * repository.
     * 
     * @param root
     *            the root of the maven like repository
     * @throws IllegalArgumentException
     *             if root does not start with "http://"
     */
    public void setRoot(String root) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (!root.endsWith("/")) {
            this.root = root + "/";
        } else {
            this.root = root;
        }
        ensureConfigured(getSettings());
        updateWholePattern();
    }

    private void updateWholePattern() {
        if (isM2compatible() && isUsepoms()) {
            setIvyPatterns(Collections.singletonList(getWholePattern()));
        } else {
            setIvyPatterns(Collections.<String> emptyList());
        }
        setArtifactPatterns(Collections.singletonList(getWholePattern()));
    }

    public void publish(Artifact artifact, File src) {
        throw new UnsupportedOperationException("publish not supported by IBiblioResolver");
    }

    // we do not allow to list organisations on ibiblio, nor modules in ibiblio 1
    @Override
    public String[] listTokenValues(String token, Map<String, String> otherTokenValues) {
        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return new String[0];
        }
        if (IvyPatternHelper.MODULE_KEY.equals(token) && !isM2compatible()) {
            return new String[0];
        }
        ensureConfigured(getSettings());
        return super.listTokenValues(token, otherTokenValues);
    }

    @Override
    protected String[] listTokenValues(String pattern, String token) {
        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return new String[0];
        }
        if (IvyPatternHelper.MODULE_KEY.equals(token) && !isM2compatible()) {
            return new String[0];
        }
        ensureConfigured(getSettings());

        // let's see if we should use maven metadata for this listing...
        if (IvyPatternHelper.REVISION_KEY.equals(token)
                && shouldUseMavenMetadata(getWholePattern())) {
            // now we must use metadata if available
            /*
             * we substitute tokens with ext token only in the m2 per module pattern, to match has
             * has been done in the given pattern
             */
            String partiallyResolvedM2PerModulePattern = IvyPatternHelper.substituteTokens(
                M2_PER_MODULE_PATTERN, Collections.singletonMap(IvyPatternHelper.EXT_KEY, "pom"));
            if (pattern.endsWith(partiallyResolvedM2PerModulePattern)) {
                /*
                 * the given pattern already contain resolved org and module, we just have to
                 * replace the per module pattern at the end by 'maven-metadata.xml' to have the
                 * maven metadata file location
                 */
                String metadataLocation = pattern.substring(0,
                    pattern.lastIndexOf(partiallyResolvedM2PerModulePattern))
                        + "maven-metadata.xml";
                List<?> revs = listRevisionsWithMavenMetadata(getRepository(), metadataLocation);
                if (revs != null) {
                    return revs.toArray(new String[revs.size()]);
                }
            } else {
                /*
                 * this is probably because the given pattern has been substituted with jar ext, if
                 * this resolver has optional module descriptors. But since we have to use maven
                 * metadata, we don't care about this case, maven metadata has already been used
                 * when looking for revisions with the pattern substituted with ext=xml for the
                 * "ivy" pattern.
                 */
                return new String[0];
            }
        }
        return super.listTokenValues(pattern, token);
    }

    @Override
    public OrganisationEntry[] listOrganisations() {
        return new OrganisationEntry[0];
    }

    @Override
    public ModuleEntry[] listModules(OrganisationEntry org) {
        if (isM2compatible()) {
            ensureConfigured(getSettings());
            return super.listModules(org);
        }
        return new ModuleEntry[0];
    }

    @Override
    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureConfigured(getSettings());
        return super.listRevisions(mod);
    }

    @Override
    protected ResolvedResource[] listResources(Repository repository, ModuleRevisionId mrid,
            String pattern, Artifact artifact) {
        if (shouldUseMavenMetadata(pattern)) {
            List<String> revs = listRevisionsWithMavenMetadata(repository, mrid.getModuleId()
                    .getAttributes());
            if (revs != null) {
                Message.debug("\tfound revs: " + revs);
                List<ResolvedResource> rres = new ArrayList<ResolvedResource>();
                for (String rev : revs) {
                    ModuleRevisionId historicalMrid = ModuleRevisionId.newInstance(mrid, rev);

                    String patternForRev = pattern;
                    if (rev.endsWith("SNAPSHOT")) {
                        String snapshotVersion = findSnapshotVersion(historicalMrid);
                        if (snapshotVersion != null) {
                            patternForRev = pattern.replaceFirst("\\-\\[revision\\]", "-"
                                    + snapshotVersion);
                        }
                    }
                    String resolvedPattern = IvyPatternHelper.substitute(patternForRev,
                        historicalMrid, artifact);
                    try {
                        Resource res = repository.getResource(resolvedPattern);
                        if (res != null) {
                            // we do not test if the resource actually exist here, it would cause
                            // a lot of checks which are not always necessary depending on the usage
                            // which is done of the returned ResolvedResource array
                            rres.add(new ResolvedResource(res, rev));
                        }
                    } catch (IOException e) {
                        Message.warn(
                            "impossible to get resource from name listed by maven-metadata.xml:"
                                    + rres, e);
                    }
                }
                return rres.toArray(new ResolvedResource[rres.size()]);
            } else {
                // maven metadata not available or something went wrong,
                // use default listing capability
                return super.listResources(repository, mrid, pattern, artifact);
            }
        } else {
            return super.listResources(repository, mrid, pattern, artifact);
        }
    }

    private List<String> listRevisionsWithMavenMetadata(Repository repository,
            Map<String, String> tokenValues) {
        String metadataLocation = IvyPatternHelper.substituteTokens(root
                + "[organisation]/[module]/maven-metadata.xml", tokenValues);
        return listRevisionsWithMavenMetadata(repository, metadataLocation);
    }

    private List<String> listRevisionsWithMavenMetadata(Repository repository,
            String metadataLocation) {
        List<String> revs = null;
        InputStream metadataStream = null;
        try {
            Resource metadata = repository.getResource(metadataLocation);
            if (metadata.exists()) {
                Message.verbose("\tlisting revisions from maven-metadata: " + metadata);
                final List<String> metadataRevs = new ArrayList<String>();
                metadataStream = metadata.openStream();
                XMLHelper.parse(metadataStream, null, new ContextualSAXHandler() {
                    @Override
                    public void endElement(String uri, String localName, String qName)
                            throws SAXException {
                        if ("metadata/versioning/versions/version".equals(getContext())) {
                            metadataRevs.add(getText().trim());
                        }
                        super.endElement(uri, localName, qName);
                    }
                }, null);
                revs = metadataRevs;
            } else {
                Message.verbose("\tmaven-metadata not available: " + metadata);
            }
        } catch (IOException e) {
            Message.verbose("impossible to access maven metadata file, ignored", e);
        } catch (SAXException e) {
            Message.verbose("impossible to parse maven metadata file, ignored", e);
        } catch (ParserConfigurationException e) {
            Message.verbose("impossible to parse maven metadata file, ignored", e);
        } finally {
            if (metadataStream != null) {
                try {
                    metadataStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
        return revs;
    }

    @Override
    protected void findTokenValues(Collection<String> names, List<String> patterns,
            Map<String, String> tokenValues, String token) {
        if (IvyPatternHelper.REVISION_KEY.equals(token)) {
            if (shouldUseMavenMetadata(getWholePattern())) {
                List<String> revs = listRevisionsWithMavenMetadata(getRepository(), tokenValues);
                if (revs != null) {
                    names.addAll(filterNames(revs));
                    return;
                }
            }
        }
        super.findTokenValues(names, patterns, tokenValues, token);
    }

    private boolean shouldUseMavenMetadata(String pattern) {
        return isUseMavenMetadata() && isM2compatible() && pattern.endsWith(M2_PATTERN);
    }

    @Override
    public String getTypeName() {
        return "ibiblio";
    }

    // override some methods to ensure configuration
    @Override
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        ensureConfigured(data.getSettings());
        return super.getDependency(dd, data);
    }

    @Override
    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        ensureConfigured(getSettings());
        return super.download(artifacts, options);
    }

    @Override
    public boolean exists(Artifact artifact) {
        ensureConfigured(getSettings());
        return super.exists(artifact);
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        ensureConfigured(getSettings());
        return super.locate(artifact);
    }

    @Override
    public List<String> getArtifactPatterns() {
        ensureConfigured(getSettings());
        return super.getArtifactPatterns();
    }

    public boolean isUsepoms() {
        return usepoms;
    }

    public void setUsepoms(boolean usepoms) {
        this.usepoms = usepoms;
        updateWholePattern();
    }

    public boolean isUseMavenMetadata() {
        return useMavenMetadata;
    }

    public void setUseMavenMetadata(boolean useMavenMetadata) {
        this.useMavenMetadata = useMavenMetadata;
    }

    @Override
    public void dumpSettings() {
        ensureConfigured(getSettings());
        super.dumpSettings();
        Message.debug("\t\troot: " + getRoot());
        Message.debug("\t\tpattern: " + getPattern());
        Message.debug("\t\tusepoms: " + usepoms);
        Message.debug("\t\tuseMavenMetadata: " + useMavenMetadata);
    }
}
