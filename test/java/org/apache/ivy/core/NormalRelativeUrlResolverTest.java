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
package org.apache.ivy.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

public class NormalRelativeUrlResolverTest extends TestCase {

    private NormalRelativeUrlResolver t = new NormalRelativeUrlResolver();

    public void testRelativeHttpURL() throws MalformedURLException {
        URL base = new URL("http://xxx/file.txt");
        assertEquals(new URL("http://xxx/file2.txt"), t.getURL(base, "file2.txt"));
    }

    public void testRelativeFileURL() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        assertEquals(new URL("file://xxx/file2.txt"), t.getURL(base, "file2.txt"));
    }

    public void testRelativeMixedURL() throws MalformedURLException {
        URL base = new URL("http://xxx/file.txt");
        assertEquals(new URL("file://file2.txt"), t.getURL(base, "file://file2.txt"));
    }

    public void testFileAndUrlWithAbsoluteFile() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        File absFile = new File(".").getAbsoluteFile();
        assertEquals(absFile.toURI().toURL(), t.getURL(base, absFile.toString(), null));
        assertEquals(absFile.toURI().toURL(), t.getURL(base, absFile.toString(), ""));
        assertEquals(absFile.toURI().toURL(), t.getURL(base, absFile.toString(), "somthing.txt"));
    }

    public void testFileAndUrlWithRelativeFile() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        assertEquals(new URL("file://xxx/file2.txt"), t.getURL(base, "file2.txt", null));
        assertEquals(new URL("file://xxx/file2.txt"), t.getURL(base, "file2.txt", ""));
        assertEquals(new URL("file://xxx/sub/file2.txt"),
            t.getURL(base, "sub/file2.txt", "something"));
    }

    public void testFileAndUrlWithAbsoluteUrl() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        URL otherBase = new URL("http://localhost:80/otherfile.txt");
        String absUrl = "http://ibiblio.org/dir/file.txt";
        assertEquals(new URL(absUrl), t.getURL(base, null, absUrl));
        assertEquals(new URL(absUrl), t.getURL(otherBase, null, absUrl));
    }

    public void testFileAndUrlWithRelativeUrl() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        URL otherBase = new URL("http://localhost:80/otherfile.txt");
        assertEquals(new URL("file://xxx/file2.txt"), t.getURL(base, null, "file2.txt"));
        assertEquals(new URL("http://localhost:80/file2.txt"),
            t.getURL(otherBase, null, "file2.txt"));
    }

}
