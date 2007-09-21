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

import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyAntSettingsTest extends TestCase {
    private IvyAntSettings antSettings;

    protected void setUp() throws Exception {
        Project project = new Project();
        project.setProperty("myproperty", "myvalue");

        antSettings = new IvyAntSettings();
        antSettings.setProject(project);
    }

    public void testExposeAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        antSettings.setUrl(confUrl);
        antSettings.setId("this.id");

        assertNotNull(antSettings.getConfiguredIvyInstance());

        assertEquals("value", 
            antSettings.getProject().getProperty("ivy.test.variable"));
        assertEquals("value", 
            antSettings.getProject().getProperty("ivy.test.variable.this.id"));
    }

    public void testIncludeTwice() throws Exception {
        // IVY-601
        antSettings.setFile(new File("test/java/org/apache/ivy/ant/ivysettings-include-twice.xml"));
        antSettings.setId("this.id");

        assertNotNull(antSettings.getConfiguredIvyInstance());
    }


}
