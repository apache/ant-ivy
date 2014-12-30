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

    private List<String> ivyPatterns = new ArrayList<String>();

    private List<String> artifactPatterns = new ArrayList<String>();

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

    @Override
    public ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        ModuleRevisionId mrid = artifact.getModuleRevisionId();
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(mrid);
        }
        return findResourceUsingPatterns(mrid, artifactPatterns, artifact,
            getDefaultRMDParser(artifact.getModuleRevisionId().getModuleId()), date);
    }

    @Override
    public ResolvedResource findResource(ResolvedResource[] rress, ResourceMDParser rmdparser,
            ModuleRevisionId mrid, Date date) {
        if (isM2compatible()) {
            // convert 'M2'-organisation back to 'Ivy'-organisation
            mrid = convertM2ResourceSearchIdToNormal(mrid);
        }
        return super.findResource(rress, rmdparser, mrid, date);
    }

    protected ResolvedResource findResourceUsingPatterns(ModuleRevisionId moduleRevision,
            List<String> patternList, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        List<ResolvedResource> resolvedResources = new ArrayList<ResolvedResource>();
        Set<String> foundRevisions = new HashSet<String>();
        boolean dynamic = getSettings().getVersionMatcher().isDynamic(moduleRevision);
        boolean stop = false;
        for (Iterator<String> iter = patternList.iterator(); iter.hasNext() && !stop;) {
            String pattern = iter.next();
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
            ResolvedResource[] rress = resolvedResources
                    .toArray(new ResolvedResource[resolvedResources.size()]);
            return findResource(rress, rmdparser, moduleRevision, date);
        } else if (resolvedResources.size() == 1) {
            return resolvedResources.get(0);
        } else {
            return null;
        }
    }

    protected abstract ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid,
            String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date);

    @Override
    protected Collection<String> findNames(Map<String, String> tokenValues, String token) {
        Collection<String> names = new HashSet<String>();
        names.addAll(findIvyNames(tokenValues, token));
        if (isAllownomd()) {
            names.addAll(findArtifactNames(tokenValues, token));
        }
        return names;
    }

    protected Collection<String> findIvyNames(Map<String, String> tokenValues, String token) {
        Collection<String> names = new HashSet<String>();
        tokenValues = new HashMap<String, String>(tokenValues);
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

    protected Collection<String> findArtifactNames(Map<String, String> tokenValues, String token) {
        Collection<String> names = new HashSet<String>();
        tokenValues = new HashMap<String, String>(tokenValues);
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

    @Override
    public Map<String, String>[] listTokenValues(String[] tokens, Map<String, Object> criteria) {
        Set<Map<String, String>> result = new LinkedHashSet<Map<String, String>>();

        // use ivy patterns
        List<String> ivyPatterns = getIvyPatterns();
        Map<String, Object> subcriteria = new HashMap<String, Object>(criteria);
        subcriteria.put(IvyPatternHelper.TYPE_KEY, "ivy");
        subcriteria.put(IvyPatternHelper.EXT_KEY, getModuleDescriptorExtension());
        if (isM2compatible()) {
            convertM2CriteriaForResourceSearch(subcriteria);
        }
        for (String ivyPattern : ivyPatterns) {
            result.addAll(resolveTokenValues(tokens, ivyPattern, subcriteria, false));
        }

        if (isAllownomd()) {
            List<String> artifactPatterns = getArtifactPatterns();
            subcriteria = new HashMap<String, Object>(criteria);
            subcriteria.put(IvyPatternHelper.TYPE_KEY, "jar");
            subcriteria.put(IvyPatternHelper.EXT_KEY, "jar");
            if (isM2compatible()) {
                convertM2CriteriaForResourceSearch(subcriteria);
            }
            for (String artifactPattern : artifactPatterns) {
                result.addAll(resolveTokenValues(tokens, artifactPattern, subcriteria, true));
            }
        }

        return result.toArray(new Map[result.size()]);
    }

    protected String getModuleDescriptorExtension() {
        return "xml";
    }

    private Set<Map<String, String>> resolveTokenValues(String[] tokens, String pattern,
            Map<String, Object> criteria, boolean noMd) {
        Set<Map<String, String>> result = new LinkedHashSet<Map<String, String>>();
        Set<String> tokenSet = new HashSet<String>(Arrays.asList(tokens));

        Map<String, String> tokenValues = new HashMap<String, String>();
        for (Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                tokenValues.put(key, (String) value);
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

        List<String> vals = new ArrayList<String>(Arrays.asList(values));
        filterNames(vals);

        for (String value : vals) {
            if ((matcher != null) && !matcher.matches(value)) {
                continue;
            }

            tokenValues.put(token, value);
            String moreResolvedPattern = IvyPatternHelper.substituteTokens(
                partiallyResolvedPattern, tokenValues);

            Map<String, Object> newCriteria = new HashMap<String, Object>(criteria);
            newCriteria.put(token, value);
            if (noMd && "artifact".equals(token)) {
                newCriteria.put("module", value);
            } else if (noMd && "module".equals(token)) {
                newCriteria.put("artifact", value);
            }
            result.addAll(resolveTokenValues(tokenSet.toArray(new String[tokenSet.size()]),
                moreResolvedPattern, newCriteria, noMd));
        }

        return result;
    }

    protected abstract String[] listTokenValues(String pattern, String token);

    protected abstract boolean exist(String path);

    protected void findTokenValues(Collection<String> names, List<String> patterns,
            Map<String, String> tokenValues, String token) {
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

    public List<String> getIvyPatterns() {
        return Collections.unmodifiableList(ivyPatterns);
    }

    public List<String> getArtifactPatterns() {
        return Collections.unmodifiableList(artifactPatterns);
    }

    protected void setIvyPatterns(List<String> patterns) {
        ivyPatterns = patterns;
    }

    protected void setArtifactPatterns(List<String> patterns) {
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

    @Override
    public void dumpSettings() {
        super.dumpSettings();
        Message.debug("\t\tm2compatible: " + isM2compatible());
        Message.debug("\t\tivy patterns:");
        for (String p : getIvyPatterns()) {
            Message.debug("\t\t\t" + p);
        }
        Message.debug("\t\tartifact patterns:");
        for (String p : getArtifactPatterns()) {
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

    protected void convertM2TokenValuesForResourceSearch(Map<String, String> tokenValues) {
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY,
            convertM2OrganizationForResourceSearch(tokenValues
                    .get(IvyPatternHelper.ORGANISATION_KEY)));
    }

    protected void convertM2CriteriaForResourceSearch(Map<String, Object> criteria) {
        Object org = criteria.get(IvyPatternHelper.ORGANISATION_KEY);
        if (org instanceof String) {
            criteria.put(IvyPatternHelper.ORGANISATION_KEY,
                convertM2OrganizationForResourceSearch((String) org));
        }
    }

}
