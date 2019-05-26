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
package org.apache.ivy.plugins.repository.vfs;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.util.FileUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing was the single biggest hurdle I faced. I have tried to provide a complete test
 * suite that covers all protocols and which can be easily extended. It does differ - somewhat - in
 * structure from the resolver/repository test suites. Setting up smb, ftp, sftp will undoubtedly be
 * your biggest headache (it was mine). Here are a few notes about the setup:
 * <ul>
 * <li>the VFS test suite uses the build/test/repositories area;
 * when setting samba, sftp, etc. the corresponding user needs both read and write privileges.</li>
 * <li>the tests assume that the user and password is the same for all services.</li>
 * <li>a limited amount of configuration is available by setting the following properties in the
 * <code>ivy.properties</code> file:</li>
 * </ul>
 * <pre>
 *   vfs.host
 *   vfs.username
 *   vfs.password
 *   vfs.samba_share
 * </pre>
 * Running the test requires that commons-io and ant jars are on the classpath. Also, I would
 * recommend that at some time the tests be converted from straight junit to something which better
 * supports functional testing. Although somewhat crude, I am using jsystem
 * (http://jsystemtest.sourceforge.net/) in other projects and am finding it a much better solution
 * than straight junit.
 * <p>Stephen Nesbitt</p>
 */
public class VfsRepositoryTest {
    private VfsRepository repo = null;

    private VfsTestHelper helper = null;

    private File scratchDir = null;

    @Before
    public void setUp() throws Exception {
        helper = new VfsTestHelper();
        repo = new VfsRepository();
        scratchDir = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR,
            VfsTestHelper.SCRATCH_DIR));
        scratchDir.mkdir();
    }

    @After
    public void tearDown() {
        repo = null;
        if (scratchDir.exists()) {
            FileUtil.forceDelete(scratchDir);
        }
    }

    /**
     * Basic validation of happy path put - valid VFS URI and no conflict with existing file
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testPutValid() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        String destFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource);

        for (VfsURI vfsURI : helper.createVFSUriSet(destResource)) {
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }

            repo.put(new File(srcFile), vfsURI.toString(), false);
            assertTrue("Put didn't happen. Src VfsURI: " + vfsURI.toString()
                    + ".\nExpected file: " + destFile, new File(srcFile).exists());
        }
    }

    /**
     * Validate that we can overwrite an existing file
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testPutOverwriteTrue() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        File destFile = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource));

        for (VfsURI vfsURI : helper.createVFSUriSet(destResource)) {
            // remove existing scratch dir and populate it with an empty file
            // that we can overwrite. We do this so that we can test file sizes.
            // seeded file has length 0, while put file will have a length > 0
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();

            repo.put(new File(srcFile), vfsURI.toString(), true);
            assertTrue("Put didn't happen. Src VfsURI: " + vfsURI.toString()
                    + ".\nExpected file: " + destFile, new File(srcFile).exists());
            assertNotEquals("Zero file size indicates file not overwritten", 0,
                    destFile.length());
        }
    }

    /**
     * Validate that we put will respect a request not to overwrite an existing file
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = IOException.class)
    public void testPutOverwriteFalse() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        File destFile = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource));
        destFile.getParentFile().mkdirs();
        destFile.createNewFile();

        for (VfsURI vfsURI : helper.createVFSUriSet(destResource)) {
            repo.put(new File(srcFile), vfsURI.toString(), false);
        }
    }

    /**
     * Test the retrieval of an artifact from the repository creating a new artifact
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetNoExisting() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String testFile = FileUtil.concat(scratchDir.getAbsolutePath(), testResource);

        for (VfsURI vfsURI : helper.createVFSUriSet(testResource)) {
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }

            repo.get(vfsURI.toString(), new File(testFile));
            assertTrue("Expected file: " + testFile + "not found. Failed vfsURI: "
                    + vfsURI.toString(), new File(testFile).exists());
        }
    }

    /**
     * Test the retrieval of an artifact from the repository overwriting an existing artifact
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetOverwriteExisting() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        File testFile = new File(FileUtil.concat(scratchDir.getAbsolutePath(), testResource));

        for (VfsURI vfsURI : helper.createVFSUriSet(testResource)) {
            // setup - remove existing scratch area and populate with a file to override
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            testFile.getParentFile().mkdirs();
            testFile.createNewFile();

            repo.get(vfsURI.toString(), testFile);
            assertTrue("Expected file: " + testFile + "not found. Failed vfsURI: "
                    + vfsURI.toString(), testFile.exists());
            assertNotEquals("Zero file size indicates file not overwritten", 0,
                    testFile.length());
        }
    }

    /**
     * Validate that we get a non null Resource instance when passed a well-formed VfsURI pointing
     * to an existing file
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetResourceValidExist() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;

        for (VfsURI vfsURI : helper.createVFSUriSet(testResource)) {
            assertNotNull(repo.getResource(vfsURI.toString()));
        }
    }

    /**
     * Validate that we get a non null Resource instance when passed a well-formed VfsURI pointing
     * to a non-existent file.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testGetResourceValidNoExist() throws Exception {
        String testResource = VfsTestHelper.SCRATCH_DIR + "/nosuchfile.jar";

        for (VfsURI vfsURI : helper.createVFSUriSet(testResource)) {
            // make sure the declared resource does not exist
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            assertNotNull(repo.getResource(vfsURI.toString()));
        }
    }

}
