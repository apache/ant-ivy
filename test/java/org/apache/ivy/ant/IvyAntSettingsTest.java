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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyAntSettingsTest extends TestCase {
    private IvyAntSettings antSettings;
    private Project project;

    protected void setUp() throws Exception {
        project = new Project();
        project.setProperty("myproperty", "myvalue");

        antSettings = new IvyAntSettings();
        antSettings.setProject(project);
    }
    
    private Ivy getIvyInstance() {
        return antSettings.getConfiguredIvyInstance();
    }

    public void testDefault() throws Exception {
        // by default settings look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        antSettings.getProject()
                .setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        antSettings.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertNotNull(publicResolver);
        assertTrue(publicResolver instanceof IBiblioResolver);
        IBiblioResolver ibiblio = (IBiblioResolver) publicResolver;
        assertTrue(ibiblio.isM2compatible());
    }

    public void testDefault14() throws Exception {
        // by default settings look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        antSettings.getProject()
                .setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        antSettings.getProject().setProperty("ivy.14.compatible", "true");
        antSettings.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertTrue(publicResolver instanceof IvyRepResolver);
    }

    public void testFile() throws Exception {
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));

        antSettings.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath(), settings
                .getVariables().getVariable("ivy.settings.file"));
        assertEquals(new File("test/repositories/ivysettings.xml").toURL().toExternalForm(),
            settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), settings.getVariables().getVariable(
            "ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivysettings.xml").toURL().toExternalForm();
        String confDirUrl = new File("test/repositories").toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        antSettings.setUrl(confUrl);

        antSettings.execute();

        IvySettings settings = getIvyInstance().getSettings();

        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(confUrl, settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(confDirUrl, settings.getVariables().getVariable("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-test.xml")
                .toExternalForm();
        antSettings.setUrl(confUrl);

        antSettings.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
        assertEquals("myvalue", settings.getDefaultCache().getName());
    }

    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        antSettings.setUrl(confUrl);

        antSettings.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("lib/test/[artifact]-[revision].[ext]", 
            settings.getVariables().getVariable("ivy.retrieve.pattern"));
    }

    public void testExposeAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        antSettings.setUrl(confUrl);
        antSettings.setId("this.id");
        
        antSettings.execute();

        assertNotNull(getIvyInstance());

        assertEquals("value", 
            antSettings.getProject().getProperty("ivy.test.variable"));
        assertEquals("value", 
            antSettings.getProject().getProperty("ivy.test.variable.this.id"));
    }

    public void testIncludeTwice() throws Exception {
        // IVY-601
        antSettings.setFile(new File("test/java/org/apache/ivy/ant/ivysettings-include-twice.xml"));
        
        antSettings.execute();

        assertNotNull(getIvyInstance());
    }

    public void testOverrideTrue() throws Exception {
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        antSettings.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        antSettings = new IvyAntSettings();
        antSettings.setProject(project);
        antSettings.setOverride(IvyAntSettings.OVERRIDE_TRUE);
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        antSettings.execute();
        assertNotNull(getIvyInstance());
        
        assertTrue(ivy != getIvyInstance());
    }

    public void testOverrideFalse() throws Exception {
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        antSettings.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        IvyAntSettings newAntSettings = new IvyAntSettings();
        newAntSettings.setProject(project);
        newAntSettings.setOverride(IvyAntSettings.OVERRIDE_FALSE);
        newAntSettings.setFile(new File("test/repositories/ivysettings.xml"));
        newAntSettings.execute();
        
        assertTrue(antSettings == project.getReference(newAntSettings.getId()));
    }

    public void testOverrideNotAllowed() throws Exception {
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        antSettings.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        antSettings = new IvyAntSettings();
        antSettings.setProject(project);
        antSettings.setOverride(IvyAntSettings.OVERRIDE_NOT_ALLOWED);
        antSettings.setFile(new File("test/repositories/ivysettings.xml"));
        
        try {
            antSettings.execute();
            fail("calling settings twice with the same id with "
                    + "override=notallowed should raise an exception");
        } catch (BuildException e) {
            assertTrue(e.getMessage().indexOf("notallowed") != -1);
            assertTrue(e.getMessage().indexOf(antSettings.getId()) != -1);
        }
    }

    public void testInvalidOverride() throws Exception {
        try {
            antSettings.setOverride("unknown");
            fail("settings override with invalid value should raise an exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("unknown") != -1);
        }
    }

}
