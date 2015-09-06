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

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.testutil.AntTaskTestCase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class IvyDependencyTreeTest extends AntTaskTestCase {

    private IvyDependencyTree dependencyTree;

    private Project project;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        project = configureProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        dependencyTree = new IvyDependencyTree();
        dependencyTree.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSimple() throws Exception {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-simple");
        assertLogContaining("\\- org1#mod1.2;2.0");
    }

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

        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogContaining("\\- org1#mod1.2;latest.integration");
    }

    public void testWithResolveIdWithoutResolve() throws Exception {
        try {
            dependencyTree.execute();
            fail("Task should have failed because no resolve was performed!");
        } catch (BuildException e) {
            // this is expected!
        }
    }

    public void testWithEvictedModule() throws Exception {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-dyn-evicted.xml"));
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogNotContaining("+- org1#mod1.2;1+");
        assertLogContaining("+- org6#mod6.1;2.0");
        assertLogContaining("   \\- org1#mod1.2;2.2");
        assertLogContaining("\\- org1#mod1.2;2.2");
    }

    public void testShowEvictedModule() throws Exception {
        dependencyTree.setFile(new File("test/java/org/apache/ivy/ant/ivy-dyn-evicted.xml"));
        dependencyTree.setShowEvicted(true);
        dependencyTree.execute();
        assertLogContaining("Dependency tree for apache-resolve-latest");
        assertLogContaining("+- org1#mod1.2;1+ evicted by [org1#mod1.2;2.2] in apache#resolve-latest;1.0");
        assertLogContaining("+- org6#mod6.1;2.0");
        assertLogContaining("   \\- org1#mod1.2;2.2");
        assertLogContaining("\\- org1#mod1.2;2.2");
    }

}
