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

public class NearestConflictManagerTest {
  private TestFixture fixture;

  @Before
  public void setUp() {
    fixture = new TestFixture();
    ConflictManager cm = fixture.getSettings().getConflictManager("nearest");
    fixture.getSettings().setDefaultConflictManager(cm);
  }

  @After
  public void tearDown() {
    fixture.clean();
  }

  @Test
  public void testInitFromSettings() throws Exception {
    Ivy ivy = new Ivy();
    ivy.configure(NearestConflictManagerTest.class.getResource("ivysettings-nearest.xml"));
    ConflictManager cm = ivy.getSettings().getDefaultConflictManager();
    assertTrue(cm instanceof NearestConflictManager);
  }

  @Test
  public void testNearestResolve1() throws Exception {
    fixture.addMD("#A;1-> { #B;1.4 #C;1.0 }").addMD("#B;1.4->#D;1.5")
           .addMD("#C;1.0->#D;1.6").addMD("#D;1.5").addMD("#D;1.6").init();
    resolveAndAssert("#A;1", "#B;1.4, #C;1.0, #D;1.5");
  }

  @Test
  public void testNearestResolve2() throws Exception {
    fixture.addMD("#A;1-> { #B;1.4 #C;1.0 }").addMD("#B;1.4->#D;1.6")
           .addMD("#C;1.0->#D;1.5").addMD("#D;1.5").addMD("#D;1.6").init();
    resolveAndAssert("#A;1", "#B;1.4, #C;1.0, #D;1.6");
  }

  @Test
  public void testNearestResolve3() throws Exception {
    fixture.addMD("#A;1-> { #B;1.4 #C;1.0 }").addMD("#B;1.4->#E;1.0").addMD("#E;1.0->#D;1.5")
           .addMD("#C;1.0->#D;1.6").addMD("#D;1.5").addMD("#D;1.6").init();
    resolveAndAssert("#A;1", "#B;1.4, #C;1.0, #D;1.6, #E;1.0");
  }

  @Test
  public void testNearestResolve4() throws Exception {
    fixture.addMD("#A;1-> { #B;1.4 #C;1.0 #D;1.5 }").addMD("#B;1.4->#E;1.0").addMD("#E;1.0->#D;1.5")
           .addMD("#C;1.0->#D;1.6").addMD("#D;1.5").addMD("#D;1.6").init();
    resolveAndAssert("#A;1", "#B;1.4, #C;1.0, #D;1.5, #E;1.0");
  }

  private void resolveAndAssert(String mrid, String expectedModuleSet) throws ParseException,
                                                                              IOException {
    ResolveReport report = fixture.resolve(mrid);
    assertFalse(report.hasError());
    ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
    TestHelper.assertModuleRevisionIds(expectedModuleSet, defaultReport.getModuleRevisionIds());
  }

}


