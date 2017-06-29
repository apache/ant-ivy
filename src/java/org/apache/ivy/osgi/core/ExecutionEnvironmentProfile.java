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

import java.util.Set;
import java.util.TreeSet;

public class ExecutionEnvironmentProfile {

    Set<String> pkgNames = new TreeSet<>();

    private final String name;

    public ExecutionEnvironmentProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPkgNames() {
        return pkgNames;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pkgNames == null) ? 0 : pkgNames.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExecutionEnvironmentProfile)) {
            return false;
        }
        ExecutionEnvironmentProfile other = (ExecutionEnvironmentProfile) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (pkgNames == null) {
            if (other.pkgNames != null) {
                return false;
            }
        } else if (!pkgNames.equals(other.pkgNames)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return name + ":" + pkgNames;
    }
}
