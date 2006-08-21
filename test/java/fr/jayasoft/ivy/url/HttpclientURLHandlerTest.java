/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import java.net.URL;

import junit.framework.TestCase;

/**
 * Test HttpClientHandler
 */
public class HttpclientURLHandlerTest extends TestCase {
	// remote.test
    public void testIsReachable() throws Exception {
        URLHandler handler = new HttpClientHandler();
        assertTrue(handler.isReachable(new URL("http://www.google.fr/")));
        assertFalse(handler.isReachable(new URL("http://www.google.fr/unknownpage.html")));
    }
}
