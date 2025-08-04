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
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1658">IVY-1658</a>.
     */
    @Test
    public void testDeliver1658() throws Exception {
        String ivyFile
            = "<ivy-module version='2.0'\n"
            + " xmlns:m='http://ant.apache.org/ivy/maven'\n"
            + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + " xsi:noNamespaceSchemaLocation='http://ant.apache.org/ivy/schemas/ivy.xsd'>\n"
            + "  <info module='xxx' organisation='zzz'/>\n"
            + "  <dependencies>\n"
            + "    <dependency name='groovy' org='org.codehaus.groovy' rev='3.0.25' transitive='false'>\n"
            + "      <artifact name='groovy' m:classifier='indy'/>\n"
            + "    </dependency>\n"
            + "  </dependencies>\n"
            + "</ivy-module>\n";

        ivyFile
            = "<ivy-module version='2.0'\n"
            + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + " xsi:noNamespaceSchemaLocation='http://ant.apache.org/ivy/schemas/ivy.xsd'>\n"
            + "  <info module='yyy' organisation='zzz' revision='1.0'>\n"
            + "    <extends module='xxx' organisation='zzz' extendType='dependencies'\n"
            + "     location='" + writeFile(ivyFile).getName() + "' revision='latest'/>\n"
            + "  </info>\n"
            + "  <dependencies>\n"
            + "  </dependencies>\n"
            + "</ivy-module>\n";

        resolve(writeFile(ivyFile));

        ivyDeliver.doExecute();

        ivyFile = readFile(deliverDir.getAbsolutePath() + "/ivys/ivy-1.0.xml");

        assertTrue(ivyFile.contains(" version=\"2.0\""));
        assertTrue(ivyFile.contains(" xmlns:m=\"http://ant.apache.org/ivy/maven\""));
        assertTrue(ivyFile.contains(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
        assertTrue(ivyFile.contains(" xsi:noNamespaceSchemaLocation=\"http://ant.apache.org/ivy/schemas/ivy.xsd\""));
    }
}
