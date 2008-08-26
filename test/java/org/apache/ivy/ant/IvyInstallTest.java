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

import junit.framework.TestCase;

import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

public class IvyInstallTest extends TestCase {
    private File cache;

    private IvyInstall install;

    private Project project;

    protected void setUp() throws Exception {
        createCache();
        cleanInstall();
        
        project = AntTestHelper.newProject();
        
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        install = new IvyInstall();
        install.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
        cleanInstall();
    }

    private void cleanCache() {
        FileUtil.forceDelete(cache);
    }

    private void cleanInstall() {
        FileUtil.forceDelete(new File("build/test/install"));
        FileUtil.forceDelete(new File("build/test/install2"));
    }

    public void testInstallDummyDefault() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-dummydefaultresolver.xml");
        install.setOrganisation("org1");
        install.setModule("mod1.4");
        install.setRevision("1.0.1");
        install.setFrom("test");
        install.setTo("install");
        install.setTransitive(true);

        install.execute();

        assertTrue(new File("build/test/install/org1/mod1.4/ivy-1.0.1.xml").exists());

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-2.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.2.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.2.jar").exists());
    }

    public void testIVY843() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-IVY843.xml");
        install.setOrganisation("org1");
        install.setModule("mod1.4");
        install.setRevision("1.0.1");
        install.setFrom("test");
        install.setTo("install");

        install.execute();
        
        cleanCache();
        
        install.setFrom("install");
        install.setTo("install2");
        install.execute();

        assertTrue(new File("build/test/install2/org1/mod1.4/ivy-1.0.1.xml").exists());
    }

    public void testInstallWithBranch() {
        project.setProperty("ivy.settings.file", "test/repositories/branches/ivysettings.xml");
        install.setOrganisation("foo");
        install.setModule("foo1");
        install.setBranch("branch1");
        install.setRevision("2");
        install.setFrom("default");
        install.setTo("install");

        install.execute();

        assertTrue(new File("build/test/install/foo/foo1/branch1/ivy-2.xml").exists());
    }

    public void testInstallWithNamespace() {
        project.setProperty("ivy.settings.file", "test/repositories/namespace/ivysettings.xml");
        install.setOrganisation("systemorg");
        install.setModule("systemmod2");
        install.setRevision("1.0");
        install.setTransitive(true);
        install.setFrom("ns");
        install.setTo("install");

        install.execute();

        assertTrue(new File("build/test/install/systemorg/systemmod2/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/systemorg/systemmod/ivy-1.0.xml").exists());
    }

    public void testInstallWithNamespace2() {
        project.setProperty("ivy.settings.file", "test/repositories/namespace/ivysettings.xml");
        install.setOrganisation("A");
        install.setModule("B");
        install.setRevision("1.0");
        install.setTransitive(true);
        install.setFrom("ns");
        install.setTo("install");

        try {
            install.execute();
            fail("installing module with namespace coordinates instead of system one should fail");
        } catch (BuildException ex) {
            // expected
        }
    }

    public void testInstallWithNamespace3() {
        project.setProperty("ivy.settings.file", "test/repositories/namespace/ivysettings.xml");
        install.setOrganisation("*");
        install.setModule("*");
        install.setRevision("*");
        install.setTransitive(true);
        install.setFrom("ns");
        install.setTo("install");

        install.execute();

        assertTrue(new File("build/test/install/systemorg/systemmod2/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/systemorg/systemmod/ivy-1.0.xml").exists());
    }

    public void testDependencyNotFoundFailure() {
        install.setOrganisation("xxx");
        install.setModule("yyy");
        install.setRevision("zzz");
        install.setFrom("test");
        install.setTo("install");

        try {
            install.execute();
            fail("unknown dependency, failure expected (haltonfailure=true)");
        } catch (BuildException be) {
            // success
        }
    }

    public void testDependencyNotFoundSuccess() {
        install.setOrganisation("xxx");
        install.setModule("yyy");
        install.setRevision("zzz");
        install.setFrom("test");
        install.setTo("1");
        install.setHaltonfailure(false);

        try {
            install.execute();
        } catch (BuildException be) {
            fail("unknown dependency, failure unexpected (haltonfailure=false). Failure: " + be);
        }
    }
}
