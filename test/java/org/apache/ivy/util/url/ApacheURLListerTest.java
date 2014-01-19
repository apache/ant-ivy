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
package org.apache.ivy.util.url;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests {@link ApacheURLLister}.
 */
public class ApacheURLListerTest extends TestCase {

    /**
     * Tests {@link ApacheURLLister#retrieveListing(URL, boolean, boolean)}.
     * 
     * @throws Exception
     */
    public void testRetrieveListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(
            ApacheURLListerTest.class.getResource("apache-file-listing.html"), true, false);
        assertNotNull(files);
        assertTrue(files.size() > 0);
        for (Iterator iter = files.iterator(); iter.hasNext();) {
            URL file = (URL) iter.next();
            assertTrue("found a non matching file: " + file,
                file.getPath().matches(".*/[^/]+\\.(jar|md5|sha1)"));
        }

        // try a directory listing
        List dirs = lister.retrieveListing(
            ApacheURLListerTest.class.getResource("apache-dir-listing.html"), false, true);
        assertNotNull(dirs);
        assertEquals(4, dirs.size());

        List empty = lister.retrieveListing(
            ApacheURLListerTest.class.getResource("apache-dir-listing.html"), true, false);
        assertTrue(empty.isEmpty());
    }

    /**
     * Tests {@link ApacheURLLister#retrieveListing(URL, boolean, boolean)}.
     * 
     * @throws Exception
     */
    public void testRetrieveListingWithSpaces() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(
            ApacheURLListerTest.class.getResource("listing-with-spaces.html"), true, false);
        assertNotNull(files);
        assertTrue(files.size() > 0);
    }

    public void testRetrieveArtifactoryListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(
            ApacheURLListerTest.class.getResource("artifactory-dir-listing.html"), true, true);
        assertNotNull(files);
        assertEquals(1, files.size());
    }

    public void testRetrieveArchivaListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List d = lister.listDirectories(ApacheURLListerTest.class
                .getResource("archiva-listing.html"));
        assertNotNull(d);
        // archiva listing is not valid html at all currently (1.0, unclosed a tags),
        // and we don't want to adapt to this
        // assertEquals(3, d.size());
    }

    public void testRetrieveFixedArchivaListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List d = lister.listDirectories(ApacheURLListerTest.class
                .getResource("fixed-archiva-listing.html"));
        assertNotNull(d);
        assertEquals(3, d.size());
    }

    public void testRetrieveMavenProxyListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List d = lister.listDirectories(ApacheURLListerTest.class
                .getResource("maven-proxy-listing.html"));
        assertNotNull(d);
        assertEquals(3, d.size());
    }
}
