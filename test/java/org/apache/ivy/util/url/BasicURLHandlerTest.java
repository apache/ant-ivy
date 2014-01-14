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

import org.apache.ivy.util.FileUtil;

import junit.framework.TestCase;

/**
 * Test BasicURLHandler
 */
public class BasicURLHandlerTest extends TestCase {
    // remote.test
    private File testDir;

    private BasicURLHandler handler;

    protected void setUp() throws Exception {
        testDir = new File("build/BasicURLHandlerTest");
        testDir.mkdirs();

        handler = new BasicURLHandler();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(testDir);
    }

    public void testIsReachable() throws Exception {
        assertTrue(handler.isReachable(new URL("http://www.google.fr/")));
        assertFalse(handler.isReachable(new URL("http://www.google.fr/unknownpage.html")));

        assertTrue(handler.isReachable(new File("build.xml").toURI().toURL()));
        assertFalse(handler.isReachable(new File("unknownfile.xml").toURI().toURL()));

        // to test ftp we should know of an anonymous ftp site... !
        // assertTrue(handler.isReachable(new URL("ftp://ftp.mozilla.org/pub/dir.sizes")));
        assertFalse(handler.isReachable(new URL("ftp://ftp.mozilla.org/unknown.file")));
    }

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

    private void assertDownloadOK(URL url, File file) throws Exception {
        handler.download(url, file, null);
        assertTrue(file.exists());
    }
}
