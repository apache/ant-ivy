/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.parser;

import java.util.Arrays;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.DependencyArtifactDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.Configuration.Visibility;
import junit.framework.TestCase;

public abstract class AbstractModuleDescriptorParserTester extends TestCase {
    protected DependencyDescriptor getDependency(DependencyDescriptor[] dependencies, String name) {
        for (int i = 0; i < dependencies.length; i++) {
            assertNotNull(dependencies[i]);
            assertNotNull(dependencies[i].getDependencyId());
            if (name.equals(dependencies[i].getDependencyId().getName())) {
                return dependencies[i];
            }
        }
        return null;
    }

    protected void assertArtifacts(Artifact[] artifacts, String[] artifactsNames) {
        assertNotNull(artifacts);
        assertEquals(artifactsNames.length, artifacts.length);
        for (int i = 0; i < artifactsNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < artifacts.length; j++) {
                assertNotNull(artifacts[j]);
                if (artifacts[j].getName().equals(artifactsNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("artifact not found: "+artifactsNames[i], found);
        }
    }

    protected void assertDependencyArtifactsIncludes(DependencyDescriptor dd, String[] confs, String[] artifactsNames) {
        DependencyArtifactDescriptor[] dads = dd.getDependencyArtifactsIncludes(confs);
        assertNotNull(dads);
        assertEquals(artifactsNames.length, dads.length);
        for (int i = 0; i < artifactsNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < dads.length; j++) {
                assertNotNull(dads[j]);
                if (dads[j].getName().equals(artifactsNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency artifact include not found: "+artifactsNames[i], found);
        }
    }

    protected void assertDependencyArtifactsExcludes(DependencyDescriptor dd, String[] confs, String[] artifactsNames) {
        DependencyArtifactDescriptor[] dads = dd.getDependencyArtifactsExcludes(confs);
        assertNotNull(dads);
        assertEquals(artifactsNames.length, dads.length);
        for (int i = 0; i < artifactsNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < dads.length; j++) {
                assertNotNull(dads[j]);
                if (dads[j].getName().equals(artifactsNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency artifact exclude not found: "+artifactsNames[i], found);
        }
    }

    protected void assertDependencyModulesExcludes(DependencyDescriptor dd, String[] confs, String[] moduleNames) {
        DependencyArtifactDescriptor[] dads = dd.getDependencyArtifactsExcludes(confs);
        assertNotNull(dads);
        assertEquals(moduleNames.length, dads.length);
        for (int i = 0; i < moduleNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < dads.length; j++) {
                assertNotNull(dads[j]);
                if (dads[j].getId().getModuleId().getName().equals(moduleNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency module exclude not found: "+moduleNames[i], found);
        }
    }

    protected void assertConf(ModuleDescriptor md, String name, String desc, Visibility visibility, String[] exts) {
        Configuration conf = md.getConfiguration(name);
        assertNotNull("configuration not found: "+name, conf);
        assertEquals(name, conf.getName());
        assertEquals(desc, conf.getDescription());
        assertEquals(visibility, conf.getVisibility());
        assertEquals(Arrays.asList(exts), Arrays.asList(conf.getExtends()));
    }

}
