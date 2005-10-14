/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

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
    private Ivy _ivy = new Ivy();
    private ResolveData _data = new ResolveData(_ivy, null, null, null, true);
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
    
    public void testReturnFirst() throws Exception {
        ChainResolver chain = new ChainResolver();
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
    
}
