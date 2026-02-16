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
package org.apache.ivy.core.cache;

import static org.junit.Assert.fail;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class DefaultResolutionCacheManagerTest {

    private File cacheDir;

    @Before
    public void setUp() throws Exception {
        cacheDir = new File("build/cache");
        cacheDir.mkdirs();
    }

    @After
    public void tearDown() {
        if (cacheDir != null && cacheDir.exists()) {
            FileUtil.forceDelete(cacheDir);
        }
    }

    @Test
    public void wontWriteIvyFileOutsideOfCache() throws Exception {
        DefaultResolutionCacheManager cm = new DefaultResolutionCacheManager(cacheDir) {
            @Override
            public File getResolvedIvyFileInCache(ModuleRevisionId mrid) {
                return new File(getResolutionCacheRoot(), "../test.ivy.xml");
            }
        };
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org", "name", "rev");
        try {
            cm.saveResolvedModuleDescriptor(DefaultModuleDescriptor.newDefaultInstance(mrid));
            fail("expected exception");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
