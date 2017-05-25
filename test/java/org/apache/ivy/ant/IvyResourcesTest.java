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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResource;

import junit.framework.TestCase;

public class IvyResourcesTest extends TestCase {

    private IvyResources resources;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());

        resources = new IvyResources();
        resources.setProject(project);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(getIvy(), organisation, module, revision, artifact,
            type, ext);
    }

    private Ivy getIvy() {
        return resources.getIvyInstance();
    }

    private List asList(IvyResources ivyResources) {
        List resources = new ArrayList();
        Iterator it = ivyResources.iterator();
        while (it.hasNext()) {
            Object r = it.next();
            assertTrue(r instanceof FileResource);
            resources.add(((FileResource) r).getFile());
        }
        return resources;
    }

    public void testSimple() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        List files = asList(resources);
        assertEquals(1, files.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar"),
            files.get(0));
    }

    public void testMultiple() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.3");
        dependency.setRev("0.7");

        List files = asList(resources);
        assertEquals(5, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.3", "0.7", "mod2.3", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar",
            "jar")));
    }

    public void testMultipleWithConf() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("A");

        List files = asList(resources);
        assertEquals(3, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar",
            "jar")));
    }

    public void testMultipleWithConf2() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        List files = asList(resources);
        assertEquals(4, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar",
            "jar")));
    }

    public void testExclude() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        IvyExclude exclude = resources.createExclude();
        exclude.setOrg("org1");
        exclude.setModule("mod1.1");

        List files = asList(resources);
        assertEquals(3, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar",
            "jar")));
    }

    public void testDependencyExclude() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        IvyDependencyExclude exclude = dependency.createExclude();
        exclude.setOrg("org1");

        List files = asList(resources);
        assertEquals(3, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar",
            "jar")));
    }

    public void testDependencyInclude() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resources.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.9");

        IvyDependencyInclude include = dependency.createInclude();
        include.setName("art22-1");

        List files = asList(resources);
        assertEquals(2, files.size());
        assertTrue(files.contains(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar",
            "jar")));
        assertTrue(files.contains(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar",
            "jar")));
    }

    public void testFail() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("noexisting");
        dependency.setRev("2.0");

        try {
            resources.iterator();
            fail("A fail resolved should have raised a build exception");
        } catch (BuildException e) {
            // ok
        }
    }

}
