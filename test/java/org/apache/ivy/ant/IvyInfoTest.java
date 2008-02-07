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

import junit.framework.TestCase;

import org.apache.tools.ant.Project;

public class IvyInfoTest extends TestCase {
    private IvyInfo info;

    protected void setUp() throws Exception {
        Project project = new Project();

        info = new IvyInfo();
        info.setProject(project);
    }

    public void testSimple() throws Exception {
        info.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        info.execute();
        
        assertEquals("apache", info.getProject().getProperty("ivy.organisation"));
        assertEquals("resolve-simple", info.getProject().getProperty("ivy.module"));
        assertEquals("1.0", info.getProject().getProperty("ivy.revision"));
        assertEquals("default", info.getProject().getProperty("ivy.configurations"));
        assertEquals("default", info.getProject().getProperty("ivy.public.configurations"));
    }

    public void testAll() throws Exception {
        info.setFile(new File("test/java/org/apache/ivy/ant/ivy-info-all.xml"));
        info.execute();
        
        assertEquals("apache", info.getProject().getProperty("ivy.organisation"));
        assertEquals("info-all", info.getProject().getProperty("ivy.module"));
        assertEquals("1.0", info.getProject().getProperty("ivy.revision"));
        assertEquals("release", info.getProject().getProperty("ivy.status"));
        assertEquals("default, test, private", info.getProject().getProperty("ivy.configurations"));
        assertEquals("default, test", info.getProject().getProperty("ivy.public.configurations"));
        assertEquals("trunk", info.getProject().getProperty("ivy.branch"));
        assertEquals("myvalue", info.getProject().getProperty("ivy.extra.myextraatt"));
    }

}