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

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModuleDescriptorMemoryCacheTest {

    private ModuleDescriptorMemoryCache cache = new ModuleDescriptorMemoryCache(2);

    private ParserSettings ivySettings = new IvySettings();

    private IvySettings ivySettings2 = new IvySettings();

    private File url1 = null;

    private File url2 = null;

    private File url3 = null;

    private ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("org", "name", "rev");

    private DefaultModuleDescriptor md1 = DefaultModuleDescriptor.newDefaultInstance(mrid1);

    private ModuleRevisionId mrid2 = ModuleRevisionId.newInstance("org", "name", "rev2");

    private DefaultModuleDescriptor md2 = DefaultModuleDescriptor.newDefaultInstance(mrid2);

    private ModuleRevisionId mrid3 = ModuleRevisionId.newInstance("org", "name", "rev3");

    private DefaultModuleDescriptor md3 = DefaultModuleDescriptor.newDefaultInstance(mrid3);

    @Before
    public void setUp() throws IOException {
        url1 = File.createTempFile("ivy", "xml");
        md1.setLastModified(url1.lastModified());
        url1.deleteOnExit();

        url2 = File.createTempFile("ivy", "xml");
        md2.setLastModified(url2.lastModified());
        url2.deleteOnExit();

        url3 = File.createTempFile("ivy", "xml");
        md3.setLastModified(url3.lastModified());
        url3.deleteOnExit();
    }

    @Test
    public void testUseModuleDescriptorProviderWhenModuleNotCached() throws ParseException,
            IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        providerMock.assertCalled();
    }

    @Test
    public void testCacheResultOfModuleDescriptorProvider() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

    @Test
    public void testValidationClearInvalidatedCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md1);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, true, providerMock2));
        providerMock2.assertCalled();
    }

    @Test
    public void testValidationDontClearvalidatedCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.get(url1, ivySettings, true, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

    @Test
    public void testSizeIsLimited() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock1b = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        ModuleDescriptorProviderMock providerMock3 = new ModuleDescriptorProviderMock(md3);
        cache.get(url1, ivySettings, false, providerMock);
        cache.get(url2, ivySettings, false, providerMock2);
        cache.get(url3, ivySettings, false, providerMock3); // adding 1
        cache.get(url1, ivySettings, false, providerMock1b); // and one has been removed
        providerMock1b.assertCalled();
    }

    @Test
    public void testLastRecentlyUsedIsFlushedWhenSizeExceed() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        ModuleDescriptorProviderMock providerMock2b = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock3 = new ModuleDescriptorProviderMock(md3);
        cache.get(url1, ivySettings, false, providerMock);
        cache.get(url2, ivySettings, false, providerMock2);
        cache.get(url1, ivySettings, false, null);
        cache.get(url3, ivySettings, false, providerMock3);
        cache.get(url1, ivySettings, false, null); // and one has been removed
        cache.get(url2, ivySettings, false, providerMock2b);
        providerMock2b.assertCalled();
    }

    @Test
    public void testVariableChangeInvalidateEntry() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md1);
        ivySettings2.getVariables().setVariable("val", "firstVal", true);
        assertEquals(md1, cache.get(url1, ivySettings2, false, providerMock));
        ivySettings2.getVariables().setVariable("val", "changedVal", true);
        assertEquals(md1, cache.get(url1, ivySettings2, false, providerMock2));
        providerMock2.assertCalled();
    }

    @Test
    public void testGetStaleDontReadFromCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md2, cache.getStale(url1, ivySettings, false, providerMock2));
        providerMock2.assertCalled();
    }

    @Test
    public void testGetStaleStoreResultInCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.getStale(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

    @Test
    public void testASizeOf0MeansNoCache() throws ParseException, IOException {
        cache = new ModuleDescriptorMemoryCache(0);
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md1);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
        providerMock2.assertCalled();
    }

    private static class ModuleDescriptorProviderMock implements ModuleDescriptorProvider {

        private boolean called = false;

        private final ModuleDescriptor result;

        public ModuleDescriptorProviderMock(ModuleDescriptor result) {
            this.result = result;
        }

        public ModuleDescriptor provideModule(ParserSettings ivySettings, File descriptorFile,
                boolean validate) {
            if (ivySettings != null) {
                ivySettings.substitute("${val}");
            }
            called = true;
            return result;
        }

        public void assertCalled() {
            assertTrue(called);
        }
    }

}
