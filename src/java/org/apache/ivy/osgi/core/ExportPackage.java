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
package org.apache.ivy.osgi.core;

import java.util.HashSet;
import java.util.Set;

import org.apache.ivy.osgi.util.Version;

public class ExportPackage extends BundleCapability {

    private final Set<String> uses = new HashSet<String>();

    public ExportPackage(String name, Version version) {
        super(BundleInfo.PACKAGE_TYPE, name, version);
    }

    public void addUse(String pkg) {
        uses.add(pkg);
    }

    public Version getVersion() {
        return super.getVersion() == null ? BundleInfo.DEFAULT_VERSION : super.getVersion();
    }

    public Set<String> getUses() {
        return uses;
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((uses == null) ? 0 : uses.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        ExportPackage other = (ExportPackage) obj;
        if (uses == null) {
            if (other.uses != null) {
                return false;
            }
        } else if (!uses.equals(other.uses)) {
            return false;
        }
        return true;
    }

}