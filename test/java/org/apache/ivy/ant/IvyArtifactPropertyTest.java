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
package org.apache.ivy.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.ivy.TestHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IvyArtifactPropertyTest {

    private IvyArtifactProperty prop;

    private Project project;

    @Before
    public void setUp() {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        prop = new IvyArtifactProperty();
        prop.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        prop.setName("[module].[artifact]-[revision]");
        prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        prop.execute();
        String val = project.getProperty("mod1.2.mod1.2-2.0");
        assertNotNull(val);
        assertEquals(new File("build/cache/mod1.2/mod1.2-2.0.jar").getCanonicalPath(),
            new File(val).getCanonicalPath());
    }

    @Test
    public void testWithResolveId() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("abc");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        prop.setName("[module].[artifact]-[revision]");
        prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        prop.setResolveId("abc");
        prop.execute();

        String val = project.getProperty("mod1.2.mod1.2-2.0");
        assertNotNull(val);
        assertEquals(new File("build/cache/mod1.2/mod1.2-2.0.jar").getCanonicalPath(),
            new File(val).getCanonicalPath());
    }

    /**
     * Test must fail because no resolve was performed
     */
    @Test(expected = BuildException.class)
    public void testWithResolveIdWithoutResolve() {
        prop.setName("[module].[artifact]-[revision]");
        prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        prop.setResolveId("abc");
        prop.execute();
    }
}
