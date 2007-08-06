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

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyRepositoryReportTest extends TestCase {
    private File cache;

    private IvyRepositoryReport report;

    protected void setUp() throws Exception {
        createCache();
        Project project = new Project();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-1.xml");

        report = new IvyRepositoryReport();
        report.setProject(project);
        report.setCache(cache);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    public void test() {
    }

    // no xslt transformation is possible in the junit test on our continuous integration server for
    // the moment...
    // public void testGraph() throws Exception {
    // _report.setOrganisation("org1");
    // _report.setXml(false);
    // _report.setGraph(true);
    // _report.setTodir(_cache);
    // _report.setOutputname("test-graph");
    // _report.execute();
    // File graphml = new File(_cache, "test-graph.graphml");
    // assertTrue(graphml.exists());
    // String g = FileUtil.readEntirely(new BufferedReader(new FileReader(graphml)));
    // assertFalse(g.indexOf("caller") != -1);
    // assertTrue(g.indexOf("mod1.1") != -1);
    // }
    //
    // public void testDot() throws Exception {
    // _report.setOrganisation("org1");
    // _report.setXml(false);
    // _report.setDot(true);
    // _report.setTodir(_cache);
    // _report.setOutputname("test-graph");
    // _report.execute();
    // File dot = new File(_cache, "test-graph.dot");
    // assertTrue(dot.exists());
    // String g = FileUtil.readEntirely(new BufferedReader(new FileReader(dot)));
    // assertFalse(g.indexOf("caller") != -1);
    // assertTrue(g.indexOf("mod1.1") != -1);
    // }
}
