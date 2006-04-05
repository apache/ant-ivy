/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.util.Arrays;
import java.util.GregorianCalendar;

import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.xml.XmlIvyConfigurationParser;
import junit.framework.TestCase;

/**
 * Test for DualResolver
 */
public class DualResolverTest extends TestCase {
    private ResolveData _data = new ResolveData(new Ivy(), null, null, null, true);

    public void testFromConf() throws Exception {
        Ivy ivy = new Ivy();
        new XmlIvyConfigurationParser(ivy).parse(DualResolverTest.class.getResource("dualresolverconf.xml"));
        
        DependencyResolver resolver = ivy.getResolver("dualok");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        DualResolver dual = (DualResolver)resolver;
        assertNotNull(dual.getIvyResolver());
        assertEquals("ivy", dual.getIvyResolver().getName());
        assertNotNull(dual.getArtifactResolver());
        assertEquals("artifact", dual.getArtifactResolver().getName());

        resolver = ivy.getResolver("dualnotenough");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);
        dual = (DualResolver)resolver;
        assertNotNull(dual.getIvyResolver());
        assertNull(dual.getArtifactResolver());
    }

    public void testFromBadConf() throws Exception {
        Ivy ivy = new Ivy();
        try {
            new XmlIvyConfigurationParser(ivy).parse(DualResolverTest.class.getResource("dualresolverconf-bad.xml"));
            fail("bad dual resolver configuration should raise exception");
        } catch (Exception ex) {
            // ok -> bad conf has raised an exception
        }
    }

    public void testBad() throws Exception {
        DualResolver dual = new DualResolver();
        dual.setIvyResolver(new IBiblioResolver());
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        try {
            dual.getDependency(dd, _data);
            fail("bad dual resolver configuration should raise exception");
        } catch (Exception ex) {
            // ok -> should have raised an exception
        }
    }

    public void testResolve() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver("ivy", true, new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver("artifact", false, new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);
        
        assertNotNull(rmr);
        assertEquals(dual, rmr.getArtifactResolver());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertTrue(artifactResolver.askedDeps.isEmpty());
    }

    public void testResolveFromArtifact() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver("ivy", false, new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver("artifact", true, new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);
        
        assertNotNull(rmr);
        assertEquals(artifactResolver, rmr.getResolver());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), artifactResolver.askedDeps);
    }

    public void testResolveFail() throws Exception {
        DualResolver dual = new DualResolver();
        MockResolver ivyResolver = MockResolver.buildMockResolver("ivy", false, new GregorianCalendar(2005, 1, 20).getTime());
        MockResolver artifactResolver = MockResolver.buildMockResolver("artifact", false, new GregorianCalendar(2005, 1, 20).getTime());
        dual.setIvyResolver(ivyResolver);
        dual.setArtifactResolver(artifactResolver);
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        ResolvedModuleRevision rmr = dual.getDependency(dd, _data);
        
        assertNull(rmr);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), ivyResolver.askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), artifactResolver.askedDeps);
    }
}
