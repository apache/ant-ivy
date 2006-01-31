/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import fr.jayasoft.ivy.matcher.PatternMatcher;

import junit.framework.TestCase;

public class InstallTest extends TestCase {

    public void testSimple() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        
        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), 
                ivy.getDefaultResolver().getName(), 
                "install", true, true, true, null, _cache, PatternMatcher.EXACT);
        
        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testDependencies() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        
        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), 
                ivy.getDefaultResolver().getName(), 
                "install", true, true, true, null, _cache, PatternMatcher.EXACT);
        
        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());
        
        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testNotTransitive() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        
        ivy.install(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), 
                ivy.getDefaultResolver().getName(), 
                "install", false, true, true, null, _cache, PatternMatcher.EXACT);
        
        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());
        
        assertFalse(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertFalse(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
    }

    public void testRegexpMatcher() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        
        ivy.install(ModuleRevisionId.newInstance("org1", ".*", ".*"), 
                "1", 
                "install", false, true, true, null, _cache, PatternMatcher.REGEXP);
        
        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.0.jar").exists());
        
        assertTrue(new File("build/test/install/org1/mod1.1/ivy-1.1.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.1/mod1.1-1.1.jar").exists());
        
        assertTrue(new File("build/test/install/org1/mod1.2/ivy-2.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.2/mod1.2-2.0.jar").exists());
        
        // mod1.3 is split because Ivy thinks there are two versions of the module:
        // this is the normal behaviour in this case
        assertTrue(new File("build/test/install/org1/mod1.3/ivy-B-3.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/ivy-A-3.0.xml").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/mod1.3-A-3.0.jar").exists());
        assertTrue(new File("build/test/install/org1/mod1.3/mod1.3-B-3.0.jar").exists());
        
        assertTrue(new File("build/test/install/org1/mod1.4/ivy-1.0.1.xml").exists());
    }

    
    
    private File _cache;
    protected void setUp() throws Exception {
        createCache();
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
        cleanInstall();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    private void cleanInstall() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/install"));
        del.execute();
    }
}
