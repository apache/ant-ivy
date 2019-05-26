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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.report.ArtifactDownloadReport;
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
    public void testSimple() {
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
    public void testEmptyConf() {
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
     */
    @Test(expected = BuildException.class)
    public void testFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        fileset.setSetid("failure-setid");
        fileset.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testInvalidPattern() {
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-invalidcachepattern.xml");
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        fileset.setSetid("simple-setid");
        fileset.execute();
    }

    /**
     * Test must pass with haltonfailure set to false.
     */
    @Test
    public void testHaltOnFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        fileset.setSetid("haltfailure-setid");
        fileset.setHaltonfailure(false);
        fileset.execute();
    }

    @Test
    public void testWithoutPreviousResolveAndNonDefaultCache() {
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
    public void getBaseDirCommonBaseDir() {
        final File file1 = new File("x/aa/b/c").getParentFile().getAbsoluteFile();
        final File file2 = new File("x/aa/b/d/e");
        final File file3 = new File("x/ab/b/d");

        // A common base deep inside the tree
        File base = fileset.getBaseDir(file1, file2);
        assertEquals(new File("x/aa/b").getAbsoluteFile(), base);

        // A common base on top directory of the tree
        base = fileset.getBaseDir(base, file3);
        assertEquals(new File("x").getAbsoluteFile(), base);

        // A common base only on the fs-root.
        final File[] filesystemRoots = File.listRoots();
        final File root1 = filesystemRoots[0];
        final File file4 = new File(root1, "abcd/xyz");
        final File file5 = new File(root1, "pqrs/xyz");
        final File commonBase = fileset.getBaseDir(file4, file5);
        assertEquals(
            "Unexpected common base dir between '" + file4 + "' and '" + file5 + "'",
            root1.getAbsoluteFile(),
            commonBase.getAbsoluteFile()
        );
    }

    /**
     * Test case for IVY-1475.
     * {@link IvyCacheFileset} must fail with an exception if it cannot determine
     * a common base directory while dealing with cached artifacts.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1475">IVY-1475</a>
     */
    @Test
    public void getBaseDirNoCommonBaseDir() {
        final File[] fileSystemRoots = File.listRoots();
        if (fileSystemRoots.length == 1) {
            // single file system root isn't what we are interested in, in this test method
            return;
        }

        final File root1 = fileSystemRoots[0];
        final File root2 = fileSystemRoots[1];
        final File fileOnRoot1 = new File(root1, "abc/file1");
        final File fileOnRoot2 = new File(root2, "abc/file2");
        File base = fileset.getBaseDir(fileOnRoot1, fileOnRoot2);
        assertNull(base);
    }

    @Test
    public void getBaseDirNullValues() {
        assertNull("Base directory was expected to be null", fileset.getBaseDir(null, new File("a")));
        assertNull("Base directory was expected to be null", fileset.getBaseDir(new File("a"), null));
    }

    @Test
    public void requireCommonBaseDirEmptyList() {
        // we expect a BuildException when we try to find a (non-existent) common base dir
        // across file system roots
        expExc.expect(BuildException.class);
        List<ArtifactDownloadReport> reports = Collections.emptyList();
        fileset.requireCommonBaseDir(reports);
    }

    @Test
    public void requireCommonBaseDirNoCommon() {
        final File[] fileSystemRoots = File.listRoots();
        if (fileSystemRoots.length == 1) {
            // single file system root isn't what we are interested in, in this test method
            return;
        }
        // we expect a BuildException when we try to find a (non-existent) common base dir
        // across file system roots
        expExc.expect(BuildException.class);
        List<ArtifactDownloadReport> reports = Arrays.asList(
            artifactDownloadReport(new File(fileSystemRoots[0], "a/b/c/d")),
            artifactDownloadReport(new File(fileSystemRoots[1], "a/b/e/f"))
        );
        fileset.requireCommonBaseDir(reports);
    }

    @Test
    public void requireCommonBaseDirCommon() {
        List<ArtifactDownloadReport> reports = Arrays.asList(
            artifactDownloadReport(new File("a/b/c/d")),
            artifactDownloadReport(new File("a/b/e/f"))
        );
        assertNotNull(fileset.requireCommonBaseDir(reports));
    }

    private ArtifactDownloadReport artifactDownloadReport(File localFile) {
        ArtifactDownloadReport report = new ArtifactDownloadReport(null);
        report.setLocalFile(localFile);
        return report;
    }
}
