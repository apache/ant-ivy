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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.apache.ivy.TestHelper;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.repo.BundleRepoDescriptor;
import org.apache.ivy.util.CollectionUtils;
import org.apache.tools.ant.Project;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class BuildOBRTaskTest extends TestCase {

    private File cache;

    private BuildOBRTask buildObr;

    private Project project;

    protected void setUp() throws Exception {
        createCache();
        project = TestHelper.newProject();

        buildObr = new BuildOBRTask();
        buildObr.setProject(project);
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    private BundleRepoDescriptor readObr(File obrFile) throws FileNotFoundException,
            ParseException, IOException, SAXException {
        BundleRepoDescriptor obr;
        FileInputStream in = new FileInputStream(obrFile);
        try {
            obr = OBRXMLParser.parse(obrFile.toURI(), in);
        } finally {
            in.close();
        }
        return obr;
    }

    public void testDir() throws Exception {
        buildObr.setBaseDir(new File("test/test-repo/bundlerepo"));
        File obrFile = new File("build/cache/obr.xml");
        buildObr.setOut(obrFile);
        buildObr.execute();

        BundleRepoDescriptor obr = readObr(obrFile);

        assertEquals(14, CollectionUtils.toList(obr.getModules()).size());
    }

    public void testEmptyDir() throws Exception {
        buildObr.setBaseDir(new File("test/test-p2/composite"));
        File obrFile = new File("build/cache/obr.xml");
        buildObr.setOut(obrFile);
        buildObr.execute();

        BundleRepoDescriptor obr = readObr(obrFile);

        assertEquals(0, CollectionUtils.toList(obr.getModules()).size());
    }

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
