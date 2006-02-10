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

import fr.jayasoft.ivy.util.IvyPatternHelper;

public class IvyRetrieveTest extends TestCase {
    private static final String RETRIEVE_PATTERN = "build/test/lib/[artifact]-[revision].[type]";
    private File _cache;
    private IvyRetrieve _retrieve;
    private Project _project;
    
    protected void setUp() throws Exception {
        createCache();
        cleanTestLib();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _retrieve = new IvyRetrieve();
        _retrieve.setProject(_project);
        _retrieve.setCache(_cache);
        _retrieve.setPattern(RETRIEVE_PATTERN);
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
        cleanTestLib();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    private void cleanTestLib() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/lib"));
        del.execute();
    }

    public void testSimple() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        _retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")).exists());
    }

    public void testWithAPreviousResolve() throws Exception {
        // first we do a resolve in another project
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");
        project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setCache(_cache);
        resolve.execute();

        // then we do a retrieve with the correct module information
        _retrieve.setOrganisation("jayasoft");
        _retrieve.setModule("resolve-simple");
        _retrieve.setConf("default");
        _retrieve.execute();
        
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")).exists());
    }

    public void testFailureWithoutAPreviousResolve() throws Exception {
        // we do a retrieve with the module information whereas no resolve has been previously done
        try {
            _retrieve.setOrganisation("jayasoft");
            _retrieve.setModule("resolve-simple");
            _retrieve.setConf("default");
            _retrieve.execute();
            fail("retrieve without previous resolve should have thrown an exception");
        } catch (Exception ex) {
            // OK
        }
    }

    public void testFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _retrieve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-failure.xml");
            _retrieve.setHaltonfailure(false);
            _retrieve.execute();
        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }
}
