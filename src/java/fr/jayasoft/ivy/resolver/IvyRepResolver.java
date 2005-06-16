/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
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

    public void setIvy(Ivy ivy) {
        super.setIvy(ivy);
        ivy.configureRepositories();
        if (_ivyroot == null) {
            String root = ivy.getVariable("ivy.ivyrep.default.ivy.root");
            if (root != null) {
                _ivyroot = root;
            } else {
                _ivyroot = DEFAULT_IVYROOT;
            }
        }
        if (_ivypattern == null) {
            String pattern = ivy.getVariable("ivy.ivyrep.default.ivy.pattern");
            if (pattern != null) {
                _ivypattern = pattern;
            } else {
                _ivypattern = DEFAULT_IVYPATTERN;
            }
        }
        if (_artroot == null) {
            String root = ivy.getVariable("ivy.ivyrep.default.artifact.root");
            if (root != null) {
                _artroot = root;
            } else {
                _artroot = IBiblioResolver.DEFAULT_ROOT;
            }
        }
        if (_artpattern == null) {
            String pattern = ivy.getVariable("ivy.ivyrep.default.artifact.pattern");
            if (pattern != null) {
                _artpattern = pattern;
            } else {
                _artpattern = IBiblioResolver.DEFAULT_PATTERN;
            }
        }
        updateWholeIvyPattern();
        updateWholeArtPattern();
    }

    private String getWholeIvyPattern() {
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
        updateWholeIvyPattern();
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
        updateWholeArtPattern();
    }
    
    public OrganisationEntry[] listOrganisations() {
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

    public String getTypeName() {
        return "ivyrep";
    }
}
