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

package org.apache.ivy.core.module.descriptor;

import junit.framework.TestCase;

public class DefaultDependencyDescriptorTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultDependencyDescriptorTest.class);
    }

    /*
     * Test method for
     * 'org.apache.ivy.DefaultDependencyDescriptor.replaceSelfFallbackPattern(String, String)'
     */
    public void testReplaceSelfFallbackPattern() {
        String replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("@(default)",
            "compile");
        assertEquals("compile(default)", replacedConf);

        replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("default", "compile");
        assertNull(replacedConf);

        replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("@", "runtime");
        assertEquals("runtime", "runtime");

    }

    /*
     * Test method for
     * 'org.apache.ivy.DefaultDependencyDescriptor.replaceThisFallbackPattern(String, String)'
     */
    public void testReplaceThisFallbackPattern() {
        String replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("#(default)",
            "compile");
        assertEquals("compile(default)", replacedConf);

        replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("default", "compile");
        assertNull(replacedConf);

        replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("#", "runtime");
        assertEquals("runtime", "runtime");

    }

}
