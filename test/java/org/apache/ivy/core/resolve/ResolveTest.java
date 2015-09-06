/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.resolve;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExtendsDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.circular.ErrorCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.IgnoreCircularDependencyStrategy;
import org.apache.ivy.plugins.circular.WarnCircularDependencyStrategy;
import org.apache.ivy.plugins.conflict.StrictConflictException;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.DualResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.MockMessageLogger;
import org.apache.ivy.util.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import junit.framework.TestCase;

/**
 *
 */
public class ResolveTest extends TestCase {
    private Ivy ivy;

    private File cache;

    private File deliverDir;

    private File workDir;

    public ResolveTest() {
    }

    @Override
    protected void setUp() throws Exception {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        createCache();

        deliverDir = new File("build/test/deliver");
        deliverDir.mkdirs();

        workDir = new File("build/test/work");
        workDir.mkdirs();

        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
    }

    private void createCache() {
        cache.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
        FileUtil.forceDelete(deliverDir);
        FileUtil.forceDelete(workDir);
    }

    public void testResolveWithRetainingArtifactName() throws Exception {
        ((DefaultRepositoryCacheManager) ivy.getSettings().getDefaultRepositoryCacheManager())
                .setArtifactPattern(ivy.substitute("[module]/[originalname].[ext]"));
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod15.2/ivy-1.1.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default")
                .getDownloadReports(ModuleRevisionId.newInstance("org15", "mod15.1", "1.1"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);

        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        String cachePath = getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath,
            cachePath.endsWith("library.jar"));

        dReports = report.getConfigurationReport("default").getDownloadReports(
            ModuleRevisionId.newInstance("org14", "mod14.1", "1.1"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);

        artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        cachePath = getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath,
            cachePath.endsWith("mod14.1-1.1.jar"));
    }

    public void testResolveWithRetainingArtifactNameAndExtraAttributes() throws Exception {
        ((DefaultRepositoryCacheManager) ivy.getSettings().getDefaultRepositoryCacheManager())
                .setArtifactPattern(ivy.substitute("[module]/[originalname].[ext]"));
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod15.4/ivy-1.1.xml"),
            getResolveOptions(new String[] {"default"}).setValidate(false));
        assertNotNull(report);

