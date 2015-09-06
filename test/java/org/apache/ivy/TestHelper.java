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
package org.apache.ivy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import junit.framework.Assert;

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
        // WARN: this doesn't work if the resolver registered is a compound resolver (chain or dual)
        // and a sub resolver doesn't use the same cache manager as the parent
        return (DefaultRepositoryCacheManager) ivy.getSettings().getResolver(id)
                .getRepositoryCacheManager();
    }

    /**
     * Assertion utility methods to test if a collection of {@link ModuleRevisionId} matches a given
     * expected set of mrids.
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
            Collection/* <ModuleRevisionId> */mrids) {
        Collection expected = parseMrids(expectedMrids);
        Assert.assertEquals(expected, mrids);
    }

    /**
     * Returns a Set of {@link ModuleRevisionId} corresponding to the given comma separated list of
     * their text representation.
     * 
     * @param mrids
     *            the text representation of the {@link ModuleRevisionId}
     * @return a collection of {@link ModuleRevisionId}
     */
    public static Collection parseMrids(String mrids) {
        String[] m = mrids.split(",?\\s+");
        Collection c = new LinkedHashSet();
        for (int i = 0; i < m.length; i++) {
            c.add(ModuleRevisionId.parse(m[i]));
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
        Collection parsedMrids = parseMrids(mrids);
        return (ModuleRevisionId[]) parsedMrids.toArray(new ModuleRevisionId[parsedMrids.size()]);
    }

    /**
     * Parses a string represenation of a module descriptor in micro ivy format.
     * <p>
     * Examples:
     * 
     * <pre>
     * #A;1
     * </pre>
     * 
     * <hr/>
     * 
     * <pre>
     * #A;2-> #B;[1.0,1.5]
     * </pre>
     * 
     * <hr/>
     * 
     * <pre>
     * #A;3-> { #B;[1.0,1.5] #C;[2.0,2.5] }
     * </pre>
     * 
     * </p>
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

        Pattern oneDependencyPattern = Pattern.compile("(" + mridPattern.pattern() + ")\\s*->\\s*("
                + mridPattern.pattern() + ")");
        m = oneDependencyPattern.matcher(microIvy);
        if (m.matches()) {
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(
                ModuleRevisionId.parse(m.group(1)), new Date());
            md.addDependency(new DefaultDependencyDescriptor(ModuleRevisionId.parse(m.group(2)),
                    false));
            return md;
        }

        String p = "(" + mridPattern.pattern() + ")\\s*->\\s*\\{\\s*((?:" + mridPattern.pattern()
                + ",?\\s+)*" + mridPattern.pattern() + ")?\\s*\\}";
        Pattern multipleDependenciesPattern = Pattern.compile(p);
        m = multipleDependenciesPattern.matcher(microIvy);
        if (m.matches()) {
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(
                ModuleRevisionId.parse(m.group(1)), new Date());
            String mrids = m.group(2);
            if (mrids != null) {
                Collection depMrids = parseMrids(mrids);
                for (Iterator iter = depMrids.iterator(); iter.hasNext();) {
                    ModuleRevisionId dep = (ModuleRevisionId) iter.next();
                    md.addDependency(new DefaultDependencyDescriptor(dep, false));
                }
            }
            return md;
        }
        throw new IllegalArgumentException("invalid micro ivy format: " + microIvy);
    }

    /**
     * Parses a collection of module descriptors in the micro ivy format, separated by double semi
     * columns.
     * 
     * @param microIvy
     *            the text representation of the collection of module descriptors
     * @return the collection of module descriptors parsed
     */
    public static Collection/* <ModuleDescriptor> */parseMicroIvyDescriptors(String microIvy) {
        String[] mds = microIvy.split("\\s*;;\\s*");
        Collection r = new ArrayList();
        for (int i = 0; i < mds.length; i++) {
            r.add(parseMicroIvyDescriptor(mds[i]));
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
    public static void fillRepository(DependencyResolver resolver,
            Collection/* <ModuleDescriptor> */mds) throws IOException {
        File tmp = File.createTempFile("ivy", "tmp");
        try {
            for (Iterator iter = mds.iterator(); iter.hasNext();) {
                boolean overwrite = false;
                ModuleDescriptor md = (ModuleDescriptor) iter.next();
                resolver.beginPublishTransaction(md.getModuleRevisionId(), overwrite);
                boolean published = false;
                try {
                    XmlModuleDescriptorWriter.write(md, tmp);
                    resolver.publish(md.getMetadataArtifact(), tmp, overwrite);
                    tmp.delete();
                    tmp.createNewFile();
                    Artifact[] artifacts = md.getAllArtifacts();
                    for (int i = 0; i < artifacts.length; i++) {
                        resolver.publish(artifacts[i], tmp, overwrite);
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
     * @see #newTestSettings()
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
}
