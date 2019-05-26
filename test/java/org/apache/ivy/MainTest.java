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
package org.apache.ivy;

import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.cli.CommandLine;
import org.apache.ivy.util.cli.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MainTest {

    private File cache;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Before
    public void setUp() {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        CacheCleaner.deleteDir(cache);
    }

    @Test
    public void testHelp() throws Exception {
        run(new String[] {"-?"});
    }

    @Test
    public void testBadOption() throws Exception {
        expExc.expect(ParseException.class);
        expExc.expectMessage("Unrecognized option: -bad");

        run(new String[] {"-bad"});
    }

    @Test
    public void testMissingParameter() throws Exception {
        expExc.expect(ParseException.class);
        expExc.expectMessage("no argument for: ivy");

        run(new String[] {"-ivy"});
    }

    @Test
    public void testResolveSimple() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-ivy",
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    @Test
    public void testResolveSimpleWithConfs() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-ivy",
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "-confs", "default"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    @Test
    public void testResolveSimpleWithConfs2() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs", "default",
                "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    @Test
    public void testExtraParams1() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "foo1",
                "foo2"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(2, leftOver.length);
        assertEquals("foo1", leftOver[0]);
        assertEquals("foo2", leftOver[1]);
    }

    @Test
    public void testExtraParams2() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "--",
                "foo1", "foo2"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(2, leftOver.length);
        assertEquals("foo1", leftOver[0]);
        assertEquals("foo2", leftOver[1]);
    }

    @Test
    public void testExtraParams3() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(0, leftOver.length);
    }

    /**
     * Test case for IVY-1355.
     * {@code types} argument to the command line must be parsed correctly when it's passed
     * more than one value for the argument.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1355">IVY-1355</a>
     */
    @Test
    public void testTypes() throws Exception {
        final String[] params = new String[]{"-settings", "test/repositories/ivysettings.xml", "-retrieve",
                "build/test/main/retrieve/[module]/[conf]/[artifact]-[revision].[ext]",
                "-types", "jar", "source"};
        final CommandLine parsedCommand = Main.getParser().parse(params);
        final String[] parsedTypes = parsedCommand.getOptionValues("types");
        assertNotNull("Values for types argument is missing", parsedTypes);
        assertEquals("Unexpected number of values parsed for types argument", 2, parsedTypes.length);
        final Set<String> uniqueParsedTypes = new HashSet<>(Arrays.asList(parsedTypes));
        assertTrue("jar type is missing from the parsed types argument", uniqueParsedTypes.contains("jar"));
        assertTrue("jar type is missing from the parsed types argument", uniqueParsedTypes.contains("source"));
    }

    /**
     * Tests that the {@code overwriteMode} passed for the retrieve command works as expected
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testRetrieveOverwriteMode() throws Exception {
        final String[] args = new String[]{"-settings", "test/repositories/ivysettings.xml", "-retrieve",
                "build/test/main/retrieve/overwrite-test/[artifact].[ext]",
                "-overwriteMode", "different",
                "-ivy", "test/repositories/1/org/mod1/ivys/ivy-5.0.xml"};
        final CommandLine parsedCommand = Main.getParser().parse(args);
        final String parsedOverwriteMode = parsedCommand.getOptionValue("overwriteMode");
        assertEquals("Unexpected overwriteMode parsed", RetrieveOptions.OVERWRITEMODE_DIFFERENT, parsedOverwriteMode);
        // create a dummy file which we expect the retrieve task to overwrite
        final Path retrieveArtifactPath = Paths.get("build/test/main/retrieve/overwrite-test/foo-bar.jar");
        Files.createDirectories(retrieveArtifactPath.getParent());
        Files.write(retrieveArtifactPath, new byte[0]);
        assertEquals("Unexpected content at " + retrieveArtifactPath, 0, Files.readAllBytes(retrieveArtifactPath).length);
        // issue the retrieve (which retrieves the org:foo-bar:2.3.4 artifact)
        run(args);
        // expect the existing jar to be overwritten
        assertTrue("Content at " + retrieveArtifactPath + " was not overwritten by retrieve task", Files.readAllBytes(retrieveArtifactPath).length > 0);
    }

    /**
     * Tests that the {@code makepom} option works as expected
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testMakePom() throws Exception {
        final String pomFilePath = this.tempDir.getRoot().getAbsolutePath() + File.separator + "testmakepom.xml";
        final String[] args = new String[]{"-settings", "test/repositories/ivysettings.xml", "-makepom", pomFilePath,
                "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"};
        final CommandLine parsedCommand = Main.getParser().parse(args);
        final String parsedMakePomPath = parsedCommand.getOptionValue("makepom");
        assertEquals("Unexpected makepom parsed", pomFilePath, parsedMakePomPath);
        assertFalse("pom file " + pomFilePath + " already exists", new File(pomFilePath).exists());
        // run the command
        run(args);
        assertTrue("pom file hasn't been generated at " + pomFilePath, new File(pomFilePath).isFile());
    }

    private void run(String[] args) throws Exception {
        Main.run(Main.getParser(), args);
    }
}
