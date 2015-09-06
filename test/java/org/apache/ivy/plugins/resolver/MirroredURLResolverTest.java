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
package org.apache.ivy.plugins.resolver;

import org.apache.ivy.TestHelper;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.apache.ivy.core.sort.SortEngine;

import junit.framework.TestCase;

public class MirroredURLResolverTest extends TestCase {

    private IvySettings settings;

    private ResolveEngine engine;

    private ResolveData data;

    protected void setUp() throws Exception {
        settings = new IvySettings();
        engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
        data = new ResolveData(engine, new ResolveOptions());
        TestHelper.createCache();
        settings.setDefaultCache(TestHelper.cache);
        settings.setVariable("test.mirroredurl.mirrorlist-solo.url",
            this.getClass().getResource("mirrorlist-solo.txt").toExternalForm());
        settings.setVariable("test.mirroredurl.mirrorlist-failover.url", this.getClass()
                .getResource("mirrorlist-failover.txt").toExternalForm());
        settings.setVariable("test.mirroredurl.mirrorlist-fail.url",
            this.getClass().getResource("mirrorlist-fail.txt").toExternalForm());
        new XmlSettingsParser(settings).parse(MirroredURLResolverTest.class
                .getResource("mirror-resolver-settings.xml"));
    }

    protected void tearDown() throws Exception {
        TestHelper.cleanCache();
    }

    public void testSolo() throws Exception {
        DependencyResolver resolver = settings.getResolver("solo");
        assertNotNull(resolver);
        assertTrue(resolver instanceof MirroredURLResolver);
        MirroredURLResolver mirrored = (MirroredURLResolver) resolver;

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.4"), false);
        ResolvedModuleRevision rmr = mirrored.getDependency(dd, data);
        assertNotNull(rmr);
    }

    public void testFailover() throws Exception {
        DependencyResolver resolver = settings.getResolver("failover");
        assertNotNull(resolver);
        assertTrue(resolver instanceof MirroredURLResolver);
        MirroredURLResolver mirrored = (MirroredURLResolver) resolver;

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.4"), false);
        ResolvedModuleRevision rmr = mirrored.getDependency(dd, data);
        assertNotNull(rmr);
    }

    public void testFail() throws Exception {
        DependencyResolver resolver = settings.getResolver("fail");
        assertNotNull(resolver);
        assertTrue(resolver instanceof MirroredURLResolver);
        MirroredURLResolver mirrored = (MirroredURLResolver) resolver;

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(
                ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.4"), false);
        ResolvedModuleRevision rmr = mirrored.getDependency(dd, data);
        assertNull(rmr);
    }

}