        Map<String, String> extra = new HashMap<String, String>();
        extra.put("extra", "foo");
        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default")
                .getDownloadReports(ModuleRevisionId.newInstance("org15", "mod15.3", "1.1", extra));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);

        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        String cachePath = getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath,
            cachePath.endsWith("library.jar"));

        dReports = report.getConfigurationReport("default").getDownloadReports(
            ModuleRevisionId.newInstance("org14", "mod14.1", "1.1"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);

        artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        cachePath = getArchivePathInCache(artifact);
        assertTrue("artifact name has not been retained: " + cachePath,
            cachePath.endsWith("mod14.1-1.1.jar"));
    }

    public void testArtifactOrigin() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default")
                .getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);

        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        String expectedLocation = new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar")
                .getAbsolutePath();

        // verify the origin in the report
        ArtifactOrigin reportOrigin = dReports[0].getArtifactOrigin();
        assertNotNull(reportOrigin);
        assertEquals("isLocal for artifact not correct", true, reportOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation,
            reportOrigin.getLocation());

        // verify the saved origin on disk
        ArtifactOrigin ivyOrigin = getSavedArtifactOrigin(artifact);
        assertNotNull(ivyOrigin);
        assertEquals("isLocal for artifact not correct", true, ivyOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation, ivyOrigin.getLocation());

        // now resolve the same artifact again and verify the origin of the (not-downloaded)
        // artifact
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        dReports = report.getConfigurationReport("default").getDownloadReports(
            ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct", 1, dReports.length);
        assertEquals("download status not correct", DownloadStatus.NO,
            dReports[0].getDownloadStatus());
        reportOrigin = dReports[0].getArtifactOrigin();
        assertNotNull(reportOrigin);
        assertEquals("isLocal for artifact not correct", true, reportOrigin.isLocal());
        assertEquals("location for artifact not correct", expectedLocation,
            reportOrigin.getLocation());
    }

    public void testUseOrigin() throws Exception {
        ((DefaultRepositoryCacheManager) ivy.getSettings().getDefaultRepositoryCacheManager())
                .setUseOrigin(true);

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ArtifactDownloadReport[] dReports = report.getConfigurationReport("default")
                .getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(dReports);
        assertEquals("number of downloaded artifacts not correct.", 1, dReports.length);
        assertEquals(
            "download status not correct: should not download the artifact in useOrigin mode.",
            DownloadStatus.NO, dReports[0].getDownloadStatus());

        Artifact artifact = dReports[0].getArtifact();
        assertNotNull(artifact);

        String expectedLocation = new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar")
                .getAbsolutePath();

        ArtifactOrigin origin = getSavedArtifactOrigin(artifact);
        File artInCache = new File(cache, getArchivePathInCache(artifact, origin));
        assertFalse("should not download artifact in useOrigin mode.", artInCache.exists());
        assertEquals("location for artifact not correct.", expectedLocation,
            getArchiveFileInCache(artifact).getAbsolutePath());
    }

    public void testResolveSimple() throws Exception {
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveBadStatus() throws Exception {
        // mod1.4 depends on modfailure, modfailure has a bad status
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveWithXmlEntities() throws Exception {
        Ivy ivy = new Ivy();
        Throwable th = null;
        try {
            ivy.configure(new File("test/repositories/xml-entities/ivysettings.xml"));
            ResolveReport report = ivy.resolve(new File("test/repositories/xml-entities/ivy.xml"),
                getResolveOptions(new String[] {"*"}));
            assertNotNull(report);
            assertFalse(report.hasError());
        } catch (Throwable e) {
            th = e;
        }
        assertNull(th);
    }

    public void testResolveNoRevisionInPattern() throws Exception {
        // module1 depends on latest version of module2, for which there is no revision in the
        // pattern
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/norev/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/norev/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
    }

    public void testResolveLatestWithNoRevisionInPattern() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/norev/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/norev/ivy-latest.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
    }

    public void testResolveNoRevisionInDep() throws Exception {
        // mod1.4 depends on mod1.6, in which the ivy file has no revision
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveNoRevisionNowhere() throws Exception {
        // test case for IVY-258
        // module1 depends on latest version of module2, which contains no revision in its ivy file,
        // nor in the pattern
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-258/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-258/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        ((BasicResolver) ivy.getSettings().getResolver("myresolver")).setCheckconsistency(false);
        report = ivy.resolve(new File("test/repositories/IVY-258/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());
    }

    public void testResolveWithConflictManagerPerModule() throws Exception {
        // test case for IVY-448
        // all modules from myorg
        // module1
        // -> module2-1.0
        // -> module3-2.0
        // module2
        // -> module3-1.0
        // settings use 'all' as default conflict manager, and latest-revision for modules from
        // myorg
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-448/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-448/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // rev 1.0 should have been evicted by latest-revision conflict manager
        assertTrue(getArchiveFileInCache(ivy, "myorg", "module3", "2.0", "module3", "jar", "jar")
                .exists());
        assertFalse(getArchiveFileInCache(ivy, "myorg", "module3", "1.0", "module3", "jar", "jar")
                .exists());
    }

    public void testResolveWithConflictManagerDefinedInModule() throws Exception {
        // test case for IVY-465
        // #M1;1.0 -> #M2;1.0
        // #M2;1.0 -> {org1#mod1.2;1.1 org1#mod1.2;2.0} with
        // <conflict org="org1" module="mod1.2" rev="1.1,2.0" />

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/IVY-465/M1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        ArtifactDownloadReport[] adrs = report.getConfigurationReport("default")
                .getDownloadedArtifactsReports();
        assertEquals(2, adrs.length);
        assertEquals("1.1", adrs[0].getArtifact().getId().getRevision());
        assertEquals("2.0", adrs[1].getArtifact().getId().getRevision());
    }

    public void testResolveRequiresDescriptor() throws Exception {
        // mod1.1 depends on mod1.2, mod1.2 has no ivy file
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        ((FileSystemResolver) ivy.getSettings().getResolver("1"))
                .setDescriptor(FileSystemResolver.DESCRIPTOR_REQUIRED);
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertTrue(report.hasError());
    }

    public void testResolveOtherConfiguration() throws Exception {
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-other.xml"),
            getResolveOptions(new String[] {"test"}));

        assertNotNull(report);
        assertFalse(report.hasError());

        assertEquals("Number of artifacts not correct", 1, report.getConfigurationReport("test")
                .getArtifactsNumber());
    }

    public void testResolveWithSlashes() throws Exception {
        // test case for IVY-198
        // module depends on mod1.2
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-198.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg/mydep", "system/module", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("yourorg/yourdep", "yoursys/yourmod", "1.0")).exists());
        assertTrue(getArchiveFileInCache("yourorg/yourdep", "yoursys/yourmod", "1.0", "yourmod",
            "jar", "jar").exists());
    }

    public void testFromCache() throws Exception {
        // mod1.1 depends on mod1.2

        // we first do a simple resolve so that module is in cache
        ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        // we now use a badly configured ivy, so that it can't find module in repository
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);
        ivy.getSettings().validate();

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}));
        assertFalse(report.hasError());

        ModuleDescriptor md = report.getModuleDescriptor();

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(ivy, mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testFromCache2() throws Exception {
        // mod1.1 depends on mod1.2

        Ivy ivy = ivyTestCache();

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        File art = new File("build/testCache2/mod1.2-2.0.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), art, null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // now we clean the repository to simulate repo not available (network pb for instance)
        FileUtil.forceDelete(new File("build/testCache2"));

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        ModuleDescriptor md = report.getModuleDescriptor();

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(ivy, mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testDynamicFromCache() throws Exception {
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[

        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "10s", true);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // now we clean the repository to simulate repo not available (network pb for instance)
        FileUtil.forceDelete(new File("build/testCache2"));

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());
    }

    public void testDynamicFromCacheWithMD() throws Exception {
        // same as above, but this time the dependency has a module descriptor
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[

        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "10s", true);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(ResolveTest.class.getResourceAsStream("ivy-mod1.2-1.5.xml"), new File(
                "build/testCache2/ivy-mod1.2-1.5.xml"), null);
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // now we clean the repository to simulate repo not available (network pb for instance)
        FileUtil.forceDelete(new File("build/testCache2"));

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());
    }

    public void testDynamicFromCacheWithMDAfterOneTTLExpiration() throws Exception {
        // same as above, but this time we make a second resolve after ttl expiration before trying
        // to use the cached resolved information
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[

        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "500ms", true);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(ResolveTest.class.getResourceAsStream("ivy-mod1.2-1.5.xml"), new File(
                "build/testCache2/ivy-mod1.2-1.5.xml"), null);
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // now we wait for ttl expiration
        Thread.sleep(700);

        // we resolve again, it should work fine
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // now we clean the repository to simulate repo not available (network pb for instance)
        FileUtil.forceDelete(new File("build/testCache2"));

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());
    }

    public void testDynamicFromCacheWithTTL0() throws Exception {
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[

        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "0ms", true);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // now we update the repository
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.6.jar"), null);

        // now do a new resolve: it should not use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.6"))), report.getConfigurationReport("default").getModuleRevisionIds());
    }

    public void testDynamicFromCacheWithTTL() throws Exception {
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[
        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "10s", true);
        ((DefaultRepositoryCacheManager) ivy.getSettings().getDefaultRepositoryCacheManager())
                .addTTL(ModuleRevisionId.newInstance("org1", "*", "*").getAttributes(),
                    ExactPatternMatcher.INSTANCE, 500);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // now we update the repository
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.6.jar"), null);

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // wait for org1 TTL to expire
        Thread.sleep(700);

        // now do a new resolve: it should resolve the dynamic revision again
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.6"))), report.getConfigurationReport("default").getModuleRevisionIds());
    }

    public void testRefreshDynamicFromCache() throws Exception {
        // mod1.4;1.0.2 depends on mod1.2;[1.0,2.0[
        Ivy ivy = ivyTestCache();
        ivy.getSettings().setVariable("ivy.cache.ttl.default", "10s", true);

        // set up repository
        FileUtil.forceDelete(new File("build/testCache2"));
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.5.jar"), null);

        // we first do a simple resolve so that module is in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // now we update the repository
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), new File(
                "build/testCache2/mod1.2-1.6.jar"), null);

        // now do a new resolve: it should use cached data
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.5"))), report.getConfigurationReport("default").getModuleRevisionIds());

        // resolve again with refresh: it should find the new version
        report = ivy.resolve(new File("test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}).setRefresh(true));
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org1",
                "mod1.2", "1.6"))), report.getConfigurationReport("default").getModuleRevisionIds());

        FileUtil.forceDelete(new File("build/testCache2"));
    }

    /**
     * Configures an Ivy instance using a resolver locating modules on file system, in a
     * build/testCache2 location which is created for the test and removed after, and can thus
     * easily simulate a repository availability problem
     * 
     * @return the configured ivy instance
     */
    private Ivy ivyTestCache() {
        Ivy ivy = Ivy.newInstance();
        DualResolver resolver = new DualResolver();
        resolver.setName("dual");
        FileSystemResolver r = new FileSystemResolver();
        r.setName("1");
        r.addIvyPattern(ivy.getSettings().getBaseDir().getPath()
                + "/build/testCache2/ivy-[module]-[revision].xml");
        resolver.add(r);
        r = new FileSystemResolver();
        r.setName("2");
        r.addArtifactPattern(ivy.getSettings().getBaseDir().getPath()
                + "/build/testCache2/[artifact]-[revision].[ext]");
        resolver.add(r);
        ivy.getSettings().addResolver(resolver);
        ivy.getSettings().setDefaultResolver("dual");
        return ivy;
    }

    public void testFromCacheOnly() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        // ResolveReport report = ivy.resolve(new
        // File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
        // getResolveOptions(new String[] {"*"}));
        // // should have an error, the conf is bad and the dependency should not be found
        // assertTrue(report.hasError());

        // put necessary stuff in cache, and it should now be ok
        File ivyfile = getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        File art = getArchiveFileInCache(ivy, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar");
        FileUtil.copy(ResolveTest.class.getResource("ivy-mod1.2.xml"), ivyfile, null);
        FileUtil.copy(new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar"), art, null);

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}));
        assertFalse(report.hasError());
    }

    public void testChangeCacheLayout() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        DefaultRepositoryCacheManager cacheMgr = (DefaultRepositoryCacheManager) ivy.getSettings()
                .getDefaultRepositoryCacheManager();

        cacheMgr.setIvyPattern("[module]/ivy.xml");
        cacheMgr.setArtifactPattern("[artifact].[ext]");

        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(ivy, mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ivy, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(new File(cache, "mod1.2/ivy.xml").exists());
        assertTrue(getArchiveFileInCache(ivy, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .exists());
        assertTrue(new File(cache, "mod1.2.jar").exists());
    }

    public void testChangeCacheLayout2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        ivy.getSettings().setDefaultRepositoryCacheBasedir(
            new File(ivy.getSettings().getDefaultCache(), "repository").getAbsolutePath());
        ivy.getSettings().setDefaultResolutionCacheBasedir(
            new File(ivy.getSettings().getDefaultCache(), "workspace").getAbsolutePath());
        ivy.getSettings().validate();
        DefaultRepositoryCacheManager cacheMgr = (DefaultRepositoryCacheManager) ivy.getSettings()
                .getDefaultRepositoryCacheManager();

        cacheMgr.setIvyPattern("[module]/ivy.xml");
        cacheMgr.setArtifactPattern("[artifact].[ext]");

        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(ivy, mrid).toString().indexOf("workspace") != -1);
        assertTrue(getResolvedIvyFileInCache(ivy, mrid).exists());
        assertTrue(getConfigurationResolveReportInCache(ivy, report.getResolveId(), "default")
                .exists());

        // dependencies
        assertTrue(getIvyFileInCache(ivy, ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(new File(cache, "repository/mod1.2/ivy.xml").exists());
        assertTrue(getArchiveFileInCache(ivy, "org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .exists());
        assertTrue(new File(cache, "repository/mod1.2.jar").exists());
    }

    public void testMultipleCache() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings-multicache.xml"));

        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        // dependencies
        DefaultArtifact depArtifact = TestHelper.newArtifact("org1", "mod1.1", "1.0", "mod1.1",
            "jar", "jar");
        ModuleRevisionId depMrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        DefaultRepositoryCacheManager cacheMgr1 = (DefaultRepositoryCacheManager) ivy.getSettings()
                .getDefaultRepositoryCacheManager();
        DefaultRepositoryCacheManager cacheMgr2 = (DefaultRepositoryCacheManager) ivy.getSettings()
                .getRepositoryCacheManager("cache2");

        // ivy file should be cached in default cache, and artifact in cache2
        assertTrue(cacheMgr1.getIvyFileInCache(depMrid).exists());
        assertFalse(cacheMgr1.getArchiveFileInCache(depArtifact).exists());
        assertEquals(new File(cache, "repo1/mod1.1/ivy-1.0.xml").getCanonicalFile(), cacheMgr1
                .getIvyFileInCache(depMrid).getCanonicalFile());

        assertFalse(cacheMgr2.getIvyFileInCache(depMrid).exists());
        assertTrue(cacheMgr2.getArchiveFileInCache(depArtifact).exists());
        assertEquals(new File(cache, "repo2/mod1.1-1.0/mod1.1.jar").getCanonicalFile(), cacheMgr2
                .getArchiveFileInCache(depArtifact).getCanonicalFile());
    }

    public void testForceLocal() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        // a local build for mod1.2 is available
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings-local.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org1", "mod1.2", "local-20080708091023")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "local-20080708091023", "mod1.2", "jar",
            "jar").exists());

        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testForceLocal2() throws Exception {
        // mod2.3 -> mod2.1;[0.0,0.4] -> mod1.1 -> mod1.2
        // a local build for mod2.1 and mod1.2 is available
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings-local.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.8.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org2", "mod2.1", "0.3-local-20050213110000")).exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3-local-20050213110000", "mod2.1",
            "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.1", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org1", "mod1.2", "local-20080708091023")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "local-20080708091023", "mod1.2", "jar",
            "jar").exists());

        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.4"))
                .exists());
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testForceLocal3() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        // a local build for mod1.2 is available
        // we do a first resolve without local build so that cache contains mod1.2;2.0 module
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings-local.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org1", "mod1.2", "local-20080708091023")).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "local-20080708091023", "mod1.2", "jar",
            "jar").exists());

        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
    }

    public void testResolveExtends() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"extension"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies from default
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExtended() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies from default
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExtendedAndExtends() throws Exception {
        // mod6.1 depends on mod1.2 2.0 in conf default, and conf extension extends default
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"default", "extension"}));
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

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtends() throws Exception {
        // mod6.2 has two confs default and extension
        // mod6.2 depends on mod6.1 in conf (default->extension)
        // conf extension extends default
        // mod6.1 has two confs default and extension
        // mod6.1 depends on mod1.2 2.0 in conf (default->default)
        // conf extension extends default
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.2/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"default", "extension"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("extension");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org6", "mod6.1", "0.4"))
                .exists());
        assertTrue(getArchiveFileInCache("org6", "mod6.1", "0.4", "mod6.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtendsAndConfs() throws Exception {
        // Test case for IVY-240
        //
        // mod6.3 1.1 has four confs libraries, run (extends libraries), compile (extends run) and
        // test (extends libraries)
        // mod6.3 depends on mod6.2 2.0 in conf (run->default)
        // mod6.3 depends on mod6.1 2.+ in conf (test->default)
        // mod6.2 2.0 depends on mod6.1 2.0 in conf (default->standalone)
        // mod6.1 2.0 has two confs default and standalone
        // mod6.1 2.0 depends on mod1.2 2.2 in conf (default->default)
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ConfigurationResolveReport crr = report.getConfigurationReport("libraries");
        assertEquals(0, crr.getArtifactsNumber());

        crr = report.getConfigurationReport("run");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.2", "2.0", "mod6.2", "jar", "jar", crr);
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);

        crr = report.getConfigurationReport("compile");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.2", "2.0", "mod6.2", "jar", "jar", crr);
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);

        crr = report.getConfigurationReport("test");
        assertEquals(2, crr.getArtifactsNumber());
        assertContainsArtifact("org6", "mod6.1", "2.0", "mod6.1", "jar", "jar", crr);
        assertContainsArtifact("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar", crr);
    }

    public void testResolveMultipleConfsWithLatest() throws Exception {
        // Test case for IVY-188
        //
        // mod6.2 has two confs compile and run
        // depends on mod6.1 in conf (compile->default)
        // depends on mod1.2 latest (which is 2.2) in conf (run->default)
        // mod6.1
        // depends on mod1.2 2.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.2/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"compile", "run"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("run");
        assertNotNull(crr);
        assertEquals(1, crr.getArtifactsNumber());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleConfsWithConflicts() throws Exception {
        // Test case for IVY-173
        //
        // mod6.2 has two confs compile and run
        // depends on mod1.2 2.1 in conf (compile->default)
        // depends on mod1.1 1.0 in conf (*->default)
        // depends on mod6.1 in conf (*->default)
        // mod6.1
        // depends on mod1.2 2.1
        // mod1.1
        // depends on mod1.2 2.0
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.2/ivys/ivy-0.5.xml"),
            getResolveOptions(new String[] {"compile", "run"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(3, crr.getArtifactsNumber());
        crr = report.getConfigurationReport("run");
        assertNotNull(crr);
        assertEquals(3, crr.getArtifactsNumber());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org6", "mod6.1", "0.5"))
                .exists());
        assertTrue(getArchiveFileInCache("org6", "mod6.1", "0.5", "mod6.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleExtends2() throws Exception {
        // same as before, except that mod6.2 depends on mod1.2 2.1 extension->default
        // so mod1.2 2.0 should be evicted in conf extension
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml"),
            getResolveOptions(new String[] {"default", "extension"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        IvyNode node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(node);
        assertFalse(node.isEvicted("default"));
        crr = report.getConfigurationReport("extension");
        assertNotNull(crr);
        assertEquals(2, crr.getArtifactsNumber());
        node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"));
        assertNotNull(node);
        assertTrue(node.isEvicted("extension"));
        node = crr.getDependency(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"));
        assertNotNull(node);
        assertFalse(node.isEvicted("extension"));

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org6", "mod6.1", "0.4"))
                .exists());
        assertTrue(getArchiveFileInCache("org6", "mod6.1", "0.4", "mod6.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveSeveralDefaultWithArtifacts() throws Exception {
        // test case for IVY-261
        // mod1.6 depends on
        // mod1.4, which depends on mod1.3 and selects one of its artifacts
        // mod1.3 and selects two of its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.6/ivys/ivy-1.0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
    }

    public void testResolveConflictsWithArtifacts() throws Exception {
        // test case for IVY-537
        // #mod2.6;0.12 -> {#mod1.6;1.0.4 #mod2.5;0.6.2 }
        // #mod1.6;1.0.4 -> #mod1.3;3.0 artifacts A and B
        // #mod2.5;0.6.2 -> #mod1.3;3.1 artifact C
        // #mod1.3;3.1 has only A and C artifacts, not B.
        // Both A and C should be downloaded, and a message should tell that B was not available.
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.12.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.1", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.1", "mod1.3-C", "jar", "jar")
                .exists());
    }

    public void testResolveSeveralDefaultWithArtifactsAndConfs() throws Exception {
        // test case for IVY-283
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-283/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-283/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("build");
        assertNotNull(crr);
        assertEquals(3,
            crr.getDownloadReports(ModuleRevisionId.newInstance("medicel", "C", "1.0")).length);

        assertTrue(getArchiveFileInCache(ivy, "medicel", "C", "1.0", "lib_c_a", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "C", "1.0", "lib_c_b", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "C", "1.0", "lib_c_d", "jar", "jar")
                .exists());
    }

    public void testResolveSeveralDefaultWithArtifactsAndConfs2() throws Exception {
        // second test case for IVY-283
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-283/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-283/ivy-d.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("build");
        assertNotNull(crr);
        assertEquals(
            9,
            crr.getDownloadReports(ModuleRevisionId.newInstance("medicel", "module_a", "local")).length);

        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_a", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_b", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_c", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_d", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_e", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_f", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_g", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_h", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "medicel", "module_a", "local", "lib_a_i", "jar",
            "jar").exists());
    }

    public void testResolveWithStartPublicConf() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.8.xml"),
            getResolveOptions(new String[] {"*(public)"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.8");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.3", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }

    public void testResolveWithPrivateConf() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.8.xml"),
            getResolveOptions(new String[] {"*(private)"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.8");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.3", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }

    public void testResolveDefaultWithArtifactsConf1() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml"),
            getResolveOptions(new String[] {"myconf1"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.3", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }

    public void testResolveDefaultWithArtifactsConf2() throws Exception {
        // mod2.2 depends on mod1.3 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.5.xml"),
            getResolveOptions(new String[] {"myconf2"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.3", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }

    public void testResolveDefaultWithArtifactsAndConfMapping() throws Exception {
        // mod2.2 depends on mod1.3 and specify its artifacts and a conf mapping
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.2/ivys/ivy-0.5.1.xml"),
            getResolveOptions(new String[] {"myconf1"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.2", "0.5.1");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.3", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-A", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3-B", "jar", "jar")
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.3", "3.0", "mod1.3", "jar", "jar").exists());
    }

    public void testResolveWithIncludeArtifactsConf1() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts in myconf1
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.4.xml"),
            getResolveOptions(new String[] {"myconf1"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithIncludeArtifactsConf2() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts in myconf1
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.4.xml"),
            getResolveOptions(new String[] {"myconf2"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithIncludeArtifactsWithoutConf() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.5");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithIncludeArtifactsTransitive() throws Exception {
        // test case for IVY-541
        // mod2.6 depends on mod2.3 and mod2.1
        // mod2.3 depends on mod2.1 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveWithExcludesArtifacts() throws Exception {
        // mod2.3 depends on mod2.1 and selects its artifacts
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithExcludesArtifacts2() throws Exception {
        // mod2.3 depends on mod2.1 and badly excludes artifacts with incorrect matcher
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.6.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.2");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithExcludesArtifacts3() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with exact matcher
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.6.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithExcludesArtifacts4() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with regexp matcher
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.6.4.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.4");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveWithExcludesArtifacts5() throws Exception {
        // mod2.3 depends on mod2.1 and excludes artifacts with glob matcher
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.3/ivys/ivy-0.6.5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.3", "0.6.5");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(!getArchiveFileInCache("org2", "mod2.1", "0.3", "mod2.1", "jar", "jar").exists());
    }

    public void testResolveTransitiveDependencies() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveTransitiveDependenciesWithOverride() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.0", "mod1.2", "jar", "jar").exists());
    }

    /**
     * Testcase for IVY-1131.
     */
    public void testResolveTransitiveDependenciesWithOverrideAndDynamicResolveMode()
            throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"})
                    .setResolveMode(ResolveOptions.RESOLVEMODE_DYNAMIC));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveTransitiveDisabled() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}).setTransitive(false));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testDependenciesOrder() throws Exception {
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-225.xml"),
            getResolveOptions(new String[] {"default"}));

        List<ModuleRevisionId> revisions = new ArrayList<ModuleRevisionId>(report
                .getConfigurationReport("default").getModuleRevisionIds());
        assertTrue("number of revisions is not correct", revisions.size() >= 3);

        int mod12Index = revisions.indexOf(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"));
        int mod32Index = revisions.indexOf(ModuleRevisionId.newInstance("org3", "mod3.2", "1.4"));
        int mod51Index = revisions.indexOf(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2"));

        // verify the order of the modules in the ivy file
        assertTrue("[ org1 | mod1.2 | 1.1 ] was not found", mod12Index > -1);
        assertTrue("[ org1 | mod1.2 | 1.1 ] must come before [ org3 | mod3.2 | 1.4 ]",
            mod12Index < mod32Index);
        assertTrue("[ org3 | mod3.2 | 1.4 ] must come before [ org5 | mod5.1 | 4.2 ]",
            mod32Index < mod51Index);
    }

    public void testDisableTransitivityPerConfiguration() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive

        // first we resolve compile conf only
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml"),
            getResolveOptions(new String[] {"compile"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml"),
            getResolveOptions(new String[] {"runtime"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call
        ResolveReport r = ivy.resolve(
            new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.1.xml"),
            getResolveOptions(new String[] {"runtime", "compile"}));
        assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }

    public void testDisableTransitivityPerConfiguration2() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive
        // compile extends runtime

        // first we resolve compile conf only
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml"),
            getResolveOptions(new String[] {"compile"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml"),
            getResolveOptions(new String[] {"runtime"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call
        ResolveReport r = ivy.resolve(
            new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.2.xml"),
            getResolveOptions(new String[] {"runtime", "compile"}));
        assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }

    public void testDisableTransitivityPerConfiguration3() throws Exception {
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive
        // runtime extends compile

        // first we resolve compile conf only
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml"),
            getResolveOptions(new String[] {"compile"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // then we resolve runtime conf
        ivy.resolve(new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml"),
            getResolveOptions(new String[] {"runtime"}));

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // same as before, but resolve both confs in one call
        ResolveReport r = ivy.resolve(
            new File("test/repositories/1/org2/mod2.1/ivys/ivy-0.3.3.xml"),
            getResolveOptions(new String[] {"runtime", "compile"}));
        assertFalse(r.hasError());
        assertEquals(1, r.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(2, r.getConfigurationReport("runtime").getArtifactsNumber());
    }

    public void testDisableTransitivityPerConfiguration4() throws Exception {
        // mod2.2 (A,B,compile) depends on mod 2.1 (A->runtime;B->compile)
        // compile is not transitive and extends A and B
        //
        // mod2.1 (compile, runtime) depends on mod1.1 which depends on mod1.2
        // compile conf is not transitive and extends runtime

        ResolveReport r = ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(r.hasError());

        // here we should get all three recursive dependencies
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
                ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))),
            r.getConfigurationReport("A").getModuleRevisionIds());

        // here we should get only mod2.1 and mod1.1 cause compile is not transitive in mod2.1
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))),
            r.getConfigurationReport("B").getModuleRevisionIds());

        // here we should get only mod2.1 cause compile is not transitive
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(ModuleRevisionId.newInstance("org2",
                "mod2.1", "0.3.2"))), r.getConfigurationReport("compile").getModuleRevisionIds());
    }

    public void testDisableTransitivityPerConfiguration5() throws Exception {
        // mod2.2 (A,B,compile) depends on
        // mod 2.1 (A->runtime;B->compile)
        // mod1.1 (A->*) ]0.9.9,1.0] (which resolves to 1.0)
        // compile is not transitive and extends A and B
        //
        // mod2.1 (compile, runtime) depends on mod1.1 1.0 which depends on mod1.2 2.0
        // compile conf is not transitive and extends runtime

        ResolveReport r = ivy.resolve(new File("test/repositories/1/org2/mod2.2/ivys/ivy-0.7.xml"),
            getResolveOptions(new String[] {"A", "B", "compile"}));
        assertFalse(r.hasError());

        // here we should get all three recursive dependencies
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"),
                ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))),
            r.getConfigurationReport("A").getModuleRevisionIds());

        // here we should get only mod2.1 and mod1.1 cause compile is not transitive in mod2.1
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))),
            r.getConfigurationReport("B").getModuleRevisionIds());

        // here we should get only mod2.1 cause compile is not transitive
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("org2", "mod2.1", "0.3.2"),
                ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))),
            r.getConfigurationReport("compile").getModuleRevisionIds());
    }

    public void testResolveDiamond() throws Exception {
        // mod4.1 depends on
        // - mod1.1 which depends on mod1.2
        // - mod3.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.0", "mod3.1", "jar", "jar").exists());
    }

    public void testResolveConflict() throws Exception {
        // mod4.1 v 4.1 depends on
        // - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.1.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.1");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).length);
        assertEquals(1,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).length);

        File r = getConfigurationResolveReportInCache(ResolveOptions.getDefaultResolveId(md),
            "default");
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    org.xml.sax.Attributes attributes) throws SAXException {
                if ("revision".equals(qName) && "2.0".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]); // the report should contain the evicted revision

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveConflict2() throws Exception {
        // mod4.1 v 4.14 depends on
        // - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        // - mod6.1 v 0.3 which depends on mod1.2 v 2.0
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.14.xml"),
            getResolveOptions(new String[] {"*"}));

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")).length);
        assertEquals(1,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1")).length);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.14");
        File r = getConfigurationResolveReportInCache(
            ResolveOptions.getDefaultResolveId(mrid.getModuleId()), "default");
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    org.xml.sax.Attributes attributes) throws SAXException {
                if ("revision".equals(qName) && "2.0".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]); // the report should contain the evicted revision

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveConflict3() throws Exception {
        // test case for IVY-264
        // a depends on x latest, y latest, z latest
        // x and z depends on commons-lang 1.0.1
        // y depends on commons-lang 2.0
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-264/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-264/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(
            0,
            crr.getDownloadReports(ModuleRevisionId.newInstance("myorg", "commons-lang", "1.0.1")).length);
        assertEquals(
            1,
            crr.getDownloadReports(ModuleRevisionId.newInstance("myorg", "commons-lang", "2.0")).length);

        assertFalse(getArchiveFileInCache(ivy, "myorg", "commons-lang", "1.0.1", "commons-lang",
            "jar", "jar").exists());

        assertTrue(getArchiveFileInCache(ivy, "myorg", "commons-lang", "2.0", "commons-lang",
            "jar", "jar").exists());
    }

    public void testResolveMergeTransitiveAfterConflict() throws Exception {
        // mod20.4 -> mod20.3;1.0 mod20.2;1.0
        // mod20.3;1.0 -> mod20.1;1.0
        // mod20.2;1.0 -> mod20.1;1.1 (transitive false)
        // mod20.1;1.0 -> mod1.2;1.0
        // mod20.1;1.1 -> mod1.2;1.0
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org20/mod20.4/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org1", "mod1.2", "1.0")).length);

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.0", "mod1.2", "jar", "jar").exists());
    }

    /**
     * Test IVY-618.
     */
    public void testResolveConflictFromPoms() throws Exception {
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod17.1/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
    }

    public void testTransitiveEviction() throws Exception {
        // mod7.3 depends on mod7.2 v1.0 and on mod7.1 v2.0
        // mod7.2 v1.0 depends on mod7.1 v1.0 (which then should be evicted)
        // mod7.1 v1.0 depends on mod 1.2 v2.0 (which should be evicted by transitivity)

        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod7.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org7", "mod7.3", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org7", "mod7.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org7", "mod7.2", "1.0", "mod7.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org7", "mod7.1", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org7", "mod7.1", "2.0", "mod7.1", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org7", "mod7.1", "1.0", "mod7.1", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testTransitiveEviction2() throws Exception {
        // IVY-199
        // mod4.1 v 4.13 depends on
        // - mod3.2 v 1.2.1 which depends on
        // - mod3.1 v 1.0 which depends on mod1.2 v 2.0
        // - mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.13.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testTransitiveEvictionWithExtendingConf() throws Exception {
        // IVY-590
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-590.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // test eviction
        ConfigurationResolveReport compileReport = report.getConfigurationReport("compile");
        IvyNode[] evicted = compileReport.getEvictedNodes();
        assertNotNull(evicted);

        Collection<ModuleRevisionId> evictedModules = new ArrayList<ModuleRevisionId>();
        for (int i = 0; i < evicted.length; i++) {
            evictedModules.add(evicted[i].getId());
        }

        assertTrue(evictedModules.contains(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0")));
    }

    public void testResolveConflictInConf() throws Exception {
        // conflicts in separate confs are not conflicts

        // mod2.1 conf A depends on mod1.1 which depends on mod1.2 2.0
        // mod2.1 conf B depends on mod1.2 2.1
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.4.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.4");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testEvictWithConf() throws Exception {
        // bug 105 - test #1

        // mod6.1 r1.0 depends on
        // mod5.1 r4.2 conf A
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //
        // mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // should have been evicted before download
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testEvictWithConf2() throws Exception {
        // same as preceding one but with inverse order, so that
        // eviction is done after download
        // bug 105 - test #2

        // mod6.1 r1.1 depends on
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        // mod5.1 r4.2 conf A
        //
        // mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.1");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // even late eviction should avoid artifact downloading
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testEvictWithConf3() throws Exception {
        // same as first one but the conf requested in evicted module is no longer present in
        // selected revision
        // test case for IVY-681

        // mod6.1 r1.4 depends on
        // mod5.1 r4.3 conf A
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //
        // mod5.1 r4.3 has only conf A, not B
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.4.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.3", "art51A", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testFailWithMissingConf() throws Exception {
        // test case for IVY-861

        // mod6.1 r1.5 depends on
        // mod5.1 [1.0,4.3] conf unknown which doesn't exist in mod5.1;4.3
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("missing conf should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "'unknown'") != -1);
    }

    public void testEvictWithConfInMultiConf() throws Exception {
        // same as preceding ones but the conflict appears in several root confs
        // bug 105 - test #3

        // mod6.1 r1.2 conf A and conf B depends on
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        // mod5.1 r4.2 conf A
        //
        // mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.2");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // all artifacts should be present in both confs
        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(2,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);

        crr = report.getConfigurationReport("B");
        assertNotNull(crr);
        assertEquals(2,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);

        // even late eviction should avoid artifact downloading
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testEvictWithConfInMultiConf2() throws Exception {
        // same as preceding one but the conflict appears in a root conf and not in another
        // which should keep the evicted
        // bug 105 - test #4

        // mod6.1 r1.3 conf A depends on
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        //
        // mod6.1 r1.3 conf B depends on
        // mod5.2 r1.0 which depends on mod5.1 r4.0 conf B
        // mod5.1 r4.2 conf A
        //
        // mod5.1 r4.2 conf B depends on mod1.2 r2.0
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.1/ivy-1.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.1", "1.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.2", "art51B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.2", "1.0", "mod5.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // 4.2 artifacts should be present in conf B only
        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(0,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);

        crr = report.getConfigurationReport("B");
        assertNotNull(crr);
        assertEquals(2,
            crr.getDownloadReports(ModuleRevisionId.newInstance("org5", "mod5.1", "4.2")).length);
    }

    public void testMultipleEviction() throws Exception {

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/IVY-644/M1/ivys/ivy-1.0.xml"), getResolveOptions(new String[] {
                "test", "runtime"})); // NB the order impact the bug
        assertFalse(report.hasError());
    }

    public void testResolveForce() throws Exception {
        // mod4.1 v 4.2 depends on
        // - mod1.2 v 2.0 and forces it
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.2");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.1", "mod3.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceAfterConflictSolved() throws Exception {
        // IVY-193
        // mod4.1 v 4.9 depends on
        // - mod3.2 v 1.1 which depends on mod1.2 v 2.0
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        // - mod1.2 v 2.0 and forces it
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.9.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.9");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceAfterDependencyExist() throws Exception {
        // IVY-193
        // mod4.1 v 4.10 depends on
        // - mod3.1 v 1.0.1 which depends on mod1.2 v 2.0 and forces it
        // - mod3.2 v 1.2 which depends on mod1.2 v 2.1 and on mod3.1 v1.0.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.10.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // dependencies
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceInDepOnly() throws Exception {
        // IVY-193
        // mod4.1 v 4.11 depends on
        // - mod1.2 v 2.0
        // - mod3.2 v 1.3 which depends on
        // - mod3.1 v1.1 which depends on
        // - mod1.2 v 2.1
        // - mod1.2 v 1.0 and forces it
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.11.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "1.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceInDepOnly2() throws Exception {
        // IVY-193
        // mod4.1 v 4.12 depends on
        // - mod3.1 v1.0 which depends on
        // - mod1.2 v 2.0
        // - mod3.2 v 1.4 which depends on
        // - mod1.2 v 2.0 and forces it
        // - mod3.1 v1.1 which depends on
        // - mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.12.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveWithDynamicRevisionsAndArtifactLockStrategy() throws Exception {
        // mod4.1 v 4.5 depends on
        // - mod1.2 v 1+ and forces it
        // - mod3.1 v 1.2 which depends on mod1.2 v 2+
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings-artifact-lock.xml"));
        ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.5.xml"),
            getResolveOptions(new String[] {"*"}));

        List<File> lockFiles = new ArrayList<File>();
        findLockFiles(cache, lockFiles);
        assertTrue("There were lockfiles left in the cache: " + lockFiles, lockFiles.isEmpty());
    }

    private void findLockFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                findLockFiles(files[i], result);
            } else if (files[i].getName().endsWith(".lck")) {
                result.add(files[i]);
            }
        }
    }

    public void testResolveForceWithDynamicRevisions() throws Exception {
        // mod4.1 v 4.5 depends on
        // - mod1.2 v 1+ and forces it
        // - mod3.1 v 1.2 which depends on mod1.2 v 2+
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.5");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.2", "mod3.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceWithDynamicRevisionsAndSeveralConfs() throws Exception {
        // mod4.1 v 4.6 (conf compile, runtime extends compile, test extends runtime) depends on
        // - mod1.2 v 1+ and forces it in conf compile
        // - mod3.1 v 1.2 in conf test which depends on mod1.2 v 2+
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.6");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.2", "mod3.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceWithDynamicRevisionsAndSeveralConfs2() throws Exception {
        // mod4.1 v 4.7 (conf compile, test extends compile) depends on
        // - mod1.2 v 1+ and forces it in conf compile
        // - mod3.1 v 1.3 in conf test->runtime
        // which defines confs compile, runtime extends compile
        // which depends on mod1.2 v 2+ in conf compile->default
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod4.1/ivy-4.7.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org4", "mod4.1", "4.7");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.3", "mod3.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveForceWithDynamicRevisionsAndCyclicDependencies() throws Exception {
        // IVY-182
        // * has no revision
        // * declares conf compile, test extends compile,
        // * depends on
        // - mod1.2 v 1+ and forces it in conf compile
        // - mod3.1 v 1+ in conf test->runtime excluding mod4.1 (to avoid cyclic dep failure)
        // which defines confs compile, runtime extends compile
        // which depends on mod1.2 v 2+ in conf compile->default
        // which depends on mod4.1 v 4+ in conf compile->compile
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-182.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleId mid = new ModuleId("test", "IVY-182");
        assertEquals(mid, md.getModuleRevisionId().getModuleId());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org3", "mod3.1", "1.4"))
                .exists());
        assertTrue(getArchiveFileInCache("org3", "mod3.1", "1.4", "mod3.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "1.1", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveContradictoryConflictResolution() throws Exception {
        // mod10.1 v 1.0 depends on
        // - mod1.2 v 2.0 and forces it
        // - mod4.1 v 4.1 (which selects mod1.2 v 2.1 and evicts mod1.2 v 2.0)
        // mod4.1 v 4.1 depends on
        // - mod1.1 v 1.0 which depends on mod1.2 v 2.0
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org10", "mod10.1", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // conflicting dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveContradictoryConflictResolution2() throws Exception {
        // BUG IVY-130 : only mod1.2 v2.0 should be resolved and not v2.1 (because of force)
        // mod10.1 v 1.1 depends on
        // - mod1.2 v 2.0 and forces it
        // - mod4.1 v 4.3
        // mod4.1 v 4.3 depends on
        // - mod1.2 v 2.1
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));

        // conflicting dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.1", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveContradictoryConflictResolution3() throws Exception {
        // mod 1.2 v2.0 should be selected (despite conflict manager in 4.1, because of force in
        // 10.1)
        // mod10.1 v 1.3 depends on
        // - mod1.2 v 2.0 and forces it
        // - mod4.1 v 4.4
        // mod4.1 v 4.4 depends on
        // - mod1.2 v 2.0 but selects mod1.2 v 2.1
        // - mod3.1 v 1.1 which depends on mod1.2 v 2.1
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod10.1/ivy-1.3.xml"),
            getResolveOptions(new String[] {"*"}));

        IvyNode[] evicted = report.getConfigurationReport("default").getEvictedNodes();
        assertEquals(1, evicted.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.1"),
            evicted[0].getResolvedId());
    }

    public void testExtends() throws Exception {
        // mod 5.2 depends on mod5.1 conf B
        // mod5.1 conf B publishes art51B
        // mod5.1 conf B extends conf A
        // mod5.1 conf A publishes art51A
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod5.2/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org5", "mod5.2", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org5", "mod5.1", "4.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
    }

    public void testMultiConfs() throws Exception {
        // mod 5.2 depends on mod5.1 conf B in its conf B and conf A in its conf A
        // mod5.1 conf B publishes art51B
        // mod5.1 conf A publishes art51A
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod5.2/ivy-2.0.xml"),
            getResolveOptions(new String[] {"B", "A"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org5", "mod5.2", "2.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org5", "mod5.1", "4.1");

        ConfigurationResolveReport crr = report.getConfigurationReport("A");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);

        File r = new File(cache, ResolveOptions.getDefaultResolveId(mrid.getModuleId()) + "-A.xml");
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    org.xml.sax.Attributes attributes) throws SAXException {
                if ("artifact".equals(qName) && "art51B".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertFalse(found[0]);

        assertTrue(getIvyFileInCache(depId).exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.1", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.1", "art51B", "jar", "jar").exists());
    }

    public void testThisConfiguration() throws Exception {
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod14.4/ivy-1.1.xml"),
            getResolveOptions(new String[] {"compile"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org14", "mod14.4", "1.1");
        assertEquals(mrid, md.getModuleRevisionId());
        ConfigurationResolveReport crr = report.getConfigurationReport("compile");
        assertNotNull(crr);
        assertEquals(4, crr.getArtifactsNumber());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.3", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.2", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.1", "1.1"))
                .exists());
        assertTrue(!getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.3", "1.0"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.1", "1.0"))
                .exists());

        CacheCleaner.deleteDir(cache);
        createCache();
        report = ivy.resolve(new File("test/repositories/2/mod14.4/ivy-1.1.xml"),
            getResolveOptions(new String[] {"standalone"}));
        crr = report.getConfigurationReport("standalone");
        assertNotNull(crr);
        assertEquals(7, crr.getArtifactsNumber());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.3", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.1", "1.1"))
                .exists());
        assertTrue(!getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.2", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org14", "mod14.3", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.3", "1.0"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.1", "1.0"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.4", "1.1"))
                .exists());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org8", "mod8.2", "1.1"))
                .exists());
    }

    public void testLatest() throws Exception {
        // mod1.4 depends on latest mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.1.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.1");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "2.2");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);

        File r = getConfigurationResolveReportInCache(
            ResolveOptions.getDefaultResolveId(mrid.getModuleId()), "default");
        assertTrue(r.exists());
        final boolean[] found = new boolean[] {false};
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(r, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    org.xml.sax.Attributes attributes) throws SAXException {
                if ("artifact".equals(qName) && "mod1.2".equals(attributes.getValue("name"))) {
                    found[0] = true;
                }
            }
        });
        assertTrue(found[0]);

        assertTrue(getIvyFileInCache(depId).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testLatestMultiple() throws Exception {
        // mod1.5 depends on
        // latest mod1.4, which depends on mod1.2 2.2
        // latest mod1.2 (which is 2.2)
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.5/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.4", "2.0"))
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testLatestWhenReleased() throws Exception {
        // The test verify that latest.integration dependencies can be resolved with released
        // version also.
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-latestreleased.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod_released", "1.1"))
                .exists());
    }

    public void testLatestWithMultiplePatterns() throws Exception {
        // The test verify that latest.integration dependencies can be resolved
        // when using a resolver with multiple patterns, when only the first pattern
        // finds something - test case for IVY-602

        // mod9.2 depends on latest.integration of mod6.2
        Ivy ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings-IVY602.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org9/mod9.2/ivys/ivy-1.3.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache("org6", "mod6.2", "2.0", "mod6.2", "jar", "jar").exists());
    }

    public void testResolveModeDynamic1() throws Exception {
        // mod1.1;1.0.1 -> mod1.2;2.0|latest.integration
        ResolveReport report = ivy.resolve(
            new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.1.xml"),
            getResolveOptions(new String[] {"default"}).setResolveMode(
                ResolveOptions.RESOLVEMODE_DYNAMIC));
        assertNotNull(report);

        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "2.2");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertEquals(1, crr.getDownloadReports(depId).length);

        assertTrue(getIvyFileInCache(depId).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveModeDynamic2() throws Exception {
        // same as ResolveModeDynamic1, but resolve mode is set in settings
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("organisation", "org1");
        attributes.put("module", "mod1.2");
        ivy.getSettings().addModuleConfiguration(attributes, ExactPatternMatcher.INSTANCE, null,
            null, null, ResolveOptions.RESOLVEMODE_DYNAMIC);
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.1.xml"),
            getResolveOptions(new String[] {"default"}));
        assertNotNull(report);

        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "2.2");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertEquals(1, crr.getDownloadReports(depId).length);

        assertTrue(getIvyFileInCache(depId).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveModeDynamicWithBranch1() throws Exception {
        // bar1;5 -> foo1#branch1|;2|[0,4]
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/5/ivy.xml"),
            getResolveOptions(new String[] {"*"})
                    .setResolveMode(ResolveOptions.RESOLVEMODE_DYNAMIC));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#trunk;3", "foo1", "jar", "jar").exists());
    }

    public void testResolveModeDynamicWithBranch2() throws Exception {
        // bar1;5 -> foo1#trunk|branch1;3|[0,4]
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/6/ivy.xml"),
            getResolveOptions(new String[] {"*"})
                    .setResolveMode(ResolveOptions.RESOLVEMODE_DYNAMIC));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#branch1;4", "foo1", "jar", "jar").exists());
    }

    public void testResolveModeDefaultOverrideSettings() throws Exception {
        // same as ResolveModeDynamic2, but resolve mode is set in settings, and overriden when
        // calling resolve
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("organisation", "org1");
        attributes.put("module", "mod1.2");
        ivy.getSettings().addModuleConfiguration(attributes, ExactPatternMatcher.INSTANCE, null,
            null, null, ResolveOptions.RESOLVEMODE_DYNAMIC);
        ResolveReport report = ivy.resolve(
            new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.1.xml"),
            getResolveOptions(new String[] {"default"}).setResolveMode(
                ResolveOptions.RESOLVEMODE_DEFAULT));
        assertNotNull(report);

        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "2.0");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertEquals(1, crr.getDownloadReports(depId).length);

        assertTrue(getIvyFileInCache(depId).exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testVersionRange1() throws Exception {
        // mod 1.4 depends on mod1.2 [1.0,2.0[
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org1", "mod1.2", "1.1");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);

        assertTrue(getIvyFileInCache(depId).exists());
    }

    public void testVersionRange2() throws Exception {
        // mod 1.4 depends on mod1.2 [1.5,2.0[
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.3.xml"),
            getResolveOptions(new String[] {"default"}));
        assertTrue(report.hasError());
    }

    public void testLatestMilestone() throws Exception {
        // mod9.2 depends on latest.milestone of mod6.4
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org9/mod9.2/ivys/ivy-1.1.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        // dependencies
        ModuleRevisionId depId = ModuleRevisionId.newInstance("org6", "mod6.4", "3");

        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(1, crr.getDownloadReports(depId).length);

        assertTrue(getIvyFileInCache(depId).exists());
    }

    public void testLatestMilestone2() throws Exception {
        // mod9.2 depends on latest.milestone of mod6.2, but there is no milestone
        // test case for IVY-318
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org9/mod9.2/ivys/ivy-1.2.xml"),
            getResolveOptions(new String[] {"default"}));
        // we should have an error since there is no milestone version, it should be considered as a
        // non resolved dependency
        assertTrue(report.hasError());

        // dependencies
        ConfigurationResolveReport crr = report.getConfigurationReport("default");
        assertNotNull(crr);
        assertEquals(0, crr.getArtifactsNumber());
    }

    public void testIVY56() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bugIVY-56/ivysettings.xml"));

        try {
            ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-56.xml"),
                getResolveOptions(new String[] {"default"}));
            assertNotNull(report);
        } finally {
            FileUtil.forceDelete(ivy.getSettings().getDefaultCache());
        }
    }

    public void testIVY214() throws Exception {
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-214.xml"),
            getResolveOptions(new String[] {"compile"}));

        assertNotNull(report);
        assertFalse(report.hasError());

        assertEquals("Number of artifacts not correct", 1, report.getConfigurationReport("compile")
                .getArtifactsNumber());
    }

    public void testIVY218() throws Exception {
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-218.xml"),
            getResolveOptions(new String[] {"test"}));

        assertNotNull(report);
        assertFalse(report.hasError());

        assertEquals("Number of artifacts not correct", 3, report.getConfigurationReport("test")
                .getArtifactsNumber());
    }

    public void testIVY729() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-729/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-729/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());
    }

    public void testCircular() throws Exception {
        // mod6.3 depends on mod6.2, which itself depends on mod6.3

        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        ivy.getSettings().setCircularDependencyStrategy(
            IgnoreCircularDependencyStrategy.getInstance());
        report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        ivy.getSettings().setCircularDependencyStrategy(
            WarnCircularDependencyStrategy.getInstance());
        report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        ivy.getSettings().setCircularDependencyStrategy(
            ErrorCircularDependencyStrategy.getInstance());
        try {
            ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.0.xml"),
                getResolveOptions(new String[] {"default"}));
            fail("no exception with circular dependency strategy set to error");
        } catch (CircularDependencyException ex) {
            assertEquals("org6#mod6.3;1.0->org6#mod6.2;1.0->org6#mod6.3;latest.integration",
                ex.getMessage());
        }
    }

    public void testCircular2() throws Exception {
        // mod 9.1 (no revision) depends on mod9.2, which depends on mod9.1 2.+

        ResolveReport report = ivy.resolve(new File("test/repositories/circular/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        ivy.getSettings().setCircularDependencyStrategy(
            ErrorCircularDependencyStrategy.getInstance());
        try {
            ivy.resolve(new File("test/repositories/circular/ivy.xml"),
                getResolveOptions(new String[] {"*"}));
            fail("no exception with circular dependency strategy set to error");
        } catch (CircularDependencyException ex) {
            // ok
            assertEquals("org8#mod8.5;NONE->org8#mod8.6;2.+->org8#mod8.5;2.+", ex.getMessage());
        }
    }

    public void testCircular3() throws Exception {
        // test case for IVY-400
        // mod6.3 depends on mod6.2, which itself depends on mod6.3,
        // in both configuration default and test

        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.2.xml"),
            getResolveOptions(new String[] {"default", "test"}));
        assertFalse(report.hasError());
        // we should have mod 6.2 artifact in both configurations
        assertEquals(1, report.getConfigurationReport("default").getArtifactsNumber());
        assertEquals(1, report.getConfigurationReport("test").getArtifactsNumber());

        ivy.getSettings().setCircularDependencyStrategy(
            IgnoreCircularDependencyStrategy.getInstance());
        report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.2.xml"),
            getResolveOptions(new String[] {"default", "test"}));
        assertFalse(report.hasError());
        assertEquals(1, report.getConfigurationReport("default").getArtifactsNumber());
        assertEquals(1, report.getConfigurationReport("test").getArtifactsNumber());

        ivy.getSettings().setCircularDependencyStrategy(
            WarnCircularDependencyStrategy.getInstance());
        report = ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.2.xml"),
            getResolveOptions(new String[] {"default", "test"}));
        assertFalse(report.hasError());
        assertEquals(1, report.getConfigurationReport("default").getArtifactsNumber());
        assertEquals(1, report.getConfigurationReport("test").getArtifactsNumber());

        ivy.getSettings().setCircularDependencyStrategy(
            ErrorCircularDependencyStrategy.getInstance());
        try {
            ivy.resolve(new File("test/repositories/2/mod6.3/ivy-1.2.xml"),
                getResolveOptions(new String[] {"default", "test"}));
            fail("no exception with circular dependency strategy set to error");
        } catch (CircularDependencyException ex) {
            assertEquals("org6#mod6.3;1.2->org6#mod6.2;1.1->...", ex.getMessage());
        }
    }

    public void testRegularCircular() throws Exception {
        // mod11.1 depends on mod11.2 but excludes itself
        // mod11.2 depends on mod11.1
        ivy.getSettings().setCircularDependencyStrategy(
            ErrorCircularDependencyStrategy.getInstance());
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod11.1/ivy-1.0.xml"),
            getResolveOptions(new String[] {"test"}));

        assertNotNull(report);
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org11", "mod11.2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org11", "mod11.2", "1.0", "mod11.2", "jar", "jar")
                .exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org11", "mod11.1", "1.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org11", "mod11.1", "1.0", "mod11.1", "jar", "jar")
                .exists());
    }

    public void testResolveDualChain() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ResolveTest.class.getResource("dualchainresolverconf.xml"));

        DependencyResolver resolver = ivy.getSettings().getResolver("default");
        assertNotNull(resolver);
        assertTrue(resolver instanceof DualResolver);

        // first without cache
        ivy.resolve(ResolveTest.class.getResource("ivy-dualchainresolver.xml"),
            getResolveOptions(new String[] {"default"}));

        assertTrue(new File("build/cache/xerces/xerces/ivy-2.6.2.xml").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());

        // second with cache for ivy file only
        new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").delete();
        new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").delete();
        assertFalse(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertFalse(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());
        ivy.resolve(ResolveTest.class.getResource("ivy-dualchainresolver.xml"),
            getResolveOptions(new String[] {"default"}));

        assertTrue(new File("build/cache/xerces/xerces/ivy-2.6.2.xml").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xmlParserAPIs-2.6.2.jar").exists());
        assertTrue(new File("build/cache/xerces/xerces/jars/xercesImpl-2.6.2.jar").exists());
    }

    public void testBug148() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bug148/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ivy.resolve(ResolveTest.class.getResource("ivy-148.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(new File("build/cache/jtv-foo/bar/ivy-1.1.0.0.xml").exists());
        assertTrue(new File("build/cache/jtv-foo/bar/jars/bar-1.1.0.0.jar").exists());
        assertTrue(new File("build/cache/idautomation/barcode/ivy-4.10.xml").exists());
        assertTrue(new File("build/cache/idautomation/barcode/jars/LinearBarCode-4.10.jar")
                .exists());
    }

    public void testBug148b() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/bug148/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ivy.resolve(ResolveTest.class.getResource("ivy-148b.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(new File("build/cache/jtv-foo/bar/ivy-1.1.0.0.xml").exists());
        assertTrue(new File("build/cache/jtv-foo/bar/jars/bar-1.1.0.0.jar").exists());
        assertTrue(new File("build/cache/idautomation/barcode/ivy-4.10.xml").exists());
        assertTrue(new File("build/cache/idautomation/barcode/jars/LinearBarCode-4.10.jar")
                .exists());
    }

    public void testIVY1178() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1178/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-1178.xml"),
            getResolveOptions(new String[] {"*"}));

        assertNotNull(report);
        assertNotNull(report.getUnresolvedDependencies());
        assertEquals("Number of unresolved dependencies not correct", 0,
            report.getUnresolvedDependencies().length);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("myorg", "modD", "1.1")).exists());
        assertTrue(getArchiveFileInCache("myorg", "modD", "1.1", "modD", "jar", "jar").exists());

        // evicted dependencies
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("myorg", "modD", "1.0"))
                .exists());
        assertFalse(getArchiveFileInCache("myorg", "modD", "1.0", "modD", "jar", "jar").exists());

        // transitive dependencies of modD-1.1 (must not exist: transitive="false" !)
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("myorg", "modE", "1.1"))
                .exists());
        assertFalse(getArchiveFileInCache("myorg", "modE", "1.1", "modE", "jar", "jar").exists());
    }

    public void testIVY1236() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1236/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-1236/ivy.xml"),
            getResolveOptions(new String[] {"*"}));

        assertNotNull(report);
        assertNotNull(report.getUnresolvedDependencies());
        assertEquals("Number of unresolved dependencies not correct", 0,
            report.getUnresolvedDependencies().length);

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("myorg", "modB", "1.0")).exists());
        assertTrue(getArchiveFileInCache("myorg", "modB", "1.0", "modB", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("myorg", "modB", "1.0", "modB-A", "jar", "jar").exists());
    }

    public void testIVY1233() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1233/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport rr = ivy.resolve(new File("test/repositories/IVY-1233/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        ConfigurationResolveReport crr = rr.getConfigurationReport("default");
        Set<ModuleRevisionId> modRevIds = crr.getModuleRevisionIds();
        assertEquals(3, modRevIds.size());
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("test", "a", "1.0")));
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("test", "b", "2.0")));
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("test", "c", "3.0")));
    }

    public void testIVY1333() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1333/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport rr = ivy.resolve(new File("test/repositories/IVY-1333/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        ConfigurationResolveReport crr = rr.getConfigurationReport("default");
        Set<ModuleRevisionId> modRevIds = crr.getModuleRevisionIds();
        assertEquals(3, modRevIds.size());
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("org", "dep1", "1.0")));
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("org", "dep2", "1.0")));

        Map<String, String> extra = new HashMap<String, String>();
        extra.put("o:a", "58701");
        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("org", "badArtifact",
            "1.0.0.m4", extra)));
    }

    public void testIVY1347() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1347/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport rr = ivy.resolve(new File(
                "test/repositories/IVY-1347/childone/childtwo/ivy.xml"),
            getResolveOptions(new String[] {"*"}));
        ModuleDescriptor md = rr.getModuleDescriptor();
        assertNotNull(md);

        ExtendsDescriptor[] parents = md.getInheritedDescriptors();
        assertNotNull(parents);
        assertEquals(1, parents.length);

        ModuleRevisionId parent = parents[0].getParentRevisionId();
        assertEquals(ModuleRevisionId.newInstance("foo", "parent", "1.0"), parent);
    }

    public void testIVY999() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-999/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport rr = ivy.resolve(ResolveTest.class.getResource("ivy-999.xml"),
            getResolveOptions(new String[] {"*"}));
        ConfigurationResolveReport crr = rr.getConfigurationReport("default");
        Set<ModuleRevisionId> modRevIds = crr.getModuleRevisionIds();

        assertTrue(modRevIds.contains(ModuleRevisionId.newInstance("junit", "junit", "4.4")));
        assertFalse(modRevIds.contains(ModuleRevisionId.newInstance("junit", "junit", "3.8")));
    }

    public void testIVY1366() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-1366/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-1366/ivy.xml"),
            new ResolveOptions().setConfs(new String[] {"runtime"}));
        assertFalse(report.hasError());

        List<Artifact> artifacts = report.getArtifacts();
        assertEquals(3, artifacts.size());
        assertEquals("test#a;1!a.jar", artifacts.get(0).toString());
        assertEquals("test#c;1!c.jar", artifacts.get(1).toString());
        assertEquals("test#b;1!b.jar", artifacts.get(2).toString());
    }

    public void testBadFiles() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/badfile/ivysettings.xml"));

        ResolveReport report = ivy.resolve(
            new File("test/repositories/badfile/ivys/ivy-badorg.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("bad org should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "'badorg'") != -1);

        report = ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badmodule.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("bad module should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "'badmodule'") != -1);

        report = ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badbranch.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("bad branch should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "'badbranch'") != -1);

        report = ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badrevision.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("bad revision should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "'badrevision'") != -1);

        report = ivy.resolve(new File("test/repositories/badfile/ivys/ivy-badxml.xml"),
            getResolveOptions(new String[] {"*"}));
        assertTrue("bad xml should have raised an error in report", report.hasError());
        assertTrue(StringUtils.join(report.getAllProblemMessages().toArray(), "\n").indexOf(
            "badatt") != -1);
    }

    public void testTransitiveSetting() throws Exception {
        // mod2.4 depends on mod1.1 with transitive set to false
        // mod1.1 depends on mod1.2, which should not be resolved because of the transitive setting
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.4/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.4", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(!getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(!getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testTransitiveSetting2() throws Exception {
        // test case for IVY-105
        // mod2.7 depends on mod1.1 and mod2.4
        // mod2.4 depends on mod1.1 with transitive set to false
        // mod1.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.7/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolverDirectlyUsingCache() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ResolveTest.class.getResource("badcacheconf.xml"));
        File depIvyFileInCache = getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1",
            "1.0"));
        FileUtil.copy(File.createTempFile("test", "xml"), depIvyFileInCache, null); // creates a
        // fake
        // dependency
        // file in cache
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.4/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));

        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.4", "0.3");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(depIvyFileInCache.exists());
        assertTrue(!getArchiveFileInCache(ivy, "org1", "mod1.1", "1.0", "mod1.1", "jar", "jar")
                .exists());
    }

    public void testVisibility1() throws Exception {
        ivy.resolve(new File("test/repositories/2/mod8.2/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertFalse(getArchiveFileInCache("org8", "mod8.1", "1.0", "a-private", "txt", "txt")
                .exists());
    }

    public void testVisibility2() throws Exception {
        ivy.resolve(new File("test/repositories/2/mod8.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"private"}));

        assertFalse(getArchiveFileInCache("org8", "mod8.1", "1.0", "a-private", "txt", "txt")
                .exists());
        assertTrue(getArchiveFileInCache("org8", "mod8.1", "1.0", "a", "txt", "txt").exists());
    }

    public void testVisibility3() throws Exception {
        ivy.resolve(new File("test/repositories/2/mod8.4/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertFalse(getArchiveFileInCache("org8", "mod8.1", "1.0", "a-private", "txt", "txt")
                .exists());
        assertTrue(getArchiveFileInCache("org8", "mod8.1", "1.0", "a", "txt", "txt").exists());
    }

    public void testVisibility4() throws Exception {
        ivy.resolve(new File("test/repositories/2/mod8.4/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org8", "mod8.1", "1.1", "a-private", "txt", "txt")
                .exists());
        assertTrue(getArchiveFileInCache("org8", "mod8.1", "1.1", "a", "txt", "txt").exists());
    }

    // /////////////////////////////////////////////////////////
    // here comes a series of test provided by Chris Rudd
    // about configuration mapping and eviction
    // /////////////////////////////////////////////////////////

    public void testConfigurationMapping1() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/1/ivy.xml"),
            getResolveOptions(new String[] {"*"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.2", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.2", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.2", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.2", "c-bt", "txt", "txt", conf);
    }

    public void testConfigurationMapping2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/2/ivy.xml"),
            getResolveOptions(new String[] {"*"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.1", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.1", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
    }

    public void testConfigurationMapping3() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/3/ivy.xml"),
            getResolveOptions(new String[] {"buildtime"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("buildtime");

        assertContainsArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
    }

    public void testConfigurationMapping4() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/4/ivy.xml"),
            getResolveOptions(new String[] {"default"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
    }

    public void testConfigurationMapping5() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/5/ivy.xml"),
            getResolveOptions(new String[] {"*"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
    }

    public void testConfigurationMapping6() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/6/ivy.xml"),
            getResolveOptions(new String[] {"default", "buildtime"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
    }

    public void testConfigurationMapping7() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/IVY-84/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-84/tests/7/ivy.xml"),
            getResolveOptions(new String[] {"buildtime", "default"}));

        ConfigurationResolveReport conf = report.getConfigurationReport("default");

        assertContainsArtifact("test", "a", "1.0.2", "a", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "a", "1.0.2", "a-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "b", "1.0.1", "b", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "b", "1.0.1", "b-bt", "txt", "txt", conf);
        assertContainsArtifact("test", "c", "1.0.1", "c", "txt", "txt", conf);
        assertDoesntContainArtifact("test", "c", "1.0.1", "c-bt", "txt", "txt", conf);
    }

    public void testIVY97() throws Exception {
        // mod9.2 depends on mod9.1 and mod1.2
        // mod9.1 depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org9/mod9.2/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org9", "mod9.2", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org9", "mod9.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org9", "mod9.1", "1.0", "mod9.1", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveMultipleSameDependency() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/multiple-same-deps/mod1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("multiple-same-deps", "mod31", "1.0", "mod31", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache("multiple-same-deps", "mod32", "1.0", "mod32", "jar",
            "jar").exists());
        assertTrue(getArchiveFileInCache("multiple-same-deps", "mod33", "1.0", "mod33", "jar",
            "jar").exists());

        ConfigurationResolveReport runtimeReport = report.getConfigurationReport("runtime");
        runtimeReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod31;1.0"));
        runtimeReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod32;1.0"));
        runtimeReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod33;1.0"));
        ConfigurationResolveReport compileReport = report.getConfigurationReport("compile");
        compileReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod31;1.0"));
        compileReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod32;1.0"));
        compileReport.getModuleRevisionIds().contains(
            ModuleRevisionId.parse("multiple-same-deps#mod33;1.0"));
    }

    public void testResolveTransitiveExcludesSimple() throws Exception {
        // mod2.5 depends on mod2.3 and excludes one artifact from mod2.1
        // mod2.3 depends on mod2.1
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.5/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.5", "0.6");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.3", "0.7"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.3", "0.7", "mod2.3", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.1", "0.3"))
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveTransitiveExcludesDiamond1() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5
        // mod2.3 depends on mod2.1 and excludes art21B
        // mod2.5 depends on mod2.1 and excludes art21A
        ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveTransitiveExcludesDiamond2() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5
        // mod2.3 depends on mod2.1 and excludes art21B
        // mod2.5 depends on mod2.1 and excludes art21B
        ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.7.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveTransitiveExcludesDiamond3() throws Exception {
        // mod2.6 depends on mod2.3 and mod2.5 and on mod2.1 for which it excludes art21A
        // mod2.3 depends on mod2.1 and excludes art21B
        // mod2.5 depends on mod2.1 and excludes art21B
        ivy.resolve(new File("test/repositories/1/org2/mod2.6/ivys/ivy-0.8.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveTransitiveExcludes2() throws Exception {
        // mod2.6 depends on mod2.3 for which it excludes art21A
        // mod2.3 depends on mod2.1 and excludes art21B
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.9.xml"),
            getResolveOptions(new String[] {"*"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.9"),
            md.getModuleRevisionId());

        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
    }

    public void testResolveExcludesModule() throws Exception {
        // mod2.6 depends on mod2.1 and excludes mod1.1
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.10.xml"),
            getResolveOptions(new String[] {"*"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.10"),
            md.getModuleRevisionId());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExcludesModuleWide() throws Exception {
        // mod2.6 depends on mod2.1 and excludes mod1.1 module wide
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.11.xml"),
            getResolveOptions(new String[] {"*"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.11"),
            md.getModuleRevisionId());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveExcludesConf() throws Exception {
        // mod2.6 depends on mod2.3 in conf default and mod2.5 in conf exclude
        // mod2.5 depends on mod2.3
        // mod2.6 globally exclude mod2.3 in conf exclude
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.13.xml"),
            getResolveOptions(new String[] {"include"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.13"),
            md.getModuleRevisionId());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.3", "0.4"))
                .exists());
    }

    public void testResolveExcludesConf2() throws Exception {
        // same as testResolveExcludesConf
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.13.xml"),
            getResolveOptions(new String[] {"exclude"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.13"),
            md.getModuleRevisionId());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.3", "0.4"))
                .exists());
    }

    public void testResolveExcludesConf3() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.6/ivys/ivy-0.14.xml"),
            getResolveOptions(new String[] {"exclude"}));
        ModuleDescriptor md = report.getModuleDescriptor();
        assertEquals(ModuleRevisionId.newInstance("org2", "mod2.6", "0.14"),
            md.getModuleRevisionId());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org2", "mod2.5", "0.9"))
                .exists());
    }

    public void testResolveExceptConfiguration() throws Exception {
        // mod10.2 depends on mod5.1 conf *, !A
        ivy.resolve(new File("test/repositories/2/mod10.2/ivy-2.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.1", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.1", "art51B", "jar", "jar").exists());
    }

    public void testResolveIntersectConfiguration1() throws Exception {
        // mod5.2;3.0 -> mod5.1;4.4 (*->@)
        // mod5.1;4.4 -> mod1.2;2.0 (B,xplatform->default)
        // mod5.1;4.4 -> mod2.2;0.9 (B,windows->myconf1;B,linux->myconf2)
        // mod5.1;4.4 -> mod2.1;0.5 (B,windows->A+B)
        ivy.resolve(new File("test/repositories/2/mod5.2/ivy-3.0.xml"),
            getResolveOptions(new String[] {"A"}));

        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "dll", "dll").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "so", "so").exists());
    }

    public void testResolveIntersectConfiguration2() throws Exception {
        // mod5.2;3.0 -> mod5.1;4.4 (*->@)
        // mod5.1;4.4 -> mod1.2;2.0 (B,xplatform->default)
        // mod5.1;4.4 -> mod2.2;0.9 (B,windows->myconf1;B,linux->myconf2)
        // mod5.1;4.4 -> mod2.1;0.5 (B,windows->A+B)
        ivy.resolve(new File("test/repositories/2/mod5.2/ivy-3.0.xml"),
            getResolveOptions(new String[] {"B"}));

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "dll", "dll").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "so", "so").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21AB", "jar", "jar").exists());
    }

    public void testResolveIntersectConfiguration3() throws Exception {
        // mod5.2;3.0 -> mod5.1;4.4 (*->@)
        // mod5.1;4.4 -> mod1.2;2.0 (B,xplatform->default)
        // mod5.1;4.4 -> mod2.2;0.9 (B,windows->myconf1;B,linux->myconf2)
        // mod5.1;4.4 -> mod2.1;0.5 (B,windows->A+B)
        ivy.resolve(new File("test/repositories/2/mod5.2/ivy-3.0.xml"),
            getResolveOptions(new String[] {"B+windows"}));

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "dll", "dll").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "so", "so").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-2", "jar", "jar")
                .exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21AB", "jar", "jar").exists());
    }

    public void testResolveIntersectConfiguration4() throws Exception {
        // mod5.2;3.0 -> mod5.1;4.4 (*->@)
        // mod5.1;4.4 -> mod1.2;2.0 (B,xplatform->default)
        // mod5.1;4.4 -> mod2.2;0.9 (B,windows->myconf1;B,linux->myconf2)
        // mod5.1;4.4 -> mod2.1;0.5 (B,windows->A+B)
        // mod5.1;4.4 -> mod2.8;0.6 (windows,linux->@+thread+debug;A,B->*)
        ivy.resolve(new File("test/repositories/2/mod5.2/ivy-3.0.xml"),
            getResolveOptions(new String[] {"B+linux"}));

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "dll", "dll").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.4", "art51B", "so", "so").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21B", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21AB", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-linux-debug-thread",
            "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-linux-debug", "jar",
            "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-windows-debug-thread",
            "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-windows-debug", "jar",
            "jar").exists());
    }

    public void testConfigurationGroups() throws Exception {
        // mod5.2;3.1 -> mod5.1;4.5 (*->@)
        // mod5.1;4.5 -> mod1.2;2.0 (B,*[axis=platform]->default)
        // mod5.1;4.5 -> mod2.2;0.9 (B,windows->myconf1;B,linux->myconf2)
        // mod5.1;4.5 -> mod2.1;0.5 (B,windows->A+B)
        // mod5.1;4.5 -> mod2.8;0.6 (windows,linux->@+thread+debug;A,B->*)
        ivy.resolve(new File("test/repositories/2/mod5.2/ivy-3.1.xml"),
            getResolveOptions(new String[] {"B+linux"}));

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.5", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.5", "art51B", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.5", "art51B", "dll", "dll").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.5", "art51B", "so", "so").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-2", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21B", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.1", "0.5", "art21AB", "jar", "jar")
                .exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-linux-debug-thread",
            "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-linux-debug", "jar",
            "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-windows-debug-thread",
            "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org2", "mod2.8", "0.6", "art28-windows-debug", "jar",
            "jar").exists());
    }

    public void testResolveFallbackConfiguration() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(default)
        ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
    }

    public void testResolveFallbackConfiguration2() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(*)
        ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.1.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testResolveFallbackConfiguration3() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime(*),compile(*)
        ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.2.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testResolveFallbackConfiguration4() throws Exception {
        // mod10.2 depends on mod5.1 conf runtime()
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod10.2/ivy-1.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertFalse(report.hasError());

        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51A", "jar", "jar").exists());
        assertFalse(getArchiveFileInCache("org5", "mod5.1", "4.0", "art51B", "jar", "jar").exists());
    }

    public void testResolveMaven2() throws Exception {
        // test3 depends on test2 which depends on test
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test3/1.0/test3-1.0.pom"),
            getResolveOptions(new String[] {"test"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test3", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar", "jar")
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                .exists());
    }

    public void testResolveMaven2WithConflict() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test3/1.1/test3-1.1.pom"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.1", "test2", "jar", "jar")
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.1"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.1", "test", "jar", "jar")
                .exists());

        assertContainsArtifact(report.getConfigurationReport("default"),
            getArtifact("org.apache", "test2", "1.1", "test2", "jar", "jar"));
        assertContainsArtifact(report.getConfigurationReport("default"),
            getArtifact("org.apache", "test", "1.1", "test", "jar", "jar"));
    }

    public void testResolveMaven2WithConflict2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-874.xml"),
            getResolveOptions(new String[] {"default"}));
        assertFalse(report.hasError());

        assertContainsArtifact(report.getConfigurationReport("default"),
            getArtifact("org.apache", "test3", "1.1", "test3", "jar", "jar"));
        assertContainsArtifact(report.getConfigurationReport("default"),
            getArtifact("org.apache", "test2", "1.1", "test2", "jar", "jar"));
        assertContainsArtifact(report.getConfigurationReport("default"),
            getArtifact("org.apache", "test", "1.2", "test", "jar", "jar"));
    }

    public void testResolveMaven2RelocationOfGroupId() throws Exception {
        // Same as testResolveMaven2 but with a relocated module pointing to the module
        // used in testResolveMaven2.
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ivy.pushContext();
        try {
            ResolveReport report = ivy.resolve(new File(
                    "test/repositories/m2/org/relocated/test3/1.0/test3-1.0.pom"),
                getResolveOptions(new String[] {"*"}));
            assertNotNull(report);

            // dependencies
            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar",
                "jar").exists());

            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                    .exists());
        } finally {
            ivy.popContext();
        }
    }

    public void testResolveMaven2FullRelocation() throws Exception {
        // Same as testResolveMaven2 but with a relocated module pointing to the module
        // used in testResolveMaven2.
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ivy.pushContext();
        try {
            ResolveReport report = ivy.resolve(new File(
                    "test/repositories/m2/org/relocated/test3full/1.1/test3full-1.1.pom"),
                getResolveOptions(new String[] {"*"}));
            assertNotNull(report);

            // dependencies
            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar",
                "jar").exists());

            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                    .exists());
        } finally {
            ivy.popContext();
        }
    }

    public void testResolveVesionRelocationChainedWithGroupRelocation() throws Exception {
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ivy.pushContext();
        try {
            ResolveReport report = ivy.resolve(new File(
                    "test/repositories/m2/org/relocated/test3/1.1/test3-1.1.pom"),
                getResolveOptions(new String[] {"*"}));
            assertNotNull(report);

            // dependencies
            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar",
                "jar").exists());

            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                    .exists());
        } finally {
            ivy.popContext();
        }
    }

    public void testResolveTransitivelyToRelocatedPom() throws Exception {
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ivy.pushContext();
        try {
            ResolveReport report = ivy.resolve(new File(
                    "test/repositories/m2/org/relocated/testRelocationUser/1.0/"
                            + "testRelocationUser-1.0.pom"),
                getResolveOptions(new String[] {"compile"}));
            assertNotNull(report);
            assertFalse(report.hasError());
            // dependencies
            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar",
                "jar").exists());

            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                    .exists());
        } finally {
            ivy.popContext();
        }
    }

    public void testResolveTransitivelyToPomRelocatedToNewVersion() throws Exception {
        ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ivy.pushContext();
        try {
            ResolveReport report = ivy.resolve(new File(
                    "test/repositories/m2/org/relocated/testRelocationUser/1.1/"
                            + "testRelocationUser-1.1.pom"),
                getResolveOptions(new String[] {"compile"}));
            assertNotNull(report);
            assertFalse(report.hasError());
            // dependencies
            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test2", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test2", "1.0", "test2", "jar",
                "jar").exists());

            assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test", "1.0"))
                    .exists());
            assertTrue(getArchiveFileInCache(ivy, "org.apache", "test", "1.0", "test", "jar", "jar")
                    .exists());
        } finally {
            ivy.popContext();
        }
    }

    public void testResolveMaven2Classifiers() throws Exception {
        // test case for IVY-418
        // test-classifier depends on test-classified with classifier asl
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test-classifier/1.0/test-classifier-1.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance("org.apache", "test-classifier", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-classified", "1.0")).exists());

        Map<String, String> cmap = new HashMap<String, String>();
        cmap.put("classifier", "asl");
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-classified", null /* branch */
        , "1.0", "test-classified", "jar", "jar", cmap).exists());
    }

    public void testResolveMaven2ClassifiersWithoutPOM() throws Exception {
        // test case for IVY-1041
        // test-classifier depends on test-classified with classifier asl
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test-classifier/2.0/test-classifier-2.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance("org.apache", "test-classifier", "2.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-classified", "2.0")).exists());

        Map<String, String> cmap = new HashMap<String, String>();
        cmap.put("classifier", "asl");
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-classified", null /* branch */
        , "2.0", "test-classified", "jar", "jar", cmap).exists());
    }

    public void testResolveMaven2GetSources() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-m2-with-sources.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-sources", "1.0")).exists());
        File jarFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-sources", "1.0",
            "test-sources", "jar", "jar");
        assertTrue(jarFileInCache.exists());
        File sourceFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-sources", null,
            "1.0", "test-sources", "source", "jar",
            Collections.singletonMap("classifier", "sources"));
        assertTrue(sourceFileInCache.exists());
        assertTrue(jarFileInCache.length() != sourceFileInCache.length());
    }

    public void testResolveMaven2GetSourcesWithSrcClassifier() throws Exception {
        // IVY-1138
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-m2-with-src.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache", "test-src", "1.0"))
                .exists());
        File jarFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-src", "1.0",
            "test-src", "jar", "jar");
        assertTrue(jarFileInCache.exists());
        File sourceFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-src", null, "1.0",
            "test-src", "source", "jar", Collections.singletonMap("classifier", "src"));
        assertTrue(sourceFileInCache.exists());
        assertTrue(jarFileInCache.length() != sourceFileInCache.length());
    }

    public void testResolveMaven2GetSourcesAndJavadocAuto() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-m2-with-sources-and-javadoc-auto.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-sources", "1.0")).exists());
        File jarFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-sources", "1.0",
            "test-sources", "jar", "jar");
        assertTrue(jarFileInCache.exists());
        File sourceFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-sources", null,
            "1.0", "test-sources", "source", "jar",
            Collections.singletonMap("classifier", "sources"));
        assertTrue(sourceFileInCache.exists());
        assertTrue(jarFileInCache.length() != sourceFileInCache.length());
        File javadocFileInCache = getArchiveFileInCache(ivy, "org.apache", "test-sources", null,
            "1.0", "test-sources", "javadoc", "jar",
            Collections.singletonMap("classifier", "javadoc"));
        assertTrue(javadocFileInCache.exists());
        assertTrue(jarFileInCache.length() != javadocFileInCache.length());
    }

    public void testResolveMaven2WithVersionProperty() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test-version/1.0/test-version-1.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test-version", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-classifier", "1.0")).exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-classifier", "1.0",
            "test-classifier", "jar", "jar").exists());
    }

    public void testResolveMaven2ParentPomChainResolver() throws Exception {
        // test has a dependency on test2 but there is no version listed. test has a parent of
        // parent(2.0)
        // then parent2. Both parents have a dependencyManagement element for test2, and each list
        // the version as
        // ${pom.version}. The parent version should take precidence over parent2,
        // so the version should be test2 version 2.0. Test3 is also a dependency of parent, and
        // it's version is listed
        // as 1.0 in parent2 (dependencies inherited from parent comes after).
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/parentPom/ivysettings.xml"));
        ivy.getSettings().setDefaultResolver("parentChain");

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/parentPom/org/apache/dm/test/1.0/test-1.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.dm", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // test the report to make sure the right dependencies are listed
        List<IvyNode> dependencies = report.getDependencies();
        assertEquals(2, dependencies.size());

        IvyNode ivyNode;
        ivyNode = dependencies.get(0);
        assertNotNull(ivyNode);

        // Bad assertions based on IVY-1301 bug, corrected below:
        // mrid = ModuleRevisionId.newInstance("org.apache.dm", "test2", "2.0");
        // assertEquals(mrid, ivyNode.getId());
        // // dependencies
        // assertTrue(getIvyFileInCache(
        // ModuleRevisionId.newInstance("org.apache.dm", "test2", "2.0")).exists());
        // assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test2", "2.0",
        // "test2", "jar", "jar").exists());

        mrid = ModuleRevisionId.newInstance("org.apache.dm", "test2", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test2", "1.0", "test2", "jar",
            "jar").exists());

        ivyNode = dependencies.get(1);
        assertNotNull(ivyNode);
        mrid = ModuleRevisionId.newInstance("org.apache.dm", "test3", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test3", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test3", "1.0", "test3", "jar",
            "jar").exists());
    }

    public void testResolveMaven2ParentPomWithNamespace() throws Exception {
        // Cfr IVY-1186
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/parentPom/ivysettings-namespace.xml"));

        ResolveReport report = ivy.resolve(
            ModuleRevisionId.newInstance("org.apache.systemDm", "test", "1.0"),
            getResolveOptions(new String[] {"*(public)"}), true);
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        // assertEquals(mrid, md.getModuleRevisionId());
        // assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // test the report to make sure the right dependencies are listed
        List<IvyNode> dependencies = report.getDependencies();
        assertEquals(3, dependencies.size()); // the test module + it's 2 dependencies

        IvyNode ivyNode = dependencies.get(0);
        assertNotNull(ivyNode);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.systemDm", "test", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache.systemDm", "test", "1.0")).exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.systemDm", "test", "1.0", "test", "jar",
            "jar").exists());

        ivyNode = dependencies.get(1);
        assertNotNull(ivyNode);
        mrid = ModuleRevisionId.newInstance("org.apache.systemDm", "test2", "2.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache.systemDm", "test2", "2.0")).exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.systemDm", "test2", "2.0", "test2",
            "jar", "jar").exists());

        ivyNode = dependencies.get(2);
        assertNotNull(ivyNode);
        mrid = ModuleRevisionId.newInstance("org.apache.systemDm", "test3", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache.systemDm", "test3", "1.0")).exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.systemDm", "test3", "1.0", "test3",
            "jar", "jar").exists());
    }

    public void testResolveMaven2ParentPomDualResolver() throws Exception {
        // test has a dependency on test2 but there is no version listed. test has a parent of
        // parent(2.0)
        // then parent2. Both parents have a dependencyManagement element for test2, and each list
        // the version as
        // ${pom.version}. The parent version should take precidence over parent2,
        // so the version should be test2 version 2.0. Test3 is also a dependency of parent, and
        // it's version is listed
        // as 1.0 in parent2. (dependencies inherited from parent comes after)

        // now run tests with dual resolver
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/parentPom/ivysettings.xml"));
        ivy.getSettings().setDefaultResolver("parentDual");

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/parentPom/org/apache/dm/test/1.0/test-1.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.dm", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // test the report to make sure the right dependencies are listed
        List<IvyNode> dependencies = report.getDependencies();
        assertEquals(2, dependencies.size());

        IvyNode ivyNode;
        ivyNode = dependencies.get(0);
        assertNotNull(ivyNode);

        // Bad assertions based on IVY-1301 bug, corrected below:
        // mrid = ModuleRevisionId.newInstance("org.apache.dm", "test2", "2.0");
        // assertEquals(mrid, ivyNode.getId());
        // // dependencies
        // assertTrue(getIvyFileInCache(
        // ModuleRevisionId.newInstance("org.apache.dm", "test2", "2.0")).exists());
        // assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test2", "2.0",
        // "test2", "jar", "jar").exists());

        mrid = ModuleRevisionId.newInstance("org.apache.dm", "test2", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test2", "1.0", "test2", "jar",
            "jar").exists());

        ivyNode = dependencies.get(1);
        assertNotNull(ivyNode);
        mrid = ModuleRevisionId.newInstance("org.apache.dm", "test3", "1.0");
        assertEquals(mrid, ivyNode.getId());
        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test3", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test3", "1.0", "test3", "jar",
            "jar").exists());
    }

    public void testResolveMaven2ParentPomDependencyManagementOverrideTransitiveVersion()
            throws Exception {
        // test;2.0 has a dependency on test2;3.0.
        // test has a parent of parent(2.0) then parent2.
        // Both parents have a dependencyManagement element for test2, and each list the version as
        // ${pom.version}. The version for test2 in test should take precedance,
        // so the version should be test2 version 3.0.
        // test2;3.0 -> test4;2.0, but parent has a dependencyManagement section specifying
        // test4;1.0.
        // since maven 2.0.6, the information in parent should override transitive dependency
        // version,
        // and thus we should get test4;1.0
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/parentPom/ivysettings.xml"));
        ivy.getSettings().setDefaultResolver("parentChain");

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/parentPom/org/apache/dm/test/2.0/test-2.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // test the report to make sure the right dependencies are listed
        List<IvyNode> dependencies = report.getDependencies();
        assertEquals(3, dependencies.size());

        IvyNode ivyNode;
        ivyNode = dependencies.get(0);
        assertNotNull(ivyNode);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.dm", "test2", "3.0");
        assertEquals(mrid, ivyNode.getId());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test2", "3.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test2", "3.0", "test2", "jar",
            "jar").exists());

        ivyNode = dependencies.get(2);
        assertNotNull(ivyNode);
        mrid = ModuleRevisionId.newInstance("org.apache.dm", "test4", "1.0");
        assertEquals(mrid, ivyNode.getId());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test4", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test4", "1.0", "test4", "jar",
            "jar").exists());
    }

    public void testResolveMaven2ParentPomDependencyManagementWithImport() throws Exception {
        // IVY-1376
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/parentPom/ivysettings.xml"));
        ivy.getSettings().setDefaultResolver("parentChain");

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/parentPom/org/apache/dm/test/3.0/test-3.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);

        // test the report to make sure the right dependencies are listed
        List<IvyNode> dependencies = report.getDependencies();
        assertFalse(report.hasError());
        assertEquals(2, dependencies.size());

        IvyNode ivyNode;
        ivyNode = dependencies.get(0);
        assertNotNull(ivyNode);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.dm", "test5", "2.0");
        assertEquals(mrid, ivyNode.getId());
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org.apache.dm", "test5", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache.dm", "test5", "2.0", "test5", "jar",
            "jar").exists());
    }

    public void testResolveMaven2Snapshot1() throws Exception {
        // test case for IVY-501
        // here we test maven SNAPSHOT versions handling,
        // with m2 snapshotRepository/uniqueVersion set to true
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test4/1.0/test4-1.0.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT1", "2.0.2-SNAPSHOT"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-SNAPSHOT1", "2.0.2-SNAPSHOT",
            "test-SNAPSHOT1", "jar", "jar").exists());
    }

    public void testResolveMaven2Snapshot1AsLatestIntegration() throws Exception {
        // test case for IVY-1036
        // here we test maven SNAPSHOT versions handling,
        // with m2 snapshotRepository/uniqueVersion set to true
        // but retrieving by latest.integration
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT1", "latest.integration"),
            getResolveOptions(new String[] {"*(public)"}), true);
        assertNotNull(report);
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT1", "2.0.2-SNAPSHOT"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-SNAPSHOT1", "2.0.2-SNAPSHOT",
            "test-SNAPSHOT1", "jar", "jar").exists());
    }

    public void testResolveMaven2Snapshot2() throws Exception {
        // test case for IVY-501
        // here we test maven SNAPSHOT versions handling,
        // without m2 snapshotRepository/uniqueVersion set to true
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/m2/org/apache/test4/1.1/test4-1.1.pom"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT2", "2.0.2-SNAPSHOT"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-SNAPSHOT2", "2.0.2-SNAPSHOT",
            "test-SNAPSHOT2", "jar", "jar").exists());
    }

    public void testResolveMaven2Snapshot2AsLatestIntegration() throws Exception {
        // test case for IVY-1036
        // here we test maven SNAPSHOT versions handling,
        // with m2 snapshotRepository/uniqueVersion set to true
        // but retrieving by latest.integration
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/m2/ivysettings.xml"));
        ResolveReport report = ivy.resolve(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT2", "latest.integration"),
            getResolveOptions(new String[] {"*(public)"}), true);
        assertNotNull(report);
        assertFalse(report.hasError());

        // dependencies
        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("org.apache", "test-SNAPSHOT2", "2.0.2-SNAPSHOT"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "org.apache", "test-SNAPSHOT2", "2.0.2-SNAPSHOT",
            "test-SNAPSHOT2", "jar", "jar").exists());
    }

    public void testNamespaceMapping() throws Exception {
        // the dependency is in another namespace
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "namespace", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // ivy file
        File ivyFile = getIvyFileInCache(ModuleRevisionId.newInstance("systemorg", "systemmod",
            "1.0"));
        assertTrue(ivyFile.exists());
        ModuleDescriptor parsedMD = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), ivyFile.toURI().toURL(), true);
        assertEquals("systemorg", parsedMD.getModuleRevisionId().getOrganisation());
        assertEquals("systemmod", parsedMD.getModuleRevisionId().getName());

        // dependencies
        assertTrue(getArchiveFileInCache(ivy, "systemorg", "systemmod", "1.0", "A", "jar", "jar")
                .exists());
    }

    public void testNamespaceMapping2() throws Exception {
        // the dependency is in another namespace and has itself a dependency on a module available
        // in the same namespace
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "namespace", "2.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("systemorg", "systemmod2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "systemorg", "systemmod2", "1.0", "B", "jar", "jar")
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("systemorg", "systemmod", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "systemorg", "systemmod", "1.0", "A", "jar", "jar")
                .exists());
    }

    public void testNamespaceMapping3() throws Exception {
        // same as 2 but with poms
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "namespace", "3.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(getResolvedIvyFileInCache(mrid).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("systemorg2", "system-2", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "systemorg2", "system-2", "1.0", "2", "jar", "jar")
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("systemorg2", "system-1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache(ivy, "systemorg2", "system-1", "1.0", "1", "jar", "jar")
                .exists());
    }

    public void testNamespaceMapping4() throws Exception {
        // same as 2 but with incorrect dependency asked: the first ivy file asks for a dependency
        // in the resolver namespace and not the system one: this should fail
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace4.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull(md);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "namespace", "4.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertTrue(report.hasError());
    }

    public void testNamespaceMapping5() throws Exception {
        // Verifies that mapping version numbers works
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/namespace/ivysettings.xml"));
        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-namespace5.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("systemorg3", "systemmod3", "1.0");
        assertTrue(getIvyFileInCache(mrid).exists());
    }

    public void testIVY151() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/multirevisions/ivysettings.xml"));
        ResolveReport report = ivy.resolve(new File("test/repositories/multirevisions/ivy.xml"),
            getResolveOptions(new String[] {"compile", "test"}));

        assertNotNull(report);
        assertNotNull(report.getUnresolvedDependencies());
        assertEquals("Number of unresolved dependencies not correct", 0,
            report.getUnresolvedDependencies().length);
    }

    public void testCheckRevision() throws Exception {
        // mod12.2 depends on mod12.1 1.0 which depends on mod1.2
        // mod12.1 doesn't have revision in its ivy file
        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod12.2/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertTrue(report.hasError());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org12", "mod12.1", "1.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org12", "mod12.1", "1.0", "mod12.1", "jar", "jar")
                .exists());

        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertFalse(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testTrustRevision() throws Exception {
        // mod12.2 depends on mod12.1 1.0 which depends on mod1.2
        // mod12.1 doesn't have revision in its ivy file

        ((BasicResolver) ivy.getSettings().getResolver("2-ivy")).setCheckconsistency(false);

        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod12.2/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertFalse(report.hasError());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org12", "mod12.1", "1.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org12", "mod12.1", "1.0", "mod12.1", "jar", "jar")
                .exists());

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testTransitiveConfMapping() throws Exception {
        // IVY-168
        // mod13.3 depends on mod13.2 which depends on mod13.1
        // each module has two confs: j2ee and compile
        // each module only publishes one artifact in conf compile
        // each module has the following conf mapping on its dependencies: *->@
        // moreover, mod13.1 depends on mod1.2 in with the following conf mapping: compile->default
        // thus conf j2ee should be empty for each modules

        ResolveReport report = ivy.resolve(new File("test/repositories/2/mod13.3/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));

        assertFalse(report.hasError());

        assertEquals(3, report.getConfigurationReport("compile").getArtifactsNumber());
        assertEquals(0, report.getConfigurationReport("j2ee").getArtifactsNumber());
    }

    public void testExtraAttributes() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/extra-attributes/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        FileSystemResolver fResolver = (FileSystemResolver) ivy.getSettings().getDefaultResolver();
        fResolver.setCheckconsistency(false); // important for testing IVY-929

        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(new File(cache, "apache/mymodule/task1/1854/ivy.xml").exists());
        assertTrue(new File(cache, "apache/mymodule/task1/1854/mymodule-windows.jar").exists());
        assertTrue(new File(cache, "apache/mymodule/task1/1854/mymodule-linux.jar").exists());

        Set<ModuleRevisionId> moduleRevisions = report.getConfigurationReport("default")
                .getModuleRevisionIds();
        assertEquals(1, moduleRevisions.size());
        ModuleRevisionId resolveModRevId = moduleRevisions.iterator().next();
        assertEquals("apache", resolveModRevId.getOrganisation());
        assertEquals("mymodule", resolveModRevId.getName());
        assertEquals("1854", resolveModRevId.getRevision());
        assertEquals("task1", resolveModRevId.getExtraAttribute("eatt"));
        assertEquals("another", resolveModRevId.getExtraAttribute("eatt2"));
    }

    public void testExtraAttributes2() throws Exception {
        // test case for IVY-773
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/extra-attributes/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);
        ivy.getSettings().validate();

        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att2.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(new File(cache, "apache/mymodule/task2/1748/ivy.xml").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1748/ivy.xml.original").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1748/mymodule-windows.jar").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1748/mymodule-linux.jar").exists());

        assertTrue(new File(cache, "apache/module2/task2/1976/ivy.xml").exists());
        assertTrue(new File(cache, "apache/module2/task2/1976/module2-windows.jar").exists());
        assertTrue(new File(cache, "apache/module2/task2/1976/module2-linux.jar").exists());
    }

    public void testExtraAttributes3() throws Exception {
        // test case for IVY-745
        MockMessageLogger mockLogger = new MockMessageLogger();
        Ivy ivy = new Ivy();
        ivy.getLoggerEngine().setDefaultLogger(mockLogger);
        ivy.configure(new File("test/repositories/extra-attributes/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);
        ivy.getSettings().validate();

        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att3.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));

        assertTrue(report.hasError());
        // should report error about missing extra attribute in dependency module descriptor
        mockLogger.assertLogContains("expected='task2' found='null'");
    }

    public void testExtraAttributes4() throws Exception {
        // second test case for IVY-745: now we disable consistency checking, everything should work
        // fine
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/extra-attributes/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);
        ((FileSystemResolver) ivy.getSettings().getResolver("default")).setCheckconsistency(false);
        ivy.getSettings().validate();

        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att3.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));

        assertFalse(report.hasError());

        assertTrue(new File(cache, "apache/mymodule/task2/1749/ivy.xml").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1749/ivy.xml.original").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1749/mymodule-windows.jar").exists());
        assertTrue(new File(cache, "apache/mymodule/task2/1749/mymodule-linux.jar").exists());
    }

    public void testNamespaceExtraAttributes() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/extra-attributes/ivysettings.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(ResolveTest.class.getResource("ivy-extra-att-ns.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(true));
        assertFalse(report.hasError());

        assertTrue(new File(cache, "apache/mymodule/task1/1855/ivy.xml").exists());
        assertTrue(new File(cache, "apache/mymodule/task1/1855/mymodule-windows.jar").exists());
        assertTrue(new File(cache, "apache/mymodule/task1/1855/mymodule-linux.jar").exists());
    }

    public void testBranches1() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/1/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#trunk;3", "foo1", "jar", "jar").exists());
    }

    public void testBranches2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/2/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#branch1;4", "foo1", "jar", "jar").exists());
    }

    public void testBranches3() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings-defaultbranch1.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/1/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#branch1;4", "foo1", "jar", "jar").exists());
    }

    public void testBranches4() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/3/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#trunk;3", "foo1", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "bar#bar2#trunk;2", "bar2", "jar", "jar").exists());
    }

    public void testBranches5() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings-fooonbranch1.xml"));

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/3/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#branch1;4", "foo1", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "bar#bar2#trunk;2", "bar2", "jar", "jar").exists());
    }

    public void testBranches6() throws Exception {
        // test case for IVY-717

        // bar1;4 -> foo#foo1#${ivy.branch};5
        // foo#foo1#branch1;5 -> foo#foo2#${ivy.branch};1
        // foo#foo1#trunk;5 -> {}

        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/branches/ivysettings.xml"));

        ivy.setVariable("ivy.branch", "branch1");
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/branches/bar/bar1/trunk/4/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#branch1;5", "foo1", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "foo#foo2#branch1;1", "foo2", "jar", "jar").exists());

        ivy.setVariable("ivy.branch", "trunk");
        report = ivy.resolve(new File("test/repositories/branches/bar/bar1/trunk/4/ivy.xml"),
            getResolveOptions(new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());
        assertEquals(1, report.getConfigurationReport("default").getNodesNumber());
        assertTrue(getArchiveFileInCache(ivy, "foo#foo1#trunk;5", "foo1", "jar", "jar").exists());
    }

    public void testExternalArtifacts() throws Exception {
        Ivy ivy = Ivy.newInstance();
        ivy.getSettings().setVariable("test.base.url",
            new File("test/repositories/external-artifacts").toURI().toURL().toExternalForm());
        ivy.configure(new File("test/repositories/external-artifacts/ivysettings.xml"));

        ResolveReport report = ivy.resolve(
            new File("test/repositories/external-artifacts/ivy.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        assertTrue(getArchiveFileInCache(ivy, "apache", "A", "1.0", "a", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "apache", "B", "2.0", "b", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache(ivy, "apache", "C", "3.0", "C", "jar", "jar").exists());
    }

    public void testResolveWithMultipleIvyPatterns() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File("test/repositories/multi-ivypattern/ivysettings.xml"));

        ModuleRevisionId module = ModuleRevisionId.newInstance("org1", "mod1.1", "1.+");

        // use non-default options and settings
        ivy.getSettings().setDefaultUseOrigin(true);
        ResolveOptions options = getResolveOptions(ivy.getSettings(), new String[] {"*"});
        options.setTransitive(false);
        options.setDownload(false);
        ResolveReport report = ivy.getResolveEngine().resolve(module, options, false);

        List<IvyNode> dependencies = report.getDependencies();
        assertNotNull(dependencies);
        assertEquals(1, dependencies.size());
        IvyNode dependency = dependencies.get(0);
        assertEquals("1.1", dependency.getResolvedId().getRevision());
    }

    public void testPrivateConfigurationTransferWhenConflict() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/orgConflictAndPrivateConf/root/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"assembly"}));
        assertFalse(report.hasError());
    }

    // //////////////////////////////////////////////////////////
    // helper methods to ease the tests
    // //////////////////////////////////////////////////////////

    private void assertContainsArtifact(String org, String module, String rev, String artName,
            String type, String ext, ConfigurationResolveReport conf) {
        Artifact art = getArtifact(org, module, rev, artName, type, ext);
        assertContainsArtifact(conf, art);
    }

    private void assertContainsArtifact(ConfigurationResolveReport conf, Artifact art) {
        if (!containsArtifact(art, conf.getDownloadedArtifactsReports())) {
            fail("artifact " + art + " should be part of " + conf.getConfiguration() + " from "
                    + conf.getModuleDescriptor().getModuleRevisionId());
        }
    }

    private void assertDoesntContainArtifact(String org, String module, String rev, String artName,
            String type, String ext, ConfigurationResolveReport conf) {
        Artifact art = getArtifact(org, module, rev, artName, type, ext);
        if (containsArtifact(art, conf.getDownloadedArtifactsReports())) {
            fail("artifact " + art + " should NOT be part of " + conf.getConfiguration() + " from "
                    + conf.getModuleDescriptor().getModuleRevisionId());
        }
    }

    private Artifact getArtifact(String org, String module, String rev, String artName,
            String type, String ext) {
        return new DefaultArtifact(ModuleRevisionId.newInstance(org, module, rev), new Date(),
                artName, type, ext);
    }

    private boolean containsArtifact(Artifact art, ArtifactDownloadReport[] adr) {
        for (int i = 0; i < adr.length; i++) {
            Artifact artifact = adr[i].getArtifact();
            if (artifact.getModuleRevisionId().equals(art.getModuleRevisionId())
                    && artifact.getName().equals(art.getName())
                    && artifact.getType().equals(art.getType())
                    && artifact.getExt().equals(art.getExt())) {
                return true;
            }
        }
        return false;
    }

    private String getArchivePathInCache(Artifact artifact) {
        return getRepositoryCacheManager(artifact.getModuleRevisionId()).getArchivePathInCache(
            artifact);
    }

    private String getArchivePathInCache(Artifact artifact, ArtifactOrigin origin) {
        return getRepositoryCacheManager(artifact.getModuleRevisionId()).getArchivePathInCache(
            artifact, origin);
    }

    private File getArchiveFileInCache(Artifact artifact) {
        return getRepositoryCacheManager(artifact.getModuleRevisionId()).getArchiveFileInCache(
            artifact);
    }

    private ArtifactOrigin getSavedArtifactOrigin(Artifact artifact) {
        return getRepositoryCacheManager(artifact.getModuleRevisionId()).getSavedArtifactOrigin(
            artifact);
    }

    private File getIvyFileInCache(ModuleRevisionId id) {
        return getRepositoryCacheManager(id).getIvyFileInCache(id);
    }

    private File getIvyFileInCache(Ivy ivy, ModuleRevisionId id) {
        return TestHelper.getRepositoryCacheManager(ivy, id).getIvyFileInCache(id);
    }

    private DefaultRepositoryCacheManager getRepositoryCacheManager(ModuleRevisionId id) {
        return TestHelper.getRepositoryCacheManager(ivy, id);
    }

    private File getResolvedIvyFileInCache(ModuleRevisionId mrid) {
        return ivy.getResolutionCacheManager().getResolvedIvyFileInCache(mrid);
    }

    private File getResolvedIvyFileInCache(Ivy ivy, ModuleRevisionId mrid) {
        return ivy.getResolutionCacheManager().getResolvedIvyFileInCache(mrid);
    }

    private File getConfigurationResolveReportInCache(String resolveId, String conf) {
        return ivy.getResolutionCacheManager()
                .getConfigurationResolveReportInCache(resolveId, conf);
    }

    private File getConfigurationResolveReportInCache(Ivy ivy, String resolveId, String conf) {
        return ivy.getResolutionCacheManager()
                .getConfigurationResolveReportInCache(resolveId, conf);
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifactName, String type, String ext) {
        return getArchiveFileInCache(ivy, organisation, module, revision, artifactName, type, ext);
    }

    private File getArchiveFileInCache(Ivy ivy, String organisation, String module, String branch,
            String revision, String artifactName, String type, String ext,
            Map<String, String> extraAttrs) {
        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance(organisation, module, branch, revision);
        DefaultArtifact artifact = new DefaultArtifact(mrid, new Date(), artifactName, type, ext,
                extraAttrs);
        return TestHelper.getRepositoryCacheManager(ivy, mrid).getArchiveFileInCache(artifact);
    }

    private File getArchiveFileInCache(Ivy ivy, String organisation, String module,
            String revision, String artifactName, String type, String ext) {
        return TestHelper.getArchiveFileInCache(ivy, organisation, module, revision, artifactName,
            type, ext);
    }

    private File getArchiveFileInCache(Ivy ivy, String mrid, String artifactName, String type,
            String ext) {
        return TestHelper.getArchiveFileInCache(ivy, mrid, artifactName, type, ext);
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return getResolveOptions(ivy.getSettings(), confs);
    }

    private ResolveOptions getResolveOptions(IvySettings settings, String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }

    public void testExtraAttributesForcedDependencies() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-forceddependencies/ivysettings-filerepo-attribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-attrib-forced-dependencies.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        ivy.deliver("1.0.0", deliverDir.getAbsolutePath() + "/ivy-1.0.0.xml", new DeliverOptions()
                .setResolveId(report.getResolveId()).setValidate(false).setPubdate(new Date()));

        File deliveredIvyFile = new File(deliverDir, "ivy-1.0.0.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(
            ModuleRevisionId.newInstance("CAE-Visualization-Components", "SGL", "MAIN", "6.2.34.7"),
            dds[1].getDependencyRevisionId());

    }

    public void testNoAttributesForcedDependencies() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-forceddependencies/ivysettings-filerepo-noattribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-attrib-forced-dependencies.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());

        ivy.deliver("1.0.0", deliverDir.getAbsolutePath() + "/ivy-1.0.0.xml", new DeliverOptions()
                .setResolveId(report.getResolveId()).setValidate(false).setPubdate(new Date()));

        File deliveredIvyFile = new File(deliverDir, "ivy-1.0.0.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(
            ModuleRevisionId.newInstance("CAE-Visualization-Components", "SGL", "MAIN", "6.2.34.7"),
            dds[1].getDependencyRevisionId());
    }

    public void testExtraAttributesMultipleDependenciesHang() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-multipledependencies/ivysettings-filerepo-attribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-att-multipledependencies.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());
    }

    public void testExtraAttributesMultipleDependenciesNoHang() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-multipledependencies/ivysettings-filerepo-noattribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-att-multipledependencies.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());
    }

    public void testExtraAttributesMultipleDependenciesHang2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-multipledependencies/ivysettings-filerepo-attribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-att-multipledependencies2.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());
    }

    public void testExtraAttributesMultipleDependenciesNoHang2() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(new File(
                "test/repositories/extra-attributes-multipledependencies/ivysettings-filerepo-noattribs.xml"));
        ivy.getSettings().setDefaultCache(cache);

        ResolveReport report = ivy.resolve(
            ResolveTest.class.getResource("ivy-extra-att-multipledependencies2.xml"),
            getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
        assertFalse(report.hasError());
    }

    public void testIVY956() throws Exception {
        ivy.getSettings().setDefaultConflictManager(
            ivy.getSettings().getConflictManager("latest-compatible"));
        try {
            ivy.resolve(ResolveTest.class.getResource("ivy-956.xml"),
                getResolveOptions(ivy.getSettings(), new String[] {"*"}).setValidate(false));
            fail("No StrictConflictException has been thrown");
        } catch (StrictConflictException e) {
            // ignore
        }
    }

    public void testIVY1159_orderIsModAModB() throws Exception {
        testIVY1159("ivy-depsorder_modA_then_modB.xml", false);

        File deliveredIvyFile = new File("build/test/deliver/ivy-1.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("myorg", "modA", "0"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("myorg", "modB", "0"),
            dds[1].getDependencyRevisionId());
    }

    public void testIVY1159_orderIsModAModBReplaceForced() throws Exception {
        testIVY1159("ivy-depsorder_modA_then_modB.xml", true);

        File deliveredIvyFile = new File("build/test/deliver/ivy-1.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("myorg", "modA", "1"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("myorg", "modB", "1"),
            dds[1].getDependencyRevisionId());
    }

    public void testIVY1159_orderIsModBModA() throws Exception {
        testIVY1159("ivy-depsorder_modB_then_modA.xml", false);

        File deliveredIvyFile = new File("build/test/deliver/ivy-1.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("myorg", "modB", "0"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("myorg", "modA", "0"),
            dds[1].getDependencyRevisionId());
    }

    public void testIVY1159_orderIsModBModAReplaceForced() throws Exception {
        testIVY1159("ivy-depsorder_modB_then_modA.xml", true);

        File deliveredIvyFile = new File("build/test/deliver/ivy-1.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("myorg", "modB", "1"),
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("myorg", "modA", "1"),
            dds[1].getDependencyRevisionId());
    }

    private void testIVY1159(String modCIvyFile, boolean replaceForced) throws Exception {
        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/IVY-1159/ivysettings.xml"));

        ResolveOptions opts = new ResolveOptions();
        opts.setConfs(new String[] {"*"});
        opts.setResolveId("resolveid");
        opts.setRefresh(true);
        opts.setTransitive(true);

        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-1159/" + modCIvyFile),
            opts);
        assertFalse(report.hasError());

        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(
                ModuleRevisionId.newInstance("myorg", "modA", "1"),
                ModuleRevisionId.newInstance("myorg", "modB", "1"))), report
                    .getConfigurationReport("default").getModuleRevisionIds());

        DeliverOptions dopts = new DeliverOptions();
        dopts.setReplaceForcedRevisions(replaceForced);
        dopts.setGenerateRevConstraint(true);
        dopts.setConfs(new String[] {"*"});
        dopts.setStatus("release");
        dopts.setPubdate(new Date());
        dopts.setResolveId("resolveid");
        String pubrev = "1";
        String deliveryPattern = "build/test/deliver/ivy-[revision].xml";

        ivy.deliver(pubrev, deliveryPattern, dopts);
    }

    public void testIVY1300() throws Exception {
        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/IVY-1300/ivysettings.xml"));

        ResolveOptions opts = new ResolveOptions();
        opts.setConfs(new String[] {"*"});
        opts.setResolveId("resolveid");
        opts.setTransitive(true);

        ResolveReport report = ivy.resolve(new File("test/repositories/IVY-1300/assembly-ivy.xml"),
            opts);
        assertFalse(report.hasError());

        ModuleRevisionId modAExpectedRevId = ModuleRevisionId.newInstance("myorg", "modA", "trunk",
            "5");
        ModuleRevisionId modBExpectedRevId = ModuleRevisionId.newInstance("myorg", "modB",
            "releasebranch", "1");

        // check that the resolve report has the expected results, namely that trunk/5 is considered
        // later than branch/1
        // purely because 5>1. Of course it is more likely that we would want to consider this a
        // 'bad comparison', but
        // this Unit Test is not about that. It is about inconsistency of results between the
        // resolve report and the
        // delivered descriptor. In fact the delivered descriptor is out of step, because retrieve
        // and the report both
        // agree that trunk/5 is selected. Deliver begs to differ.

        Set<ModuleRevisionId> reportMrids = report.getConfigurationReport("default")
                .getModuleRevisionIds();
        assertEquals(
            new HashSet<ModuleRevisionId>(Arrays.asList(modAExpectedRevId, modBExpectedRevId)),
            reportMrids);

        DeliverOptions dopts = new DeliverOptions();
        dopts.setGenerateRevConstraint(true);
        dopts.setConfs(new String[] {"*"});
        dopts.setStatus("release");
        dopts.setPubdate(new Date());
        dopts.setResolveId("resolveid");
        String pubrev = "1";
        String deliveryPattern = "build/test/deliver/assembly-[revision].xml";

        ivy.deliver(pubrev, deliveryPattern, dopts);

        // now check that the resolve report has the same info as the delivered descriptor

        File deliveredIvyFile = new File("build/test/deliver/assembly-1.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivy.getSettings(), deliveredIvyFile.toURI().toURL(), false);
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("myorg", "modB", "releasebranch", "1"),
            dds[1].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("myorg", "modA", "trunk", "5"),
            dds[0].getDependencyRevisionId());
    }

    public void testUseCacheOnly() throws Exception {
        ResolveOptions option = getResolveOptions(new String[] {"*"}).setValidate(false);
        URL url = new File("test/repositories/1/usecacheonly/mod1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        option.setUseCacheOnly(true);

        // use cache only, hit the cache
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        CacheCleaner.deleteDir(cache);
        createCache();

        // no more in the cache, missed
        report = ivy.resolve(url, option);
        assertTrue(report.hasError());

        option.setUseCacheOnly(false);

        // try with use origin: should fail as the cache is empty
        ivy.getSettings().setDefaultUseOrigin(true);
        option.setUseCacheOnly(true);
        report = ivy.resolve(url, option);
        assertTrue(report.hasError());

        // populate the cache
        option.setUseCacheOnly(false);
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        // use origin should now work
        option.setUseCacheOnly(true);
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        // ensure that we hit only the cache and never try to hit in the repository
        FileSystemResolver resolver = (FileSystemResolver) ivy.getSettings().getResolver("1");
        resolver.setRepository(new AbstractRepository() {
            public List<String> list(String parent) throws IOException {
                throw new UnsupportedOperationException();
            }

            public Resource getResource(String source) throws IOException {
                throw new UnsupportedOperationException();
            }

            public void get(String source, File destination) throws IOException {
                throw new UnsupportedOperationException();
            }
        });
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());
    }

    public void testUseCacheOnlyWithRange() throws Exception {
        ResolveOptions option = getResolveOptions(new String[] {"*"});
        option.setValidate(false);

        ivy.getSettings().setDefaultUseOrigin(true);
        ivy.getSettings().setDefaultResolveMode("dynamic");

        URL url = new File("test/repositories/1/usecacheonly/mod3/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        option.setUseCacheOnly(true);

        // use cache only, hit the cache
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());
    }

    public void testUseCacheOnlyWithChanging() throws Exception {
        ResolveOptions option = getResolveOptions(new String[] {"*"});
        option.setValidate(false);

        ivy.getSettings().setDefaultUseOrigin(true);

        URL url = new File("test/repositories/1/usecacheonly/mod4/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, option);
        assertFalse(report.hasError());

        option.setUseCacheOnly(true);

        // use cache only, hit the cache
        report = ivy.resolve(url, option);
        assertFalse(report.hasError());

    }

    public void testUnpack() throws Exception {
        ResolveOptions options = getResolveOptions(new String[] {"*"});

        URL url = new File("test/repositories/1/packaging/module1/ivys/ivy-1.0.xml").toURI()
                .toURL();

        // normal resolve, the file goes in the cache
        ResolveReport report = ivy.resolve(url, options);
        assertFalse(report.hasError());

        ArtifactDownloadReport adr = report.getAllArtifactsReports()[0];
        File cacheDir = ivy.getSettings().getDefaultRepositoryCacheBasedir();
        assertEquals(new File(cacheDir, "packaging/module2/jars/module2-1.0.jar"),
            adr.getLocalFile());
        assertEquals(new File(cacheDir, "packaging/module2/jar_unpackeds/module2-1.0"),
            adr.getUnpackedLocalFile());

        File[] jarContents = adr.getUnpackedLocalFile().listFiles();
        Arrays.sort(jarContents);
        assertEquals(new File(adr.getUnpackedLocalFile(), "META-INF"), jarContents[0]);
        assertEquals(new File(adr.getUnpackedLocalFile(), "test.txt"), jarContents[1]);
        assertEquals(new File(adr.getUnpackedLocalFile(), "META-INF/MANIFEST.MF"),
            jarContents[0].listFiles()[0]);
    }
}
