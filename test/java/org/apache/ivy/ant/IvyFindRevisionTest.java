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
package org.apache.ivy.ant;

import org.apache.ivy.TestHelper;

import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IvyFindRevisionTest {

    private IvyFindRevision findRevision;

    @Before
    public void setUp() {
        TestHelper.createCache();
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        findRevision = new IvyFindRevision();
        findRevision.setProject(project);
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testProperty() {
        findRevision.setOrganisation("org1");
        findRevision.setModule("mod1.1");
        findRevision.setRevision("1.0");
        findRevision.setProperty("test.revision");
        findRevision.execute();
        assertEquals("1.0", findRevision.getProject().getProperty("test.revision"));
    }

    @Test
    public void testLatest() {
        findRevision.setOrganisation("org1");
        findRevision.setModule("mod1.1");
        findRevision.setRevision("latest.integration");
        findRevision.execute();
        assertEquals("2.0", findRevision.getProject().getProperty("ivy.revision"));
    }

    @Test
    public void testLatestSubversion() {
        findRevision.setOrganisation("org1");
        findRevision.setModule("mod1.1");
        findRevision.setRevision("1.0+");
        findRevision.execute();
        assertEquals("1.0.1", findRevision.getProject().getProperty("ivy.revision"));
    }

}
