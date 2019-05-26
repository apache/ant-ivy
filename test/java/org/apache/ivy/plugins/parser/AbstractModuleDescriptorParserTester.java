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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractModuleDescriptorParserTester {
    protected DependencyDescriptor getDependency(DependencyDescriptor[] dependencies, String name) {
        for (DependencyDescriptor dependency : dependencies) {
            assertNotNull(dependency);
            assertNotNull(dependency.getDependencyId());
            if (name.equals(dependency.getDependencyId().getName())) {
                return dependency;
            }
        }
        return null;
    }

    protected void assertArtifacts(Artifact[] artifacts, String[] artifactsNames) {
        assertNotNull(artifacts);
        assertEquals(artifactsNames.length, artifacts.length);
        for (String artifactsName : artifactsNames) {
            boolean found = false;
            for (Artifact artifact : artifacts) {
                assertNotNull(artifact);
                if (artifact.getName().equals(artifactsName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("artifact not found: " + artifactsName, found);
        }
    }

    protected void assertDependencyArtifacts(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        DependencyArtifactDescriptor[] dads = dd.getDependencyArtifacts(confs);
        assertNotNull(dads);
        assertEquals(artifactsNames.length, dads.length);
        for (String artifactsName : artifactsNames) {
            boolean found = false;
            for (DependencyArtifactDescriptor dad : dads) {
                assertNotNull(dad);
                if (dad.getName().equals(artifactsName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency artifact not found: " + artifactsName, found);
        }
    }

    protected void assertDependencyArtifactIncludeRules(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        IncludeRule[] dads = dd.getIncludeRules(confs);
        assertNotNull(dads);
        assertEquals(artifactsNames.length, dads.length);
        for (String artifactsName : artifactsNames) {
            boolean found = false;
            for (IncludeRule dad : dads) {
                assertNotNull(dad);
                if (dad.getId().getName().equals(artifactsName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency include not found: " + artifactsName, found);
        }
    }

    protected void assertDependencyArtifactExcludeRules(DependencyDescriptor dd, String[] confs,
            String[] artifactsNames) {
        ExcludeRule[] rules = dd.getExcludeRules(confs);
        assertNotNull(rules);
        assertEquals(artifactsNames.length, rules.length);
        for (String artifactsName : artifactsNames) {
            boolean found = false;
            for (ExcludeRule rule : rules) {
                assertNotNull(rule);
                if (rule.getId().getName().equals(artifactsName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency exclude not found: " + artifactsName, found);
        }
    }

    protected void assertDependencyModulesExcludes(DependencyDescriptor dd, String[] confs,
            String[] moduleNames) {
        ExcludeRule[] rules = dd.getExcludeRules(confs);
        assertNotNull(rules);
        assertEquals(moduleNames.length, rules.length);
        for (String moduleName : moduleNames) {
            boolean found = false;
            for (ExcludeRule rule : rules) {
                assertNotNull(rule);
                if (rule.getId().getModuleId().getName().equals(moduleName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency module exclude not found: " + moduleName, found);
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
