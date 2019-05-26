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
package org.apache.ivy.core.retrieve;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.retrieve.EndRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.EndRetrieveEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveArtifactEvent;
import org.apache.ivy.core.event.retrieve.StartRetrieveEvent;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MockMessageLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RetrieveTest {

    private Ivy ivy;

    private static boolean systemHasSymlinkAbility;

    @BeforeClass
    public static void beforeClass() {
        final List<File> tmpFilesCreated = new ArrayList<>();
        // create a dummy symlink and see if it works fine
        try {
            final File tmpDir = Files.createTempDirectory(null).toFile();
            tmpFilesCreated.add(tmpDir);

            final Path tmpFile = Files.createTempFile(tmpDir.toPath(), null, null);
            tmpFilesCreated.add(tmpFile.toFile());

            final File symlinkedFile = new File(tmpDir, "symlinked-test-file");
            tmpFilesCreated.add(symlinkedFile);

            // attempt to create the symlink
            Files.createSymbolicLink(symlinkedFile.toPath(), tmpFile);
            systemHasSymlinkAbility = true;
        } catch (IOException ioe) {
            Message.info("Current system is considered as not having symlink ability due to failure to create a test symlink", ioe);
            systemHasSymlinkAbility = false;
        }
        // delete on exit, the tmp files we created
        for (final File file : tmpFilesCreated) {
            file.deleteOnExit();
        }
    }

    @Before
    public void setUp() throws Exception {
        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        TestHelper.createCache();
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_INFO));
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/retrieve"));
        del.execute();
    }

    @Test
    public void testRetrieveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());
    }

    @Test
    public void testRetrieveSameFileConflict() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.1.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[artifact]-[revision].[ext]";
        MockMessageLogger mockLogger = new MockMessageLogger();
        Message.setDefaultLogger(mockLogger);
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.2", "mod1.2",
            "jar", "jar", "default")).exists());
        mockLogger.assertLogDoesntContain("conflict on");
    }

    @Test(expected = RuntimeException.class)
    public void testRetrieveDifferentArtifactsOfSameModuleToSameFile() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[module].[ext]";
        MockMessageLogger mockLogger = new MockMessageLogger();
        Message.setDefaultLogger(mockLogger);
        try {
            ivy.retrieve(md.getModuleRevisionId(),
                getRetrieveOptions().setDestArtifactPattern(pattern));
        } finally {
            mockLogger.assertLogDoesntContain("multiple artifacts");
        }
    }

    @Test
    public void testEvent() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));

        final List<IvyEvent> events = new ArrayList<>();
        ivy.getEventManager().addIvyListener(new IvyListener() {
            public void progress(IvyEvent event) {
                events.add(event);
            }
        });
        ModuleDescriptor md = report.getModuleDescriptor();
        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        assertEquals(4, events.size());
        assertTrue(events.get(0) instanceof StartRetrieveEvent);
        assertTrue(events.get(1) instanceof StartRetrieveArtifactEvent);
        assertTrue(events.get(2) instanceof EndRetrieveArtifactEvent);
        assertTrue(events.get(3) instanceof EndRetrieveEvent);
        EndRetrieveEvent ev = (EndRetrieveEvent) events.get(3);
        assertEquals(1, ev.getNbCopied());
        assertEquals(0, ev.getNbUpToDate());

        events.clear();
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof StartRetrieveEvent);
        assertTrue(events.get(1) instanceof EndRetrieveEvent);
        ev = (EndRetrieveEvent) events.get(1);
        assertEquals(0, ev.getNbCopied());
        assertEquals(1, ev.getNbUpToDate());
    }

    @Test
    public void testRetrieveOverwrite() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";

        // we create a fake old file to see if it is overwritten
        File file = new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar", "default")).getCanonicalFile();
        file.getParentFile().mkdirs();
        file.createNewFile();
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setOverwriteMode("always").setDestArtifactPattern(pattern));
        assertEquals(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").lastModified(),
            file.lastModified());
    }

    @Test
    public void testRetrieveWithSymlinks() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setMakeSymlinks(true).setDestArtifactPattern(pattern));
        assertLinkOrExists(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setMakeSymlinks(true).setDestArtifactPattern(pattern));
        assertLinkOrExists(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));
    }

    /**
     * Tests that retrieve, when invoked with "symlink" enabled, creates the necessary symlink
     * when the artifact being retrieved is a directory instead of a regular file
     *
     * @throws Exception
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1594">IVY-1594</a>
     */
    @Test
    public void testRetrieveZipArtifactWithSymlinks() throws Exception {
        // resolve (inline) with org1:mod1.1:3.0 as a dependency
        final ResolveReport report = ivy.resolve(new ModuleRevisionId(new ModuleId("org1", "mod1.7"), "3.0"),
                getResolveOptions(new String[]{"*"}), false);
        assertNotNull("Resolution report is null", report);
        final ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull("Module descriptor is null", md);

        final String retrievePattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision]";
        ivy.retrieve(md.getModuleRevisionId(),
                getRetrieveOptions().setMakeSymlinks(true).setDestArtifactPattern(retrievePattern));

        final String expectedRetrieveLocation = IvyPatternHelper.substitute(retrievePattern, "org1", "mod1.7",
                "3.0", "zipped-artifact", null, null, "default");
        // make sure it's retrieved as a symlink (on systems that support symlink)
        assertLinkOrExists(expectedRetrieveLocation);
    }

    /**
     * This test is here to just test the deprecated {@code symlinkmass} option for retrieve task.
     * A version or two down the line, after 2.5 release, we can remove this test and the option
     * altogether.
     *
     * @throws Exception if something goes wrong
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testRetrieveWithSymlinksMass() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setMakeSymlinksInMass(true).setDestArtifactPattern(pattern));
        assertLinkOrExists(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));

        pattern = "build/test/retrieve/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setMakeSymlinksInMass(true).setDestArtifactPattern(pattern));
        assertLinkOrExists(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar", "default"));
    }

    /**
     * If the system {@link #systemHasSymlinkAbility has symlink ability} then asserts that the
     * passed {@code filePath} is a {@link Files#isSymbolicLink(Path) symbolic link}. Else asserts
     * that the {@code filePath} {@link Files#exists(Path, LinkOption...) exists}.
     *
     * @param filePath String
     */
    private void assertLinkOrExists(final String filePath) {
        if (systemHasSymlinkAbility) {
            assertTrue(filePath + " was expected to be a symlink", Files.isSymbolicLink(Paths.get(filePath)));
            return;
        }
        Message.info("System doesn't have symlink ability so checking if path " + filePath + " exists instead of checking for it to be a symlink");
        assertTrue("Missing " + filePath, Files.exists(Paths.get(filePath)));
    }

    @Test
    public void testRetrieveWithVariable() throws Exception {
        // mod1.1 depends on mod1.2
        ivy.setVariable("retrieve.dir", "retrieve");
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/${retrieve.dir}/[module]/[conf]/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());

        pattern = "build/test/${retrieve.dir}/[module]/[conf]/[type]s/[artifact]-[revision].[ext]";
        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));
        pattern = IvyPatternHelper.substituteVariable(pattern, "retrieve.dir", "retrieve");
        assertTrue(new File(IvyPatternHelper.substitute(pattern, "org1", "mod1.2", "2.0", "mod1.2",
            "jar", "jar", "default")).exists());
    }

    @Test
    public void testRetrieveReport() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org20/mod20.1/ivys/ivy-1.2.xml").toURI().toURL(),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        ModuleRevisionId mrid = md.getModuleRevisionId();
        RetrieveOptions options = getRetrieveOptions();
        options.setConfs(new String[] {"A"});
        Map<ArtifactDownloadReport, Set<String>> artifactsToCopy = ivy.getRetrieveEngine()
                .determineArtifactsToCopy(mrid,
                    "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(2, artifactsToCopy.size());

        options.setConfs(new String[] {"B"});
        artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(mrid,
            "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(2, artifactsToCopy.size());

        options.setConfs(new String[] {"A", "B"});
        artifactsToCopy = ivy.getRetrieveEngine().determineArtifactsToCopy(mrid,
            "build/test/retrieve/[module]/[conf]/[artifact]-[revision].[ext]", options);
        assertEquals(3, artifactsToCopy.size());
    }

    @Test
    public void testUnpack() throws Exception {
        ResolveOptions roptions = getResolveOptions(new String[] {"*"});

        URL url = new File("test/repositories/1/packaging/module1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, roptions);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[organization]/[module]/[conf]/[type]s/[artifact]-[revision](.[ext])";

        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));

        File dest = new File("build/test/retrieve/packaging/module2/default/jars/module2-1.0");
        assertTrue(dest.exists());
        assertTrue(dest.isDirectory());
        File[] jarContents = dest.listFiles();
        Arrays.sort(jarContents);
        assertEquals(new File(dest, "META-INF"), jarContents[0]);
        assertEquals(new File(dest, "test.txt"), jarContents[1]);
        assertEquals(new File(dest, "META-INF/MANIFEST.MF"), jarContents[0].listFiles()[0]);
    }

    @Test
    public void testUnpackSync() throws Exception {
        ResolveOptions roptions = getResolveOptions(new String[] {"*"});

        URL url = new File("test/repositories/1/packaging/module1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, roptions);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        String pattern = "build/test/retrieve/[organization]/[module]/[conf]/[type]s/[artifact]-[revision](.[ext])";

        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setSync(true).setDestArtifactPattern(pattern));

        File dest = new File("build/test/retrieve/packaging/module2/default/jars/module2-1.0");
        assertTrue(dest.exists());
        assertTrue(dest.isDirectory());
        File[] jarContents = dest.listFiles();
        Arrays.sort(jarContents);
        assertEquals(new File(dest, "META-INF"), jarContents[0]);
        assertEquals(new File(dest, "test.txt"), jarContents[1]);
        assertEquals(new File(dest, "META-INF/MANIFEST.MF"), jarContents[0].listFiles()[0]);
    }

    /**
     * Test case for IVY-1478.
     * {@link RetrieveEngine} must retrieve artifacts with the correct extension if the artifact
     * is unpacked.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1478">IVY-1478</a>
     */
    @Test
    public void testUnpackExt() throws Exception {
        final ResolveOptions roptions = getResolveOptions(new String[] {"*"});

        final URL url = new File("test/repositories/1/packaging/module10/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        final ResolveReport report = ivy.resolve(url, roptions);
        assertFalse("Resolution report has errors", report.hasError());
        final ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull("Module descriptor from report was null", md);

        final String pattern = "build/test/retrieve/[organization]/[module]/[conf]/[type]s/[artifact]-[revision](.[ext])";

        ivy.retrieve(md.getModuleRevisionId(),
            getRetrieveOptions().setDestArtifactPattern(pattern));

        final File dest = new File("build/test/retrieve/packaging/module9/default/jars/module9-1.0.jar");
        assertTrue("Retrieved artifact is missing at " + dest.getAbsolutePath(), dest.exists());
        assertTrue("Retrieved artifact at " + dest.getAbsolutePath() + " is not a file", dest.isFile());
    }

    /**
     * Test case for IVY-1498.
     * More than one retrieve, one having symlink enabled and the other not,
     * must not lead to repository cache corruption.
     * <p>
     * The test does the following:
     * </p>
     * <ul>
     * <li>
     * Resolves a module, let's call it "irrelevant-A" which has a dependency on
     * "org:foo-bar:1.2.3"
     * </li>
     * <li>
     * Invokes a retrieve RT1, with {@link RetrieveOptions#setMakeSymlinks(boolean) symlinks true}
     * on that resolved module with a pattern that translates to a path "a/b/c/foo-bar.jar". When
     * the RT1 retrieve is done, the path "a/b/c/foo-bar.jar" will be a symlink to a path
     * "repo/cache/org/foo-bar/foo-bar-1.2.3.jar" in the Ivy cache.
     * All fine so far.
     * </li>
     * <li>
     * We then resolve another module, let's call it "irrelevant-B" which has a dependency on
     * "org:foo-bar:2.3.4"
     * </li>
     * <li>
     * Next, do a new retrieve RT2, on this newly resolved module with
     * {@link RetrieveOptions#setMakeSymlinks(boolean) symlinks false} and
     * {@link RetrieveOptions#getOverwriteMode() overwrite semantics enabled}
     * and with the same pattern as before), that translates to a path "a/b/c/foo-bar.jar".
     * </li>
     * </ul>
     * <p>
     * When RT2 retrieve is done, we expect the path "a/b/c/foo-bar.jar" will *not* be a symlink
     * and instead be an actual file that represents the org:foo-bar:2.3.4 artifact jar.
     * </p>
     * <p>
     * We also expect that this RT2 retrieve will not update/overwrite the file at path
     * "repo/cache/org/foo-bar/foo-bar-1.2.3.jar" in the Ivy cache - the file that was the end
     * target of the symlink generated by the previous RT1.
     * </p>
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1498">IVY-1498</a>
     */
    @Test
    public void testRetrieveWithSymlinkToggling() throws Exception {
        // org:mod1:2.0 depends on org:foo-bar:1.2.3
        final ResolveReport resolve1Report = ivy.resolve(new File("test/repositories/1/org/mod1/ivys/ivy-2.0.xml").toURI().toURL(),
                getResolveOptions(new String[]{"*"}));
        assertNotNull("Resolve report isn't available", resolve1Report);
        assertNotNull("Module descriptor missing in resolve report", resolve1Report.getModuleDescriptor());

        // verify the downloaded dependency artifact in cache
        final ModuleRevisionId fooBar123Mrid = ModuleRevisionId.newInstance("org", "foo-bar", "1.2.3");
        final ArtifactDownloadReport[] downloadReports = resolve1Report.getArtifactsReports(fooBar123Mrid);
        assertNotNull("No artifact download report found for the " + fooBar123Mrid + " dependency", downloadReports);
        assertEquals("Unexpected number for artifact download reports for the " + fooBar123Mrid + " dependency", 1, downloadReports.length);
        final File fooBar123ArtifactInCache = downloadReports[0].getLocalFile();
        assertNotNull("Local file for " + fooBar123Mrid + " missing in download report", fooBar123ArtifactInCache);
        assertTrue("Artifact file for " + fooBar123Mrid + " isn't a file, in cache at " + fooBar123ArtifactInCache,
                fooBar123ArtifactInCache.isFile());
        final byte[] fooBar123ArtifactContentsInCache = Files.readAllBytes(fooBar123ArtifactInCache.toPath());
        assertTrue("Artifact content was empty at " + new String(fooBar123ArtifactContentsInCache), fooBar123ArtifactContentsInCache.length > 0);


        // now do a retrieve of the resolved module
        final String retrievePattern = "build/test/retrieve/symlink-test/[module]/[artifact].[ext]";
        ivy.retrieve(resolve1Report.getModuleDescriptor().getModuleRevisionId(),
                getRetrieveOptions().setMakeSymlinks(true).setOverwriteMode(RetrieveOptions.OVERWRITEMODE_ALWAYS)
                        .setDestArtifactPattern(retrievePattern));
        // we expect org:foo-bar:1.2.3 to have been retrieved
        final String retrievedArtifactSymlinkPath = IvyPatternHelper.substitute(retrievePattern, "org", "foo-bar",
                "1.2.3", "foo-bar", "jar", "jar", "default");
        assertLinkOrExists(retrievedArtifactSymlinkPath);

        // get hold of the contents of the retrieved artifact
        final byte[] retrievedArtifactContent = Files.readAllBytes(Paths.get(retrievedArtifactSymlinkPath));
        // compare it to the contents of org:foo-bar:1.2.3 artifact in repo cache. Should be the same
        assertTrue("Unexpected content in the retrieved artifact at " + retrievedArtifactSymlinkPath,
                Arrays.equals(fooBar123ArtifactContentsInCache, retrievedArtifactContent));

        // let this retrieved artifact file stay and let's now try and resolve a module and then
        // retrieve a different version of the same artifact, *without* symlinking enabled, which
        // will end up at this exact location org:mod1:2.0 depends on org:foo-bar:1.2.3
        final ResolveReport resolve2Report = ivy.resolve(new File("test/repositories/1/org/mod1/ivys/ivy-5.0.xml").toURI().toURL(),
                getResolveOptions(new String[]{"*"}));
        assertNotNull("Resolve report isn't available", resolve2Report);
        assertNotNull("Module descriptor missing in resolve report", resolve2Report.getModuleDescriptor());

        // verify the downloaded dependency artifact in cache
        final ModuleRevisionId fooBar234Mrid = ModuleRevisionId.newInstance("org", "foo-bar", "2.3.4");
        final ArtifactDownloadReport[] foobar234report = resolve2Report.getArtifactsReports(fooBar234Mrid);
        assertNotNull("No artifact download report found for the " + fooBar234Mrid + " dependency", foobar234report);
        assertEquals("Unexpected number for artifact download reports for the " + fooBar234Mrid + " dependency", 1, foobar234report.length);
        final File foobar234InCache = foobar234report[0].getLocalFile();
        assertNotNull("Local file for " + fooBar234Mrid + " missing in download report", foobar234InCache);
        assertTrue("Artifact file for " + fooBar234Mrid + " isn't a file, in cache at " + foobar234InCache,
                foobar234InCache.isFile());
        final byte[] foobar234CacheFileContents = Files.readAllBytes(foobar234InCache.toPath());
        assertTrue("Artifact content was empty at " + new String(foobar234CacheFileContents), foobar234CacheFileContents.length > 0);

        // do the retrieve with symlinks disabled
        ivy.retrieve(resolve2Report.getModuleDescriptor().getModuleRevisionId(),
                getRetrieveOptions().setMakeSymlinks(false).setDestArtifactPattern(retrievePattern)
                        .setOverwriteMode(RetrieveOptions.OVERWRITEMODE_ALWAYS));
        // we expect org:foo-bar:2.3.4 to have been retrieved
        final Path secondRetrieveArtifactPath = new File(IvyPatternHelper.substitute(retrievePattern, "org", "foo-bar",
                "2.3.4", "foo-bar", "jar", "jar", "default")).toPath();
        assertTrue("Artifact wasn't retrieved to " + secondRetrieveArtifactPath, Files.exists(secondRetrieveArtifactPath));
        // expected to be a regular file and not a symlink
        assertFalse("Artifact retrieved at " + secondRetrieveArtifactPath + " wasn't expected to be a "
                + "symlink", Files.isSymbolicLink(secondRetrieveArtifactPath));

        // get hold of the contents of the retrieved artifact
        final byte[] secondRetrievedArtifactContents = Files.readAllBytes(secondRetrieveArtifactPath);
        // compare it to the contents of org:foo-bar:2.3.4 artifact in repo cache. Should be the same
        assertTrue("Unexpected content in the retrieved artifact at " + secondRetrieveArtifactPath,
                Arrays.equals(foobar234CacheFileContents, secondRetrievedArtifactContents));

        // also make sure that this retrieved content hasn't messed up the other (unrelated) file,
        // in the cache, that was previously symlinked to this retrieved path
        assertTrue("A second retrieve of an artifact has corrupted an unrelated (previously symlinked) artifact in cache",
                Arrays.equals(fooBar123ArtifactContentsInCache, Files.readAllBytes(Paths.get(fooBar123ArtifactInCache.getPath()))));


    }

    private RetrieveOptions getRetrieveOptions() {
        return new RetrieveOptions();
    }

    private ResolveOptions getResolveOptions(final String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }

}
