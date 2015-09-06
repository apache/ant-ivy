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
package org.apache.ivy.plugins.parser;

import java.util.Arrays;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

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
            assertTrue("artifact not found: " + artifactsNames[i], found);
        }
    }

    protected void assertDependencyArtifacts(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        DependencyArtifactDescriptor[] dads = dd.getDependencyArtifacts(confs);
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
            assertTrue("dependency artifact not found: " + artifactsNames[i], found);
        }
    }

    protected void assertDependencyArtifactIncludeRules(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        IncludeRule[] dads = dd.getIncludeRules(confs);
        assertNotNull(dads);
        assertEquals(artifactsNames.length, dads.length);
        for (int i = 0; i < artifactsNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < dads.length; j++) {
                assertNotNull(dads[j]);
                if (dads[j].getId().getName().equals(artifactsNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency include not found: " + artifactsNames[i], found);
        }
    }

    protected void assertDependencyArtifactExcludeRules(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        ExcludeRule[] rules = dd.getExcludeRules(confs);
        assertNotNull(rules);
        assertEquals(artifactsNames.length, rules.length);
        for (int i = 0; i < artifactsNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < rules.length; j++) {
                assertNotNull(rules[j]);
                if (rules[j].getId().getName().equals(artifactsNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency exclude not found: " + artifactsNames[i], found);
        }
    }

    protected void assertDependencyModulesExcludes(DependencyDescriptor dd, String[] confs,
            String[] moduleNames) {
        ExcludeRule[] rules = dd.getExcludeRules(confs);
        assertNotNull(rules);
        assertEquals(moduleNames.length, rules.length);
        for (int i = 0; i < moduleNames.length; i++) {
            boolean found = false;
            for (int j = 0; j < rules.length; j++) {
                assertNotNull(rules[j]);
                if (rules[j].getId().getModuleId().getName().equals(moduleNames[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency module exclude not found: " + moduleNames[i], found);
        }
    }

    protected void assertConf(ModuleDescriptor md, String name, String desc, Visibility visibility,
            String[] exts) {
        Configuration conf = md.getConfiguration(name);
        assertNotNull("configuration not found: " + name, conf);
        assertEquals(name, conf.getName());
        assertEquals(desc, conf.getDescription());
        assertEquals(visibility, conf.getVisibility());
        assertEquals(Arrays.asList(exts), Arrays.asList(conf.getExtends()));
    }

}
