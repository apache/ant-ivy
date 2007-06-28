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

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

public class NormalRelativeUrlResolverTest extends TestCase {

    
    public void testRelativeHttpURL() throws MalformedURLException {
        URL base = new URL("http://xxx/file.txt");
        NormalRelativeUrlResolver t = new NormalRelativeUrlResolver();
        assertEquals(new URL("http://xxx/file2.txt"), t.getURL(base , "file2.txt"));
    }
    
    public void testRelativeFileURL() throws MalformedURLException {
        URL base = new URL("file://xxx/file.txt");
        NormalRelativeUrlResolver t = new NormalRelativeUrlResolver();
        assertEquals(new URL("file://xxx/file2.txt"), t.getURL(base , "file2.txt"));
    }

    public void testRelativeMixedURL() throws MalformedURLException {
        URL base = new URL("http://xxx/file.txt");
        NormalRelativeUrlResolver t = new NormalRelativeUrlResolver();
        assertEquals(new URL("file://file2.txt"), t.getURL(base , "file://file2.txt"));
    }

}
