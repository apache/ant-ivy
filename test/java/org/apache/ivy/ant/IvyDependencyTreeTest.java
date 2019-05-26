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
import org.apache.ivy.ant.testutil.AntTaskTestCase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IvyDependencyTreeTest extends AntTaskTestCase {

    private IvyDependencyTree dependencyTree;

    private Project project;

    @Before
    public void setUp() {
        TestHelper.createCache();
        project = configureProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        dependencyTree = new IvyDependencyTree();
        dependencyTree.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testSimple() {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-simple");
        assertLogContaining("\\- org1#mod1.2;2.0");
    }

    @Test
    public void testEmpty() {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-empty.xml"));
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-empty");
        assertLogNotContaining("\\-");
    }

    @Test
    public void testWithResolveId() {
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

        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogContaining("\\- org1#mod1.2;latest.integration");
    }

    /**
     * Task must fail because no resolve was performed.
     */
    @Test(expected = BuildException.class)
    public void testWithResolveIdWithoutResolve() {
        dependencyTree.execute();
    }

    @Test
    public void testWithEvictedModule() {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-dyn-evicted.xml"));
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogNotContaining("+- org1#mod1.2;1+");
        assertLogContaining("+- org6#mod6.1;2.0");
        assertLogContaining("   \\- org1#mod1.2;2.2");
        assertLogContaining("\\- org1#mod1.2;2.2");
    }

    @Test
    public void testShowEvictedModule() {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-dyn-evicted.xml"));
        dependencyTree.setShowEvicted(true);
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogContaining("+- org1#mod1.2;1+ evicted by [org1#mod1.2;2.2] in apache#resolve-latest;1.0");
        assertLogContaining("+- org6#mod6.1;2.0");
        assertLogContaining("   \\- org1#mod1.2;2.2");
        assertLogContaining("\\- org1#mod1.2;2.2");
    }

    /**
     * Test case for IVY-1540.
     * Dependency tree task must not run into an infinite loop due to circular dependencies.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1540">IVY-1540</a>
     */
    @Test
    public void testCircularDep() {
        final String resolveId = "circular-dep-tree";
        // resolve
        final IvyResolve ivyResolve = new IvyResolve();
        ivyResolve.setProject(project);
        ivyResolve.setResolveId(resolveId);
        ivyResolve.setFile(new File("test/repositories/1/org/foo-bar/ivys/ivy-1.2.3.xml"));
        ivyResolve.execute();
        // use the resolveid to fetch the dependency tree from that previous resolution
        dependencyTree.setResolveId(resolveId);
        dependencyTree.execute();
        // check the logged message
        assertLogContaining("Dependency tree for " + resolveId);
        assertLogContaining("(circularly depends on) " + "org.circular#module1;1.0");
        assertLogNotContaining("(circularly depends on) " + "org.circular#module2;2.0");
    }

}
