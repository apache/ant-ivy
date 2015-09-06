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
package org.apache.ivy.core;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.apache.ivy.Ivy;
import org.apache.ivy.TestHelper;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

/**
 * Not a Junit test, performance depends on the machine on which the test is run...
 */
public class TestPerformance {
    private static final String PATTERN = "build/test/perf/[module]/[artifact]-[revision].[ext]";

    private final Ivy ivy;

    public TestPerformance() throws Exception {
        ivy = new Ivy();
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName("def");
        resolver.setSettings(ivy.getSettings());

        resolver.addIvyPattern(PATTERN);
        resolver.addArtifactPattern(PATTERN);

        ivy.getSettings().addResolver(resolver);
        ivy.getSettings().setDefaultResolver("def");
    }

    protected void setUp() throws Exception {
        TestHelper.createCache();
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    private void cleanRepo() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(new File("build/test/perf"));
        del.execute();
    }

    private void generateModules(int nbModules, int minDependencies, int maxDependencies,
            int minVersions, int maxVersions) throws IOException {
        int nb = 0;
        int curDep = 1;
        int varDeps = maxDependencies - minDependencies;
        int varVersions = maxVersions - minVersions;
        Random r = new Random(System.currentTimeMillis());

        while (nb < nbModules) {
            int deps = minDependencies + r.nextInt(varDeps + 1);
            int versions = minVersions + r.nextInt(varVersions + 1);

            int prevCurDep = curDep;
            for (int ver = 0; ver < versions; ver++) {
                DefaultModuleDescriptor md = new DefaultModuleDescriptor(
                        ModuleRevisionId.newInstance("apache", "mod" + nb, "1." + ver),
                        "integration", new Date());

                curDep = prevCurDep;
                for (int i = 0; i < deps && curDep < nbModules; i++) {
                    int d;
                    if (i % 2 == 1) {
                        d = nb + i;
                        if (d >= prevCurDep) {
                            d = curDep;
                            curDep++;
                        }
                    } else {
                        d = curDep;
                        curDep++;
                    }
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                            md,
                            ModuleRevisionId.newInstance("apache", "mod" + d, "latest.integration"),
                            false, false, true);
                    dd.addDependencyConfiguration("default", "default");
                    md.addDependency(dd);
                }
                XmlModuleDescriptorWriter.write(md, new File("build/test/perf/mod" + nb + "/ivy-1."
                        + ver + ".xml"));
                FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"),
                    new File("build/test/perf/mod" + nb + "/mod" + nb + "-1." + ver + ".jar"), null);
            }
            nb++;
        }
    }

    public void testPerfs() throws Exception {
        generateModules(70, 2, 5, 2, 15);

        long start = System.currentTimeMillis();
        ResolveReport report = ivy.resolve(new File("build/test/perf/mod0/ivy-1.0.xml"),
            getResolveOptions(new String[] {"*"}).setRevision("1.0"));
        long end = System.currentTimeMillis();
        System.out.println("resolve " + report.getConfigurationReport("default").getNodesNumber()
                + " modules took " + (end - start) + " ms");

        cleanRepo();
    }

    private ResolveOptions getResolveOptions(String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }

    public static void main(String[] args) throws Exception {
        TestPerformance t = new TestPerformance();
        t.setUp();
        t.testPerfs();
        t.tearDown();
    }
}
