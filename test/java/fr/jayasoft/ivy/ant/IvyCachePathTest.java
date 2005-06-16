/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
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
