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

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.util.Version;

public class BundleCapabilityAndLocation {

    private final String name;

    private final Version version;

    private final BundleInfo bundleInfo;

    private final String type;

    public BundleCapabilityAndLocation(String type, String name, Version version,
            BundleInfo bundleInfo) {
        this.type = type;
        this.name = name;
        this.version = version;
        this.bundleInfo = bundleInfo;
    }

    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Version getVersion() {
        return version;
    }

}