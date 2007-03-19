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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyFindRevisionTest extends TestCase {
    private File _cache;
    private IvyFindRevision _findRevision;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        _findRevision = new IvyFindRevision();
        _findRevision.setProject(project);
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

    public void testProperty() throws Exception {
    	_findRevision.setOrganisation("org1");
    	_findRevision.setModule("mod1.1");
    	_findRevision.setRevision("1.0");
    	_findRevision.setProperty("test.revision");
    	_findRevision.execute();
    	assertEquals("1.0", _findRevision.getProject().getProperty("test.revision"));
    }
    
    public void testLatest() throws Exception {
    	_findRevision.setOrganisation("org1");
    	_findRevision.setModule("mod1.1");
    	_findRevision.setRevision("latest.integration");
    	_findRevision.execute();
    	assertEquals("2.0", _findRevision.getProject().getProperty("ivy.revision"));
    }
    
    public void testLatestSubversion() throws Exception {
    	_findRevision.setOrganisation("org1");
    	_findRevision.setModule("mod1.1");
    	_findRevision.setRevision("1.0+");
    	_findRevision.execute();
    	assertEquals("1.0.1", _findRevision.getProject().getProperty("ivy.revision"));
    }
    
}
