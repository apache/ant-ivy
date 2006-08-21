/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DefaultDependencyArtifactDescriptor;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.matcher.ExactPatternMatcher;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;

/**
 * 
 */
public class IBiblioResolverTest extends TestCase {
	// remote.test

	private File _cache;
    private ResolveData _data;
    private Ivy _ivy;
    
    protected void setUp() throws Exception {
        _cache = new File("build/cache");
        _ivy = new Ivy();
        _data = new ResolveData(_ivy, _cache, null, null, true);
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    
    public void testDefaults() {
        IBiblioResolver resolver = new IBiblioResolver();
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.ibiblio.default.artifact.root", "http://www.ibiblio.org/mymaven/");
        ivy.setVariable("ivy.ibiblio.default.artifact.pattern", "[module]/jars/[artifact]-[revision].jar");
        resolver.setIvy(ivy);
        List l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/jars/[artifact]-[revision].jar", l.get(0));
    }

    public void testInitFromConf() throws Exception {
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.ibiblio.default.artifact.root", "http://www.ibiblio.org/maven/");
        ivy.setVariable("ivy.ibiblio.default.artifact.pattern", "[module]/jars/[artifact]-[revision].jar");
        ivy.setVariable("my.ibiblio.root", "http://www.ibiblio.org/mymaven/");
        ivy.setVariable("my.ibiblio.pattern", "[module]/[artifact]-[revision].jar");
        ivy.configure(IBiblioResolverTest.class.getResource("ibiblioresolverconf.xml"));
        IBiblioResolver resolver = (IBiblioResolver)ivy.getResolver("ibiblioA");
        assertNotNull(resolver);
        List l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/[artifact]-[revision].jar", l.get(0));
        
        resolver = (IBiblioResolver)ivy.getResolver("ibiblioB");
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[organisation]/jars/[artifact]-[revision].jar", l.get(0));

        resolver = (IBiblioResolver)ivy.getResolver("ibiblioC");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/maven2/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]", l.get(0));

        resolver = (IBiblioResolver)ivy.getResolver("ibiblioD");
        assertFalse(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/maven/[module]/jars/[artifact]-[revision].jar", l.get(0));
}

    public void testIBiblio() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setIvy(_ivy);
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "commons-fileupload", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "commons-fileupload", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, _data.getIvy(), _cache);
        assertNotNull(report);
        
        assertEquals(1, report.getArtifactsReports().length);
        
        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);
        
        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, _data.getIvy(), _cache);
        assertNotNull(report);
        
        assertEquals(1, report.getArtifactsReports().length);
        
        ar = report.getArtifactReport(artifact);
        assertNotNull(ar);
        
        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testIBiblioArtifacts() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setIvy(_ivy);
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "nanning", "0.9");
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mrid, false);
        dd.addDependencyArtifactIncludes("default", new DefaultDependencyArtifactDescriptor(dd, "nanning-profiler", "jar", "jar", true, ExactPatternMatcher.getInstance()));
        dd.addDependencyArtifactIncludes("default", new DefaultDependencyArtifactDescriptor(dd, "nanning-trace", "jar", "jar", true, ExactPatternMatcher.getInstance()));
        ResolvedModuleRevision rmr = resolver.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact profiler = new DefaultArtifact(mrid, rmr.getPublicationDate(), "nanning-profiler", "jar", "jar");
        DefaultArtifact trace = new DefaultArtifact(mrid, rmr.getPublicationDate(), "nanning-trace", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {profiler, trace}, _data.getIvy(), _cache);
        assertNotNull(report);
        
        assertEquals(2, report.getArtifactsReports().length);
        
        ArtifactDownloadReport ar = report.getArtifactReport(profiler);
        assertNotNull(ar);
        
        assertEquals(profiler, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        ar = report.getArtifactReport(trace);
        assertNotNull(ar);
        
        assertEquals(trace, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {profiler, trace}, _data.getIvy(), _cache);
        assertNotNull(report);
        
        assertEquals(2, report.getArtifactsReports().length);
        
        ar = report.getArtifactReport(profiler);
        assertNotNull(ar);
        
        assertEquals(profiler, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());

        ar = report.getArtifactReport(trace);
        assertNotNull(ar);
        
        assertEquals(trace, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testUnknown() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setIvy(_ivy);

        assertNull(resolver.getDependency(new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("unknown", "unknown", "1.0"), false), _data));
    }

}
