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


public class IvyConfigureTest extends TestCase {
    private File _cache;
    private IvyConfigure _configure;
    
    protected void setUp() throws Exception {
        Project project = new Project();
        project.setProperty("myproperty", "myvalue");

        _configure = new IvyConfigure();
        _configure.setProject(project);
    }

    public void testDefault() throws Exception {
    	// by default configure look in the current directory for an ivysettings.xml file...
    	// but Ivy itself has one, and we don't want to use it
    	_configure.getProject().setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        _configure.execute();
        
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
    	_configure.getProject().setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
    	_configure.getProject().setProperty("ivy.14.compatible", "true");
        _configure.execute();
        
        IvySettings settings = getIvyInstance().getSettings();
		assertNotNull(settings.getDefaultResolver());
		
        DependencyResolver publicResolver = settings.getResolver("public");
		assertTrue(publicResolver instanceof IvyRepResolver);
    }

    public void testFile() throws Exception {
        _configure.setFile(new File("test/repositories/ivysettings.xml"));
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
		IvySettings settings = ivy.getSettings();
        assertNotNull(settings);
        
        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath(), settings.getVariables().get("ivy.settings.file"));
        assertEquals(new File("test/repositories/ivysettings.xml").toURL().toExternalForm(), settings.getVariables().get("ivy.settings.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), settings.getVariables().get("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().get("myproperty"));
    }

    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivysettings.xml").toURL().toExternalForm();
        String confDirUrl = new File("test/repositories").toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        IvySettings settings = getIvyInstance().getSettings();
        
        assertEquals(new File("build/cache"), settings.getDefaultCache());
        assertEquals(confUrl, settings.getVariables().get("ivy.settings.url"));
        assertEquals(confDirUrl, settings.getVariables().get("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().get("myproperty"));
    }

    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-test.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);
        
        assertEquals("myvalue", settings.getVariables().get("myproperty"));
        assertEquals("myvalue", settings.getDefaultCache().getName());
    }

    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);
        
        assertEquals("lib/test/[artifact]-[revision].[ext]", settings.getVariables().get("ivy.retrieve.pattern"));
    }

    private Ivy getIvyInstance() {
        return (Ivy)_configure.getProject().getReference("ivy.instance");
    }

}
