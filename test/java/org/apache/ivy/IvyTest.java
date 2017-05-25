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
package org.apache.ivy;

import java.io.File;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.MockMessageLogger;

import junit.framework.TestCase;

public class IvyTest extends TestCase {
    private File cache;

    protected void setUp() throws Exception {
        createCache();
        System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        CacheCleaner.deleteDir(cache);
    }

    public void testMultipleInstances() throws Exception {
        // this test checks that IvyContext is properly set and unset when using multiple instances
        // of Ivy. We also check logging, because it heavily relies on IvyContext.

        // we start by loading one ivy instance and using it to resolve some dependencies
        MockMessageLogger mockLogger = new MockMessageLogger();
        Ivy ivy = Ivy.newInstance();
        ivy.getLoggerEngine().setDefaultLogger(mockLogger);
        ivy.configure(new File("test/repositories/ivysettings.xml"));
        assertFalse("IvyContext should be cleared and return a default Ivy instance", IvyContext
                .getContext().getIvy() == ivy);

        ResolveReport report = ivy.resolve(new File(
                "test/repositories/1/org1/mod1.1/ivys/ivy-1.0.xml"),
            getResolveOptions(ivy, new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        mockLogger.assertLogContains("mod1.1");
        assertFalse("IvyContext should be cleared and return a default Ivy instance", IvyContext
                .getContext().getIvy() == ivy);

        // then we load another instance, and use it for another resolution
        MockMessageLogger mockLogger2 = new MockMessageLogger();
        Ivy ivy2 = new Ivy();
        ivy2.getLoggerEngine().setDefaultLogger(mockLogger2);
        ivy2.configure(new File("test/repositories/norev/ivysettings.xml").toURI().toURL());
        report = ivy2.resolve(new File("test/repositories/norev/ivy.xml"),
            getResolveOptions(ivy2, new String[] {"*"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        mockLogger2.assertLogContains("norev/ivysettings.xml");
        assertFalse("IvyContext should be cleared and return a default Ivy instance", IvyContext
                .getContext().getIvy() == ivy2);

        // finally we reuse the first instance to make another resolution
        report = ivy.resolve(new File("test/repositories/1/org6/mod6.1/ivys/ivy-0.3.xml"),
            getResolveOptions(ivy, new String[] {"extension"}));
        assertNotNull(report);
        assertFalse(report.hasError());
        mockLogger.assertLogContains("mod6.1");
        assertFalse("IvyContext should be cleared and return a default Ivy instance", IvyContext
                .getContext().getIvy() == ivy);
    }

    private ResolveOptions getResolveOptions(Ivy ivy, String[] confs) {
        return getResolveOptions(ivy.getSettings(), confs);
    }

    private ResolveOptions getResolveOptions(IvySettings settings, String[] confs) {
        return new ResolveOptions().setConfs(confs);
    }
}
