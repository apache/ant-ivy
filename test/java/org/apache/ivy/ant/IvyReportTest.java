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
import java.util.Locale;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyReportTest extends TestCase {

    private IvyReport report;

    private Project project;

    protected void setUp() throws Exception {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        report = new IvyReport();
        report.setTaskName("report");
        report.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSimple() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            res.execute();

            report.setTodir(new File(TestHelper.cache, "report"));
            report.execute();

            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-default.html").exists());
            assertTrue(new File(TestHelper.cache, "report/ivy-report.css").exists()); // IVY-826
            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-default.graphml").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testWithLatest() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/repositories/1/org6/mod6.2/ivys/ivy-0.7.xml"));
            res.execute();

            report.setTodir(new File(TestHelper.cache, "report"));
            report.setXml(true);
            report.execute();

            File xmlReport = new File(TestHelper.cache, "report/org6-mod6.2-default.xml");
            assertTrue(xmlReport.exists());
            // check that revision 2.2 of mod1.2 is only present once
            String reportContent = FileUtil.readEntirely(xmlReport);
            int index = reportContent.indexOf("<revision name=\"2.2\"");
            assertTrue(index != -1);
            index = reportContent.indexOf("<revision name=\"2.2\"", index + 1);
            assertTrue(index == -1);
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testCopyCssIfTodirNotSet() {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            res.execute();

            report.setGraph(false);
            report.execute();

            assertTrue(new File("apache-resolve-simple-default.html").exists());
            assertTrue(new File("ivy-report.css").exists()); // IVY-826
        } finally {
            Locale.setDefault(oldLocale);
            new File("apache-resolve-simple-default.html").delete();
            new File("ivy-report.css").delete();
        }
    }

    public void testNoRevisionInOutputPattern() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            res.execute();

            report.setTodir(new File(TestHelper.cache, "report"));
            report.setOutputpattern("[organisation]-[module]-[revision].[ext]");
            report.setConf("default");
            report.execute();

            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-1.0.html").exists());
            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-1.0.graphml").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testMultipleConfigurations() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
            res.execute();

            report.setTodir(new File(TestHelper.cache, "report"));
            report.execute();

            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-default.html").exists());
            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-default.graphml").exists());
            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-compile.html").exists());
            assertTrue(new File(TestHelper.cache, "report/apache-resolve-simple-compile.graphml").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testRegularCircular() throws Exception {
        Locale oldLocale = Locale.getDefault();

        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            project.setProperty("ivy.dep.file", "test/repositories/2/mod11.1/ivy-1.0.xml");
            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.execute();

            report.setTodir(new File(TestHelper.cache, "report"));
            report.setXml(true);

            report.execute();

            assertTrue(new File(TestHelper.cache, "report/org11-mod11.1-compile.xml").exists());
            assertTrue(new File(TestHelper.cache, "report/org11-mod11.1-compile.html").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }
}
