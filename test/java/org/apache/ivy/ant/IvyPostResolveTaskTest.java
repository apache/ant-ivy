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
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.CacheCleaner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyPostResolveTaskTest extends TestCase {
    private File cache;

    private IvyPostResolveTask task;

    private Project project;

    protected void setUp() throws Exception {
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

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testWithPreviousResolveInSameBuildAndLessConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default,compile");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndSameConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndWildcard() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndBothWildcard() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
    }

    public void testWithPreviousResolveInSameBuildAndMoreConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        task.setConf("*");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertNotSame("IvyPostResolveTask hasn't performed a resolve where it should have",
            reportBefore, reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testWithoutKeep() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        task.setConf("*"); // will trigger a resolve
        task.setKeep(false); // don't keep the resolve results
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertSame("IvyPostResolveTask has kept the resolve report where it should have",
            reportBefore, reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testInlineWithoutKeep() throws Exception {
        task.setOrganisation("org1");
        task.setModule("mod1.1");
        task.setRevision("2.0");
        task.setInline(true);
        task.setConf("*"); // will trigger a resolve
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertNull("IvyPostResolveTask has kept the resolve report where it should have",
            reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testInlineWithKeep() throws Exception {
        task.setOrganisation("org1");
        task.setModule("mod1.1");
        task.setRevision("2.0");
        task.setInline(true);
        task.setKeep(true);
        task.setConf("*"); // will trigger a resolve
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");

        assertNotNull("IvyPostResolveTask has kept the resolve report where it should have",
            reportAfter);
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndLessConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default,compile");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");
        ResolveReport report2 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndSameConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("default");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");
        ResolveReport report2 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndWildcard() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("default");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");
        ResolveReport report2 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndBothWildcard() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");
        ResolveReport report2 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");

        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", reportBefore,
            reportAfter);
        assertSame("IvyPostResolveTask has performed a resolve where it shouldn't", report1,
            report2);
    }

    public void testWithResolveIdAndPreviousResolveInSameBuildAndMoreConfs() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("compile");
        resolve.setResolveId("testResolveId");
        resolve.execute();

        ResolveReport report1 = (ResolveReport) project
                .getReference("ivy.resolved.report.testResolveId");
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "2.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // perform another resolve
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setConf("*");
        resolve.execute();

        ResolveReport reportBefore = (ResolveReport) project.getReference("ivy.resolved.report");

        task.setConf("*");
        task.setResolveId("testResolveId");
        task.execute();

        ResolveReport reportAfter = (ResolveReport) project.getReference("ivy.resolved.report");
        ResolveReport report2 = (ResolveReport) project
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
