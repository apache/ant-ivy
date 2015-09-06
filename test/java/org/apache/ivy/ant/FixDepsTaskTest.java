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
package org.apache.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class FixDepsTaskTest extends TestCase {

    private FixDepsTask fixDeps;

    private Project project;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        fixDeps = new FixDepsTask();
        fixDeps.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");

        File dest = new File("build/testFixDeps/testSimple.xml");
        fixDeps.setToFile(dest);
        fixDeps.execute();

        assertTrue(dest.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURI().toURL(), true);
        assertEquals("apache", md.getModuleRevisionId().getOrganisation());
        assertEquals("resolve-simple", md.getModuleRevisionId().getName());
        assertEquals("1.0", md.getModuleRevisionId().getRevision());
        assertEquals("release", md.getStatus());

        assertEquals(1, md.getConfigurations().length);
        assertEquals("default", md.getConfigurations()[0].getName());
        assertEquals(0, md.getConfigurations()[0].getExtends().length);

        assertEquals(1, md.getDependencies().length);
        assertEquals("org1", md.getDependencies()[0].getDependencyId().getOrganisation());
        assertEquals("mod1.2", md.getDependencies()[0].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[0].isChanging());
        assertEquals(true, md.getDependencies()[0].isForce());
        assertEquals(false, md.getDependencies()[0].isTransitive());
        assertEquals("2.0", md.getDependencies()[0].getDependencyRevisionId().getRevision());
    }

    public void testMulticonf() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-multiconf.xml");

        File dest = new File("build/testFixDeps/testMultiConf.xml");
        fixDeps.setToFile(dest);
        fixDeps.execute();

        assertTrue(dest.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURI().toURL(), true);
        assertEquals("apache", md.getModuleRevisionId().getOrganisation());
        assertEquals("resolve-simple", md.getModuleRevisionId().getName());
        assertEquals("1.0", md.getModuleRevisionId().getRevision());
        assertEquals("release", md.getStatus());

        assertEquals(2, md.getConfigurations().length);
        assertEquals("default", md.getConfigurations()[0].getName());
        assertEquals(0, md.getConfigurations()[0].getExtends().length);
        assertEquals("compile", md.getConfigurations()[1].getName());
        assertEquals(0, md.getConfigurations()[1].getExtends().length);

        assertEquals(2, md.getDependencies().length);

        assertEquals("org1", md.getDependencies()[0].getDependencyId().getOrganisation());
        assertEquals("mod1.2", md.getDependencies()[0].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[0].isChanging());
        assertEquals(true, md.getDependencies()[0].isForce());
        assertEquals(false, md.getDependencies()[0].isTransitive());
        assertEquals("2.0", md.getDependencies()[0].getDependencyRevisionId().getRevision());
        assertEquals(1, md.getDependencies()[0].getModuleConfigurations().length);
        assertEquals("default", md.getDependencies()[0].getModuleConfigurations()[0]);
        assertEquals(1, md.getDependencies()[0].getDependencyConfigurations("default").length);
        assertEquals("default", md.getDependencies()[0].getDependencyConfigurations("default")[0]);

        assertEquals("org1", md.getDependencies()[1].getDependencyId().getOrganisation());
        assertEquals("mod1.1", md.getDependencies()[1].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[1].isChanging());
        assertEquals(true, md.getDependencies()[1].isForce());
        assertEquals(false, md.getDependencies()[1].isTransitive());
        assertEquals("2.0", md.getDependencies()[1].getDependencyRevisionId().getRevision());
        assertEquals(1, md.getDependencies()[1].getModuleConfigurations().length);
        assertEquals("compile", md.getDependencies()[1].getModuleConfigurations()[0]);
        assertEquals(1, md.getDependencies()[1].getDependencyConfigurations("compile").length);
        assertEquals("default", md.getDependencies()[1].getDependencyConfigurations("compile")[0]);
    }

    public void testTransitivity() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-transitive.xml");

        File dest = new File("build/testFixDeps/testTransitivity.xml");
        fixDeps.setToFile(dest);
        fixDeps.execute();

        assertTrue(dest.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURI().toURL(), true);
        assertEquals("apache", md.getModuleRevisionId().getOrganisation());
        assertEquals("resolve-simple", md.getModuleRevisionId().getName());
        assertEquals("1.0", md.getModuleRevisionId().getRevision());
        assertEquals("release", md.getStatus());

        assertEquals(2, md.getConfigurations().length);
        assertEquals("default", md.getConfigurations()[0].getName());
        assertEquals(0, md.getConfigurations()[0].getExtends().length);
        assertEquals("compile", md.getConfigurations()[1].getName());
        assertEquals(0, md.getConfigurations()[1].getExtends().length);

        assertEquals(3, md.getDependencies().length);

        assertEquals("org1", md.getDependencies()[0].getDependencyId().getOrganisation());
        assertEquals("mod1.2", md.getDependencies()[0].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[0].isChanging());
        assertEquals(true, md.getDependencies()[0].isForce());
        assertEquals(false, md.getDependencies()[0].isTransitive());
        assertEquals("2.0", md.getDependencies()[0].getDependencyRevisionId().getRevision());
        assertEquals(1, md.getDependencies()[0].getModuleConfigurations().length);
        assertEquals("default", md.getDependencies()[0].getModuleConfigurations()[0]);
        assertEquals(1, md.getDependencies()[0].getDependencyConfigurations("default").length);
        assertEquals("default", md.getDependencies()[0].getDependencyConfigurations("default")[0]);

        assertEquals("org1", md.getDependencies()[1].getDependencyId().getOrganisation());
        assertEquals("mod1.1", md.getDependencies()[1].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[1].isChanging());
        assertEquals(true, md.getDependencies()[1].isForce());
        assertEquals(false, md.getDependencies()[1].isTransitive());
        assertEquals("2.0", md.getDependencies()[1].getDependencyRevisionId().getRevision());
        assertEquals(1, md.getDependencies()[1].getModuleConfigurations().length);
        assertEquals("compile", md.getDependencies()[1].getModuleConfigurations()[0]);
        assertEquals(1, md.getDependencies()[1].getDependencyConfigurations("compile").length);
        assertEquals("default", md.getDependencies()[1].getDependencyConfigurations("compile")[0]);

        assertEquals("org1", md.getDependencies()[2].getDependencyId().getOrganisation());
        assertEquals("mod1.2", md.getDependencies()[2].getDependencyId().getName());
        assertEquals(false, md.getDependencies()[2].isChanging());
        assertEquals(true, md.getDependencies()[2].isForce());
        assertEquals(false, md.getDependencies()[2].isTransitive());
        assertEquals("2.1", md.getDependencies()[2].getDependencyRevisionId().getRevision());
        assertEquals(1, md.getDependencies()[2].getModuleConfigurations().length);
        assertEquals("compile", md.getDependencies()[2].getModuleConfigurations()[0]);
        assertEquals(1, md.getDependencies()[2].getDependencyConfigurations("compile").length);
        assertEquals("*", md.getDependencies()[2].getDependencyConfigurations("compile")[0]);
    }

    public void testFixedResolve() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-transitive.xml");

        File dest = new File("build/testFixDeps/testTransitivity.xml");
        fixDeps.setToFile(dest);
        fixDeps.execute();

        project.setProperty("ivy.dep.file", dest.getAbsolutePath());
        File dest2 = new File("build/testFixDeps/testTransitivity2.xml");
        fixDeps.setToFile(dest2);
        fixDeps.execute();

        ModuleDescriptor md1 = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURI().toURL(), true);
        ModuleDescriptor md2 = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest2.toURI().toURL(), true);
        assertEquals(md1, md2);
        assertEquals(Arrays.asList(md1.getConfigurations()), Arrays.asList(md2.getConfigurations()));
        assertEquals(toString(Arrays.asList(md1.getDependencies())),
            toString(Arrays.asList(md2.getDependencies())));
    }

    private List/* <String> */toString(List list) {
        List strings = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            strings.add(list.get(i).toString());
        }
        return strings;
    }
}
