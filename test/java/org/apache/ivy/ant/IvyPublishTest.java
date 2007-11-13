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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Echo;

public class IvyPublishTest extends TestCase {
    private File cache;

    private IvyPublish publish;

    private Project project;

    protected void setUp() throws Exception {
        cleanTestDir();
        cleanRep();
        createCache();
        project = new Project();
        project.init();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("build", "build/test/publish");

        publish = new IvyPublish();
        publish.setProject(project);
        publish.setCache(cache);

        Message.setDefaultLogger(new DefaultMessageLogger(10));
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
        cleanTestDir();
        cleanRep();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    private void cleanTestDir() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/publish"));
        del.execute();
    }

    private void cleanRep() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("test/repositories/1/apache"));
        del.execute();
    }

    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-multiconf.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.execute();

        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/ivy-1.2.xml").exists());

        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/apache/resolve-simple/ivys/ivy-1.2.xml").exists());
        assertTrue(new File("test/repositories/1/apache/resolve-simple/jars/resolve-simple-1.2.jar")
                .exists());

        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            new File("test/repositories/1/apache/resolve-simple/ivys/ivy-1.2.xml").toURL(), false);
        assertEquals("1.2", md.getModuleRevisionId().getRevision());
    }

    public void testPublishNotAllConfigs() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-multiconf.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        publish.setConf("compile");
        publish.setUpdate(true);
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.execute();

        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/ivy-1.2.xml").exists());

        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/apache/resolve-simple/ivys/ivy-1.2.xml").exists());
        assertTrue(new File("test/repositories/1/apache/resolve-simple/jars/resolve-simple-1.2.jar")
                .exists());

        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            new File("test/repositories/1/apache/resolve-simple/ivys/ivy-1.2.xml").toURL(), false);
        assertEquals("1.2", md.getModuleRevisionId().getRevision());

        // should only contain the default configuration
        String[] configs = md.getConfigurationsNames();
        assertEquals("Number of configurations not correct", 1, configs.length);
        assertEquals("Compile configuration not present", "compile", configs[0]);
    }

    public void testMultiPatterns() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-publish-multi.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        File art = new File("build/test/publish/1/multi1-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        art = new File("build/test/publish/2/multi2-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.addArtifactspattern("build/test/publish/1/[artifact]-[revision].[ext]");
        publish.addArtifactspattern("build/test/publish/2/[artifact]-[revision].[ext]");
        publish.execute();

        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/1/ivy-1.2.xml").exists());

        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/apache/multi/ivys/ivy-1.2.xml").exists());
        assertTrue(new File("test/repositories/1/apache/multi/jars/multi1-1.2.jar").exists());
        assertTrue(new File("test/repositories/1/apache/multi/jars/multi2-1.2.jar").exists());
    }

    public void testCustom() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-custom.xml");
        IvyResolve res = new IvyResolve();
        res.setValidate(false);
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setPubdate("20060906141243");
        publish.setResolver("1");
        publish.setValidate(false);
        File art = new File("build/test/publish/resolve-custom-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.execute();

        // should have do the ivy delivering
        assertTrue(new File("build/test/publish/ivy-1.2.xml").exists());

        File dest = new File("test/repositories/1/apache/resolve-custom/ivys/ivy-1.2.xml");
        // should have published the files with "1" resolver
        assertTrue(dest.exists());
        assertTrue(new File("test/repositories/1/apache/resolve-custom/jars/resolve-custom-1.2.jar")
                .exists());

        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), dest.toURL(), false);
        assertEquals("1.2", md.getModuleRevisionId().getRevision());

        // should have kept custom attributes
        assertEquals("cval1", md.getModuleRevisionId().getAttribute("custom-info"));
        assertEquals("cval2", md.getConfiguration("default").getAttribute("custom-conf"));
        assertEquals("cval3", md.getDependencies()[0].getAttribute("custom-dep"));

        // should respect the ivy file, with descriptions, ...
        String expected = FileUtil.readEntirely(new BufferedReader(new InputStreamReader(
                IvyPublishTest.class.getResourceAsStream("published-ivy-custom.xml"))));
        String updated = FileUtil.readEntirely(new BufferedReader(new FileReader(dest)));
        assertEquals(expected, updated);

    }

    public void testNoDeliver() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.3");
        publish.setResolver("1");
        publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");

        FileUtil.copy(new File("test/java/org/apache/ivy/ant/ivy-publish.xml"), new File(
                "build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.execute();

        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/apache/resolve-latest/ivys/ivy-1.3.xml").exists());
        assertTrue(new File("test/repositories/1/apache/resolve-latest/jars/resolve-latest-1.3.jar")
                .exists());

        // the published ivy version should be ok (ok in ivy-publish file)
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            new File("test/repositories/1/apache/resolve-latest/ivys/ivy-1.3.xml").toURL(), false);
        assertEquals("1.3", md.getModuleRevisionId().getRevision());

        // should not have done delivery (replace dynamic revisions with static ones)
        assertEquals("latest.integration", md.getDependencies()[0].getDependencyRevisionId()
                .getRevision());
    }

    public void testForceDeliver() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.3");
        publish.setResolver("1");
        publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");
        publish.setForcedeliver(true);

        FileUtil.copy(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"), new File(
                "build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        publish.execute();

        // should have published the files with "1" resolver
        assertTrue(new File("test/repositories/1/apache/resolve-latest/ivys/ivy-1.3.xml").exists());
        assertTrue(new File("test/repositories/1/apache/resolve-latest/jars/resolve-latest-1.3.jar")
                .exists());

        // should have updated published ivy version
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(),
            new File("test/repositories/1/apache/resolve-latest/ivys/ivy-1.3.xml").toURL(), false);
        assertEquals("1.3", md.getModuleRevisionId().getRevision());
    }

    public void testBadNoDeliver() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.3");
        publish.setResolver("1");
        publish.setSrcivypattern("build/test/publish/ivy-1.3.xml");

        FileUtil.copy(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"), new File(
                "build/test/publish/ivy-1.3.xml"), null);

        File art = new File("build/test/publish/resolve-latest-1.3.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);
        try {
            publish.execute();
            fail("shouldn't publish ivy file with bad revision");
        } catch (BuildException ex) {
            //normal
        }
    }

    public void testReadonly() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);

        Echo echo = new Echo();
        echo.setProject(project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File(
                "test/repositories/1/apache/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), dest, null);

        echo = new Echo();
        echo.setProject(project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();

        dest.setReadOnly();

        try {
            publish.execute();
            fail("by default, publish should fail when a readonly artifact already exist");
        } catch (Exception ex) {
            assertTrue(dest.exists());
            BufferedReader reader = new BufferedReader(new FileReader(dest));
            assertEquals("old version", reader.readLine());
            reader.close();
        }
    }

    public void testOverwrite() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);

        Echo echo = new Echo();
        echo.setProject(project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File(
                "test/repositories/1/apache/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), dest, null);

        echo = new Echo();
        echo.setProject(project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();

        publish.setOverwrite(true);
        publish.execute();
        assertTrue(dest.exists());
        BufferedReader reader = new BufferedReader(new FileReader(dest));
        assertEquals("new version", reader.readLine());
        reader.close();
    }

    public void testOverwriteReadOnly() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        publish.setPubrevision("1.2");
        publish.setResolver("1");
        File art = new File("build/test/publish/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), art, null);

        Echo echo = new Echo();
        echo.setProject(project);
        echo.setMessage("new version");
        echo.setFile(art);
        echo.execute();

        File dest = new File(
                "test/repositories/1/apache/resolve-simple/jars/resolve-simple-1.2.jar");
        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), dest, null);

        echo = new Echo();
        echo.setProject(project);
        echo.setMessage("old version");
        echo.setFile(dest);
        echo.execute();

        dest.setReadOnly();

        publish.setOverwrite(true);
        publish.execute();
        assertTrue(dest.exists());
        BufferedReader reader = new BufferedReader(new FileReader(dest));
        assertEquals("new version", reader.readLine());
        reader.close();
    }

}
