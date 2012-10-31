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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyDependencyTreeTest extends TestCase {
    private File cache;

    private IvyDependencyTree dependencyTree;

    private AntTestListener antTestListener;

    private Project project;

    protected void setUp() throws Exception {
        createCache();
        project = AntTestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        antTestListener = new AntTestListener(Project.MSG_INFO);
        project.addBuildListener(antTestListener);

        dependencyTree = new IvyDependencyTree();
        dependencyTree.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
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

    public void assertLogContaining(String substring) {
        String realLog = antTestListener.getLog();
        assertTrue("expecting log to contain \"" + substring + "\" log was \"" + realLog + "\"",
            realLog.contains(substring));
    }

    public void assertLogNotContaining(String substring) {
        String realLog = antTestListener.getLog();
        assertFalse("expecting log to contain \"" + substring + "\" log was \"" + realLog + "\"",
            realLog.contains(substring));
    }

    /**
     * Our own personal build listener.
     */
    private class AntTestListener implements BuildListener {
        private int logLevel;

        private StringBuilder buildLog = new StringBuilder();

        /**
         * Constructs a test listener which will ignore log events above the given level.
         */
        public AntTestListener(int logLevel) {
            this.logLevel = logLevel;
        }

        /**
         * Fired before any targets are started.
         */
        public void buildStarted(BuildEvent event) {
        }

        /**
         * Fired after the last target has finished. This event will still be thrown if an error
         * occurred during the build.
         * 
         * @see BuildEvent#getException()
         */
        public void buildFinished(BuildEvent event) {
        }

        /**
         * Fired when a target is started.
         * 
         * @see BuildEvent#getTarget()
         */
        public void targetStarted(BuildEvent event) {
            // System.out.println("targetStarted " + event.getTarget().getName());
        }

        /**
         * Fired when a target has finished. This event will still be thrown if an error occurred
         * during the build.
         * 
         * @see BuildEvent#getException()
         */
        public void targetFinished(BuildEvent event) {
            // System.out.println("targetFinished " + event.getTarget().getName());
        }

        /**
         * Fired when a task is started.
         * 
         * @see BuildEvent#getTask()
         */
        public void taskStarted(BuildEvent event) {
            // System.out.println("taskStarted " + event.getTask().getTaskName());
        }

        /**
         * Fired when a task has finished. This event will still be throw if an error occurred
         * during the build.
         * 
         * @see BuildEvent#getException()
         */
        public void taskFinished(BuildEvent event) {
            // System.out.println("taskFinished " + event.getTask().getTaskName());
        }

        /**
         * Fired whenever a message is logged.
         * 
         * @see BuildEvent#getMessage()
         * @see BuildEvent#getPriority()
         */
        public void messageLogged(BuildEvent event) {
            if (event.getPriority() > logLevel) {
                // ignore event
                return;
            }

            buildLog.append(event.getMessage());
        }

        public String getLog() {
            return buildLog.toString();
        }

    }

}
