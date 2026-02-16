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
package org.apache.ivy.plugins.repository.file;

import java.io.File;

import org.apache.ivy.util.FileUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileRepositoryTest {

    private File repoDir;

    @Before
    public void setUp() throws Exception {
        repoDir = new File("build/filerepo").getAbsoluteFile();
        repoDir.mkdirs();
    }

    @After
    public void tearDown() {
        if (repoDir != null && repoDir.exists()) {
            FileUtil.forceDelete(repoDir);
        }
    }

    @Test
    public void putWrites() throws Exception {
        FileRepository fp = new FileRepository(repoDir);
        fp.put(new File("build.xml"), "foo/bar/baz.xml", true);
        assertTrue(new File(repoDir + "/foo/bar/baz.xml").exists());
    }

    @Test
    public void putWontWriteOutsideBasedir() throws Exception {
        FileRepository fp = new FileRepository(repoDir);
        try {
            fp.put(new File("build.xml"), "../baz.xml", true);
            fail("should have thrown an exception");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void getReads() throws Exception {
        FileRepository fp = new FileRepository(repoDir);
        fp.put(new File("build.xml"), "foo/bar/baz.xml", true);
        fp.get("foo/bar/baz.xml", new File("build/filerepo/a.xml"));
        assertTrue(new File(repoDir + "/a.xml").exists());
    }

    @Test
    public void getWontReadOutsideBasedir() throws Exception {
        FileRepository fp = new FileRepository(repoDir);
        try {
            fp.get("../../build.xml", new File("build/filerepo/a.xml"));
            fail("should have thrown an exception");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

}
