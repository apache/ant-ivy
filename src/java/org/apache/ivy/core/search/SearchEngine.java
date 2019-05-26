/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.Matcher;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.NameSpaceHelper;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;

public class SearchEngine {
    private IvySettings settings;

    public SearchEngine(IvySettings settings) {
        this.settings = settings;
    }

    /**
     * Returns an empty array when no token values are found.
     *
     * @param token
     *            ditto
     * @param otherTokenValues Map
     * @return String[]
     */
    public String[] listTokenValues(String token, Map<String, Object> otherTokenValues) {
        Set<String> entries = new LinkedHashSet<>();

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] values = resolver.listTokenValues(new String[] {token},
                otherTokenValues);
            for (Map<String, String> value : values) {
                entries.add(value.get(token));
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

    public OrganisationEntry[] listOrganisationEntries() {
        Set<OrganisationEntry> entries = new HashSet<>();

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] orgs = resolver.listTokenValues(
                new String[] {IvyPatternHelper.ORGANISATION_KEY}, new HashMap<String, Object>());
            for (Map<String, String> oe : orgs) {
                String org = oe.get(IvyPatternHelper.ORGANISATION_KEY);
                entries.add(new OrganisationEntry(resolver, org));
            }
        }

        return entries.toArray(new OrganisationEntry[entries.size()]);
    }

    public String[] listOrganisations() {
        Set<String> entries = new HashSet<>();

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] orgs = resolver.listTokenValues(
                new String[] {IvyPatternHelper.ORGANISATION_KEY}, new HashMap<String, Object>());
            for (Map<String, String> org : orgs) {
                entries.add(org.get(IvyPatternHelper.ORGANISATION_KEY));
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

    public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
        Set<ModuleEntry> entries = new HashSet<>();

        Map<String, Object> tokenValues = new HashMap<>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] modules = resolver.listTokenValues(
                new String[] {IvyPatternHelper.MODULE_KEY}, tokenValues);
            for (Map<String, String> me : modules) {
                String module = me.get(IvyPatternHelper.MODULE_KEY);
                entries.add(new ModuleEntry(org, module));
            }
        }

        return entries.toArray(new ModuleEntry[entries.size()]);
    }

    public String[] listModules(String org) {
        Set<String> entries = new HashSet<>();

        Map<String, Object> tokenValues = new HashMap<>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org);

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] modules = resolver.listTokenValues(
                new String[] {IvyPatternHelper.MODULE_KEY}, tokenValues);
            for (Map<String, String> module : modules) {
                entries.add(module.get(IvyPatternHelper.MODULE_KEY));
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

    public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
        Set<RevisionEntry> entries = new HashSet<>();

        Map<String, Object> tokenValues = new HashMap<>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, module.getOrganisation());
        tokenValues.put(IvyPatternHelper.MODULE_KEY, module.getModule());

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] revisions = resolver.listTokenValues(
                new String[] {IvyPatternHelper.REVISION_KEY}, tokenValues);
            for (Map<String, String> revision : revisions) {
                entries.add(new RevisionEntry(module, revision.get(IvyPatternHelper.REVISION_KEY)));
            }
        }

        return entries.toArray(new RevisionEntry[entries.size()]);
    }

    public String[] listRevisions(String org, String module) {
        Set<String> entries = new HashSet<>();

        Map<String, Object> tokenValues = new HashMap<>();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org);
        tokenValues.put(IvyPatternHelper.MODULE_KEY, module);

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] revisions = resolver.listTokenValues(
                new String[] {IvyPatternHelper.REVISION_KEY}, tokenValues);
            for (Map<String, String> revision : revisions) {
                entries.add(revision.get(IvyPatternHelper.REVISION_KEY));
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

    /**
     * List module ids of the module accessible through the current resolvers matching the given mid
     * criteria according to the given matcher.
     * <p>
     * ModuleId are returned in the system namespace.
     * </p>
     *
     * @param moduleCrit ModuleId
     * @param matcher PatternMatcher
     * @return ModuleId[]
     */
    public ModuleId[] listModules(ModuleId moduleCrit, PatternMatcher matcher) {
        List<ModuleId> ret = new ArrayList<>();

        Map<String, Object> criteria = new HashMap<>();
        addMatcher(matcher, moduleCrit.getOrganisation(), criteria,
            IvyPatternHelper.ORGANISATION_KEY);
        addMatcher(matcher, moduleCrit.getName(), criteria, IvyPatternHelper.MODULE_KEY);

        String[] tokensToList = new String[] {IvyPatternHelper.ORGANISATION_KEY,
                IvyPatternHelper.MODULE_KEY};

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
            for (Map<String, String> moduleId : moduleIdAsMap) {
                String org = moduleId.get(IvyPatternHelper.ORGANISATION_KEY);
                String name = moduleId.get(IvyPatternHelper.MODULE_KEY);
                ModuleId modId = ModuleId.newInstance(org, name);
                ret.add(NameSpaceHelper.transform(modId, resolver.getNamespace()
                        .getToSystemTransformer()));
            }
        }

        return ret.toArray(new ModuleId[ret.size()]);
    }

    /**
     * List module revision ids of the module accessible through the current resolvers matching the
     * given mrid criteria according to the given matcher.
     * <p>
     * ModuleRevisionId are returned in the system namespace.
     * </p>
     *
     * @param moduleCrit ModuleRevisionId
     * @param matcher PatternMatcher
     * @return ModuleRevisionId[]
     */
    public ModuleRevisionId[] listModules(ModuleRevisionId moduleCrit, PatternMatcher matcher) {
        List<ModuleRevisionId> ret = new ArrayList<>();

        Map<String, Object> criteria = new HashMap<>();
        for (Map.Entry<String, String> entry : moduleCrit.getAttributes().entrySet()) {
            addMatcher(matcher, entry.getValue(), criteria, entry.getKey());
        }

        String[] tokensToList = moduleCrit.getAttributes().keySet()
                .toArray(new String[moduleCrit.getAttributes().size()]);

        for (DependencyResolver resolver : settings.getResolvers()) {
            Map<String, String>[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
            for (Map<String, String> moduleId : moduleIdAsMap) {
                String org = moduleId.get(IvyPatternHelper.ORGANISATION_KEY);
                String name = moduleId.get(IvyPatternHelper.MODULE_KEY);
                String branch = moduleId.get(IvyPatternHelper.BRANCH_KEY);
                String rev = moduleId.get(IvyPatternHelper.REVISION_KEY);

                Map<String, String> foundExtraAtts = new HashMap<>();
                for (String qualifiedKey : moduleCrit.getQualifiedExtraAttributes().keySet()) {
                    String value = null;
                    int colonIndex = qualifiedKey.indexOf(':');
                    if (colonIndex == -1) {
                        value = moduleId.get(qualifiedKey);
                    } else {
                        value = moduleId.get(qualifiedKey.substring(colonIndex + 1));
                    }

                    if (value != null) {
                        foundExtraAtts.put(qualifiedKey, value);
                    }
                }

                ModuleRevisionId modRevId = ModuleRevisionId.newInstance(org, name, branch, rev,
                    foundExtraAtts);
                ret.add(resolver.getNamespace().getToSystemTransformer().transform(modRevId));
            }
        }

        return ret.toArray(new ModuleRevisionId[ret.size()]);
    }

    /**
     * List modules matching a given criteria, available in the given dependency resolver.
     * <p>
     * ModuleRevisionId are returned in the system namespace.
     * </p>
     *
     * @param resolver
     *            the resolver in which modules should looked up
     * @param moduleCrit
     *            the criteria to match
     * @param matcher
     *            the matcher to use to match criteria
     * @return an array of matching module revision ids
     */
    public ModuleRevisionId[] listModules(DependencyResolver resolver, ModuleRevisionId moduleCrit,
            PatternMatcher matcher) {
        Map<String, Object> criteria = new HashMap<>();
        for (Map.Entry<String, String> entry : moduleCrit.getAttributes().entrySet()) {
            addMatcher(matcher, entry.getValue(), criteria, entry.getKey());
        }

        String[] tokensToList = moduleCrit.getAttributes().keySet()
                .toArray(new String[moduleCrit.getAttributes().size()]);

        Map<String, String>[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
        Set<ModuleRevisionId> result = new LinkedHashSet<>(); // we use a Set to remove duplicates
        for (Map<String, String> moduleId : moduleIdAsMap) {
            String org = moduleId.get(IvyPatternHelper.ORGANISATION_KEY);
            String name = moduleId.get(IvyPatternHelper.MODULE_KEY);
            String branch = moduleId.get(IvyPatternHelper.BRANCH_KEY);
            String rev = moduleId.get(IvyPatternHelper.REVISION_KEY);

            Map<String, String> foundExtraAtts = new HashMap<>();
            for (String qualifiedKey : moduleCrit.getQualifiedExtraAttributes().keySet()) {
                String value = null;
                int colonIndex = qualifiedKey.indexOf(':');
                if (colonIndex == -1) {
                    value = moduleId.get(qualifiedKey);
                } else {
                    value = moduleId.get(qualifiedKey.substring(colonIndex + 1));
                }

                if (value != null) {
                    foundExtraAtts.put(qualifiedKey, value);
                }
            }

            ModuleRevisionId modRevId = ModuleRevisionId.newInstance(org, name, branch, rev,
                foundExtraAtts);
            result.add(resolver.getNamespace().getToSystemTransformer().transform(modRevId));
        }

        return result.toArray(new ModuleRevisionId[result.size()]);
    }

    private void addMatcher(PatternMatcher patternMatcher, String expression,
            Map<String, Object> criteria, String key) {
        if (expression == null) {
            return;
        }

        Matcher matcher = patternMatcher.getMatcher(expression);
        if (matcher.isExact()) {
            criteria.put(key, expression);
        } else {
            criteria.put(key, matcher);
        }
    }

    public Collection<ModuleRevisionId> findModuleRevisionIds(DependencyResolver resolver,
            ModuleRevisionId pattern, PatternMatcher matcher) {
        Collection<ModuleRevisionId> mrids = new ArrayList<>();
        String resolverName = resolver.getName();

        Message.verbose("looking for modules matching " + pattern + " using " + matcher.getName());
        Namespace fromNamespace = null;
        if (resolver instanceof AbstractResolver) {
            fromNamespace = resolver.getNamespace();
        }

        Collection<ModuleEntry> modules = new ArrayList<>();

        OrganisationEntry[] orgs = resolver.listOrganisations();
        if (orgs == null || orgs.length == 0) {
            // hack for resolvers which are not able to list organisation, we try to see if the
            // asked organisation is not an exact one:
            String org = pattern.getOrganisation();
            if (fromNamespace != null) {
                org = NameSpaceHelper.transform(pattern.getModuleId(),
                    fromNamespace.getFromSystemTransformer()).getOrganisation();
            }
            modules.addAll(Arrays.asList(resolver.listModules(new OrganisationEntry(resolver, org))));
        } else {
            Matcher orgMatcher = matcher.getMatcher(pattern.getOrganisation());
            for (OrganisationEntry oe : orgs) {
                String org = oe.getOrganisation();
                String systemOrg = (fromNamespace == null) ? org
                    : NameSpaceHelper.transformOrganisation(org,
                        fromNamespace.getToSystemTransformer());
                if (orgMatcher.matches(systemOrg)) {
                    modules.addAll(Arrays.asList(resolver.listModules(new OrganisationEntry(
                            resolver, org))));
                }
            }
        }
        Message.debug("found " + modules.size() + " modules for " + pattern.getOrganisation()
                + " on " + resolverName);
        boolean foundModule = false;
        for (ModuleEntry mEntry : modules) {

            ModuleId foundMid = new ModuleId(mEntry.getOrganisation(), mEntry.getModule());
            ModuleId systemMid = foundMid;
            if (fromNamespace != null) {
                systemMid = NameSpaceHelper.transform(foundMid,
                    fromNamespace.getToSystemTransformer());
            }

            if (MatcherHelper.matches(matcher, pattern.getModuleId(), systemMid)) {
                // The module corresponds to the searched module pattern
                foundModule = true;
                RevisionEntry[] rEntries = resolver.listRevisions(mEntry);
                Message.debug("found " + rEntries.length + " revisions for ["
                        + mEntry.getOrganisation() + ", " + mEntry.getModule() + "] on "
                        + resolverName);

                boolean foundRevision = false;
                for (RevisionEntry rEntry : rEntries) {
                    ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(
                        mEntry.getOrganisation(), mEntry.getModule(), rEntry.getRevision());
                    ModuleRevisionId systemMrid = foundMrid;
                    if (fromNamespace != null) {
                        systemMrid = fromNamespace.getToSystemTransformer().transform(foundMrid);
                    }

                    if (MatcherHelper.matches(matcher, pattern, systemMrid)) {
                        // We have a matching module revision
                        foundRevision = true;
                        mrids.add(systemMrid);
                    }
                }
                if (!foundRevision) {
                    Message.debug("no revision found matching " + pattern + " in ["
                            + mEntry.getOrganisation() + "," + mEntry.getModule() + "] using "
                            + resolverName);
                }
            }
        }
        if (!foundModule) {
            Message.debug("no module found matching " + pattern + " using " + resolverName);
        }
        return mrids;
    }

}
