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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class IvyCacheFilesetTest {

    private IvyCacheFileset fileset;

    private Project project;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        fileset = new IvyCacheFileset();
        fileset.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
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

    @Test
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

    /**
     * Test must fail with default haltonfailure setting.
     *
     * @throws Exception
     */
    @Test(expected = BuildException.class)
    public void testFailure() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        fileset.setSetid("failure-setid");
        fileset.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     *
     * @throws Exception
     */
    @Test(expected = BuildException.class)
    public void testInvalidPattern() throws Exception {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-invalidcachepattern.xml");
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        fileset.setSetid("simple-setid");
        fileset.execute();
    }

    @Test
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

    @Test
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

    @Test
    public void testGetBaseDir() {
        File base = null;
        base = fileset.getBaseDir(base, new File("x/aa/b/c"));
        assertNull("Base directory was expected to be null", base);

        base = new File("x/aa/b/c").getParentFile().getAbsoluteFile();

        base = fileset.getBaseDir(base, new File("x/aa/b/d/e"));
        assertEquals(new File("x/aa/b").getAbsoluteFile(), base);

        base = fileset.getBaseDir(base, new File("x/ab/b/d"));
        assertEquals(new File("x").getAbsoluteFile(), base);

        final File[] filesytemRoots = File.listRoots();
        final File root1 = filesytemRoots[0];
        final File file1 = new File(root1, "abcd/xyz");
        final File file2 = new File(root1, "pqrs/xyz");
        final File commonBase = fileset.getBaseDir(file1, file2);
        assertEquals("Unexpected common base dir between '" + file1 + "' and '" + file2 + "'", root1.getAbsoluteFile(), commonBase.getAbsoluteFile());

    }

    /**
     * Tests that the {@link IvyCacheFileset} fails with an exception if it can't determine a common base directory
     * while dealing with cached artifacts
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1475">IVY-1475</a> for more details
     */
    @Test
    public void testNoCommonBaseDir() {
        final File[] fileSystemRoots = File.listRoots();
        if (fileSystemRoots.length == 1) {
            // single file system root isn't what we are interested in, in this test method
            return;
        }
        // we expect a BuildException when we try to find a (non-existent) common base dir
        // across file system roots
        expExc.expect(BuildException.class);

        final File root1 = fileSystemRoots[0];
        final File root2 = fileSystemRoots[1];
        final File fileOnRoot1 = new File(root1, "abc/file1");
        final File fileOnRoot2 = new File(root2, "abc/file2");
        fileset.getBaseDir(fileOnRoot1, fileOnRoot2);
        fail("A BuildException was expected when trying to find a common base dir between '" + fileOnRoot1 + "' and '" + fileOnRoot2 + "'");
    }
}
