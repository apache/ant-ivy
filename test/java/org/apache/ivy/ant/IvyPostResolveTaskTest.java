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
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.CacheCleaner;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class IvyPostResolveTaskTest {
    private File cache;

    private IvyPostResolveTask task;

    private Project project;

    @Before
    public void setUp() {
        createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        task = new IvyPostResolveTask() {
            public void doExecute() throws BuildException {
                prepareAndCheck();
            }
        };
        task.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
        CacheCleaner.deleteDir(cache);
    }

    @Test
    public void testWithPreviousResolveInSameBuildAndLessConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default,compile");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    @Test
    public void testWithPreviousResolveInSameBuildAndSameConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    @Test
    public void testWithPreviousResolveInSameBuildAndWildcard() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    @Test
    public void testWithPreviousResolveInSameBuildAndBothWildcard() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    @Test
    public void testWithPreviousResolveInSameBuildAndMoreConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        task.setConf("*");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have",
            reportBefore, reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    @Test
    public void testWithoutKeep() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        task.setConf("*"); // will trigger a resolve
        task.setKeep(false); // don't keep the resolve results
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has kept the resolve report where it should have",
            reportBefore, reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    @Test
    public void testInlineWithoutKeep() {
        task.setOrganisation("org1");
        task.setModule("mod1.1");
        task.setRevision("2.0");
        task.setInline(true);
        task.setConf("*"); // will trigger a resolve
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertNull("IvyPostResolveTask has kept the resolve report where it should have",
            reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    @Test
    public void testInlineWithKeep() {
        task.setOrganisation("org1");
        task.setModule("mod1.1");
        task.setRevision("2.0");
        task.setInline(true);
        task.setKeep(true);
        task.setConf("*"); // will trigger a resolve
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");

        assertNotNull("IvyPostResolveTask has kept the resolve report where it should have",
            reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    @Test
    public void testWithResolveIdAndPreviousResolveInSameBuildAndLessConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default,compile");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = project.getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");
        ResolveReport report2 = project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    @Test
    public void testWithResolveIdAndPreviousResolveInSameBuildAndSameConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");
        ResolveReport report2 = project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    @Test
    public void testWithResolveIdAndPreviousResolveInSameBuildAndWildcard() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");
        ResolveReport report2 = project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    @Test
    public void testWithResolveIdAndPreviousResolveInSameBuildAndBothWildcard() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");
        ResolveReport report2 = project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    @Test
    public void testWithResolveIdAndPreviousResolveInSameBuildAndMoreConfs() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = project
                .getReference("ivy.resolved.report.testResolveId");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = project.getReference("ivy.resolved.report");
        ResolveReport report2 = project
                .getReference("ivy.resolved.report.testResolveId");

        assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have",
            reportBefore, reportAfter);
        assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have",
            report1, report2);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(task.getIvyInstance(), organisation, module,
            revision, artifact, type, ext);
    }
}
