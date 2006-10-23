/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

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
 * Tests URLResolver. Http tests are based upon ibiblio site.
 * 
 */
public class URLResolverTest extends TestCase {
	// remote.test
    private File _cache;
    private ResolveData _data;
    private Ivy _ivy = new Ivy();
    
    protected void setUp() throws Exception {
        _cache = new File("build/cache");
        _data = new ResolveData(_ivy, _cache, null, null, true);
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    
    public void testFile() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        String rootpath = new File("test/repositories/1").getAbsolutePath();
        resolver.addIvyPattern("file:"+rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("file:"+rootpath + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        
        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
        
        
        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
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

    public void testLatestFile() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        String rootpath = new File("test/repositories/1").getAbsolutePath().replaceAll("\\\\", "/");
        resolver.addIvyPattern("file:"+rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("file:"+rootpath + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), _data);
        assertNotNull(rmr);
        
        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 1, 15, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testIBiblio() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        resolver.addArtifactPattern(ibiblioRoot+"/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
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
        
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        resolver.addArtifactPattern(ibiblioRoot+"/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
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

    public void testLatestIBiblio() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        resolver.addArtifactPattern(ibiblioRoot+"/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("objectweb", "asm", "1.4+");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        assertEquals("1.4.3", rmr.getId().getRevision());
    }

    public void testUnknown() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        resolver.addIvyPattern(ibiblioRoot+"/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(ibiblioRoot+"/maven/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");

        assertNull(resolver.getDependency(new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("unknown", "unknown", "1.0"), false), _data));
    }

    public void testDownloadWithUseOriginIsTrue() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setIvy(_ivy);
        String rootpath = new File("test/repositories/1").getAbsolutePath();
        resolver.addIvyPattern("file:"+rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("file:"+rootpath + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        
        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
        
        
        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, _data.getIvy(), _cache, true);
        assertNotNull(report);
        
        assertEquals(1, report.getArtifactsReports().length);
        
        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);
        
        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
//        report = resolver.download(new Artifact[] {artifact}, _data.getIvy(), _cache);
//        assertNotNull(report);
//        
//        assertEquals(1, report.getArtifactsReports().length);
//        
//        ar = report.getArtifactReport(artifact);
//        assertNotNull(ar);
//        
//        assertEquals(artifact, ar.getArtifact());
//        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
//    	
    }
}
