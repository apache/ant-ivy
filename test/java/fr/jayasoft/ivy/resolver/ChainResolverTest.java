/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import fr.jayasoft.ivy.latest.LatestTimeStrategy;
import fr.jayasoft.ivy.xml.XmlIvyConfigurationParser;

/**
 * Tests ChainResolver
 */
public class ChainResolverTest extends TestCase {
    private Ivy _ivy;
    private ResolveData _data;
    private File _cache;
    
    protected void setUp() throws Exception {
        _ivy = new Ivy();
        _cache = new File("build/cache");
        _data = new ResolveData(_ivy, _cache, null, null, true);
        _cache.mkdirs();
        _ivy.setDefaultCache(_cache);
    }
    
    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    
    public void testOrderFromConf() throws Exception {
        new XmlIvyConfigurationParser(_data.getIvy()).parse(ChainResolverTest.class.getResource("chainresolverconf.xml"));
        DependencyResolver resolver = _data.getIvy().getResolver("chain");
        assertNotNull(resolver);
        assertTrue(resolver instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)resolver;
        assertResolversSizeAndNames(chain, 3);
    }

    private void assertResolversSizeAndNames(ChainResolver chain, int size) {
        List resolvers = chain.getResolvers();
        assertEquals(size, resolvers.size());
        for (int i=0; i < resolvers.size(); i++) {
            DependencyResolver r = (DependencyResolver)resolvers.get(i);
            assertEquals(String.valueOf(i+1), r.getName());
        }
    }
    
