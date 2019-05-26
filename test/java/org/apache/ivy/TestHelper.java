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
package org.apache.ivy;

import com.sun.net.httpserver.Authenticator.Failure;
import com.sun.net.httpserver.Authenticator.Result;
import com.sun.net.httpserver.Authenticator.Retry;
import com.sun.net.httpserver.Authenticator.Success;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class TestHelper {

    public static DefaultArtifact newArtifact(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return new DefaultArtifact(ModuleRevisionId.newInstance(organisation, module, revision),
                new Date(), artifact, type, ext);
    }

    public static File getArchiveFileInCache(Ivy ivy, String mrid, String artifactName,
            String type, String ext) {
        DefaultArtifact artifact = new DefaultArtifact(ModuleRevisionId.parse(mrid), new Date(),
                artifactName, type, ext);
        return getRepositoryCacheManager(ivy, artifact.getModuleRevisionId())
                .getArchiveFileInCache(artifact);
    }

    public static File getArchiveFileInCache(Ivy ivy, String organisation, String module,
            String revision, String artifactName, String type, String ext) {
        DefaultArtifact artifact = newArtifact(organisation, module, revision, artifactName, type,
            ext);
        return getRepositoryCacheManager(ivy, artifact.getModuleRevisionId())
                .getArchiveFileInCache(artifact);
    }

    public static DefaultRepositoryCacheManager getRepositoryCacheManager(Ivy ivy,
            ModuleRevisionId id) {
        // WARN: this doesn't work if the resolver registered is a compound resolver (chain
        // or dual) and a sub resolver doesn't use the same cache manager as the parent
        return (DefaultRepositoryCacheManager) ivy.getSettings().getResolver(id)
                .getRepositoryCacheManager();
    }

    /**
     * Assertion utility methods to test if a collection of {@link ModuleRevisionId} matches a
     * given expected set of mrids.
     * <p>
     * Expected mrids is given as a String of comma separated string representations of
     * {@link ModuleRevisionId}.
     *
     * @param expectedMrids
     *            the expected set of mrids
     * @param mrids
     *            the3 mrids to test
     */
    public static void assertModuleRevisionIds(String expectedMrids,
            Collection<ModuleRevisionId> mrids) {
        Collection<ModuleRevisionId> expected = parseMrids(expectedMrids);
        assertEquals(expected, mrids);
    }

    /**
     * Returns a Set of {@link ModuleRevisionId} corresponding to the given comma separated list of
     * their text representation.
     *
     * @param mrids
     *            the text representation of the {@link ModuleRevisionId}
     * @return a collection of {@link ModuleRevisionId}
     */
    public static Collection<ModuleRevisionId> parseMrids(String mrids) {
        Collection<ModuleRevisionId> c = new LinkedHashSet<>();
        for (String s : mrids.split(",?\\s+")) {
            c.add(ModuleRevisionId.parse(s));
        }
        return c;
    }

    /**
     * Returns an array of {@link ModuleRevisionId} corresponding to the given comma separated list
     * of their text representation.
     *
     * @param mrids
     *            the text representation of the {@link ModuleRevisionId}
     * @return an array of {@link ModuleRevisionId}
     */
    public static ModuleRevisionId[] parseMridsToArray(String mrids) {
        Collection<ModuleRevisionId> parsedMrids = parseMrids(mrids);
        return parsedMrids.toArray(new ModuleRevisionId[parsedMrids.size()]);
    }

    /**
     * Parses a string representation of a module descriptor in micro ivy format.
     * <p>
     * Examples:
     * </p>
     * <pre>
     * #A;1
     * </pre>
     * <hr>
     * <pre>
     * #A;2-&gt; #B;[1.0,1.5]
     * </pre>
     * <hr>
     * <pre>
     * #A;3-&gt; { #B;[1.0,1.5] #C;[2.0,2.5] }
     * </pre>
     *
     * @param microIvy
     *            the micro ivy description of the module descriptor
     * @return the parsed module descriptor
     */
    public static ModuleDescriptor parseMicroIvyDescriptor(String microIvy) {
        Pattern mridPattern = ModuleRevisionId.NON_CAPTURING_PATTERN;
        Matcher m = mridPattern.matcher(microIvy);
        if (m.matches()) {
            return DefaultModuleDescriptor.newBasicInstance(ModuleRevisionId.parse(microIvy),
                new Date());
        }

        Pattern oneDependencyPattern = Pattern.compile(String.format("(%s)\\s*->\\s*(%s)",
                mridPattern.pattern(), mridPattern.pattern()));
        m = oneDependencyPattern.matcher(microIvy);
        if (m.matches()) {
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(
                ModuleRevisionId.parse(m.group(1)), new Date());
            md.addDependency(new DefaultDependencyDescriptor(ModuleRevisionId.parse(m.group(2)),
                    false));
            return md;
        }

        String p = String.format("(%s)\\s*->\\s*\\{\\s*((?:%s,?\\s+)*%s)?\\s*\\}",
                mridPattern.pattern(), mridPattern.pattern(), mridPattern.pattern());
        Pattern multipleDependenciesPattern = Pattern.compile(p);
        m = multipleDependenciesPattern.matcher(microIvy);
        if (m.matches()) {
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(
                ModuleRevisionId.parse(m.group(1)), new Date());
            String mrids = m.group(2);
            if (mrids != null) {
                for (ModuleRevisionId dep : parseMrids(mrids)) {
                    md.addDependency(new DefaultDependencyDescriptor(dep, false));
                }
            }
            return md;
        }
        throw new IllegalArgumentException("invalid micro ivy format: " + microIvy);
    }

    /**
     * Parses a collection of module descriptors in the micro ivy format, separated by double
     * semicolons.
     *
     * @param microIvy
     *            the text representation of the collection of module descriptors
     * @return the collection of module descriptors parsed
     */
    public static Collection<ModuleDescriptor> parseMicroIvyDescriptors(String microIvy) {
        Collection<ModuleDescriptor> r = new ArrayList<>();
        for (String md : microIvy.split("\\s*;;\\s*")) {
            r.add(parseMicroIvyDescriptor(md));
        }
        return r;
    }

    /**
     * Fills a repository with a set of module, using empty files for published artifacts.
     *
     * @param resolver
     *            the resolver to use to publish the modules
     * @param mds
     *            the descriptors of the modules to put in the repository
     * @throws IOException
     *             if an IO problem occurs while filling the repository
     */
    public static void fillRepository(DependencyResolver resolver, Collection<ModuleDescriptor> mds)
            throws IOException {
        File tmp = File.createTempFile("ivy", "tmp");
        try {
            for (ModuleDescriptor md : mds) {
                boolean overwrite = false;
                resolver.beginPublishTransaction(md.getModuleRevisionId(), overwrite);
                boolean published = false;
                try {
                    XmlModuleDescriptorWriter.write(md, tmp);
                    resolver.publish(md.getMetadataArtifact(), tmp, overwrite);
                    tmp.delete();
                    tmp.createNewFile();
                    for (Artifact artifact : md.getAllArtifacts()) {
                        resolver.publish(artifact, tmp, overwrite);
                    }
                    resolver.commitPublishTransaction();
                    published = true;
                } finally {
                    if (!published) {
                        resolver.abortPublishTransaction();
                    }
                }
            }
        } finally {
            tmp.delete();
        }
    }

    /**
     * A file system resolver which can be used with the
     * {@link #fillRepository(DependencyResolver, Collection)} method to create a test case of
     * module descriptor.
     * <p>
     * When finished you should call {@link #cleanTestRepository()}
     * </p>
     *
     * @return FileSystemResolver
     */
    public static FileSystemResolver newTestRepository() {
        FileSystemResolver testRepository = new FileSystemResolver();
        testRepository.setName("test");
        String testRepoDir = new File("build/test/test-repo").getAbsolutePath();
        testRepository.addIvyPattern(testRepoDir
                + "/[organisation]/[module]/[revision]/[artifact].[ext]");
        testRepository.addArtifactPattern(testRepoDir
                + "/[organisation]/[module]/[revision]/[artifact].[ext]");
        return testRepository;
    }

    /**
     * Cleans up the test repository.
     *
     * @see #newTestRepository()
     */
    public static void cleanTestRepository() {
        FileUtil.forceDelete(new File("build/test/test-repo"));
    }

    /**
     * Cleans up the test repository and cache.
     *
     * @see #newTestRepository()
     */
    public static void cleanTest() {
        cleanTestRepository();
        FileUtil.forceDelete(new File("build/test/cache"));
    }

    /**
     * Init a test resolver as default, useful combined with
     * {@link #fillRepository(DependencyResolver, Collection)}.
     *
     * @param settings
     *            the settings to initialize
     * @return test settings
     */
    public static IvySettings loadTestSettings(IvySettings settings) {
        settings.setDefaultCache(new File("build/test/cache"));
        settings.addResolver(newTestRepository());
        settings.setDefaultResolver("test");
        return settings;
    }

    /**
     * Create basic resolve data using the given settings
     *
     * @param settings
     *            the settings to use to create the resolve data
     * @return basic resolve data useful for testing
     */
    public static ResolveData newResolveData(IvySettings settings) {
        return new ResolveData(new ResolveEngine(settings, new EventManager(), new SortEngine(
                settings)), newResolveOptions(settings));
    }

    /**
     * Create basic resolve options using the given settings
     *
     * @param settings
     *            the settings to use to create the resolve options
     * @return the basic resolve options, useful for testing
     */
    public static ResolveOptions newResolveOptions(IvySettings settings) {
        return new ResolveOptions();
    }

    public static Project newProject() {
        Project project = new Project();
        DefaultLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.out);
        project.addBuildListener(logger);
        return project;
    }

    public static File cache = new File("build/cache");

    public static void createCache() {
        cache.mkdirs();
    }

    public static void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    /**
     * Creates a HTTP server, backed by a local file system, which can be used as a repository to
     * serve Ivy module descriptors and artifacts.
     * NOTE: This is supposed to be used only in test cases and only a limited functionality is
     * added in the handler(s) backing the server
     *
     * @param serverAddress           The address to which the server will be bound
     * @param webAppContext           The context root of the application which will be handling
     *                                the requests to the server
     * @param localFilesystemRepoRoot The path to the root directory containing the module
     *                                descriptors and artifacts
     * @return AutoCloseable
     * @throws IOException if something goes wrong
     */
    public static AutoCloseable createHttpServerBackedRepository(final InetSocketAddress serverAddress, final String webAppContext,
                                                                 final Path localFilesystemRepoRoot) throws IOException {
        final LocalFileRepoOverHttp handler = new LocalFileRepoOverHttp(webAppContext, localFilesystemRepoRoot);
        final HttpServer server = HttpServer.create(serverAddress, -1);
        // setup the handler
        server.createContext(webAppContext, handler);
        // start the server
        server.start();
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                final int delaySeconds = 0;
                server.stop(delaySeconds);
            }
        };
    }

    /**
     * Creates a HTTP server, backed by a local file system, which can be used as a repository to
     * serve Ivy module descriptors and artifacts. The context within the server will be backed by
     * {@code BASIC} authentication mechanism with {@code realm} as the realm and
     * {@code validCredentials} as the credentials that the server will recognize. The server will
     * allow access to resources, only if the credentials that are provided by the request, belong
     * to these credentials.
     * <p>
     * NOTE: This is supposed to be used only in test cases and only a limited functionality is
     * added in the handler(s) backing the server
     *
     * @param serverAddress           The address to which the server will be bound
     * @param webAppContext           The context root of the application which will be handling
     *                                the requests to the server
     * @param localFilesystemRepoRoot The path to the root directory containing the module
     *                                descriptors and artifacts
     * @param realm                   The realm to use for the {@code BASIC} auth mechanism
     * @param validCredentials        A {@link Map} of valid credentials, the key being the user
     *                                name and the value being the password, that the server will
     *                                use during the authentication process of the incoming requests
     * @return AutoCloseable
     * @throws IOException if something goes wrong
     */
    public static AutoCloseable createBasicAuthHttpServerBackedRepo(final InetSocketAddress serverAddress, final String webAppContext,
                                                                    final Path localFilesystemRepoRoot, final String realm,
                                                                    final Map<String, String> validCredentials) throws IOException {
        final LocalFileRepoOverHttp handler = new LocalFileRepoOverHttp(webAppContext, localFilesystemRepoRoot);
        final HttpServer server = HttpServer.create(serverAddress, -1);
        // setup the handler
        final HttpContext context = server.createContext(webAppContext, handler);
        // setup basic auth on this context
        final com.sun.net.httpserver.Authenticator authenticator = new BasicAuthenticator(realm) {
            @Override
            public boolean checkCredentials(final String user, final String pass) {
                if (validCredentials == null || !validCredentials.containsKey(user)) {
                    return false;
                }
                final String expectedPass = validCredentials.get(user);
                return expectedPass != null && expectedPass.equals(pass);
            }
        };
        context.setAuthenticator(authenticator);
        // setup a auth filter backed by the authenticator
        context.getFilters().add(new AuthFilter(authenticator));
        // start the server
        server.start();
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                final int delaySeconds = 0;
                server.stop(delaySeconds);
            }
        };
    }

    private static final class AuthFilter extends Filter {

        private final com.sun.net.httpserver.Authenticator authenticator;

        AuthFilter(final com.sun.net.httpserver.Authenticator authenticator) {
            this.authenticator = authenticator;
        }

        @Override
        public void doFilter(final HttpExchange httpExchange, final Chain chain) throws IOException {
            if (authenticator == null) {
                chain.doFilter(httpExchange);
                return;
            }
            final Result authResult = this.authenticator.authenticate(httpExchange);
            if (authResult instanceof Success) {
                @SuppressWarnings("unused")
                final Success success = (Success) authResult;
                // auth succeeded - move to next filter
                chain.doFilter(httpExchange);
            } else if (authResult instanceof Retry) {
                final Retry retry = (Retry) authResult;
                this.drainInput(httpExchange);
                // send auth retry (401)
                httpExchange.sendResponseHeaders(retry.getResponseCode(), -1L);
            } else if (authResult instanceof Failure) {
                final Failure failure = (Failure) authResult;
                this.drainInput(httpExchange);
                // send auth failure (401)
                httpExchange.sendResponseHeaders(failure.getResponseCode(), -1L);
            }
        }

        private void drainInput(final HttpExchange httpExchange) throws IOException {
            try (final InputStream is = httpExchange.getRequestBody()) {
                final byte[] content = new byte[1024];
                while (is.read(content) != -1) {
                    // ignore the content
                }
            }
        }

        @Override
        public String description() {
            return "AuthFilter";
        }
    }

    /**
     * Find a TCP/IP port which may continue to be available.
     * <br />
     * The returned port is available since a socket has successfully bind to it, but this availability is not ensured
     * after this method since the associated socket is released and some other process can now use it.
     */
    public static int getMaybeAvailablePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException e) {
            // ignore
        }
        throw new IllegalStateException("Not TCP/IP port available");
    }
}
