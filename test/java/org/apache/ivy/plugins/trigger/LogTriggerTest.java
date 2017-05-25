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
package org.apache.ivy.plugins.trigger;

import java.io.File;
import java.util.Date;

import org.apache.ivy.core.event.resolve.StartResolveEvent;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MockMessageLogger;

import junit.framework.TestCase;

public class LogTriggerTest extends TestCase {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private StartResolveEvent ev;

    private LogTrigger trigger;

    private File testDir;

    protected void setUp() {
        ev = new StartResolveEvent(DefaultModuleDescriptor.newBasicInstance(
            ModuleRevisionId.parse("o#A;1"), new Date()), new String[] {"c"});
        trigger = new LogTrigger();
        trigger.setEvent(ev.getName());
        testDir = new File("build/test/trigger");
        testDir.mkdirs();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(testDir);
    }

    public void testMessage() throws Exception {
        trigger.setMessage("msg: ${organisation} ${module} ${revision}");

        MockMessageLogger mockLogger = new MockMessageLogger();
        Message.setDefaultLogger(mockLogger);
        trigger.progress(ev);

        mockLogger.assertLogInfoContains("msg: o A 1");
    }

    public void testFile() throws Exception {
        trigger.setMessage("msg: ${organisation} ${module} ${revision}");
        File f = new File(testDir, "test.log");
        trigger.setFile(f);

        trigger.progress(ev);

        assertTrue(f.exists());
        assertEquals("msg: o A 1" + LINE_SEPARATOR, FileUtil.readEntirely(f));

        trigger.progress(ev);

        assertEquals("msg: o A 1" + LINE_SEPARATOR + "msg: o A 1" + LINE_SEPARATOR,
            FileUtil.readEntirely(f));
    }

    public void testFileNoAppend() throws Exception {
        trigger.setMessage("msg: ${organisation} ${module} ${revision}");
        File f = new File(testDir, "test.log");
        trigger.setFile(f);
        trigger.setAppend(false);

        trigger.progress(ev);
        trigger.progress(ev);

        assertTrue(f.exists());
        assertEquals("msg: o A 1" + LINE_SEPARATOR, FileUtil.readEntirely(f));
    }
}
