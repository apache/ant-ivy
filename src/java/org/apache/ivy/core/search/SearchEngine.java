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
package org.apache.ivy.core.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
     * @param otherTokenValues
     * @return
     */
    public String[] listTokenValues(String token, Map otherTokenValues) {
        Set entries = new LinkedHashSet();

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] values = resolver.listTokenValues(new String[] {token}, otherTokenValues);
            for (int i = 0; i < values.length; i++) {
                entries.add(values[i].get(token));
            }
        }

        return (String[]) entries.toArray(new String[entries.size()]);
    }

    public OrganisationEntry[] listOrganisationEntries() {
        Set entries = new HashSet();

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] orgs = resolver.listTokenValues(new String[] {IvyPatternHelper.ORGANISATION_KEY},
                new HashMap());
            for (int i = 0; i < orgs.length; i++) {
                String org = (String) orgs[i].get(IvyPatternHelper.ORGANISATION_KEY);
                entries.add(new OrganisationEntry(resolver, org));
            }
        }

        return (OrganisationEntry[]) entries.toArray(new OrganisationEntry[entries.size()]);
    }

    public String[] listOrganisations() {
        Set entries = new HashSet();

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] orgs = resolver.listTokenValues(new String[] {IvyPatternHelper.ORGANISATION_KEY},
                new HashMap());
            for (int i = 0; i < orgs.length; i++) {
                entries.add(orgs[i].get(IvyPatternHelper.ORGANISATION_KEY));
            }
        }

        return (String[]) entries.toArray(new String[entries.size()]);
    }

    public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
        Set entries = new HashSet();

        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org.getOrganisation());

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] modules = resolver.listTokenValues(new String[] {IvyPatternHelper.MODULE_KEY},
                tokenValues);
            for (int i = 0; i < modules.length; i++) {
                String module = (String) modules[i].get(IvyPatternHelper.MODULE_KEY);
                entries.add(new ModuleEntry(org, module));
            }
        }

        return (ModuleEntry[]) entries.toArray(new ModuleEntry[entries.size()]);
    }

    public String[] listModules(String org) {
        Set entries = new HashSet();

        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org);

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] modules = resolver.listTokenValues(new String[] {IvyPatternHelper.MODULE_KEY},
                tokenValues);
            for (int i = 0; i < modules.length; i++) {
                entries.add(modules[i].get(IvyPatternHelper.MODULE_KEY));
            }
        }

        return (String[]) entries.toArray(new String[entries.size()]);
    }

    public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
        Set entries = new HashSet();

        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, module.getOrganisation());
        tokenValues.put(IvyPatternHelper.MODULE_KEY, module.getModule());

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] revisions = resolver.listTokenValues(
                new String[] {IvyPatternHelper.REVISION_KEY}, tokenValues);
            for (int i = 0; i < revisions.length; i++) {
                String revision = (String) revisions[i].get(IvyPatternHelper.REVISION_KEY);
                entries.add(new RevisionEntry(module, revision));
            }
        }

        return (RevisionEntry[]) entries.toArray(new RevisionEntry[entries.size()]);
    }

    public String[] listRevisions(String org, String module) {
        Set entries = new HashSet();

        Map tokenValues = new HashMap();
        tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, org);
        tokenValues.put(IvyPatternHelper.MODULE_KEY, module);

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] revisions = resolver.listTokenValues(
                new String[] {IvyPatternHelper.REVISION_KEY}, tokenValues);
            for (int i = 0; i < revisions.length; i++) {
                entries.add(revisions[i].get(IvyPatternHelper.REVISION_KEY));
            }
        }

        return (String[]) entries.toArray(new String[entries.size()]);
    }

    /**
     * List module ids of the module accessible through the current resolvers matching the given mid
     * criteria according to the given matcher.
     * <p>
     * ModuleId are returned in the system namespace.
     * </p>
     * 
     * @param criteria
     * @param matcher
     * @return
     */
    public ModuleId[] listModules(ModuleId moduleCrit, PatternMatcher matcher) {
        List ret = new ArrayList();

        Map criteria = new HashMap();
        addMatcher(matcher, moduleCrit.getOrganisation(), criteria,
            IvyPatternHelper.ORGANISATION_KEY);
        addMatcher(matcher, moduleCrit.getName(), criteria, IvyPatternHelper.MODULE_KEY);

        String[] tokensToList = new String[] {IvyPatternHelper.ORGANISATION_KEY,
                IvyPatternHelper.MODULE_KEY};

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
            for (int i = 0; i < moduleIdAsMap.length; i++) {
                String org = (String) moduleIdAsMap[i].get(IvyPatternHelper.ORGANISATION_KEY);
                String name = (String) moduleIdAsMap[i].get(IvyPatternHelper.MODULE_KEY);
                ModuleId modId = ModuleId.newInstance(org, name);
                ret.add(NameSpaceHelper.transform(modId, resolver.getNamespace()
                        .getToSystemTransformer()));
            }
        }

        return (ModuleId[]) ret.toArray(new ModuleId[ret.size()]);
    }

    /**
     * List module revision ids of the module accessible through the current resolvers matching the
     * given mrid criteria according to the given matcher.
     * <p>
     * ModuleRevisionId are returned in the system namespace.
     * </p>
     * 
     * @param criteria
     * @param matcher
     * @return
     */
    public ModuleRevisionId[] listModules(ModuleRevisionId moduleCrit, PatternMatcher matcher) {
        List ret = new ArrayList();

        Map criteria = new HashMap();
        for (Iterator it = moduleCrit.getAttributes().entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            addMatcher(matcher, (String) entry.getValue(), criteria, (String) entry.getKey());
        }

        String[] tokensToList = (String[]) moduleCrit.getAttributes().keySet()
                .toArray(new String[moduleCrit.getAttributes().size()]);

        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            Map[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
            for (int i = 0; i < moduleIdAsMap.length; i++) {
                String org = (String) moduleIdAsMap[i].get(IvyPatternHelper.ORGANISATION_KEY);
                String name = (String) moduleIdAsMap[i].get(IvyPatternHelper.MODULE_KEY);
                String branch = (String) moduleIdAsMap[i].get(IvyPatternHelper.BRANCH_KEY);
                String rev = (String) moduleIdAsMap[i].get(IvyPatternHelper.REVISION_KEY);

                Map foundExtraAtts = new HashMap();
                Set qualAttributes = moduleCrit.getQualifiedExtraAttributes().keySet();
                for (Iterator iter2 = qualAttributes.iterator(); iter2.hasNext();) {
                    String qualifiedKey = (String) iter2.next();
                    String value = null;
                    int colonIndex = qualifiedKey.indexOf(':');
                    if (colonIndex == -1) {
                        value = (String) moduleIdAsMap[i].get(qualifiedKey);
                    } else {
                        value = (String) moduleIdAsMap[i].get(qualifiedKey
                                .substring(colonIndex + 1));
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

        return (ModuleRevisionId[]) ret.toArray(new ModuleRevisionId[ret.size()]);
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
        Map criteria = new HashMap();
        for (Iterator it = moduleCrit.getAttributes().entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            addMatcher(matcher, (String) entry.getValue(), criteria, (String) entry.getKey());
        }

        String[] tokensToList = (String[]) moduleCrit.getAttributes().keySet()
                .toArray(new String[moduleCrit.getAttributes().size()]);

        Map[] moduleIdAsMap = resolver.listTokenValues(tokensToList, criteria);
        Set result = new LinkedHashSet(); // we use a Set to remove duplicates
        for (int i = 0; i < moduleIdAsMap.length; i++) {
            String org = (String) moduleIdAsMap[i].get(IvyPatternHelper.ORGANISATION_KEY);
            String name = (String) moduleIdAsMap[i].get(IvyPatternHelper.MODULE_KEY);
            String branch = (String) moduleIdAsMap[i].get(IvyPatternHelper.BRANCH_KEY);
            String rev = (String) moduleIdAsMap[i].get(IvyPatternHelper.REVISION_KEY);

            Map foundExtraAtts = new HashMap();
            Set qualExtraAttributes = moduleCrit.getQualifiedExtraAttributes().keySet();
            for (Iterator iter2 = qualExtraAttributes.iterator(); iter2.hasNext();) {
                String qualifiedKey = (String) iter2.next();
                String value = null;
                int colonIndex = qualifiedKey.indexOf(':');
                if (colonIndex == -1) {
                    value = (String) moduleIdAsMap[i].get(qualifiedKey);
                } else {
                    value = (String) moduleIdAsMap[i].get(qualifiedKey.substring(colonIndex + 1));
                }

                if (value != null) {
                    foundExtraAtts.put(qualifiedKey, value);
                }
            }

            ModuleRevisionId modRevId = ModuleRevisionId.newInstance(org, name, branch, rev,
                foundExtraAtts);
            result.add(resolver.getNamespace().getToSystemTransformer().transform(modRevId));
        }

        return (ModuleRevisionId[]) result.toArray(new ModuleRevisionId[result.size()]);
    }

    private void addMatcher(PatternMatcher patternMatcher, String expression, Map criteria,
            String key) {
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

    public Collection findModuleRevisionIds(DependencyResolver resolver, ModuleRevisionId pattern,
            PatternMatcher matcher) {
        Collection mrids = new ArrayList();
        String resolverName = resolver.getName();

        Message.verbose("looking for modules matching " + pattern + " using " + matcher.getName());
        Namespace fromNamespace = null;
        if (resolver instanceof AbstractResolver) {
            fromNamespace = ((AbstractResolver) resolver).getNamespace();
        }

        Collection modules = new ArrayList();

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
            for (int i = 0; i < orgs.length; i++) {
                String org = orgs[i].getOrganisation();
                String systemOrg = org;
                if (fromNamespace != null) {
                    systemOrg = NameSpaceHelper.transformOrganisation(org,
                        fromNamespace.getToSystemTransformer());
                }
                if (orgMatcher.matches(systemOrg)) {
                    modules.addAll(Arrays.asList(resolver.listModules(new OrganisationEntry(
                            resolver, org))));
                }
            }
        }
        Message.debug("found " + modules.size() + " modules for " + pattern.getOrganisation()
                + " on " + resolverName);
        boolean foundModule = false;
        for (Iterator iter = modules.iterator(); iter.hasNext();) {
            ModuleEntry mEntry = (ModuleEntry) iter.next();

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
                for (int j = 0; j < rEntries.length; j++) {
                    RevisionEntry rEntry = rEntries[j];

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
