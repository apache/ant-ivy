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
import java.util.Date;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.CacheCleaner;

import junit.framework.TestCase;

public class ResolveEngineTest extends TestCase {

    private Ivy ivy;

    private File cache;

    protected void setUp() throws Exception {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        createCache();

        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testInlineResolveWithNonExistingModule() throws Exception {
        ResolveEngine engine = new ResolveEngine(ivy.getSettings(), ivy.getEventManager(),
                ivy.getSortEngine());

        ResolveOptions options = new ResolveOptions();
        options.setConfs(new String[] {"*"});

        ModuleRevisionId mRevId = ModuleRevisionId.newInstance("org1XX", "mod1.0XX", "1.0XX");
        ResolveReport report = engine.resolve(mRevId, options, true);

        assertNotNull("The ResolveReport may never be null", report);
        assertTrue(report.hasError());
    }

    public void testLocateThenDownload() throws Exception {
        ResolveEngine engine = new ResolveEngine(ivy.getSettings(), ivy.getEventManager(),
                ivy.getSortEngine());

        testLocateThenDownload(engine,
            DefaultArtifact.newIvyArtifact(ModuleRevisionId.parse("org1#mod1.1;1.0"), new Date()),
            new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"));
        testLocateThenDownload(engine,
            new DefaultArtifact(ModuleRevisionId.parse("org1#mod1.1;1.0"), new Date(), "mod1.1",
                    "jar", "jar"), new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"));
    }

    private void testLocateThenDownload(ResolveEngine engine, Artifact artifact, File artifactFile) {
        ArtifactOrigin origin = engine.locate(artifact);
        assertNotNull(origin);
        assertTrue(origin.isLocal());
        assertEquals(artifactFile.getAbsolutePath(),
            new File(origin.getLocation()).getAbsolutePath());

        ArtifactDownloadReport r = engine.download(origin, new DownloadOptions());
        assertNotNull(r);
        assertEquals(DownloadStatus.SUCCESSFUL, r.getDownloadStatus());
        assertNotNull(r.getLocalFile());
        assertTrue(r.getLocalFile().exists());
    }

    private void createCache() {
        cache.mkdirs();
    }
}
