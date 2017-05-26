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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VfsResourceTest {
    private VfsTestHelper helper = null;

    @Before
    public void setUp() throws Exception {
        helper = new VfsTestHelper();
    }

    @After
    public void tearDown() {
        helper = null;
    }

    /**
     * Validate VFSResource creation for a valid VFS URI pointing to an physically existing file
     */
    @SuppressWarnings("unused")
    @Test
    public void testCreateResourceThatExists() throws Exception {
        Iterator vfsURIs = helper.createVFSUriSet(VfsTestHelper.TEST_IVY_XML).iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();
            String resId = vfsURI.toString();
            VfsResource res = new VfsResource(resId, helper.fsManager);
            assertNotNull("Unexpected null value on VFS URI: " + resId, res);
            assertTrue("Resource does not exist and it should: " + resId, res.exists());

            // VFS apparently does some weird normalization so that resource id used to create
            // the VFS resource is not necessarily identical to the id returned from the getName
            // method <sigh>. We try to work around this by transforming things into java URIs.
            assertEquals("Failed on getName. Expected: " + resId + ". Actual: " + res.getName(),
                    new URI(escapeUrl(resId)), new URI(escapeUrl(res.getName())).normalize());
            assertNotEquals("Expected non-null file modification date for URI: " + resId,
                    0, res.getLastModified());
            assertNotEquals("Expected non-zero file length for URI: " + resId,
                    0, res.getContentLength());
            assertTrue("Physical existence check returned false for existing resource: " + resId,
                    res.physicallyExists());
        }
    }

    /**
     * Escape invalid URL characters (Copied from Wicket, just use StringUtils instead of Strings)
     *
     * @param queryString
     *            The original querystring
     * @return url The querystring with invalid characters escaped
     */
    private String escapeUrl(String queryString) {
        queryString = StringUtils.replace(queryString, " ", "%20");
        queryString = StringUtils.replace(queryString, "\"", "%22");
        queryString = StringUtils.replace(queryString, "%", "%26");
        queryString = StringUtils.replace(queryString, "=", "%3D");
        queryString = StringUtils.replace(queryString, "/", "%2F");
        queryString = StringUtils.replace(queryString, "+", "%2B");
        queryString = StringUtils.replace(queryString, "&", "%26");
        queryString = StringUtils.replace(queryString, "~", "%7E");
        queryString = StringUtils.replace(queryString, "?", "%3F");
        return queryString;
    }

    /**
     * Validating that resource can be created for files which don't physically exists - e.g.
     * resources that are going to created.
     */
    @SuppressWarnings("unused")
    @Test
    public void testCreateResourceThatDoesntExist() throws Exception {
        Iterator vfsURIs = helper.createVFSUriSet("zzyyxx.zzyyxx").iterator();
        while (vfsURIs.hasNext()) {
            VfsURI vfsURI = (VfsURI) vfsURIs.next();
            String resId = vfsURI.toString();
            VfsResource res = new VfsResource(resId, helper.fsManager);
            assertNotNull("Unexpected null value on VFS URI: " + resId, res);
            assertFalse("Resource does exist and it should not: " + resId, res.exists());

            // VFS apparently does some weird normalization so that resource id used to create
            // the VFS resource is not necessarily identical to the id returned from the getName
            // method <sigh>. We try to work around this by transforming things into java URIs.
            assertEquals("Failed on getName. Expected: " + resId + ". Actual: " + res.getName(),
                    new URI(escapeUrl(resId)), new URI(escapeUrl(res.getName())).normalize());
            assertEquals("Expected null file modification date for URI: " + resId,
                    0, res.getLastModified());
            assertEquals("Expected zero file length for URI: " + resId,
                    0, res.getContentLength());
            assertFalse("Physical existence check returned true for non-existent resource: " + resId,
                    res.physicallyExists());
       }
    }

    /**
     * Validate VFSResource creation when given a poorly formed VFS identifier
     */
    @SuppressWarnings("unused")
    @Test
    public void testBadURI() throws Exception {
        String vfsURI = "smb1:/goobeldygook";
        VfsResource res = new VfsResource(vfsURI, helper.fsManager);

        assertNotNull("Unexpected null value on VFS URI: " + vfsURI, res);
        assertFalse("Resource is marked as existing and it should not: " + vfsURI, res.exists());
        assertEquals("Failed on getName. Expected: " + vfsURI + ". Actual: " + res.getName(),
                res.getName(), "smb1:/goobeldygook");
        assertEquals("Expected null file modification date for URI: " + vfsURI + ": "
                        + res.getLastModified(), 0, res.getLastModified());
        assertEquals("Expected zero file length for URI: " + vfsURI,
                0, res.getContentLength());
        assertFalse("Physical existence check returned true for non-existent resource: " + vfsURI,
                res.physicallyExists());
    }

    /**
     * Validate getChildren when given a VFS URI for a directory
     */
    @Test
    public void testListFolderChildren() throws Exception {
        final String testFolder = "2/mod10.1";
        final List expectedFiles = Arrays.asList(new String[] {"ivy-1.0.xml", "ivy-1.1.xml",
                "ivy-1.2.xml", "ivy-1.3.xml"});

        Iterator baseVfsURIs = helper.createVFSUriSet(testFolder).iterator();
        while (baseVfsURIs.hasNext()) {
            VfsURI baseVfsURI = (VfsURI) baseVfsURIs.next();

            List expected = new ArrayList();
            for (int i = 0; i < expectedFiles.size(); i++) {
                String resId = baseVfsURI.toString() + "/" + expectedFiles.get(i);
                expected.add(resId);
            }

            List actual = new ArrayList();
            VfsResource res = new VfsResource(baseVfsURI.toString(), helper.fsManager);
            Iterator children = res.getChildren().iterator();
            while (children.hasNext()) {
                String resId = (String) children.next();
                // remove entries ending in .svn
                if (!resId.endsWith(".svn")) {
                    actual.add(resId);
                }
            }

            Collections.sort(actual);
            Collections.sort(expected);
            assertTrue("\nExpected: " + expected.toString() + "\nActual: " + actual.toString(),
                    actual.equals(expected));
        }
    }

    /**
     * Validate that we don't get any results when we query a VFSResource file object for its
     * children
     */
    @Test
    public void testListFileChildren() throws Exception {
        Iterator testSet = helper.createVFSUriSet(VfsTestHelper.TEST_IVY_XML).iterator();
        while (testSet.hasNext()) {
            VfsURI vfsURI = (VfsURI) testSet.next();
            VfsResource res = new VfsResource(vfsURI.toString(), helper.fsManager);
            List results = res.getChildren();
            assertEquals("getChildren query on file provided results when it shouldn't have",
                    0, results.size());
        }
    }

    /**
     * Validate that we don't get any results if we ask an IMAGINARY VFSResource - a nonexistent
     * file - for a list of its children
     */
    @Test
    public void testListImaginary() throws Exception {
        Iterator testSet = helper.createVFSUriSet("idontexistzzxx").iterator();
        while (testSet.hasNext()) {
            VfsURI vfsURI = (VfsURI) testSet.next();
            VfsResource res = new VfsResource(vfsURI.toString(), helper.fsManager);
            List results = res.getChildren();
            assertEquals("getChildren query on file provided results when it shouldn't have",
                    0, results.size());
        }
    }
}
