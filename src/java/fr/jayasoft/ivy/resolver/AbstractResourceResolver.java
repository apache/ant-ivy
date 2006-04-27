/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.IvyPattern;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public abstract class AbstractResourceResolver extends BasicResolver {
    
    private static final Map IVY_ARTIFACT_ATTRIBUTES = new HashMap();
    static {
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.ARTIFACT_KEY, "ivy");
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.TYPE_KEY, "ivy");
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.EXT_KEY, "xml");
    }
    
    private List _ivyPatterns = new ArrayList(); // List (String pattern)
    private List _artifactPatterns = new ArrayList();  // List (String pattern)
    private boolean _m2compatible = false;

    
    public AbstractResourceResolver() {
    }

    protected ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, _ivyPatterns, DefaultArtifact.newIvyArtifact(mrid, data.getDate()), data.getDate());
    }

    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, _artifactPatterns, artifact, date);
    }

    /**
     * @deprecated 
     * @return
     */
    protected ResolvedResource findResourceUsingPatterns(ModuleRevisionId moduleRevision, List patternList, String artifact, String type, String ext, Date date) {
        ResolvedResource rres = null;
        for (Iterator iter = patternList.iterator(); iter.hasNext() && rres == null;) {
            String pattern = (String)iter.next();
            rres = findResourceUsingPattern(moduleRevision, pattern, artifact, type, ext, date);
        }
        return rres;
    }
    
    protected ResolvedResource findResourceUsingPatterns(ModuleRevisionId moduleRevision, List patternList, Artifact artifact, Date date) {
        ResolvedResource rres = null;
        for (Iterator iter = patternList.iterator(); iter.hasNext() && rres == null;) {
            String pattern = (String)iter.next();
            rres = findResourceUsingPattern(moduleRevision, pattern, artifact, date);
        }
        return rres;
    }
    
    /**
     * No need to implement that in post 1.4 dependency resolvers.
     * @deprecated 
     * @return
     */
    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, String artifact, String type, String ext, Date date) {
        // implemented for backward compatibility reason only.
        // in post 1.4 dependency resolvers this has no utility except to let old code use this old method
        // WARNING: if none of the two methods is overriden, this will result in a StackOverflow error !
        return findResourceUsingPattern(mrid, pattern, new DefaultArtifact(mrid, date, artifact, type, ext), date);
    }
    
    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, Date date) {
        // implemented for backward compatibility reason only.
        // MUST be overriden in post 1.4 dependency resolvers
        return findResourceUsingPattern(mrid, pattern, artifact.getName(), artifact.getType(), artifact.getExt(), date);
    }
    
    /**
     * No need to implement that in post 1.4 dependency resolvers.
     * @deprecated 
     * @return
     */
    protected ResolvedResource[] findAll(ModuleRevisionId mrid, String pattern, String artifact, String type, String ext) {
        // implemented for backward compatibility reason only.
        // in post 1.4 dependency resolvers this has no utility except to let old code use this old method
        // WARNING: if none of the two methods is overriden, this will result in a StackOverflow error !
        return findAll(mrid, pattern, new DefaultArtifact(mrid, null, artifact, type, ext));
    }

    protected ResolvedResource[] findAll(ModuleRevisionId mrid, String pattern, Artifact artifact) {
        // implemented for backward compatibility reason only.
        // MUST be overriden in post 1.4 dependency resolvers
        return findAll(mrid, pattern, artifact.getName(), artifact.getType(), artifact.getExt());
    }
    
    protected abstract long get(Resource resource, File dest) throws IOException;    

    /**
     * Output message to log indicating what have been done to look for an artifact which
     * has finally not been found
     * 
     * @param artifact the artifact which has not been found
     */
    protected void logIvyNotFound(ModuleRevisionId mrid) {
        Artifact artifact = DefaultArtifact.newIvyArtifact(mrid, null);
        logMdNotFound(mrid, artifact);
    }

    protected void logMdNotFound(ModuleRevisionId mrid, Artifact artifact) {
        String revisionToken = mrid.getRevision().startsWith("latest.")?"[any "+mrid.getRevision().substring("latest.".length())+"]":"["+mrid.getRevision()+"]";
        Artifact latestArtifact = new DefaultArtifact(new ModuleRevisionId(mrid.getModuleId(), revisionToken, mrid.getExtraAttributes()), null, artifact.getName(), artifact.getType(), artifact.getExt(), artifact.getExtraAttributes());
        if (_ivyPatterns.isEmpty()) {
            logIvyAttempt("no ivy pattern => no attempt to find module descriptor file for "+mrid);
        } else {
            for (Iterator iter = _ivyPatterns.iterator(); iter.hasNext();) {
                String pattern = (String)iter.next();
                String resolvedFileName = IvyPatternHelper.substitute(pattern, artifact);
                logIvyAttempt(resolvedFileName);
                if (!mrid.isExactRevision()) {
                    resolvedFileName = IvyPatternHelper.substitute(pattern, latestArtifact);
                    logIvyAttempt(resolvedFileName);
                }
            }
        }
    }

    /**
     * Output message to log indicating what have been done to look for an artifact which
     * has finally not been found
     * 
     * @param artifact the artifact which has not been found
     */
    protected void logArtifactNotFound(Artifact artifact) {
        if (_artifactPatterns.isEmpty()) {
            logArtifactAttempt(artifact, "no artifact pattern => no attempt to find artifact "+artifact);
        }
        for (Iterator iter = _artifactPatterns.iterator(); iter.hasNext();) {
            String pattern = (String)iter.next();
            String resolvedFileName = IvyPatternHelper.substitute(pattern, artifact);
            logArtifactAttempt(artifact, resolvedFileName);
        }
    }

    protected Collection findNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        names.addAll(findIvyNames(tokenValues, token));
        names.addAll(findArtifactNames(tokenValues, token));
        return names;
    }

    protected Collection findIvyNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues.put(IvyPatternHelper.ARTIFACT_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "xml");
        findTokenValues(names, getIvyPatterns(), tokenValues, token);
        getIvy().filterIgnore(names);
        return names;
    }
    
    protected Collection findArtifactNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues.put(IvyPatternHelper.ARTIFACT_KEY, tokenValues.get(IvyPatternHelper.MODULE_KEY));
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "jar");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "jar");
        findTokenValues(names, getArtifactPatterns(), tokenValues, token);
        getIvy().filterIgnore(names);
        return names;
    }

    // should be overridden by subclasses wanting to have listing features
    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
    }
    /**
     * example of pattern : ~/Workspace/[module]/[module].ivy.xml
     * @param pattern
     */
    public void addIvyPattern(String pattern) {
        _ivyPatterns.add(pattern);
    }

    public void addArtifactPattern(String pattern) {
        _artifactPatterns.add(pattern);
    }
    
    public List getIvyPatterns() {
        return Collections.unmodifiableList(_ivyPatterns);
    }

    public List getArtifactPatterns() {
        return Collections.unmodifiableList(_artifactPatterns);
    }
    protected void setIvyPatterns(List ivyPatterns) {
        _ivyPatterns = ivyPatterns;
    }
    protected void setArtifactPatterns(List artifactPatterns) {
        _artifactPatterns = artifactPatterns;
    }

    /*
     * Methods respecting ivy conf method specifications
     */
    public void addConfiguredIvy(IvyPattern p) {
        _ivyPatterns.add(p.getPattern());
    }

    public void addConfiguredArtifact(IvyPattern p) {
        _artifactPatterns.add(p.getPattern());
    }
    
    public void dumpConfig() {
        super.dumpConfig();
        Message.debug("\t\tm2compatible: "+isM2compatible());
        Message.debug("\t\tivy patterns:");
        for (ListIterator iter = getIvyPatterns().listIterator(); iter.hasNext();) {
            String p = (String)iter.next();
            Message.debug("\t\t\t"+p);
        }
        Message.debug("\t\tartifact patterns:");
        for (ListIterator iter = getArtifactPatterns().listIterator(); iter.hasNext();) {
            String p = (String)iter.next();
            Message.debug("\t\t\t"+p);
        }
    }

    public boolean isM2compatible() {
        return _m2compatible;
    }

    public void setM2compatible(boolean m2compatible) {
        _m2compatible = m2compatible;
    }

    protected ModuleRevisionId convertM2IdForResourceSearch(ModuleRevisionId mrid) {
        if (mrid.getOrganisation().indexOf('.') == -1) {
            return mrid;
        }
        return ModuleRevisionId.newInstance(mrid.getOrganisation().replace('.', '/'), mrid.getName(), mrid.getRevision(), mrid.getExtraAttributes());
    }

}
