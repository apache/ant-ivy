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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyArtifactPropertyTest extends TestCase {
    private File _cache;
    private IvyArtifactProperty _prop;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        _project = new Project();
        _project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        _prop = new IvyArtifactProperty();
        _prop.setProject(_project);
        _prop.setCache(_cache);
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
        _prop.setName("[module].[artifact]-[revision]");
        _prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        _prop.execute();
        String val = _project.getProperty("mod1.2.mod1.2-2.0");
        assertNotNull(val);
        assertEquals("build/cache/mod1.2/mod1.2-2.0.jar", val);
    }
    
    public void testWithResolveId() throws Exception {
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
    	resolve.setResolveId("abc");
    	resolve.execute();
    	
    	// resolve another ivy file
    	resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setCache(_cache);
    	resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
        _prop.setName("[module].[artifact]-[revision]");
        _prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        _prop.setResolveId("abc");
        _prop.execute();

        String val = _project.getProperty("mod1.2.mod1.2-2.0");
        assertNotNull(val);
        assertEquals("build/cache/mod1.2/mod1.2-2.0.jar", val);
    }

    public void testWithResolveIdWithoutResolve() throws Exception {
    	try {
	        _prop.setName("[module].[artifact]-[revision]");
	        _prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
	        _prop.setResolveId("abc");
	        _prop.execute();
	        fail("Task should have failed because no resolve was performed!");
    	} catch (BuildException e) {
    		// this is expected!
    	}
   }
}
