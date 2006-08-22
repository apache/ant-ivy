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

public class IvyFindModuleTest extends TestCase {
    private File _cache;
    private IvyFindModule _findModule;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _findModule = new IvyFindModule();
        _findModule.setProject(project);
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

    public void testPrefix() throws Exception {
    	_findModule.setOrganisation("org1");
    	_findModule.setModule("mod1.1");
    	_findModule.setRevision("1.0");
    	_findModule.setPrefix("test");
    	_findModule.execute();
    	assertEquals("org1", _findModule.getProject().getProperty("test.organisation"));
    	assertEquals("mod1.1", _findModule.getProject().getProperty("test.module"));
    	assertEquals("1.0", _findModule.getProject().getProperty("test.revision"));
    }
    
    public void testLatest() throws Exception {
    	_findModule.setOrganisation("org1");
    	_findModule.setModule("mod1.1");
    	_findModule.setRevision("latest.integration");
    	_findModule.execute();
    	assertEquals("org1", _findModule.getProject().getProperty("ivy.organisation"));
    	assertEquals("mod1.1", _findModule.getProject().getProperty("ivy.module"));
    	assertEquals("2.0", _findModule.getProject().getProperty("ivy.revision"));
    }
    
    public void testLatestSubversion() throws Exception {
    	_findModule.setOrganisation("org1");
    	_findModule.setModule("mod1.1");
    	_findModule.setRevision("1.0+");
    	_findModule.setPrefix("");
    	_findModule.execute();
    	assertEquals("org1", _findModule.getProject().getProperty("organisation"));
    	assertEquals("mod1.1", _findModule.getProject().getProperty("module"));
    	assertEquals("1.0.1", _findModule.getProject().getProperty("revision"));
    }
    
}
