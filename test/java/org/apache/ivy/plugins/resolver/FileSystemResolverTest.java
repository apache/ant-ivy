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
package org.apache.ivy.plugins.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.latest.LatestTimeStrategy;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.FileUtil;

/**
 * 
 */
public class FileSystemResolverTest extends AbstractDependencyResolverTest {
    // CheckStyle:MagicNumberCheck OFF

    private static final String FS = System.getProperty("file.separator");

    private static final String REL_IVY_PATTERN = "test" + FS + "repositories" + FS + "1" + FS
            + "[organisation]" + FS + "[module]" + FS + "ivys" + FS + "ivy-[revision].xml";

    private static final String IVY_PATTERN = new File(".").getAbsolutePath() + FS
            + REL_IVY_PATTERN;

    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    private File cache;

    private DefaultRepositoryCacheManager cacheManager;

    public FileSystemResolverTest() {
        setupLastModified();
    }

    protected void setUp() throws Exception {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        cache = new File("build/cache");
        data = new ResolveData(engine, new ResolveOptions());
        cache.mkdirs();
        settings.setDefaultCache(cache);
        cacheManager = (DefaultRepositoryCacheManager) settings.getDefaultRepositoryCacheManager();
    }

    private void setupLastModified() {
        // change important last modified dates cause svn doesn't keep them
        long minute = 60 * 1000;
        long time = new GregorianCalendar().getTimeInMillis() - (4 * minute);
        new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml").setLastModified(time);
        time += minute;
        new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.1.xml").setLastModified(time);
        time += minute;
        new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.1.xml").setLastModified(time);
        time += minute;
        new File("test/repositories/1/org1/mod1.1/ivys/ivy-2.0.xml").setLastModified(time);
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testFixedRevision() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());

        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, getDownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, getDownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testFindIvyFileRefWithMultipleIvyPatterns() throws Exception {
        // cfr IVY-676
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/multi-ivypattern/ivy1/ivy-[revision].xml");
        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/multi-ivypattern/ivy2/ivy-[revision].xml");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0+");
        ResolvedResource ivyRef = resolver.findIvyFileRef(new DefaultDependencyDescriptor(mrid,
                false), data);

