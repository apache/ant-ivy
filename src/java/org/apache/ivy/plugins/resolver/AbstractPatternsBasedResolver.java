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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.settings.IvyPattern;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.util.Message;

/**
 *
 */
public abstract class AbstractPatternsBasedResolver extends BasicResolver {

    private List ivyPatterns = new ArrayList(); // List (String pattern)

    private List artifactPatterns = new ArrayList(); // List (String pattern)

    private boolean m2compatible = false;

    public AbstractPatternsBasedResolver() {
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, ivyPatterns,
            DefaultArtifact.newIvyArtifact(mrid, data.getDate()), getRMDParser(dd, data),
            data.getDate());
    }

    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, artifactPatterns, artifact,
            getDefaultRMDParser(artifact.getModuleRevisionId().getModuleId()), date);
    }

    public ResolvedResource findResource(ResolvedResource[] rress, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        if (isM2compatible()) {
            // convert 'M2'-organisation back to 'Ivy'-organisation
            mrid = convertM2ResourceSearchIdToNormal(mrid);
        }
        return super.findResource(rress, rmdparser, mrid, date);
    }

    protected ResolvedResource findResourceUsingPatterns(ModuleRevisionId moduleRevision,
            List patternList, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        List resolvedResources = new ArrayList();
        Set foundRevisions = new HashSet();
        boolean dynamic = getSettings().getVersionMatcher().isDynamic(moduleRevision);
        boolean stop = false;
        for (Iterator iter = patternList.iterator(); iter.hasNext() && !stop;) {
            String pattern = (String) iter.next();
            ResolvedResource rres = findResourceUsingPattern(moduleRevision, pattern, artifact,
                rmdparser, date);
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
            return findResource(rress, rmdparser, moduleRevision, date);
        } else if (resolvedResources.size() == 1) {
            return (ResolvedResource) resolvedResources.get(0);
        } else {
            return null;
        }
    }

    protected abstract ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid,
            String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date);

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
        if (isM2compatible()) {
            convertM2TokenValuesForResourceSearch(tokenValues);
        }
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
        if (isM2compatible()) {
            convertM2TokenValuesForResourceSearch(tokenValues);
        }
        findTokenValues(names, getArtifactPatterns(), tokenValues, token);
        filterNames(names);
        return names;
    }

    public Map[] listTokenValues(String[] tokens, Map criteria) {
        Set result = new LinkedHashSet();

        // use ivy patterns
        List ivyPatterns = getIvyPatterns();
        Map tokenValues = new HashMap(criteria);
        tokenValues.put(IvyPatternHelper.TYPE_KEY, "ivy");
        tokenValues.put(IvyPatternHelper.EXT_KEY, getModuleDescriptorExtension());
        if (isM2compatible()) {
            convertM2TokenValuesForResourceSearch(tokenValues);
        }
        for (Iterator it = ivyPatterns.iterator(); it.hasNext();) {
            String ivyPattern = (String) it.next();
            result.addAll(resolveTokenValues(tokens, ivyPattern, tokenValues, false));
        }

        if (isAllownomd()) {
            List artifactPatterns = getArtifactPatterns();
            tokenValues = new HashMap(criteria);
            tokenValues.put(IvyPatternHelper.TYPE_KEY, "jar");
            tokenValues.put(IvyPatternHelper.EXT_KEY, "jar");
            if (isM2compatible()) {
                convertM2TokenValuesForResourceSearch(tokenValues);
            }
            for (Iterator it = artifactPatterns.iterator(); it.hasNext();) {
                String artifactPattern = (String) it.next();
                result.addAll(resolveTokenValues(tokens, artifactPattern, tokenValues, true));
            }
        }

        return (Map[]) result.toArray(new Map[result.size()]);
    }

    protected String getModuleDescriptorExtension() {
        return "xml";
    }

    private Set resolveTokenValues(String[] tokens, String pattern, Map criteria, boolean noMd) {
        Set result = new LinkedHashSet();
        Set tokenSet = new HashSet(Arrays.asList(tokens));

        Map tokenValues = new HashMap();
        for (Iterator it = criteria.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                tokenValues.put(key, value);
            }
        }

        if (tokenSet.isEmpty()) {
            // no more tokens to resolve
            result.add(tokenValues);
            return result;
        }

        String partiallyResolvedPattern = IvyPatternHelper.substituteTokens(pattern, tokenValues);
        String token = IvyPatternHelper.getFirstToken(partiallyResolvedPattern);
        if ((token == null) && exist(partiallyResolvedPattern)) {
            // no more tokens to resolve
            result.add(tokenValues);
            return result;
        }

        tokenSet.remove(token);

        Matcher matcher = null;
        Object criteriaForToken = criteria.get(token);
        if (criteriaForToken instanceof Matcher) {
            matcher = (Matcher) criteriaForToken;
        }

        String[] values = listTokenValues(partiallyResolvedPattern, token);
        if (values == null) {
            return result;
        }

        List vals = new ArrayList(Arrays.asList(values));
        filterNames(vals);

        for (Iterator it = vals.iterator(); it.hasNext();) {
            String value = (String) it.next();
            if ((matcher != null) && !matcher.matches(value)) {
                continue;
            }

            tokenValues.put(token, value);
            String moreResolvedPattern = IvyPatternHelper.substituteTokens(
                partiallyResolvedPattern, tokenValues);

            Map newCriteria = new HashMap(criteria);
            newCriteria.put(token, value);
            if (noMd && "artifact".equals(token)) {
                newCriteria.put("module", value);
            } else if (noMd && "module".equals(token)) {
                newCriteria.put("artifact", value);
            }
            result.addAll(resolveTokenValues(
                (String[]) tokenSet.toArray(new String[tokenSet.size()]), moreResolvedPattern,
                newCriteria, noMd));
        }

        return result;
    }

    protected abstract String[] listTokenValues(String pattern, String token);

    protected abstract boolean exist(String path);

    protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token) {
        // to be overridden by subclasses wanting to have listing features
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

    protected ModuleRevisionId convertM2ResourceSearchIdToNormal(ModuleRevisionId mrid) {
        if (mrid.getOrganisation() == null || mrid.getOrganisation().indexOf('/') == -1) {
            return mrid;
        }
        return ModuleRevisionId.newInstance(mrid.getOrganisation().replace('/', '.'),
            mrid.getName(), mrid.getBranch(), mrid.getRevision(),
            mrid.getQualifiedExtraAttributes());
    }

    protected ModuleRevisionId convertM2IdForResourceSearch(ModuleRevisionId mrid) {
        if (mrid.getOrganisation() == null || mrid.getOrganisation().indexOf('.') == -1) {
            return mrid;
        }
        return ModuleRevisionId.newInstance(mrid.getOrganisation().replace('.', '/'),
            mrid.getName(), mrid.getBranch(), mrid.getRevision(),
            mrid.getQualifiedExtraAttributes());
    }

    protected String convertM2OrganizationForResourceSearch(String org) {
        return org.replace('.', '/');
    }

    protected void convertM2TokenValuesForResourceSearch(Map tokenValues) {
        if (tokenValues.get(IvyPatternHelper.ORGANISATION_KEY) instanceof String) {
            tokenValues.put(IvyPatternHelper.ORGANISATION_KEY,
                convertM2OrganizationForResourceSearch((String) tokenValues
                        .get(IvyPatternHelper.ORGANISATION_KEY)));
        }
    }

}
