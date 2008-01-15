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
import java.util.Set;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.settings.IvyPattern;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.resolver.util.MDResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;

/**
 *
 */
public abstract class AbstractResourceResolver extends BasicResolver {

    private static final Map IVY_ARTIFACT_ATTRIBUTES = new HashMap();
    static {
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.ARTIFACT_KEY, "ivy");
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.TYPE_KEY, "ivy");
        IVY_ARTIFACT_ATTRIBUTES.put(IvyPatternHelper.EXT_KEY, "xml");
    }

    private List ivyPatterns = new ArrayList(); // List (String pattern)

    private List artifactPatterns = new ArrayList(); // List (String pattern)

    private boolean m2compatible = false;

    public AbstractResourceResolver() {
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, ivyPatterns, DefaultArtifact.newIvyArtifact(mrid,
            data.getDate()), getRMDParser(dd, data), data.getDate());
    }

    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, artifactPatterns, artifact,
            getDefaultRMDParser(artifact.getModuleRevisionId().getModuleId()), date);
    }

    protected ResolvedResource findResourceUsingPatterns(ModuleRevisionId moduleRevision,
            List patternList, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        List resolvedResources = new ArrayList();
        Set foundRevisions = new HashSet();
        boolean dynamic = getSettings().getVersionMatcher().isDynamic(moduleRevision);
        boolean stop = false;
        for (Iterator iter = patternList.iterator(); iter.hasNext() && !stop;) {
            String pattern = (String) iter.next();
            ResolvedResource rres = findResourceUsingPattern(
                moduleRevision, pattern, artifact, rmdparser, date);
            if ((rres != null) && !foundRevisions.contains(rres.getRevision())) {
                // only add the first found ResolvedResource for each revision
                foundRevisions.add(rres.getRevision());
                resolvedResources.add(rres);
                stop = !dynamic; // stop iterating if we are not searching a dynamic revision
            }
        }

        if (resolvedResources.size() > 1) {
            ResolvedResource[] rress = (ResolvedResource[]) resolvedResources
                    .toArray(new ResolvedResource[resolvedResources.size()]);
            return findResource(rress, getName(), getLatestStrategy(), getSettings()
                    .getVersionMatcher(), rmdparser, moduleRevision, date);
        } else if (resolvedResources.size() == 1) {
            return (ResolvedResource) resolvedResources.get(0);
        } else {
            return null;
        }
    }

    protected abstract ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid,
            String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date);

    public ResolvedResource findResource(ResolvedResource[] rress, String name,
            LatestStrategy strategy, VersionMatcher versionMatcher, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        ResolvedResource found = null;
        List sorted = strategy.sort(rress);
        List rejected = new ArrayList();
        List foundBlacklisted = new ArrayList();
        IvyContext context = IvyContext.getContext();
        
        for (ListIterator iter = sorted.listIterator(sorted.size()); iter.hasPrevious();) {
            ResolvedResource rres = (ResolvedResource) iter.previous();
            if (filterNames(new ArrayList(Collections.singleton(rres.getRevision()))).isEmpty()) {
                Message.debug("\t" + name + ": filtered by name: " + rres);
                continue;
            }
            if ((date != null && rres.getLastModified() > date.getTime())) {
                Message.verbose("\t" + name + ": too young: " + rres);
                rejected.add(rres.getRevision() + " (" + rres.getLastModified() + ")");
                continue;
            }
            ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mrid, rres.getRevision());
            
            ResolveData data = context.getResolveData();
            if (data != null
                    && data.getReport() != null 
                    && data.isBlacklisted(data.getReport().getConfiguration(), foundMrid)) {
                Message.debug("\t" + name + ": blacklisted: " + rres);
                rejected.add(rres.getRevision() + " (blacklisted)");
                foundBlacklisted.add(foundMrid);
                continue;
            }
            
            if (!versionMatcher.accept(mrid, foundMrid)) {
                Message.debug("\t" + name + ": rejected by version matcher: " + rres);
                rejected.add(rres.getRevision());
                continue;
            }
            if (versionMatcher.needModuleDescriptor(mrid, foundMrid)) {
                ResolvedResource r = rmdparser.parse(rres.getResource(), rres.getRevision());
                if (r == null) {
                    Message.debug("\t" + name 
                        + ": impossible to get module descriptor resource: " + rres);
                    rejected.add(rres.getRevision() + " (no or bad MD)");
                    continue;
                }
                ModuleDescriptor md = ((MDResolvedResource) r).getResolvedModuleRevision()
                        .getDescriptor();
                if (md.isDefault()) {
                    Message.debug("\t" + name + ": default md rejected by version matcher" 
                            + "requiring module descriptor: " + rres);
                    rejected.add(rres.getRevision() + " (MD)");
                    continue;
                } else if (!versionMatcher.accept(mrid, md)) {
                    Message.debug("\t" + name + ": md rejected by version matcher: " + rres);
                    rejected.add(rres.getRevision() + " (MD)");
                    continue;
                } else {
                    found = r;
                }
            } else {
                found = rres;
            }

            if (found != null) {
                if (!found.getResource().exists()) {
                    Message.debug("\t" + name + ": resource not reachable for " + mrid + ": res="
                            + found.getResource());
                    logAttempt(found.getResource().toString());
                    continue;
                }
                break;
            }
        }
        if (found == null && !rejected.isEmpty()) {
            logAttempt(rejected.toString());
        }
        if (found == null && !foundBlacklisted.isEmpty()) {
            // all acceptable versions have been blacklisted, this means that an unsolvable conflict
            // has been found
            DependencyDescriptor dd = context.getDependencyDescriptor();
            IvyNode parentNode = context.getResolveData().getNode(dd.getParentRevisionId());
            ConflictManager cm = parentNode.getConflictManager(mrid.getModuleId());
            cm.handleAllBlacklistedRevisions(dd, foundBlacklisted);
        }

        return found;
    }

    protected Collection findNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        names.addAll(findIvyNames(tokenValues, token));
        if (isAllownomd()) {
            names.addAll(findArtifactNames(tokenValues, token));
        }
        return names;
    }

    protected Collection findIvyNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues.put(IvyPatternHelper.ARTIFACT_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "xml");
        findTokenValues(names, getIvyPatterns(), tokenValues, token);
        filterNames(names);
        return names;
    }

    protected Collection findArtifactNames(Map tokenValues, String token) {
        Collection names = new HashSet();
        tokenValues = new HashMap(tokenValues);
        tokenValues
                .put(IvyPatternHelper.ARTIFACT_KEY, tokenValues.get(IvyPatternHelper.MODULE_KEY));
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "jar");
        tokenValues.put(IvyPatternHelper.EXT_KEY, "jar");
        findTokenValues(names, getArtifactPatterns(), tokenValues, token);
        filterNames(names);
        return names;
    }

    /**
     * Filters names before returning them in the findXXXNames or findTokenValues method.
     * <p>
     * Remember to call the super implementation when overriding this method.
     * </p>
     * 
     * @param names
     *            the list to filter.
     * @return the filtered list
     */
    protected Collection filterNames(Collection names) {
        getSettings().filterIgnore(names);
        return names;
    }

    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
        //to be overridden by subclasses wanting to have listing features
    }

    /**
     * example of pattern : ~/Workspace/[module]/[module].ivy.xml
     * 
     * @param pattern
     */
    public void addIvyPattern(String pattern) {
        ivyPatterns.add(pattern);
    }

    public void addArtifactPattern(String pattern) {
        artifactPatterns.add(pattern);
    }

    public List getIvyPatterns() {
        return Collections.unmodifiableList(ivyPatterns);
    }

    public List getArtifactPatterns() {
        return Collections.unmodifiableList(artifactPatterns);
    }

    protected void setIvyPatterns(List patterns) {
        ivyPatterns = patterns;
    }

    protected void setArtifactPatterns(List patterns) {
        artifactPatterns = patterns;
    }

    /*
     * Methods respecting ivy conf method specifications
     */
    public void addConfiguredIvy(IvyPattern p) {
        ivyPatterns.add(p.getPattern());
    }

    public void addConfiguredArtifact(IvyPattern p) {
        artifactPatterns.add(p.getPattern());
    }

    public void dumpSettings() {
        super.dumpSettings();
        Message.debug("\t\tm2compatible: " + isM2compatible());
        Message.debug("\t\tivy patterns:");
        for (ListIterator iter = getIvyPatterns().listIterator(); iter.hasNext();) {
            String p = (String) iter.next();
            Message.debug("\t\t\t" + p);
        }
        Message.debug("\t\tartifact patterns:");
        for (ListIterator iter = getArtifactPatterns().listIterator(); iter.hasNext();) {
            String p = (String) iter.next();
            Message.debug("\t\t\t" + p);
        }
    }

    public boolean isM2compatible() {
        return m2compatible;
    }

    public void setM2compatible(boolean compatible) {
        m2compatible = compatible;
    }

    protected ModuleRevisionId convertM2IdForResourceSearch(ModuleRevisionId mrid) {
        if (mrid.getOrganisation().indexOf('.') == -1) {
            return mrid;
        }
        return ModuleRevisionId.newInstance(mrid.getOrganisation().replace('.', '/'), mrid
                .getName(), mrid.getBranch(), mrid.getRevision(), mrid.getExtraAttributes());
    }

}
