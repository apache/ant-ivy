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
package org.apache.ivy.plugins.lock;

import java.io.File;
import java.text.ParseException;

import junit.framework.TestCase;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.CopyProgressEvent;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

public class ArtifactLockStrategyTest extends TestCase {
    protected void setUp() throws Exception {
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
        FileUtil.forceDelete(new File("build/test/cache"));
    }
    protected void tearDown() throws Exception {
        FileUtil.forceDelete(new File("build/test/cache"));
    }

    public void testConcurrentResolve() throws Exception {
        // we use different settings because Ivy do not support multi thread resolve with the same
        // settings yet and this is not what this test is about: the focus of this test is running
        // concurrent resolves in separate vms but using the same cache. We don't span the test on
        // multiple vms, but using separate settings we should only run into shared cache related
        // issues, and not multi thread related issues.
        IvySettings settings1 = new IvySettings();
        IvySettings settings2 = new IvySettings();
                
        ResolveThread t1 = asyncResolve(
            settings1, createSlowResolver(settings1, 100), "org6#mod6.4;3");
        ResolveThread t2 = asyncResolve(
            settings2, createSlowResolver(settings2, 50), "org6#mod6.4;3");
        t1.join(1000);
        t2.join(1000);
        assertFound("org6#mod6.4;3", t1.getResult());
        assertFound("org6#mod6.4;3", t2.getResult());
    }    

    
    private CacheManager newCacheManager(IvySettings settings) {
        CacheManager cacheManager = new CacheManager(settings, new File("build/test/cache"));
        cacheManager.setLockStrategy(new ArtifactLockStrategy());
        return cacheManager;
    }
    
    
    private FileSystemResolver createSlowResolver(IvySettings settings, final int sleep) {
        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setRepository(new FileRepository() {
            private RepositoryCopyProgressListener progress = new RepositoryCopyProgressListener(this) {
                public void progress(CopyProgressEvent evt) {
                    super.progress(evt);
                    sleepSilently(sleep); // makes the file copy longer to test concurrency issues
                }
            };
            protected RepositoryCopyProgressListener getProgressListener() {
                return progress ;
            }
        });
        resolver.setName("test");
        resolver.setSettings(settings);
        resolver.addIvyPattern(
            "test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]");
        resolver.addArtifactPattern(
            "test/repositories/1/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]");
        return resolver;
    }

    
    private ResolveThread asyncResolve(
            IvySettings settings, FileSystemResolver resolver, String module) {
        ResolveThread thread = new ResolveThread(settings, resolver, module);
        thread.start();
        return thread;
    }
    
    
    private void assertFound(String module, ResolvedModuleRevision rmr) {
        assertNotNull(rmr);
        assertEquals(module, rmr.getId().toString());
    }
    private ResolvedModuleRevision resolveModule(
            IvySettings settings, FileSystemResolver resolver, String module)
            throws ParseException {
        return resolver.getDependency(
            new DefaultDependencyDescriptor(ModuleRevisionId.parse(module), false), 
            new ResolveData(
                new ResolveEngine(settings, new EventManager(), new SortEngine(settings)), 
                new ResolveOptions().setCache(
                    newCacheManager(settings))));
    }
    private void sleepSilently(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
        }
    }
    private class ResolveThread extends Thread {
        private ResolvedModuleRevision result;
        private IvySettings settings;
        private FileSystemResolver resolver;
        private String module;
        
        public ResolveThread(IvySettings settings, FileSystemResolver resolver, String module) {
            this.settings = settings;
            this.resolver = resolver;
            this.module = module;
        }
        
        public ResolvedModuleRevision getResult() {
            return result;
        }
        public void run() {
            try {
                ResolvedModuleRevision rmr = resolveModule(settings, resolver, module);
                synchronized (this) {
                    result = rmr;
                }
            } catch (ParseException e) {
                Message.info("parse exception "+e);
            }
        }
    }

}
