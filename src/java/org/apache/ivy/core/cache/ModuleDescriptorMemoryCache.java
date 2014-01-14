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
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.util.Message;

/**
 * Cache ModuleDescriptors so that when the same module is used twice (in multi-module build for
 * instance), it is parsed only once. This cache is has a limited size, and keep the most recently
 * used entries. The entry in the cache are invalidated if there is a change to one variable used in
 * the module descriptor.
 */
class ModuleDescriptorMemoryCache {

    private final int maxSize;

    private final LinkedHashMap/* <File,CacheEntry> */valueMap;

    /**
     * Create a cache of the given size
     * 
     * @param size
     */
    public ModuleDescriptorMemoryCache(int size) {
        this.maxSize = size;
        this.valueMap = new LinkedHashMap(size);
    }

    public ModuleDescriptor get(File ivyFile, ParserSettings ivySettings, boolean validated,
            ModuleDescriptorProvider mdProvider) throws ParseException, IOException {

        ModuleDescriptor descriptor = getFromCache(ivyFile, ivySettings, validated);
        if (descriptor == null) {
            descriptor = getStale(ivyFile, ivySettings, validated, mdProvider);
        }
        return descriptor;
    }

    /**
     * Get the module descriptor from the mdProvider and store it into the cache.
     */
    public ModuleDescriptor getStale(File ivyFile, ParserSettings ivySettings, boolean validated,
            ModuleDescriptorProvider mdProvider) throws ParseException, IOException {
        ParserSettingsMonitor settingsMonitor = new ParserSettingsMonitor(ivySettings);
        ModuleDescriptor descriptor = mdProvider.provideModule(
            settingsMonitor.getMonitoredSettings(), ivyFile, validated);
        putInCache(ivyFile, settingsMonitor, validated, descriptor);
        return descriptor;
    }

    ModuleDescriptor getFromCache(File ivyFile, ParserSettings ivySettings, boolean validated) {
        if (maxSize <= 0) {
            // cache is disbaled
            return null;
        }
        CacheEntry entry = (CacheEntry) valueMap.get(ivyFile);
        if (entry != null) {
            if (entry.isStale(validated, ivySettings)) {
                Message.debug("Entry is found in the ModuleDescriptorCache but entry should be "
                        + "reevaluated : " + ivyFile);
                valueMap.remove(ivyFile);
                return null;
            } else {
                // Move the entry at the end of the list
                valueMap.remove(ivyFile);
                valueMap.put(ivyFile, entry);
                Message.debug("Entry is found in the ModuleDescriptorCache : " + ivyFile);
                return entry.md;
            }
        } else {
            Message.debug("No entry is found in the ModuleDescriptorCache : " + ivyFile);
            return null;
        }
    }

    void putInCache(File url, ParserSettingsMonitor ivySettingsMonitor, boolean validated,
            ModuleDescriptor descriptor) {
        if (maxSize <= 0) {
            // cache is disabled
            return;
        }
        if (valueMap.size() >= maxSize) {
            Message.debug("ModuleDescriptorCache is full, remove one entry");
            Iterator it = valueMap.values().iterator();
            it.next();
            it.remove();
        }
        valueMap.put(url, new CacheEntry(descriptor, validated, ivySettingsMonitor));
    }

    private static class CacheEntry {
        private final ModuleDescriptor md;

        private final boolean validated;

        private final ParserSettingsMonitor parserSettingsMonitor;

        CacheEntry(ModuleDescriptor md, boolean validated,
                ParserSettingsMonitor parserSettingsMonitor) {
            this.md = md;
            this.validated = validated;
            this.parserSettingsMonitor = parserSettingsMonitor;
        }

        boolean isStale(boolean validated, ParserSettings newParserSettings) {
            return (validated && !this.validated)
                    || parserSettingsMonitor.hasChanged(newParserSettings);
        }
    }

}
