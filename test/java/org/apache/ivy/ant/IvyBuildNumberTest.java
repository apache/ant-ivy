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

public class IvyBuildNumberTest extends TestCase {

    private IvyBuildNumber buildNumber;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        buildNumber = new IvyBuildNumber();
        buildNumber.setProject(project);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testDefault() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("newmod");
        buildNumber.execute();
        assertEquals(null, buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testDefault2() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("newmod");
        buildNumber.setDefault("1.0-dev-1");
        buildNumber.execute();
        assertEquals(null, buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("1.0-dev-1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testDefault3() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("newmod");
        buildNumber.setDefault("mydefault");
        buildNumber.execute();
        assertEquals(null, buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("mydefault", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testLatest() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.1");
        buildNumber.execute();
        assertEquals("2.0", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("2.1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testLatest2() throws Exception {
        buildNumber.setOrganisation("orgbn");
        buildNumber.setModule("buildnumber");
        buildNumber.execute();
        assertEquals("test", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("test.1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testPrefix() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.1");
        buildNumber.setPrefix("test");
        buildNumber.execute();
        assertEquals("2.0", buildNumber.getProject().getProperty("test.revision"));
        assertEquals("2.1", buildNumber.getProject().getProperty("test.new.revision"));
        assertEquals("0", buildNumber.getProject().getProperty("test.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("test.new.build.number"));
    }

    public void testBuildNumber() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.1");
        buildNumber.setRevision("1.");
        buildNumber.execute();
        assertEquals("1.1", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("1.2", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("2", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testBuildNumber2() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.5");
        buildNumber.setRevision("1.");
        buildNumber.execute();
        assertEquals("1.0.2", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("1.1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testBuildNumber3() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.1");
        buildNumber.setRevision("1.1");
        buildNumber.execute();
        assertEquals("1.1", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("1.1.1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testBuildNumber4() throws Exception {
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("mod1.1");
        buildNumber.setRevision("3.");
        buildNumber.execute();
        assertEquals(null, buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("3.0", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals(null, buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testWithBadChecksum() throws Exception {
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-checksums.xml");

        buildNumber = new IvyBuildNumber();
        buildNumber.setProject(project);
        buildNumber.setOrganisation("org1");
        buildNumber.setModule("badivycs");
        buildNumber.setRevision("1.");
        buildNumber.execute();
        assertEquals("1.0", buildNumber.getProject().getProperty("ivy.revision"));
        assertEquals("1.1", buildNumber.getProject().getProperty("ivy.new.revision"));
        assertEquals("0", buildNumber.getProject().getProperty("ivy.build.number"));
        assertEquals("1", buildNumber.getProject().getProperty("ivy.new.build.number"));
    }

    public void testChainResolver() throws Exception {
        // IVY-1037
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/IVY-1037/ivysettings.xml");

        buildNumber = new IvyBuildNumber();
        buildNumber.setProject(project);
        buildNumber.setOrganisation("org");
        buildNumber.setModule("module");
        buildNumber.setResolver("chain");
        buildNumber.execute();
        assertEquals("3", buildNumber.getProject().getProperty("ivy.new.revision"));
    }

}
