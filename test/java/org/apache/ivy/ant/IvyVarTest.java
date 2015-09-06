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

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;

import junit.framework.TestCase;

public class IvyVarTest extends TestCase {
    public void testSimple() {
        IvyVar task = new IvyVar();
        task.setProject(TestHelper.newProject());
        task.setName("mytest");
        task.setValue("myvalue");
        task.execute();
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue", ivy.getVariable("mytest"));
    }

    public void testPrefix() {
        IvyVar task = new IvyVar();
        task.setProject(TestHelper.newProject());
        task.setName("mytest");
        task.setValue("myvalue");
        task.setPrefix("myprefix");
        task.execute();
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue", ivy.getVariable("myprefix.mytest"));
    }

    public void testURL() {
        IvyVar task = new IvyVar();
        task.setProject(TestHelper.newProject());
        task.setUrl(IvyVarTest.class.getResource("vartest.properties").toExternalForm());
        task.execute();
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue1", ivy.getVariable("mytest1"));
        assertEquals("myvalue2", ivy.getVariable("mytest2"));
    }

    public void testURLPrefix() {
        IvyVar task = new IvyVar();
        task.setProject(TestHelper.newProject());
        task.setUrl(IvyVarTest.class.getResource("vartest.properties").toExternalForm());
        task.setPrefix("myprefix.");
        task.execute();
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        assertEquals("myvalue1", ivy.getVariable("myprefix.mytest1"));
        assertEquals("myvalue2", ivy.getVariable("myprefix.mytest2"));
    }
}
