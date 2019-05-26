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

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.IvyDeliver;
import org.apache.ivy.ant.IvyResolve;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeliverTest {
    private File cache;

    private File deliverDir;

    private IvyDeliver ivyDeliver;

    @Before
    public void setUp() {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        createCache();

        deliverDir = new File("build/test/deliver");
        deliverDir.mkdirs();

        Project project = TestHelper.newProject();
        project.init();

        ivyDeliver = new IvyDeliver();
        ivyDeliver.setProject(project);
        ivyDeliver.setDeliverpattern(deliverDir.getAbsolutePath()
                + "/[type]s/[artifact]-[revision](-[classifier]).[ext]");
    }

    @After
    public void tearDown() {
        FileUtil.forceDelete(cache);
        FileUtil.forceDelete(deliverDir);
    }

    private void createCache() {
        cache.mkdirs();
    }

    /**
     * Test case for IVY-1111.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1111">IVY-1111</a>
     */
    @Test
    public void testIVY1111() throws Exception {
        Project project = ivyDeliver.getProject();
        project.setProperty("ivy.settings.file", "test/repositories/IVY-1111/ivysettings.xml");
        File ivyFile = new File(new URI(DeliverTest.class.getResource("ivy-1111.xml").toString()));

        resolve(ivyFile);

        ivyDeliver.setReplacedynamicrev(true);
        ivyDeliver.doExecute();

        String deliverContent = readFile(deliverDir.getAbsolutePath() + "/ivys/ivy-1.0.xml");
        assertFalse(deliverContent.contains("rev=\"latest.integration\""));
        assertTrue(deliverContent.contains("name=\"b\" rev=\"1.5\""));
    }

    private void resolve(File ivyFile) {
        IvyResolve ivyResolve = new IvyResolve();
        ivyResolve.setProject(ivyDeliver.getProject());
        ivyResolve.setFile(ivyFile);
        ivyResolve.doExecute();
    }

    private String readFile(String fileName) throws IOException {
        StringBuilder retval = new StringBuilder();

        File ivyFile = new File(fileName);
        BufferedReader reader = new BufferedReader(new FileReader(ivyFile));

        String line = null;
        while ((line = reader.readLine()) != null) {
            retval.append(line).append("\n");
        }

        reader.close();
        return retval.toString();
    }
}
