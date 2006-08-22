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

public class IvyListModulesTest extends TestCase {
    private File _cache;
    private IvyListModules _findModules;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _findModules = new IvyListModules();
        _findModules.setProject(project);
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

    public void testExact() throws Exception {
    	_findModules.setOrganisation("org1");
    	_findModules.setModule("mod1.1");
    	_findModules.setRevision("1.0");
    	_findModules.setProperty("found");
    	_findModules.setValue("[organisation]/[module]/[revision]");
    	_findModules.execute();
    	assertEquals("org1/mod1.1/1.0", _findModules.getProject().getProperty("found"));
    }
    
    public void testAllRevs() throws Exception {
    	_findModules.setOrganisation("org1");
    	_findModules.setModule("mod1.1");
    	_findModules.setRevision("*");
    	_findModules.setProperty("found.[revision]");
    	_findModules.setValue("true");
    	_findModules.execute();
    	assertEquals("true", _findModules.getProject().getProperty("found.1.0"));
    	assertEquals("true", _findModules.getProject().getProperty("found.1.1"));
    	assertEquals("true", _findModules.getProject().getProperty("found.2.0"));
    }
    
}
