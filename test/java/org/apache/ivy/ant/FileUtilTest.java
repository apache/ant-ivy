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

import org.apache.ivy.util.FileUtil;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link FileUtil}
 *
 * @author Jaikiran Pai
 */
public class FileUtilTest {

    /**
     * Tests that {@link FileUtil#normalize(String)} works as expected for some basic file paths
     *
     * @throws Exception
     */
    @Test
    public void testSimpleNormalize() throws Exception {
        final File ivySettingsFile = new File("test/repositories/ivysettings.xml");
        final File normalizedIvySettingsFile = FileUtil.normalize(ivySettingsFile.getAbsolutePath());
        assertEquals("Unexpected normalization of file path " + ivySettingsFile.getAbsolutePath(), ivySettingsFile.getAbsolutePath(), normalizedIvySettingsFile.getAbsolutePath());
        assertTrue(normalizedIvySettingsFile.getAbsolutePath() + " isn't a file", normalizedIvySettingsFile.isFile());
    }

    /**
     * Tests that {@link FileUtil#normalize(String)} works as expected when passed a path that starts with
     * {@link File#listRoots() filesystem roots}
     *
     * @throws Exception
     */
    @Test
    public void testNormalizeOfFileSystemRootPath() throws Exception {
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

}
