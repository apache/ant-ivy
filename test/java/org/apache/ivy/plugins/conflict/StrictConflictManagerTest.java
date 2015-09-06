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

import junit.framework.TestCase;

public class StrictConflictManagerTest extends TestCase {
    private Ivy ivy;

    private File cache;

    protected void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(StrictConflictManagerTest.class.getResource("ivysettings-strict-test.xml"));
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(cache);
    }

    public void testInitFromConf() throws Exception {
        ConflictManager cm = ivy.getSettings().getDefaultConflictManager();
        assertTrue(cm instanceof StrictConflictManager);
    }

    public void testNoConflictResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-noconflict.xml"),
            getResolveOptions());
    }

    public void testNoConflictWithDynamicRevisionResolve() throws Exception {
        ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-noconflict-dynamic.xml"),
            getResolveOptions());
    }

    public void testConflictResolve() throws Exception {
        try {
            ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-conflict.xml"),
                getResolveOptions());

            fail("Resolve should have failed with a conflict");
        } catch (StrictConflictException e) {
            // this is expected
        }
    }

    public void testConflictWithDynamicRevisionResolve() throws Exception {
        try {
            ivy.resolve(StrictConflictManagerTest.class.getResource("ivy-conflict-dynamic.xml"),
                getResolveOptions());

            fail("Resolve should have failed with a conflict");
        } catch (StrictConflictException e) {
            // this is expected
        }
    }

    private ResolveOptions getResolveOptions() {
        return new ResolveOptions().setValidate(false);
    }
}
