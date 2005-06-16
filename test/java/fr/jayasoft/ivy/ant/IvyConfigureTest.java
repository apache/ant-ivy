/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Ivy;

public class IvyConfigureTest extends TestCase {
    private File _cache;
    private IvyConfigure _configure;
    
    protected void setUp() throws Exception {
        Project project = new Project();
        project.setProperty("myproperty", "myvalue");

        _configure = new IvyConfigure();
        _configure.setProject(project);
    }

    public void testFile() throws Exception {
        _configure.setFile(new File("test/repositories/ivyconf.xml"));
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals(new File("build/cache"), ivy.getDefaultCache());
        assertEquals(new File("test/repositories/ivyconf.xml").getAbsolutePath(), ivy.getVariables().get("ivy.conf.file"));
        assertEquals(new File("test/repositories/ivyconf.xml").toURL().toExternalForm(), ivy.getVariables().get("ivy.conf.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), ivy.getVariables().get("ivy.conf.dir"));
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
    }

    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivyconf.xml").toURL().toExternalForm();
        String confDirUrl = new File("test/repositories").toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals(new File("build/cache"), ivy.getDefaultCache());
        assertEquals(confUrl, ivy.getVariables().get("ivy.conf.url"));
        assertEquals(confDirUrl, ivy.getVariables().get("ivy.conf.dir"));
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
    }

    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivyconf-test.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals("myvalue", ivy.getVariables().get("myproperty"));
        assertEquals("myvalue", ivy.getDefaultCache().getName());
    }

    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivyconf-props.xml").toExternalForm();
        _configure.setUrl(confUrl);
        
        _configure.execute();
        
        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        
        assertEquals("lib/test/[artifact]-[revision].[ext]", ivy.getVariables().get("ivy.retrieve.pattern"));
    }

    private Ivy getIvyInstance() {
        return (Ivy)_configure.getProject().getReference("ivy.instance");
    }

}
