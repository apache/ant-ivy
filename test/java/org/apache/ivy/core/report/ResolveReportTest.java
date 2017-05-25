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
package org.apache.ivy.core.report;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.FileUtil;

import junit.framework.TestCase;

public class ResolveReportTest extends TestCase {

    private Ivy ivy;

    private File cache;

    private File deliverDir;

    private File workDir;

    protected void setUp() throws Exception {
        cache = new File("build/cache");
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        createCache();

        deliverDir = new File("build/test/deliver");
        deliverDir.mkdirs();

        workDir = new File("build/test/work");
        workDir.mkdirs();

        ivy = Ivy.newInstance();
        ivy.configure(new File("test/repositories/ivysettings.xml"));
    }

    private void createCache() {
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
        FileUtil.forceDelete(deliverDir);
        FileUtil.forceDelete(workDir);
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return getResolveOptions(ivy.getSettings(), confs);
    }

    private ResolveOptions getResolveOptions(IvySettings settings, String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }

    private void checkFixedMdDependency(DependencyDescriptor dep, String org, String mod,
            String rev, String conf, String[] targetConfs) {
        assertEquals(ModuleRevisionId.newInstance(org, mod, rev), dep.getDependencyRevisionId());
        assertTrue(Arrays.asList(dep.getModuleConfigurations()).contains(conf));
        assertEquals(new HashSet<String>(Arrays.asList(targetConfs)),
            new HashSet<String>(Arrays.asList(dep.getDependencyConfigurations(conf))));
    }

    public void testFixedMdSimple() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(), null);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.1", "1.0");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(1, fixedMd.getDependencies().length);
        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.2", "2.0", "default",
            new String[] {"*"});
    }

    public void testFixedMdTransitiveDependencies() throws Exception {
        // mod2.1 depends on mod1.1 which depends on mod1.2
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.1/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(), null);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.1", "0.3");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(2, fixedMd.getDependencies().length);

        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.1", "1.0", "default",
            new String[] {"*"});
        checkFixedMdDependency(fixedMd.getDependencies()[1], "org1", "mod1.2", "2.0", "default",
            new String[] {"*"});
    }

    public void testFixedMdMultipleExtends() throws Exception {
        // mod6.2 has two confs default and extension
        // mod6.2 depends on mod6.1 in conf (default->extension)
        // conf extension extends default
        // mod6.1 has two confs default and extension
        // mod6.1 depends on mod1.2 2.0 in conf (default->default)
        // conf extension extends default
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org6/mod6.2/ivys/ivy-0.3.xml"),
            getResolveOptions(new String[] {"default", "extension"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(), null);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org6", "mod6.2", "0.3");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default", "extension"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(2, fixedMd.getDependencies().length);

        checkFixedMdDependency(fixedMd.getDependencies()[0], "org6", "mod6.1", "0.4", "extension",
            new String[] {"extension", "default"});
        checkFixedMdDependency(fixedMd.getDependencies()[0], "org6", "mod6.1", "0.4", "default",
            new String[] {"extension", "default"});
        checkFixedMdDependency(fixedMd.getDependencies()[1], "org1", "mod1.2", "2.0", "extension",
            new String[] {"default"});
        checkFixedMdDependency(fixedMd.getDependencies()[1], "org1", "mod1.2", "2.0", "default",
            new String[] {"default"});
    }

    public void testFixedMdRange() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(), null);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.2");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default", "compile"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(1, fixedMd.getDependencies().length);

        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.2", "1.1", "default",
            new String[] {"*"});
        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.2", "1.1", "compile",
            new String[] {"default"});
    }

    public void testFixedMdKeep() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.4/ivys/ivy-1.0.2.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(),
            Arrays.asList(new ModuleId[] {ModuleId.newInstance("org1", "mod1.2")}));

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org1", "mod1.4", "1.0.2");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default", "compile"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(1, fixedMd.getDependencies().length);

        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.2", "[1.0,2.0[",
            "default", new String[] {"*"});
        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.2", "[1.0,2.0[",
            "compile", new String[] {"default"});
    }

    public void testFixedMdTransitiveKeep() throws Exception {
        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org2/mod2.9/ivys/ivy-0.6.xml"),
            getResolveOptions(new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        ModuleDescriptor fixedMd = report.toFixedModuleDescriptor(ivy.getSettings(),
            Arrays.asList(new ModuleId[] {ModuleId.newInstance("org1", "mod1.2")}));

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org2", "mod2.9", "0.6");
        assertEquals(mrid, fixedMd.getModuleRevisionId());

        assertEquals(Arrays.asList(new String[] {"default", "compile"}),
            Arrays.asList(fixedMd.getConfigurationsNames()));

        assertEquals(2, fixedMd.getDependencies().length);

        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.4", "1.0.2", "default",
            new String[] {"*"});
        checkFixedMdDependency(fixedMd.getDependencies()[0], "org1", "mod1.4", "1.0.2", "compile",
            new String[] {"default", "compile"});
        checkFixedMdDependency(fixedMd.getDependencies()[1], "org1", "mod1.2", "[1.0,2.0[",
            "default", new String[] {"*"});
        checkFixedMdDependency(fixedMd.getDependencies()[1], "org1", "mod1.2", "[1.0,2.0[",
            "compile", new String[] {"default"});
    }

}
