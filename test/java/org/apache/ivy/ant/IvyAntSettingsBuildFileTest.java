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

import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.BuildFileTest;

public class IvyAntSettingsBuildFileTest extends BuildFileTest {

    protected void setUp() throws Exception {
        configureProject("test/java/org/apache/ivy/ant/IvyAntSettingsBuildFile.xml");
    }

    public void testOverrideNotSpecified() {
        executeTarget("testOverrideNotSpecified");
        ResolveReport report = (ResolveReport) getProject().getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getDependencies().size());
    }

    public void testOverrideSetToFalse() {
        executeTarget("testOverrideSetToFalse");
        ResolveReport report = (ResolveReport) getProject().getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getDependencies().size());
    }

    public void testUnnecessaryDefaultIvyInstance() {
        executeTarget("testUnnecessaryDefaultIvyInstance");
        assertNull("Default ivy.instance settings shouldn't have been loaded", getProject()
                .getReference("ivy.instance"));
    }

    public void testSettingsWithIdIvyInstance() {
        // IVY-925
        executeTarget("testSettingsWithPropertyAsId");
        ResolveReport report = (ResolveReport) getProject().getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getDependencies().size());
    }

    public void testStackOverflow() {
        // IVY-924
        configureProject("test/java/org/apache/ivy/ant/IvyAntSettingsBuildFileStackOverflow.xml");
        executeTarget("testStackOverflow");
        ResolveReport report = (ResolveReport) getProject().getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getDependencies().size());
    }
}
