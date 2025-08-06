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
package org.apache.ivy.core.deliver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.IvyDeliver;
import org.apache.ivy.ant.IvyResolve;
import org.apache.ivy.util.FileUtil;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DeliverTest {

    private File cacheDir;

    private File deliverDir;

    private IvyDeliver ivyDeliver;

    private void createCache() {
        cacheDir.mkdirs();
    }

    private void resolve(File ivyFile) {
        IvyResolve ivyResolve = new IvyResolve();
        ivyResolve.setProject(ivyDeliver.getProject());
        ivyResolve.setFile(ivyFile);
        ivyResolve.doExecute();
    }

    private String readFile(String fileName) throws IOException {
        StringBuilder retval = new StringBuilder();
        File file = new File(fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                retval.append(line).append("\n");
            }
        }
        return retval.toString();
    }

    private File writeFile(String fileText) throws IOException {
        File file = File.createTempFile("ivy", ".xml");
        file.deleteOnExit();
        FileUtils.write(file, fileText, "UTF-8");
        return file;
    }

    @Before
    public void setUp() {
        cacheDir = new File("build/cache");
        System.setProperty("ivy.cache.dir", cacheDir.getAbsolutePath());
        createCache();

        deliverDir = new File("build/test/deliver");
        deliverDir.mkdirs();

        Project project = TestHelper.newProject();
        project.init();

        ivyDeliver = new IvyDeliver();
        ivyDeliver.setProject(project);
        ivyDeliver.setDeliverpattern(deliverDir.getAbsolutePath() + "/[type]s/[artifact]-[revision](-[classifier]).[ext]");
    }

    @After
    public void tearDown() {
        FileUtil.forceDelete(cacheDir);
        FileUtil.forceDelete(deliverDir);
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1111">IVY-1111</a>.
     */
    @Test
    public void testDeliver1111() throws Exception {
        ivyDeliver.getProject().setProperty("ivy.settings.file", "test/repositories/IVY-1111/ivysettings.xml");

        String ivyFile
            = "<ivy-module version='1.0' xmlns:e='http://ant.apache.org/ivy/extra'>\n"
            + "  <info organisation='apache' module='IVY-1111' revision='1.0' e:att='att'/>\n"
            + "  <dependencies>\n"
            + "    <dependency org='test' name='a' rev='latest.integration'/>\n"
            + "    <dependency org='test' name='b' rev='latest.integration' e:att='att'/>\n"
            + "    <dependency org='junit' name='junit' rev='latest.integration'/>\n"
            + "  </dependencies>\n"
            + "</ivy-module>\n";

        resolve(writeFile(ivyFile));

        ivyDeliver.setReplacedynamicrev(true);
        ivyDeliver.doExecute();

        ivyFile = readFile(deliverDir.getAbsolutePath() + "/ivys/ivy-1.0.xml");
        assertTrue(ivyFile.contains("org=\"test\" name=\"a\" rev=\"1\" revConstraint=\"latest.integration\""));
        assertTrue(ivyFile.contains("org=\"test\" name=\"b\" rev=\"1.5\" revConstraint=\"latest.integration\" e:att=\"att\""));
        assertTrue(ivyFile.contains("org=\"junit\" name=\"junit\" rev=\"4.4\" revConstraint=\"latest.integration\""));
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1410">IVY-1410</a>.
     */
    @Test
    public void testDeliver1410() throws Exception {
        ivyDeliver.getProject().setProperty("ivy.settings.file", "test/repositories/ivysettings-1.xml");

        String ivyFile
            = "<ivy-module version='2.0'>\n"
            + "  <info organisation='org' module='xxx'/>\n"
            + "  <dependencies>\n"
            + "    <dependency org='org1' name='mod1.1' rev='1.+'/>\n"
            + "  </dependencies>\n"
            + "</ivy-module>\n";

        ivyFile
            = "<ivy-module version='2.0'>\n"
            + "  <info organisation='org' module='yyy' revision='1.0'>\n"
            + "    <extends organisation='org' module='xxx' extendType='dependencies'\n"
            + "     location='" + writeFile(ivyFile).getName() + "' revision='latest'/>\n"
            + "  </info>\n"
            + "  <dependencies>\n"
            + "    <dependency org='org2' name='mod2.1' rev='0.+'/>\n"
            + "  </dependencies>\n"
            + "</ivy-module>\n";

        resolve(writeFile(ivyFile));

        ivyDeliver.doExecute();

        ivyFile = readFile(deliverDir.getAbsolutePath() + "/ivys/ivy-1.0.xml");
        assertTrue(ivyFile.contains("org=\"org1\" name=\"mod1.1\" rev=\"1.1\""));
        assertTrue(ivyFile.contains("org=\"org2\" name=\"mod2.1\" rev=\"0.7\""));
    }
}
