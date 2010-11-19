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

public class VersionRangeTest extends TestCase {

    public void testParse() throws Exception {
        assertEquals(new VersionRange(false, new Version("1.0.0"), false, null), new VersionRange(
                "1.0.0"));
        assertEquals(new VersionRange(false, new Version("1.0.0"), false, null), new VersionRange(
                " 1.0.0 "));

        assertEquals(new VersionRange(false, new Version("1.0.0"), false, new Version("2.0.0")),
            new VersionRange("[1.0.0,2.0.0]"));
        assertEquals(new VersionRange(false, new Version("1.0.0"), false, new Version("2.0.0")),
            new VersionRange("[1.0.0 , 2.0.0]"));
        assertEquals(new VersionRange(false, new Version("1.0.0"), false, new Version("2.0.0")),
            new VersionRange(" [1.0.0,2.0.0] "));
        assertEquals(new VersionRange(false, new Version("1.0.0"), false, new Version("2.0.0")),
            new VersionRange("[ 1.0.0,2.0.0 ]"));

        assertEquals(new VersionRange(false, new Version("1.0.0.A"), false, null),
            new VersionRange("1.0.0.A"));
    }

    public void testContains() throws Exception {
        assertFalse(new VersionRange("1").contains("0.9"));
        assertTrue(new VersionRange("1").contains("1"));
        assertTrue(new VersionRange("1").contains("1.0"));
        assertTrue(new VersionRange("1").contains("1.0.0"));
        assertTrue(new VersionRange("1").contains("2"));
        assertTrue(new VersionRange("1").contains("1.0.1"));

        assertFalse(new VersionRange("1.2.3").contains("1.0.0"));
        assertTrue(new VersionRange("1.2.3").contains("1.2.3"));
        assertTrue(new VersionRange("1.2.3").contains("1.2.4"));
        assertTrue(new VersionRange("1.2.3").contains("1.2.3.A"));

        assertFalse(new VersionRange("[1.0.0,2.0.0]").contains("0.9.0"));
        assertTrue(new VersionRange("[1.0.0,2.0.0]").contains("1.0.0"));
        assertTrue(new VersionRange("[1.0.0,2.0.0]").contains("1.999.999"));
        assertTrue(new VersionRange("[1.0.0,2.0.0]").contains("2.0.0"));
        assertFalse(new VersionRange("[1.0.0,2.0.0]").contains("2.1.0"));
        assertFalse(new VersionRange("[1.0.0,2.0.0]").contains("2.0.1"));

        assertFalse(new VersionRange("[1.0.0,2]").contains("0.9.0"));
        assertTrue(new VersionRange("[1.0.0,2]").contains("1.0.0"));
        assertTrue(new VersionRange("[1.0.0,2]").contains("1.999.999"));
        assertTrue(new VersionRange("[1.0.0,2]").contains("2.0.0"));
        assertFalse(new VersionRange("[1.0.0,2]").contains("2.1.0"));
        assertFalse(new VersionRange("[1.0.0,2]").contains("2.0.1"));

        assertFalse(new VersionRange("(1.0.0,2.0.0)").contains("0.9.0"));
        assertFalse(new VersionRange("(1.0.0,2.0.0)").contains("1.0.0"));
        assertTrue(new VersionRange("(1.0.0,2.0.0)").contains("1.999.999"));
        assertFalse(new VersionRange("(1.0.0,2.0.0)").contains("2.0.0"));
        assertFalse(new VersionRange("(1.0.0,2.0.0)").contains("2.1.0"));
        assertFalse(new VersionRange("(1.0.0,2.0.0)").contains("2.0.1"));

        assertFalse(new VersionRange("(1.0.0,)").contains("0.9.0"));
        assertFalse(new VersionRange("(1.0.0,)").contains("1.0.0"));
        assertTrue(new VersionRange("(1.0.0,)").contains("1.999.999"));
        assertTrue(new VersionRange("(1.0.0,)").contains("2.0.0"));
        assertTrue(new VersionRange("(1.0.0,)").contains("2.1.0"));
        assertTrue(new VersionRange("(1.0.0,)").contains("2.0.1"));

        assertFalse(new VersionRange("(1.0.0,]").contains("0.9.0"));
        assertFalse(new VersionRange("(1.0.0,]").contains("1.0.0"));
        assertTrue(new VersionRange("(1.0.0,]").contains("1.999.999"));
        assertTrue(new VersionRange("(1.0.0,]").contains("2.0.0"));
        assertTrue(new VersionRange("(1.0.0,]").contains("2.1.0"));
        assertTrue(new VersionRange("(1.0.0,]").contains("2.0.1"));
    }
}
