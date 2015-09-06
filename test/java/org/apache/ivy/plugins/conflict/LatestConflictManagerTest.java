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
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.FileUtil;

import junit.framework.TestCase;

public class LatestConflictManagerTest extends TestCase {

    private Ivy ivy;

    private File _cache;

    protected void setUp() throws Exception {
        ivy = new Ivy();
        ivy.configure(LatestConflictManagerTest.class.getResource("ivysettings-latest.xml"));
        _cache = new File("build/cache");
        _cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(_cache);
    }

    // Test case for issue IVY-388
    public void testIvy388() throws Exception {
        ResolveReport report = ivy.resolve(
            LatestConflictManagerTest.class.getResource("ivy-388.xml"), getResolveOptions());

        List deps = report.getDependencies();
        Iterator dependencies = deps.iterator();
        String[] confs = report.getConfigurations();
        while (dependencies.hasNext()) {
            IvyNode node = (IvyNode) dependencies.next();
            for (int i = 0; i < confs.length; i++) {
                String conf = confs[i];
                if (!node.isEvicted(conf)) {
                    boolean flag1 = report.getConfigurationReport(conf).getDependency(
                        node.getResolvedId()) != null;
                    boolean flag2 = report.getConfigurationReport(conf).getModuleRevisionIds()
                            .contains(node.getResolvedId());
                    assertEquals("Inconsistent data for node " + node + " in conf " + conf, flag1,
                        flag2);
                }
            }
        }
    }

    // Test case for issue IVY-383
    public void testIvy383() throws Exception {
        ResolveReport report = ivy.resolve(
            LatestConflictManagerTest.class.getResource("ivy-383.xml"), getResolveOptions());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        Iterator iter = defaultReport.getModuleRevisionIds().iterator();
        while (iter.hasNext()) {
            ModuleRevisionId mrid = (ModuleRevisionId) iter.next();
            if (mrid.getName().equals("mod1.1")) {
                assertEquals("1.0", mrid.getRevision());
            } else if (mrid.getName().equals("mod1.2")) {
                assertEquals("2.2", mrid.getRevision());
            }
        }
    }

    // Test case for issue IVY-407
    public void testLatestTime1() throws Exception {
        ivy = new Ivy();
        ivy.configure(LatestConflictManagerTest.class.getResource("ivysettings-latest-time.xml"));
        ivy.getSettings().setVariable("ivy.log.conflict.resolution", "true", true);

        // set timestamps, because svn is not preserving this information,
        // and the latest time strategy is relying on it
        long time = System.currentTimeMillis() - 10000;
        new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").setLastModified(time);
        new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.2.jar")
                .setLastModified(time + 2000);

        ResolveReport report = ivy.resolve(
            LatestConflictManagerTest.class.getResource("ivy-latest-time-1.xml"),
            getResolveOptions());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        Iterator iter = defaultReport.getModuleRevisionIds().iterator();
        while (iter.hasNext()) {
            ModuleRevisionId mrid = (ModuleRevisionId) iter.next();
            if (mrid.getName().equals("mod1.1")) {
                assertEquals("1.0", mrid.getRevision());
            } else if (mrid.getName().equals("mod1.2")) {
                assertEquals("2.2", mrid.getRevision());
            }
        }
    }

    public void testLatestTime2() throws Exception {
        ivy = new Ivy();
        ivy.configure(LatestConflictManagerTest.class.getResource("ivysettings-latest-time.xml"));
        ivy.getSettings().setVariable("ivy.log.conflict.resolution", "true", true);

        // set timestamps, because svn is not preserving this information,
        // and the latest time strategy is relying on it
        long time = System.currentTimeMillis() - 10000;
        new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.0.jar").setLastModified(time);
        new File("test/repositories/1/org1/mod1.2/jars/mod1.2-2.2.jar")
                .setLastModified(time + 2000);

        ResolveReport report = ivy.resolve(
            LatestConflictManagerTest.class.getResource("ivy-latest-time-2.xml"),
            getResolveOptions());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        Iterator iter = defaultReport.getModuleRevisionIds().iterator();
        while (iter.hasNext()) {
            ModuleRevisionId mrid = (ModuleRevisionId) iter.next();
            if (mrid.getName().equals("mod1.1")) {
                assertEquals("1.0", mrid.getRevision());
            } else if (mrid.getName().equals("mod1.2")) {
                assertEquals("2.2", mrid.getRevision());
            }
        }
    }

