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
package org.apache.ivy.core.pack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZipPackingTest {

    private static final Project PROJECT = TestHelper.newProject();
    private static final File TEST_DIR = PROJECT.resolveFile("build/test/pack");

    @Before
    public void setUp() {
        Mkdir mkdir = new Mkdir();
        mkdir.setProject(PROJECT);
        mkdir.setDir(TEST_DIR);
        mkdir.execute();
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_INFO));
    }

    @After
    public void tearDown() {
        Delete del = new Delete();
        del.setProject(PROJECT);
        del.setDir(TEST_DIR);
        del.execute();
    }

    @Test
    public void zipPackingExtractsArchive() throws IOException {
        try (InputStream zip = new FileInputStream(PROJECT.resolveFile("test/zip/test.zip"))) {
            new ZipPacking().unpack(zip, TEST_DIR);
        }
        assertTrue("Expecting file a", FileUtil.resolveFile(TEST_DIR, "a").isFile());
        assertTrue("Expecting directory b", FileUtil.resolveFile(TEST_DIR, "b").isDirectory());
        assertTrue("Expecting file b/c", FileUtil.resolveFile(TEST_DIR, "b/c").isFile());
        assertTrue("Expecting directory d", FileUtil.resolveFile(TEST_DIR, "d").isDirectory());
        assertFalse("Not expecting file e", PROJECT.resolveFile("build/test/e").exists());
    }
}
