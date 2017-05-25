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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import junit.framework.TestCase;

public class IvyCacheFilesetTest extends TestCase {

    private IvyCacheFileset fileset;

    private Project project;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        fileset = new IvyCacheFileset();
        fileset.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        fileset.setSetid("simple-setid");
        fileset.execute();
        Object ref = project.getReference("simple-setid");
        assertNotNull(ref);
        assertTrue(ref instanceof FileSet);
        FileSet fs = (FileSet) ref;
        DirectoryScanner directoryScanner = fs.getDirectoryScanner(project);
        assertEquals(1, directoryScanner.getIncludedFiles().length);
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(),
            new File(directoryScanner.getBasedir(), directoryScanner.getIncludedFiles()[0])
                    .getAbsolutePath());
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(fileset.getIvyInstance(), organisation, module,
            revision, artifact, type, ext);
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext, File cache) {
        return TestHelper.getArchiveFileInCache(fileset.getIvyInstance(), organisation, module,
            revision, artifact, type, ext);
    }

    public void testEmptyConf() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-108.xml");
        fileset.setSetid("emptyconf-setid");
        fileset.setConf("empty");
        fileset.execute();
        Object ref = project.getReference("emptyconf-setid");
        assertNotNull(ref);
        assertTrue(ref instanceof FileSet);
        FileSet fs = (FileSet) ref;
        DirectoryScanner directoryScanner = fs.getDirectoryScanner(project);
        directoryScanner.scan();
        assertEquals(0, directoryScanner.getIncludedFiles().length);
    }

    public void testFailure() throws Exception {
        try {
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            fileset.setSetid("failure-setid");
            fileset.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testInvalidPattern() throws Exception {
        try {
            project.setProperty("ivy.settings.file",
                "test/repositories/ivysettings-invalidcachepattern.xml");
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
            fileset.setSetid("simple-setid");
            fileset.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            fileset.setSetid("haltfailure-setid");
            fileset.setHaltonfailure(false);
            fileset.execute();
        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }

    public void testWithoutPreviousResolveAndNonDefaultCache() throws Exception {
        File cache2 = new File("build/cache2");
        cache2.mkdirs();

        try {
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
            fileset.setSetid("simple-setid");
            System.setProperty("ivy.cache.dir", cache2.getAbsolutePath());
            fileset.execute();
            Object ref = project.getReference("simple-setid");
            assertNotNull(ref);
            assertTrue(ref instanceof FileSet);
            FileSet fs = (FileSet) ref;
            DirectoryScanner directoryScanner = fs.getDirectoryScanner(project);
            assertEquals(1, directoryScanner.getIncludedFiles().length);
            assertEquals(
                getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar", cache2)
                        .getAbsolutePath(), new File(directoryScanner.getBasedir(),
                        directoryScanner.getIncludedFiles()[0]).getAbsolutePath());
        } finally {
            Delete del = new Delete();
            del.setProject(new Project());
            del.setDir(cache2);
            del.execute();
        }
    }

    public void testGetBaseDir() {
        File base = null;
        base = fileset.getBaseDir(base, new File("x/aa/b/c"));
        assertEquals(new File("x/aa/b").getAbsoluteFile(), base);

        base = fileset.getBaseDir(base, new File("x/aa/b/d/e"));
        assertEquals(new File("x/aa/b").getAbsoluteFile(), base);

        base = fileset.getBaseDir(base, new File("x/ab/b/d"));
        assertEquals(new File("x").getAbsoluteFile(), base);
    }
}
