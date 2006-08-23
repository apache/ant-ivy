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

public class IvyFindRevisionTest extends TestCase {
    private File _cache;
    private IvyFindRevision _findRevision;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

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
