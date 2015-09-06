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
package org.apache.ivy.core.install;

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import junit.framework.TestCase;

public class InstallTest extends TestCase {

    public void testSimple() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), ivy.getSettings()
                .getDefaultResolver().getName(), "install", new InstallOptions());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testValidate() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("orgfailure", "modfailure", "1.0"), ivy
                .getSettings().getDefaultResolver().getName(), "install", new InstallOptions());

        assertFalse(new File("build/test/install/orgfailure/modfailure/ivy-1.0.xml").exists());
        assertFalse(new File("build/test/install/orgfailure/modfailure/modfailure-1.0.jar")
                .exists());
    }

    public void testMaven() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ResolveReport rr = ivy.install(ModuleRevisionId.newInstance("org.apache", "test", "1.0"),
            ivy.getSettings().getDefaultResolver().getName(), "install", new InstallOptions());

        assertTrue(new File("build/test/install/org.apache/test/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org.apache/test/test-1.0.jar").exists());

        // the original descriptor is not installed
        assertFalse(new File("build/test/install/org.apache/test/test-1.0.pom").exists());

        ivy.install(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), ivy.getSettings()
                .getDefaultResolver().getName(), "install", new InstallOptions()
                .setInstallOriginalMetadata(true).setOverwrite(true));

        // the original descriptor is installed now, too
        assertTrue(new File("build/test/install/org.apache/test/test-1.0.pom").exists());
    }

    public void testNoValidate() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("orgfailure", "modfailure", "1.0"), ivy
                .getSettings().getDefaultResolver().getName(), "install",
            new InstallOptions().setValidate(false));

        assertTrue(new File("build/test/install/orgfailure/modfailure/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/orgfailure/modfailure/modfailure-1.0.jar").exists());
    }

    public void testSimpleWithoutDefaultResolver() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings-nodefaultresolver.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), "test", "install",
            new InstallOptions());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testDependencies() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), ivy.getSettings()
                .getDefaultResolver().getName(), "install", new InstallOptions());

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testLatestDependenciesNoDefaultResolver() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings-nodefaultresolver.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.1"), "test", "install",
            new InstallOptions());

        assertTrue(new File("build/test/install/org1/mod1.4/ivy-1.0.1.xml").exists());

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-2.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.2.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.2.jar").exists());
    }

    public void testLatestDependenciesDummyDefaultResolver() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings-dummydefaultresolver.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.1"), "test", "install",
            new InstallOptions());

        assertTrue(new File("build/test/install/org1/mod1.4/ivy-1.0.1.xml").exists());

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-2.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.2.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.2.jar").exists());
    }

    public void testNotTransitive() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), ivy.getSettings()
                .getDefaultResolver().getName(), "install",
            new InstallOptions().setTransitive(false));

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());

        assertFalse(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertFalse(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testRegexpMatcher() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));

        ivy.install(ModuleRevisionId.newInstance("org1", ".*", ".*"), "1", "install",
            new InstallOptions().setMatcherName(PatternMatcher.REGEXP).setOverwrite(true));

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.1.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.1.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());

        // mod1.3 is split because Ivy thinks there are two versions of the module:
        // this is the normal behaviour in this case
        assertTrue(new File("build/test/install/org1/mod1.3/ivy-B-3.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/ivy-A-3.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/mod1.3-A-3.0.jar").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/mod1.3-B-3.0.jar").exists());

        assertTrue(new File("build/test/install/org1/mod1.4/ivy-1.0.1.xml").exists());
    }

    protected void setUp() throws Exception {
        TestHelper.createCache();
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
        cleanInstall();
    }

    private void cleanInstall() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/install"));
        del.execute();
    }
}
