/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyArtifactPropertyTest extends TestCase {
    private File _cache;
    private IvyArtifactProperty _prop;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

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
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        _prop.setName("[module].[artifact]-[revision]");
        _prop.setValue("${cache.dir}/[module]/[artifact]-[revision].[type]");
        _prop.execute();
        String val = _project.getProperty("mod1.2.mod1.2-2.0");
        assertNotNull(val);
        assertEquals("build/cache/mod1.2/mod1.2-2.0.jar", val);
    }
}
