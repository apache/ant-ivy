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
package org.apache.ivy.osgi.updatesite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.repo.ModuleDescriptorWrapper;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.util.CacheCleaner;
import org.apache.ivy.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class UpdateSiteLoaderTest {

    private UpdateSiteLoader loader;

    private File cache;

    @Before
    public void setUp() {
        IvySettings ivySettings = new IvySettings();
        cache = new File("build/cache");
        cache.mkdirs();
        ivySettings.setDefaultCache(cache);
        CacheResourceOptions options = new CacheResourceOptions();
        loader = new UpdateSiteLoader(ivySettings.getDefaultRepositoryCacheManager(), null, options, null);
    }

    @After
    public void tearDown() {
        CacheCleaner.deleteDir(cache);
    }

    @Test
    public void testIvyDE() throws IOException, ParseException, SAXException, URISyntaxException {
        RepoDescriptor site = loader.load(new URI(
                "http://www.apache.org/dist/ant/ivyde/updatesite/"));
        assertTrue(site.getModules().hasNext());
        Iterator<ModuleDescriptorWrapper> it = site.getModules();
        while (it.hasNext()) {
            String name = it.next().getModuleDescriptor().getModuleRevisionId().getName();
            assertTrue(name, name.startsWith("org.apache.ivy"));
        }
    }

    @Test
    public void testM2Eclipse() throws IOException, ParseException, SAXException,
            URISyntaxException {
        RepoDescriptor site = loader.load(new URI(
                "http://download.eclipse.org/technology/m2e/releases/"));
        assertTrue(CollectionUtils.toList(site.getModules()).size() > 20);
    }

    @Ignore
    @Test
    public void testHeliosEclipse() throws IOException, ParseException, SAXException,
            URISyntaxException {
        RepoDescriptor site = loader.load(new URI("http://download.eclipse.org/releases/helios/"));
        assertTrue(CollectionUtils.toList(site.getModules()).size() > 900);
    }

    @Test
    public void testComposite() throws Exception {
        RepoDescriptor site = loader.load(new File("test/test-p2/composite/").toURI());
        assertEquals(8, CollectionUtils.toList(site.getModules()).size());

        // check that the url of the artifact is correctly resolved
        String path = new File("test/test-p2/ivyde-repo/").toURI().toURL().toExternalForm();
        ModuleDescriptor md = site.getModules().next().getModuleDescriptor();
        assertTrue(md.getAllArtifacts()[0].getUrl().toExternalForm().startsWith(path));
    }
}
