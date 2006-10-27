/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import fr.jayasoft.ivy.latest.LatestTimeStrategy;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;
import fr.jayasoft.ivy.report.ReportOutputter;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.resolver.ChainResolver;
import fr.jayasoft.ivy.resolver.FileSystemResolver;
import fr.jayasoft.ivy.resolver.IvyRepResolver;
import fr.jayasoft.ivy.resolver.MockResolver;
import fr.jayasoft.ivy.url.URLHandler;
import fr.jayasoft.ivy.url.URLHandlerDispatcher;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.version.ChainVersionMatcher;
import fr.jayasoft.ivy.version.MockVersionMatcher;
import fr.jayasoft.ivy.version.VersionMatcher;

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
        
        LatestStrategy latest = ivy.getLatestStrategy("mylatest-revision");
        assertNotNull(latest);
        assertTrue(latest instanceof LatestRevisionStrategy);
        LatestRevisionStrategy l = (LatestRevisionStrategy)latest;
        assertEquals(new Integer(-2), l.getSpecialMeanings().get("pre"));
        assertEquals(new Integer(4), l.getSpecialMeanings().get("qa"));
        
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
        assertEquals(1, fsInt1.getIvyPatterns().size());
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", fsInt1.getIvyPatterns().get(0));
        assertEquals("int2", ((DependencyResolver)subresolvers.get(1)).getName());
        
        strategy = fsInt1.getLatestStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof LatestTimeStrategy);

        assertEquals("libraries", ivy.getResolver(new ModuleId("unknown", "lib")).getName());
        assertEquals("internal", ivy.getResolver(new ModuleId("jayasoft", "swtbinding")).getName());
        assertEquals("int1", ivy.getResolver(new ModuleId("jayasoft", "ivy")).getName());
        assertEquals("int1", ivy.getResolver(new ModuleId("jayasoft", "ivycruise")).getName());        
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
    
    public void testStatuses() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-status.xml"));
        
        assertEquals("bronze", ivy.getStatusManager().getDefaultStatus());
        assertEquals(0, ivy.getStatusManager().getPriority("gold"));
        assertEquals(1, ivy.getStatusManager().getPriority("silver"));
        assertEquals(2, ivy.getStatusManager().getPriority("bronze"));
        assertEquals(false, ivy.getStatusManager().isIntegration("gold"));
        assertEquals(false, ivy.getStatusManager().isIntegration("silver"));
        assertEquals(true, ivy.getStatusManager().isIntegration("bronze"));
    }
    
    public void testConflictManager() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-conflict-manager.xml"));
        
        assertEquals("latest-time", ivy.getConflictManager(new ModuleId("jayasoft", "ivyde")).getName());
        assertEquals("all", ivy.getConflictManager(new ModuleId("jayasoft", "uistate")).getName());
    }
    
    public void testVersionMatchers1() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-vmatcher1.xml"));
        
        VersionMatcher mock = ivy.getVersionMatcher("vmock");
        assertNotNull(mock);
        assertTrue(mock instanceof MockVersionMatcher);
        
        VersionMatcher v = ivy.getVersionMatcher();
        assertTrue(v instanceof ChainVersionMatcher);
        ChainVersionMatcher chain = (ChainVersionMatcher) v;
        assertEquals(3, chain.getMatchers().size());
        assertTrue(chain.getMatchers().contains(mock));
        assertTrue(chain.getMatchers().contains(ivy.getVersionMatcher("exact")));
        assertTrue(chain.getMatchers().contains(ivy.getVersionMatcher("latest")));
    }
    
    public void testVersionMatchers2() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-vmatcher2.xml"));
        
        VersionMatcher mock = ivy.getVersionMatcher("vmock");
        assertNotNull(mock);
        assertTrue(mock instanceof MockVersionMatcher);
        
        VersionMatcher v = ivy.getVersionMatcher();
        assertTrue(v instanceof ChainVersionMatcher);
        ChainVersionMatcher chain = (ChainVersionMatcher) v;
        assertEquals(5, chain.getMatchers().size());
        assertTrue(chain.getMatchers().contains(mock));
    }
    
    public void testRef() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-ref.xml"));
        
        DependencyResolver internal = ivy.getResolver("internal");
        assertNotNull(internal);
        assertTrue(internal instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)internal;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("fs", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns.get(0));

        DependencyResolver external = ivy.getResolver("external");
        assertNotNull(external);
        assertTrue(external instanceof ChainResolver);
        chain = (ChainResolver)external;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        FileSystemResolver fsInt2 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("fs", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("sharedrep/[organisation]/[module]/ivys/ivy-[revision].xml", ivyPatterns.get(0));
    }
    
    public void testMacro() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-macro.xml"));
        
        DependencyResolver def = ivy.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)def;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]", ivyPatterns.get(0));

        FileSystemResolver fsInt2 = (FileSystemResolver)subresolvers.get(1);
        assertEquals("default-fs2", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/secondrep/[organisation]/[module]/[type]s/ivy-[revision].xml", ivyPatterns.get(0));
        
        DependencyResolver other = ivy.getResolver("other");
        assertNotNull(other);
        assertTrue(other instanceof ChainResolver);
        chain = (ChainResolver)other;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        fsInt2 = (FileSystemResolver)subresolvers.get(1);
        assertEquals("other-fs2", fsInt2.getName());

        ivyPatterns = fsInt2.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/secondrep/[module]/[type]s/ivy-[revision].xml", ivyPatterns.get(0));
    }
    
    public void testMacroAndRef() throws Exception {
    	// test case for IVY-319
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-macro+ref.xml"));
        
        DependencyResolver shared = ivy.getResolver("shared");
        assertNotNull(shared);
        assertTrue(shared instanceof FileSystemResolver);
        
        DependencyResolver mychain = ivy.getResolver("mychain");
        assertNotNull(mychain);
        assertTrue(mychain instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)mychain;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(1, subresolvers.size());
        shared = (DependencyResolver) subresolvers.get(0);
        assertEquals("shared", shared.getName());
        assertTrue(shared instanceof FileSystemResolver);
    }
    
    public void testInclude() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-include.xml"));
        
        DependencyResolver def = ivy.getResolver("default");
        assertNotNull(def);
        assertTrue(def instanceof ChainResolver);
        ChainResolver chain = (ChainResolver)def;
        List subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());
        FileSystemResolver fsInt1 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("default-fs1", fsInt1.getName());

        List ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("path/to/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]", ivyPatterns.get(0));
        
        DependencyResolver inc = ivy.getResolver("includeworks");
        assertNotNull(inc);
        assertTrue(inc instanceof ChainResolver);
        chain = (ChainResolver)inc;
        subresolvers = chain.getResolvers();
        assertNotNull(subresolvers);
        assertEquals(2, subresolvers.size());

        fsInt1 = (FileSystemResolver)subresolvers.get(0);
        assertEquals("includeworks-fs1", fsInt1.getName());

        ivyPatterns = fsInt1.getIvyPatterns();
        assertNotNull(ivyPatterns);
        assertEquals(1, ivyPatterns.size());
        assertEquals("included/myrep/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]", ivyPatterns.get(0));
    }
    
    public void testParser() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-parser.xml"));
        assertEquals("fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistryTest$MyParser", ModuleDescriptorParserRegistry.getInstance().getParsers()[0].getClass().getName());
    }
    
    public void testOutputter() throws Exception {
        Ivy ivy = new Ivy();
        XmlIvyConfigurationParser parser = new XmlIvyConfigurationParser(ivy);
        parser.parse(XmlIvyConfigurationParserTest.class.getResource("ivyconf-outputter.xml"));
        
        //System.out.println(Arrays.asList(ivy.getReportOutputters()));
        
        ReportOutputter testOutputter = ivy.getReportOutputter("test");
        assertNotNull(testOutputter);
        assertTrue(testOutputter instanceof MyOutputter);
    }
    
    private void configureURLHandler() {
        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }
    
    public static class MyOutputter implements ReportOutputter {

       public void output(ResolveReport report, File destDir) {
       }

       public String getName() {
           return "test";
       }
       
    }
}
