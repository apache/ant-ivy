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
import java.net.MalformedURLException;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

import junit.framework.TestCase;

public class IvyTaskTest extends TestCase {

    public void testDefaultSettings() throws MalformedURLException {
        Project p = TestHelper.newProject();
        p.setBasedir("test/repositories");
        p.setProperty("myproperty", "myvalue");
        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(p);

        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("test/repositories/build/cache").getAbsoluteFile(),
            settings.getDefaultCache());
        // The next test doesn't always works on windows (mix C: and c: drive)
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath().toUpperCase(),
            new File(settings.getVariables().getVariable("ivy.settings.file")).getAbsolutePath()
                    .toUpperCase());
        assertEquals(new File("test/repositories/ivysettings.xml").toURI().toURL().toExternalForm()
                .toUpperCase(), settings.getVariables().getVariable("ivy.settings.url")
                .toUpperCase());
        assertEquals(new File("test/repositories").getAbsolutePath().toUpperCase(), settings
                .getVariables().getVariable("ivy.settings.dir").toUpperCase());
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    public void testReferencedSettings() throws MalformedURLException {
        Project p = TestHelper.newProject();
        p.setProperty("myproperty", "myvalue");

        IvyAntSettings antSettings = new IvyAntSettings();
        antSettings.setProject(p);
        // antSettings.setId("mySettings");
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        p.addReference("mySettings", antSettings);

        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(p);
        task.setSettingsRef(new Reference("mySettings"));
        Ivy ivy = task.getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("build/cache").getAbsoluteFile(), settings.getDefaultCache());
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath(), settings
                .getVariables().getVariable("ivy.settings.file"));
        assertEquals(
            new File("test/repositories/ivysettings.xml").toURI().toURL().toExternalForm(),
            settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), settings.getVariables()
                .getVariable("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    public void testIvyVersionAsAntProperty() {
        Project p = TestHelper.newProject();
        p.setBasedir("test/repositories");
        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(p);
        task.execute();

        assertNotNull(p.getProperty("ivy.version"));
    }
}
