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
package org.apache.ivy.plugins.resolver;

import java.util.Arrays;

import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ResolverTestHelper {
    static void assertOrganisationEntries(DependencyResolver resolver, String[] orgNames,
            OrganisationEntry[] orgs) {
        assertNotNull(orgs);
        assertEquals(
            "invalid organisation entries: unmatched number: expected: " + Arrays.asList(orgNames)
                    + " but was " + Arrays.asList(orgs), orgNames.length, orgs.length);
        assertOrganisationEntriesContains(resolver, orgNames, orgs);
    }

    static void assertOrganisationEntriesContains(DependencyResolver resolver, String[] orgNames,
            OrganisationEntry[] orgs) {
        assertNotNull(orgs);
        for (String orgName : orgNames) {
            boolean found = false;
            for (OrganisationEntry org : orgs) {
                if (orgName.equals(org.getOrganisation())) {
                    found = true;
                    assertEquals(resolver, org.getResolver());
                }
            }
            assertTrue("organisation not found: " + orgName, found);
        }
    }

    static void assertModuleEntries(DependencyResolver resolver, OrganisationEntry org,
            String[] names, ModuleEntry[] mods) {
        assertNotNull(mods);
        assertEquals(
            "invalid module entries: unmatched number: expected: " + Arrays.asList(names)
                    + " but was " + Arrays.asList(mods), names.length, mods.length);
        assertModuleEntriesContains(resolver, org, names, mods);
    }

    private static void assertModuleEntriesContains(DependencyResolver resolver, OrganisationEntry org,
                                                    String[] names, ModuleEntry[] mods) {
        assertNotNull(mods);
        for (String name : names) {
            boolean found = false;
            for (ModuleEntry mod : mods) {
                if (name.equals(mod.getModule())) {
                    found = true;
                    assertEquals(resolver, mod.getResolver());
                    assertEquals(org, mod.getOrganisationEntry());
                }
            }
            assertTrue("module not found: " + name, found);
        }
    }

    static void assertRevisionEntries(DependencyResolver resolver, ModuleEntry mod, String[] names,
            RevisionEntry[] revs) {
        assertNotNull(revs);
        assertEquals(
            "invalid revision entries: unmatched number: expected: " + Arrays.asList(names)
                    + " but was " + Arrays.asList(revs), names.length, revs.length);
        assertRevisionEntriesContains(resolver, mod, names, revs);
    }

    private static void assertRevisionEntriesContains(DependencyResolver resolver, ModuleEntry mod,
                                                      String[] names, RevisionEntry[] revs) {
        assertNotNull(revs);
        for (String name : names) {
            boolean found = false;
            for (RevisionEntry rev : revs) {
                if (name.equals(rev.getRevision())) {
                    found = true;
                    assertEquals(resolver, rev.getResolver());
                    assertEquals(mod, rev.getModuleEntry());
                }
            }
            assertTrue("revision not found: " + name, found);
        }
    }

    static OrganisationEntry getEntry(OrganisationEntry[] orgs, String name) {
        for (OrganisationEntry org : orgs) {
            if (name.equals(org.getOrganisation())) {
                return org;
            }
        }
        return null; // for compilation only
    }

    static ModuleEntry getEntry(ModuleEntry[] mods, String name) {
        for (ModuleEntry mod : mods) {
            if (name.equals(mod.getModule())) {
                return mod;
            }
        }
        return null; // for compilation only
    }
}
