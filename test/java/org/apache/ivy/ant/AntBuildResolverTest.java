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
package org.apache.ivy.ant;

import java.io.File;

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.AntWorkspaceResolver.WorkspaceArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import junit.framework.TestCase;

public class AntBuildResolverTest extends TestCase {

    private static final ModuleRevisionId MRID_MODULE1 = ModuleRevisionId.newInstance("org.acme",
        "module1", "1.1");

    private static final ModuleRevisionId MRID_PROJECT1 = ModuleRevisionId.newInstance(
        "org.apache.ivy.test", "project1", "0.1");

    private Project project;

    private IvyConfigure configure;

    private WorkspaceArtifact wa;

    @Override
    protected void setUp() throws Exception {
        TestHelper.cleanCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());

        AntWorkspaceResolver antWorkspaceResolver = new AntWorkspaceResolver();
        antWorkspaceResolver.setName("test-workspace");
        wa = antWorkspaceResolver.createArtifact();
        FileSet fileset = new FileSet();
        fileset.setProject(project);
        fileset.setDir(new File("test/workspace"));
        fileset.setIncludes("*/ivy.xml");
        antWorkspaceResolver.addConfigured(fileset);
        antWorkspaceResolver.setProject(project);

        configure = new IvyConfigure();
        configure.setProject(project);
        configure.setFile(new File("test/workspace/ivysettings.xml"));
        configure.addConfiguredWorkspaceResolver(antWorkspaceResolver);
        configure.execute();
    }

    @Override
    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testNoProject() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project1/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        ResolveReport report = (ResolveReport) project.getReference("ivy.resolved.report");
        assertEquals(1, report.getDependencies().size());
        assertEquals(MRID_MODULE1, report.getDependencies().get(0).getResolvedId());
    }

    public void testProject() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project2/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        ResolveReport report = (ResolveReport) project.getReference("ivy.resolved.report");
        assertEquals(2, report.getDependencies().size());
        assertEquals(MRID_PROJECT1, report.getDependencies().get(0).getResolvedId());
        assertEquals(MRID_MODULE1, report.getDependencies().get(1).getResolvedId());
        assertEquals(1, report.getArtifactsReports(MRID_PROJECT1).length);
        assertEquals(DownloadStatus.NO,
            report.getArtifactsReports(MRID_PROJECT1)[0].getDownloadStatus());
        assertEquals(new File("test/workspace/project1/target/dist/jars/project1.jar").toURI()
                .toURL(), report.getArtifactsReports(MRID_PROJECT1)[0].getArtifact().getUrl());
        assertEquals(
            new File("test/workspace/project1/target/dist/jars/project1.jar").getAbsoluteFile(),
            report.getArtifactsReports(MRID_PROJECT1)[0].getLocalFile());
    }

    public void testProjectFolder() throws Exception {
        wa.setPath("target/classes");

        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project2/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        ResolveReport report = (ResolveReport) project.getReference("ivy.resolved.report");
        assertEquals(2, report.getDependencies().size());
        assertEquals(MRID_PROJECT1, report.getDependencies().get(0).getResolvedId());
        assertEquals(MRID_MODULE1, report.getDependencies().get(1).getResolvedId());
        assertEquals(1, report.getArtifactsReports(MRID_PROJECT1).length);
        assertEquals(DownloadStatus.NO,
            report.getArtifactsReports(MRID_PROJECT1)[0].getDownloadStatus());
        assertEquals(new File("test/workspace/project1/target/classes").toURI().toURL(),
            report.getArtifactsReports(MRID_PROJECT1)[0].getArtifact().getUrl());
        assertEquals(new File("test/workspace/project1/target/classes").getAbsoluteFile(),
            report.getArtifactsReports(MRID_PROJECT1)[0].getLocalFile());
    }

    public void testDependencyArtifact() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project3/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        ResolveReport report = (ResolveReport) project.getReference("ivy.resolved.report");
        assertEquals(2, report.getDependencies().size());
        assertEquals(MRID_PROJECT1, report.getDependencies().get(0).getResolvedId());
        assertEquals(MRID_MODULE1, report.getDependencies().get(1).getResolvedId());
        assertEquals(1, report.getArtifactsReports(MRID_PROJECT1).length);
        assertEquals(DownloadStatus.NO,
            report.getArtifactsReports(MRID_PROJECT1)[0].getDownloadStatus());
        assertEquals(new File("test/workspace/project1/target/dist/jars/project1.jar").toURI()
                .toURL(), report.getArtifactsReports(MRID_PROJECT1)[0].getArtifact().getUrl());
        assertEquals(
            new File("test/workspace/project1/target/dist/jars/project1.jar").getAbsoluteFile(),
            report.getArtifactsReports(MRID_PROJECT1)[0].getLocalFile());
    }

    public void testCachePath() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project2/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        IvyCachePath cachePath = new IvyCachePath();
        cachePath.setProject(project);
        cachePath.setPathid("test.cachepath.id");
        cachePath.execute();

        Path path = (Path) project.getReference("test.cachepath.id");
        assertEquals(2, path.size());
        assertEquals(
            new File("test/workspace/project1/target/dist/jars/project1.jar").getAbsolutePath(),
            path.list()[0]);
        assertEquals(new File(TestHelper.cache, "org.acme/module1/jars/module1-1.1.jar").getAbsolutePath(),
            path.list()[1]);
    }

    public void testCachePathFolder() throws Exception {
        wa.setPath("target/classes");

        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/workspace/project2/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        IvyCachePath cachePath = new IvyCachePath();
        cachePath.setProject(project);
        cachePath.setPathid("test.cachepath.id");
        cachePath.execute();

        Path path = (Path) project.getReference("test.cachepath.id");
        assertEquals(2, path.size());
        assertEquals(new File("test/workspace/project1/target/classes").getAbsolutePath(),
            path.list()[0]);
        assertEquals(new File(TestHelper.cache, "org.acme/module1/jars/module1-1.1.jar").getAbsolutePath(),
            path.list()[1]);
    }
}
