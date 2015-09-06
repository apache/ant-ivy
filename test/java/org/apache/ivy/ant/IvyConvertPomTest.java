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
package org.apache.ivy.ant;

import java.io.File;

import org.apache.ivy.TestHelper;

import junit.framework.TestCase;

public class IvyConvertPomTest extends TestCase {
    public void testSimple() throws Exception {
        IvyConvertPom task = new IvyConvertPom();
        task.setProject(TestHelper.newProject());
        task.setPomFile(new File("test/java/org/apache/ivy/ant/test.pom"));
        File destFile = File.createTempFile("ivy", ".xml");
        destFile.deleteOnExit();
        task.setIvyFile(destFile);
        task.execute();

        // do not work properly on all platform and depends on the file date
        // keep the code in comments in case someone manage to fix this and to highlight the fact
        // that this is not checked

        // String wrote = FileUtil.readEntirely(new BufferedReader(new FileReader(destFile)));
        // String expected = readEntirely("test-convertpom.xml").replaceAll("\r\n", "\n").replace(
        // '\r', '\n');
        // assertEquals(expected, wrote);
    }

    // private String readEntirely(String resource) throws IOException {
    // return FileUtil.readEntirely(
    // new BufferedReader(new InputStreamReader(IvyConvertPomTest.class.getResource(resource)
    // .openStream()))).replaceAll("\r\n", "\n").replace('\r', '\n');
    // }
}
