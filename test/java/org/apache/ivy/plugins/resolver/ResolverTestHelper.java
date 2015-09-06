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

import java.util.Arrays;

import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;

import junit.framework.Assert;

/**
 * 
 */
public class ResolverTestHelper {
    static void assertOrganisationEntries(DependencyResolver resolver, String[] orgNames,
            OrganisationEntry[] orgs) {
        Assert.assertNotNull(orgs);
        Assert.assertEquals(
            "invalid organisation entries: unmatched number: expected: " + Arrays.asList(orgNames)
                    + " but was " + Arrays.asList(orgs), orgNames.length, orgs.length);
        assertOrganisationEntriesContains(resolver, orgNames, orgs);
    }

    static void assertOrganisationEntriesContains(DependencyResolver resolver, String[] orgNames,
            OrganisationEntry[] orgs) {
        Assert.assertNotNull(orgs);
        for (int i = 0; i < orgNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < orgs.length; j++) {
                if (orgNames[i].equals(orgs[j].getOrganisation())) {
                    found = true;
                    Assert.assertEquals(resolver, orgs[j].getResolver());
                }
            }
            Assert.assertTrue("organisation not found: " + orgNames[i], found);
        }
    }

    static void assertModuleEntries(DependencyResolver resolver, OrganisationEntry org,
            String[] names, ModuleEntry[] mods) {
        Assert.assertNotNull(mods);
        Assert.assertEquals(
            "invalid module entries: unmatched number: expected: " + Arrays.asList(names)
                    + " but was " + Arrays.asList(mods), names.length, mods.length);
        assertModuleEntriesContains(resolver, org, names, mods);
    }

    static void assertModuleEntriesContains(DependencyResolver resolver, OrganisationEntry org,
            String[] names, ModuleEntry[] mods) {
        Assert.assertNotNull(mods);
        for (int i = 0; i < names.length; i++) {
            boolean found = false;
            for (int j = 0; j < mods.length; j++) {
                if (names[i].equals(mods[j].getModule())) {
                    found = true;
                    Assert.assertEquals(resolver, mods[j].getResolver());
                    Assert.assertEquals(org, mods[j].getOrganisationEntry());
                }
            }
            Assert.assertTrue("module not found: " + names[i], found);
        }
    }

    static void assertRevisionEntries(DependencyResolver resolver, ModuleEntry mod, String[] names,
            RevisionEntry[] revs) {
        Assert.assertNotNull(revs);
        Assert.assertEquals(
            "invalid revision entries: unmatched number: expected: " + Arrays.asList(names)
                    + " but was " + Arrays.asList(revs), names.length, revs.length);
        assertRevisionEntriesContains(resolver, mod, names, revs);
    }

    static void assertRevisionEntriesContains(DependencyResolver resolver, ModuleEntry mod,
            String[] names, RevisionEntry[] revs) {
        Assert.assertNotNull(revs);
        for (int i = 0; i < names.length; i++) {
            boolean found = false;
            for (int j = 0; j < revs.length; j++) {
                if (names[i].equals(revs[j].getRevision())) {
                    found = true;
                    Assert.assertEquals(resolver, revs[j].getResolver());
                    Assert.assertEquals(mod, revs[j].getModuleEntry());
                }
            }
            Assert.assertTrue("revision not found: " + names[i], found);
        }
    }

    static OrganisationEntry getEntry(OrganisationEntry[] orgs, String name) {
        for (int i = 0; i < orgs.length; i++) {
            if (name.equals(orgs[i].getOrganisation())) {
                return orgs[i];
            }
        }
        Assert.fail("organisation not found: " + name);
        return null; // for compilation only
    }

    static ModuleEntry getEntry(ModuleEntry[] mods, String name) {
        for (int i = 0; i < mods.length; i++) {
            if (name.equals(mods[i].getModule())) {
                return mods[i];
            }
        }
        Assert.fail("module not found: " + name);
        return null; // for compilation only
    }

}
