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
package org.apache.ivy.osgi.util;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testParsing() throws Exception {
        Version v;

        v = new Version("1");
        assertEquals("1.0.0", v.toString());
        assertEquals("", v.qualifier());

        v = new Version("1.2");
        assertEquals("1.2.0", v.toString());
        assertEquals("", v.qualifier());

        v = new Version("1.2.3");
        assertEquals("1.2.3", v.toString());
        assertEquals("", v.qualifier());

        v = new Version("1.2.3.abc");
        assertEquals("1.2.3.abc", v.toString());
        assertEquals("abc", v.qualifier());
    }

    public void testCompareTo() throws Exception {
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.3")) == 0);

        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.2")) > 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("1.1.3")) > 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("0.2.3")) > 0);
        assertTrue(new Version("1.2.3.xyz").compareTo(new Version("1.2.3")) > 0);
        assertTrue(new Version("1.2.3.xyz").compareTo(new Version("1.2.3.abc")) > 0);

        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.4")) < 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("1.3.3")) < 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("2.2.3")) < 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.3.xyz")) < 0);
        assertTrue(new Version("1.2.3.abc").compareTo(new Version("1.2.3.xyz")) < 0);
    }

}
