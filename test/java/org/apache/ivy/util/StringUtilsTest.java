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
package org.apache.ivy.util;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public void testGetStackTrace() throws Exception {
        String trace = StringUtils.getStackTrace(new Exception());
        assertTrue(trace.indexOf("java.lang.Exception") != -1);
        assertTrue(trace
                .indexOf("at org.apache.ivy.util.StringUtilsTest.testGetStackTrace(StringUtilsTest.java") != -1);
    }

    public void testEncryption() {
        assertEquals("apache", StringUtils.decrypt(StringUtils.encrypt("apache")));
        assertEquals("yet another string with 126 digits and others :;%_-$& characters",
            StringUtils.decrypt(StringUtils
                    .encrypt("yet another string with 126 digits and others :;%_-$& characters")));

        assertFalse("apache".equals(StringUtils.encrypt("apache")));
    }
}
