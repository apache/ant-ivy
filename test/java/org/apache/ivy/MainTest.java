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
package org.apache.ivy;

import java.io.File;

import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.cli.CommandLine;
import org.apache.ivy.util.cli.ParseException;

import junit.framework.TestCase;

public class MainTest extends TestCase {

    private File cache;

    protected void setUp() throws Exception {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testHelp() throws Exception {
        run(new String[] {"-?"});
    }

    public void testBadOption() throws Exception {
        try {
            run(new String[] {"-bad"});
            fail("running Ivy Main with -bad option should raise an exception");
        } catch (ParseException ex) {
            assertEquals("Unrecognized option: -bad", ex.getMessage());
        }
    }

    public void testMissingParameter() throws Exception {
        try {
            run(new String[] {"-ivy"});
            fail("running Ivy Main with missing argument for -ivy option should raise an exception");
        } catch (ParseException ex) {
            assertEquals("no argument for: ivy", ex.getMessage());
        }
    }

    public void testResolveSimple() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-ivy",
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    public void testResolveSimpleWithConfs() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-ivy",
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "-confs", "default"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    public void testResolveSimpleWithConfs2() throws Exception {
        run(new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs", "default",
                "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"});
        assertTrue(new File("build/cache/org1/mod1.2/ivy-2.0.xml").exists());
    }

    public void testExtraParams1() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "foo1",
                "foo2"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(2, leftOver.length);
        assertEquals("foo1", leftOver[0]);
        assertEquals("foo2", leftOver[1]);
    }

    public void testExtraParams2() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml", "--",
                "foo1", "foo2"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(2, leftOver.length);
        assertEquals("foo1", leftOver[0]);
        assertEquals("foo2", leftOver[1]);
    }

    public void testExtraParams3() throws Exception {
        String[] params = new String[] {"-settings", "test/repositories/ivysettings.xml", "-confs",
                "default", "-ivy", "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"};
        CommandLine line = Main.getParser().parse(params);
        String[] leftOver = line.getLeftOverArgs();
        assertNotNull(leftOver);
        assertEquals(0, leftOver.length);
    }

    private void run(String[] args) throws Exception {
        Main.run(Main.getParser(), args);
    }
}
