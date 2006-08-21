/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

/**
 * Test BasicURLHandler
 */
public class BasicURLHandlerTest extends TestCase {
	// remote.test
    public void testIsReachable() throws Exception {
        URLHandler handler = new BasicURLHandler();
        assertTrue(handler.isReachable(new URL("http://www.google.fr/")));
        assertFalse(handler.isReachable(new URL("http://www.google.fr/unknownpage.html")));
        
        assertTrue(handler.isReachable(new File("build.xml").toURL()));
        assertFalse(handler.isReachable(new File("unknownfile.xml").toURL()));
  
        // to test ftp we should know of an anonymous ftp site... !
//        assertTrue(handler.isReachable(new URL("ftp://ftp.mozilla.org/pub/dir.sizes")));
        assertFalse(handler.isReachable(new URL("ftp://ftp.mozilla.org/unknown.file")));
    }
}
