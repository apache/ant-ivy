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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.RelativeUrlResolver;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;

/**
 * Keep traces of the usage of a ParserSettings in order to check afterwards that the relevant
 * settings didn't changed.
 * <p>
 * A ParserSettingsMonitor provide a ParserSettings that must be used in place of the orignal one.
 * </p>
 * <p>
 * The current implementation consider that a settings changed iff one of the used variable has
 * changed.
 * </p>
 */
class ParserSettingsMonitor {

    private ParserSettings delegatedSettings;

    private final Map/* <String,String> */substitutes;

    public ParserSettingsMonitor(ParserSettings settings) {
        this.delegatedSettings = settings;
        this.substitutes = new HashMap();
    }

    /**
     * @return The parser settings that must be used in place of the orignal settings The returned
     *         object delegates all the call to the original settings.
     */
    public ParserSettings getMonitoredSettings() {
        return monitoredSettings;
    }

    /**
     * Free the ressource used during the monitoring, keeping only the info required to evaluate
     * hasChanged.
     */
    public void endMonitoring() {
        monitoredSettings = null;
        delegatedSettings = null;
    }

    /**
     * Check if the newSettings is compatible with the original settings that has been monitored.
     * Only the info that was actually used is compared.
     */
    public boolean hasChanged(ParserSettings newSettings) {
        for (Iterator it = substitutes.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            Object oldValue = entry.getValue();
            String newValue = newSettings.substitute(key);
            if (!oldValue.equals(newValue)) {
                Message.debug("settings variable has changed for : " + entry.getKey());
                return true;
            }
        }
        return false;
    }

    private ParserSettings monitoredSettings = new ParserSettings() {

        public ConflictManager getConflictManager(String name) {
            return delegatedSettings.getConflictManager(name);
        }

        public PatternMatcher getMatcher(String matcherName) {
            return delegatedSettings.getMatcher(matcherName);
        }

        public Namespace getNamespace(String namespace) {
            return delegatedSettings.getNamespace(namespace);
        }

        public RelativeUrlResolver getRelativeUrlResolver() {
            return delegatedSettings.getRelativeUrlResolver();
        }

        public ResolutionCacheManager getResolutionCacheManager() {
            return delegatedSettings.getResolutionCacheManager();
        }

        public DependencyResolver getResolver(ModuleRevisionId mRevId) {
            return delegatedSettings.getResolver(mRevId);
        }

        public StatusManager getStatusManager() {
            return delegatedSettings.getStatusManager();
        }

        public File resolveFile(String filename) {
            return delegatedSettings.resolveFile(filename);
        }

        public String getDefaultBranch(ModuleId moduleId) {
            return delegatedSettings.getDefaultBranch(moduleId);
        }

        public Namespace getContextNamespace() {
            return delegatedSettings.getContextNamespace();
        }

        public Map substitute(Map strings) {
            Map substituted = new LinkedHashMap();
            for (Iterator it = strings.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                substituted.put(entry.getKey(), substitute((String) entry.getValue()));
            }
            return substituted;
        }

        public String substitute(String value) {
            String r = delegatedSettings.substitute(value);
            if (value != null && value != r) {
                substitutes.put(value, r);
            }
            return r;
        }

        public String getVariable(String value) {
            return delegatedSettings.getVariable(value);
        }
    };
}
