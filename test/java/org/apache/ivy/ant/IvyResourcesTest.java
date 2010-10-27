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
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyResourcesTest extends TestCase {

    private File cache;

    private IvyResources resources;

    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        resources = new IvyResources();
        resources.setProject(project);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    private File getIvyFileInCache(String organisation, String module, String revision) {
        ModuleRevisionId id = ModuleRevisionId.newInstance(organisation, module, revision);
        return TestHelper.getRepositoryCacheManager(getIvy(), id).getIvyFileInCache(id);
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(getIvy(), organisation, module, revision, artifact,
            type, ext);
    }

    private Ivy getIvy() {
        return resources.getIvyInstance();
    }

    public void testSimple() throws Exception {
        IvyDependency dependency = resources.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        resources.iterator();

        // dependencies
        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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

        resources.iterator();

        // dependencies
        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.3", "0.7").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.3", "0.7", "mod2.3", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.1", "0.3").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org1", "mod1.1", "1.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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

        resources.iterator();

        // dependencies
        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.2", "0.10").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.1", "0.7").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache("org1", "mod1.1", "1.0").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
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

        resources.iterator();

        // dependencies
        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.2", "0.10").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.1", "0.7").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org1", "mod1.1", "1.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
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

        resources.iterator();

        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.2", "0.10").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.1", "0.7").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache("org1", "mod1.1", "1.0").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
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

        resources.iterator();

        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.2", "0.10").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.1", "0.7").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache("org1", "mod1.1", "1.0").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
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

        resources.iterator();

        assertTrue(getIvyFileInCache("org1", "mod1.2", "2.0").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache("org2", "mod2.2", "0.9").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-2", "jar", "jar").exists());
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
