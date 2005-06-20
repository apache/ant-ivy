/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.jayasoft.ivy.report.ConfigurationResolveReport;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.report.XmlReportOutputter;
import fr.jayasoft.ivy.resolver.DualResolver;

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

    public void testResolveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").toURL(),
                null, new String[] {"*"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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
        
        assertTrue(ivy.getIvyFileInCache(_cache, mrid).exists());
        assertTrue(new File(_cache, "mod1.1/ivy.xml").exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.1", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org5", "mod5.2", "1.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
        // dependencies
        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org3", "mod3.1", "1.1")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertTrue(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(_ivy.getIvyFileInCache(_cache, ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).exists());
        assertFalse(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
    
    public void testLatest() throws Exception {
        // mod1.4 depends on latest mod1.2
        ResolveReport report = _ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.1.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.1");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertTrue(_ivy.getIvyFileInCache(_cache, mrid).exists());
        
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
        
        assertTrue(_ivy.getIvyFileInCache(_cache, depId).exists());
        assertTrue(_ivy.getArchiveFileInCache(_cache, "org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }
    
    public void testCircular() throws Exception {
        // mod6.3 depends on mod6.2, which itself depends on mod6.3 !
        ResolveReport report = _ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml").toURL(),
                null, new String[] {"default"}, _cache, null, true);
        assertNotNull(report);
    }
    
    public void testResolveDualChain() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ResolveTest.class.getResource("dualchainresolverconf.xml"));
        
        DependencyResolver resolver = ivy.getResolver("default");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        DualResolver dual = (DualResolver)resolver;
        
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
    
}
