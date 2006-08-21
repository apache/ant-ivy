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
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;

/**
 * 
 */
public class IvyRepResolverTest extends TestCase {
	// remote.test
	
    private File _cache;
    private ResolveData _data;
    private Ivy _ivy;
    
    protected void setUp() throws Exception {
        _cache = new File("build/cache");
        _ivy = new Ivy();
        _data = new ResolveData(_ivy, _cache, null, null, false);
        _cache.mkdirs();
    }
    
    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    public void testDefaults() {
        IvyRepResolver resolver = new IvyRepResolver();
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.ivyrep.default.ivy.root", "http://www.jayasoft.fr/myivyrep/");
        ivy.setVariable("ivy.ivyrep.default.ivy.pattern", "[organisation]/[module]/ivy-[revision].[ext]");
        ivy.setVariable("ivy.ivyrep.default.artifact.root", "http://www.ibiblio.org/mymaven/");
        ivy.setVariable("ivy.ivyrep.default.artifact.pattern", "[module]/jars/[artifact]-[revision].jar");
        resolver.setIvy(ivy);
        List l = resolver.getIvyPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.jayasoft.fr/myivyrep/[organisation]/[module]/ivy-[revision].[ext]", l.get(0));
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/jars/[artifact]-[revision].jar", l.get(0));
    }

    public void testIvyRep() throws Exception {        
        IvyRepResolver resolver = new IvyRepResolver();
        resolver.setName("test");
        resolver.setIvy(_ivy);
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "commons-cli", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());
        assertEquals(2, rmr.getDescriptor().getDependencies().length);

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "commons-cli", "jar", "jar");
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
    
    /*
     * Tests IvyRepResolver with a root path given as 'file:/path_to_root'
     */
    public void testIvyRepLocalURL() throws Exception {
        IvyRepResolver resolver = new IvyRepResolver();
        String rootpath = new File("test/repositories/1").getAbsolutePath();

        resolver.setName("testLocal");
        resolver.setIvyroot("file:" + rootpath);
        resolver.setIvypattern("[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.setIvy(_ivy);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
    }
    
    public void testListing() {
        IvyRepResolver resolver = new IvyRepResolver();
        resolver.setName("test");
        resolver.setIvy(_ivy);
        
        OrganisationEntry[] orgs = resolver.listOrganisations();
        ResolverTestHelper.assertOrganisationEntriesContains(resolver, new String[] {"hibernate", "apache"}, orgs);
        
        OrganisationEntry org = ResolverTestHelper.getEntry(orgs, "apache");
        ModuleEntry[] mods = resolver.listModules(org);
        ResolverTestHelper.assertModuleEntriesContains(resolver, org, new String[] {"commons-logging", "commons-lang"}, mods);

        ModuleEntry mod = ResolverTestHelper.getEntry(mods, "commons-logging");
        RevisionEntry[] revs = resolver.listRevisions(mod);
        ResolverTestHelper.assertRevisionEntriesContains(resolver, mod, new String[] {"1.0", "1.0.2", "1.0.3", "1.0.4"}, revs);
    }
}
