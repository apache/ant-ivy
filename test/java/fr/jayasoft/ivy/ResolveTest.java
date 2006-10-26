/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.circular.CircularDependencyException;
import fr.jayasoft.ivy.circular.ErrorCircularDependencyStrategy;
import fr.jayasoft.ivy.circular.IgnoreCircularDependencyStrategy;
import fr.jayasoft.ivy.circular.WarnCircularDependencyStrategy;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.ConfigurationResolveReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.report.XmlReportOutputter;
import fr.jayasoft.ivy.resolver.BasicResolver;
import fr.jayasoft.ivy.resolver.DualResolver;
import fr.jayasoft.ivy.resolver.FileSystemResolver;
import fr.jayasoft.ivy.util.FileUtil;

/**
 * @author Xavier Hanin
 *
 */
public class ResolveTest extends TestCase {
	private final Ivy _ivy;
    private File _cache;

    public ResolveTest() throws Exception {
        _ivy = new Ivy();
        _ivy.configure(new File("test/repositories/ivyconf.xml"));
    }

    protected void setUp() throws Exception {
        createCache();
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
    
    public void testResolveWithRetainingArtifactName() throws Exception {
    	_ivy.setCacheArtifactPattern(_ivy.substitute("[module]/[originalname].[ext]"));    	
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod15.2/ivy-1.1.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
        
        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default").getDownloadReports(ModuleRevisionId.newInstance("org15", "mod15.1", "1.1"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);
        
        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);
        
        String cachePath = _ivy.getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath, cachePath.endsWith("library.jar"));
        
