/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import junit.framework.Assert;
import fr.jayasoft.ivy.DependencyResolver;

/**
 * 
 */
public class ResolverTestHelper {
    static void assertOrganisationEntries(DependencyResolver resolver, String[] orgNames, OrganisationEntry[] orgs) {
        Assert.assertNotNull(orgs);
        Assert.assertEquals(orgNames.length, orgs.length);
        assertOrganisationEntriesContains(resolver, orgNames, orgs);
    }
    
    static void assertOrganisationEntriesContains(DependencyResolver resolver, String[] orgNames, OrganisationEntry[] orgs) {
        Assert.assertNotNull(orgs);
        for (int i = 0; i < orgNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < orgs.length; j++) {
                if (orgNames[i].equals(orgs[j].getOrganisation())) {
                    found = true;
                    Assert.assertEquals(resolver, orgs[j].getResolver());
                }
            }
            Assert.assertTrue("organisation not found: "+orgNames[i], found);
        }
    }
    
    static void assertModuleEntries(DependencyResolver resolver, OrganisationEntry org, String[] names, ModuleEntry[] mods) {
        Assert.assertNotNull(mods);
        Assert.assertEquals(names.length, mods.length);
        assertModuleEntriesContains(resolver, org, names, mods);
    }
    
    
    static void assertModuleEntriesContains(DependencyResolver resolver, OrganisationEntry org, String[] names, ModuleEntry[] mods) {
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
            Assert.assertTrue("module not found: "+names[i], found);
        }
    }
    
    
    static void assertRevisionEntries(DependencyResolver resolver, ModuleEntry mod, String[] names, RevisionEntry[] revs) {
        Assert.assertNotNull(revs);
        Assert.assertEquals(names.length, revs.length);
        assertRevisionEntriesContains(resolver, mod, names, revs);
    }
    
    static void assertRevisionEntriesContains(DependencyResolver resolver, ModuleEntry mod, String[] names, RevisionEntry[] revs) {
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
            Assert.assertTrue("revision not found: "+names[i], found);
        }
    }
    
    static OrganisationEntry getEntry(OrganisationEntry[] orgs, String name) {
        for (int i = 0; i < orgs.length; i++) {
            if (name.equals(orgs[i].getOrganisation())) {
                return orgs[i];
            }
        }
        Assert.fail("organisation not found: "+name);
        return null; // for compilation only
    }
    
    static ModuleEntry getEntry(ModuleEntry[] mods, String name) {
        for (int i = 0; i < mods.length; i++) {
            if (name.equals(mods[i].getModule())) {
                return mods[i];
            }
        }
        Assert.fail("module not found: "+name);
        return null; // for compilation only
    }
    
}
