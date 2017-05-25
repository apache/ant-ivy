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
import java.io.IOException;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.util.CacheCleaner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyRetrieveTest extends TestCase {
    private static final String IVY_RETRIEVE_PATTERN = "build/test/lib/[organisation]/[module]/ivy-[revision].xml";

    private static final String RETRIEVE_PATTERN = "build/test/lib/[conf]/[artifact]-[revision].[type]";

    private File cache;

    private IvyRetrieve retrieve;

    private Project project;

    protected void setUp() throws Exception {
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

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
        CacheCleaner.deleteDir(new File("build/test/lib"));
    }

    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.0",
            "mod1.2", "jar", "jar")).exists());
    }

    public void testRetrievePrivateWithWildcard() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-381.xml");
        retrieve.setConf("*");
        retrieve.execute();
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "1.1",
            "mod1.2", "jar", "jar", "public")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org3", "mod3.2", "1.4",
            "mod3.2", "jar", "jar", "private")).exists());
    }

    public void testValidateInIvySettings() throws Exception {
        // cfr IVY-992
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest-extra.xml");
        retrieve.getSettings().setValidate(false);
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.2",
            "mod1.2", "jar", "jar", "default")).exists());
    }

    public void testInline() throws Exception {
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

    public void testWithConf() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.1",
            "mod1.2", "jar", "jar", "extension")).exists());
    }

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
        for (int i = 0; i < old.length; i++) {
            touch(old[i]);
        }
        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org6", "mod6.1", "0.4",
            "mod6.1", "jar", "jar", "extension")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(RETRIEVE_PATTERN, "org1", "mod1.2", "2.1",
            "mod1.2", "jar", "jar", "extension")).exists());
        for (int i = 0; i < old.length; i++) {
            assertFalse(old[i] + " should have been deleted by sync", old[i].exists());
        }
        assertFalse(new File("build/test/lib/unknown").exists()); // even conf directory should
        // have been deleted
    }

    public void testSyncWithIgnoreList() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/1/org6/mod6.2/ivys/ivy-0.4.xml");
        retrieve.setSync(true);

        new File("build/test/lib/.svn").mkdirs();
        new File("build/test/lib/.svn/test.txt").createNewFile();
        assertTrue(new File("build/test/lib/.svn/test.txt").exists());

        retrieve.execute();

        assertTrue(new File("build/test/lib/.svn/test.txt").exists());
    }

    public void testWithAPreviousResolve() throws Exception {
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

    public void testWithAPreviousResolveAndResolveId() throws Exception {
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

    public void testUseOrigin() throws Exception {
        // test case for IVY-304
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

    public void testUseOriginWithIvyPattern() throws Exception {
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

    public void testRetrieveWithOriginalNamePattern() throws Exception {
        retrieve.setFile(new File("test/java/org/apache/ivy/ant/ivy-631.xml"));
        retrieve.setConf("default");
        retrieve.setPattern("build/test/lib/[conf]/[originalname].[ext]");
        retrieve.setSync(true);
        retrieve.execute();

        assertTrue(new File("build/test/lib/default/mod1.2-2.2.jar").exists());
    }

    public void testFailureWithoutAPreviousResolve() throws Exception {
        // we do a retrieve with the module information whereas no resolve has been previously done
        try {
            retrieve.setOrganisation("apache");
            retrieve.setModule("resolve-simple");
            retrieve.setConf("default");
            retrieve.execute();
            fail("retrieve without previous resolve should have thrown an exception");
        } catch (Exception ex) {
            // OK
        }
    }

    public void testFailure() throws Exception {
        try {
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            retrieve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raised an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
            retrieve.setHaltonfailure(false);
            retrieve.execute();

        } catch (BuildException ex) {
            fail("failure raised an exception with haltonfailure set to false");
        }
    }

    public void testCustomIvyPattern() throws Exception {
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

    public void testCustomIvyPatternWithConf() throws Exception {
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
        for (int i = 0; i < old.length; i++) {
            touch(old[i]);
        }

        retrieve.execute();

        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "default")).exists());
        assertTrue(new File(IvyPatternHelper.substitute(ivyPattern, "org6", "mod6.1", "0.4", "ivy",
            "ivy", "xml", "extension")).exists());
        assertFalse(new File(IvyPatternHelper.substitute(ivyPattern, "org1", "mod1.2", "2.1",
            "ivy", "ivy", "xml", "extension")).exists());
        for (int i = 0; i < old.length; i++) {
            assertFalse(old[i] + " should have been deleted by sync", old[i].exists());
        }
        assertFalse(new File("build/test/lib/unknown").exists());
        assertFalse(new File("build/test/lib/unk").exists());
        assertFalse(new File("build/test/lib/default/unknown").exists());
    }

    public void testDoubleRetrieveWithDifferentConfigurations() {
        // IVY-315
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
