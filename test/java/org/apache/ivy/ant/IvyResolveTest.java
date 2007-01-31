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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;


public class IvyResolveTest extends TestCase {
    private File _cache;
    private IvyResolve _resolve;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _resolve = new IvyResolve();
        _resolve.setProject(project);
        _resolve.setCache(_cache);
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
    	// depends on org="org1" name="mod1.2" rev="2.0"
        _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        _resolve.execute();
        
        assertTrue(getResolvedIvyFileInCache(ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
        
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    private File getArchiveFileInCache(String organisation, String module, String revision, String artifact, String type, String ext) {
		return TestHelper.getArchiveFileInCache(getIvy(), _cache, 
				organisation, module, revision, artifact, type, ext);
	}
    private File getIvyFileInCache(ModuleRevisionId id) {
		return getIvy().getCacheManager(_cache).getIvyFileInCache(id);
	}

	private File getResolvedIvyFileInCache(ModuleRevisionId id) {
		return getIvy().getCacheManager(_cache).getResolvedIvyFileInCache(id);
	}

	public void testInline() throws Exception {
    	// same as before, but expressing dependency directly without ivy file
        _resolve.setOrganisation("org1");
        _resolve.setModule("mod1.2");
        _resolve.setRevision("2.0");
        _resolve.setInline(true);
        _resolve.execute();
        
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testWithSlashes() throws Exception {
        _resolve.setFile(new File("test/java/org/apache/ivy/core/resolve/ivy-198.xml"));
        _resolve.execute();
        
        File resolvedIvyFileInCache = getResolvedIvyFileInCache(ModuleRevisionId.newInstance("myorg/mydep", "system/module", "1.0"));
        assertTrue(resolvedIvyFileInCache.exists());
        
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("yourorg/yourdep", "yoursys/yourmod", "1.0")).exists());
        assertTrue(getArchiveFileInCache("yourorg/yourdep", "yoursys/yourmod", "1.0", "yourmod", "jar", "jar").exists());
    }

    public void testDepsChanged() throws Exception {
        _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        _resolve.execute();
        
        assertEquals("true", getIvy().getVariable("ivy.deps.changed"));

        _resolve.execute();
        
        assertEquals("false", getIvy().getVariable("ivy.deps.changed"));
    }

    public void testConflictingDepsChanged() throws Exception {
        _resolve.setFile(new File("test/repositories/2/mod4.1/ivy-4.1.xml"));
        _resolve.execute();
        
        assertEquals("true", getIvy().getVariable("ivy.deps.changed"));

        _resolve.execute();
        
        assertEquals("false", getIvy().getVariable("ivy.deps.changed"));
    }

    public void testDouble() throws Exception {
        _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        _resolve.execute();
        
        assertEquals("resolve-simple", getIvy().getVariable("ivy.module"));

        _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-double.xml"));
        _resolve.execute();
        
        assertEquals("resolve-double", getIvy().getVariable("ivy.module"));
    }

    public void testFailure() throws Exception {
        try {
            _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            _resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailureOnBadDependencyIvyFile() throws Exception {
        try {
            _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure2.xml"));
            _resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailureOnBadStatusInDependencyIvyFile() throws Exception {
        try {
            _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure3.xml"));
            _resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            _resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            _resolve.setHaltonfailure(false);
            _resolve.execute();
        } catch (BuildException ex) {
            ex.printStackTrace();
            fail("failure raised an exception with haltonfailure set to false");
        }
    }
    
    private Ivy getIvy() {
        return _resolve.getIvyInstance();
    }

    
}
