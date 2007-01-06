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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.Path;

public class IvyCachePathTest extends TestCase {
    private File _cache;
    private IvyCachePath _path;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _path = new IvyCachePath();
        _path.setProject(_project);
        _path.setCache(_cache);
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    public void testSimple() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        _path.setPathid("simple-pathid");
        _path.execute();
        Object ref = _project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(_path.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());
    }

    public void testInline1() throws Exception {
    	// we first resolve another ivy file
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setFile(new File("test/java/fr/jayasoft/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	assertTrue(_path.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    	
    	// then we resolve a dependency directly
    	_path.setOrganisation("org1");
    	_path.setModule("mod1.2");
    	_path.setRevision("2.0");
    	_path.setInline(true);
        _path.setPathid("simple-pathid");
        _path.execute();
        Object ref = _project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(_path.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());
    }

    public void testInline2() throws Exception {
    	// we first resolve a dependency directly
    	_path.setOrganisation("org1");
    	_path.setModule("mod1.2");
    	_path.setRevision("2.0");
    	_path.setInline(true);
        _path.setPathid("simple-pathid");
        _path.execute();
        Object ref = _project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(_path.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());

        // we then resolve another ivy file
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setFile(new File("test/java/fr/jayasoft/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	assertTrue(_path.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }


    public void testEmptyConf() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-108.xml");
        _path.setPathid("emptyconf-pathid");
        _path.setConf("empty");
        _path.execute();
        Object ref = _project.getReference("emptyconf-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(0, p.size());
    }

    public void testFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _path.setPathid("failure-pathid");
            _path.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _path.setPathid("haltfailure-pathid");
            _path.setHaltonfailure(false);
            _path.execute();
        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }
}
