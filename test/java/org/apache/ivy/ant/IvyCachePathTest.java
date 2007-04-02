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

import org.apache.ivy.TestHelper;
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
        _project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

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
        _project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        _path.setPathid("simple-pathid");
        _path.execute();
        Object ref = _project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());
    }

	public void testInline1() throws Exception {
    	// we first resolve another ivy file
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    	
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
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
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
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());

        // we then resolve another ivy file
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }


    public void testEmptyConf() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-108.xml");
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
            _project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            _path.setPathid("failure-pathid");
            _path.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            _path.setPathid("haltfailure-pathid");
            _path.setHaltonfailure(false);
            _path.execute();
        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }
    
    public void testWithResolveId() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setResolveId("withResolveId");
    	resolve.execute();
    	
    	// resolve another ivy file
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	_path.setResolveId("withResolveId");
    	_path.setPathid("withresolveid-pathid");
    	_path.execute();

        Object ref = _project.getReference("withresolveid-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());
    }

    public void testWithResolveIdWithoutResolve() throws Exception {
    	Project project = new Project();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setResolveId("withResolveId");
    	resolve.execute();
    	
    	// resolve another ivy file
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	_path.setResolveId("withResolveId");
    	_path.setPathid("withresolveid-pathid");
    	_path.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	_path.execute();

        Object ref = _project.getReference("withresolveid-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path)ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File(p.list()[0]).getAbsolutePath());
    }
    
    public void testWithResolveIdAndMissingConfs() throws Exception {
    	Project project = new Project();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	resolve.setResolveId("testWithResolveIdAndMissingConfs");
    	resolve.setConf("default");
    	resolve.execute();
    	
    	// resolve another ivy file
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
        _project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-multiconf.xml");

    	_path.setResolveId("testWithResolveIdAndMissingConfs");
    	_path.setPathid("withresolveid-pathid");
    	_path.setConf("default,compile");
    	_path.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
    	_path.execute();
    }

    private File getArchiveFileInCache(String organisation, String module, String revision, String artifact, String type, String ext) {
		return TestHelper.getArchiveFileInCache(_path.getIvyInstance(), _cache, 
				organisation, module, revision, artifact, type, ext);
	}
}
