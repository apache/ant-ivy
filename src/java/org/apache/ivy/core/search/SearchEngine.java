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
        Collection r = new LinkedHashSet();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            r.addAll(Arrays.asList(resolver.listTokenValues(token, otherTokenValues)));
        }
        return (String[]) r.toArray(new String[r.size()]);
    }

    public OrganisationEntry[] listOrganisationEntries() {
        List entries = new ArrayList();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            entries.addAll(Arrays.asList(resolver.listOrganisations()));
        }
        return (OrganisationEntry[]) entries.toArray(new OrganisationEntry[entries.size()]);
    }

    public String[] listOrganisations() {
        Collection orgs = new HashSet();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            OrganisationEntry[] entries = resolver.listOrganisations();
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        orgs.add(entries[i].getOrganisation());
                    }
                }
            }
        }
        return (String[]) orgs.toArray(new String[orgs.size()]);
    }

    public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
        List entries = new ArrayList();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            entries.addAll(Arrays.asList(resolver.listModules(org)));
        }
        return (ModuleEntry[]) entries.toArray(new ModuleEntry[entries.size()]);
    }

    public String[] listModules(String org) {
        List mods = new ArrayList();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            ModuleEntry[] entries = resolver.listModules(new OrganisationEntry(resolver, org));
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        mods.add(entries[i].getModule());
                    }
                }
            }
        }
        return (String[]) mods.toArray(new String[mods.size()]);
    }

    public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
        List entries = new ArrayList();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            entries.addAll(Arrays.asList(resolver.listRevisions(module)));
        }
        return (RevisionEntry[]) entries.toArray(new RevisionEntry[entries.size()]);
    }

    public String[] listRevisions(String org, String module) {
        List revs = new ArrayList();
        for (Iterator iter = settings.getResolvers().iterator(); iter.hasNext();) {
            DependencyResolver resolver = (DependencyResolver) iter.next();
            RevisionEntry[] entries = resolver.listRevisions(new ModuleEntry(new OrganisationEntry(
                    resolver, org), module));
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i] != null) {
                        revs.add(entries[i].getRevision());
                    }
                }
            }
        }
        return (String[]) revs.toArray(new String[revs.size()]);
    }

    /**
     * List module ids of the module accessible through the current resolvers matching the given mid
     * criteria according to the given matcher.
     * 
     * @param criteria
     * @param matcher
     * @return
     */
    public ModuleId[] listModules(ModuleId criteria, PatternMatcher matcher) {
        List ret = new ArrayList();
        Matcher orgMatcher = matcher.getMatcher(criteria.getOrganisation());
        Matcher modMatcher = matcher.getMatcher(criteria.getName());
        Map tokenValues = new HashMap();
        String[] orgs = listTokenValues(IvyPatternHelper.ORGANISATION_KEY, tokenValues);
        for (int i = 0; i < orgs.length; i++) {
            if (orgMatcher.matches(orgs[i])) {
                tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, orgs[i]);
                String[] mods = listTokenValues(IvyPatternHelper.MODULE_KEY, tokenValues);
                for (int j = 0; j < mods.length; j++) {
                    if (modMatcher.matches(mods[j])) {
                        ret.add(new ModuleId(orgs[i], mods[j]));
                    }
                }
            }
        }
        return (ModuleId[]) ret.toArray(new ModuleId[ret.size()]);
    }

    /**
     * List module revision ids of the module accessible through the current resolvers matching the
     * given mrid criteria according to the given matcher.
     * 
     * @param criteria
     * @param matcher
     * @return
     */
    public ModuleRevisionId[] listModules(ModuleRevisionId criteria, PatternMatcher matcher) {
        List ret = new ArrayList();
        Matcher orgMatcher = matcher.getMatcher(criteria.getOrganisation());
        Matcher modMatcher = matcher.getMatcher(criteria.getName());
        Matcher branchMatcher = matcher.getMatcher(criteria.getBranch());
        Matcher revMatcher = matcher.getMatcher(criteria.getRevision());
        Map tokenValues = new HashMap();
        String[] orgs = listTokenValues(IvyPatternHelper.ORGANISATION_KEY, tokenValues);
        for (int i = 0; i < orgs.length; i++) {
            if (orgMatcher.matches(orgs[i])) {
                tokenValues.put(IvyPatternHelper.ORGANISATION_KEY, orgs[i]);
                String[] mods = listTokenValues(IvyPatternHelper.MODULE_KEY, tokenValues);
                for (int j = 0; j < mods.length; j++) {
                    if (modMatcher.matches(mods[j])) {
                        tokenValues.put(IvyPatternHelper.MODULE_KEY, mods[j]);
                        String[] branches = listTokenValues(IvyPatternHelper.BRANCH_KEY,
                            tokenValues);
                        if (branches == null || branches.length == 0) {
                            branches = new String[] {settings.getDefaultBranch(new ModuleId(
                                    orgs[i], mods[j]))};
                        }
                        for (int k = 0; k < branches.length; k++) {
                            if (branches[k] == null || branchMatcher.matches(branches[k])) {
                                tokenValues.put(IvyPatternHelper.BRANCH_KEY, tokenValues);
                                String[] revs = listTokenValues(IvyPatternHelper.REVISION_KEY,
                                    tokenValues);
                                for (int l = 0; l < revs.length; l++) {
                                    if (revMatcher.matches(revs[l])) {
                                        ret.add(ModuleRevisionId.newInstance(orgs[i], mods[j],
                                            branches[k], revs[l]));
                                    }
                                }
                                tokenValues.remove(IvyPatternHelper.REVISION_KEY);
                            }
                        }
                        tokenValues.remove(IvyPatternHelper.BRANCH_KEY);
                    }
                }
                tokenValues.remove(IvyPatternHelper.MODULE_KEY);
            }
        }
        return (ModuleRevisionId[]) ret.toArray(new ModuleRevisionId[ret.size()]);
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
            modules.addAll(Arrays
                    .asList(resolver.listModules(new OrganisationEntry(resolver, org))));
        } else {
            Matcher orgMatcher = matcher.getMatcher(pattern.getOrganisation());
            for (int i = 0; i < orgs.length; i++) {
                String org = orgs[i].getOrganisation();
                String systemOrg = org;
                if (fromNamespace != null) {
                    systemOrg = NameSpaceHelper.transformOrganisation(org, fromNamespace
                            .getToSystemTransformer());
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
                systemMid = NameSpaceHelper.transform(foundMid, fromNamespace
                        .getToSystemTransformer());
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

                    ModuleRevisionId foundMrid = ModuleRevisionId.newInstance(mEntry
                            .getOrganisation(), mEntry.getModule(), rEntry.getRevision());
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
