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
import java.util.Locale;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.resolver.packager.PackagerResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

/**
 * Tests PackagerResolver.
 */
public class PackagerResolverTest extends AbstractDependencyResolverTest {

    private IvySettings settings;

    private ResolveData data;

    private File cache;

    private File workdir;

    private File builddir;

    private File cachedir;

    protected void setUp() throws Exception {
        Message.setDefaultLogger(new DefaultMessageLogger(99));

        settings = new IvySettings();
        ResolveEngine engine = new ResolveEngine(settings, new EventManager(), new SortEngine(
                settings));
        cache = new File("build/cache");
        data = new ResolveData(engine, new ResolveOptions());
        cache.mkdirs();
        settings.setDefaultCache(cache);

        // Create work space with build and resource cache directories
        workdir = new File("build/test/PackagerResolverTest");
        builddir = new File(workdir, "build");
        cachedir = new File(workdir, "resources");
        cleanupTempDirs();
        if (!builddir.mkdirs() || !cachedir.mkdirs()) {
            throw new Exception("can't create directories under " + workdir);
        }
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(cache);
        cleanupTempDirs();
    }

    protected void cleanupTempDirs() throws Exception {
        FileUtil.forceDelete(workdir);
    }

    public void testFile() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            // Create and configure resolver
            PackagerResolver resolver = new PackagerResolver();
            resolver.setSettings(settings);
            String repoRoot = new File("test/repositories/packager/repo").toURI().toURL()
                    .toExternalForm();
            resolver.addIvyPattern(repoRoot + "[organisation]/[module]/[revision]/ivy.xml");
            resolver.setPackagerPattern(repoRoot
                    + "[organisation]/[module]/[revision]/packager.xml");
            resolver.setBuildRoot(builddir);
            resolver.setResourceCache(cachedir);
            resolver.setPreserveBuildDirectories(true);
            resolver.setVerbose(true);

            resolver.setProperty("packager.website.url", new File(
                    "test/repositories/packager/website").toURI().toURL().toExternalForm());

            resolver.setName("packager");
            assertEquals("packager", resolver.getName());

            // Get module descriptor
            ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", "mod", "1.0");
            ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                    mrid, false), data);
            assertNotNull(rmr);

            assertEquals(mrid, rmr.getId());
            Date pubdate = new GregorianCalendar(2004, 10, 1, 11, 0, 0).getTime();
            assertEquals(pubdate, rmr.getPublicationDate());

            // Download artifact
            Artifact artifact = new DefaultArtifact(mrid, pubdate, "mod", "jar", "jar");
            DownloadReport report = resolver.download(new Artifact[] {artifact}, downloadOptions());
            assertNotNull(report);

            assertEquals(1, report.getArtifactsReports().length);

            ArtifactDownloadReport ar = report.getArtifactReport(artifact);
            System.out.println("downloaddetails: " + ar.getDownloadDetails());
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

            // Verify resource cache now contains the distribution archive
            assertTrue(new File(cachedir, "mod-1.0.tar.gz").exists());

            // Download again, should use Ivy cache this time
            report = resolver.download(new Artifact[] {artifact}, downloadOptions());
            assertNotNull(report);

            assertEquals(1, report.getArtifactsReports().length);

            ar = report.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.NO, ar.getDownloadStatus());

            // Now download the maven2 artifact
            artifact = DefaultArtifact.cloneWithAnotherName(artifact, "foobar-janfu");
            report = resolver.download(new Artifact[] {artifact}, downloadOptions());
            assertNotNull(report);

            assertEquals(1, report.getArtifactsReports().length);

            ar = report.getArtifactReport(artifact);
            assertNotNull(ar);

            assertEquals(artifact, ar.getArtifact());
            assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testZipResourceInclusion() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            // Create and configure resolver
            PackagerResolver resolver = new PackagerResolver();
            resolver.setSettings(settings);
            String repoRoot = new File("test/repositories/IVY-1179/repo").toURI().toURL()
                    .toExternalForm();
            resolver.addIvyPattern(repoRoot + "[organisation]/[module]/[revision]/ivy.xml");
            resolver.setPackagerPattern(repoRoot
                    + "[organisation]/[module]/[revision]/packager.xml");
            resolver.setBuildRoot(builddir);
            resolver.setResourceCache(cachedir);
            resolver.setPreserveBuildDirectories(true);
            resolver.setVerbose(true);

            resolver.setProperty("packager.website.url", new File(
                    "test/repositories/IVY-1179/website").toURI().toURL().toExternalForm());

            resolver.setName("packager");

            // Get module descriptor
            ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", "A", "1.0");
            ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                    mrid, false), data);

            // Download artifact
            Artifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "A", "jar",
                    "jar");
            resolver.download(new Artifact[] {artifact}, downloadOptions());

            // assert that the file A.jar is extracted from the archive
            File jar = new File(builddir, "org/A/1.0/artifacts/jars/A.jar");
            assertTrue(jar.exists());

            // assert that the file README is not extracted from the archive
            File readme = new File(builddir, "org/A/1.0/extract/A-1.0/README");
            assertFalse(readme.exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testTarResourceInclusion() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            // Create and configure resolver
            PackagerResolver resolver = new PackagerResolver();
            resolver.setSettings(settings);
            String repoRoot = new File("test/repositories/IVY-1179/repo").toURI().toURL()
                    .toExternalForm();
            resolver.addIvyPattern(repoRoot + "[organisation]/[module]/[revision]/ivy.xml");
            resolver.setPackagerPattern(repoRoot
                    + "[organisation]/[module]/[revision]/packager.xml");
            resolver.setBuildRoot(builddir);
            resolver.setResourceCache(cachedir);
            resolver.setPreserveBuildDirectories(true);
            resolver.setVerbose(true);

            resolver.setProperty("packager.website.url", new File(
                    "test/repositories/IVY-1179/website").toURI().toURL().toExternalForm());

            resolver.setName("packager");

            // Get module descriptor
            ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", "B", "1.0");
            ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(
                    mrid, false), data);

            // Download artifact
            Artifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "B", "jar",
                    "jar");
            resolver.download(new Artifact[] {artifact}, downloadOptions());

            // assert that the file B.jar is extracted from the archive
            File jar = new File(builddir, "org/B/1.0/artifacts/jars/B.jar");
            assertTrue(jar.exists());

            // assert that the file README is not extracted from the archive
            File readme = new File(builddir, "org/B/1.0/extract/B-1.0/README");
            assertFalse(readme.exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }
}
