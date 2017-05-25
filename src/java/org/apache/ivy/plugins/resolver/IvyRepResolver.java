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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * IvyRepResolver is a resolver which can be used to resolve dependencies found in the ivy official
 * repository for ivy files and ibiblio maven repository for the artifacts, or similar repositories.
 * For more flexibility with url and patterns, see
 * {@link org.apache.ivy.plugins.resolver.URLResolver}.
 */
public class IvyRepResolver extends URLResolver {
    public static final String DEFAULT_IVYPATTERN = "[organisation]/[module]/ivy-[revision].xml";

    public static final String DEFAULT_IVYROOT = "http://ivyrep.jayasoft.org/";

    private String ivyroot = null;

    private String ivypattern = null;

    private String artroot = null;

    private String artpattern = null;

    public IvyRepResolver() {
    }

    private void ensureArtifactConfigured(ResolverSettings settings) {
        if (settings != null && (artroot == null || artpattern == null)) {
            if (artroot == null) {
                String root = settings.getVariable("ivy.ivyrep.default.artifact.root");
                if (root != null) {
                    artroot = root;
                } else {
                    settings.configureRepositories(true);
                    artroot = settings.getVariable("ivy.ivyrep.default.artifact.root");
                }
            }
            if (artpattern == null) {
                String pattern = settings.getVariable("ivy.ivyrep.default.artifact.pattern");
                if (pattern != null) {
                    artpattern = pattern;
                } else {
                    settings.configureRepositories(false);
                    artpattern = settings.getVariable("ivy.ivyrep.default.artifact.pattern");
                }
            }
            updateWholeArtPattern();
        }
    }

    private void ensureIvyConfigured(ResolverSettings settings) {
        if (settings != null && (ivyroot == null || ivypattern == null)) {
            if (ivyroot == null) {
                String root = settings.getVariable("ivy.ivyrep.default.ivy.root");
                if (root != null) {
                    ivyroot = root;
                } else {
                    throw new IllegalStateException("ivyroot is mandatory on IvyRepResolver. "
                            + "Make sure to set it in your settings, before setting ivypattern "
                            + "if you wish to set ivypattern too.");
                }
            }
            if (ivypattern == null) {
                String pattern = settings.getVariable("ivy.ivyrep.default.ivy.pattern");
                if (pattern != null) {
                    ivypattern = pattern;
                } else {
                    settings.configureRepositories(false);
                    ivypattern = settings.getVariable("ivy.ivyrep.default.ivy.pattern");
                }
            }
            updateWholeIvyPattern();
        }
    }

    private String getWholeIvyPattern() {
        if (ivyroot == null || ivypattern == null) {
            return null;
        }
        return ivyroot + ivypattern;
    }

    private String getWholeArtPattern() {
        return artroot + artpattern;
    }

    public String getIvypattern() {
        return ivypattern;
    }

    public void setIvypattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        ivypattern = pattern;
        ensureIvyConfigured(getSettings());
        updateWholeIvyPattern();
    }

    public String getIvyroot() {
        return ivyroot;
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
    public void setIvyroot(String root) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (!root.endsWith("/")) {
            ivyroot = root + "/";
        } else {
            ivyroot = root;
        }
        ensureIvyConfigured(getSettings());
        updateWholeIvyPattern();
    }

    @Override
    public void setM2compatible(boolean m2compatible) {
        if (m2compatible) {
            throw new IllegalArgumentException(
                    "ivyrep does not support maven2 compatibility. "
                            + "Please use ibiblio resolver instead, or even url or filesystem resolvers for"
                            + " more specific needs.");
        }
    }

    private void updateWholeIvyPattern() {
        setIvyPatterns(Collections.singletonList(getWholeIvyPattern()));
    }

    private void updateWholeArtPattern() {
        setArtifactPatterns(Collections.singletonList(getWholeArtPattern()));
    }

    public void publish(Artifact artifact, File src) {
        throw new UnsupportedOperationException("publish not supported by IBiblioResolver");
    }

    public String getArtroot() {
        return artroot;
    }

    public String getArtpattern() {
        return artpattern;
    }

    public void setArtpattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        artpattern = pattern;
        ensureArtifactConfigured(getSettings());
        updateWholeArtPattern();
    }

    public void setArtroot(String root) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (!root.endsWith("/")) {
            artroot = root + "/";
        } else {
            artroot = root;
        }
        ensureArtifactConfigured(getSettings());
        updateWholeArtPattern();
    }

    @Override
    public OrganisationEntry[] listOrganisations() {
        ensureIvyConfigured(getSettings());
        try {
            URL content = new URL(ivyroot + "content.xml");
            final List<OrganisationEntry> ret = new ArrayList<OrganisationEntry>();
            XMLHelper.parse(content, null, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName,
                        org.xml.sax.Attributes attributes) throws SAXException {
                    if ("organisation".equals(qName)) {
                        String org = attributes.getValue("name");
                        if (org != null) {
                            ret.add(new OrganisationEntry(IvyRepResolver.this, org));
                        }
                    }
                }
            });
            return ret.toArray(new OrganisationEntry[ret.size()]);
        } catch (MalformedURLException e) {
            // ???
        } catch (Exception e) {
            Message.warn("unable to parse content.xml file on ivyrep", e);
        }
        return super.listOrganisations();
    }

    // overwrite parent to use only ivy patterns (and not artifact ones, cause ibiblio is too slow
    // to answer)
    @Override
    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureIvyConfigured(getSettings());
        Map<String, String> tokenValues = new HashMap<String, String>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection<String> names = findIvyNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i = 0;
        for (String name : names) {
            ret[i] = new ModuleEntry(org, name);
        }
        return ret;
    }

    @Override
    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureIvyConfigured(getSettings());
        ensureArtifactConfigured(getSettings());
        return super.listRevisions(mod);
    }

    @Override
    public String getTypeName() {
        return "ivyrep";
    }

    // override some methods to ensure configuration
    @Override
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        ensureIvyConfigured(data.getSettings());
        return super.getDependency(dd, data);
    }

    @Override
    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureArtifactConfigured(getSettings());
        return super.findArtifactRef(artifact, date);
    }

    @Override
    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        ensureArtifactConfigured(getSettings());
        return super.download(artifacts, options);
    }

    @Override
    public boolean exists(Artifact artifact) {
        ensureArtifactConfigured(getSettings());
        return super.exists(artifact);
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        ensureArtifactConfigured(getSettings());
        return super.locate(artifact);
    }

    @Override
    public List<String> getIvyPatterns() {
        ensureIvyConfigured(getSettings());
        return super.getIvyPatterns();
    }

    @Override
    public List<String> getArtifactPatterns() {
        ensureArtifactConfigured(getSettings());
        return super.getArtifactPatterns();
    }
}
