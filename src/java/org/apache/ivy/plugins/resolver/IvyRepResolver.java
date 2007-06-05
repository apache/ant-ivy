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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
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

    private String _ivyroot = null;

    private String _ivypattern = null;

    private String _artroot = null;

    private String _artpattern = null;

    public IvyRepResolver() {
    }

    private void ensureArtifactConfigured(IvySettings settings) {
        if (settings != null && (_artroot == null || _artpattern == null)) {
            if (_artroot == null) {
                String root = settings.getVariable("ivy.ivyrep.default.artifact.root");
                if (root != null) {
                    _artroot = root;
                } else {
                    settings.configureRepositories(true);
                    _artroot = settings.getVariable("ivy.ivyrep.default.artifact.root");
                }
            }
            if (_artpattern == null) {
                String pattern = settings.getVariable("ivy.ivyrep.default.artifact.pattern");
                if (pattern != null) {
                    _artpattern = pattern;
                } else {
                    settings.configureRepositories(false);
                    _artpattern = settings.getVariable("ivy.ivyrep.default.artifact.pattern");
                }
            }
            updateWholeArtPattern();
        }
    }

    private void ensureIvyConfigured(IvySettings settings) {
        if (settings != null && (_ivyroot == null || _ivypattern == null)) {
            if (_ivyroot == null) {
                String root = settings.getVariable("ivy.ivyrep.default.ivy.root");
                if (root != null) {
                    _ivyroot = root;
                } else {
                    settings.configureRepositories(true);
                    _ivyroot = settings.getVariable("ivy.ivyrep.default.ivy.root");
                }
            }
            if (_ivypattern == null) {
                String pattern = settings.getVariable("ivy.ivyrep.default.ivy.pattern");
                if (pattern != null) {
                    _ivypattern = pattern;
                } else {
                    settings.configureRepositories(false);
                    _ivypattern = settings.getVariable("ivy.ivyrep.default.ivy.pattern");
                }
            }
            updateWholeIvyPattern();
        }
    }

    private String getWholeIvyPattern() {
        if (_ivyroot == null || _ivypattern == null) {
            return null;
        }
        return _ivyroot + _ivypattern;
    }

    private String getWholeArtPattern() {
        return _artroot + _artpattern;
    }

    public String getIvypattern() {
        return _ivypattern;
    }

    public void setIvypattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        _ivypattern = pattern;
        ensureIvyConfigured(getSettings());
        updateWholeIvyPattern();
    }

    public String getIvyroot() {
        return _ivyroot;
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
            _ivyroot = root + "/";
        } else {
            _ivyroot = root;
        }
        ensureIvyConfigured(getSettings());
        updateWholeIvyPattern();
    }

    public void setM2compatible(boolean m2compatible) {
        if (m2compatible) {
            throw new IllegalArgumentException(
                    "ivyrep does not support maven2 compatibility. Please use ibiblio resolver instead, or even url or filesystem resolvers for more specific needs.");
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
        return _artroot;
    }

    public String getArtpattern() {
        return _artpattern;
    }

    public void setArtpattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        _artpattern = pattern;
        ensureArtifactConfigured(getSettings());
        updateWholeArtPattern();
    }

    public void setArtroot(String root) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (!root.endsWith("/")) {
            _artroot = root + "/";
        } else {
            _artroot = root;
        }
        ensureArtifactConfigured(getSettings());
        updateWholeArtPattern();
    }

    public OrganisationEntry[] listOrganisations() {
        ensureIvyConfigured(getSettings());
        try {
            URL content = new URL(_ivyroot + "content.xml");
            final List ret = new ArrayList();
            XMLHelper.parse(content, null, new DefaultHandler() {
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
            return (OrganisationEntry[]) ret.toArray(new OrganisationEntry[ret.size()]);
        } catch (MalformedURLException e) {
        } catch (Exception e) {
            Message.warn("unable to parse content.xml file on ivyrep: " + e.getMessage());
        }
        return super.listOrganisations();
    }

    // overwrite parent to use only ivy patterns (and not artifact ones, cause ibiblio is too slow
    // to answer)
    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureIvyConfigured(getSettings());
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection names = findIvyNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i = 0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String) iter.next();
            ret[i] = new ModuleEntry(org, name);
        }
        return ret;
    }

    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureIvyConfigured(getSettings());
        ensureArtifactConfigured(getSettings());
        return super.listRevisions(mod);
    }

    public String getTypeName() {
        return "ivyrep";
    }

    // override some methods to ensure configuration
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        ensureIvyConfigured(data.getSettings());
        return super.getDependency(dd, data);
    }

    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureArtifactConfigured(getSettings());
        return super.findArtifactRef(artifact, date);
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        ensureArtifactConfigured(options.getSettings());
        return super.download(artifacts, options);
    }

    public boolean exists(Artifact artifact) {
        ensureArtifactConfigured(getSettings());
        return super.exists(artifact);
    }

    public List getIvyPatterns() {
        ensureIvyConfigured(getSettings());
        return super.getIvyPatterns();
    }

    public List getArtifactPatterns() {
        ensureArtifactConfigured(getSettings());
        return super.getArtifactPatterns();
    }
}
