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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyDeliverTest extends TestCase {
    private File cache;

    private IvyDeliver deliver;

    private Project project;

    protected void setUp() throws Exception {
        cleanTestDir();
        cleanRep();
        createCache();
        project = new Project();
        project.init();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        project.setProperty("build", "build/test/deliver");

        deliver = new IvyDeliver();
        deliver.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
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
        del.setDir(new File("build/test/deliver"));
        del.execute();
    }

    private void cleanRep() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("test/repositories/1/apache"));
        del.execute();
    }

    public void testSimple() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2"), 
            dds[0].getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "latest.integration"), 
            dds[0].getDynamicConstraintDependencyRevisionId());
    }

    public void testWithResolveId() throws Exception {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("withResolveId");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        deliver.setResolveId("withResolveId");
        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-simple", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), dds[0]
                .getDependencyRevisionId());
    }

    public void testWithResolveIdInAnotherBuild() throws Exception {
        // create a new build
        Project other = new Project();
        other.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");
        other.setProperty("build", "build/test/deliver");

        // do a resolve in the new build
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(other);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("withResolveId");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        deliver.setResolveId("withResolveId");
        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-simple", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), dds[0]
                .getDependencyRevisionId());
    }

    public void testWithBranch() throws Exception {
        // test case for IVY-404
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest-branch.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "TRUNK", "2.2"), dds[0]
                .getDependencyRevisionId());
    }

    public void testReplaceBranch() throws Exception {
        IvyAntSettings settings = new IvyAntSettings();
        settings.setProject(project);
        settings.execute();
        // change the default branch to use
        settings.getConfiguredIvyInstance().getSettings().setDefaultBranch("BRANCH1");
        
        // resolve a module dependencies
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        // deliver this module
        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering, including setting the branch according to the 
        // configured default
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "BRANCH1", "2.2"), dds[0]
                .getDependencyRevisionId());
    }

    public void testWithExtraAttributes() throws Exception {
        // test case for IVY-415
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest-extra.xml");
        IvyResolve res = new IvyResolve();
        res.setValidate(false);
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.setValidate(false);
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), false);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        Map extraAtt = new HashMap();
        extraAtt.put("myExtraAtt", "myValue");
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.2", extraAtt), dds[0]
                .getDependencyRevisionId());
    }

    public void testWithDynEvicted() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-dyn-evicted.xml");
        IvyResolve res = new IvyResolve();
        res.setValidate(false);
        res.setProject(project);
        res.execute();
        
        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.setValidate(false);
        deliver.execute();
        
        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists()); 
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
	    new IvySettings(), deliveredIvyFile.toURL(), false);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), 
            md.getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"), 
            dds[0].getDependencyRevisionId());
    }

    public void testReplaceImportedConfigurations() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-import-confs.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        String deliveredFileContent = FileUtil.readEntirely(new BufferedReader(new FileReader(
                deliveredIvyFile)));
        assertTrue("import not replaced: import can still be found in file", deliveredFileContent
                .indexOf("import") == -1);
        assertTrue("import not replaced: conf1 cannot be found in file", deliveredFileContent
                .indexOf("conf1") != -1);
    }

    public void testReplaceVariables() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-with-variables.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        res.getIvyInstance().getSettings().setVariable("myvar", "myvalue");

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        String deliveredFileContent = FileUtil.readEntirely(new BufferedReader(new FileReader(
                deliveredIvyFile)));
        assertTrue("variable not replaced: myvar can still be found in file", deliveredFileContent
                .indexOf("myvar") == -1);
        assertTrue("variable not replaced: myvalue cannot be found in file", deliveredFileContent
                .indexOf("myvalue") != -1);
    }

    public void testNoReplaceDynamicRev() throws Exception {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-latest.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.setReplacedynamicrev(false);
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "resolve-latest", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "latest.integration"), dds[0]
                .getDependencyRevisionId());
    }

    public void testDifferentRevisionsForSameModule() throws Exception {
        project.setProperty("ivy.dep.file",
            "test/java/org/apache/ivy/ant/ivy-different-revisions.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        deliver.setPubrevision("1.2");
        deliver.setDeliverpattern("build/test/deliver/ivy-[revision].xml");
        deliver.execute();

        // should have done the ivy delivering
        File deliveredIvyFile = new File("build/test/deliver/ivy-1.2.xml");
        assertTrue(deliveredIvyFile.exists());
        ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), deliveredIvyFile.toURL(), true);
        assertEquals(ModuleRevisionId.newInstance("apache", "different-revs", "1.2"), md
                .getModuleRevisionId());
        DependencyDescriptor[] dds = md.getDependencies();
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "2.0"), dds[0]
                .getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.1", "1.0"), dds[1]
                .getDependencyRevisionId());
        assertEquals(ModuleRevisionId.newInstance("org1", "mod1.2", "1.1"), dds[2]
                .getDependencyRevisionId());
    }
}
