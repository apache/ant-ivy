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
import org.apache.ivy.plugins.resolver.packager.PackagerProperty;
import org.apache.ivy.plugins.resolver.packager.PackagerResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

/**
 * Tests PackagerResolver.
 */
// junit
public class PackagerResolverTest extends AbstractDependencyResolverTest {

    private IvySettings _settings;

    private ResolveEngine _engine;

    private ResolveData _data;

    private File _cache;

    private File _workdir;
    private File _builddir;
    private File _cachedir;
    private File _websitedir;

    protected void setUp() throws Exception {
        
        _settings = new IvySettings();
        Message.setDefaultLogger(new DefaultMessageLogger(99));
        _engine = new ResolveEngine(_settings, new EventManager(), new SortEngine(_settings));
        _cache = new File("build/cache");
        _data = new ResolveData(_engine, new ResolveOptions());
        _cache.mkdirs();
        _settings.setDefaultCache(_cache);

        // Create work space with build and resource cache directories
        _workdir = new File("build/test/PackagerResolverTest");
        _builddir = new File(_workdir, "build");
        _cachedir = new File(_workdir, "resources");
        _websitedir = new File(_workdir, "website");
        cleanupTempDirs();
        if (!_builddir.mkdirs() || !_cachedir.mkdirs()) {
            throw new Exception("can't create directories under " + _workdir);
        }

        // copy "website"
        Copy copy = new Copy();
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File("test/repositories/packager/website"));
        copy.addFileset(fileSet);
        copy.setTodir(_websitedir);
        copy.setProject(new Project());
        copy.execute();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(_cache);
        cleanupTempDirs();
    }

    protected void cleanupTempDirs() throws Exception {
        FileUtil.forceDelete(_workdir);
    }

    public void testFile() throws Exception {

        // Create and configure resolver
        PackagerResolver resolver = new PackagerResolver();
        resolver.setSettings(_settings);
        File repoRoot = new File("test/repositories/packager/repo");
        resolver.addIvyPattern(
          "" + new File(repoRoot, "[organisation]/[module]/[revision]/ivy.xml").getAbsoluteFile().toURL().toExternalForm());
        resolver.setPackagerPattern(
          "" + new File(repoRoot, "[organisation]/[module]/[revision]/packager.xml").getAbsoluteFile().toURL().toExternalForm());
        resolver.setBuildRoot(_builddir.getAbsolutePath());
        resolver.setResourceCache(_cachedir.getAbsolutePath());
        resolver.setPreserveBuildDirectories(true);
        resolver.setVerbose(true);
        
        System.setProperty("packager.website.url", new File("test/repositories/packager/website").getAbsoluteFile().toURL().toExternalForm());

        resolver.setName("packager");
        assertEquals("packager", resolver.getName());

        // Get module descriptor
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", "mod", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(
          new DefaultDependencyDescriptor(mrid, false), _data);
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
        assertTrue(new File(_cachedir, "mod-1.0.tar.gz").exists());

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
    }
}
