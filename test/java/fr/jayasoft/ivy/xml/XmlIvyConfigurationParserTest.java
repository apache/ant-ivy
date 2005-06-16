/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import fr.jayasoft.ivy.latest.LatestTimeStrategy;
import fr.jayasoft.ivy.resolver.ChainResolver;
import fr.jayasoft.ivy.resolver.FileSystemResolver;
import fr.jayasoft.ivy.resolver.MockResolver;

/**
 * TODO write javadoc
 */
public class XmlIvyConfigurationParserTest extends TestCase {
    public void test() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-test.xml"));
        
        File defaultCache = ivy.getDefaultCache();
        assertNotNull(defaultCache);
        assertEquals("mycache", defaultCache.getName());
        
        assertFalse(ivy.isCheckUpToDate());
        assertFalse(ivy.doValidate());
        
        assertEquals("[module]/ivys/ivy-[revision].xml", ivy.getCacheIvyPattern());
        assertEquals("[module]/[type]s/[artifact]-[revision].[ext]", ivy.getCacheArtifactPattern());
        
        DependencyResolver defaultResolver = ivy.getDefaultResolver();
        assertNotNull(defaultResolver);
        assertEquals("libraries", defaultResolver.getName());
        assertTrue(defaultResolver instanceof FileSystemResolver);
        FileSystemResolver fsres = (FileSystemResolver)defaultResolver;
        List ivyPatterns = fsres.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("lib/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns.get(0));

        LatestStrategy strategy = fsres.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestRevisionStrategy);
        
        DependencyResolver internal = ivy.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)internal;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("int1", fsInt1.getName());
        assertEquals("int2", ((DependencyResolver)subresolvers.get(1)).getName());
        
        strategy = fsInt1.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestTimeStrategy);

        assertEquals("libraries", ivy.getResolver(new ModuleId("unknown", "lib")).getName());
        assertEquals("internal", ivy.getResolver(new ModuleId("jayasoft", "ivy")).getName());
        
    }

    public void testTypedef() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-typedef.xml"));
        
        DependencyResolver mock = ivy.getResolver("mock3");
        assertNotNull(mock);
        assertTrue(mock instanceof MockResolver);
        
        DependencyResolver internal = ivy.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)internal;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        
        assertEquals("mock1", ((DependencyResolver)subresolvers.get(0)).getName());
        assertEquals("mock2", ((DependencyResolver)subresolvers.get(1)).getName());
        assertTrue(subresolvers.get(0) instanceof MockResolver);
        assertTrue(subresolvers.get(1) instanceof MockResolver);
    }
}
