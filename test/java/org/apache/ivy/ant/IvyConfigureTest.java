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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.apache.tools.ant.Project;

/**
 * Test the deprecated IvyConfigureTest and the underlying implementation AntIvySettings. When
 * IvyConfigure will be removed, this class should be renamed AntIvySettingsTest
 */
public class IvyConfigureTest extends TestCase {
    private IvyConfigure configure;

    protected void setUp() throws Exception {
        Project project = new Project();
        project.setProperty("myproperty", "myvalue");

        configure = new IvyConfigure();
        configure.setProject(project);
    }

    private Ivy getIvyInstance() {
        return IvyAntSettings.getDefaultInstance(configure.getProject())
                .getConfiguredIvyInstance();
    }

    public void testDefault() throws Exception {
        // by default configure look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        configure.getProject()
                .setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertNotNull(publicResolver);
        assertTrue(publicResolver instanceof IBiblioResolver);
        IBiblioResolver ibiblio = (IBiblioResolver) publicResolver;
        assertTrue(ibiblio.isM2compatible());
    }

    public void testDefault14() throws Exception {
        // by default configure look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        configure.getProject()
                .setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        configure.getProject().setProperty("ivy.14.compatible", "true");
        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertTrue(publicResolver instanceof IvyRepResolver);
    }

    public void testFile() throws Exception {
        configure.setFile(new File("test/repositories/ivysettings.xml"));

        configure.execute();

        Ivy ivy = configure.getIvyInstance();
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

    /*@SuppressWarnings*/
    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivysettings.xml").toURL().toExternalForm();
        String confDirUrl = new File("test/repositories").toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = configure.getIvyInstance().getSettings();

        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(confUrl, settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(confDirUrl, settings.getVariables().getVariable("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-test.xml")
                .toExternalForm();
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = configure.getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
        assertEquals("myvalue", settings.getDefaultCache().getName());
    }

    public void testExposeAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        configure.setUrl(confUrl);
        
        configure.execute();

        assertNotNull(configure.getIvyInstance());

        assertEquals("value", 
            configure.getProject().getProperty("ivy.test.variable"));

        assertEquals(configure.getIvyInstance().getSettings().getDefaultCache().getAbsolutePath(), 
            configure.getProject().getProperty("ivy.cache.dir"));
    }

    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = configure.getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("lib/test/[artifact]-[revision].[ext]", 
            settings.getVariables().getVariable("ivy.retrieve.pattern"));
    }

}
