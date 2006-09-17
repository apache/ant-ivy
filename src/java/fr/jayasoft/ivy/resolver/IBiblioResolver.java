/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.util.IvyPatternHelper;

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
    
    // use poms if m2 compatible is true
    private boolean _usepoms = true;
    
    public IBiblioResolver() {
    }
    
    protected ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        if (isM2compatible() && isUsepoms()) {
            ModuleRevisionId mrid = dd.getDependencyRevisionId();
            mrid = convertM2IdForResourceSearch(mrid);
            ResolvedResource rres = findResourceUsingPatterns(mrid, getIvyPatterns(), DefaultArtifact.newPomArtifact(mrid, data.getDate()), getRMDParser(dd, data), data.getDate());
            return rres;
        } else {
            return null;
        }
    }
    
    protected void logIvyNotFound(ModuleRevisionId mrid) {
        if (isM2compatible() && isUsepoms()) {
            Artifact artifact = DefaultArtifact.newPomArtifact(mrid, null);
            logMdNotFound(mrid, artifact);
        }
    }

    public void setM2compatible(boolean m2compatible) {
        super.setM2compatible(m2compatible);
        if (m2compatible) {
            _root = "http://www.ibiblio.org/maven2/";
            _pattern = "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]";
            updateWholePattern();
        }
    }
    
    public void ensureConfigured(Ivy ivy) {
        if (ivy != null && (_root == null || _pattern == null)) {
            if (_root == null) {
                String root = ivy.getVariable("ivy.ibiblio.default.artifact.root");
                if (root != null) {
                    _root = root;
                } else {
                    ivy.configureRepositories(true);
                    _root = ivy.getVariable("ivy.ibiblio.default.artifact.root");
                }
            }
            if (_pattern == null) {
                String pattern = ivy.getVariable("ivy.ibiblio.default.artifact.pattern");
                if (pattern != null) {
                    _pattern = pattern;
                } else {
                    ivy.configureRepositories(false);
                    _pattern = ivy.getVariable("ivy.ibiblio.default.artifact.pattern");
                }
            }
            updateWholePattern();
        }
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
        ensureConfigured(getIvy());
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
        ensureConfigured(getIvy());
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
        ensureConfigured(getIvy());
    	return super.listTokenValues(token, otherTokenValues);
    }
    public OrganisationEntry[] listOrganisations() {
        return new OrganisationEntry[0];
    }
    public ModuleEntry[] listModules(OrganisationEntry org) {
    	if (isM2compatible()) {
            ensureConfigured(getIvy());
            return super.listModules(org);
    	}
        return new ModuleEntry[0];
    }    
    public RevisionEntry[] listRevisions(ModuleEntry mod) {
        ensureConfigured(getIvy());
        return super.listRevisions(mod);
    }
    public String getTypeName() {
        return "ibiblio";
    }

    // override some methods to ensure configuration    
    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        ensureConfigured(data.getIvy());
        return super.getDependency(dd, data);
    }
    
    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ensureConfigured(getIvy());
        return super.findArtifactRef(artifact, date);
    }
    
    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache, boolean useOrigin) {
        ensureConfigured(ivy);
        return super.download(artifacts, ivy, cache, useOrigin);
    }
    public boolean exists(Artifact artifact) {
        ensureConfigured(getIvy());
        return super.exists(artifact);
    }
    public List getArtifactPatterns() {
        ensureConfigured(getIvy());
        return super.getArtifactPatterns();
    }

	public boolean isUsepoms() {
		return _usepoms;
	}

	public void setUsepoms(boolean usepoms) {
		_usepoms = usepoms;
		updateWholePattern();
	}
}
