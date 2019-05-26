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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StrictConflictManagerTest {
    private Ivy ivy;

    private File cache;

    @Before
    public void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(StrictConflictManagerTest.class.getResource("ivysettings-strict-test.xml"));
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtil.forceDelete(cache);
    }

    @Test
    public void testInitFromConf() {
        ConflictManager cm = ivy.getSettings().getDefaultConflictManager();
        assertTrue(cm instanceof StrictConflictManager);
    }

    @Test
    public void testNoConflictResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-noconflict.xml"),
            getResolveOptions());
    }

    @Test
    public void testNoConflictWithDynamicRevisionResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-noconflict-dynamic.xml"),
            getResolveOptions());
    }

    /**
     * Resolve must fail with a conflict.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = StrictConflictException.class)
    public void testConflictResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-conflict.xml"),
                getResolveOptions());
    }

    /**
     * Resolve must fail with a conflict.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = StrictConflictException.class)
    public void testConflictWithDynamicRevisionResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-conflict-dynamic.xml"),
                getResolveOptions());
    }

    private ResolveOptions getResolveOptions() {
        return new ResolveOptions().setValidate(false);
    }
}
