/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.XMLHelper;

/**
 * IvyRepResolver is a resolver which can be used to resolve dependencies found
 * in the ivy official repository for ivy files and ibiblio maven repository for the artifacts, 
 * or similar repositories.
 * For more flexibility with url and patterns, see {@link fr.jayasoft.ivy.resolver.URLResolver}.
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

    private void ensureArtifactConfigured(Ivy ivy) {
        if (ivy != null && (_artroot == null || _artpattern == null)) {
            if (_artroot == null) {
                String root = ivy.getVariable("ivy.ivyrep.default.artifact.root");
                if (root != null) {
                    _artroot = root;
                } else {
                    ivy.configureRepositories(true);
                    _artroot = ivy.getVariable("ivy.ivyrep.default.artifact.root");
                }
            }
            if (_artpattern == null) {
                String pattern = ivy.getVariable("ivy.ivyrep.default.artifact.pattern");
                if (pattern != null) {
                    _artpattern = pattern;
                } else {
                    ivy.configureRepositories(false);
                    _artpattern = ivy.getVariable("ivy.ivyrep.default.artifact.pattern");
                }
            }
            updateWholeArtPattern();
        }
    }

    private void ensureIvyConfigured(Ivy ivy) {
        if (ivy != null && (_ivyroot == null || _ivypattern == null)) {
            if (_ivyroot == null) {
                String root = ivy.getVariable("ivy.ivyrep.default.ivy.root");
                if (root != null) {
                    _ivyroot = root;
                } else {
                    ivy.configureRepositories(true);
                    _ivyroot = ivy.getVariable("ivy.ivyrep.default.ivy.root");
                }
            }
            if (_ivypattern == null) {
                String pattern = ivy.getVariable("ivy.ivyrep.default.ivy.pattern");
                if (pattern != null) {
                    _ivypattern = pattern;
                } else {
                    ivy.configureRepositories(false);
                    _ivypattern = ivy.getVariable("ivy.ivyrep.default.ivy.pattern");
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
        ensureIvyConfigured(getIvy());
        updateWholeIvyPattern();
    }
    public String getIvyroot() {
        return _ivyroot;
    }
    /**
     * Sets the root of the maven like repository.
     * The maven like repository is necessarily an http repository.
     * @param root the root of the maven like repository
     * @throws IllegalArgumentException if root does not start with "http://"
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
        ensureIvyConfigured(getIvy());
        updateWholeIvyPattern();
    }
    
    public void setM2compatible(boolean m2compatible) {
        if (m2compatible) {
            throw new IllegalArgumentException("ivyrep does not support maven2 compatibility. Please use ibiblio resolver instead, or even url or filesystem resolvers for more specific needs.");
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
        ensureArtifactConfigured(getIvy());
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
        ensureArtifactConfigured(getIvy());
        updateWholeArtPattern();
    }
    
    public OrganisationEntry[] listOrganisations() {
        ensureIvyConfigured(getIvy());
        try {
            URL content = new URL(_ivyroot + "content.xml");
            final List ret = new ArrayList();
            XMLHelper.parse(content, null, new DefaultHandler() {
                public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes attributes) throws SAXException {
                    if ("organisation".equals(qName)) {
                        String org = attributes.getValue("name");
                        if (org != null) {
                            ret.add(new OrganisationEntry(IvyRepResolver.this, org));
                        }
                    }
                }
            });
            return (OrganisationEntry[])ret.toArray(new OrganisationEntry[ret.size()]);
        } catch (MalformedURLException e) {
        } catch (Exception e) {
            Message.warn("unable to parse content.xml file on ivyrep: "+e.getMessage());
        }
        return super.listOrganisations();
    }

    // overwrite parent to use only ivy patterns (and not artifact ones, cause ibiblio is too slow to answer)
    public ModuleEntry[] listModules(OrganisationEntry org) {
        ensureIvyConfigured(getIvy());
        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());
        Collection names = findIvyNames(tokenValues, IvyPatternHelper.MODULE_KEY);
        ModuleEntry[] ret = new ModuleEntry[names.size()];
        int i =0;
        for (Iterator iter = names.iterator(); iter.hasNext(); i++) {
            String name = (String)iter.next();
            ret[i] = new ModuleEntry(org, name);
        }
        return ret;
    }
    
    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureIvyConfigured(getIvy());
        ensureArtifactConfigured(getIvy());
        return super.listRevisions(mod);
    }

    public String getTypeName() {
        return "ivyrep";
    }
    
    // override some methods to ensure configuration    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        ensureIvyConfigured(data.getIvy());
        return super.getDependency(dd, data);
    }
    
    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureArtifactConfigured(getIvy());
        return super.findArtifactRef(artifact, date);
    }
    
    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache, boolean useOrigin) {
        ensureArtifactConfigured(ivy);
        return super.download(artifacts, ivy, cache, useOrigin);
    }
    public boolean exists(Artifact artifact) {
        ensureArtifactConfigured(getIvy());
        return super.exists(artifact);
    }
    public List getIvyPatterns() {
        ensureIvyConfigured(getIvy());
        return super.getIvyPatterns();
    }
    public List getArtifactPatterns() {
        ensureArtifactConfigured(getIvy());
        return super.getArtifactPatterns();
    }
}
