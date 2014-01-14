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

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;

/**
 * Cleans the content of Ivy cache(s).
 */
public class IvyCleanCache extends IvyTask {
    public static final String ALL = "*";

    public static final String NONE = "NONE";

    private boolean resolution = true;

    private String cache = ALL;

    public String getCache() {
        return cache;
    }

    /**
     * Sets the name of the repository cache to clean, '*' for all caches, 'NONE' for no repository
     * cache cleaning at all.
     * 
     * @param cache
     *            the name of the cache to clean. Must not be <code>null</code>.
     */
    public void setCache(String cache) {
        this.cache = cache;
    }

    public boolean isResolution() {
        return resolution;
    }

    /**
     * Sets weither the resolution cache should be cleaned or not.
     * 
     * @param resolution
     *            <code>true</code> if the resolution cache should be cleaned, <code>false</code>
     *            otherwise.
     */
    public void setResolution(boolean resolution) {
        this.resolution = resolution;
    }

    public void doExecute() throws BuildException {
        IvySettings settings = getIvyInstance().getSettings();
        if (isResolution()) {
            settings.getResolutionCacheManager().clean();
        }
        if (ALL.equals(getCache())) {
            RepositoryCacheManager[] caches = settings.getRepositoryCacheManagers();
            for (int i = 0; i < caches.length; i++) {
                caches[i].clean();
            }
        } else if (!NONE.equals(getCache())) {
            RepositoryCacheManager cache = settings.getRepositoryCacheManager(getCache());
            if (cache == null) {
                throw new BuildException("unknown cache '" + getCache() + "'");
            } else {
                cache.clean();
            }
        }
    }
}