    /*
     * Test case for issue IVY-407 (with transitivity) There are 5 modules A, B, C, D and E. 1)
     * publish C-1.0.0, C-1.0.1 and C-1.0.2 2) B needs C-1.0.0 : retrieve ok and publish B-1.0.0 3)
     * A needs B-1.0.0 and C-1.0.2 : retrieve ok and publish A-1.0.0 4) D needs C-1.0.1 : retrieve
     * ok and publish D-1.0.0 5) E needs D-1.0.0 and A-1.0.0 (D before A in ivy file) retrieve
     * failed to get C-1.0.2 from A (get apparently C-1.0.1 from D)
     */
    public void testLatestTimeTransitivity() throws Exception {
        ivy = new Ivy();
        ivy.configure(LatestConflictManagerTest.class
                .getResource("ivysettings-latest-time-transitivity.xml"));
        ivy.getSettings().setVariable("ivy.log.conflict.resolution", "true", true);

        // set timestamps, because svn is not preserving this information,
        // and the latest time strategy is relying on it
        long time = System.currentTimeMillis() - 10000;
        new File("test/repositories/IVY-407/MyCompany/C/ivy-1.0.0.xml").setLastModified(time);
        new File("test/repositories/IVY-407/MyCompany/C/ivy-1.0.1.xml")
                .setLastModified(time + 2000);
        new File("test/repositories/IVY-407/MyCompany/C/ivy-1.0.2.xml")
                .setLastModified(time + 4000);

        ResolveReport report = ivy.resolve(
            LatestConflictManagerTest.class.getResource("ivy-latest-time-transitivity.xml"),
            getResolveOptions());
        ConfigurationResolveReport defaultReport = report.getConfigurationReport("default");
        Iterator iter = defaultReport.getModuleRevisionIds().iterator();
        while (iter.hasNext()) {
            ModuleRevisionId mrid = (ModuleRevisionId) iter.next();

            if (mrid.getName().equals("A")) {
                assertEquals("A revision should be 1.0.0", "1.0.0", mrid.getRevision());
            } else if (mrid.getName().equals("D")) {
                assertEquals("D revision should be 1.0.0", "1.0.0", mrid.getRevision());
            }
            // by transitivity
            else if (mrid.getName().equals("B")) {
                assertEquals("B revision should be 1.0.0", "1.0.0", mrid.getRevision());
            } else if (mrid.getName().equals("C")) {
                assertEquals("C revision should be 1.0.2", "1.0.2", mrid.getRevision());
            }
        }
    }

    /*
     * Test case for issue IVY-1399:
     * Dependency tree:
     * Mycompany#target;1
     *     MyCompany#A;1
     *         conflicting-dependency#dep;1
     *         OtherCompany#prefers-later;1
     *             conflicting-dependency#dep;2
     *     MyCompany#B;1
     *         MyCompany#A;1
     *             ...
     *         OtherCompany#prefers-later;1
     *             ...
     *         MyCompany#C;1
     *             conflicting-dependency#dep;1
     */
    public void testEvictedModules() throws Exception {
        ivy.configure(LatestConflictManagerTest.class
                .getResource("ivysettings-evicted.xml"));

        ivy.getSettings().setVariable("ivy.log.conflict.resolution", "true", true);
        try {
            ResolveReport report = ivy.resolve(
                new File("test/repositories/IVY-1399/MyCompany/target/ivy-1.xml"),
                getResolveOptions());
            report.getConfigurationReport("all");
        } catch (IllegalStateException e) {
            fail("Resolving target should not throw an exception");
        }
    }

    private ResolveOptions getResolveOptions() {
        return new ResolveOptions().setValidate(false);
    }
}
