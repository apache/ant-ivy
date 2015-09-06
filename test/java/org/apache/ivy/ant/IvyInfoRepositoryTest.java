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

import org.apache.ivy.TestHelper;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyInfoRepositoryTest extends TestCase {

    private IvyInfo info;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        info = new IvyInfo();
        info.setProject(project);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testProperty() throws Exception {
        info.setOrganisation("org1");
        info.setModule("mod1.1");
        info.setRevision("1.0");
        info.setProperty("test");
        info.execute();
        assertEquals("1.0", info.getProject().getProperty("test.revision"));
        assertEquals("org1", info.getProject().getProperty("test.organisation"));
        assertEquals("mod1.1", info.getProject().getProperty("test.module"));
        assertEquals("integration", info.getProject().getProperty("test.status"));
        assertEquals("default", info.getProject().getProperty("test.configurations"));
    }

    public void testLatest() throws Exception {
        info.setOrganisation("org1");
        info.setModule("mod1.1");
        info.setRevision("latest.integration");
        info.execute();
        assertEquals("2.0", info.getProject().getProperty("ivy.revision"));
        assertEquals("org1", info.getProject().getProperty("ivy.organisation"));
        assertEquals("mod1.1", info.getProject().getProperty("ivy.module"));
        assertEquals("integration", info.getProject().getProperty("ivy.status"));
        assertEquals("default", info.getProject().getProperty("ivy.configurations"));
    }

    public void testLatestSubversion() throws Exception {
        info.setOrganisation("org1");
        info.setModule("mod1.1");
        info.setRevision("1.0+");
        info.execute();
        assertEquals("1.0.1", info.getProject().getProperty("ivy.revision"));
        assertEquals("org1", info.getProject().getProperty("ivy.organisation"));
        assertEquals("mod1.1", info.getProject().getProperty("ivy.module"));
        assertEquals("integration", info.getProject().getProperty("ivy.status"));
        assertEquals("default", info.getProject().getProperty("ivy.configurations"));
    }

}
