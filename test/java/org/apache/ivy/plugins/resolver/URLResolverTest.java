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

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;

/**
 * Tests URLResolver. Http tests are based upon ibiblio site.
 */
public class URLResolverTest extends AbstractDependencyResolverTest {
    // remote.test
    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    protected void setUp() throws Exception {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        data = new ResolveData(engine, new ResolveOptions());
        TestHelper.createCache();
        settings.setDefaultCache(TestHelper.cache);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testFile() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        String rootpath = new File("test/repositories/1").toURI().toURL().toExternalForm();
        resolver.addIvyPattern(rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(rootpath
                + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());

        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, downloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, downloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testLatestFile() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        String rootpath = new File("test/repositories/1").toURI().toURL().toExternalForm();
        resolver.addIvyPattern(rootpath + "[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(rootpath
                + "[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2005, 1, 15, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());
    }

    public void testLatestFileWithOpaqueURL() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        String rootpath = new File("test/repositories/1").getAbsoluteFile().toURI().toURL()
                .toExternalForm();
        resolver.addIvyPattern(rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(rootpath
                + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "2.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("org1", "mod1.1", "latest.integration"), false), data);
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
        resolver.setSettings(settings);
        resolver.addArtifactPattern(ibiblioRoot + "/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "commons-fileupload", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(),
                "commons-fileupload", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, downloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, downloadOptions());
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
        resolver.setSettings(settings);
        resolver.addArtifactPattern(ibiblioRoot + "/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "nanning", "0.9");
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mrid, false);
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(),
                "nanning-profiler", "jar", "jar"), ExactPatternMatcher.INSTANCE, null));
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(),
                "nanning-trace", "jar", "jar"), ExactPatternMatcher.INSTANCE, null));
        ResolvedModuleRevision rmr = resolver.getDependency(dd, data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact profiler = new DefaultArtifact(mrid, rmr.getPublicationDate(),
                "nanning-profiler", "jar", "jar");
        DefaultArtifact trace = new DefaultArtifact(mrid, rmr.getPublicationDate(),
                "nanning-trace", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {profiler, trace},
            downloadOptions());
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
        report = resolver.download(new Artifact[] {profiler, trace}, downloadOptions());
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
        resolver.setSettings(settings);
        resolver.addArtifactPattern(ibiblioRoot + "/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("objectweb", "asm", "1.4+");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        assertEquals("1.4.3", rmr.getId().getRevision());
    }

    public void testVersionRangeIBiblio() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }

        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        resolver.setAlwaysCheckExactRevision(true);
        resolver.addIvyPattern(ibiblioRoot + "/[module]/poms/[module]-[revision].pom");
        resolver.addArtifactPattern(ibiblioRoot + "/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("asm", "asm", "[1.4,1.5]");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);
        assertEquals("1.4.4", rmr.getId().getRevision());
    }

    public void testUnknown() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }

        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        resolver.addIvyPattern(ibiblioRoot + "/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(ibiblioRoot
                + "/maven/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");

        assertNull(resolver.getDependency(
            new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("unknown", "unknown",
                "1.0"), false), data));
    }

    public void testDownloadWithUseOriginIsTrue() throws Exception {
        URLResolver resolver = new URLResolver();
        resolver.setSettings(settings);
        String rootpath = new File("test/repositories/1").toURI().toURL().toExternalForm();
        resolver.addIvyPattern(rootpath + "/[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.addArtifactPattern(rootpath
                + "/[organisation]/[module]/[type]s/[artifact]-[revision].[type]");
        resolver.setName("test");
        ((DefaultRepositoryCacheManager) resolver.getRepositoryCacheManager()).setUseOrigin(true);
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        assertEquals(mrid, rmr.getId());
        Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
        assertEquals(pubdate, rmr.getPublicationDate());

        // test to ask to download
        DefaultArtifact artifact = new DefaultArtifact(mrid, pubdate, "mod1.1", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, new DownloadOptions());
        assertNotNull(report);

        assertEquals(1, report.getArtifactsReports().length);

        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);

        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }
}
