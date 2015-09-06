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
package org.apache.ivy.core.cache;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ParserSettings;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ModuleDescriptorMemoryCacheTest extends TestCase {

    ModuleDescriptorMemoryCache cache = new ModuleDescriptorMemoryCache(2);

    ParserSettings ivySettings = new IvySettings();

    IvySettings ivySettings2 = new IvySettings();

    File url1 = new File("file://cached/file.txt");;

    File url2 = new File("file://cached/file2.txt");;

    File url3 = new File("file://cached/file3.txt");;

    ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("org", "name", "rev");

    ModuleDescriptor md1 = DefaultModuleDescriptor.newDefaultInstance(mrid1);

    ModuleRevisionId mrid2 = ModuleRevisionId.newInstance("org", "name", "rev2");

    ModuleDescriptor md2 = DefaultModuleDescriptor.newDefaultInstance(mrid2);

    ModuleRevisionId mrid3 = ModuleRevisionId.newInstance("org", "name", "rev3");

    ModuleDescriptor md3 = DefaultModuleDescriptor.newDefaultInstance(mrid3);

    public void testUseModuleDescriptorProviderWhenModuleNotCached() throws ParseException,
            IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        providerMock.assertCalled();
    }

    public void testCacheResultOfModuleDescriptorProvider() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

    public void testValidationClearInvalidatedCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md1);
        ;
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, true, providerMock2));
        providerMock2.assertCalled();
    }

    public void testValidationDontClearvalidatedCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.get(url1, ivySettings, true, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

    public void testSizeIsLimitied() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock1b = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        ModuleDescriptorProviderMock providerMock3 = new ModuleDescriptorProviderMock(md3);
        cache.get(url1, ivySettings, false, providerMock);
        cache.get(url2, ivySettings, false, providerMock2);
        cache.get(url3, ivySettings, false, providerMock3);// adding 1
        cache.get(url1, ivySettings, false, providerMock1b);// and one has been removed
        providerMock1b.assertCalled();
    }

    public void testLastRecentlyUsedIsFlushedWhenSizeExceed() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        ModuleDescriptorProviderMock providerMock2b = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock3 = new ModuleDescriptorProviderMock(md3);
        cache.get(url1, ivySettings, false, providerMock);
        cache.get(url2, ivySettings, false, providerMock2);
        cache.get(url1, ivySettings, false, null);
        cache.get(url3, ivySettings, false, providerMock3);
        cache.get(url1, ivySettings, false, null);// and one has been removed
        cache.get(url2, ivySettings, false, providerMock2b);
        providerMock2b.assertCalled();
    }

    public void testVariableChangeInvalidateEntry() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md1);
        assertEquals(md1, cache.get(url1, ivySettings2, false, providerMock));
        ivySettings2.getVariables().setVariable("val", "changedVal", true);
        assertEquals(md1, cache.get(url1, ivySettings2, false, providerMock2));
        providerMock2.assertCalled();
    }

    public void testGetStaleDontReadFromCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = new ModuleDescriptorProviderMock(md2);
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock));
        assertEquals(md2, cache.getStale(url1, ivySettings, false, providerMock2));
        providerMock2.assertCalled();
    }

    public void testGetStaleStoreResultInCache() throws ParseException, IOException {
        ModuleDescriptorProviderMock providerMock = new ModuleDescriptorProviderMock(md1);
        ModuleDescriptorProviderMock providerMock2 = null;
        assertEquals(md1, cache.getStale(url1, ivySettings, false, providerMock));
        assertEquals(md1, cache.get(url1, ivySettings, false, providerMock2));
    }

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
            Assert.assertTrue(called);
        }
    }

}
