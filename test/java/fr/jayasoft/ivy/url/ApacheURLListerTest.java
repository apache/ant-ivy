/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import junit.framework.TestCase;

import java.net.URL;
import java.util.Iterator;
import java.util.List;


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
