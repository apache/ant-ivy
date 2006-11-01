/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Echo;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;

public class IvyPublishTest extends TestCase {
    private File _cache;
    private IvyPublish _publish;
    private Project _project;
    
    protected void setUp() throws Exception {
        cleanTestDir();
        cleanRep();
        createCache();
        _project = new Project();
        _project.setProperty("ivy.conf.file", "test/repositories/ivyconf.xml");
        _project.setProperty("build", "build/test/publish");

        _publish = new IvyPublish();
        _publish.setProject(_project);
        _publish.setCache(_cache);
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        cleanCache();
        cleanTestDir();
        cleanRep();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    private void cleanTestDir() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/publish"));
        del.execute();
    }

    private void cleanRep() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("test/repositories/1/jayasoft"));
        del.execute();
    }

    public void testSimple() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        _publish.execute();
        
        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/ivy-1.2.xml").exists()); 
        
        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/jayasoft/resolve-simple/ivys/ivy-1.2.xml").exists()); 
        assertTrue(new File("test/repositories/1/jayasoft/resolve-simple/jars/resolve-simple-1.2.jar").exists());
        
        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), new File("test/repositories/1/jayasoft/resolve-simple/ivys/ivy-1.2.xml").toURL(), false);
        assertEquals("1.2", md.getModuleRevisionId().getRevision());
    }

    public void testMultiPatterns() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-publish-multi.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setResolver("1");
        File art = new File("build/test/publish/1/multi1-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        art = new File("build/test/publish/2/multi2-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        _publish.addArtifactspattern("build/test/publish/1/[artifact]-[revision].[ext]");
        _publish.addArtifactspattern("build/test/publish/2/[artifact]-[revision].[ext]");
        _publish.execute();
        
        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/1/ivy-1.2.xml").exists()); 
        
        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/jayasoft/multi/ivys/ivy-1.2.xml").exists()); 
        assertTrue(new File("test/repositories/1/jayasoft/multi/jars/multi1-1.2.jar").exists());
        assertTrue(new File("test/repositories/1/jayasoft/multi/jars/multi2-1.2.jar").exists());
    }

    public void testCustom() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-custom.xml");
        IvyResolve res = new IvyResolve();
        res.setValidate(false);
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setPubdate("20060906141243");
        _publish.setResolver("1");
        _publish.setValidate(false);
        File art = new File("build/test/publish/resolve-custom-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        _publish.execute();
        
        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/ivy-1.2.xml").exists()); 
        
        File dest = new File("test/repositories/1/jayasoft/resolve-custom/ivys/ivy-1.2.xml");
		// should have published the files with "1" resolver
        assertTrue(dest.exists()); 
        assertTrue(new File("test/repositories/1/jayasoft/resolve-custom/jars/resolve-custom-1.2.jar").exists());
        
        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), dest.toURL(), false);
        assertEquals("1.2", md.getModuleRevisionId().getRevision());
        
        // should have kept custom attributes
        assertEquals("cval1", md.getModuleRevisionId().getAttribute("custom-info"));
        assertEquals("cval2", md.getConfiguration("default").getAttribute("custom-conf"));
        assertEquals("cval3", md.getDependencies()[0].getAttribute("custom-dep"));
        
        // should respect the ivy file, with descriptions, ...
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(IvyPublishTest.class.getResourceAsStream("published-ivy-custom.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);
        
    }

    public void testNoDeliver() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        
        _publish.setPubrevision("1.3");
        _publish.setResolver("1");
        _publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");

        FileUtil.copy(new File("test/java/fr/jayasoft/ivy/ant/ivy-publish.xml"), new File("build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        _publish.execute();
        
        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/jayasoft/resolve-latest/ivys/ivy-1.3.xml").exists()); 
        assertTrue(new File("test/repositories/1/jayasoft/resolve-latest/jars/resolve-latest-1.3.jar").exists());
        
        // the published ivy version should be ok (ok in ivy-publish file)
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), new File("test/repositories/1/jayasoft/resolve-latest/ivys/ivy-1.3.xml").toURL(), false);
        assertEquals("1.3", md.getModuleRevisionId().getRevision());
        
        // should not have done delivery (replace dynamic revisions with static ones)
        assertEquals("latest.integration", md.getDependencies()[0].getDependencyRevisionId().getRevision());
    }

    public void testForceDeliver() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.3");
        _publish.setResolver("1");
        _publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");
        _publish.setForcedeliver(true);

        FileUtil.copy(new File("test/java/fr/jayasoft/ivy/ant/ivy-latest.xml"), new File("build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        _publish.execute();
        
        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/jayasoft/resolve-latest/ivys/ivy-1.3.xml").exists()); 
        assertTrue(new File("test/repositories/1/jayasoft/resolve-latest/jars/resolve-latest-1.3.jar").exists());
        
        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), new File("test/repositories/1/jayasoft/resolve-latest/ivys/ivy-1.3.xml").toURL(), false);
        assertEquals("1.3", md.getModuleRevisionId().getRevision());
    }

    public void testBadNoDeliver() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        
        _publish.setPubrevision("1.3");
        _publish.setResolver("1");
        _publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");

        FileUtil.copy(new File("test/java/fr/jayasoft/ivy/ant/ivy-latest.xml"), new File("build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        try {
        	_publish.execute();
        	fail("shouldn't publish ivy file with bad revision");
        } catch (BuildException ex) {
        	
        }
    }

    public void testReadonly() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        
        Echo echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File("test/repositories/1/jayasoft/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), 
                dest, null);

        echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();

        dest.setReadOnly();
        
        try {
            _publish.execute();
            fail("by default, publish should fail when a readonly artifact already exist");
        } catch (Exception ex) {
            assertTrue(dest.exists());
            BufferedReader reader = new BufferedReader(new FileReader(dest));
            assertEquals("old version", reader.readLine());
            reader.close();
        }
    }

    public void testOverwrite() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        
        Echo echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File("test/repositories/1/jayasoft/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), 
                dest, null);

        echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();


        _publish.setOverwrite(true);
        _publish.execute();
        assertTrue(dest.exists());
        BufferedReader reader = new BufferedReader(new FileReader(dest));
        assertEquals("new version", reader.readLine());
        reader.close();
    }

    public void testOverwriteReadOnly() throws Exception {
        _project.setProperty("ivy.dep.file", "test/java/fr/jayasoft/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(_project);
        res.execute();
        
        _publish.setPubrevision("1.2");
        _publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        
        Echo echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File("test/repositories/1/jayasoft/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), 
                dest, null);

        echo = new Echo();
        echo.setProject(_project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();

        dest.setReadOnly();
        
        _publish.setOverwrite(true);
        _publish.execute();
        assertTrue(dest.exists());
        BufferedReader reader = new BufferedReader(new FileReader(dest));
        assertEquals("new version", reader.readLine());
        reader.close();
    }

}
