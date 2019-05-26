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

import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link FileUtil}
 *
 * @author Jaikiran Pai
 */
public class FileUtilTest {

    private static boolean symlinkCapable = false;

    @BeforeClass
    public static void beforeClass() {
        try {
            final Path tmpFile = Files.createTempFile(null, null);
            tmpFile.toFile().deleteOnExit();
            final Path symlink = Files.createSymbolicLink(Paths.get(Files.createTempDirectory(null).toString(),
                    "symlink-test-file"), tmpFile);
            symlinkCapable = true;
            symlink.toFile().deleteOnExit();
        } catch (IOException ioe) {
            // ignore and move on
            symlinkCapable = false;
            Message.info("Current system isn't considered to have symlink capability due to ", ioe);
        }
    }

    /**
     * Tests that {@link FileUtil#normalize(String)} works as expected for some basic file paths
     */
    @Test
    public void testSimpleNormalize() {
        final File ivySettingsFile = new File("test/repositories/ivysettings.xml");
        final File normalizedIvySettingsFile = FileUtil.normalize(ivySettingsFile.getAbsolutePath());
        assertEquals("Unexpected normalization of file path " + ivySettingsFile.getAbsolutePath(), ivySettingsFile.getAbsolutePath(), normalizedIvySettingsFile.getAbsolutePath());
        assertTrue(normalizedIvySettingsFile.getAbsolutePath() + " isn't a file", normalizedIvySettingsFile.isFile());
    }

    /**
     * Tests that {@link FileUtil#normalize(String)} works as expected when passed a path that starts with
     * {@link File#listRoots() filesystem roots}
     */
    @Test
    public void testNormalizeOfFileSystemRootPath() {
        for (final File fileSystemRoot : File.listRoots()) {
            final File normalized = FileUtil.normalize(fileSystemRoot.getPath());
            assertNotNull("Normalized path was null for " + fileSystemRoot, normalized);
            assertEquals("Unexpected normalized path for " + fileSystemRoot, fileSystemRoot.getPath(), normalized.getPath());

            // use . and .. characters in the path to test out the normalize method
            final String pathOne = fileSystemRoot.getPath() + "." + File.separator;
            assertEquals("Unexpected normalization of " + pathOne, fileSystemRoot.getPath(), FileUtil.normalize(pathOne).getPath());

            final String pathTwo = fileSystemRoot.getPath() + "." + File.separator + "foo-bar";
            assertEquals("Unexpected normalization of " + pathTwo, fileSystemRoot.getPath() + "foo-bar",
                    FileUtil.normalize(pathTwo).getPath());

            final String pathThree = fileSystemRoot.getPath() + "foo-bar" + File.separator + ".." + File.separator;
            assertEquals("Unexpected normalization of " + pathThree, fileSystemRoot.getPath(),
                    FileUtil.normalize(pathThree).getPath());

            // append some additional file separator characters to the file system root and normalize it
            final String pathFour = fileSystemRoot.getPath() + File.separator + File.separator + File.separator;
            assertEquals("Unexpected normalization of " + pathFour, fileSystemRoot.getPath(), FileUtil.normalize(pathFour).getPath());

            final String pathFive = fileSystemRoot.getPath() + File.separator + File.separator + "abcd";
            assertEquals("Unexpected normalization of path " + pathFive, fileSystemRoot.getPath() + "abcd", FileUtil.normalize(pathFive).getPath());

            final String pathSix = fileSystemRoot.getPath() + File.separator + File.separator + "1234" + File.separator;
            assertEquals("Unexpected normalization of path " + pathSix, fileSystemRoot.getPath() + "1234", FileUtil.normalize(pathSix).getPath());

            final String pathSeven = fileSystemRoot.getPath() + "helloworld" + File.separator + ".." + File.separator + File.separator + File.separator + "helloworld";
            assertEquals("Unexpected normalization of path " + pathSeven, fileSystemRoot.getPath() + "helloworld", FileUtil.normalize(pathSeven).getPath());

        }
    }

    /**
     * Tests that the call to {@link FileUtil#copy(File, File, CopyProgressListener)} doesn't corrupt
     * the source file if the destination file resolves back to the source file being copied
     *
     * @throws Exception
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1602">IVY-1602</a> for more details
     */
    @Test
    public void testCopyOfSameFile() throws Exception {
        Assume.assumeTrue("Skipping test due to system not having symlink capability", symlinkCapable);
        final Path srcDir = Files.createTempDirectory(null);
        srcDir.toFile().deleteOnExit();
        // create a src file
        final Path srcFile = Paths.get(srcDir.toString(), "helloworld.txt");
        srcFile.toFile().deleteOnExit();
        final byte[] fileContent = "Hello world!!!".getBytes(StandardCharsets.UTF_8);
        Files.write(srcFile, fileContent);

        final Path destDir = Paths.get(Files.createTempDirectory(null).toString(), "symlink-dest");
        destDir.toFile().deleteOnExit();
        // now create a symlink to the dir containing the src file we intend to copy later
        Files.createSymbolicLink(destDir, srcDir);
        // at this point destDir is a symlink to the srcDir and the srcDir contains the srcFile.
        // we now attempt to copy the srcFile to a destination which resolves back the same srcFile
        final Path destFile = Paths.get(destDir.toString(), srcFile.getFileName().toString());
        FileUtil.copy(srcFile.toFile(), destFile.toFile(), null, false);
        // make sure the copy didn't corrupt the source file
        Assert.assertTrue("Unexpected content in source file " + srcFile, Arrays.equals(fileContent, Files.readAllBytes(srcFile)));
        // also check the dest file has the same content as source file after the copy operation
        Assert.assertTrue("Unexpected content in dest file " + destFile, Arrays.equals(fileContent, Files.readAllBytes(destFile)));

        // do the same tests now with overwrite = true
        FileUtil.copy(srcFile.toFile(), destFile.toFile(), null, true);
        // make sure the copy didn't corrupt the source file
        Assert.assertTrue("Unexpected content in source file " + srcFile, Arrays.equals(fileContent, Files.readAllBytes(srcFile)));
        // also check the dest file has the same content as source file after the copy operation
        Assert.assertTrue("Unexpected content in dest file " + destFile, Arrays.equals(fileContent, Files.readAllBytes(destFile)));
    }

}
