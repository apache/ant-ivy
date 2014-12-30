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
package org.apache.ivy.core.module.id;

import junit.framework.TestCase;

public class ModuleIdTest extends TestCase {

    public void testModuleId() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);

        assertEquals(org, moduleId.getOrganisation());
        assertEquals(name, moduleId.getName());
    }

    public void testModuleIdIllegalArgumentException() {
        String org = "apache";
        String name = "some-new-module";

        try {
            new ModuleId(null, name);
        } catch (IllegalArgumentException iae) {
            fail("A null should be allowed for argument 'org'.");
        }

        try {
            new ModuleId(org, null);
            fail("A IllegalArgumentException should have been thrown for the argument 'name'.");
        } catch (IllegalArgumentException iae) {
            // success
        }
    }

    public void testEqualsObjectTrue() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);
        ModuleId moduleId2 = new ModuleId(org, name);

        assertTrue(moduleId.equals(moduleId));
        assertTrue(moduleId.equals(moduleId2));
        assertTrue(moduleId2.equals(moduleId));
    }

    public void testEqualsObjectFalse() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);
        ModuleId moduleId2 = new ModuleId(null, name);

        assertFalse(moduleId.equals(null));
        assertFalse(moduleId.equals(moduleId2));
        assertFalse(moduleId2.equals(moduleId));
    }

    public void testEncodeToString() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);

        assertEquals(org + ModuleId.ENCODE_SEPARATOR + name, moduleId.encodeToString());
    }

    public void testDecode() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);

        ModuleId moduleId2 = ModuleId.decode(moduleId.encodeToString());
        assertEquals(moduleId, moduleId2);
    }

    public void testCompareToNullObject() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);

        try {
            moduleId.compareTo(null);
            fail("A NullPointerException was expected.");
        } catch (NullPointerException npe) {
            // success
        }
    }

    public void testCompareToEqual() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);

        assertTrue(moduleId.compareTo(new ModuleId(org, name)) == 0);
    }

    public void testCompareToLessThan() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);
        String name2 = "the-new-module";
        ModuleId moduleId2 = new ModuleId(org, name2);
        System.out.println(moduleId + "\n" + moduleId2);

        assertTrue(moduleId.compareTo(moduleId2) < 0);
    }

    public void testCompareToGreatherThan() {
        String org = "apache";
        String name = "some-new-module";
        ModuleId moduleId = new ModuleId(org, name);
        String name2 = "the-new-module";
        ModuleId moduleId2 = new ModuleId(org, name2);
        System.out.println(moduleId + "\n" + moduleId2);

        assertTrue(moduleId2.compareTo(moduleId) > 0);
    }
}
