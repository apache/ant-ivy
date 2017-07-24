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

import java.io.File;
import java.net.URL;

import org.apache.ivy.core.settings.NamedTimeoutConstraint;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.url.URLHandler.URLInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        assertTrue("URL resource was expected to be reachable", handler.isReachable(new URL("http://www.google.fr/"), defaultTimeoutConstraint));
        assertFalse("URL resource was expected to be unreachable", handler.isReachable(new URL("http://www.google.fr/unknownpage.html"), defaultTimeoutConstraint));
    }

    @Test
    public void testGetURLInfo() throws Exception {
        // IVY-390
        URLHandler handler = new HttpClientHandler();
        URLInfo info = handler
                .getURLInfo(new URL(
                        "https://repo1.maven.org/maven2/commons-lang/commons-lang/[1.0,3.0[/commons-lang-[1.0,3.0[.pom"), defaultTimeoutConstraint);

        assertEquals(URLHandler.UNAVAILABLE, info);
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

    private void assertDownloadOK(final URL url, final File file) throws Exception {
        handler.download(url, file, null, defaultTimeoutConstraint);
        assertTrue("Content from " + url + " wasn't downloaded to " + file, file.exists());
        assertTrue("Unexpected content at " + file + " for resource that was downloaded from " + url, file.isFile() && file.length() > 0);
    }
}
