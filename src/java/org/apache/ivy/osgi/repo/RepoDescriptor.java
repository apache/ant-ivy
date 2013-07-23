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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.util.Message;

public class RepoDescriptor {

    private final Map/* <String, Map<String, Set<ModuleDescriptor>>> */moduleByCapabilities = new HashMap();

    private final List/* <ModuleDescriptor> */modules = new ArrayList();

    private final ExecutionEnvironmentProfileProvider profileProvider;

    private final URI baseUri;

    private int logLevel = Message.MSG_INFO;

    public RepoDescriptor(URI baseUri, ExecutionEnvironmentProfileProvider profileProvider) {
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

    public Iterator/* <ModuleDescriptorWrapper> */getModules() {
        return modules.iterator();
    }

    public Set/* <String> */getCapabilities() {
        return moduleByCapabilities.keySet();
    }

    public Set/* <ModuleDescriptorWrapper> */findModule(String requirement, String value) {
        Map/* <String, Set<ModuleDescriptorWrapper>> */modules = (Map) moduleByCapabilities
                .get(requirement);
        if (modules == null) {
            return null;
        }
        return (Set) modules.get(value);
    }

    public Set/* <String> */getCapabilityValues(String capabilityName) {
        Map/* <String, Set<ModuleDescriptorWrapper>> */modules = (Map) moduleByCapabilities
                .get(capabilityName);
        if (modules == null) {
            return Collections.EMPTY_SET;
        }
        return modules.keySet();
    }

    public void add(String type, String value, ModuleDescriptorWrapper md) {
        modules.add(md);
        Map/* <String, Set<ModuleDescriptorWrapper>> */map = (Map) moduleByCapabilities.get(type);
        if (map == null) {
            map = new HashMap/* <String, Set<ModuleDescriptorWrapper>> */();
            moduleByCapabilities.put(type, map);
        }
        Set/* <ModuleDescriptorWrapper> */bundleReferences = (Set) map.get(value);
        if (bundleReferences == null) {
            bundleReferences = new HashSet/* <ModuleDescriptorWrapper> */();
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
        ModuleDescriptorWrapper md = new ModuleDescriptorWrapper(bundleInfo, baseUri,
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
