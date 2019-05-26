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

import java.io.File;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.FileUtil;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IvyInstallTest {
    private File cache;

    private IvyInstall install;

    private Project project;

    @Before
    public void setUp() {
        createCache();
        cleanInstall();

        project = TestHelper.newProject();

        install = new IvyInstall();
        install.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
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

    @Test
    public void testInstallDummyDefault() {
        project.setProperty("ivy.settings.file",
            "test/repositories/ivysettings-dummydefaultresolver.xml");
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

    @Test
    public void testInstallWithAnyType() {
        project.setProperty("ivy.settings.file",
            "test/repositories/ivysettings-dummydefaultresolver.xml");
        install.setOrganisation("org8");
        install.setModule("mod8.1");
        install.setRevision("1.1");
        install.setFrom("2");
        install.setTo("install");
        install.setType("*");

        install.execute();

        assertTrue(new File("build/test/install/org8/mod8.1/a-1.1.txt").exists());
    }

    @Test
    public void testInstallWithMultipleType() {
        project.setProperty("ivy.settings.file",
            "test/repositories/ivysettings-dummydefaultresolver.xml");
        install.setOrganisation("org8");
        install.setModule("mod8.1");
        install.setRevision("1.1");
        install.setFrom("2");
        install.setTo("install");
        install.setType("unused,txt,other");

        install.execute();

        assertTrue(new File("build/test/install/org8/mod8.1/a-1.1.txt").exists());
    }

    /**
     * Normal case; no confs set (should use the default-&gt;* configuration).
     */
    @Test
    public void testInstallWithConfsDefaultSettings() {
        project.setProperty("ivy.settings.file", "test/repositories/IVY-1313/ivysettings.xml");
        install.setOrganisation("org1");
        install.setModule("mod1");
        install.setRevision("1.0");
        install.setFrom("default");
        install.setTo("install");
        install.setTransitive(true);

        install.execute();

        assertTrue(new File("build/test/install/org1/mod1/jars/mod1-1.0.jar").exists());
        assertTrue(new File("build/test/install/org1/mod2/jars/mod2-1.0.jar").exists());
        assertTrue(new File("build/test/install/org1/mod3/jars/mod3-1.0.jar").exists());
    }

    /**
     * Test case for IVY-1313.
     * Retrieve artifacts only for the master and runtime configuration.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1313">IVY-1313</a>
     */
    @Test
    public void testInstallWithConfsRuntimeOnly() {
        project.setProperty("ivy.settings.file", "test/repositories/IVY-1313/ivysettings.xml");
        install.setOrganisation("org1");
        install.setModule("mod1");
        install.setRevision("1.0");
        install.setFrom("default");
        install.setTo("install");
        install.setConf("master,runtime");
        install.setTransitive(true);

        install.execute();

        assertTrue(new File("build/test/install/org1/mod1/jars/mod1-1.0.jar").exists());
        assertTrue(new File("build/test/install/org1/mod2/jars/mod2-1.0.jar").exists());
        assertFalse(new File("build/test/install/org1/mod3/jars/mod3-1.0.jar").exists());
    }

    /**
     * Test case for IVY-1324.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1324">IVY-1324</a>
     */
    @Test
    public void testInstallWithClassifiers() throws Exception {
        project.setProperty("ivy.settings.url", new File("test/repositories/m2/ivysettings.xml")
                .toURI().toURL().toExternalForm());
        install.setOrganisation("org.apache");
        install.setModule("test-sources");
        install.setRevision("1.0");
        install.setType("*");
        install.setFrom("m2");
        install.setTo("IVY-1324");

        install.execute();

        assertTrue(new File(
                "build/test/install/org.apache/test-sources/test-sources-1.0-javadoc.jar").exists());
        assertTrue(new File(
                "build/test/install/org.apache/test-sources/test-sources-1.0-sources.jar").exists());
        assertTrue(new File("build/test/install/org.apache/test-sources/test-sources-1.0.jar")
                .exists());
        assertTrue(new File("build/test/install/org.apache/test-sources/ivy-1.0.xml").exists());
    }

    @Test
    public void testInstallWithUnusedType() {
        project.setProperty("ivy.settings.file",
            "test/repositories/ivysettings-dummydefaultresolver.xml");
        install.setOrganisation("org8");
        install.setModule("mod8.1");
        install.setRevision("1.1");
        install.setFrom("2");
        install.setTo("install");
        install.setType("unused");

        install.execute();

        assertFalse(new File("build/test/install/org8/mod8.1/a-1.1.txt").exists());
    }

    @Test
    public void testInstallWithOriginalMetadata() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        install.setOrganisation("org.apache");
        install.setModule("test");
        install.setRevision("1.0");
        install.setFrom("test");
        install.setTo("install");
        install.setHaltonfailure(false);
        install.execute();

        assertFalse(new File("build/test/install/org.apache/test/test-1.0.pom").exists());

        install.setInstallOriginalMetadata(true);
        install.setOverwrite(true);
        install.execute();

        assertTrue(new File("build/test/install/org.apache/test/test-1.0.pom").exists());
    }

    /**
     * Test case for IVY-843.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-843">IVY-843</a>
     */
    @Test
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

    @Test
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

    @Test
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

    /**
     * Installing a module with namespace coordinates instead of system one must fail.
     */
    @Test(expected = BuildException.class)
    public void testInstallWithNamespace2() {
        project.setProperty("ivy.settings.file", "test/repositories/namespace/ivysettings.xml");
        install.setOrganisation("A");
        install.setModule("B");
        install.setRevision("1.0");
        install.setTransitive(true);
        install.setFrom("ns");
        install.setTo("install");
        install.execute();
    }

    @Test
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

    /**
     * Fail on unknown dependency when haltonfailure=true.
     */
    @Test(expected = BuildException.class)
    public void testDependencyNotFoundFailure() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        install.setOrganisation("xxx");
        install.setModule("yyy");
        install.setRevision("zzz");
        install.setFrom("test");
        install.setTo("install");
        install.execute();
    }

    @Test
    public void testDependencyNotFoundSuccess() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        install.setOrganisation("xxx");
        install.setModule("yyy");
        install.setRevision("zzz");
        install.setFrom("test");
        install.setTo("1");
        install.setHaltonfailure(false);
        install.execute();
    }
}