        // check that the found ivy file is the one from the first pattern!
        assertEquals(
            new File("test/repositories/multi-ivypattern/ivy1/ivy-1.0.xml").getCanonicalPath(),
            new File(ivyRef.getResource().getName()).getCanonicalPath());
    }

    private DownloadOptions getDownloadOptions() {
        return new DownloadOptions();
    }

    public void testMaven2() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        resolver.setM2compatible(true);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(settings.getBaseDir() + "/test/repositories/m2/"
                + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/m2/"
                + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        mrid = ModuleRevisionId.newInstance("org.apache.unknown", "test", "1.0");
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNull(rmr);
        resolver.reportFailure();
    }

    public void testChecksum() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);

        resolver.addIvyPattern(settings.getBaseDir()
                + "/test/repositories/checksums/[module]/[artifact]-[revision].[ext]");
        resolver.addArtifactPattern(settings.getBaseDir()
                + "/test/repositories/checksums/[module]/[artifact]-[revision].[ext]");

        resolver.setChecksums("sha1, md5");
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("test", "allright", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        DownloadReport dr = resolver.download(rmr.getDescriptor().getAllArtifacts(),
            getDownloadOptions());
        assertEquals(4, dr.getArtifactsReports(DownloadStatus.SUCCESSFUL).length);

        resolver.setChecksums("md5");
        mrid = ModuleRevisionId.newInstance("test", "badivycs", "1.0");
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNull(rmr);
        resolver.setChecksums("none");
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        dr = resolver.download(new Artifact[] {new DefaultArtifact(mrid, rmr.getPublicationDate(),
                mrid.getName(), "jar", "jar")}, getDownloadOptions());
        assertEquals(1, dr.getArtifactsReports(DownloadStatus.SUCCESSFUL).length);

        resolver.setChecksums("md5");
        mrid = ModuleRevisionId.newInstance("test", "badartcs", "1.0");
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        dr = resolver.download(new Artifact[] {new DefaultArtifact(mrid, rmr.getPublicationDate(),
                mrid.getName(), "jar", "jar")}, getDownloadOptions());
        assertEquals(1, dr.getArtifactsReports(DownloadStatus.FAILED).length);

        resolver.setChecksums("");
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);
        dr = resolver.download(new Artifact[] {new DefaultArtifact(mrid, rmr.getPublicationDate(),
                mrid.getName(), "jar", "jar")}, getDownloadOptions());
        assertEquals(1, dr.getArtifactsReports(DownloadStatus.SUCCESSFUL).length);
    }

    public void testCheckModified() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        settings.addResolver(resolver);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                + "checkmodified" + FS + "ivy-[revision].xml");
        File modify = new File("test/repositories/checkmodified/ivy-1.0.xml");
        FileUtil.copy(new File("test/repositories/checkmodified/ivy-1.0-before.xml"), modify, null,
            true);
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());

        // updates ivy file in repository
        FileUtil.copy(new File("test/repositories/checkmodified/ivy-1.0-after.xml"), modify, null,
            true);
        pubdate = new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());

        // should not get the new version
        resolver.setCheckmodified(false);
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime(),
            rmr.getPublicationDate());

        // should now get the new version
        resolver.setCheckmodified(true);
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testNoRevision() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        settings.addResolver(resolver);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                + "norevision" + FS + "ivy-[module].xml");
        resolver.addArtifactPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                + "norevision" + FS + "[artifact].[ext]");
        File modify = new File("test/repositories/norevision/ivy-mod1.1.xml");
        File artifact = new File("test/repositories/norevision/mod1.1.jar");

        // 'publish' 'before' version
        FileUtil.copy(new File("test/repositories/norevision/ivy-mod1.1-before.xml"), modify, null,
            true);
        FileUtil.copy(new File("test/repositories/norevision/mod1.1-before.jar"), artifact, null,
            true);
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());

        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance("org1", "mod1.1", "latest.integration");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());

        Artifact[] artifacts = rmr.getDescriptor().getArtifacts("default");
        File archiveFileInCache = cacheManager.getArchiveFileInCache(artifacts[0]);
        resolver.download(artifacts, getDownloadOptions());
        assertTrue(archiveFileInCache.exists());
        BufferedReader r = new BufferedReader(new FileReader(archiveFileInCache));
        assertEquals("before", r.readLine());
        r.close();

        // updates ivy file and artifact in repository
        FileUtil.copy(new File("test/repositories/norevision/ivy-mod1.1-after.xml"), modify, null,
            true);
        FileUtil.copy(new File("test/repositories/norevision/mod1.1-after.jar"), artifact, null,
            true);
        pubdate = new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());
        // no need to update new artifact timestamp cause it isn't used

        // should get the new version even if checkModified is false, because we ask a
        // latest.integration
        resolver.setCheckmodified(false);
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);

        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.1", "1.1"), rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());

        artifacts = rmr.getDescriptor().getArtifacts("default");
        archiveFileInCache = cacheManager.getArchiveFileInCache(artifacts[0]);

        assertFalse(archiveFileInCache.exists());

        // should download the new artifact
        artifacts = rmr.getDescriptor().getArtifacts("default");
        resolver.download(artifacts, getDownloadOptions());
        assertTrue(archiveFileInCache.exists());
        r = new BufferedReader(new FileReader(archiveFileInCache));
        assertEquals("after", r.readLine());
        r.close();
    }

    public void testChanging() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        settings.addResolver(resolver);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                + "checkmodified" + FS + "ivy-[revision].xml");
        resolver.addArtifactPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                + "checkmodified" + FS + "[artifact]-[revision].[ext]");
        File modify = new File("test/repositories/checkmodified/ivy-1.0.xml");
        File artifact = new File("test/repositories/checkmodified/mod1.1-1.0.jar");

        // 'publish' 'before' version
        FileUtil.copy(new File("test/repositories/checkmodified/ivy-1.0-before.xml"), modify, null,
            true);
        FileUtil.copy(new File("test/repositories/checkmodified/mod1.1-1.0-before.jar"), artifact,
            null, true);
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());

        Artifact[] artifacts = rmr.getDescriptor().getArtifacts("default");
        resolver.download(artifacts, getDownloadOptions());
        File archiveFileInCache = cacheManager.getArchiveFileInCache(artifacts[0]);
        assertTrue(archiveFileInCache.exists());
        BufferedReader r = new BufferedReader(new FileReader(archiveFileInCache));
        assertEquals("before", r.readLine());
        r.close();

        // updates ivy file and artifact in repository
        FileUtil.copy(new File("test/repositories/checkmodified/ivy-1.0-after.xml"), modify, null,
            true);
        FileUtil.copy(new File("test/repositories/checkmodified/mod1.1-1.0-after.jar"), artifact,
            null, true);
        pubdate = new GregorianCalendar(2005, 4, 1, 11, 0, 0).getTime();
        modify.setLastModified(pubdate.getTime());
        // no need to update new artifact timestamp cause it isn't used

        // should not get the new version: checkmodified is false and dependency is not told to be a
        // changing one
        resolver.setCheckmodified(false);
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime(),
            rmr.getPublicationDate());

        assertTrue(archiveFileInCache.exists());
        r = new BufferedReader(new FileReader(archiveFileInCache));
        assertEquals("before", r.readLine());
        r.close();

        // should now get the new version cause we say it's a changing one
        rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false, true), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        assertEquals(pubdate, rmr.getPublicationDate());

        assertFalse(archiveFileInCache.exists());

        artifacts = rmr.getDescriptor().getArtifacts("default");
        resolver.download(artifacts, getDownloadOptions());
        assertTrue(archiveFileInCache.exists());
        r = new BufferedReader(new FileReader(archiveFileInCache));
        assertEquals("after", r.readLine());
        r.close();
    }

    public void testLatestTime() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        resolver.setLatestStrategy(new LatestTimeStrategy());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 1, 15, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testLatestRevision() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        resolver.setLatestStrategy(new LatestRevisionStrategy());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 1, 15, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testRelativePath() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(new File("src/java").getAbsolutePath() + "/../../" + REL_IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/src/../test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        resolver.setLatestStrategy(new LatestRevisionStrategy());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 1, 15, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testFormattedLatestTime() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        resolver.setLatestStrategy(new LatestTimeStrategy());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.1");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1+"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 0, 2, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testFormattedLatestRevision() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        resolver.setLatestStrategy(new LatestRevisionStrategy());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.1");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "1+"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 0, 2, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testPublish() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            assertEquals("test", resolver.getName());

            resolver.addIvyPattern(settings.getBaseDir() + FS + "test" + FS + "repositories" + FS
                    + "1" + FS + "[organisation]" + FS + "[module]" + FS + "[revision]" + FS
                    + "[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir() + FS + "test/repositories/1/"
                    + "[organisation]/[module]/[type]s/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            resolver.beginPublishTransaction(mrid, false);
            resolver.publish(ivyArtifact, src, false);
            resolver.publish(artifact, src, false);
            resolver.commitPublishTransaction();

            assertTrue(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/mytypes/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testPublishOverwrite() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            assertEquals("test", resolver.getName());

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            File ivyFile = new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml");
            File artifactFile = new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext");
            touch(ivyFile);
            touch(artifactFile);

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            resolver.beginPublishTransaction(mrid, true);
            resolver.publish(ivyArtifact, src, true);
            resolver.publish(artifact, src, true);
            resolver.commitPublishTransaction();

            long length = src.length();
            assertEquals(length, ivyFile.length());
            assertEquals(length, artifactFile.length());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    private void touch(File file) throws IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    public void testPublishTransaction() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");

            resolver.beginPublishTransaction(mrid, false);

            // files should not be available until the transaction is committed
            resolver.publish(ivyArtifact, src, false);
            assertFalse(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());

            resolver.publish(artifact, src, false);
            assertFalse(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());

            resolver.commitPublishTransaction();

            assertTrue(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testPublishTransactionWithBranch() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[branch]/[revision]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[branch]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "mybranch",
                "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");

            resolver.beginPublishTransaction(mrid, false);

            // files should not be available until the transaction is committed
            resolver.publish(ivyArtifact, src, false);
            assertFalse(new File("test/repositories/1/myorg/mymodule/mybranch/myrevision/ivy.xml")
                    .exists());

            resolver.publish(artifact, src, false);
            assertFalse(new File(
                    "test/repositories/1/myorg/mymodule/mybranch/myrevision/myartifact-myrevision.myext")
                    .exists());

            resolver.commitPublishTransaction();

            assertTrue(new File("test/repositories/1/myorg/mymodule/mybranch/myrevision/ivy.xml")
                    .exists());
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/mybranch/myrevision/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testPublishTransactionWithSubDirectories() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[type]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[type]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");

            resolver.beginPublishTransaction(mrid, false);

            // files should not be available until the transaction is committed
            resolver.publish(ivyArtifact, src, false);
            assertFalse(new File("test/repositories/1/myorg/mymodule/myrevision/ivy/ivy.xml")
                    .exists());

            resolver.publish(artifact, src, false);
            assertFalse(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/mytype/myartifact-myrevision.myext")
                    .exists());

            resolver.commitPublishTransaction();

            assertTrue(new File("test/repositories/1/myorg/mymodule/myrevision/ivy/ivy.xml")
                    .exists());
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/mytype/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testPublishTransactionWithDottedOrganisation() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setM2compatible(true);
            resolver.setSettings(settings);

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/m2/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/m2/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "mymodule",
                "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");

            resolver.beginPublishTransaction(mrid, false);

            // files should not be available until the transaction is committed
            resolver.publish(ivyArtifact, src, false);
            assertFalse(new File(
                    "test/repositories/m2/org/apache/mymodule/myrevision/ivy-myrevision.xml")
                    .exists());
            resolver.publish(artifact, src, false);
            assertFalse(new File(
                    "test/repositories/m2/org/apache/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());

            resolver.commitPublishTransaction();
            assertTrue(new File(
                    "test/repositories/m2/org/apache/mymodule/myrevision/ivy-myrevision.xml")
                    .exists());
            assertTrue(new File(
                    "test/repositories/m2/org/apache/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/m2/org/apache/mymodule"));
        }
    }

    public void testAbortTransaction() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            resolver.beginPublishTransaction(mrid, false);
            resolver.publish(ivyArtifact, src, false);
            resolver.publish(artifact, src, false);
            resolver.abortPublishTransaction();

            assertFalse(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());
            assertFalse(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testUnsupportedTransaction() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            resolver.setTransactional("true");

            resolver.addArtifactPattern(
            // this pattern is not supported for transaction publish
            settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            try {
                resolver.beginPublishTransaction(mrid, false);

                resolver.publish(artifact, src, false);
                fail("publishing with transaction=true and an unsupported pattern should raise an exception");
            } catch (IllegalStateException ex) {
                assertTrue(ex.getMessage().indexOf("transactional") != -1);
            }
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testUnsupportedTransaction2() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            resolver.setTransactional("true");

            // the two patterns are inconsistent and thus not supported for transactions
            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]-[module]/[revision]/[artifact]-[revision].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            try {
                resolver.beginPublishTransaction(mrid, false);
                resolver.publish(ivyArtifact, src, false);
                resolver.publish(artifact, src, false);
                fail("publishing with transaction=true and an unsupported combination of patterns should raise an exception");
            } catch (IllegalStateException ex) {
                assertTrue(ex.getMessage().indexOf("transactional") != -1);
            }
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testUnsupportedTransaction3() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            resolver.setTransactional("true");

            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            try {
                // overwrite transaction not supported
                resolver.beginPublishTransaction(mrid, true);

                resolver.publish(artifact, src, true);
                fail("publishing with transaction=true and overwrite mode should raise an exception");
            } catch (IllegalStateException ex) {
                assertTrue(ex.getMessage().indexOf("transactional") != -1);
            }
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testDisableTransaction() throws Exception {
        try {
            FileSystemResolver resolver = new FileSystemResolver();
            resolver.setName("test");
            resolver.setSettings(settings);
            resolver.setTransactional("false");

            resolver.addIvyPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact].[ext]");
            resolver.addArtifactPattern(settings.getBaseDir()
                    + "/test/repositories/1/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]");

            ModuleRevisionId mrid = ModuleRevisionId.newInstance("myorg", "mymodule", "myrevision");
            Artifact ivyArtifact = new DefaultArtifact(mrid, new Date(), "ivy", "ivy", "xml");
            Artifact artifact = new DefaultArtifact(mrid, new Date(), "myartifact", "mytype",
                    "myext");
            File src = new File("test/repositories/ivysettings.xml");
            resolver.beginPublishTransaction(mrid, false);

            // with transactions disabled the file should be available as soon as they are published
            resolver.publish(ivyArtifact, src, false);
            assertTrue(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());

            resolver.publish(artifact, src, false);
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());

            resolver.commitPublishTransaction();

            assertTrue(new File("test/repositories/1/myorg/mymodule/myrevision/ivy.xml").exists());
            assertTrue(new File(
                    "test/repositories/1/myorg/mymodule/myrevision/myartifact-myrevision.myext")
                    .exists());
        } finally {
            FileUtil.forceDelete(new File("test/repositories/1/myorg"));
        }
    }

    public void testListing() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[ext]");

        OrganisationEntry[] orgs = resolver.listOrganisations();
        ResolverTestHelper.assertOrganisationEntriesContains(resolver, new String[] {"org1",
                "org2", "org6", "org9", "orgfailure", "yourorg", "IVY-644"}, orgs);

        OrganisationEntry org = ResolverTestHelper.getEntry(orgs, "org1");
        ModuleEntry[] mods = resolver.listModules(org);
        ResolverTestHelper.assertModuleEntries(resolver, org, new String[] {"mod1.1", "mod1.2",
                "mod1.3", "mod1.4", "mod1.5", "mod1.6"}, mods);

        ModuleEntry mod = ResolverTestHelper.getEntry(mods, "mod1.1");
        RevisionEntry[] revs = resolver.listRevisions(mod);
        ResolverTestHelper.assertRevisionEntries(resolver, mod, new String[] {"1.0", "1.0.1",
                "1.1", "2.0"}, revs);

        mod = ResolverTestHelper.getEntry(mods, "mod1.2");
        revs = resolver.listRevisions(mod);
        ResolverTestHelper.assertRevisionEntries(resolver, mod, new String[] {"0.9", "1.0", "1.1",
                "2.0", "2.1", "2.2"}, revs);
    }

    public void testDownloadWithUseOriginIsTrue() throws Exception {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("test");
        resolver.setSettings(settings);
        ((DefaultRepositoryCacheManager) resolver.getRepositoryCacheManager()).setUseOrigin(true);
        assertEquals("test", resolver.getName());

        resolver.addIvyPattern(IVY_PATTERN);
        resolver.addArtifactPattern(settings.getBaseDir() + "/test/repositories/1/"
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());

        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, getDownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }
}
