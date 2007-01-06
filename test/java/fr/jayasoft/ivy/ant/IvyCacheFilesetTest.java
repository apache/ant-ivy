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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

public class IvyCacheFilesetTest extends TestCase {
    private File _cache;
    private IvyCacheFileset _fileset;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _fileset = new IvyCacheFileset();
        _fileset.setProject(_project);
        _fileset.setCache(_cache);
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
        _fileset.setSetid("simple-setid");
        _fileset.execute();
        Object ref = _project.getReference("simple-setid");
        assertNotNull(ref);
        assertTrue(ref instanceof FileSet);
        FileSet fs = (FileSet)ref;
        DirectoryScanner directoryScanner = fs.getDirectoryScanner(_project);
        assertEquals(1, directoryScanner.getIncludedFiles().length);
        assertEquals(_fileset.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").getAbsolutePath(),
                new File("build/cache/"+directoryScanner.getIncludedFiles()[0]).getAbsolutePath());
    }

    public void testEmptyConf() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-108.xml");
        _fileset.setSetid("emptyconf-setid");
        _fileset.setConf("empty");
        _fileset.execute();
        Object ref = _project.getReference("emptyconf-setid");
        assertNotNull(ref);
        assertTrue(ref instanceof FileSet);
        FileSet fs = (FileSet)ref;
        DirectoryScanner directoryScanner = fs.getDirectoryScanner(_project);
        assertEquals(0, directoryScanner.getIncludedFiles().length);
    }

    public void testFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _fileset.setSetid("failure-setid");
            _fileset.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _fileset.setSetid("haltfailure-setid");
            _fileset.setHaltonfailure(false);
            _fileset.execute();
        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }
}
