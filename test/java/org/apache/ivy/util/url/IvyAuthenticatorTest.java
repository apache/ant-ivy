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

package org.apache.ivy.util.url;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link IvyAuthenticator}
 */
public class IvyAuthenticatorTest {

    private Authenticator previousAuthenticator;

    private TestAuthenticator testAuthenticator;

    @Before
    public void before() {
        previousAuthenticator = IvyAuthenticator.getCurrentAuthenticator();
        this.setupTestAuthenticator();
    }

    private void setupTestAuthenticator() {
        this.testAuthenticator = new TestAuthenticator();
        // first setup our TestAuthenticator
        Authenticator.setDefault(this.testAuthenticator);
        // now install IvyAuthenticator on top of it
        IvyAuthenticator.install();
    }

    @After
    public void after() {
        // reset to the authenticator that was around before the test was run
        Authenticator.setDefault(previousAuthenticator);
    }

    /**
     * Tests that when {@link IvyAuthenticator} can't handle a authentication request and falls back
     * on an authenticator that was previously set, before IvyAuthenticator installed on top of it,
     * the other authenticator gets passed all the relevant requesting information, including the
     * {@link Authenticator#getRequestingURL() requesting URL} and
     * {@link Authenticator#getRequestorType() request type}
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1557">IVY-1557</a>
     */
    @Test
    public void testRequestURLAndType() throws Exception {
        testAuthenticator.expectedHost = "localhost";
        testAuthenticator.expectedPort = 12345;
        testAuthenticator.expectedPrompt = "Test prompt - testRequestURLAndType";
        testAuthenticator.expectedProtocol = "HTTP/1.1";
        testAuthenticator.expectedURL = new URL("http", "localhost", 12345, "/a/b/c");
        testAuthenticator.expectedType = Authenticator.RequestorType.PROXY;
        testAuthenticator.expectedScheme = "BASIC";
        testAuthenticator.expectedSite = InetAddress.getLoopbackAddress();

        // trigger the authentication
        final PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(testAuthenticator.expectedHost, testAuthenticator.expectedSite,
                testAuthenticator.expectedPort, testAuthenticator.expectedProtocol, testAuthenticator.expectedPrompt,
                testAuthenticator.expectedScheme, testAuthenticator.expectedURL, testAuthenticator.expectedType);

        assertNotNull("Expected a password authentication, but got none", auth);
        assertEquals("Unexpected username", "dummy", auth.getUserName());
        assertTrue("Unexpected password", Arrays.equals("dummy".toCharArray(), auth.getPassword()));
    }

    private class TestAuthenticator extends Authenticator {

        private String expectedHost;
        private int expectedPort = -1;
        private String expectedPrompt;
        private String expectedProtocol;
        private String expectedScheme;
        private URL expectedURL;
        private RequestorType expectedType;
        private InetAddress expectedSite;

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            assertEquals("Unexpected requesting host", expectedHost, getRequestingHost());
            assertEquals("Unexpected requesting port", expectedPort, getRequestingPort());
            assertEquals("Unexpected prompt", expectedPrompt, getRequestingPrompt());
            assertEquals("Unexpected protocol", expectedProtocol, getRequestingProtocol());
            assertEquals("Unexpected scheme", expectedScheme, getRequestingScheme());
            assertEquals("Unexpected requesting URL", expectedURL, getRequestingURL());
            assertEquals("Unexpected requesting type", expectedType, getRequestorType());
            assertEquals("Unexpected requesting site", expectedSite, getRequestingSite());
            return new PasswordAuthentication("dummy", "dummy".toCharArray());
        }
    }
}
