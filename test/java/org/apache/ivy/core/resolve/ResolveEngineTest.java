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
package org.apache.ivy.core.resolve;

import java.io.File;
import java.util.Date;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.DefaultResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.CacheCleaner;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResolveEngineTest {

    private Ivy ivy;

    private File cache;

    @Before
    public void setUp() throws Exception {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        createCache();

        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
    }

    @After
    public void tearDown() {
        CacheCleaner.deleteDir(cache);
    }

    @Test
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

    @Test
    public void testLocateThenDownload() {
        ResolveEngine engine = new ResolveEngine(ivy.getSettings(), ivy.getEventManager(),
                ivy.getSortEngine());

        testLocateThenDownload(engine,
            DefaultArtifact.newIvyArtifact(ModuleRevisionId.parse("org1#mod1.1;1.0"), new Date()),
            new File("test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"));
        testLocateThenDownload(engine,
            new DefaultArtifact(ModuleRevisionId.parse("org1#mod1.1;1.0"), new Date(), "mod1.1",
                    "jar", "jar"), new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"));
    }

    @Test
    public void wontWriteResolvedDependenciesOutsideOfCache() throws Exception {
        DefaultResolutionCacheManager orig = (DefaultResolutionCacheManager) ivy.getSettings()
            .getResolutionCacheManager();

        DefaultResolutionCacheManager fake = new DefaultResolutionCacheManager() {
            {
                setBasedir(orig.getBasedir());
                setSettings(ivy.getSettings());
            }

            @Override
            public File getResolvedIvyPropertiesInCache(ModuleRevisionId mrid) {
                return new File(getBasedir(), "../foo.properties");
            }
        };

        ivy.getSettings().setResolutionCacheManager(fake);
        ResolveEngine engine = new ResolveEngine(ivy.getSettings(), ivy.getEventManager(),
                ivy.getSortEngine());

        ResolveOptions options = new ResolveOptions();
        options.setConfs(new String[] {"*"});

        ModuleRevisionId mRevId = ModuleRevisionId.parse("org1#mod1.1;1.0");
        try {
            engine.resolve(mRevId, options, true);
            fail("expected an exception");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * Tests that setting the dictator resolver on the resolve engine doesn't change the
     * dependency resolver set in the Ivy settings. See IVY-1618 for details.
     */
    @Test
    public void testSetDictatorResolver() throws Exception {
        final Ivy current = Ivy.newInstance();
        current.configure(new File("test/repositories/ivysettings.xml"));
        final FileSystemResolver settingsResolver = new FileSystemResolver();
        settingsResolver.setName("resolver-1");
        current.getSettings().setDictatorResolver(settingsResolver);

        final ResolveEngine engine = new ResolveEngine(current.getSettings(), current.getEventManager(),
                current.getSortEngine());
        final FileSystemResolver engineResolver = new FileSystemResolver();
        engineResolver.setName("resolver-2");
        engine.setDictatorResolver(engineResolver);

        Assert.assertNotNull("Resolver is null on engine", engine.getDictatorResolver());
        Assert.assertSame("Unexpected resolver on engine", engineResolver, engine.getDictatorResolver());

        final ModuleRevisionId dummy = ModuleRevisionId.newInstance("foo", "bar", "1.2.3");
        Assert.assertNotNull("Resolver is null on Ivy settings", current.getSettings().getResolver(dummy));
        Assert.assertSame("Unexpected resolver on Ivy settings", settingsResolver,
                current.getSettings().getResolver(dummy));

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
