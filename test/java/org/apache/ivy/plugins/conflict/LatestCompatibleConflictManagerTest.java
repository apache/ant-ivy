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
package org.apache.ivy.plugins.conflict;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestFixture;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LatestCompatibleConflictManagerTest {
    private TestFixture fixture;

    @Before
    public void setUp() {
        fixture = new TestFixture();
        LatestCompatibleConflictManager cm = new LatestCompatibleConflictManager();
        fixture.getSettings().addConfigured(cm);
        fixture.getSettings().setDefaultConflictManager(cm);
    }

    @After
    public void tearDown() {
        fixture.clean();
    }

    @Test
    public void testInitFromSettings() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(LatestCompatibleConflictManagerTest.class
                .getResource("ivysettings-latest-compatible.xml"));
        ConflictManager cm = ivy.getSettings().getDefaultConflictManager();
        assertTrue(cm instanceof LatestCompatibleConflictManager);
    }

    @Test
    public void testCompatibilityResolve1() throws Exception {
        fixture.addMD("#A;1-> { #B;1.4 #C;[2.0,2.5] }").addMD("#B;1.4->#D;1.5")
                .addMD("#C;2.5->#D;[1.0,1.6]").addMD("#D;1.5").addMD("#D;1.6").init();
        resolveAndAssert("#A;1", "#B;1.4, #C;2.5, #D;1.5");
    }

    @Test
    public void testCompatibilityResolve2() throws Exception {
        fixture.addMD("#A;2-> { #B;[1.0,1.5] #C;[2.0,2.5] }").addMD("#B;1.4->#D;1.5")
                .addMD("#B;1.5->#D;2.0").addMD("#C;2.5->#D;[1.0,1.6]").addMD("#D;1.5")
                .addMD("#D;1.6").addMD("#D;2.0").init();
        resolveAndAssert("#A;2", "#B;1.4, #C;2.5, #D;1.5");
    }

    @Test
    public void testCompatibilityResolve3() throws Exception {
        fixture.addMD("#A;3-> { #B;[2.0,2.5] #C;[3.0,3.5] }").addMD("#B;2.3-> { #D;1.5 #E;1.0 }")
                .addMD("#B;2.4-> { #D;1.5 #E;2.0 }").addMD("#B;2.5-> { #D;2.0 }")
                .addMD("#C;3.4-> { #D;[1.0,1.6] #E;1.0 }")
                .addMD("#C;3.5-> { #D;[1.0,1.6] #E;1.9 }").addMD("#D;1.5").addMD("#D;1.6")
                .addMD("#D;2.0").addMD("#E;1.0").addMD("#E;1.9").addMD("#E;2.0").init();
        resolveAndAssert("#A;3", "#B;2.3, #C;3.4, #D;1.5, #E;1.0");
    }

    @Test
    public void testCompatibilityResolve4() throws Exception {
        fixture.addMD("#A;4-> { #B;[1.0,1.5] #C;[2.0,2.5] #F;[1.0,1.1] }").addMD("#B;1.4->#D;1.5")
                .addMD("#B;1.5->#D;2.0").addMD("#C;2.5->#D;[1.0,1.6]").addMD("#F;1.0->#D;1.5")
                .addMD("#F;1.1->#D;1.6").addMD("#D;1.5").addMD("#D;1.6").addMD("#D;2.0").init();
        resolveAndAssert("#A;4", "#B;1.4, #C;2.5, #D;1.5, #F;1.0");
    }

    @Test
    public void testCompatibilityResolve5() throws Exception {
        fixture.addMD("#A;5->{ #B;[1.0,1.5] #C;2.6 }").addMD("#B;1.3->{ }").addMD("#B;1.4->#D;1.5")
                .addMD("#B;1.5->#D;2.0").addMD("#C;2.6->#D;1.6").addMD("#D;1.5").addMD("#D;1.6")
                .addMD("#D;2.0").init();
        resolveAndAssert("#A;5", "#B;1.3, #C;2.6, #D;1.6");
    }

    @Test
    public void testCompatibilityResolve6() throws Exception {
        fixture.addMD("#A;1-> { #C;[2.0,2.5] #B;1.4 }").addMD("#B;1.4->#D;1.5")
                .addMD("#C;2.5->#D;[1.0,1.6]").addMD("#D;1.5").addMD("#D;1.6").init();
        resolveAndAssert("#A;1", "#B;1.4, #C;2.5, #D;1.5");
    }

    @Test
    public void testCompatibilityResolveCircularDependency1() throws Exception {
        fixture.addMD("#A;6->{ #B;[3.0,3.5] #C;4.6 }").addMD("#B;3.4->#D;2.5")
                .addMD("#B;3.5->#D;3.0").addMD("#C;4.6->#D;2.5").addMD("#D;3.0->#B;3.5") // circular
                                                                                         // dependency
                .addMD("#D;2.5->#B;3.4") // circular dependency
                .init();
        resolveAndAssert("#A;6", "#B;3.4, #C;4.6, #D;2.5");
    }

    @Test
    public void testCompatibilityResolveCircularDependency2() throws Exception {
        fixture.addMD("#A;1->#C;2").addMD("#C;1->#B;1").addMD("#C;2->#B;2").addMD("#C;3->#B;3")
                .addMD("#B;1->#C;latest.integration") // circular dependency
                .addMD("#B;2->#C;latest.integration") // circular dependency
                .addMD("#B;3->#C;latest.integration") // circular dependency
                .init();
        resolveAndAssert("#A;1", "#B;2, #C;2");
    }

    @Test
    public void testCompatibilityResolveCircularDependency3() throws Exception {
        // same as 2, but A depends on B
        fixture.addMD("#A;1->#B;2").addMD("#C;1->#B;1").addMD("#C;2->#B;2").addMD("#C;3->#B;3")
                .addMD("#B;1->#C;latest.integration") // circular dependency
                .addMD("#B;2->#C;latest.integration") // circular dependency
                .addMD("#B;3->#C;latest.integration") // circular dependency
                .init();
        resolveAndAssert("#A;1", "#B;2, #C;2");
    }

    /**
     * Resolve must fail with a conflict.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = StrictConflictException.class)
    public void testConflict() throws Exception {
        fixture.addMD("#A;conflict-> { #B;[1.5,1.6] #C;2.5 }").addMD("#B;1.5->#D;2.0")
                .addMD("#B;1.6->#D;2.0").addMD("#C;2.5->#D;[1.0,1.6]").addMD("#D;1.5")
                .addMD("#D;1.6").addMD("#D;2.0").init();
        fixture.resolve("#A;conflict");
    }

    /**
     * Resolve must fail with a conflict.
     *
     * @throws Exception if something goes wrong
     */
    @Test(expected = StrictConflictException.class)
    public void testDynamicRootConflict() throws Exception {
        fixture.addMD("#A;conflict-> {#B;[1.2,2.0[ #C;pCC.main.+ #D;[1.5,1.7[ }")
                .addMD("#B;1.0.0->#D;[1.6.1,2.0[").addMD("#B;1.1.0->#D;[1.6.1,2.0[")
                .addMD("#B;pCC.main.0.0->#D;[1.6.1,2.0[")
                .addMD("#C;1.0.0-> {#B;[1.0,2.0[ #D;[1.6.0,1.7[ }")
                .addMD("#C;1.1.0-> {#B;[1.1,2.0[ #D;[1.6.0,1.7[ }")
                .addMD("#C;pCC.main.1.9-> {#B;pCC.main.+ #D;[1.6.0,1.7[ }").addMD("#D;1.6.1")
                .init();
        fixture.resolve("#A;conflict");
    }

    private void resolveAndAssert(String mrid, String expectedModuleSet) throws ParseException,
            IOException {
        ResolveReport report = fixture.resolve(mrid);
        assertFalse(report.hasError());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        TestHelper.assertModuleRevisionIds(expectedModuleSet, defaultReport.getModuleRevisionIds());
    }
}
