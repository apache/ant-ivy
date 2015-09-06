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

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Parallel;

import junit.framework.TestCase;

public class IvyResolveTest extends TestCase {

    private Project project;

    private IvyResolve resolve;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());

        resolve = new IvyResolve();
        resolve.setProject(project);
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testIVY1455() throws Exception {
        project.setProperty("ivy.settings.file", "test/repositories/IVY-1455/ivysettings.xml");
        resolve.setFile(new File("test/repositories/IVY-1455/ivy.xml"));
        resolve.execute();
    }

    /* disabled: Ivy is not thread-safe, and this usage is not supported at this time */
    public void disabledIVY1454() throws Exception {
        // run init in parent thread, then resolve in children
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-with-nio.xml");
        project.setProperty("ivy.log.locking", "true");
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));

        Parallel parallel = new Parallel();
        parallel.setThreadCount(4);
        parallel.addTask(resolve);
        parallel.addTask(resolve);
        parallel.addTask(resolve);
        parallel.addTask(resolve);
        parallel.execute();
    }

    public void testIVY779() throws Exception {
        Project project = TestHelper.newProject();
        project.setProperty("ivy.local.default.root",
            new File("test/repositories/norev").getAbsolutePath());
        project.setProperty("ivy.local.default.ivy.pattern", "[module]/[artifact].[ext]");
        project.setProperty("ivy.local.default.artifact.pattern", "[module]/[artifact].[ext]");

        resolve.setProject(project);
        project.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
        resolve.setFile(new File("test/repositories/norev/ivy.xml"));
        resolve.setKeep(true);
        resolve.execute();

        ResolveReport report = (ResolveReport) project.getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getArtifacts().size());
    }

    public void testSimple() throws Exception {
        // depends on org="org1" name="mod1.2" rev="2.0"
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.execute();

        assertTrue(getResolvedIvyFileInCache(
            ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testResolveWithoutIvyFile() throws Exception {
        // IVY-630
        resolve.getProject().setProperty("ivy.settings.file",
            "test/repositories/IVY-630/ivysettings.xml");

        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-630.xml"));
        resolve.setConf("default");
        resolve.setHaltonfailure(false);
        resolve.execute();

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("junit", "junit", "4.1"))
                .exists());
        assertTrue(getArchiveFileInCache("junit", "junit", "4.1", "junit", "jar", "jar").exists());
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(getIvy(), organisation, module, revision, artifact,
            type, ext);
    }

    private File getIvyFileInCache(ModuleRevisionId id) {
        return TestHelper.getRepositoryCacheManager(getIvy(), id).getIvyFileInCache(id);
    }

    private File getResolvedIvyFileInCache(ModuleRevisionId id) {
        return getIvy().getResolutionCacheManager().getResolvedIvyFileInCache(id);
    }

    public void testInline() throws Exception {
        // same as before, but expressing dependency directly without ivy file
        resolve.setOrganisation("org1");
        resolve.setModule("mod1.2");
        resolve.setRevision("2.0");
        resolve.setInline(true);
        resolve.execute();

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testInlineWithResolveId() throws Exception {
        // same as before, but expressing dependency directly without ivy file
        resolve.setOrganisation("org1");
        resolve.setModule("mod1.2");
        resolve.setRevision("2.0");
        resolve.setInline(true);
        resolve.setResolveId("testInlineWithResolveId");
        resolve.setKeep(true);
        resolve.execute();

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testInlineForNonExistingModule() throws Exception {
        resolve.setOrganisation("org1XX");
        resolve.setModule("mod1.2");
        resolve.setRevision("2.0");
        resolve.setInline(true);
        resolve.setHaltonfailure(false);
        resolve.setFailureProperty("failure.property");
        resolve.execute();

        // the resolve must have failed -> the failure property must be set
        String failure = resolve.getProject().getProperty("failure.property");
        assertTrue("Failure property must have been specified!", Boolean.valueOf(failure)
                .booleanValue());
    }

    public void testWithSlashes() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/core/resolve/ivy-198.xml"));
        resolve.execute();

        File resolvedIvyFileInCache = getResolvedIvyFileInCache(ModuleRevisionId.newInstance(
            "myorg/mydep", "system/module", "1.0"));
        assertTrue(resolvedIvyFileInCache.exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        assertTrue(getIvyFileInCache(
            ModuleRevisionId.newInstance("yourorg/yourdep", "yoursys/yourmod", "1.0")).exists());
        assertTrue(getArchiveFileInCache("yourorg/yourdep", "yoursys/yourmod", "1.0", "yourmod",
            "jar", "jar").exists());
    }

    public void testDepsChanged() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.execute();

        assertEquals("true", getIvy().getVariable("ivy.deps.changed"));

        resolve.execute();

        assertEquals("false", getIvy().getVariable("ivy.deps.changed"));
    }

    public void testDontCheckIfChanged() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setCheckIfChanged(false);
        resolve.execute();
        assertNull(getIvy().getVariable("ivy.deps.changed"));
        resolve.execute();
        assertNull(getIvy().getVariable("ivy.deps.changed"));
        // To be complete, we should also check that the XmlReportParser is not invoked
        // but this would require a too big refactoring to inject a mock object
    }

    public void testConflictingDepsChanged() throws Exception {
        resolve.setFile(new File("test/repositories/2/mod4.1/ivy-4.1.xml"));
        resolve.execute();

        assertEquals("true", getIvy().getVariable("ivy.deps.changed"));

        resolve.execute();

        assertEquals("false", getIvy().getVariable("ivy.deps.changed"));
    }

    public void testDouble() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.execute();

        assertEquals("resolve-simple", getIvy().getVariable("ivy.module"));
        assertEquals("1.0", getIvy().getVariable("ivy.revision"));

        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-double.xml"));
        resolve.execute();

        assertEquals("resolve-double", getIvy().getVariable("ivy.module"));
        assertEquals("1.1", getIvy().getVariable("ivy.revision"));
    }

    public void testFailure() throws Exception {
        try {
            resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testIvyLogModulesInUseWithFailure() throws Exception {
        resolve.getProject().setProperty("ivy.log.modules.in.use", "true");
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
        resolve.setHaltonfailure(false);
        resolve.execute();

        // we did manage to get here, so no NPE has been thrown (IVY-961)
    }

    public void testFailureWithMissingConfigurations() throws Exception {
        try {
            resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            resolve.setConf("default,unknown");
            resolve.execute();
            fail("missing configurations didn't raised an exception");
        } catch (BuildException ex) {
            assertTrue(ex.getMessage().indexOf("unknown") != -1);
        }
    }

    public void testFailureOnBadDependencyIvyFile() throws Exception {
        try {
            resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure2.xml"));
            resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testFailureOnBadStatusInDependencyIvyFile() throws Exception {
        try {
            resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure3.xml"));
            resolve.execute();
            fail("failure didn't raised an exception with default haltonfailure setting");
        } catch (BuildException ex) {
            // ok => should raise an exception
        }
    }

    public void testHaltOnFailure() throws Exception {
        try {
            resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-failure.xml"));
            resolve.setHaltonfailure(false);
            resolve.execute();
        } catch (BuildException ex) {
            ex.printStackTrace();
            fail("failure raised an exception with haltonfailure set to false");
        }
    }

    public void testWithResolveId() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("testWithResolveId");
        resolve.execute();

        assertTrue(getResolvedIvyFileInCache(
            ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
        assertTrue(getIvy().getResolutionCacheManager()
                .getConfigurationResolveReportInCache("testWithResolveId", "default").exists());

        // dependencies
        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());

        // test the properties
        Project project = resolve.getProject();
        assertEquals("apache", project.getProperty("ivy.organisation"));
        assertEquals("apache", project.getProperty("ivy.organisation.testWithResolveId"));
        assertEquals("resolve-simple", project.getProperty("ivy.module"));
        assertEquals("resolve-simple", project.getProperty("ivy.module.testWithResolveId"));
        assertEquals("1.0", project.getProperty("ivy.revision"));
        assertEquals("1.0", project.getProperty("ivy.revision.testWithResolveId"));
        assertEquals("true", project.getProperty("ivy.deps.changed"));
        assertEquals("true", project.getProperty("ivy.deps.changed.testWithResolveId"));
        assertEquals("default", project.getProperty("ivy.resolved.configurations"));
        assertEquals("default",
            project.getProperty("ivy.resolved.configurations.testWithResolveId"));

        // test the references
        assertNotNull(project.getReference("ivy.resolved.report"));
        assertNotNull(project.getReference("ivy.resolved.report.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.descriptor"));
        assertNotNull(project.getReference("ivy.resolved.descriptor.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref.testWithResolveId"));
    }

    public void testDoubleResolveWithResolveId() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("testWithResolveId");
        resolve.execute();

        IvyResolve newResolve = new IvyResolve();
        newResolve.setProject(resolve.getProject());
        newResolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple2.xml"));
        newResolve.execute();

        // test the properties
        Project project = resolve.getProject();
        assertEquals("apache2", project.getProperty("ivy.organisation"));
        assertEquals("apache", project.getProperty("ivy.organisation.testWithResolveId"));
        assertEquals("resolve-simple2", project.getProperty("ivy.module"));
        assertEquals("resolve-simple", project.getProperty("ivy.module.testWithResolveId"));
        assertEquals("1.1", project.getProperty("ivy.revision"));
        assertEquals("1.0", project.getProperty("ivy.revision.testWithResolveId"));
        assertEquals("true", project.getProperty("ivy.deps.changed"));
        assertEquals("true", project.getProperty("ivy.deps.changed.testWithResolveId"));
        assertEquals("default", project.getProperty("ivy.resolved.configurations"));
        assertEquals("default",
            project.getProperty("ivy.resolved.configurations.testWithResolveId"));

        // test the references
        assertNotNull(project.getReference("ivy.resolved.report"));
        assertNotNull(project.getReference("ivy.resolved.report.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.descriptor"));
        assertNotNull(project.getReference("ivy.resolved.descriptor.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref.testWithResolveId"));
    }

    public void testDifferentResolveWithSameResolveId() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("testWithResolveId");
        resolve.execute();

        IvyResolve newResolve = new IvyResolve();
        newResolve.setProject(resolve.getProject());
        newResolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple2.xml"));
        newResolve.setResolveId("testWithResolveId");
        newResolve.execute();

        // test the properties
        Project project = resolve.getProject();
        assertEquals("apache2", project.getProperty("ivy.organisation"));
        assertEquals("apache2", project.getProperty("ivy.organisation.testWithResolveId"));
        assertEquals("resolve-simple2", project.getProperty("ivy.module"));
        assertEquals("resolve-simple2", project.getProperty("ivy.module.testWithResolveId"));
        assertEquals("1.1", project.getProperty("ivy.revision"));
        assertEquals("1.1", project.getProperty("ivy.revision.testWithResolveId"));
        assertEquals("true", project.getProperty("ivy.deps.changed"));
        assertEquals("true", project.getProperty("ivy.deps.changed.testWithResolveId"));
        assertEquals("default", project.getProperty("ivy.resolved.configurations"));
        assertEquals("default",
            project.getProperty("ivy.resolved.configurations.testWithResolveId"));

        // test the references
        assertNotNull(project.getReference("ivy.resolved.report"));
        assertNotNull(project.getReference("ivy.resolved.report.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.descriptor"));
        assertNotNull(project.getReference("ivy.resolved.descriptor.testWithResolveId"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref"));
        assertNotNull(project.getReference("ivy.resolved.configurations.ref.testWithResolveId"));
    }

    public void testExcludedConf() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setConf("*,!default");
        resolve.execute();

        assertTrue(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.1", "2.0"))
                .exists());
        assertFalse(getIvyFileInCache(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"))
                .exists());

        // test the properties
        Project project = resolve.getProject();
        assertFalse(project.getProperty("ivy.resolved.configurations").indexOf("default") > -1);
    }

    public void testResolveWithAbsoluteFile() {
        // IVY-396
        File ivyFile = new File("test/java/org/apache/ivy/ant/ivy-simple.xml");
        resolve.getProject().setProperty("ivy.dep.file", ivyFile.getAbsolutePath());
        resolve.execute();

        assertTrue(getResolvedIvyFileInCache(
            ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
    }

    public void testResolveWithRelativeFile() {
        // IVY-396
        resolve.getProject().setProperty("ivy.dep.file",
            "test/java/org/apache/ivy/ant/ivy-simple.xml");
        resolve.execute();

        assertTrue(getResolvedIvyFileInCache(
            ModuleRevisionId.newInstance("apache", "resolve-simple", "1.0")).exists());
    }

    private Ivy getIvy() {
        return resolve.getIvyInstance();
    }

    public void testChildsSimple() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
    }

    public void testChildsMultiple() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.3");
        dependency.setRev("0.7");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.3", "0.7", "mod2.3", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21A", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.3", "art21B", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
    }

    public void testChildsMultipleWithConf() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("A");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());
    }

    public void testChildsMultipleWithConf2() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org1", "mod1.1", "1.0", "mod1.1", "jar", "jar").exists());
    }

    public void testChildsExclude() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        IvyExclude exclude = resolve.createExclude();
        exclude.setOrg("org1");
        exclude.setModule("mod1.1");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());
    }

    public void testChildsDependencyExclude() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.10");
        dependency.setConf("B");

        IvyDependencyExclude exclude = dependency.createExclude();
        exclude.setOrg("org1");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.10", "mod2.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.1", "0.7", "mod2.1", "jar", "jar").exists());
    }

    public void testChildsDependencyInclude() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("mod1.2");
        dependency.setRev("2.0");

        dependency = resolve.createDependency();
        dependency.setOrg("org2");
        dependency.setName("mod2.2");
        dependency.setRev("0.9");

        IvyDependencyInclude include = dependency.createInclude();
        include.setName("art22-1");

        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar").exists());
        assertTrue(getArchiveFileInCache("org2", "mod2.2", "0.9", "art22-1", "jar", "jar").exists());
    }

    public void testChildsFail() throws Exception {
        IvyDependency dependency = resolve.createDependency();
        dependency.setOrg("org1");
        dependency.setName("noexisting");
        dependency.setRev("2.0");

        try {
            resolve.execute();
            fail("A fail resolved should have raised a build exception");
        } catch (BuildException e) {
            // ok
        }
    }

    public void testSimpleExtends() throws Exception {
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-extends-multiconf.xml"));
        resolve.execute();
        assertEquals("1", resolve.getProject().getProperty("ivy.parents.count"));
        assertEquals("apache", resolve.getProject().getProperty("ivy.parent[0].organisation"));
        assertEquals("resolve-simple", resolve.getProject().getProperty("ivy.parent[0].module"));
        assertEquals("1.0", resolve.getProject().getProperty("ivy.parent[0].revision"));
        assertEquals(null, resolve.getProject().getProperty("ivy.parent[0].branch"));
    }

}
