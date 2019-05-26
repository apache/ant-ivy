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

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.settings.NamedTimeoutConstraint;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.url.URLHandler.URLInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;

import static org.apache.ivy.plugins.resolver.IBiblioResolver.DEFAULT_M2_ROOT;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link HttpClientHandler}
 */
public class HttpclientURLHandlerTest {
    // remote.test
    private File testDir;

    private HttpClientHandler handler;

    private final TimeoutConstraint defaultTimeoutConstraint;

    {
        defaultTimeoutConstraint = new NamedTimeoutConstraint("default-http-client-handler-timeout");
        ((NamedTimeoutConstraint) defaultTimeoutConstraint).setConnectionTimeout(5000);
    }

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        testDir = new File("build/HttpclientURLHandlerTest");
        testDir.mkdirs();

        handler = new HttpClientHandler();
    }

    @After
    public void tearDown() {
        try {
            handler.close();
        } catch (Exception e) {
            // ignore
        }
        FileUtil.forceDelete(testDir);
    }

    @Test
    public void testIsReachable() throws Exception {
        assertTrue("URL resource was expected to be reachable",
                handler.isReachable(new URL("http://www.google.fr/"), defaultTimeoutConstraint));
        assertFalse("URL resource was expected to be unreachable",
                handler.isReachable(new URL("http://www.google.fr/unknownpage.html"), defaultTimeoutConstraint));
    }

    /**
     * Test case for IVY-390.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-390">IVY-390</a>
     */
    @SuppressWarnings({"resource", "deprecation"})
    @Test
    public void testGetURLInfo() throws Exception {
        final TimeoutConstrainedURLHandler handler = new HttpClientHandler();
        assertTrue("Default Maven URL must end with '/'", DEFAULT_M2_ROOT.endsWith("/"));
        URLInfo info = handler.getURLInfo(new URL(DEFAULT_M2_ROOT
                + "commons-lang/commons-lang/[1.0,3.0[/commons-lang-[1.0,3.0[.pom"), defaultTimeoutConstraint);

        assertEquals(TimeoutConstrainedURLHandler.UNAVAILABLE, info);
    }

    @Test
    public void testContentEncoding() throws Exception {
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/daniels.html"), new File(
                testDir, "gzip.txt"));
        assertDownloadOK(new URL(
                "http://carsten.codimi.de/gzip.yaws/daniels.html?deflate=on&zlib=on"), new File(
                testDir, "deflate-zlib.txt"));
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/daniels.html?deflate=on"),
                new File(testDir, "deflate.txt"));
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/a5.ps"), new File(testDir,
                "a5-gzip.ps"));
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/a5.ps?deflate=on"), new File(
                testDir, "a5-deflate.ps"));
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/nh80.pdf"), new File(testDir,
                "nh80-gzip.pdf"));
        assertDownloadOK(new URL("http://carsten.codimi.de/gzip.yaws/nh80.pdf?deflate=on"),
                new File(testDir, "nh80-deflate.pdf"));
    }

    /**
     * Tests that the {@link HttpClientHandler}, backed by
     * {@link CredentialsStore Ivy credentials store} works as expected when it interacts
     * with a HTTP server which requires authentication for accessing resources.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1336">IVY-1336</a>
     */
    @Test
    public void testCredentials() throws Exception {
        // we catch it and check for presence of 401 in the exception message.
        // It's not exactly an contract that the IOException will have the 401 message
        // but for now that's how it's implemented and it's fine to check for the presence
        // of that message at the moment
        expExc.expect(IOException.class);
        expExc.expectMessage(endsWith("ivysettings.xml' 401 - 'Unauthorized"));

        final CredentialsStore credentialsStore = CredentialsStore.INSTANCE;
        final String realm = "test-http-client-handler-realm";
        final String host = "localhost";
        final Random random = new Random();
        final String userName = "test-http-user-" + random.nextInt();
        final String password = "pass-" + random.nextInt();
        credentialsStore.addCredentials(realm, host, userName, password);
        final InetSocketAddress serverBindAddr = new InetSocketAddress("localhost", TestHelper.getMaybeAvailablePort());
        final String contextRoot = "/testHttpClientHandler";
        final Path repoRoot = new File("test/repositories").toPath();
        assertTrue(repoRoot + " is not a directory", Files.isDirectory(repoRoot));
        // create a server backed by BASIC auth with the set of "allowed" credentials
        try (final AutoCloseable server = TestHelper.createBasicAuthHttpServerBackedRepo(serverBindAddr,
                contextRoot, repoRoot, realm, Collections.singletonMap(userName, password))) {

            final File target = new File(testDir, "downloaded.xml");
            assertFalse("File " + target + " already exists", target.exists());
            final URL src = new URL("http://localhost:" + serverBindAddr.getPort() + "/"
                    + contextRoot + "/ivysettings.xml");
            // download it
            handler.download(src, target, null, defaultTimeoutConstraint);
            assertTrue("File " + target + " was not downloaded from " + src, target.isFile());
        }
        // now create a server backed by BASIC auth with a set of credentials that do *not* match
        // with what the Ivy credentials store will return for a given realm+host combination, i.e.
        // Ivy credential store will return back invalid credentials and the server will reject them
        try (final AutoCloseable server = TestHelper.createBasicAuthHttpServerBackedRepo(serverBindAddr,
                contextRoot, repoRoot, realm, Collections.singletonMap("other-" + userName, "other-" + password))) {

            final File target = new File(testDir, "should-not-have-been-downloaded.xml");
            assertFalse("File " + target + " already exists", target.exists());
            final URL src = new URL("http://localhost:" + serverBindAddr.getPort() + "/"
                    + contextRoot + "/ivysettings.xml");
            // download it (expected to fail)
            handler.download(src, target, null, defaultTimeoutConstraint);
        }
    }

    private void assertDownloadOK(final URL url, final File file) throws Exception {
        handler.download(url, file, null, defaultTimeoutConstraint);
        assertTrue("Content from " + url + " wasn't downloaded to " + file, file.exists());
        assertTrue("Unexpected content at " + file + " for resource that was downloaded from "
                + url, file.isFile() && file.length() > 0);
    }
}
