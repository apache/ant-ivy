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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyInstallTest extends TestCase {
    private File _cache;

    private IvyInstall _install;

    private Project _project;

    protected void setUp() throws Exception {
        createCache();
        cleanTestLib();
        _project = new Project();
        _project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        _install = new IvyInstall();
        _install.setProject(_project);
        _install.setCache(_cache);
    }

    private void createCache() {
        _cache = new File("build/cache");
        _cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
        cleanTestLib();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }

    private void cleanTestLib() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/lib"));
        del.execute();
    }

    public void testDependencyNotFoundFailure() {
        _install.setOrganisation("xxx");
        _install.setModule("yyy");
        _install.setRevision("zzz");
        _install.setFrom("test");
        _install.setTo("1");

        try {
            _install.execute();
            fail("unknown dependency, failure expected (haltunresolved=true)");
        } catch (BuildException be) {
            // success
        }
    }

    public void testDependencyNotFoundSuccess() {
        _install.setOrganisation("xxx");
        _install.setModule("yyy");
        _install.setRevision("zzz");
        _install.setFrom("test");
        _install.setTo("1");
        _install.setHaltonfailure(false);

        try {
            _install.execute();
        } catch (BuildException be) {
            fail("unknown dependency, failure unexepected (haltunresolved=false)");
        }
    }
}
