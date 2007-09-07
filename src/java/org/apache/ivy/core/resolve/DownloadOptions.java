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
package org.apache.ivy.core.resolve;

import java.io.File;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.settings.IvySettings;

public class DownloadOptions {
    private CacheManager cacheManager;

    private EventManager eventManager = null; // can be null

    private boolean useOrigin = false;

    public DownloadOptions(IvySettings settings, File cache) {
        this(new CacheManager(settings, cache));
    }

    public DownloadOptions(CacheManager cacheManager) {
        this(cacheManager, null, false);
    }

    public DownloadOptions(CacheManager cacheManager, EventManager eventManager,
            boolean useOrigin) {
        this.cacheManager = cacheManager;
        this.eventManager = eventManager;
        this.useOrigin = useOrigin;
    }

    public boolean isUseOrigin() {
        return useOrigin;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

}
