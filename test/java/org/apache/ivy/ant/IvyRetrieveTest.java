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
package org.apache.ivy.ant;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.CacheCleaner;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IvyRetrieveTest {
    private static final String IVY_RETRIEVE_PATTERN = "build/test/lib/[organisation]/[module]/ivy-[revision].xml";

    private static final String RETRIEVE_PATTERN = "build/test/lib/[conf]/[artifact]-[revision].[type]";

    private File cache;

    private IvyRetrieve retrieve;

    private Project project;

    @Before
    public void setUp() {
        createCache();
        CacheCleaner.deleteDir(new File("build/test/lib"));
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        retrieve = new IvyRetrieve();
        retrieve.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        retrieve.setPattern(RETRIEVE_PATTERN);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
        CacheCleaner.deleteDir(cache);
        CacheCleaner.deleteDir(new File("build/test/lib"));
    }

    @Test
    public void testSimple() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    @Test
    public void testRetrievePrivateWithWildcard() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-381.xml");
        retrieve.setConf("*");
        retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "1.1",
            "mod1.2", "jar", "jar", "public")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org3", "mod3.2", "1.4",
            "mod3.2", "jar", "jar", "private")).exists());
    }

    /**
     * Test case for IVY-992.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-992">IVY-992</a>
     */
    @Test
    public void testValidateInIvySettings() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest-extra.xml");
        retrieve.getSettings().setValidate(false);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.2",
            "mod1.2", "jar", "jar", "default")).exists());
    }

    @Test
    public void testInline() {
        // we first resolve another ivy file
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());

        // then we resolve a dependency directly
        retrieve.setOrganisation("org1");
        retrieve.setModule("mod1.2");
        retrieve.setRevision("2.0");
        retrieve.setInline(true);
        retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    @Test
    public void testWithConf() {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.1",
            "mod1.2", "jar", "jar", "extension")).exists());
    }

    @Test
    public void testSync() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        retrieve.setSync(true);

        File[] old = new File[] {
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
                    "mod6.1", "jar", "jar", "unknown")), // unknown configuration
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
                    "mod6.1", "unknown", "unknown", "default")), // unknown type
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
                    "unknown", "jar", "jar", "default")), // unknown artifact
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "unknown",
                    "mod6.1", "jar", "jar", "default")), // unknown revision
        };
        for (File of : old) {
            touch(of);
        }
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.1",
            "mod1.2", "jar", "jar", "extension")).exists());
        for (File of : old) {
            assertFalse(of + " should have been deleted by sync", of.exists());
        }
        assertFalse(new File("build/test/lib/unknown").exists()); // even conf directory should
        // have been deleted
    }

    @Test
    public void testSyncWithIgnoreList() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        retrieve.setSync(true);

        new File("build/test/lib/.svn").mkdirs();
        new File("build/test/lib/.svn/test.txt").createNewFile();
        assertTrue(new File("build/test/lib/.svn/test.txt").exists());

        retrieve.execute();

        assertTrue(new File("build/test/lib/.svn/test.txt").exists());
    }

    @Test
    public void testWithAPreviousResolve() {
        // first we do a resolve in another project
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.execute();

        // then we do a retrieve with the correct module information
        retrieve.setOrganisation("apache");
        retrieve.setModule("resolve-simple");
        retrieve.setConf("default");
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    @Test
    public void testWithAPreviousResolveAndResolveId() {
        // first we do a resolve in another project
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setResolveId("testWithAPreviousResolveAndResolveId");
        resolve.execute();

        // then we do a retrieve with the correct module information
        retrieve.setOrganisation("apache");
        retrieve.setModule("resolve-simple");
        retrieve.setConf("default");
        retrieve.setResolveId("testWithAPreviousResolveAndResolveId");
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    /**
     * Test case for IVY-304.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-304">IVY-304</a>
     */
    @Test
    public void testUseOrigin() {
        // first we do a resolve with useOrigin=true in another project
        Project project = TestHelper.newProject();
        project.init();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setUseOrigin(true);
        resolve.execute();

        // then we do a retrieve with the correct module information and useOrigin=false
        retrieve.setOrganisation("apache");
        retrieve.setModule("resolve-simple");
        retrieve.setConf("default");
        retrieve.setUseOrigin(false);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    @Test
    public void testUseOriginWithIvyPattern() {
        // mod2.5 depends on virtual mod2.3 which depends on mod2.1 which depends on mod1.1 which
        // depends on mod1.2
        project.setProperty("ivy.dep.file", "test/repositories/1/org2/mod2.5/ivys/ivy-0.6.1.xml");

        String ivyPattern = IVY_RETRIEVE_PATTERN;

        retrieve.setIvypattern(ivyPattern);
        retrieve.setUseOrigin(true);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org2", "mod2.3", "0.4.1",
            "ivy", "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org2", "mod2.1", "0.3", "ivy",
            "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.1", "1.0", "ivy",
            "ivy", "xml")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.2", "2.0",
            "ivy", "ivy", "xml")).exists());
    }

    /**
     * Test case for IVY-631.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-631">IVY-631</a>
     */
    @Test
    public void testRetrieveWithOriginalNamePattern() {
        retrieve.setFile(new File("test/java/org/apache/ivy/ant/ivy-631.xml"));
        retrieve.setConf("default");
        retrieve.setPattern("build/test/lib/[conf]/[originalname].[ext]");
        retrieve.setSync(true);
        retrieve.execute();

        assertTrue(new File("build/test/lib/default/mod1.2-2.2.jar").exists());
    }

    /**
     * Retrieve without previous resolve must fail.
     */
    @Test(expected = BuildException.class)
    public void testFailureWithoutAPreviousResolve() {
        retrieve.setOrganisation("apache");
        retrieve.setModule("resolve-simple");
        retrieve.setConf("default");
        retrieve.execute();
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        retrieve.execute();
    }

    @Test
    public void testHaltOnFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        retrieve.setHaltonfailure(false);
        retrieve.execute();
    }

    @Test
    public void testCustomIvyPattern() {
        // mod2.5 depends on virtual mod2.3 which depends on mod2.1 which depends on mod1.1 which
        // depends on mod1.2
        project.setProperty("ivy.dep.file", "test/repositories/1/org2/mod2.5/ivys/ivy-0.6.1.xml");

        String ivyPattern = IVY_RETRIEVE_PATTERN;

        retrieve.setIvypattern(ivyPattern);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org2", "mod2.3", "0.4.1",
            "ivy", "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org2", "mod2.1", "0.3", "ivy",
            "ivy", "xml")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.1", "1.0", "ivy",
            "ivy", "xml")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.2", "2.0",
            "ivy", "ivy", "xml")).exists());
    }

    @Test
    public void testCustomIvyPatternWithConf() {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");

        String ivyPattern = "build/test/lib/[conf]/[organisation]/[module]/ivy-[revision].xml";

        retrieve.setIvypattern(ivyPattern);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "extension")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.2", "2.1",
            "ivy", "ivy", "xml", "extension")).exists());
    }

    @Test
    public void testSyncWithIvyPattern() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");

        String ivyPattern = "build/test/lib/[conf]/[organisation]/[module]/ivy-[revision].xml";

        retrieve.setIvypattern(ivyPattern);

        retrieve.setSync(true);

        File[] old = new File[] {
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
                    "mod6.1", "jar", "jar", "unknown")), // unknown configuration
                new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
                    "mod6.1", "unknown", "unknown", "default")), // unknown type
                new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
                    "ivy", "xml", "unk")), // unknown conf for ivy
                new File(IvyPatternHelper.substitute(ivyPattern, "unknown", "mod6.1", "0.4", "ivy",
                    "ivy", "xml", "default")), // unknown organisation for ivy
        };
        for (File of : old) {
            touch(of);
        }

        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "extension")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.2", "2.1",
            "ivy", "ivy", "xml", "extension")).exists());
        for (File of : old) {
            assertFalse(of + " should have been deleted by sync", of.exists());
        }
        assertFalse(new File("build/test/lib/unknown").exists());
        assertFalse(new File("build/test/lib/unk").exists());
        assertFalse(new File("build/test/lib/default/unknown").exists());
    }

    /**
     * Test case for IVY-315.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-315">IVY-315</a>
     */
    @Test
    public void testDoubleRetrieveWithDifferentConfigurations() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-doubleretrieve.xml");

        retrieve.setConf("compile");
        retrieve.execute();

        retrieve = new IvyRetrieve();
        retrieve.setProject(project);
        retrieve.setPattern(RETRIEVE_PATTERN);
        retrieve.setConf("compile,unittest");
        retrieve.execute();
    }

    // creates an empty file, creating parent directories if necessary
    private void touch(File file) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(retrieve.getIvyInstance(), organisation, module,
            revision, artifact, type, ext);
    }

}
