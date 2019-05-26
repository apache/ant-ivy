/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.apache.ivy.TestHelper;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 */
public class IvyRepResolverTest extends AbstractDependencyResolverTest {
    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        data = new ResolveData(engine, new ResolveOptions());
        TestHelper.createCache();
        settings.setDefaultCache(TestHelper.cache);
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testDefaults() {
        IvyRepResolver resolver = new IvyRepResolver();
        settings.setVariable("ivy.ivyrep.default.ivy.root", "http://www.jayasoft.fr/myivyrep/");
        settings.setVariable("ivy.ivyrep.default.ivy.pattern",
            "[organisation]/[module]/ivy-[revision].[ext]");
        settings.setVariable("ivy.ivyrep.default.artifact.root",
            "http://www.ibiblio.org/mymaven/");
        settings.setVariable("ivy.ivyrep.default.artifact.pattern",
            "[module]/jars/[artifact]-[revision].jar");
        resolver.setSettings(settings);
        List<String> l = resolver.getIvyPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.jayasoft.fr/myivyrep/[organisation]/[module]/ivy-[revision].[ext]",
            l.get(0));
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/jars/[artifact]-[revision].jar",
            l.get(0));
    }

    /**
     * Test case for IVY-625. Must fail if no ivyroot specified.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-625">IVY-625</a>
     */
    @Test
    public void testMandatoryRoot() throws Exception {
        expExc.expect(IllegalStateException.class);
        expExc.expectMessage("ivyroot");

        IvyRepResolver resolver = new IvyRepResolver();
        resolver.setName("test");
        resolver.setSettings(settings);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "commons-cli", "1.0");

        resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), data);
    }

    @Test
    public void testIvyRepWithLocalURL() throws Exception {
        IvyRepResolver resolver = new IvyRepResolver();
        String rootpath = new File("test/repositories/1").getAbsolutePath();

        resolver.setName("testLocal");
        resolver.setIvyroot("file:" + rootpath);
        resolver.setIvypattern("[organisation]/[module]/ivys/ivy-[revision].xml");
        resolver.setArtroot("file:" + rootpath);
        resolver.setArtpattern("[organisation]/[module]/jars/[artifact]-[revision].[ext]");
        resolver.setSettings(settings);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid,
                false), data);
        assertNotNull(rmr);

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "mod1.1",
                "jar", "jar");
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
}
