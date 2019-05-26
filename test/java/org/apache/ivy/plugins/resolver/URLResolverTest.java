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

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
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
import org.apache.ivy.core.settings.NamedTimeoutConstraint;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.core.sort.SortEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests URLResolver. Http tests are based upon ibiblio site.
 */
public class URLResolverTest extends AbstractDependencyResolverTest {
    // remote.test
    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

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

    @Test
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

    @Test
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

    @Test
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

    /**
     * Tests that the timeout constraint set on the URL resolver is used correctly by the resolver
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testTimeoutConstraint() throws Exception {
        final NamedTimeoutConstraint highTimeout = new NamedTimeoutConstraint("test-high-timeout");
        highTimeout.setConnectionTimeout(60000);
        settings.addConfigured(highTimeout);
        final NamedTimeoutConstraint extremelyLowTimeout = new NamedTimeoutConstraint("test-extremely-low-timeout");
        extremelyLowTimeout.setConnectionTimeout(10);
        extremelyLowTimeout.setReadTimeout(20);
        settings.addConfigured(extremelyLowTimeout);

        // setup a HTTP backed repo
        final InetSocketAddress fastServerBindAddr = new InetSocketAddress("localhost", TestHelper.getMaybeAvailablePort());
        final String contextRoot = "/testTimeouts";
        final Path repoRoot = new File("test/repositories/1").toPath();
        assertTrue(repoRoot + " is not a directory", Files.isDirectory(repoRoot));
        final DependencyDescriptor dependency = new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("org1", "mod1.1", "2.0"), false);
        try (final AutoCloseable httpServer = TestHelper.createHttpServerBackedRepository(fastServerBindAddr, contextRoot, repoRoot)) {
            final String ivyPattern = "http://" + fastServerBindAddr.getHostName() + ":" + fastServerBindAddr.getPort()
                    + "/testTimeouts/[organisation]/[module]/ivys/ivy-[revision].xml";
            final String artifactPattern = "http://" + fastServerBindAddr.getHostName() + ":" + fastServerBindAddr.getPort()
                    + "/testTimeouts/[organisation]/[module]/[type]s/[artifact]-[revision].[type]";
            // first use a resolver with a high timeout to make sure
            // it can actually fetch the resources
            final URLResolver highTimeoutResolver = new URLResolver();
            highTimeoutResolver.setName("high-timeout-resolver");
            highTimeoutResolver.setAllownomd(false);
            highTimeoutResolver.setTimeoutConstraint("test-high-timeout");
            highTimeoutResolver.setSettings(settings);
            highTimeoutResolver.setIvyPatterns(Collections.singletonList(ivyPattern));
            highTimeoutResolver.setArtifactPatterns(Collections.singletonList(artifactPattern));
            highTimeoutResolver.validate();

            final TimeoutConstraint resolverTimeoutConstraint = highTimeoutResolver.getTimeoutConstraint();
            assertNotNull("Timeout constraint is missing on resolver " + highTimeoutResolver.getName(), resolverTimeoutConstraint);
            assertEquals("Unexpected connection timeout on resolver", 60000, resolverTimeoutConstraint.getConnectionTimeout());
            assertEquals("Unexpected read timeout on resolver", -1, resolverTimeoutConstraint.getReadTimeout());

            // do the fetch (expected to work fine)
            final ResolvedModuleRevision resolvedModule = highTimeoutResolver.getDependency(dependency, data);
            assertNotNull("Dependency wasn't resolved by resolver " + highTimeoutResolver.getName(), resolvedModule);
            assertEquals("Unexpected dependency resolved by resolver " + highTimeoutResolver.getName(), dependency.getDependencyRevisionId(), resolvedModule.getId());
        }

        // now test this whole fetch using a resolver with a very low connection timeout and
        // by starting the repo server with a delay so that the connection request can timeout

        // clean the cache before testing to ensure the resource isn't fetched from cache
        settings.getDefaultRepositoryCacheManager().clean();
        settings.getResolutionCacheManager().clean();

        final InetSocketAddress slowServerAddr = new InetSocketAddress("localhost", TestHelper.getMaybeAvailablePort());
        final String ivyPattern = "http://" + slowServerAddr.getHostName() + ":" + slowServerAddr.getPort()
                + "/testTimeouts/[organisation]/[module]/ivys/ivy-[revision].xml";
        final String artifactPattern = "http://" + slowServerAddr.getHostName() + ":" + slowServerAddr.getPort()
                + "/testTimeouts/[organisation]/[module]/[type]s/[artifact]-[revision].[type]";
        final URLResolver lowTimeoutResolver = new URLResolver();
        lowTimeoutResolver.setAllownomd(false);
        lowTimeoutResolver.setName("low-timeout-resolver");
        lowTimeoutResolver.setTimeoutConstraint("test-extremely-low-timeout");
        lowTimeoutResolver.setSettings(settings);
        lowTimeoutResolver.setIvyPatterns(Collections.singletonList(ivyPattern));
        lowTimeoutResolver.setArtifactPatterns(Collections.singletonList(artifactPattern));
        lowTimeoutResolver.validate();

        final TimeoutConstraint lowTimeoutConstraint = lowTimeoutResolver.getTimeoutConstraint();
        assertNotNull("Timeout constraint is missing on resolver " + lowTimeoutResolver.getName(), lowTimeoutConstraint);
        assertEquals("Unexpected connection timeout on resolver", 10, lowTimeoutConstraint.getConnectionTimeout());
        assertEquals("Unexpected read timeout on resolver", 20, lowTimeoutConstraint.getReadTimeout());

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final long serverStartupDelayInMillis = 500;
        final Future<AutoCloseable> httpServer = executor.submit(new ServerManager(slowServerAddr, contextRoot, repoRoot, serverStartupDelayInMillis));
        try {
            // do the fetch (resolution *isn't* expected to return resolved module)
            final ResolvedModuleRevision resolvedModuleFromLowTimeouts = lowTimeoutResolver.getDependency(dependency, data);
            assertNull("Dependency wasn't expected to be resolved by resolver " + lowTimeoutResolver.getName(), resolvedModuleFromLowTimeouts);
        } finally {
            try {
                // stop the server
                httpServer.get().close();
            } catch (Exception e) {
                // ignore
                // TODO: Better log it too. But I don't see usage of loggers in test cases currently. So need to get to this later
            }
            try {
                executor.shutdownNow();
            } catch (Exception e) {
                // ignore
                // TODO: Better log it too. But I don't see usage of loggers in test cases currently. So need to get to this later
            }
        }
    }

    private final class ServerManager implements Callable<AutoCloseable> {

        private final InetSocketAddress serverBindAddress;
        private final long startupDelayInMillis;
        private final String contextRoot;
        private final Path localRepoRoot;

        ServerManager(final InetSocketAddress serverBindAddress, final String contextRoot,
                      final Path localRepoRoot, final long startupDelayInMillis) {
            this.serverBindAddress = serverBindAddress;
            this.contextRoot = contextRoot;
            this.localRepoRoot = localRepoRoot;
            this.startupDelayInMillis = startupDelayInMillis;
        }

        @Override
        public AutoCloseable call() throws Exception {
            if (startupDelayInMillis <= 0) {
                return TestHelper.createHttpServerBackedRepository(serverBindAddress, contextRoot, localRepoRoot);
            }
            // wait for the specified amount of startup delay
            Thread.sleep(startupDelayInMillis);
            // start the server
            return TestHelper.createHttpServerBackedRepository(serverBindAddress, contextRoot, localRepoRoot);
        }
    }
}
