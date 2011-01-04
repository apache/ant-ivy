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

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;

public class BundleRepo {

    private String name;

    private Long lastModified;

    private final Set/* <BundleInfo> */bundles = new HashSet/* <BundleInfo> */();

    private final Map/* <String, Map<String, Set<BundleCapabilityAndLocation>>> */bundleByCapabilities = new HashMap/*
                                                                                                                     * <
                                                                                                                     * String
                                                                                                                     * ,
                                                                                                                     * Map
                                                                                                                     * <
                                                                                                                     * String
                                                                                                                     * ,
                                                                                                                     * Set
                                                                                                                     * <
                                                                                                                     * BundleCapabilityAndLocation
                                                                                                                     * >>>
                                                                                                                     */();

    public BundleRepo() {
        // default constructor
    }

    public BundleRepo(Iterator/* <ManifestAndLocation> */it) {
        populate(it);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void populate(Iterator/* <ManifestAndLocation> */it) {
        while (it.hasNext()) {
            ManifestAndLocation manifestAndLocation = (ManifestAndLocation) it.next();
            try {
                BundleInfo bundleInfo = ManifestParser.parseManifest(manifestAndLocation
                        .getManifest());
                bundleInfo.setUri(manifestAndLocation.getLocation());
                addBundle(bundleInfo);
            } catch (ParseException e) {
                Message.error("Rejected " + manifestAndLocation.getLocation() + ": "
                        + e.getMessage());
            }
        }
    }

    public void addBundle(BundleInfo bundleInfo) {
        bundles.add(bundleInfo);
        populateCapabilities(BundleInfo.BUNDLE_TYPE, bundleInfo.getSymbolicName(),
            bundleInfo.getVersion(), bundleInfo);
        Iterator itCapability = bundleInfo.getCapabilities().iterator();
        while (itCapability.hasNext()) {
            BundleCapability capability = (BundleCapability) itCapability.next();
            populateCapabilities(capability.getType(), capability.getName(),
                capability.getVersion(), bundleInfo);
        }
    }

    private void populateCapabilities(String type, String n, Version version, BundleInfo bundleInfo) {
        Map/* <String, Set<BundleCapabilityAndLocation>> */map = (Map) bundleByCapabilities
                .get(type);
        if (map == null) {
            map = new HashMap/* <String, Set<BundleCapabilityAndLocation>> */();
            bundleByCapabilities.put(type, map);
        }
        Set/* <BundleCapabilityAndLocation> */bundleReferences = (Set) map.get(n);
        if (bundleReferences == null) {
            bundleReferences = new HashSet/* <BundleCapabilityAndLocation> */();
            map.put(n, bundleReferences);
        }
        if (!bundleReferences.add(new BundleCapabilityAndLocation(type, n, version, bundleInfo))) {
            Message.warn("The repo did already contains " + n + "#" + version);
        }
    }

    public Set/* <BundleInfo> */getBundles() {
        return bundles;
    }

    public Map/* <String, Map<String, Set<BundleCapabilityAndLocation>>> */getBundleByCapabilities() {
        return bundleByCapabilities;
    }

    public String toString() {
        return bundles.toString();
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundles == null) ? 0 : bundles.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BundleRepo)) {
            return false;
        }
        BundleRepo other = (BundleRepo) obj;
        if (bundles == null) {
            if (other.bundles != null) {
                return false;
            }
        } else if (!bundles.equals(other.bundles)) {
            return false;
        }
        return true;
    }

}
