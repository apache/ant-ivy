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
package org.apache.ivy.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {

    @Test
    public void testGetStackTrace() {
        String trace = StringUtils.getStackTrace(new Exception());
        assertTrue(trace.contains("java.lang.Exception"));
        assertTrue(trace.contains("at org.apache.ivy.util.StringUtilsTest.testGetStackTrace(StringUtilsTest.java"));
    }

    @Test
    public void testEncryption() {
        assertEquals("apache", StringUtils.decrypt(StringUtils.encrypt("apache")));
        assertEquals("yet another string with 126 digits and others :;%_-$& characters",
            StringUtils.decrypt(StringUtils
                    .encrypt("yet another string with 126 digits and others :;%_-$& characters")));

        assertNotEquals("apache", StringUtils.encrypt("apache"));
    }
}
