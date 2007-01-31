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
 *
 * @author Xavier Hanin
 * @author <a href="johnmshields@yahoo.com">John M. Shields</a>
 */
public class ApacheURLListerTest extends TestCase {

    /**
     * Tests {@link ApacheURLLister#retrieveListing(URL, boolean, boolean)}.
     *
     * @throws Exception
     */
    public void testRetrieveListing() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(ApacheURLListerTest.class.getResource("apache-file-listing.html"), true, false);
        assertNotNull(files);
        assertTrue(files.size() > 0);
        for (Iterator iter = files.iterator(); iter.hasNext();) {
			URL file = (URL) iter.next();
			assertTrue("found a non matching file: "+file, file.getPath().matches(".*/[^/]+\\.(jar|md5|sha1)"));
		}


        // try a directory listing
        List dirs = lister.retrieveListing(ApacheURLListerTest.class.getResource("apache-dir-listing.html"), false, true);
        assertNotNull(dirs);
        assertEquals(4, dirs.size());


        List empty = lister.retrieveListing(ApacheURLListerTest.class.getResource("apache-dir-listing.html"), true, false);
        assertTrue(empty.isEmpty());
    }
    
    /**
     * Tests {@link ApacheURLLister#retrieveListing(URL, boolean, boolean)}.
     *
     * @throws Exception
     */
    public void testRetrieveListingWithSpaces() throws Exception {
        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(ApacheURLListerTest.class.getResource("listing-with-spaces.html"), true, false);
        assertNotNull(files);
        assertTrue(files.size() > 0);
    }
}
