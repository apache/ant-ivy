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

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyContext;
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
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.util.MockMessageLogger;

public class BintrayResolverTest extends AbstractDependencyResolverTest {

    private IvySettings _settings;

    private ResolveEngine _engine;

    private ResolveData _data;

    @Override
    protected void setUp() throws Exception {
        _settings = new IvySettings();
        _engine = new ResolveEngine(_settings, new EventManager(), new SortEngine(_settings));
        _data = new ResolveData(_engine, new ResolveOptions());
        _settings.setDefaultCache(TestHelper.cache);
    }

    @Override
    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testDefaults() {
        BintrayResolver resolver = new BintrayResolver();
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("bintray/jcenter", resolver.getName());
    }

    public void testDefaultsWithName() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setName("TestName");
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("TestName", resolver.getName());
    }

    public void testSubjectOnly() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setSubject("jfrog");
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("bintray/jcenter", resolver.getName());
    }

    public void testRepoOnly() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setRepo("jfrog-jars");
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("bintray/jcenter", resolver.getName());
    }

    public void testSubjectOnlyWithName() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setSubject("jfrog");
        resolver.setName("TestName");
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("TestName", resolver.getName());
    }

    public void testRepoOnlyWithName() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setRepo("jfrog-jars");
        resolver.setName("TestName");
        assertEquals("https://jcenter.bintray.com/", resolver.getRoot());
        assertEquals("TestName", resolver.getName());
    }

    public void testSubjectAndRepo() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setSubject("jfrog");
        resolver.setRepo("jfrog-jars");
        assertEquals("https://dl.bintray.com/jfrog/jfrog-jars/", resolver.getRoot());
        assertEquals("bintray/jfrog/jfrog-jars", resolver.getName());
    }

    public void testSubjectAndRepoWithName() {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setSubject("jfrog");
        resolver.setRepo("jfrog-jars");
        resolver.setName("TestName");
        assertEquals("https://dl.bintray.com/jfrog/jfrog-jars/", resolver.getRoot());
        assertEquals("TestName", resolver.getName());
    }

    public void testBintray() throws Exception {

        BintrayResolver resolver = new BintrayResolver();
        resolver.setSettings(_settings);
        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance("org.apache.ant", "ant-antunit", "1.2");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(),
                "ant-antunit", "jar", "jar");
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

    public void testErrorReport() throws Exception {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setSubject("unknown");
        resolver.setRepo("unknown");
        resolver.setName("test");
        resolver.setM2compatible(true);
        resolver.setSettings(_settings);
        assertEquals("test", resolver.getName());

        MockMessageLogger mockMessageImpl = new MockMessageLogger();
        IvyContext.getContext().getIvy().getLoggerEngine().setDefaultLogger(mockMessageImpl);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "commons-fileupload",
            "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), _data);
        assertNull(rmr);

        mockMessageImpl
                .assertLogContains("trying https://dl.bintray.com/unknown/unknown/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.jar");
        mockMessageImpl
                .assertLogContains("tried https://dl.bintray.com/unknown/unknown/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.jar");
    }

    public void testBintrayArtifacts() throws Exception {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setName("test");
        resolver.setSettings(_settings);
        assertEquals("test", resolver.getName());

        ModuleRevisionId mrid = ModuleRevisionId
                .newInstance("org.apache.ant", "ant-antunit", "1.2");
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mrid, false);
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(),
                "ant-antunit", "javadoc", "jar"), ExactPatternMatcher.INSTANCE, null));
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(),
                "ant-antunit", "sources", "jar"), ExactPatternMatcher.INSTANCE, null));
        ResolvedModuleRevision rmr = resolver.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact profiler = new DefaultArtifact(mrid, rmr.getPublicationDate(),
                "ant-antunit", "javadoc", "jar");
        DefaultArtifact trace = new DefaultArtifact(mrid, rmr.getPublicationDate(), "ant-antunit",
                "sources", "jar");
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

    public void testUnknown() throws Exception {
        BintrayResolver resolver = new BintrayResolver();
        resolver.setName("test");
        resolver.setSettings(_settings);

        assertNull(resolver.getDependency(
            new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("unknown", "unknown",
                "1.0"), false), _data));
    }

}
