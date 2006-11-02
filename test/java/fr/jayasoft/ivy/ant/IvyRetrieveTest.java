/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import fr.jayasoft.ivy.util.IvyPatternHelper;

public class IvyRetrieveTest extends TestCase {
    private static final String IVY_RETRIEVE_PATTERN = "build/test/lib/[organisation]/[module]/ivy-[revision].xml";
    private static final String RETRIEVE_PATTERN = "build/test/lib/[conf]/[artifact]-[revision].[type]";
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
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, 
        		"org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")).exists());
    }
    
    public void testInline() throws Exception {
    	// we first resolve another ivy file
    	IvyResolve resolve = new IvyResolve();
    	resolve.setProject(_project);
    	resolve.setFile(new File("test/java/fr/jayasoft/ivy/ant/ivy-latest.xml"));
    	resolve.execute();
    	
    	assertTrue(_retrieve.getIvyInstance().getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    	
    	// then we resolve a dependency directly
    	_retrieve.setOrganisation("org1");
    	_retrieve.setModule("mod1.2");
    	_retrieve.setRevision("2.0");
    	_retrieve.setInline(true);
    	_retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, 
        		"org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")).exists());
    }


    public void testWithConf() throws Exception {
        _project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar", "extension")).exists());
    }

    public void testSync() throws Exception {
        _project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        _retrieve.setSync(true);
        
        File[] old = new File[] {
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "unknown")), // unknown configuration
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "0.4", "mod6.1", "unknown", "unknown", "default")), // unknown type
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "0.4", "unknown", "jar", "jar", "default")), // unknown artifact
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "unknown", "mod6.1", "jar", "jar", "default")), // unknown revision
        };
        for (int i = 0; i < old.length; i++) {
			touch(old[i]);
		}
        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar", "extension")).exists());
        for (int i = 0; i < old.length; i++) {
			assertFalse(old[i] +" should have been deleted by sync", old[i].exists());
		}
        assertFalse(new File("build/test/lib/unknown").exists()); //even conf directory should have been deleted
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

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0", "mod1.2",
                "jar", "jar")).exists());
    }

	public void testUseOrigin() throws Exception {
        // test case for IVY-304
		// first we do a resolve with useOrigin=true in another project
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");
        project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setCache(_cache);
        resolve.setUseOrigin(true);
        resolve.execute();

        // then we do a retrieve with the correct module information and useOrigin=false
        _retrieve.setOrganisation("jayasoft");
        _retrieve.setModule("resolve-simple");
        _retrieve.setConf("default");
        _retrieve.setUseOrigin(false);
        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0", "mod1.2",
                "jar", "jar")).exists());
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

    public void testCustomIvyPattern() throws Exception {
        //      mod2.5 depends on virtual mod2.3 which depends on mod2.1 which depends on mod1.1 which depends on mod1.2
        _project.setProperty("ivy.dep.file", "test/repositories/1/org2/mod2.5/ivys/ivy-0.6.1.xml");

        String ivyPattern = IVY_RETRIEVE_PATTERN;

        _retrieve.setIvypattern(ivyPattern);
        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org2", "mod2.3", "0.4.1", "ivy", "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org2", "mod2.1", "0.3", "ivy", "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org1", "mod1.1", "1.0", "ivy", "ivy", "xml")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern,
                "org1", "mod1.2", "2.0", "ivy", "ivy", "xml")).exists());
    }
    
    public void testCustomIvyPatternWithConf() throws Exception {
        _project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");

        String ivyPattern = "build/test/lib/[conf]/[organisation]/[module]/ivy-[revision].xml";

        _retrieve.setIvypattern(ivyPattern);
        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org6", "mod6.1", "0.4", "ivy", "ivy", "xml", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org6", "mod6.1", "0.4", "ivy", "ivy", "xml", "extension")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern,
                "org1", "mod1.2", "2.1", "ivy", "ivy", "xml", "extension")).exists());
    }

    public void testSyncWithIvyPattern() throws Exception {
        _project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");

        String ivyPattern = "build/test/lib/[conf]/[organisation]/[module]/ivy-[revision].xml";

        _retrieve.setIvypattern(ivyPattern);

        _retrieve.setSync(true);
        
        File[] old = new File[] {
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar", "unknown")), // unknown configuration
        		new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN,
                        "org6", "mod6.1", "0.4", "mod6.1", "unknown", "unknown", "default")), // unknown type
        		new File(IvyPatternHelper.substitute(ivyPattern,
                        "org6", "mod6.1", "0.4", "ivy", "ivy", "xml", "unk")), // unknown conf for ivy
        		new File(IvyPatternHelper.substitute(ivyPattern,
                        "unknown", "mod6.1", "0.4", "ivy", "ivy", "xml", "default")), // unknown organisation for ivy
        };
        for (int i = 0; i < old.length; i++) {
			touch(old[i]);
		}

        _retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org6", "mod6.1", "0.4", "ivy", "ivy", "xml", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern,
                "org6", "mod6.1", "0.4", "ivy", "ivy", "xml", "extension")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern,
                "org1", "mod1.2", "2.1", "ivy", "ivy", "xml", "extension")).exists());
        for (int i = 0; i < old.length; i++) {
			assertFalse(old[i] +" should have been deleted by sync", old[i].exists());
		}
        assertFalse(new File("build/test/lib/unknown").exists()); 
        assertFalse(new File("build/test/lib/unk").exists()); 
        assertFalse(new File("build/test/lib/default/unknown").exists()); 
    }
    
    public void testDoubleRetrieveWithDifferentConfigurations() {
    	// IVY-315
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-doubleretrieve.xml");
        
        _retrieve.setConf("compile");
        _retrieve.execute();
    	
        _retrieve = new IvyRetrieve();
        _retrieve.setProject(_project);
        _retrieve.setCache(_cache);
        _retrieve.setPattern(RETRIEVE_PATTERN);
        _retrieve.setConf("compile,unittest");
        _retrieve.execute();
    }

    // creates an empty file, creating parent directories if necessary
    private void touch(File file) throws IOException {
    	if (file.getParentFile() != null) {
    		file.getParentFile().mkdirs();
    	}
    	file.createNewFile();
	}

}