    public void testName() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setIvy(_ivy);
        chain.setName("chain");
        assertEquals("chain", chain.getName());
    }
    
    public void testResolveOrder() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null), 
                MockResolver.buildMockResolver("2", true, null), 
                MockResolver.buildMockResolver("3", true, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), resolvers[0].askedDeps);
        assertEquals(Arrays.asList(new DependencyDescriptor[] {dd}), resolvers[1].askedDeps);
        assertTrue(resolvers[2].askedDeps.isEmpty());
    }
    
    public void testLatestTimeResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setLatestStrategy(new LatestTimeStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, new GregorianCalendar(2005, 1, 20).getTime()), 
                MockResolver.buildMockResolver("2", false, null), 
                MockResolver.buildMockResolver("3", true, new GregorianCalendar(2005, 1, 25).getTime()), // younger -> should the one kept 
                MockResolver.buildMockResolver("4", false, null), 
                MockResolver.buildMockResolver("5", true, new GregorianCalendar(2005, 1, 22).getTime()),
                MockResolver.buildMockResolver("6", true, new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("3", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }
    
    public void testLatestRevisionResolve() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, ModuleRevisionId.newInstance("org", "mod", "1"), new GregorianCalendar(2005, 1, 20).getTime()), 
                MockResolver.buildMockResolver("2", false, null), 
                MockResolver.buildMockResolver("3", true, ModuleRevisionId.newInstance("org", "mod", "2"), new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver("4", false, null), 
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest -> should the one kept 
                MockResolver.buildMockResolver("6", true, ModuleRevisionId.newInstance("org", "mod", "3"), new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }
    
    public void testWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null), 
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // latest -> but default 
                MockResolver.buildMockResolver("3", false, null),
                MockResolver.buildMockResolver("4", false, null), 
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest -> should the one kept 
                MockResolver.buildMockResolver("6", false, null),
                MockResolver.buildMockResolver("7", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < 5; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
        for (int i = 5; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }
    
    public void testLatestWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, ModuleRevisionId.newInstance("org", "mod", "1"), new GregorianCalendar(2005, 1, 20).getTime()), 
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // latest -> but default 
                MockResolver.buildMockResolver("3", true, ModuleRevisionId.newInstance("org", "mod", "2"), new GregorianCalendar(2005, 1, 25).getTime()),
                MockResolver.buildMockResolver("4", false, null), 
                MockResolver.buildMockResolver("5", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // latest -> should the one kept 
                MockResolver.buildMockResolver("6", true, ModuleRevisionId.newInstance("org", "mod", "3"), new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("5", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < resolvers.length; i++) {
            assertEquals(ddAsList, resolvers[i].askedDeps);
        }
    }
    
    public void testFixedWithDefault() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setLatestStrategy(new LatestRevisionStrategy());
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null), 
                MockResolver.buildMockResolver("2", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime(), true), // default 
                MockResolver.buildMockResolver("3", false, null), 
                MockResolver.buildMockResolver("4", true, ModuleRevisionId.newInstance("org", "mod", "4"), new GregorianCalendar(2005, 1, 22).getTime()), // not default -> should the one kept 
                MockResolver.buildMockResolver("5", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "4"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("4", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 0; i < 4; i++) {
            assertEquals("invalid asked dependencies for "+resolvers[i], ddAsList, resolvers[i].askedDeps);
        }
        for (int i = 4; i < resolvers.length; i++) {
            assertTrue("invalid asked dependencies for "+resolvers[i], resolvers[i].askedDeps.isEmpty());
        }
    }
    
    public void testFixedWithDefaultAndRealResolver() throws Exception {
        // test case for IVY-206
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        
        // no ivy pattern for first resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setIvy(_ivy);
        
        resolver.addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);
        
        // second resolver has an ivy pattern and will thus find the real module, which should be kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setIvy(_ivy);
        
        resolver.addIvyPattern("test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);
        
        _ivy.addResolver(chain);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org1","mod1.1", "1.0"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
    }
    
    public void testReturnFirst() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setReturnFirst(true);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", true, new GregorianCalendar(2005, 1, 20).getTime()), 
                MockResolver.buildMockResolver("2", false, null), 
                MockResolver.buildMockResolver("3", true, new GregorianCalendar(2005, 1, 25).getTime()), // younger -> should the one kept 
                MockResolver.buildMockResolver("4", false, null), 
                MockResolver.buildMockResolver("5", true, new GregorianCalendar(2005, 1, 22).getTime()),
                MockResolver.buildMockResolver("6", true, new GregorianCalendar(2005, 1, 18).getTime()),
                MockResolver.buildMockResolver("7", false, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "latest.integration"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("1", rmr.getResolver().getName());
        List ddAsList = Arrays.asList(new DependencyDescriptor[] {dd});
        for (int i = 1; i < resolvers.length; i++) {
            assertTrue(resolvers[i].askedDeps.isEmpty());
        }
    }
    
    public void testReturnFirstWithDefaultAndCacheAndRealResolver() throws Exception {
        // test case for IVY-207
        
        // 1 ---- we first do a first resolve which puts a default file in cache
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        
        // no ivy pattern for resolver: will only find a 'default' module
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("old");
        resolver.setIvy(_ivy);
        
        resolver.addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);
                
        _ivy.addResolver(chain);
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org1","mod1.1", "1.0"), false);
        chain.getDependency(dd, _data);
        
        // 2 ---- now we ask to resolve dependencies with a chain in return first mode, in which the first resolver 
        //        is not able to find the module, but the second is 
        
        chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setReturnFirst(true);
        
        // no pattern for first resolver: will not find the module
        resolver = new FileSystemResolver();
        resolver.setName("1");
        resolver.setIvy(_ivy);
        
        chain.add(resolver);
        
        // second resolver will find the real module, which should be kept
        resolver = new FileSystemResolver();
        resolver.setName("2");
        resolver.setIvy(_ivy);
        
        resolver.addIvyPattern("test/repositories/1/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern("test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        chain.add(resolver);
        
        _ivy.addResolver(chain);
        
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
    }
    
    public void testDual() throws Exception {
        ChainResolver chain = new ChainResolver();
        chain.setName("chain");
        chain.setIvy(_ivy);
        chain.setDual(true);
        MockResolver[] resolvers = new MockResolver[] {
                MockResolver.buildMockResolver("1", false, null), 
                MockResolver.buildMockResolver("2", true, null), 
                MockResolver.buildMockResolver("3", true, null)
            };
        for (int i = 0; i < resolvers.length; i++) {
            chain.add(resolvers[i]);
        }
        assertResolversSizeAndNames(chain, resolvers.length);
        
        
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org","mod", "rev"), false);
        ResolvedModuleRevision rmr = chain.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals("2", rmr.getResolver().getName());
        assertEquals("chain", rmr.getArtifactResolver().getName());
    }
        
}
