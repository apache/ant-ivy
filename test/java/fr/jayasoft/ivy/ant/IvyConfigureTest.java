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
package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Ivy;

public class IvyConfigureTest extends TestCase {
    private File _cache;
    private IvyConfigure _configure;
    
    protected void setUp() throws Exception {
        Project project = new Project();
        project.setProperty("myproperty", "myvalue");

        _configure = new IvyConfigure();
        _configure.setProject(project);
    }

    public void testFile() throws Exception {
        _configure.setFile(new File("test/repositories/ivyconf.xml"));
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals(new File("build/cache"), ivy.getDefaultCache());
        assertEquals(new File("test/repositories/ivyconf.xml").getAbsolutePath(), ivy.getVariables().get("ivy.conf.file"));
        assertEquals(new File("test/repositories/ivyconf.xml").toURL().toExternalForm(), ivy.getVariables().get("ivy.conf.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), ivy.getVariables().get("ivy.conf.dir"));
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
    }

    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivyconf.xml").toURL().toExternalForm();
        String confDirUrl = new File("test/repositories").toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals(new File("build/cache"), ivy.getDefaultCache());
        assertEquals(confUrl, ivy.getVariables().get("ivy.conf.url"));
        assertEquals(confDirUrl, ivy.getVariables().get("ivy.conf.dir"));
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
    }

    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivyconf-test.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
        assertEquals("myvalue", ivy.getDefaultCache().getName());
    }

    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivyconf-props.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals("lib/test/[artifact]-[revision].[ext]", ivy.getVariables().get("ivy.retrieve.pattern"));
    }

    private Ivy getIvyInstance() {
        return (Ivy)_configure.getProject().getReference("ivy.instance");
    }

}
