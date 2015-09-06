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
package org.apache.ivy.core.publish;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.FileUtil;

import junit.framework.TestCase;

public class PublishEngineTest extends TestCase {
    protected void setUp() throws Exception {
        System.setProperty("ivy.cache.dir", new File("build/test/publish/cache").getAbsolutePath());
        FileUtil.forceDelete(new File("build/test/publish"));
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(new File("build/test/publish"));
    }

    public void testAtomicity() throws Exception {
        IvySettings settings = new IvySettings();
        final PublishEngine engine = new PublishEngine(settings, new EventManager());
        final int[] counter = new int[] {0};

        final DefaultModuleDescriptor md = DefaultModuleDescriptor
                .newDefaultInstance(ModuleRevisionId.parse("#A;1.0"));
        final FileSystemResolver resolver = new FileSystemResolver() {
            public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
                super.publish(artifact, src, overwrite);
                synchronized (PublishEngineTest.this) {
                    counter[0]++;
                }
                sleepSilently(50);
                synchronized (PublishEngineTest.this) {
                    counter[0]++;
                }
            }
        };
        resolver.setName("test");
        resolver.setSettings(settings);
        String publishRepoDir = new File("build/test/publish/repo").getAbsolutePath();
        resolver.addIvyPattern(publishRepoDir + "/[module]/[revision]/[artifact].[ext]");
        resolver.addArtifactPattern(publishRepoDir + "/[module]/[revision]/[artifact].[ext]");

        FileUtil.copy(new File("test/repositories/1/org1/mod1.1/jars/mod1.1-1.0.jar"), new File(
                "build/test/publish/module/A.jar"), null);
        XmlModuleDescriptorWriter.write(md, new File("build/test/publish/module/ivy.xml"));

        resolveAndAssertNotFound(settings, resolver, "#A;latest.integration", "before publishing");

        // run publish asynchronously
        new Thread() {
            public void run() {
                try {
                    engine.publish(md, Arrays
                            .asList(new String[] {"build/test/publish/module/[artifact].[ext]"}),
                        resolver, new PublishOptions()
                                .setSrcIvyPattern("build/test/publish/module/[artifact].[ext]"));
                    synchronized (PublishEngineTest.this) {
                        counter[0]++;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        while (true) {
            sleepSilently(5);
            synchronized (this) {
                if (counter[0] == 5) {
                    break;
                } else if (counter[0] < 4) {
                    resolveAndAssertNotFound(settings, resolver, "#A;latest.integration", "after "
                            + (counter[0] / 2) + " artifacts published");
                }
            }
        }
        resolveAndAssertFound(settings, resolver, "#A;1.0");
    }

    private void resolveAndAssertNotFound(IvySettings settings, FileSystemResolver resolver,
            String module, String context) throws ParseException {
        ResolvedModuleRevision rmr = resolveModule(settings, resolver, module);
        assertNull("module found " + context + ". module=" + rmr, rmr);
    }

    private void resolveAndAssertFound(IvySettings settings, FileSystemResolver resolver,
            String module) throws ParseException {
        ResolvedModuleRevision rmr = resolveModule(settings, resolver, module);
        assertNotNull(rmr);
        assertEquals(module, rmr.getId().toString());
    }

    private ResolvedModuleRevision resolveModule(IvySettings settings, FileSystemResolver resolver,
            String module) throws ParseException {
        return resolver.getDependency(
            new DefaultDependencyDescriptor(ModuleRevisionId.parse(module), false),
            new ResolveData(new ResolveEngine(settings, new EventManager(),
                    new SortEngine(settings)), new ResolveOptions()));
    }

    private void sleepSilently(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
        }
    }
}
