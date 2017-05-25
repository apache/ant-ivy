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
package org.apache.ivy.plugins.conflict;

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RegexpConflictManagerTest {
    private Ivy ivy;

    private File _cache;

    @Before
    public void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(RegexpConflictManagerTest.class.getResource("ivysettings-regexp-test.xml"));
        _cache = new File("build/cache");
        _cache.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtil.forceDelete(_cache);
    }

    @Test
    public void testNoApiConflictResolve() throws Exception {
        try {
            ivy.resolve(RegexpConflictManagerTest.class.getResource("ivy-no-regexp-conflict.xml"),
                getResolveOptions());
        } catch (StrictConflictException e) {
            fail("Unexpected conflict: " + e);
        }
    }

    @Test
    public void testConflictResolve() throws Exception {
        try {
            ivy.resolve(RegexpConflictManagerTest.class.getResource("ivy-conflict.xml"),
                getResolveOptions());

            fail("Resolve should have failed with a conflict");
        } catch (StrictConflictException e) {
            // this is expected
            assertTrue(
                "bad exception message: " + e.getMessage(),
                    e.getMessage().contains("org1#mod1.2;2.0.0:2.0 (needed by [apache#resolve-noconflict;1.0])"));
            assertTrue("bad exception message: " + e.getMessage(),
                    e.getMessage().contains("conflicts with"));
            assertTrue(
                "bad exception message: " + e.getMessage(),
                    e.getMessage().contains("org1#mod1.2;2.1.0:2.1 (needed by [apache#resolve-noconflict;1.0])"));
        }
    }

    private ResolveOptions getResolveOptions() {
        return new ResolveOptions().setValidate(false);
    }
}
