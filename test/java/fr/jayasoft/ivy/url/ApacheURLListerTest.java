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
    //~ Methods ----------------------------------------------------------------

    /**
     * Tests {@link ApacheURLLister#retrieveListing(URL, boolean, boolean)}.
     *
     * @throws Exception
     */
    public void testRetrieveListing() throws Exception {
        URL url = new URL("http://www.ibiblio.org/maven/ant/jars/");

        ApacheURLLister lister = new ApacheURLLister();

        List files = lister.retrieveListing(url, true, false);
        assertNotNull(files);
        assertTrue(files.size() > 0);

        Iterator iter = files.iterator();

        while (iter.hasNext()) {
            URL file = (URL) iter.next();
            assertNotNull(file);
            assertTrue("found non matching file: "+file.getPath(), file.getPath().matches(".*/ant/jars/[^/]*$"));
        }

        // try a directory listing
        url = new URL("http://www.ibiblio.org/maven/ant/");

        List dirs = lister.retrieveListing(url, false, true);
        assertNotNull(dirs);
        assertTrue(dirs.size() > 0);

        iter = dirs.iterator();

        while (iter.hasNext()) {
            URL dir = (URL) iter.next();
            assertNotNull(dir);
            assertTrue("found non matching dir: "+dir.getPath(), dir.getPath().matches(".*/$"));
        }

        List empty = lister.retrieveListing(url, true, false);
        assertTrue(empty.isEmpty());
    }
    
//    public void testRetrieveTomcatListing() throws Exception {
//        URL url = new URL("http://localhost:8080/ivyrep/apache/commons-collections/");
//
//        ApacheURLLister lister = new ApacheURLLister();
//
//        List files = lister.retrieveListing(url, true, false);
//        assertNotNull(files);
//        assertTrue(files.size() > 0);
//    }
}