        dReports = report.getConfigurationReport("default").getDownloadReports(ModuleRevisionId.newInstance("org14", "mod14.1", "1.1"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);
        
        artifact = dReports[0].getArtifact();
        assertNotNull(artifact);
        
        cachePath = _ivy.getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath, cachePath.endsWith("mod14.1-1.1.jar"));
    }
    
    public void testArtifactOrigin() throws Exception {
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);

        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default").getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);
        
        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);
        
        String expectedLocation = new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").getAbsolutePath();

        // verify the origin in the report
        ArtifactOrigin reportOrigin = dReports[0].getArtifactOrigin();
        assertNotNull(reportOrigin);
        assertEquals("isLocal for artifact not correct", true, reportOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation, reportOrigin.getLocation());
        
        // verify the saved origin on disk
        ArtifactOrigin ivyOrigin = _ivy.getSavedArtifactOrigin(_cache, artifact);
        assertNotNull(ivyOrigin);
        assertEquals("isLocal for artifact not correct", true, ivyOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation, ivyOrigin.getLocation());
        
        // now resolve the same artifact again and verify the origin of the (not-downloaded) artifact
        report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);

        dReports = report.getConfigurationReport("default").getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);
        assertEquals("download status not correct", DownloadStatus.NO, dReports[0].getDownloadStatus());
        reportOrigin = dReports[0].getArtifactOrigin();
        assertNotNull(reportOrigin);
        assertEquals("isLocal for artifact not correct", true, reportOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation, reportOrigin.getLocation());
    }

    public void testUseOrigin() throws Exception {
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true, false, true, true, null);
        assertNotNull(report);

        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default").getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct.", 1, dReports.length);
        assertEquals("download status not correct: should not download the artifact in useOrigin mode.", DownloadStatus.NO, dReports[0].getDownloadStatus());
        
        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);
        
        String expectedLocation = new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").getAbsolutePath();

        ArtifactOrigin origin = _ivy.getSavedArtifactOrigin(_cache, artifact);
        File artInCache = new File(_cache, _ivy.getArchivePathInCache(artifact, origin));
        assertFalse("should not download artifact in useOrigin mode.", artInCache.exists());
        assertEquals("location for artifact not correct.", expectedLocation, _ivy.getArchiveFileInCache(_cache, artifact).getAbsolutePath());
    }

    public void testResolveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveBadStatus() throws Exception {
        // mod1.4 depends on modfailure, modfailure has a bad status
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveNoRevisionInPattern() throws Exception {
        // module1 depends on latest version of module2, for which there is no revision in the pattern
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/norev/ivyconf.xml").toURL());
        ResolveReport report = ivy.resolve(new File("test/repositories/norev/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());
    }

    public void testResolveNoRevisionInDep() throws Exception {
        // mod1.4 depends on mod1.6, in which the ivy file has no revision
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.2.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveNoRevisionNowhere() throws Exception {
        // test case for IVY-258
        // module1 depends on latest version of module2, which contains no revision in its ivy file, nor in the pattern
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-258/ivyconf.xml").toURL());
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-258/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        ((BasicResolver)ivy.getResolver("myresolver")).setCheckconsistency(false);
        report = ivy.resolve(new File("test/repositories/IVY-258/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
    }

    public void testResolveRequiresIvyFile() throws Exception {
        // mod1.1 depends on mod1.2, mod1.2 has no ivy file
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ((FileSystemResolver)ivy.getResolver("1")).setAllownomd(false);
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveOtherConfiguration() throws Exception {
        ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-other.xml"), null, new String[] {"test"}, _cache, null, true);
        
        assertNotNull(report);
        assertFalse(report.hasError());
        
        assertEquals("Number of artifacts not correct", 1, report.getConfigurationReport("test").getArtifactsNumber());
    }

    public void testResolveWithSlashes() throws Exception {
        // test case for IVY-198
        // module depends on mod1.2
        ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-198.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg/mydep", "system/module", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("yourorg/yourdep", "yoursys/yourmod", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "yourorg/yourdep", "yoursys/yourmod", "1.0", "yourmod", "jar", "jar").exists());
    }

    public void testFromCache() throws Exception {
        // mod1.1 depends on mod1.2
        
        // we first do a simple resolve so that module is in cache
        _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);

        // we now use a badly configured ivy, so that it can't find module in repository
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());

        ModuleDescriptor md = report.getModuleDescriptor();

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testFromCache2() throws Exception {
        // mod1.1 depends on mod1.2

        // configuration
        Ivy ivy = new Ivy();
        DualResolver resolver = new DualResolver();
        resolver.setName("dual");
        FileSystemResolver r = new FileSystemResolver();
        r.setName("1");
        r.addArtifactPattern("build/testCache2/[artifact]-[revision].[ext]");
        resolver.add(r);
        r = new FileSystemResolver();
        r.setName("2");
        r.addArtifactPattern("build/testCache2/[artifact]-[revision].[ext]");
        resolver.add(r);
        ivy.addResolver(resolver);
        ivy.setDefaultResolver("dual");
        
        // set up repository
        File art = new File("build/testCache2/mod1.2-2.0.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), art, null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());

        // now we clean the repository to simulate repo not available (network pb for instance)
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/testCache2"));
        del.execute();
        
        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());

        ModuleDescriptor md = report.getModuleDescriptor();

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testFromCacheOnly() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivyconf.xml"));
        
//        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
//                null, new String[] {"*"}, _cache, null, true);
//        // should have an error, the conf is bad and the dependency should not be found
//        assertTrue(report.hasError());

        // put necessary stuff in cache, and it should now be ok
        File ivyfile = ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        File art = ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar");
        FileUtil.copy(ResolveTest.class.getResource("ivy-mod1.2.xml"), ivyfile, null);
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), art, null);

        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
    }

    public void testChangeCacheLayout() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ivy.setCacheIvyPattern("[module]/ivy.xml");
        ivy.setCacheArtifactPattern("[artifact].[ext]");

        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(new File(_cache, "mod1.2/ivy.xml").exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(new File(_cache, "mod1.2.jar").exists());
    }

    public void testResolveExtends() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"extension"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies from default
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExtended() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies from default
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExtendedAndExtends() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"default", "extension"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("extension");
        assertNotNull(crr);
        assertEquals(1, crr.getArtifactsNumber());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtends() throws Exception {
        // mod6.2  has two confs default and extension
        //    mod6.2 depends on mod6.1 in conf (default->extension)
        //   conf extension extends default
        // mod6.1 has two confs default and extension
        //   mod6.1 depends on mod1.2 2.0 in conf (default->default)
        //   conf extension extends default
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.2/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"default", "extension"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("extension");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org6", "mod6.1", "0.4")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtendsAndConfs() throws Exception {
        // Test case for IVY-240
        //
        // mod6.3 1.1 has four confs libraries, run (extends libraries), compile (extends run) and test (extends libraries)
        //    mod6.3 depends on mod6.2 2.0 in conf (run->default)
        //    mod6.3 depends on mod6.1 2.+ in conf (test->default)
        // mod6.2 2.0 depends on mod6.1 2.0 in conf (default->standalone)
        // mod6.1 2.0 has two confs default and standalone
        //   mod6.1 2.0 depends on mod1.2 2.2 in conf (default->default)
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ConfigurationResolveReport crr = report.getConfigurationReport("libraries");
        assertEquals(0, crr.getArtifactsNumber());
        
        crr = report.getConfigurationReport("run");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.2", "2.0", "mod6.2", "jar", "jar", crr);
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);
        
        crr = report.getConfigurationReport("compile");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.2", "2.0", "mod6.2", "jar", "jar", crr);
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);
        
        crr = report.getConfigurationReport("test");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);
        assertContainsArtifact("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar", crr);
    }

    public void testResolveMultipleConfsWithLatest() throws Exception {
        // Test case for IVY-188
        //
        // mod6.2  has two confs compile and run
        //    depends on mod6.1     in conf (compile->default)
        //    depends on mod1.2 latest (which is 2.2) in conf (run->default)
        // mod6.1 
        //    depends on mod1.2 2.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.2/ivys/ivy-0.6.xml").toURL(),
                null, new String[] {"compile", "run"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());

        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("run");
        assertNotNull(crr);
        assertEquals(1, crr.getArtifactsNumber());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleConfsWithConflicts() throws Exception {
        // Test case for IVY-173
        //
        // mod6.2  has two confs compile and run
        //    depends on mod1.2 2.1 in conf (compile->default)
        //    depends on mod1.1 1.0 in conf (*->default)
        //    depends on mod6.1     in conf (*->default)
        // mod6.1 
        //    depends on mod1.2 2.1
        // mod1.1 
        //    depends on mod1.2 2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.2/ivys/ivy-0.5.xml").toURL(),
                null, new String[] {"compile", "run"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(3, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("run");
        assertNotNull(crr);
        assertEquals(3, crr.getArtifactsNumber());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org6", "mod6.1", "0.5")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org6", "mod6.1", "0.5", "mod6.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtends2() throws Exception {
        // same as before, except that mod6.2 depends on mod1.2 2.1 extension->default
        // so mod1.2 2.0 should be evicted in conf extension
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml").toURL(),
                null, new String[] {"default", "extension"}, _cache, null, true);
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        IvyNode node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(node);
        assertFalse(node.isEvicted("default"));
        crr = report.getConfigurationReport("extension");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(node);
        assertTrue(node.isEvicted("extension"));
        node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"));
        assertNotNull(node);
        assertFalse(node.isEvicted("extension"));
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org6", "mod6.1", "0.4")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org6", "mod6.1", "0.4", "mod6.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveSeveralDefaultWithArtifacts() throws Exception {
    	// test case for IVY-261
    	// mod1.6 depends on
    	//   mod1.4, which depends on mod1.3 and selects one of its artifacts
    	//   mod1.3 and selects two of its artifacts
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.6/ivys/ivy-1.0.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar").exists());
    }

    public void testResolveSeveralDefaultWithArtifactsAndConfs() throws Exception {
    	// test case for IVY-283
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-283/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-283/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("build");
        assertNotNull(crr);
        assertEquals(3, crr.getDownloadReports(ModuleRevisionId.newInstance("medicel", "C", "1.0")).length);

        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "C", "1.0", "lib_c_a", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "C", "1.0", "lib_c_b", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "C", "1.0", "lib_c_d", "jar", "jar").exists());
    }
    
    public void testResolveSeveralDefaultWithArtifactsAndConfs2() throws Exception {
    	// second test case for IVY-283
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-283/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-283/ivy-d.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("build");
        assertNotNull(crr);
        assertEquals(9, crr.getDownloadReports(ModuleRevisionId.newInstance("medicel", "module_a", "local")).length);

        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_a", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_b", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_c", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_d", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_e", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_f", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_g", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_h", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "medicel", "module_a", "local", "lib_a_i", "jar", "jar").exists());
    }
    


    public void testResolveDefaultWithArtifactsConf1() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml").toURL(),
                null, new String[] {"myconf1"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.3", "3.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }
    
    public void testResolveDefaultWithArtifactsConf2() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml").toURL(),
                null, new String[] {"myconf2"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.3", "3.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }
    
    public void testResolveWithDependencyArtifactsConf1() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts in myconf1
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.4.xml").toURL(),
                null, new String[] {"myconf1"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithDependencyArtifactsConf2() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts in myconf1
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.4.xml").toURL(),
                null, new String[] {"myconf2"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithDependencyArtifactsWithoutConf() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.5.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithExcludesArtifacts() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.6.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithExcludesArtifacts2() throws Exception {
        // mod2.3 depends on mod2.1 and badly excludes artifacts with incorrect matcher
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.6.2.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.2");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithExcludesArtifacts3() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with exact matcher
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.6.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithExcludesArtifacts4() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with regexp matcher
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.6.4.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.4");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveWithExcludesArtifacts5() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with glob matcher
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.3/ivys/ivy-0.6.5.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.5");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveDependencies() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveDisabled() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true, false, false, null);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testDependenciesOrder() throws Exception {
        ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-225.xml"),
                null, new String[] {"default"}, _cache, null, true);
        
        Set revisions = report.getConfigurationReport("default").getModuleRevisionIds();
        assertTrue("number of revisions is not correct", revisions.size() >= 3);
        
        // verify the first 3 modules against the ones in the ivy file
        Iterator it = revisions.iterator();
        ModuleRevisionId revId1 = (ModuleRevisionId) it.next();
        assertEquals("mod1.2", revId1.getName());
        assertEquals("1.1", revId1.getRevision());
        
        ModuleRevisionId revId2 = (ModuleRevisionId) it.next();
        assertEquals("mod3.2", revId2.getName());
        assertEquals("1.4", revId2.getRevision());
        
        ModuleRevisionId revId3 = (ModuleRevisionId) it.next();
        assertEquals("mod5.1", revId3.getName());
        assertEquals("4.2", revId3.getRevision());
    }
    
    public void testDisableTransitivityPerConfiguration() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive
        
        // first we resolve compile conf only
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml").toURL(),
                null, new String[] {"compile"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml").toURL(),
                null, new String[] {"runtime"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call         
        ResolveReport r = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml").toURL(),
                null, new String[] {"runtime", "compile"}, _cache, null, true);
    	assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }
    
    public void testDisableTransitivityPerConfiguration2() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive
    	// compile extends runtime 
        
        // first we resolve compile conf only
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml").toURL(),
                null, new String[] {"compile"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml").toURL(),
                null, new String[] {"runtime"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call         
        ResolveReport r = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml").toURL(),
                null, new String[] {"runtime", "compile"}, _cache, null, true);
    	assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }
    
    public void testDisableTransitivityPerConfiguration3() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive
    	// runtime extends compile 
        
        // first we resolve compile conf only
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml").toURL(),
                null, new String[] {"compile"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml").toURL(),
                null, new String[] {"runtime"}, _cache, null, true);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call         
        ResolveReport r = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml").toURL(),
                null, new String[] {"compile", "runtime"}, _cache, null, true);
    	assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }
    
    public void testDisableTransitivityPerConfiguration4() throws Exception {
    	// mod2.2 (A,B,compile) depends on mod 2.1 (A->runtime;B->compile)
    	// compile is not transitive and extends A and B
    	//
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive and extends runtime 
        
    	ResolveReport r = _ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.6.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
    	assertFalse(r.hasError());

    	// here we should get all three recursive dependencies
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        		ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
        		ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"),
        })), r.getConfigurationReport("A").getModuleRevisionIds());

        // here we should get only mod2.1 and mod1.1 cause compile is not transitive in mod2.1
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        		ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
        })), r.getConfigurationReport("B").getModuleRevisionIds());
        
        // here we should get only mod2.1 cause compile is not transitive
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        })), r.getConfigurationReport("compile").getModuleRevisionIds());
    }
    
    public void testDisableTransitivityPerConfiguration5() throws Exception {
    	// mod2.2 (A,B,compile) depends on 
    	//		mod 2.1 (A->runtime;B->compile)
    	//		mod1.1 (A->*) ]0.9.9,1.0] (which resolves to 1.0)
    	// compile is not transitive and extends A and B
    	//
        // mod2.1 (compile, runtime) depends on mod1.1 1.0 which depends on mod1.2 2.0
        // compile conf is not transitive and extends runtime 
        
    	ResolveReport r = _ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.7.xml").toURL(),
                null, new String[] {"A","B","compile"}, _cache, null, true);
    	assertFalse(r.hasError());
    	
    	// here we should get all three recursive dependencies
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        		ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
        		ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"),
        })), r.getConfigurationReport("A").getModuleRevisionIds());

        // here we should get only mod2.1 and mod1.1 cause compile is not transitive in mod2.1
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        		ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
        })), r.getConfigurationReport("B").getModuleRevisionIds());
        
        // here we should get only mod2.1 cause compile is not transitive
        assertEquals(new HashSet(Arrays.asList(new ModuleRevisionId[] {
        		ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
        		ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
        })), r.getConfigurationReport("compile").getModuleRevisionIds());
    }
    
    public void testResolveDiamond() throws Exception {
        // mod4.1 depends on 
        //   - mod1.1 which depends on mod1.2
        //   - mod3.1 which depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.0", "mod3.1", "jar", "jar").exists());
    }

    public void testResolveConflict() throws Exception {
        // mod4.1 v 4.1 depends on 
        //   - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.1");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0, crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).length);
        assertEquals(1, crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).length);
        
        File r = new File(_cache, XmlReportOutputter.getReportFileName(mrid.getModuleId(), "default"));
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes attributes) throws SAXException {
                if ("revision".equals(qName) && "2.0".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]); // the report should contain the evicted revision

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveConflict2() throws Exception {
        // mod4.1 v 4.14 depends on 
        //   - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        //   - mod6.1 v 0.3 which depends on mod1.2 v 2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.14.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0, crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).length);
        assertEquals(1, crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).length);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.14");
        File r = new File(_cache, XmlReportOutputter.getReportFileName(mrid.getModuleId(), "default"));
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes attributes) throws SAXException {
                if ("revision".equals(qName) && "2.0".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]); // the report should contain the evicted revision

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveConflict3() throws Exception {
        // test case for IVY-264
        // a depends on x latest, y latest, z latest
    	// x and z depends on commons-lang 1.0.1
    	// y depends on commons-lang 2.0
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-264/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-264/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0, crr.getDownloadReports(ModuleRevisionId.newInstance("myorg", "commons-lang", "1.0.1")).length);
        assertEquals(1, crr.getDownloadReports(ModuleRevisionId.newInstance("myorg", "commons-lang", "2.0")).length);

        assertFalse(_ivy.getArchiveFileInCache(_cache, "myorg", "commons-lang", "1.0.1", "commons-lang", "jar", "jar").exists());

        assertTrue(_ivy.getArchiveFileInCache(_cache, "myorg", "commons-lang", "2.0", "commons-lang", "jar", "jar").exists());
    }

    public void testTransitiveEviction() throws Exception {
        // mod7.3 depends on mod7.2 v1.0 and on mod7.1 v2.0
        //      mod7.2 v1.0 depends on mod7.1 v1.0 (which then should be evicted)
        //      mod7.1 v1.0 depends on mod 1.2 v1.0 (which should be evicted by transitivity)

        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod7.3/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org7", "mod7.3", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org7", "mod7.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org7", "mod7.2", "1.0", "mod7.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org7", "mod7.1", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org7", "mod7.1", "2.0", "mod7.1", "jar", "jar").exists());

        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org7", "mod7.1", "1.0", "mod7.1", "jar", "jar").exists());

        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testTransitiveEviction2() throws Exception {
        // IVY-199
        // mod4.1 v 4.13 depends on 
        //   - mod3.2 v 1.2.1 which depends on 
        //         - mod3.1 v 1.0 which depends on mod1.2 v 2.0
        //         - mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.13.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveConflictInConf() throws Exception {
        // conflicts in separate confs are not conflicts
        
        // mod2.1 conf A depends on mod1.1 which depends on mod1.2 2.0
        // mod2.1 conf B depends on mod1.2 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.4.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testEvictWithConf() throws Exception {
        // bug 105 - test #1
        
        // mod6.1 r1.0 depends on 
        //       mod5.1 r4.2 conf A 
        //       mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //
        //       mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // should have been evicted before download
        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testEvictWithConf2() throws Exception {
        // same as preceding one but with inverse order, so that
        // eviction is done after download
        // bug 105 - test #2
        
        // mod6.1 r1.1 depends on 
        //       mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //       mod5.1 r4.2 conf A 
        //
        //       mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.1");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // even late eviction should avoid artifact downloading
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testEvictWithConfInMultiConf() throws Exception {
        // same as preceding ones but the conflict appears in several root confs
        // bug 105 - test #3
        
        // mod6.1 r1.2 conf A and conf B depends on 
        //       mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //       mod5.1 r4.2 conf A 
        //
        //       mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.2.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.2");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // all artifacts should be present in both confs
        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(2, crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);

        crr = report.getConfigurationReport("B");
        assertNotNull(crr);
        assertEquals(2, crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);
        
        // even late eviction should avoid artifact downloading
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testEvictWithConfInMultiConf2() throws Exception {
        // same as preceding one but the conflict appears in a root conf and not in another
        // which should keep the evicted
        // bug 105 - test #4
        
        // mod6.1 r1.3 conf A depends on
        //       mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //
        // mod6.1 r1.3 conf B depends on
        //       mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //       mod5.1 r4.2 conf A 
        //
        //       mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // 4.2 artifacts should be present in conf B only
        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(0, crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);

        crr = report.getConfigurationReport("B");
        assertNotNull(crr);
        assertEquals(2, crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);
    }
    
    public void testResolveForce() throws Exception {
        // mod4.1 v 4.2 depends on 
        //   - mod1.2 v 2.0 and forces it
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.2.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.2");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceAfterConflictSolved() throws Exception {
        // IVY-193
        // mod4.1 v 4.9 depends on 
        //   - mod3.2 v 1.1 which depends on mod1.2 v 2.0
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        //   - mod1.2 v 2.0 and forces it
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.9.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.9");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceAfterDependencyExist() throws Exception {
        // IVY-193
        // mod4.1 v 4.10 depends on 
        //   - mod3.1 v 1.0.1 which depends on mod1.2 v 2.0 and forces it
        //   - mod3.2 v 1.2 which depends on mod1.2 v 2.1 and on mod3.1 v1.0.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.10.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        
        // dependencies
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceInDepOnly() throws Exception {
        // IVY-193
        // mod4.1 v 4.11 depends on 
        //   - mod1.2 v 2.0
        //   - mod3.2 v 1.3 which depends on 
        //          - mod3.1 v1.1 which depends on
        //                  - mod1.2 v 2.1
        //          - mod1.2 v 1.0 and forces it 
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.11.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "1.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceInDepOnly2() throws Exception {
        // IVY-193
        // mod4.1 v 4.12 depends on 
        //   - mod3.1 v1.0 which depends on
        //          - mod1.2 v 2.0
        //   - mod3.2 v 1.4 which depends on 
        //          - mod1.2 v 2.0 and forces it 
        //          - mod3.1 v1.1 which depends on
        //                  - mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.12.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceWithDynamicRevisions() throws Exception {
        // mod4.1 v 4.5 depends on 
        //   - mod1.2 v 1+ and forces it
        //   - mod3.1 v 1.2 which depends on mod1.2 v 2+
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.5.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.5");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.2", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceWithDynamicRevisionsAndSeveralConfs() throws Exception {
        // mod4.1 v 4.6 (conf compile, test extends compile) depends on 
        //   - mod1.2 v 1+ and forces it in conf compile
        //   - mod3.1 v 1.2 in conf test which depends on mod1.2 v 2+
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.6.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.6");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.2", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceWithDynamicRevisionsAndSeveralConfs2() throws Exception {
        // mod4.1 v 4.7 (conf compile, test extends compile) depends on 
        //   - mod1.2 v 1+ and forces it in conf compile
        //   - mod3.1 v 1.3 in conf test->runtime 
        //           which defines confs compile, runtime extends compile
        //           which depends on mod1.2 v 2+ in conf compile->default
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.7.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.7");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.3", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveForceWithDynamicRevisionsAndCyclicDependencies() throws Exception {
        // IVY-182
        //   * has no revision 
        //   * declares conf compile, test extends compile, 
        //   * depends on 
        //     - mod1.2 v 1+ and forces it in conf compile
        //     - mod3.1 v 1+ in conf test->runtime excluding mod4.1 (to avoid cyclic dep failure)
        //           which defines confs compile, runtime extends compile
        //           which depends on mod1.2 v 2+ in conf compile->default
        //           which depends on mod4.1 v 4+ in conf compile->compile
        ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-182.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleId mid = new ModuleId("test", "IVY-182");
        assertEquals(mid, md.getModuleRevisionId().getModuleId());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.4")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.4", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveContradictoryConflictResolution() throws Exception {
        // mod10.1 v 1.0 depends on 
        //   - mod1.2 v 2.0 and forces it 
        //   - mod4.1 v 4.1 (which selects mod1.2 v 2.1 and evicts mod1.2 v 2.0)
        // mod4.1 v 4.1 depends on 
        //   - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org10", "mod10.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // conflicting dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveContradictoryConflictResolution2() throws Exception {
        // BUG IVY-130 : only mod1.2 v2.0 should be resolved and not v2.1 (because of force)
        // mod10.1 v 1.1 depends on 
        //   - mod1.2 v 2.0 and forces it 
        //   - mod4.1 v 4.3
        // mod4.1 v 4.3 depends on 
        //   - mod1.2 v 2.1
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        _ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        // conflicting dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveContradictoryConflictResolution3() throws Exception {
        // mod 1.2 v2.0 should be selected (despite conflict manager in 4.1, because of force in 10.1)
        // mod10.1 v 1.3 depends on 
        //   - mod1.2 v 2.0 and forces it
        //   - mod4.1 v 4.4
        // mod4.1 v 4.4 depends on 
        //   - mod1.2 v 2.0 but selects mod1.2 v 2.1
        //   - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        IvyNode[] evicted = report.getConfigurationReport("default").getEvictedNodes();
        assertEquals(1, evicted.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"), evicted[0].getResolvedId());
    }
    
    public void testExtends() throws Exception {
        // mod 5.2 depends on mod5.1 conf B
        // mod5.1 conf B publishes art51B
        // mod5.1 conf B extends conf A
        // mod5.1 conf A publishes art51A
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod5.2/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org5", "mod5.2", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
    }
    
    public void testMultiConfs() throws Exception {
        // mod 5.2 depends on mod5.1 conf B in its conf B and conf A in its conf A
        // mod5.1 conf B publishes art51B
        // mod5.1 conf A publishes art51A
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod5.2/ivy-2.0.xml").toURL(),
                null, new String[] {"B", "A"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org5", "mod5.2", "2.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org5", "mod5.1", "4.1");

        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);
        
        File r = new File(_cache, XmlReportOutputter.getReportFileName(mrid.getModuleId(), "A"));
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes attributes) throws SAXException {
                if ("artifact".equals(qName) && "art51B".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertFalse(found[0]);

        assertTrue(_ivy.getIvyFileInCache(_cache, depId).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.1", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.1", "art51B", "jar", "jar").exists());
    }
    
    public void testThisConfiguration() throws Exception {
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod14.4/ivy-1.1.xml").toURL(),
                null, new String[] {"compile"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org14", "mod14.4", "1.1");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(4, crr.getArtifactsNumber());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.3", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.2", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.1", "1.1")).exists());
        assertTrue(!_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.3", "1.0")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.1", "1.0")).exists());
        
        cleanCache();
        createCache();
        report = _ivy.resolve(new File("test/repositories/2/mod14.4/ivy-1.1.xml").toURL(),
                null, new String[] {"standalone"}, _cache, null, true);
        crr = report.getConfigurationReport("standalone");
        assertNotNull(crr);
        assertEquals(7, crr.getArtifactsNumber());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.3", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.1", "1.1")).exists());
        assertTrue(!_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.2", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org14", "mod14.3", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.3", "1.0")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.1", "1.0")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.4", "1.1")).exists());
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org8", "mod8.2", "1.1")).exists());
    }
    
    public void testLatest() throws Exception {
        // mod1.4 depends on latest mod1.2
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.1.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.1");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "2.2");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);
        
        File r = new File(_cache, XmlReportOutputter.getReportFileName(mrid.getModuleId(), "default"));
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            public void startElement(String uri,String localName,String qName,org.xml.sax.Attributes attributes) throws SAXException {
                if ("artifact".equals(qName) && "mod1.2".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]);
        
        assertTrue(ivy.getIvyFileInCache(_cache, depId).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testLatestMultiple() throws Exception {
        // mod1.5 depends on 
    	//    latest mod1.4, which depends on mod1.2 2.2
    	//    latest mod1.2 (which is 2.2)
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.5/ivys/ivy-1.0.2.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
                
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.4", "2.0")).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    
    public void testVersionRange1() throws Exception {
    	// mod 1.4 depends on mod1.2 [1.0,2.0[ 
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "1.1");
        
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);
        
        assertTrue(ivy.getIvyFileInCache(_cache, depId).exists());
    }
    
    public void testVersionRange2() throws Exception {
    	// mod 1.4 depends on mod1.2 [1.5,2.0[ 
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.3.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertTrue(report.hasError());
    }
    
    public void testLatestMilestone() throws Exception {
    	// mod9.2 depends on latest.milestone of mod6.4
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org9/mod9.2/ivys/ivy-1.1.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
        
        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org6", "mod6.4", "3");
        
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);
        
        assertTrue(ivy.getIvyFileInCache(_cache, depId).exists());
    }
    
    public void testLatestMilestone2() throws Exception {
    	// mod9.2 depends on latest.milestone of mod6.2, but there is no milestone
    	// test case for IVY-318
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org9/mod9.2/ivys/ivy-1.2.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        // we should have an error since there is no milestone version, it should be considered as a non resolved dependency
        assertTrue(report.hasError());
        
        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0, crr.getArtifactsNumber());
    }
    
    public void testIVY56() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-56.xml"),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
    }
        
    public void testIVY214() throws Exception {
    	ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-214.xml"), null, new String[] {"compile"}, _cache, null, true);
    	
    	assertNotNull(report);
    	assertFalse(report.hasError());
    	
    	assertEquals("Number of artifacts not correct", 1, report.getConfigurationReport("compile").getArtifactsNumber());
    }

    public void testIVY218() throws Exception {
    	ResolveReport report = _ivy.resolve(ResolveTest.class.getResource("ivy-218.xml"), null, new String[] {"test"}, _cache, null, true);
    	
    	assertNotNull(report);
    	assertFalse(report.hasError());
    	
    	assertEquals("Number of artifacts not correct", 3, report.getConfigurationReport("test").getArtifactsNumber());
    }

    public void testCircular() throws Exception {
        // mod6.3 depends on mod6.2, which itself depends on mod6.3
    	
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
        
        _ivy.setCircularDependencyStrategy(IgnoreCircularDependencyStrategy.getInstance());
        report = _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
        
        _ivy.setCircularDependencyStrategy(WarnCircularDependencyStrategy.getInstance());
        report = _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml").toURL(),
        		null, new String[] {"default"}, _cache, null, true);
        assertFalse(report.hasError());
        
        _ivy.setCircularDependencyStrategy(ErrorCircularDependencyStrategy.getInstance());
        try {
	        _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml").toURL(),
	                null, new String[] {"default"}, _cache, null, true);
	        fail("no exception with circular dependency strategy set to error");
        } catch (CircularDependencyException ex)  {
        	assertEquals("[ org6 | mod6.3 | 1.0 ]->[ org6 | mod6.2 | 1.0 ]->[ org6 | mod6.3 | latest.integration ]", ex.getMessage());
        }
    }
    
    public void testCircular2() throws Exception {
        // mod 9.1 (no revision) depends on mod9.2, which depends on mod9.1 2.+
    	
        ResolveReport report = _ivy.resolve(new File("test/repositories/circular/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        _ivy.setCircularDependencyStrategy(ErrorCircularDependencyStrategy.getInstance());
        try {
	        _ivy.resolve(new File("test/repositories/circular/ivy.xml").toURL(),
	                null, new String[] {"*"}, _cache, null, true);
	        fail("no exception with circular dependency strategy set to error");
        } catch (CircularDependencyException ex)  {
        	// ok
        	assertEquals("[ org8 | mod8.5 | NONE ]->[ org8 | mod8.6 | 2.+ ]->[ org8 | mod8.5 | 2.+ ]", ex.getMessage());
        }
    }
    
    public void testRegularCircular() throws Exception {
        // mod11.1 depends on mod11.2 but excludes itself
        // mod11.2 depends on mod11.1
    	_ivy.setCircularDependencyStrategy(ErrorCircularDependencyStrategy.getInstance());
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod11.1/ivy-1.0.xml").toURL(),
                null, new String[] {"test"}, _cache, null, true);
        
        assertNotNull(report);
        assertFalse(report.hasError());
            
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org11", "mod11.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org11", "mod11.2", "1.0", "mod11.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org11", "mod11.1", "1.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org11", "mod11.1", "1.0", "mod11.1", "jar", "jar").exists());
    }
    
    public void testResolveDualChain() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ResolveTest.class.getResource("dualchainresolverconf.xml"));
        
        DependencyResolver resolver = ivy.getResolver("default");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        
        // first without cache
        ivy.resolve(ResolveTest.class.getResource("ivy-dualchainresolver.xml"), null, new String[] {"default"}, new File("build/cache"), null, true);
        
        assertTrue(new File("build/cache/xerces/xerces/ivy-2.6.2.xml").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());

        // second with cache for ivy file only
        new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").delete();
        new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").delete();
        assertFalse(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertFalse(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());
        ivy.resolve(ResolveTest.class.getResource("ivy-dualchainresolver.xml"), null, new String[] {"default"}, new File("build/cache"), null, true);
        
        assertTrue(new File("build/cache/xerces/xerces/ivy-2.6.2.xml").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());
    }

    
    public void testBug148() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bug148/ivyconf.xml"));
        
        ivy.resolve(ResolveTest.class.getResource("ivy-148.xml"), null, new String[] {"*"}, new File("build/cache"), null, true);
        
        assertTrue(new File("build/cache/jtv-foo/bar/ivy-1.1.0.0.xml").exists());
        assertTrue(new File("build/cache/jtv-foo/bar/jars/bar-1.1.0.0.jar").exists());
        assertTrue(new File("build/cache/idautomation/barcode/ivy-4.10.xml").exists());
        assertTrue(new File("build/cache/idautomation/barcode/jars/LinearBarCode-4.10.jar").exists());        
    }
    
    public void testBug148b() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bug148/ivyconf.xml"));
        
        ivy.resolve(ResolveTest.class.getResource("ivy-148b.xml"), null, new String[] {"*"}, new File("build/cache"), null, true);
        
        assertTrue(new File("build/cache/jtv-foo/bar/ivy-1.1.0.0.xml").exists());
        assertTrue(new File("build/cache/jtv-foo/bar/jars/bar-1.1.0.0.jar").exists());
        assertTrue(new File("build/cache/idautomation/barcode/ivy-4.10.xml").exists());
        assertTrue(new File("build/cache/idautomation/barcode/jars/LinearBarCode-4.10.jar").exists());        
    }

    public void testBadFiles() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/badfile/ivyconf.xml"));
        
        try {
            ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badorg.xml").toURL(), null, new String[] {"*"}, new File("build/cache"), null, true);
            fail("bad org should have raised an exception !");
        } catch (Exception ex) {
            // OK, it raised an exception
        }
        try {
            ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badmodule.xml").toURL(), null, new String[] {"*"}, new File("build/cache"), null, true);
            fail("bad module should have raised an exception !");
        } catch (Exception ex) {
            // OK, it raised an exception
        }
        try {
            ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badrevision.xml").toURL(), null, new String[] {"*"}, new File("build/cache"), null, true);
            fail("bad revision should have raised an exception !");
        } catch (Exception ex) {
            // OK, it raised an exception
        }
    }
    
    public void testTransitiveSetting() throws Exception {
        // mod2.4 depends on mod1.1 with transitive set to false
        //     mod1.1 depends on mod1.2, which should not be resolved because of the transitive setting
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.4/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.4", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(!_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(!_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolverDirectlyUsingCache() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ResolveTest.class.getResource("badcacheconf.xml"));
        File depIvyFileInCache = ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"));
        FileUtil.copy(File.createTempFile("test", "xml"), depIvyFileInCache, null); // creates a fake dependency file in cache
        ResolveReport report = ivy.resolve(new File("test/repositories/1/org2/mod2.4/ivys/ivy-0.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.4", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(depIvyFileInCache.exists());
        assertTrue(!ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
    }
    
    public void testVisibility1() throws Exception {
        _ivy.resolve(new File("test/repositories/2/mod8.2/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.0", "a-private", "txt", "txt").exists());
    }
    
    public void testVisibility2() throws Exception {
        _ivy.resolve(new File("test/repositories/2/mod8.3/ivy-1.0.xml").toURL(),
                null, new String[] {"private"}, _cache, null, true);
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.0", "a-private", "txt", "txt").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.0", "a", "txt", "txt").exists());
    }
    
    public void testVisibility3() throws Exception {
        _ivy.resolve(new File("test/repositories/2/mod8.4/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.0", "a-private", "txt", "txt").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.0", "a", "txt", "txt").exists());
    }
    
    public void testVisibility4() throws Exception {
        _ivy.resolve(new File("test/repositories/2/mod8.4/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.1", "a-private", "txt", "txt").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org8", "mod8.1", "1.1", "a", "txt", "txt").exists());
    }
    
    ///////////////////////////////////////////////////////////
    // here comes a series of test provided by Chris Rudd
    // about configuration mapping and eviction
    ///////////////////////////////////////////////////////////
    
    public void testConfigurationMapping1() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/1/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.2", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.2", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.2", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.2", "c-bt", "txt", "txt", conf);        
    }

    public void testConfigurationMapping2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/2/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.1", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.1", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);        
    }

    public void testConfigurationMapping3() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/3/ivy.xml").toURL(),
                null, new String[] {"buildtime"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("buildtime");
        
        assertContainsArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);        
    }

    public void testConfigurationMapping4() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/4/ivy.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);        
    }

    public void testConfigurationMapping5() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/5/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);        
    }

    public void testConfigurationMapping6() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/6/ivy.xml").toURL(),
                null, new String[] {"default", "buildtime"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);        
    }

    public void testConfigurationMapping7() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/7/ivy.xml").toURL(),
                null, new String[] {"buildtime", "default"}, _cache, null, true);
        
        ConfigurationResolveReport conf = report.getConfigurationReport("default");
        
        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);        
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);        
    }

    public void testIVY97() throws Exception {
        // mod9.2 depends on mod9.1 and mod1.2
        //     mod9.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org9/mod9.2/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org9", "mod9.2", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org9", "mod9.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org9", "mod9.1", "1.0", "mod9.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveExcludesSimple() throws Exception {
        // mod2.5 depends on mod2.3 and excludes one artifact from mod2.1
        //      mod2.3 depends on mod2.1
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.5/ivys/ivy-0.6.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.5", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.3", "0.7")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.3", "0.7", "mod2.3", "jar", "jar").exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org2", "mod2.1", "0.3")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveExcludesDiamond1() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5
        //      mod2.3 depends on mod2.1 and excludes art21B
        //      mod2.5 depends on mod2.1 and excludes art21A
        _ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.6.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveExcludesDiamond2() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5
        //      mod2.3 depends on mod2.1 and excludes art21B
        //      mod2.5 depends on mod2.1 and excludes art21B
        _ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.7.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveExcludesDiamond3() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5 and on mod2.1 for which it excludes art21A
        //      mod2.3 depends on mod2.1 and excludes art21B
        //      mod2.5 depends on mod2.1 and excludes art21B
        _ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.8.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }
    
    public void testResolveTransitiveExcludes2() throws Exception {
        // mod2.6 depends on mod2.3 for which it excludes art21A
        //      mod2.3 depends on mod2.1 and excludes art21B
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.9.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.9"), md.getModuleRevisionId());
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }
    
    public void testResolveExcludesModule() throws Exception {
        // mod2.6 depends on mod2.1 and excludes mod1.1
        //      mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.10.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.10"), md.getModuleRevisionId());
        
        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }
    
    public void testResolveExceptConfiguration() throws Exception {
        // mod10.2 depends on mod5.1 conf *, !A
        _ivy.resolve(new File("test/repositories/2/mod10.2/ivy-2.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.1", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.1", "art51B", "jar", "jar").exists());
    }
    
    public void testResolveFallbackConfiguration() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(default)
        _ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
    }
    
    public void testResolveFallbackConfiguration2() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(*)
        _ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.1.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testResolveFallbackConfiguration3() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(*),compile(*)
        _ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.2.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testResolveFallbackConfiguration4() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime()
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.3.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertFalse(report.hasError());
        
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }
    
    public void testResolveMaven2() throws Exception {
        // test3 depends on test2 which depends on test
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/m2/fr/jayasoft/test3/1.0/test3-1.0.pom").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("fr.jayasoft", "test3", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("fr.jayasoft", "test2", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "fr.jayasoft", "test2", "1.0", "test2", "jar", "jar").exists());

        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("fr.jayasoft", "test", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "fr.jayasoft", "test", "1.0", "test", "jar", "jar").exists());
    }
    
    public void testNamespaceMapping() throws Exception {
        // the dependency is in another namespace
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivyconf.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("jayasoft", "namespace", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("systemorg", "systemmod", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "systemorg", "systemmod", "1.0", "A", "jar", "jar").exists());
    }
    
    public void testNamespaceMapping2() throws Exception {
        // the dependency is in another namespace and has itself a dependency on a module available in the same namespace
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivyconf.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace2.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("jayasoft", "namespace", "2.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("systemorg", "systemmod2", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "systemorg", "systemmod2", "1.0", "B", "jar", "jar").exists());

        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("systemorg", "systemmod", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "systemorg", "systemmod", "1.0", "A", "jar", "jar").exists());
    }
    
    public void testNamespaceMapping3() throws Exception {
        // same as 2 but with poms
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivyconf.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace3.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("jayasoft", "namespace", "3.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(ivy.getResolvedIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("systemorg2", "system-2", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "systemorg2", "system-2", "1.0", "2", "jar", "jar").exists());

        assertTrue(ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("systemorg2", "system-1", "1.0")).exists());
        assertTrue(ivy.getArchiveFileInCache(_cache, "systemorg2", "system-1", "1.0", "1", "jar", "jar").exists());
    }
    
    public void testNamespaceMapping4() throws Exception {
        // same as 2 but with incorrect dependency asked: the first ivy file asks for a dependency 
        // in the resolver namespace and not the system one: this should fail
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivyconf.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace4.xml"),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("jayasoft", "namespace", "4.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(report.hasError());
    }
    
    public void testIVY151() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/multirevisions/ivyconf.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/multirevisions/ivy.xml").toURL(), null, new String[] {"compile", "test"}, _cache, null, true);

        assertNotNull(report);
        assertNotNull(report.getUnresolvedDependencies());
        assertEquals("Number of unresolved dependencies not correct", 0, report.getUnresolvedDependencies().length);
    }
    
    public void testCheckRevision() throws Exception {
        // mod12.2 depends on mod12.1 1.0 which depends on mod1.2
        // mod12.1 doesn't have revision in its ivy file
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod12.2/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertTrue(report.hasError());
        
        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org12", "mod12.1", "1.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org12", "mod12.1", "1.0", "mod12.1", "jar", "jar").exists());        
        
        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());        
    }

    public void testTrustRevision() throws Exception {
        // mod12.2 depends on mod12.1 1.0 which depends on mod1.2
        // mod12.1 doesn't have revision in its ivy file
        
        ((BasicResolver)_ivy.getResolver("2-ivy")).setCheckconsistency(false);
        
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod12.2/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org12", "mod12.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org12", "mod12.1", "1.0", "mod12.1", "jar", "jar").exists());        
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());        
    }

    public void testTransitiveConfMapping() throws Exception {
        // IVY-168
        // mod13.3 depends on mod13.2 which depends on mod13.1
        // each module has two confs: j2ee and compile
        // each module only publishes one artifact in conf compile
        // each module has the following conf mapping on its dependencies: *->@
        // moreover, mod13.1 depends on mod1.2 in with the following conf mapping: compile->default
        // thus conf j2ee should be empty for each modules
        
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod13.3/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        
        assertFalse(report.hasError());
        
        assertEquals(3, report.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(0, report.getConfigurationReport("j2ee").getArtifactsNumber());
    }

    public void testExtraAttributes() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/extra-attributes/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att.xml"),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(new File(_cache, "jayasoft/mymodule/task1/1854/ivy.xml").exists());
        assertTrue(new File(_cache, "jayasoft/mymodule/task1/1854/mymodule-windows.jar").exists());
        assertTrue(new File(_cache, "jayasoft/mymodule/task1/1854/mymodule-linux.jar").exists());
    }

    public void testBranches1() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/1/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "foo", "foo1", "3", "foo1", "jar", "jar").exists());        
    }

    public void testBranches2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/2/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "foo", "foo1", "4", "foo1", "jar", "jar").exists());        
    }

    public void testBranches3() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivyconf-defaultbranch1.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/1/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "foo", "foo1", "4", "foo1", "jar", "jar").exists());        
    }

    public void testBranches4() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/3/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "foo", "foo1", "3", "foo1", "jar", "jar").exists());        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "bar", "bar2", "2", "bar2", "jar", "jar").exists());        
    }

    public void testBranches5() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivyconf-fooonbranch1.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/3/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "foo", "foo1", "4", "foo1", "jar", "jar").exists());        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "bar", "bar2", "2", "bar2", "jar", "jar").exists());        
    }

    public void testExternalArtifacts() throws Exception {
        Ivy ivy = new Ivy();
        ivy.setVariable("test.base.url", new File("test/repositories/external-artifacts").toURL().toString());
        ivy.configure(new File("test/repositories/external-artifacts/ivyconf.xml"));
        
        ResolveReport report = ivy.resolve(new File("test/repositories/external-artifacts/ivy.xml").toURL(),
                null, new String[] {"*"}, _cache, null, false);
        assertFalse(report.hasError());
        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "jayasoft", "A", "1.0", "a", "jar", "jar").exists());        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "jayasoft", "B", "2.0", "b", "jar", "jar").exists());        
        assertTrue(_ivy.getArchiveFileInCache(_cache, "jayasoft", "C", "3.0", "C", "jar", "jar").exists());        
    }


    ////////////////////////////////////////////////////////////
    // helper methods to ease the tests
    ////////////////////////////////////////////////////////////
    
    private void assertContainsArtifact(String org, String module, String rev, String artName, String type, String ext, ConfigurationResolveReport conf) {
        Artifact art = getArtifact(org, module, rev, artName, type, ext);
        if (!containsArtifact(art, conf.getDownloadedArtifactsReports())) {
            fail("artifact "+art+" should be part of "+conf.getConfiguration()+" from "+conf.getModuleDescriptor().getModuleRevisionId());
        }        
    }
    
    private void assertDoesntContainArtifact(String org, String module, String rev, String artName, String type, String ext, ConfigurationResolveReport conf) {
        Artifact art = getArtifact(org, module, rev, artName, type, ext);
        if (containsArtifact(art, conf.getDownloadedArtifactsReports())) {
            fail("artifact "+art+" should NOT be part of "+conf.getConfiguration()+" from "+conf.getModuleDescriptor().getModuleRevisionId());
        }        
    }

    private Artifact getArtifact(String org, String module, String rev, String artName, String type, String ext) {
         return new DefaultArtifact(ModuleRevisionId.newInstance(org, module, rev), new Date(), artName, type, ext);
    }

    private boolean containsArtifact(Artifact art, ArtifactDownloadReport[] adr) {
        for (int i = 0; i < adr.length; i++) {
            Artifact artifact = adr[i].getArtifact();
            if (artifact.getModuleRevisionId().equals(art.getModuleRevisionId())
                    && artifact.getName().equals(art.getName())
                    && artifact.getType().equals(art.getType())
                    && artifact.getExt().equals(art.getExt())) {
                return true;
            }
        }
        return false;
    }

}
