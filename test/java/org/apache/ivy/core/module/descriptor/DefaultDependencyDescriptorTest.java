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

package org.apache.ivy.core.module.descriptor;

import org.junit.Test;

import static org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor.replaceSelfFallbackPattern;
import static org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor.replaceThisFallbackPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultDependencyDescriptorTest {

    /*
     * Test method for
     * 'org.apache.ivy.DefaultDependencyDescriptor.replaceSelfFallbackPattern(String, String)'
     */
    @Test
    public void testReplaceSelfFallbackPattern() {
        assertEquals("compile(default)", replaceSelfFallbackPattern("@(default)", "compile"));

        assertNull(replaceSelfFallbackPattern("default", "compile"));

        assertEquals("runtime", replaceSelfFallbackPattern("@", "runtime"));

    }

    /*
     * Test method for
     * 'org.apache.ivy.DefaultDependencyDescriptor.replaceThisFallbackPattern(String, String)'
     */
    @Test
    public void testReplaceThisFallbackPattern() {
        assertEquals("compile(default)", replaceThisFallbackPattern("#(default)", "compile"));

        assertNull(replaceThisFallbackPattern("default", "compile"));

        assertEquals("runtime", replaceThisFallbackPattern("#", "runtime"));

    }

}
