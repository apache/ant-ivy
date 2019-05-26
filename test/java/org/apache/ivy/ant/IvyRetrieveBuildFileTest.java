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

import org.apache.ivy.core.report.ResolveReport;

import org.apache.tools.ant.BuildFileRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class IvyRetrieveBuildFileTest {
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("test/java/org/apache/ivy/ant/IvyRetrieveBuildFile.xml");
    }

    @Test
    public void testMultipleInlineRetrievesWithCacheCleaning() {
        buildRule.executeTarget("testMultipleInlineRetrievesWithCacheCleaning");
        ResolveReport report = buildRule.getProject().getReference("ivy.resolved.report");
        assertNotNull(report);
        assertFalse(report.hasError());
        assertEquals(1, report.getDependencies().size());
    }
}
