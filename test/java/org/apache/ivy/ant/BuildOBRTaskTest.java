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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.ivy.TestHelper;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.repo.BundleRepoDescriptor;
import org.apache.ivy.util.CollectionUtils;

import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.xml.sax.SAXException;

public class BuildOBRTaskTest {

    private File cache;

    private BuildOBRTask buildObr;

    @Before
    public void setUp() {
        createCache();

        buildObr = new BuildOBRTask();
        buildObr.setProject(TestHelper.newProject());
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    private BundleRepoDescriptor readObr(File obrFile) throws IOException, SAXException {
        BundleRepoDescriptor obr;
        try (FileInputStream in = new FileInputStream(obrFile)) {
            obr = OBRXMLParser.parse(obrFile.toURI(), in);
        }
        return obr;
    }

    @Test
    public void testDir() throws Exception {
        buildObr.setBaseDir(new File("test/test-repo/bundlerepo"));
        File obrFile = new File("build/cache/obr.xml");
        buildObr.setOut(obrFile);
        buildObr.execute();

        BundleRepoDescriptor obr = readObr(obrFile);

        assertEquals(14, CollectionUtils.toList(obr.getModules()).size());
    }

    @Test
    public void testEmptyDir() throws Exception {
        buildObr.setBaseDir(new File("test/test-p2/composite"));
        File obrFile = new File("build/cache/obr.xml");
        buildObr.setOut(obrFile);
        buildObr.execute();

        BundleRepoDescriptor obr = readObr(obrFile);

        assertEquals(0, CollectionUtils.toList(obr.getModules()).size());
    }

    @Test
    public void testResolve() throws Exception {
        Project otherProject = TestHelper.newProject();
        otherProject.setProperty("ivy.settings.file", "test/test-repo/bundlerepo/ivysettings.xml");

        IvyResolve resolve = new IvyResolve();
        resolve.setProject(otherProject);
        resolve.setFile(new File("test/test-repo/ivy-test-buildobr.xml"));
        resolve.setResolveId("withResolveId");
        resolve.execute();

        File obrFile = new File("build/cache/obr.xml");

        buildObr.setProject(otherProject);
        buildObr.setResolveId("withResolveId");
        buildObr.setOut(obrFile);
        buildObr.execute();

        BundleRepoDescriptor obr = readObr(obrFile);

        assertEquals(1, CollectionUtils.toList(obr.getModules()).size());
    }

}
