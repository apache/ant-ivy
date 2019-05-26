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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.status.Status;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IvyConfigureTest {
    private IvyConfigure configure;

    private Project project;

    @Rule
    public ExpectedException expExc = ExpectedException.none();

    @Before
    public void setUp() {
        project = TestHelper.newProject();
        project.setProperty("myproperty", "myvalue");

        configure = new IvyConfigure();
        configure.setProject(project);
    }

    private Ivy getIvyInstance() {
        IvyTask task = new IvyTask() {
            public void doExecute() throws BuildException {
            }
        };
        task.setProject(project);
        task.init();

        Reference ref = new Reference(task.getProject(), configure.getSettingsId());
        // ref.setProject(project);
        task.setSettingsRef(ref);
        return task.getIvyInstance();
    }

    @Test
    public void testDefaultCacheDir() {
        // test with an URL
        configure.setUrl(getClass().getResource("ivysettings-defaultCacheDir.xml"));
        configure.setSettingsId("test");
        configure.execute();

        assertEquals(new File("mycache").getAbsolutePath(),
            project.getProperty("ivy.cache.dir.test"));

        // test with a File
        project = TestHelper.newProject();
        configure = new IvyConfigure();
        configure.setProject(project);
        configure.setFile(new File("test/java/org/apache/ivy/ant/ivysettings-defaultCacheDir.xml"));
        configure.setSettingsId("test2");
        configure.execute();

        assertEquals(new File("mycache").getAbsolutePath(),
            project.getProperty("ivy.cache.dir.test2"));

        // test if no defaultCacheDir is specified
        project = TestHelper.newProject();
        configure = new IvyConfigure();
        configure.setProject(project);
        configure
                .setFile(new File("test/java/org/apache/ivy/ant/ivysettings-noDefaultCacheDir.xml"));
        configure.setSettingsId("test3");
        configure.execute();

        assertNotNull(project.getProperty("ivy.cache.dir.test3"));
    }

    @Test
    public void testDefault() {
        // by default settings look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        configure.getProject().setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertNotNull(publicResolver);
        assertTrue(publicResolver instanceof IBiblioResolver);
        IBiblioResolver ibiblio = (IBiblioResolver) publicResolver;
        assertTrue(ibiblio.isM2compatible());
    }

    @Test
    public void testDefault14() {
        // by default settings look in the current directory for an ivysettings.xml file...
        // but Ivy itself has one, and we don't want to use it
        configure.getProject().setProperty("ivy.settings.file", "no/settings/will/use/default.xml");
        configure.getProject().setProperty("ivy.14.compatible", "true");
        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings.getDefaultResolver());

        DependencyResolver publicResolver = settings.getResolver("public");
        assertTrue(publicResolver instanceof IvyRepResolver);
    }

    @Test
    public void testFile() throws Exception {
        configure.setFile(new File("test/repositories/ivysettings.xml"));

        configure.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);
        IvySettings settings = ivy.getSettings();
        assertNotNull(settings);

        assertEquals(new File("build/cache").getAbsoluteFile(), settings.getDefaultCache());
        assertEquals(new File("test/repositories/ivysettings.xml").getAbsolutePath(), settings
                .getVariables().getVariable("ivy.settings.file"));
        assertEquals(
            new File("test/repositories/ivysettings.xml").toURI().toURL().toExternalForm(),
            settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(new File("test/repositories").getAbsolutePath(), settings.getVariables()
                .getVariable("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    @Test
    public void testURL() throws Exception {
        String confUrl = new File("test/repositories/ivysettings-url.xml").toURI().toURL()
                .toExternalForm();
        String confDirUrl = new File("test/repositories").toURI().toURL().toExternalForm();
        if (confDirUrl.endsWith("/")) {
            confDirUrl = confDirUrl.substring(0, confDirUrl.length() - 1);
        }
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();

        assertEquals(new File("build/cache").getAbsoluteFile(), settings.getDefaultCache());
        assertEquals(confUrl, settings.getVariables().getVariable("ivy.settings.url"));
        assertEquals(confDirUrl, settings.getVariables().getVariable("ivy.settings.dir"));
        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
    }

    @Test
    public void testAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-test.xml")
                .toExternalForm();
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("myvalue", settings.getVariables().getVariable("myproperty"));
        assertEquals("myvalue", settings.getDefaultResolver().getName());
    }

    @Test
    public void testOverrideVariables() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        configure.setUrl(confUrl);

        configure.execute();

        IvySettings settings = getIvyInstance().getSettings();
        assertNotNull(settings);

        assertEquals("lib/test/[artifact]-[revision].[ext]",
            settings.getVariables().getVariable("ivy.retrieve.pattern"));
    }

    @Test
    public void testExposeAntProperties() throws Exception {
        String confUrl = IvyConfigureTest.class.getResource("ivysettings-props.xml")
                .toExternalForm();
        configure.setUrl(confUrl);
        configure.setSettingsId("this.id");

        configure.execute();

        assertNotNull(getIvyInstance());

        assertEquals("value", configure.getProject().getProperty("ivy.test.variable"));
        assertEquals("value", configure.getProject().getProperty("ivy.test.variable.this.id"));
    }

    /**
     * Test case for IVY-601.
     *
     * @see <a href="https://issues.apache.org/jira/browse/IVY-601">IVY-601</a>
     */
    @Test
    public void testIncludeTwice() {
        configure.setFile(new File("test/java/org/apache/ivy/ant/ivysettings-include-twice.xml"));

        configure.execute();

        assertNotNull(getIvyInstance());
    }

    @Test
    public void testOverrideTrue() {
        configure.setFile(new File("test/repositories/ivysettings.xml"));
        configure.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        configure = new IvyConfigure();
        configure.setProject(project);
        configure.setOverride("true");
        configure.setFile(new File("test/repositories/ivysettings.xml"));
        configure.execute();
        assertNotNull(getIvyInstance());

        assertNotSame(ivy, getIvyInstance());
    }

    @Test
    public void testOverrideFalse() {
        configure.setFile(new File("test/repositories/ivysettings.xml"));
        configure.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        IvyConfigure newAntSettings = new IvyConfigure();
        newAntSettings.setProject(project);
        newAntSettings.setOverride("false");
        newAntSettings.setFile(new File("test/repositories/ivysettings.xml"));
        newAntSettings.execute();

        assertSame(ivy, getIvyInstance());
    }

    /**
     * Calling settings twice with the same id with override=notallowed must fail
     */
    @Test
    public void testOverrideNotAllowed() {
        expExc.expect(BuildException.class);
        expExc.expectMessage("Overriding a previous definition of ivy:settings with the id '"
                + configure.getSettingsId() + "' is not allowed when using override='notallowed'.");

        configure.setFile(new File("test/repositories/ivysettings.xml"));
        configure.execute();

        Ivy ivy = getIvyInstance();
        assertNotNull(ivy);

        configure = new IvyConfigure();
        configure.setProject(project);
        configure.setOverride("notallowed");
        configure.setFile(new File("test/repositories/ivysettings.xml"));

        configure.execute();
    }

    /**
     * Settings override with invalid value must fail.
     */
    @Test
    public void testInvalidOverride() {
        expExc.expect(IllegalArgumentException.class);
        expExc.expectMessage("invalid override value 'unknown'. Valid values are "
                + "[true, false, notallowed]");

        configure.setOverride("unknown");
    }

    /**
     * Tests that if the Ivy settings file <code>include</code>s another file as
     * <code>optional</code>, then the absence of that file doesn't lead to failures
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testOptionalFileInclude() throws Exception {
        final File ivySettingsXml = new File("test/repositories/ivysettings-optional-file-include.xml");
        final Ivy ivy = new Ivy();
        ivy.configure(ivySettingsXml);
        final IvySettings ivySettings = ivy.getSettings();
        // just test that it indeed parsed fine
        assertTrue("Unexpected number of resolvers in Ivy settings", ivySettings.getResolvers().isEmpty());
        final List<Status> statuses = ivySettings.getStatusManager().getStatuses();
        assertEquals("Unexpected number of custom status in parsed Ivy settings", 1, statuses.size());
        assertEquals("Custom status not found in the parsed Ivy settings", "ivy-1555", statuses.get(0).getName());
    }
}
