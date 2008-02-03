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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.IvyPatternHelper;
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
    private static final String M2_PATTERN 
        = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

    public static final String DEFAULT_PATTERN = "[module]/[type]s/[artifact]-[revision].[ext]";

    public static final String DEFAULT_ROOT = "http://www.ibiblio.org/maven/";

    private String root = null;

    private String pattern = null;

    // use poms if m2 compatible is true
    private boolean usepoms = true;
    
    // use maven-metadata.xml is exists to list revisions
    private boolean useMavenMetadata = true;

    public IBiblioResolver() {
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        if (isM2compatible() && isUsepoms()) {
            ModuleRevisionId mrid = dd.getDependencyRevisionId();
            mrid = convertM2IdForResourceSearch(mrid);
            ResolvedResource rres = findResourceUsingPatterns(mrid, getIvyPatterns(),
                DefaultArtifact.newPomArtifact(mrid, data.getDate()), getRMDParser(dd, data), data
                        .getDate());
            return rres;
        } else {
            return null;
        }
    }

    public void setM2compatible(boolean m2compatible) {
        super.setM2compatible(m2compatible);
        if (m2compatible) {
            if (root == null) {
                root = "http://repo1.maven.org/maven2/";
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
        }
        setArtifactPatterns(Collections.singletonList(getWholePattern()));
    }

    public void publish(Artifact artifact, File src) {
        throw new UnsupportedOperationException("publish not supported by IBiblioResolver");
    }

    // we do not allow to list organisations on ibiblio, nor modules in ibiblio 1
    public String[] listTokenValues(String token, Map otherTokenValues) {
        if (IvyPatternHelper.ORGANISATION_KEY.equals(token)) {
            return new String[0];
        }
        if (IvyPatternHelper.MODULE_KEY.equals(token) && !isM2compatible()) {
            return new String[0];
        }
        ensureConfigured(getSettings());
        return super.listTokenValues(token, otherTokenValues);
    }

    public OrganisationEntry[] listOrganisations() {
        return new OrganisationEntry[0];
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        if (isM2compatible()) {
            ensureConfigured(getSettings());
            return super.listModules(org);
        }
        return new ModuleEntry[0];
    }

    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureConfigured(getSettings());
        return super.listRevisions(mod);
    }
    
    protected ResolvedResource[] listResources(
            Repository repository, ModuleRevisionId mrid, String pattern, Artifact artifact) {
        if (isUseMavenMetadata() && isM2compatible() && pattern.endsWith(M2_PATTERN)) {
            // use maven-metadata.xml if it exists
            InputStream metadataStream = null;
            try {
                String metadataLocation = IvyPatternHelper.substitute(
                    root + "[organisation]/[module]/maven-metadata.xml", mrid);
                Resource metadata = repository.getResource(metadataLocation);
                if (metadata.exists()) {
                    Message.verbose("\tlisting revisions from maven-metadata: " + metadata);
                    final List revs = new ArrayList();
                    metadataStream = metadata.openStream();
                    XMLHelper.parse(metadataStream, null, new ContextualSAXHandler() {
                        public void endElement(String uri, String localName, String qName) 
                                throws SAXException {
                            if ("metadata/versioning/versions/version".equals(getContext())) {
                                revs.add(getText().trim());
                            }
                            super.endElement(uri, localName, qName);
                        }
                    }, null);
                    Message.debug("\tfound revs: " + revs);
                    List rres = new ArrayList();
                    for (Iterator iter = revs.iterator(); iter.hasNext();) {
                        String rev = (String) iter.next();
                        String resolvedPattern = IvyPatternHelper.substitute(
                            pattern, ModuleRevisionId.newInstance(mrid, rev), artifact);
                        try {
                            Resource res = repository.getResource(resolvedPattern);
                            if ((res != null) && res.exists()) {
                                rres.add(new ResolvedResource(res, rev));
                            }
                        } catch (IOException e) {
                            Message.warn(
                                "impossible to get resource from name listed by maven-metadata.xml:"
                                    + rres + ": " + e.getMessage());
                        }
                    }
                    return (ResolvedResource[]) rres.toArray(new ResolvedResource[rres.size()]);
                } else {
                    Message.verbose("\tmaven-metadata not available: " + metadata);
                }
            } catch (IOException e) {
                Message.verbose(
                    "impossible to access maven metadata file, ignored: " + e.getMessage());
            } catch (SAXException e) {
                Message.verbose(
                    "impossible to parse maven metadata file, ignored: " + e.getMessage());
            } catch (ParserConfigurationException e) {
                Message.verbose(
                    "impossible to parse maven metadata file, ignored: " + e.getMessage());
            } finally {
                if (metadataStream != null) {
                    try {
                        metadataStream.close();
                    } catch (IOException e) {
                        // ignored
                    }
                }
            }
            // maven metadata not available or something went wrong, use default listing capability
            return super.listResources(repository, mrid, pattern, artifact);
        } else {
            return super.listResources(repository, mrid, pattern, artifact);
        }
    }


    public String getTypeName() {
        return "ibiblio";
    }

    // override some methods to ensure configuration
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        ensureConfigured(data.getSettings());
        return super.getDependency(dd, data);
    }

    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureConfigured(getSettings());
        return super.findArtifactRef(artifact, date);
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        ensureConfigured(getSettings());
        return super.download(artifacts, options);
    }

    public boolean exists(Artifact artifact) {
        ensureConfigured(getSettings());
        return super.exists(artifact);
    }

    public List getArtifactPatterns() {
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

    public void dumpSettings() {
        ensureConfigured(getSettings());
        super.dumpSettings();
        Message.debug("\t\troot: " + getRoot());
        Message.debug("\t\tpattern: " + getPattern());
        Message.debug("\t\tusepoms: " + usepoms);
        Message.debug("\t\tuseMavenMetadata: " + useMavenMetadata);
    }
}
