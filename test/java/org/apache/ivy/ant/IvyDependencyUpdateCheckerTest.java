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

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.testutil.AntTaskTestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class IvyDependencyUpdateCheckerTest extends AntTaskTestCase {

    private IvyDependencyUpdateChecker dependencyUpdateChecker;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        TestHelper.createCache();
        Project project = configureProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());

        dependencyUpdateChecker = new IvyDependencyUpdateChecker();
        dependencyUpdateChecker.setProject(project);
    }

   @After
   public void tearDown() {
        TestHelper.cleanCache();
    }

    private Ivy getIvy() {
        return dependencyUpdateChecker.getIvyInstance();
    }

    @Test
    public void testSimple() {
        // depends on org="org1" name="mod1.1" rev="1.0"
        // has transitive dependency on org="org1" name="mod1.2" rev="2.0"
        dependencyUpdateChecker.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple3.xml"));
        dependencyUpdateChecker.execute();

        assertEquals("resolve-simple", getIvy().getVariable("ivy.module"));
        assertEquals("1.0", getIvy().getVariable("ivy.revision"));

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.1\t1.0 -> 2.0");
        assertLogNotContaining("org1#mod1.2 (transitive)\t2.0 -> 2.1");
    }

    @Test
    public void testSimpleAndShowTransitiveDependencies() {
        // depends on org="org1" name="mod1.1" rev="1.0"
        // has transitive dependency on org="org1" name="mod1.2" rev="2.0"
        dependencyUpdateChecker.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple3.xml"));
        dependencyUpdateChecker.setShowTransitive(true);
        dependencyUpdateChecker.execute();

        assertEquals("resolve-simple", getIvy().getVariable("ivy.module"));
        assertEquals("1.0", getIvy().getVariable("ivy.revision"));

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.1\t1.0 -> 2.0");
        assertLogContaining("org1#mod1.2 (transitive)\t2.0 -> 2.1");
    }

    @Test
    public void testResolveWithoutIvyFile() {
        // depends on org="org1" name="mod1.2" rev="2.0"
        dependencyUpdateChecker.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        dependencyUpdateChecker.setConf("default");
        dependencyUpdateChecker.setHaltonfailure(false);
        dependencyUpdateChecker.execute();

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

    @Test
    public void testInline() {
        // same as before, but expressing dependency directly without ivy file
        dependencyUpdateChecker.setOrganisation("org1");
        dependencyUpdateChecker.setModule("mod1.2");
        dependencyUpdateChecker.setRevision("2.0");
        dependencyUpdateChecker.setInline(true);
        dependencyUpdateChecker.execute();

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testInlineForNonExistingModule() {
        dependencyUpdateChecker.setOrganisation("org1XXYZ");
        dependencyUpdateChecker.setModule("mod1.2");
        dependencyUpdateChecker.setRevision("2.0");
        dependencyUpdateChecker.setInline(true);
        dependencyUpdateChecker.setHaltonfailure(true);
        dependencyUpdateChecker.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testFailure() {
        dependencyUpdateChecker
                .setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
        dependencyUpdateChecker.execute();
    }

    /**
     * Test must fail because of missing configurations.
     */
    @Test
    public void testFailureWithMissingConfigurations() {
        expExc.expect(BuildException.class);
        expExc.expectMessage("unknown");

        dependencyUpdateChecker
                .setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        dependencyUpdateChecker.setConf("default,unknown");
        dependencyUpdateChecker.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testFailureOnBadDependencyIvyFile() {
        dependencyUpdateChecker.setFile(new File(
                "test/java/org/apache/ivy/ant/ivy-failure2.xml"));
        dependencyUpdateChecker.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testFailureOnBadStatusInDependencyIvyFile() {
        dependencyUpdateChecker.setFile(new File(
                "test/java/org/apache/ivy/ant/ivy-failure3.xml"));
        dependencyUpdateChecker.execute();
    }

    @Test
    public void testHaltOnFailure() {
        dependencyUpdateChecker
                .setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
        dependencyUpdateChecker.setHaltonfailure(false);
        dependencyUpdateChecker.execute();
    }

    @Test
    public void testExcludedConf() {
        dependencyUpdateChecker.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        dependencyUpdateChecker.setConf("*,!default");
        dependencyUpdateChecker.execute();

        // assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "2.0"))
        // .exists());
        // assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
        // .exists());

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("All dependencies are up to date");
        // test the properties
        Project project = dependencyUpdateChecker.getProject();
        assertFalse(project.getProperty("ivy.resolved.configurations").contains("default"));
    }

    /**
     * Test case for IVY-396.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-396">IVY-396</a>
     */
    @Test
    public void testResolveWithAbsoluteFile() {
        File ivyFile = new File("test/java/org/apache/ivy/ant/ivy-simple.xml");
        dependencyUpdateChecker.getProject().setProperty("ivy.dep.file", ivyFile.getAbsolutePath());
        dependencyUpdateChecker.execute();

        // assertTrue(getResolvedIvyFileInCache(
        // ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
    }

    /**
     * Test case for IVY-396.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-396">IVY-396</a>
     */
    @Test
    public void testResolveWithRelativeFile() {
        dependencyUpdateChecker.getProject().setProperty("ivy.dep.file",
            "test/java/org/apache/ivy/ant/ivy-simple.xml");
        dependencyUpdateChecker.execute();

        // assertTrue(getResolvedIvyFileInCache(
        // ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

    @Test
    public void testSimpleExtends() {
        dependencyUpdateChecker.setFile(new File(
                "test/java/org/apache/ivy/ant/ivy-extends-multiconf.xml"));
        dependencyUpdateChecker.execute();
        assertEquals("1", dependencyUpdateChecker.getProject().getProperty("ivy.parents.count"));
        assertEquals("apache",
            dependencyUpdateChecker.getProject().getProperty("ivy.parent[0].organisation"));
        assertEquals("resolve-simple",
            dependencyUpdateChecker.getProject().getProperty("ivy.parent[0].module"));
        assertEquals("1.0",
            dependencyUpdateChecker.getProject().getProperty("ivy.parent[0].revision"));
        assertNull(dependencyUpdateChecker.getProject().getProperty("ivy.parent[0].branch"));

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.1\t1.1 -> 2.0");
        assertLogContaining("org1#mod1.1\t1.0 -> 2.0");
        assertLogContaining("org1#mod1.2\t2.1 -> 2.2");
        assertLogContaining("org2#mod2.1\t0.3 -> 0.7");

        // inherited from parent
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

}
