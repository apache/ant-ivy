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
package org.apache.ivy.ant;

import java.io.File;

import org.apache.ivy.TestHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

public class IvyCleanCacheTest extends TestCase {
    private IvyCleanCache cleanCache;

    private File cacheDir;

    private File repoCache2;

    private File repoCache;

    private File resolutionCache;

    protected void setUp() throws Exception {
        Project p = TestHelper.newProject();
        cacheDir = new File("build/cache");
        p.setProperty("cache", cacheDir.getAbsolutePath());
        cleanCache = new IvyCleanCache();
        cleanCache.setProject(p);
        IvyConfigure settings = new IvyConfigure();
        settings.setProject(p);
        settings.setUrl(IvyCleanCacheTest.class.getResource("ivysettings-cleancache.xml")
                .toExternalForm());
        settings.perform();

        resolutionCache = new File(cacheDir, "resolution");
        repoCache = new File(cacheDir, "repository");
        repoCache2 = new File(cacheDir, "repository2");
        resolutionCache.mkdirs();
        repoCache.mkdirs();
        repoCache2.mkdirs();
    }

    public void testCleanAll() throws Exception {
        cleanCache.perform();
        assertFalse(resolutionCache.exists());
        assertFalse(repoCache.exists());
        assertFalse(repoCache2.exists());
    }

    public void testResolutionOnly() throws Exception {
        cleanCache.setCache(IvyCleanCache.NONE);
        cleanCache.perform();
        assertFalse(resolutionCache.exists());
        assertTrue(repoCache.exists());
        assertTrue(repoCache2.exists());
    }

    public void testRepositoryOnly() throws Exception {
        cleanCache.setResolution(false);
        cleanCache.perform();
        assertTrue(resolutionCache.exists());
        assertFalse(repoCache.exists());
        assertFalse(repoCache2.exists());
    }

    public void testOneRepositoryOnly() throws Exception {
        cleanCache.setResolution(false);
        cleanCache.setCache("mycache");
        cleanCache.perform();
        assertTrue(resolutionCache.exists());
        assertFalse(repoCache.exists());
        assertTrue(repoCache2.exists());
    }

    public void testUnknownCache() throws Exception {
        cleanCache.setResolution(false);
        cleanCache.setCache("yourcache");
        try {
            cleanCache.perform();
            fail("clean cache should have raised an exception with unkown cache");
        } catch (BuildException e) {
            assertTrue(e.getMessage().indexOf("yourcache") != -1);
        }
    }
}
