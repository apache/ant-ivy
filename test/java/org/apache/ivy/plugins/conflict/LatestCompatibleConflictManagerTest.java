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
package org.apache.ivy.plugins.conflict;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import junit.framework.TestCase;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

public class LatestCompatibleConflictManagerTest extends TestCase {
    private Ivy ivy;

    private File cache;
    
    protected void setUp() throws Exception {
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
        ivy = new Ivy();
        ivy.configure(LatestCompatibleConflictManagerTest.class.getResource("ivysettings-latest-compatible.xml"));
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(cache);
    }

    public void testInitFromSettings() throws Exception {
        ConflictManager cm = ivy.getSettings().getDefaultConflictManager();
        assertTrue(cm instanceof LatestCompatibleConflictManager);
    }

    public void testCompatibilityResolve1() throws Exception {
        /* Test data:
            #A;1-> { #B;1.4 #C;[2.0,2.5] }
            #B;1.4->#D;1.5
            #C;2.5->#D;[1.0,1.6]
         */
        resolveAndAssert("ivy-latest-compatible-1.xml", "#B;1.4, #C;2.5, #D;1.5");
    }

    public void testCompatibilityResolve2() throws Exception {
        /* Test data:
            #A;2-> { #B;[1.0,1.5] #C;[2.0,2.5] }
            #B;1.4->#D;1.5
            #B;1.5->#D;2.0
            #C;2.5->#D;[1.0,1.6]
         */
        resolveAndAssert("ivy-latest-compatible-2.xml", "#B;1.4, #C;2.5, #D;1.5");
    }

    public void testCompatibilityResolve3() throws Exception {
        /* Test data:
            #A;3-> { #B;[2.0,2.5] #C;[3.0,3.5] }
            #B;2.3-> { #D;1.5 #E;1.0 }
            #B;2.4-> { #D;1.5 #E;2.0 }
            #B;2.5-> { #D;2.0 }
            #C;3.4-> { #D;[1.0,1.6] #E;1.0 } 
            #C;3.5-> { #D;[1.0,1.6] #E;1.9 } 
         */
        resolveAndAssert("ivy-latest-compatible-3.xml", "#B;2.3, #C;3.4, #D;1.5, #E;1.0");
    }

    public void testCompatibilityResolve4() throws Exception {
        /* Test data:
            #A;4-> { #B;[1.0,1.5] #C;[2.0,2.5] #F;[1.0,1.1] }
            #B;1.4->#D;1.5
            #B;1.5->#D;2.0
            #C;2.5->#D;[1.0,1.6]
            #F;1.0->#D;1.5
            #F;1.1->#D;1.6
         */
        resolveAndAssert("ivy-latest-compatible-4.xml", "#B;1.4, #C;2.5, #D;1.5, #F;1.0");
    }

    public void testCompatibilityResolve5() throws Exception {
        /* Test data:
            #A;5->{ #B;[1.0,1.5] #C;2.6 }
            #B;1.3->{ }
            #B;1.4->#D;1.5
            #B;1.5->#D;2.0
            #C;2.6->#D;1.6
         */
        resolveAndAssert("ivy-latest-compatible-5.xml", "#B;1.3, #C;2.6, #D;1.6");
    }

    public void testCompatibilityResolve6() throws Exception {
        /* Test data:
            #A;6->{ #B;[3.0,3.5] #C;4.6 }
            #B;3.4->#D;2.5
            #B;3.5->#D;3.0
            #C;4.6->#D;2.5
            #D;3.0->#B;3.5 (circular dependency)
            #D;2.5->#B;3.4 (circular dependency)
         */
        resolveAndAssert("ivy-latest-compatible-6.xml", "#B;3.4, #C;4.6, #D;2.5");
    }

    public void testCompatibilityResolve7() throws Exception {
        /* Test data: (same as 1, but with reverse dependencies order 
            #A;7-> { #C;[2.0,2.5] #B;1.4 }
            #B;1.4->#D;1.5
            #C;2.5->#D;[1.0,1.6]
         */
        resolveAndAssert("ivy-latest-compatible-7.xml", "#B;1.4, #C;2.5, #D;1.5");
    }


    public void testConflict() throws Exception {
        /* Test data:
            #A;conflict-> { #B;[1.5,1.6] #C;2.5 }
            #B;1.5->#D;2.0
            #B;1.6->#D;2.0
            #C;2.5->#D;[1.0,1.6]
         */
        try {
            ivy.resolve(
                LatestCompatibleConflictManagerTest.class.getResource("ivy-latest-compatible-conflict.xml"),
                getResolveOptions());

            fail("Resolve should have failed with a conflict");
        } catch (StrictConflictException e) {
            // this is expected
        }
    }

    private void resolveAndAssert(String ivyFile, String expectedModuleSet) 
        throws ParseException, IOException {
        ResolveReport report = ivy.resolve(
            LatestCompatibleConflictManagerTest.class.getResource(ivyFile),
            getResolveOptions());
        assertFalse(report.hasError());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        TestHelper.assertModuleRevisionIds(expectedModuleSet, 
            defaultReport.getModuleRevisionIds());
    }
    
    private ResolveOptions getResolveOptions() {
        return new ResolveOptions()
                .setCache(CacheManager.getInstance(ivy.getSettings()))
                .setValidate(false);
    }
}
