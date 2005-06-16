/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.util.Collections;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;

/**
 * IBiblioResolver is a resolver which can be used to resolve dependencies found
 * in the ibiblio maven repository, or similar repositories.
 * For more flexibility with url and patterns, see {@link fr.jayasoft.ivy.resolver.URLResolver}.
 */
public class IBiblioResolver extends URLResolver {
    public static final String DEFAULT_PATTERN = "[module]/[type]s/[artifact]-[revision].[ext]";
    public static final String DEFAULT_ROOT = "http://www.ibiblio.org/maven/";
    private String _root = null;
    private String _pattern = null;
    
    public IBiblioResolver() {
    }
    
    public void setIvy(Ivy ivy) {
        super.setIvy(ivy);
        ivy.configureRepositories();
        if (_root == null) {
            String root = ivy.getVariable("ivy.ibiblio.default.artifact.root");
            if (root != null) {
                _root = root;
            } else {
                _root = DEFAULT_ROOT;
            }
        }
        if (_pattern == null) {
            String pattern = ivy.getVariable("ivy.ibiblio.default.artifact.pattern");
            if (pattern != null) {
                _pattern = pattern;
            } else {
                _pattern = DEFAULT_PATTERN;
            }
        }
        updateWholePattern();
    }

    private String getWholePattern() {
        return _root + _pattern;
    }    
    public String getPattern() {
        return _pattern;
    }
    public void setPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern must not be null");
        }
        _pattern = pattern;
        updateWholePattern();
    }
    public String getRoot() {
        return _root;
    }
    /**
     * Sets the root of the maven like repository.
     * The maven like repository is necessarily an http repository.
     * @param root the root of the maven like repository
     * @throws IllegalArgumentException if root does not start with "http://"
     */
    public void setRoot(String root) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (!root.endsWith("/")) {
            _root = root + "/";
        } else {
            _root = root;
        }
        updateWholePattern();
    }
    
    private void updateWholePattern() {
        setArtifactPatterns(Collections.singletonList(getWholePattern()));
    }
    public void publish(Artifact artifact, File src) {
        throw new UnsupportedOperationException("publish not supported by IBiblioResolver");
    }
    
    public OrganisationEntry[] listOrganisations() {
        return null;
    }
    public ModuleEntry[] listModules(OrganisationEntry org) {
        return null;
    }    
    public String getTypeName() {
        return "ibiblio";
    }

}
