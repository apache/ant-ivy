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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleInfoAdapter;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.util.Message;

public class RepoDescriptor {

    private final Map/* <String, Map<String, Set<ModuleDescriptor>>> */moduleByCapbilities = new HashMap();

    private final Set/* <ModuleDescriptor> */modules = new HashSet();

    private final ExecutionEnvironmentProfileProvider profileProvider;

    public RepoDescriptor(ExecutionEnvironmentProfileProvider profileProvider) {
        this.profileProvider = profileProvider;
    }

    public Set getModules() {
        return modules;
    }

    public Set/* <ModuleDescriptor> */findModule(String requirement, String value) {
        Map/* <String, Set<ModuleDescriptor>> */modules = (Map) moduleByCapbilities
                .get(requirement);
        if (modules == null) {
            return null;
        }
        return (Set) modules.get(value);
    }

    public Set/* <String> */getCapabilityValues(String capabilityName) {
        Map/* <String, Set<ModuleDescriptor>> */modules = (Map) moduleByCapbilities
                .get(capabilityName);
        if (modules == null) {
            return null;
        }
        return (Set) modules.keySet();
    }

    public void add(String type, String value, ModuleDescriptor md) {
        modules.add(md);
        Map/* <String, Set<ModuleDescriptor>> */map = (Map) moduleByCapbilities.get(type);
        if (map == null) {
            map = new HashMap/* <String, Set<ModuleDescriptor>> */();
            moduleByCapbilities.put(type, map);
        }
        Set/* <ModuleDescriptor> */bundleReferences = (Set) map.get(value);
        if (bundleReferences == null) {
            bundleReferences = new HashSet/* <ModuleDescriptor> */();
            map.put(value, bundleReferences);
        }
        if (!bundleReferences.add(md)) {
            Message.warn("The repo did already contains " + md);
        }
    }

    public void addBundle(BundleInfo bundleInfo) {
        DefaultModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(bundleInfo,
            profileProvider);
        add(BundleInfo.BUNDLE_TYPE, bundleInfo.getSymbolicName(), md);
        Iterator itCapability = bundleInfo.getCapabilities().iterator();
        while (itCapability.hasNext()) {
            BundleCapability capability = (BundleCapability) itCapability.next();
            add(capability.getType(), capability.getName(), md);
        }
    }

    public String toString() {
        return modules.toString();
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modules == null) ? 0 : modules.hashCode());
        return result;
    }

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
        RepoDescriptor other = (RepoDescriptor) obj;
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
