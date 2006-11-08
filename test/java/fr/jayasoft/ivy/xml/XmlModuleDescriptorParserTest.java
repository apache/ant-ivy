/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.ConflictManager;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.License;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.Configuration.Visibility;
import fr.jayasoft.ivy.conflict.FixedConflictManager;
import fr.jayasoft.ivy.conflict.NoConflictManager;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.parser.AbstractModuleDescriptorParserTester;
import fr.jayasoft.ivy.util.XMLHelper;

/**
 * 
 */
public class XmlModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    // junit test -- DO NOT REMOVE used by ant to know it's a junit test
    
    private Ivy _ivy = new Ivy();
    public void testSimple() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-simple.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(null, md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {new Configuration("default")}), Arrays.asList(md.getConfigurations()));
        
        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);
        assertEquals("mymodule", md.getArtifacts("default")[0].getName());
        assertEquals("jar", md.getArtifacts("default")[0].getType());
        
        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }
    
    public void testEmptyDependencies() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-empty-dependencies.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {new Configuration("default")}), Arrays.asList(md.getConfigurations()));
        
        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);
        assertEquals("mymodule", md.getArtifacts("default")[0].getName());
        assertEquals("jar", md.getArtifacts("default")[0].getType());
        
        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }
    
    public void testBad() throws IOException {
        try {
            XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-bad.xml"), true);
            fail("bad ivy file raised no error");
        } catch (ParseException ex) {
            if (XMLHelper.canUseSchemaValidation()) {
                assertTrue("exception message not explicit. It should contain 'modul', but it's:"+ex.getMessage(), ex.getMessage().indexOf("'modul'") != -1);
            }
        }
    }

    public void testBadOrg() throws IOException {
        try {
            XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-bad-org.xml"), true);
            fail("bad ivy file raised no error");
        } catch (ParseException ex) {
            if (XMLHelper.canUseSchemaValidation()) {
                assertTrue("invalid exception: "+ex.getMessage(), ex.getMessage().indexOf("organization") != -1);
            }
        }
    }

    public void testBadConfs() throws IOException {
        try {
            XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-bad-confs.xml"), true);
            fail("bad ivy file raised no error");
        } catch (ParseException ex) {
            assertTrue("invalid exception: "+ex.getMessage(), ex.getMessage().indexOf("invalidConf") != -1);
        }
    }

    public void testNoValidate() throws IOException, ParseException {
        XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-novalidate.xml"), false);
    }

    public void testBadVersion() throws IOException {
        try {
            XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-bad-version.xml"), true);
            fail("bad version ivy file raised no error");
        } catch (ParseException ex) {
            // ok
        }
    }
    
    public void testFull() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus()); 
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());
        
        License[] licenses = md.getLicenses();
        assertEquals(1, licenses.length);
        assertEquals("MyLicense", licenses[0].getName());
        assertEquals("http://www.my.org/mymodule/mylicense.html", licenses[0].getUrl());
        
        assertEquals("http://www.my.org/mymodule/", md.getHomePage());
        
        Configuration[] confs = md.getConfigurations();
        assertNotNull(confs);
        assertEquals(5, confs.length);
        
        assertConf(md, "myconf1", "desc 1", Configuration.Visibility.PUBLIC, new String[0]);
        assertConf(md, "myconf2", "desc 2", Configuration.Visibility.PUBLIC, new String[0]);
        assertConf(md, "myconf3", "desc 3", Configuration.Visibility.PRIVATE, new String[0]);
        assertConf(md, "myconf4", "desc 4", Configuration.Visibility.PUBLIC, new String[] {"myconf1", "myconf2"});
        assertConf(md, "myoldconf", "my old desc", Configuration.Visibility.PUBLIC, new String[0]);
        
        assertArtifacts(md.getArtifacts("myconf1"), new String[] {"myartifact1", "myartifact2", "myartifact3", "myartifact4"});
        assertArtifacts(md.getArtifacts("myconf2"), new String[] {"myartifact1", "myartifact3"});
        assertArtifacts(md.getArtifacts("myconf3"), new String[] {"myartifact1", "myartifact3", "myartifact4"});
        assertArtifacts(md.getArtifacts("myconf4"), new String[] {"myartifact1"});
        
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(13, dependencies.length);
        
        // no conf def => equivalent to *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        assertFalse(dd.isChanging());
        assertTrue(dd.isTransitive());
        
        // changing = true
        dd = getDependency(dependencies, "mymodule3");
        assertNotNull(dd);
        assertTrue(dd.isChanging());
        assertFalse(dd.isTransitive());
        
        // conf="myconf1" => equivalent to myconf1->myconf1
        dd = getDependency(dependencies, "yourmodule1");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"myconf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"myconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        // conf="myconf1->yourconf1"
        dd = getDependency(dependencies, "yourmodule2");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("2+", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"myconf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"yourconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        // conf="myconf1->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule3");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("3.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"myconf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf2", "myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        // conf="myconf1, myconf2->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule4");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("4.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"myconf1", "myconf2"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf2")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        // conf="myconf1->yourconf1;myconf2->yourconf1, yourconf2"
        dd = getDependency(dependencies, "yourmodule5");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("5.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"myconf1", "myconf2"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"yourconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf2")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        // conf="*->@"
        dd = getDependency(dependencies, "yourmodule11");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("11.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"*"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"myconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"myconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf2")));        
        assertEquals(Arrays.asList(new String[] {"myconf3"}), Arrays.asList(dd.getDependencyConfigurations("myconf3")));        
        assertEquals(Arrays.asList(new String[] {"myconf4"}), Arrays.asList(dd.getDependencyConfigurations("myconf4")));        

        dd = getDependency(dependencies, "yourmodule6");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("latest.integration", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"myconf1", "myconf2"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"yourconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf2")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        dd = getDependency(dependencies, "yourmodule7");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("7.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"myconf1", "myconf2"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"yourconf1"}), Arrays.asList(dd.getDependencyConfigurations("myconf1")));        
        assertEquals(Arrays.asList(new String[] {"yourconf1", "yourconf2"}), Arrays.asList(dd.getDependencyConfigurations("myconf2")));        
        assertEquals(Arrays.asList(new String[] {}), Arrays.asList(dd.getDependencyConfigurations(new String[] {"myconf3", "myconf4"})));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1", "myconf2", "myconf3", "myconf4"}, new String[0]);
        
        dd = getDependency(dependencies, "yourmodule8");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("8.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"*"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1"}, new String[] {"yourartifact8-1", "yourartifact8-2"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf2"}, new String[] {"yourartifact8-1", "yourartifact8-2"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf3"}, new String[] {"yourartifact8-1", "yourartifact8-2"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf4"}, new String[] {"yourartifact8-1", "yourartifact8-2"});
        
        dd = getDependency(dependencies, "yourmodule9");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("9.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"myconf1", "myconf2", "myconf3"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1"}, new String[] {"yourartifact9-1"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf2"}, new String[] {"yourartifact9-1", "yourartifact9-2"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf3"}, new String[] {"yourartifact9-2"});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf4"}, new String[] {});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf1"}, new String[] {});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf2"}, new String[] {});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf3"}, new String[] {});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf4"}, new String[] {});
        
        dd = getDependency(dependencies, "yourmodule10");
        assertNotNull(dd);
        assertEquals("yourorg", dd.getDependencyId().getOrganisation());
        assertEquals("10.1", dd.getDependencyRevisionId().getRevision());
        assertEquals(new HashSet(Arrays.asList(new String[] {"*"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf1"}, new String[] {"your.*", PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf2"}, new String[] {"your.*", PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf3"}, new String[] {"your.*", PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactsIncludes(dd, new String[] {"myconf4"}, new String[] {"your.*", PatternMatcher.ANY_EXPRESSION});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf1"}, new String[] {"toexclude"});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf2"}, new String[] {"toexclude"});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf3"}, new String[] {"toexclude"});
        assertDependencyArtifactsExcludes(dd, new String[] {"myconf4"}, new String[] {"toexclude"});
                
        ConflictManager cm = md.getConflictManager(new ModuleId("yourorg", "yourmodule1"));
        assertNotNull(cm);
        assertTrue(cm instanceof NoConflictManager);
        
        cm = md.getConflictManager(new ModuleId("yourorg", "yourmodule2"));
        assertNotNull(cm);
        assertTrue(cm instanceof NoConflictManager);

        cm = md.getConflictManager(new ModuleId("theirorg", "theirmodule1"));
        assertNotNull(cm);
        assertTrue(cm instanceof FixedConflictManager);
        FixedConflictManager fcm = (FixedConflictManager)cm;
        assertEquals(2, fcm.getRevs().size());
        assertTrue(fcm.getRevs().contains("1.0"));
        assertTrue(fcm.getRevs().contains("1.1"));

        cm = md.getConflictManager(new ModuleId("theirorg", "theirmodule2"));
        assertNull(cm);
    }

    public void testBug60() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-bug60.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus()); 
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());
        
        assertEquals(Arrays.asList(new Configuration[] {new Configuration("default")}), Arrays.asList(md.getConfigurations()));
        
        assertArtifacts(md.getArtifacts("default"), new String[] {"myartifact1", "myartifact2"});
    }

    public void testNoArtifact() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-noartifact.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals(null, md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {new Configuration("default")}), Arrays.asList(md.getConfigurations()));
        
        assertNotNull(md.getArtifacts("default"));
        assertEquals(0, md.getArtifacts("default").length);
        
        assertNotNull(md.getDependencies());
        assertEquals(0, md.getDependencies().length);
    }
    
    public void testNoPublication() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-nopublication.xml"), true);
        assertNotNull(md);
        assertEquals("myorg", md.getModuleRevisionId().getOrganisation());
        assertEquals("mymodule", md.getModuleRevisionId().getName());
        assertEquals("myrev", md.getModuleRevisionId().getRevision());
        assertEquals("integration", md.getStatus()); 
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, md.getPublicationDate());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {new Configuration("default")}), Arrays.asList(md.getConfigurations()));
        
        assertNotNull(md.getArtifacts("default"));
        assertEquals(1, md.getArtifacts("default").length);
        
        assertNotNull(md.getDependencies());
        assertEquals(1, md.getDependencies().length);
    }
    
    public void testArtifactsDefaults() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-artifacts-defaults.xml"), true);
        assertNotNull(md);
        
        Artifact[] artifacts = md.getArtifacts("default");
        assertNotNull(artifacts);
        assertEquals(3, artifacts.length);
        assertArtifactEquals("mymodule", "jar", "jar", artifacts[0]);
        assertArtifactEquals("myartifact", "jar", "jar", artifacts[1]);
        assertArtifactEquals("mymodule", "dll", "dll", artifacts[2]);
    }
    
    private void assertArtifactEquals(String name, String type, String ext, Artifact artifact) {
        assertEquals(name+"/"+type+"/"+ext, artifact.getName()+"/"+artifact.getType()+"/"+artifact.getExt());        
    }

    public void testDefaultConf() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-defaultconf.xml"), true);
        assertNotNull(md);
        
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf: default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("default")));        

        // confs def: *->*
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("default")));        
    }
    
    public void testDefaultConf2() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-defaultconf2.xml"), true);
        assertNotNull(md);
        
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf: *->default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("default")));        
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("test")));        

        // confs def: test: should not use default conf for the right side (use of defaultconfmapping is required for that) => test->test
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"test"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"test"}), Arrays.asList(dd.getDependencyConfigurations("test")));        
    }
    
    public void testDefaultConfMapping() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-defaultconfmapping.xml"), true);
        assertNotNull(md);
        
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf: *->default
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("default")));        
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("test")));        

        // confs def: test: should use default conf mapping for the right side => test->default
        dd = getDependency(dependencies, "mymodule2");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("2.0", dd.getDependencyRevisionId().getRevision());
        assertEquals(Arrays.asList(new String[] {"test"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default"}), Arrays.asList(dd.getDependencyConfigurations("test")));        
    }
    
    public void testExtraAttributes() throws Exception {
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-extra-attributes.xml"), false);
        assertNotNull(md);

        assertEquals("infoextravalue", md.getAttribute("infoextra"));
        assertEquals("infoextravalue", md.getModuleRevisionId().getAttribute("infoextra"));

        assertEquals("confextravalue", md.getConfiguration("default").getAttribute("confextra"));
        
        Artifact[] artifacts = md.getArtifacts("default");
        assertEquals(1, artifacts.length);
        Artifact art = artifacts[0];
        assertEquals("art1", art.getName());
        assertEquals("artextravalue", art.getAttribute("artextra"));
        
        
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(1, dependencies.length);
        
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertNotNull(dd);
        assertEquals("myorg", dd.getDependencyId().getOrganisation());
        assertEquals("1.0", dd.getDependencyRevisionId().getRevision());
        assertEquals("depextravalue", dd.getAttribute("depextra"));
        assertEquals("depextravalue", dd.getDependencyRevisionId().getAttribute("depextra"));
    }
    
    public void testImportConfigurations1() throws Exception {
        // import configurations
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configurations-import1.xml"), true);
        assertNotNull(md);
        
        // should have imported configurations
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {
                new Configuration("conf1", Visibility.PUBLIC, "", new String[0]),
                new Configuration("conf2", Visibility.PRIVATE, "", new String[0])
                }), Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf: *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf1")));        

        // confs def: conf1->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList(new String[] {"conf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf1")));        
    }
    
    public void testImportConfigurations2() throws Exception {
        // import configurations and add another one
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configurations-import2.xml"), true);
        assertNotNull(md);
        
        // should have imported configurations and added the one defined in the file itself
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {
                new Configuration("conf1", Visibility.PUBLIC, "", new String[0]),
                new Configuration("conf2", Visibility.PRIVATE, "", new String[0]),
                new Configuration("conf3", Visibility.PUBLIC, "", new String[0])
                }), Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf: *->*
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf1")));        

        // confs def: conf2,conf3->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(new HashSet(Arrays.asList(new String[] {"conf2", "conf3"})), new HashSet(Arrays.asList(dd.getModuleConfigurations())));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf2")));        
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf3")));        
    }
    
    public void testImportConfigurations3() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configurations-import3.xml"), true);
        assertNotNull(md);
        
        // should have imported configurations
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(new Configuration[] {
                new Configuration("conf1", Visibility.PUBLIC, "", new String[0]),
                new Configuration("conf2", Visibility.PRIVATE, "", new String[0])
                }), Arrays.asList(md.getConfigurations()));

        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // no conf def => defaults to defaultConf defined in imported file: *->@
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"conf1"}), Arrays.asList(dd.getDependencyConfigurations("conf1")));        
        assertEquals(Arrays.asList(new String[] {"conf2"}), Arrays.asList(dd.getDependencyConfigurations("conf2")));        

        // confs def: conf1->*
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList(new String[] {"conf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getDependencyConfigurations("conf1")));        
    }
    
    public void testExtendOtherConfigs() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configextendsothers1.xml"), true);
        assertNotNull(md);
        
        // has an 'all-public' configuration
        Configuration allPublic = md.getConfiguration("all-public");
        assertNotNull(allPublic);
        
        // 'all-public' extends all other public configurations
        String[] allPublicExt = allPublic.getExtends();
        assertEquals(Arrays.asList(new String[] {"default", "test"}), Arrays.asList(allPublicExt));
    }
    
    public void testImportConfigurationsWithExtendOtherConfigs() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configextendsothers2.xml"), true);
        assertNotNull(md);
        
        // has an 'all-public' configuration
        Configuration allPublic = md.getConfiguration("all-public");
        assertNotNull(allPublic);
        
        // 'all-public' extends all other public configurations
        String[] allPublicExt = allPublic.getExtends();
        assertEquals(Arrays.asList(new String[] {"default", "test", "extra"}), Arrays.asList(allPublicExt));
    }
    
    public void testImportConfigurationsWithMappingOverride() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configurations-import4.xml"), true);
        assertNotNull(md);
        
        // has 2 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // confs dep1: conf1->A;conf2->B (mappingoverride = true)
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"conf2", "conf1"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"A"}), Arrays.asList(dd.getDependencyConfigurations("conf1"))); 
        assertEquals(Arrays.asList(new String[] {"B"}), Arrays.asList(dd.getDependencyConfigurations("conf2")));  
        
        // confs dep2: conf2->B
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList(new String[] {"conf2"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"B"}), Arrays.asList(dd.getDependencyConfigurations("conf2")));  
    }
    
    public void testImportConfigurationsWithWildcardAndMappingOverride() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-configextendsothers3.xml"), true);
        assertNotNull(md);
        
        // has 2 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.length);
        
        // confs dep1: all-public->all-public (mappingoverride = true)
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"all-public"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"all-public"}), Arrays.asList(dd.getDependencyConfigurations("all-public"))); 
        
        // confs dep2: extra->extra;all-public->all-public (mappingoverride = true)
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList(new String[] {"all-public", "extra"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"extra"}), Arrays.asList(dd.getDependencyConfigurations("extra")));  
        assertEquals(Arrays.asList(new String[] {"all-public"}), Arrays.asList(dd.getDependencyConfigurations("all-public")));  
    }

    public void testDefaultConfMappingWithSelectors() throws Exception {
        // import configurations and default mapping
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(_ivy, getClass().getResource("test-defaultconfmapping-withselectors.xml"), true);
        assertNotNull(md);
        
        // has 3 dependencies
        DependencyDescriptor[] dependencies = md.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.length);
        
        // confs dep1: *->default1,default3
        DependencyDescriptor dd = getDependency(dependencies, "mymodule1");
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default1", "default3"}), Arrays.asList(dd.getDependencyConfigurations("default"))); 
        
        // confs dep2: test->default2,default3
        dd = getDependency(dependencies, "mymodule2");
        assertEquals(Arrays.asList(new String[] {"test"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default2", "default3"}), Arrays.asList(dd.getDependencyConfigurations("test")));  
        
        // confs dep3: *->default4
        dd = getDependency(dependencies, "mymodule3");
        assertEquals(Arrays.asList(new String[] {"*"}), Arrays.asList(dd.getModuleConfigurations()));
        assertEquals(Arrays.asList(new String[] {"default4"}), Arrays.asList(dd.getDependencyConfigurations("bla")));  
    }
}
