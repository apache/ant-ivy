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

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.version.MavenTimedSnapshotVersionMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that the {@link IBiblioResolver} can resolve both regular Maven snapshot (like
 * 1.0.2-SNAPSHOT) and timestamped Maven snapshot (like 1.0.2-20100925.223013-19) dependencies
 * correctly.
 */
public class IBiblioMavenSnapshotsResolutionTest {

    private Ivy ivy;

    @Before
    public void before() throws Exception {
        TestHelper.createCache();
        this.ivy = new Ivy();
        this.ivy.configure(new File("test/repositories/ivysettings.xml"));
        // add the maven timestamped snapshot version matcher
        this.ivy.getSettings().addVersionMatcher(new MavenTimedSnapshotVersionMatcher());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    /**
     * Tests that an Ivy module that depends on regular and timestamped snapshots of Maven
     * artifacts, when resolved using a {@link IBiblioResolver} and with
     * {@link MavenTimedSnapshotVersionMatcher} configured in {@link IvySettings}, is resolved
     * correctly for such snapshot dependencies.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void testSnapshotResolution() throws Exception {
        final IvySettings settings = this.ivy.getSettings();
        assertNotNull("Maven timestamped snapshot revision version matcher is absent",
            settings.getVersionMatcher(new MavenTimedSnapshotVersionMatcher().getName()));
        final ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(new String[]{"default"});
        final ResolveReport report = ivy.resolve(new File("test/repositories/2/maven-snapshot-deps-test/ivy-with-maven-snapshot-deps.xml"), resolveOptions);
        assertNotNull("Resolution report was null", report);
        assertFalse("Resolution report has error(s)", report.hasError());

        final ModuleDescriptor md = report.getModuleDescriptor();
        assertNotNull("Module descriptor in resolution report was null", md);
        final ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache.ivy",
            "maven-snapshot-deps-test", "1.2.3");
        assertEquals("Unexpected module resolved", mrid, md.getModuleRevisionId());

        final ConfigurationResolveReport crr = report.getConfigurationReport("default");

        final ModuleRevisionId exactRevision = ModuleRevisionId.newInstance("org.apache.ivy.maven-snapshot-test", "exact-revision", "2.3.4");
        final ArtifactDownloadReport[] dr1 = crr.getDownloadReports(exactRevision);
        assertNotNull("Artifact download report missing for dependency " + exactRevision, dr1);
        assertEquals("Unexpected number of artifact download report for dependency " + exactRevision, dr1.length, 1);
        final ArtifactDownloadReport exactRevDownloadReport = dr1[0];
        assertEquals("Unexpected download status for dependency " + exactRevision,
            exactRevDownloadReport.getDownloadStatus(), DownloadStatus.SUCCESSFUL);

        final ModuleRevisionId regularSnapshot = ModuleRevisionId.newInstance("org.apache.ivy.maven-snapshot-test", "regular-snapshot", "1.2.3-SNAPSHOT");
        final ArtifactDownloadReport[] dr2 = crr.getDownloadReports(regularSnapshot);
        assertNotNull("Artifact download report missing for dependency " + regularSnapshot, dr2);
        assertEquals("Unexpected number of artifact download report for dependency " + regularSnapshot, dr2.length, 1);
        final ArtifactDownloadReport regularSnapshotDownloadReport = dr2[0];
        assertEquals("Unexpected download status for dependency " + regularSnapshot,
            regularSnapshotDownloadReport.getDownloadStatus(), DownloadStatus.SUCCESSFUL);

        final ModuleRevisionId timestampedSnapshot = ModuleRevisionId.newInstance("org.apache.ivy.maven-snapshot-test", "timestamped-snapshot", "5.6.7-20170911.130943-1");
        final ArtifactDownloadReport[] dr3 = crr.getDownloadReports(timestampedSnapshot);
        assertNotNull("Artifact download report missing for dependency " + timestampedSnapshot, dr3);
        assertEquals("Unexpected number of artifact download report for dependency " + timestampedSnapshot, dr3.length, 1);
        final ArtifactDownloadReport timestampedSnapshotDownloadReport = dr3[0];
        assertEquals("Unexpected download status for dependency " + timestampedSnapshot,
            timestampedSnapshotDownloadReport.getDownloadStatus(), DownloadStatus.SUCCESSFUL);

    }

}
