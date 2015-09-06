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
package org.apache.ivy.ant.testutil;

import org.apache.ivy.TestHelper;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class AntTaskTestCase extends TestCase {

    private AntTestListener antTestListener;

    public Project configureProject() {
        Project project = TestHelper.newProject();
        antTestListener = new AntTestListener(Project.MSG_INFO);
        project.addBuildListener(antTestListener);
        return project;
    }

    public void assertLogContaining(String substring) {
        checkAntListener();
        String realLog = antTestListener.getLog();
        assertTrue("expecting log to contain \"" + substring + "\" log was \"" + realLog + "\"",
            realLog.contains(substring));
    }

    public void assertLogNotContaining(String substring) {
        checkAntListener();
        String realLog = antTestListener.getLog();
        assertFalse("expecting log to contain \"" + substring + "\" log was \"" + realLog + "\"",
            realLog.contains(substring));
    }

    private void checkAntListener() {
        if (antTestListener == null) {
            throw new IllegalStateException(
                    "Project is not properly configure, please invoke configureProject method first");
        }
    }

}
