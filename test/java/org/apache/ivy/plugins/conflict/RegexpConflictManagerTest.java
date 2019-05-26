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
package org.apache.ivy.plugins.conflict;

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RegexpConflictManagerTest {
    private Ivy ivy;

    private File cache;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(RegexpConflictManagerTest.class.getResource("ivysettings-regexp-test.xml"));
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtil.forceDelete(cache);
    }

    @Test
    public void testNoApiConflictResolve() throws Exception {
        ivy.resolve(RegexpConflictManagerTest.class.getResource("ivy-no-regexp-conflict.xml"),
                getResolveOptions());
    }

    @Test
    public void testConflictResolve() throws Exception {
        expExc.expect(StrictConflictException.class);
        expExc.expectMessage("org1#mod1.2;2.0.0:2.0 (needed by [apache#resolve-noconflict;1.0]) conflicts with org1#mod1.2;2.1.0:2.1 (needed by [apache#resolve-noconflict;1.0])");

        ivy.resolve(RegexpConflictManagerTest.class.getResource("ivy-conflict.xml"),
                getResolveOptions());
    }

    private ResolveOptions getResolveOptions() {
        return new ResolveOptions().setValidate(false);
    }
}
