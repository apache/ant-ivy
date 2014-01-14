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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.ivy.util.CopyProgressListener;

import junit.framework.TestCase;

public class AbstractURLHandlerTest extends TestCase {

    /**
     * JUnit test for IVY-923.
     */
    public void testNormalizeToStringWithSpaceURL() throws Exception {
        AbstractURLHandler handler = new TestURLHandler();
        String normalizedUrl = handler.normalizeToString(new URL(
                "http://ant.apache.org/ivy/url with space/ivy-1.0.xml"));
        assertEquals("http://ant.apache.org/ivy/url%20with%20space/ivy-1.0.xml", normalizedUrl);
    }

    /**
     * JUnit test for IVY-923.
     */
    public void testNormalizeToStringWithPlusCharacter() throws Exception {
        AbstractURLHandler handler = new TestURLHandler();
        String normalizedUrl = handler.normalizeToString(new URL(
                "http://ant.apache.org/ivy/ivy-1.+.xml"));
        assertEquals("http://ant.apache.org/ivy/ivy-1.%2B.xml", normalizedUrl);
    }

    public void testNormalizeToStringWithUnderscoreInHostname() throws Exception {
        AbstractURLHandler handler = new TestURLHandler();
        String normalizedUrl = handler.normalizeToString(new URL(
                "http://peat_hal.users.sourceforge.net/m2repository"));
        assertEquals("http://peat_hal.users.sourceforge.net/m2repository", normalizedUrl);
    }

    public void testNormalizeToStringWithUnderscoreInHostnameAndSpaceInPath() throws Exception {
        AbstractURLHandler handler = new TestURLHandler();
        String normalizedUrl = handler.normalizeToString(new URL(
                "http://peat_hal.users.sourceforge.net/m2 repository"));
        assertEquals("http://peat_hal.users.sourceforge.net/m2%20repository", normalizedUrl);
    }

    private static class TestURLHandler extends AbstractURLHandler {

        public void download(URL src, File dest, CopyProgressListener l) throws IOException {
            throw new UnsupportedOperationException();
        }

        public URLInfo getURLInfo(URL url) {
            throw new UnsupportedOperationException();
        }

        public URLInfo getURLInfo(URL url, int timeout) {
            throw new UnsupportedOperationException();
        }

        public InputStream openStream(URL url) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void upload(File src, URL dest, CopyProgressListener l) throws IOException {
            throw new UnsupportedOperationException();
        }

    }
}
