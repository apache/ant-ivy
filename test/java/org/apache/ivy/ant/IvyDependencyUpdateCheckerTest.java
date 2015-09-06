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

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.testutil.AntTaskTestCase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class IvyDependencyUpdateCheckerTest extends AntTaskTestCase {

    private IvyDependencyUpdateChecker dependencyUpdateChecker;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        Project project = configureProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());

        dependencyUpdateChecker = new IvyDependencyUpdateChecker();
        dependencyUpdateChecker.setProject(project);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSimple() throws Exception {
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

    public void testSimpleAndShowTransitiveDependencies() throws Exception {
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

    public void testResolveWithoutIvyFile() throws Exception {
        // depends on org="org1" name="mod1.2" rev="2.0"

        dependencyUpdateChecker.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        dependencyUpdateChecker.setConf("default");
        dependencyUpdateChecker.setHaltonfailure(false);
        dependencyUpdateChecker.execute();

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

    public void testInline() throws Exception {
        // same as before, but expressing dependency directly without ivy file
        dependencyUpdateChecker.setOrganisation("org1");
        dependencyUpdateChecker.setModule("mod1.2");
        dependencyUpdateChecker.setRevision("2.0");
        dependencyUpdateChecker.setInline(true);
        dependencyUpdateChecker.execute();

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");

    }

    public void testInlineForNonExistingModule() throws Exception {
        try {
            dependencyUpdateChecker.setOrganisation("org1XXYZ");
            dependencyUpdateChecker.setModule("mod1.2");
            dependencyUpdateChecker.setRevision("2.0");
            dependencyUpdateChecker.setInline(true);
            dependencyUpdateChecker.setHaltonfailure(true);
            dependencyUpdateChecker.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailure() throws Exception {
        try {
            dependencyUpdateChecker
                    .setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            dependencyUpdateChecker.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailureWithMissingConfigurations() throws Exception {
        try {
            dependencyUpdateChecker
                    .setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            dependencyUpdateChecker.setConf("default,unknown");
            dependencyUpdateChecker.execute();
            fail("missing configurations didn't raised an exception");
        } catch (BuildException ex) {
            assertTrue(ex.getMessage().indexOf("unknown") != -1);
        }
    }

    public void testFailureOnBadDependencyIvyFile() throws Exception {
        try {
            dependencyUpdateChecker.setFile(new File(
                    "test/java/org/apache/ivy/ant/ivy-failure2.xml"));
            dependencyUpdateChecker.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailureOnBadStatusInDependencyIvyFile() throws Exception {
        try {
            dependencyUpdateChecker.setFile(new File(
                    "test/java/org/apache/ivy/ant/ivy-failure3.xml"));
            dependencyUpdateChecker.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            dependencyUpdateChecker
                    .setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            dependencyUpdateChecker.setHaltonfailure(false);
            dependencyUpdateChecker.execute();
        } catch (BuildException ex) {
            ex.printStackTrace();
            fail("failure raised an exception with haltonfailure set to false");
        }
    }

    public void testExcludedConf() throws Exception {
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
        assertFalse(project.getProperty("ivy.resolved.configurations").indexOf("default") > -1);
    }

    public void testResolveWithAbsoluteFile() {
        // IVY-396
        File ivyFile = new File("test/java/org/apache/ivy/ant/ivy-simple.xml");
        dependencyUpdateChecker.getProject().setProperty("ivy.dep.file", ivyFile.getAbsolutePath());
        dependencyUpdateChecker.execute();

        // assertTrue(getResolvedIvyFileInCache(
        // ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
    }

    public void testResolveWithRelativeFile() {
        // IVY-396
        dependencyUpdateChecker.getProject().setProperty("ivy.dep.file",
            "test/java/org/apache/ivy/ant/ivy-simple.xml");
        dependencyUpdateChecker.execute();

        // assertTrue(getResolvedIvyFileInCache(
        // ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");
    }

    private Ivy getIvy() {
        return dependencyUpdateChecker.getIvyInstance();
    }

    public void testSimpleExtends() throws Exception {
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
        assertEquals(null, dependencyUpdateChecker.getProject().getProperty("ivy.parent[0].branch"));

        assertLogContaining("Dependencies updates available :");
        assertLogContaining("org1#mod1.1\t1.1 -> 2.0");
        assertLogContaining("org1#mod1.1\t1.0 -> 2.0");
        assertLogContaining("org1#mod1.2\t2.1 -> 2.2");
        assertLogContaining("org2#mod2.1\t0.3 -> 0.7");

        // inherited from parent
        assertLogContaining("org1#mod1.2\t2.0 -> 2.2");

    }

}
