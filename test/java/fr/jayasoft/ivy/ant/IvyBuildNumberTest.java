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

public class IvyBuildNumberTest extends TestCase {
    private File _cache;
    private IvyBuildNumber _buildNumber;
    
    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");

        _buildNumber = new IvyBuildNumber();
        _buildNumber.setProject(project);
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

    public void testDefault() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("newmod");
    	_buildNumber.execute();
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("0", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("0", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testDefault2() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("newmod");
    	_buildNumber.setDefault("1.0-dev-1");
    	_buildNumber.execute();
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("1.0-dev-1", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testDefault3() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("newmod");
    	_buildNumber.setDefault("mydefault");
    	_buildNumber.execute();
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("mydefault", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testLatest() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.1");
    	_buildNumber.execute();
    	assertEquals("2.0", _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("2.1", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals("0", _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testLatest2() throws Exception {
    	_buildNumber.setOrganisation("orgbn");
    	_buildNumber.setModule("buildnumber");
    	_buildNumber.execute();
    	assertEquals("test", _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("test.1", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testPrefix() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.1");
    	_buildNumber.setPrefix("test");
    	_buildNumber.execute();
    	assertEquals("2.0", _buildNumber.getProject().getProperty("test.revision"));
    	assertEquals("2.1", _buildNumber.getProject().getProperty("test.new.revision"));
    	assertEquals("0", _buildNumber.getProject().getProperty("test.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("test.new.build.number"));
    }
    
    public void testBuildNumber() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.1");
    	_buildNumber.setRevision("1.");
    	_buildNumber.execute();
    	assertEquals("1.1", _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("1.2", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("2", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testBuildNumber2() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.5");
    	_buildNumber.setRevision("1.");
    	_buildNumber.execute();
    	assertEquals("1.0.2", _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("1.1", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals("0", _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testBuildNumber3() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.1");
    	_buildNumber.setRevision("1.1");
    	_buildNumber.execute();
    	assertEquals("1.1", _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("1.1.1", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("1", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
    public void testBuildNumber4() throws Exception {
    	_buildNumber.setOrganisation("org1");
    	_buildNumber.setModule("mod1.1");
    	_buildNumber.setRevision("3.");
    	_buildNumber.execute();
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.revision"));
    	assertEquals("3.0", _buildNumber.getProject().getProperty("ivy.new.revision"));
    	assertEquals(null, _buildNumber.getProject().getProperty("ivy.build.number"));
    	assertEquals("0", _buildNumber.getProject().getProperty("ivy.new.build.number"));
    }
    
}
