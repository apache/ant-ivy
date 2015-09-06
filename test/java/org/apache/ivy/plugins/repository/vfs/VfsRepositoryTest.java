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
package org.apache.ivy.plugins.repository.vfs;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.ivy.util.FileUtil;

import junit.framework.TestCase;

/**
 * Testing Testing was the single biggest hurdle I faced. I have tried to provide a complete test
 * suite that covers all protocols and which can be easily extended. It does differ - somewhat - in
 * structure from the resolver/repository test suites. Setting up smb, ftp, sftp will undoubtedly be
 * your biggest headache (it was mine). Here are a few notes about the setup: - the VFS test suite
 * uses the build/test/repositories area - when setting samba, sftp, etc. the corresponding user
 * needs both read and write privileges. - the tests assume that the user and password is the same
 * for all services. - a limited amount of configuration is available by setting the following
 * properties in the ivy.properties file: * vfs.host * vfs.username * vfs.password * vfs.samba_share
 * Running the test requires that commons-io and ant.jar are on the classpath. Also, I would
 * recommend that at some time the tests be converted from straight junit to something which betters
 * supports functional testing. Although somewhat crude I am using jsystem
 * (http://jsystemtest.sourceforge.net/) in other projects and am finding it a much better solution
 * than straight junit. Stephen Nesbitt
 */
public class VfsRepositoryTest extends TestCase {
    private VfsRepository repo = null;

    private VfsTestHelper helper = null;

    private File scratchDir = null;

    public VfsRepositoryTest(String arg0) throws Exception {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        helper = new VfsTestHelper();
        repo = new VfsRepository();
        scratchDir = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR,
            VfsTestHelper.SCRATCH_DIR));
        scratchDir.mkdir();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        repo = null;
        if (scratchDir.exists()) {
            FileUtil.forceDelete(scratchDir);
        }
    }

    /**
     * Basic validation of happy path put - valid VFS URI and no conflict with existing file
     * 
     * @throws Exception
     */
    public void testPutValid() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        String destFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource);

        Iterator vfsURIs = helper.createVFSUriSet(destResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }

            try {
                repo.put(new File(srcFile), vfsURI.toString(), false);
                if (!new File(srcFile).exists()) {
                    fail("Put didn't happen. Src VfsURI: " + vfsURI.toString()
                            + ".\nExpected file: " + destFile);
                }
            } catch (IOException e) {
                fail("Caught unexpected IOException on Vfs URI: " + vfsURI.toString() + "\n"
                        + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Validate that we can overwrite an existing file
     * 
     * @throws Exception
     */
    public void testPutOverwriteTrue() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        File destFile = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource));

        Iterator vfsURIs = helper.createVFSUriSet(destResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();

            // remove existing scratch dir and populate it with an empty file
            // that we can overwrite. We do this so that we can test file sizes.
            // seeded file has length 0, while put file will have a length > 0
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();

            try {
                repo.put(new File(srcFile), vfsURI.toString(), true);
                if (!new File(srcFile).exists()) {
                    fail("Put didn't happen. Src VfsURI: " + vfsURI.toString()
                            + ".\nExpected file: " + destFile);
                }
                if (destFile.length() == 0) {
                    fail("Zero file size indicates file not overwritten");
                }
            } catch (IOException e) {
                fail("Caught unexpected IOException on Vfs URI: " + vfsURI.toString() + "\n"
                        + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Validate that we put will respect a request not to overwrite an existing file
     * 
     * @throws Exception
     */
    public void testPutOverwriteFalse() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String srcFile = FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, testResource);
        String destResource = VfsTestHelper.SCRATCH_DIR + "/" + testResource;
        File destFile = new File(FileUtil.concat(VfsTestHelper.TEST_REPO_DIR, destResource));
        destFile.getParentFile().mkdirs();
        destFile.createNewFile();

        Iterator vfsURIs = helper.createVFSUriSet(destResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();

            try {
                repo.put(new File(srcFile), vfsURI.toString(), false);
                fail("Did not throw expected IOException from attempted overwrite of existing file");
            } catch (IOException e) {
            }
        }
    }

    /**
     * Test the retrieval of an artifact from the repository creating a new artifact
     * 
     * @throws Exception
     */
    public void testGetNoExisting() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        String testFile = FileUtil.concat(scratchDir.getAbsolutePath(), testResource);

        Iterator vfsURIs = helper.createVFSUriSet(testResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }

            try {
                repo.get(vfsURI.toString(), new File(testFile));
                if (!new File(testFile).exists()) {
                    fail("Expected file: " + testFile + "not found. Failed vfsURI: "
                            + vfsURI.toString());
                }
            } catch (IOException e) {
                fail("Caught unexpected IOException on Vfs URI: " + vfsURI.toString() + "\n"
                        + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Test the retrieval of an artifact from the repository overwriting an existing artifact
     * 
     * @throws Exception
     */
    public void testGetOverwriteExisting() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;
        File testFile = new File(FileUtil.concat(scratchDir.getAbsolutePath(), testResource));

        Iterator vfsURIs = helper.createVFSUriSet(testResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();

            // setup - remove existing scratch area and populate with a file to override
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            testFile.getParentFile().mkdirs();
            testFile.createNewFile();

            try {
                repo.get(vfsURI.toString(), testFile);
                if (!testFile.exists()) {
                    fail("Expected file: " + testFile + "not found. Failed vfsURI: "
                            + vfsURI.toString());
                }
                if (testFile.length() == 0) {
                    fail("Zero file size indicates file not overwritten");
                }
            } catch (IOException e) {
                fail("Caught unexpected IOException on Vfs URI: " + vfsURI.toString() + "\n"
                        + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Validate that we get a non null Resource instance when passed a well-formed VfsURI pointing
     * to an existing file
     */
    public void testGetResourceValidExist() throws Exception {
        String testResource = VfsTestHelper.TEST_IVY_XML;

        Iterator vfsURIs = helper.createVFSUriSet(testResource).iterator();

        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();
            try {
                assertNotNull(repo.getResource(vfsURI.toString()));
            } catch (IOException e) {
                fail("Unexpected IOError on fetch of valid resource");
                e.printStackTrace();
            }
        }
    }

    /**
     * Validate that we get a non null Resource instance when passed a well-formed VfsURI pointing
     * to a non-existent file.
     */
    public void testGetResourceValidNoExist() throws Exception {
        String testResource = VfsTestHelper.SCRATCH_DIR + "/nosuchfile.jar";

        Iterator vfsURIs = helper.createVFSUriSet(testResource).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();

            // make sure the declared resource does not exist
            if (scratchDir.exists()) {
                FileUtil.forceDelete(scratchDir);
            }
            try {
                assertNotNull(repo.getResource(vfsURI.toString()));
            } catch (IOException e) {
                // this should not happen
                fail("Unexpected IOException");
            }
        }
    }

}
