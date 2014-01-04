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
package org.apache.ivy.osgi.repo;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;

public class EditableRepoDescriptor extends RepoDescriptor {

    private final Map<String, Map<String, Set<ModuleDescriptorWrapper>>> moduleByCapabilities = new HashMap<String, Map<String, Set<ModuleDescriptorWrapper>>>();

    private final Set<ModuleDescriptorWrapper> modules = new HashSet<ModuleDescriptorWrapper>();

    private final ExecutionEnvironmentProfileProvider profileProvider;

    private final URI baseUri;

    private int logLevel = Message.MSG_INFO;

    public EditableRepoDescriptor(URI baseUri, ExecutionEnvironmentProfileProvider profileProvider) {
        this.baseUri = baseUri;
        this.profileProvider = profileProvider;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public Iterator<ModuleDescriptorWrapper> getModules() {
        return modules.iterator();
    }

    public Set<String> getCapabilities() {
        return moduleByCapabilities.keySet();
    }

    public Set<ModuleDescriptorWrapper> findModules(String requirement, String value) {
        Map<String, Set<ModuleDescriptorWrapper>> modules = moduleByCapabilities.get(requirement);
        if (modules == null) {
            return null;
        }
        return modules.get(value);
    }

    public ModuleDescriptorWrapper findModule(String symbolicName, Version version) {
        Set<ModuleDescriptorWrapper> modules = findModules(BundleInfo.BUNDLE_TYPE, symbolicName);
        if (modules == null) {
            return null;
        }
        for (ModuleDescriptorWrapper module : modules) {
            if (module.getBundleInfo().getVersion().equals(version)) {
                return module;
            }
        }
        return null;
    }

    public Set<String> getCapabilityValues(String capabilityName) {
        Map<String, Set<ModuleDescriptorWrapper>> modules = moduleByCapabilities
                .get(capabilityName);
        if (modules == null) {
            return Collections.emptySet();
        }
        return modules.keySet();
    }

    private void add(String type, String value, ModuleDescriptorWrapper md) {
        modules.add(md);
        Map<String, Set<ModuleDescriptorWrapper>> map = moduleByCapabilities.get(type);
        if (map == null) {
            map = new HashMap<String, Set<ModuleDescriptorWrapper>>();
            moduleByCapabilities.put(type, map);
        }
        Set<ModuleDescriptorWrapper> bundleReferences = map.get(value);
        if (bundleReferences == null) {
            bundleReferences = new HashSet<ModuleDescriptorWrapper>();
            map.put(value, bundleReferences);
        }
        if (!bundleReferences.add(md)) {
            if (logLevel <= Message.MSG_DEBUG) {
                Message.debug("Duplicate module in the repo " + baseUri + " for " + type + " "
                        + value + ": " + md.getBundleInfo().getSymbolicName() + "#"
                        + md.getBundleInfo().getVersion());
            }
        }
    }

    public void addBundle(BundleInfo bundleInfo) {
        ModuleDescriptorWrapper module = findModule(bundleInfo.getSymbolicName(),
            bundleInfo.getVersion());
        if (module != null) {
            Message.debug("Duplicate module " + bundleInfo.getSymbolicName() + "@"
                    + bundleInfo.getVersion());
            return;
        }
        ModuleDescriptorWrapper md = new ModuleDescriptorWrapper(bundleInfo, baseUri,
                profileProvider);
        add(BundleInfo.BUNDLE_TYPE, bundleInfo.getSymbolicName(), md);
        for (BundleCapability capability : bundleInfo.getCapabilities()) {
            add(capability.getType(), capability.getName(), md);
        }
    }

    @Override
    public String toString() {
        return modules.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modules == null) ? 0 : modules.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EditableRepoDescriptor other = (EditableRepoDescriptor) obj;
        if (modules == null) {
            if (other.modules != null) {
                return false;
            }
        } else if (!modules.equals(other.modules)) {
            return false;
        }
        return true;
    }

}
