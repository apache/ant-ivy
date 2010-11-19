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
package org.apache.ivy.osgi.util;

import static java.lang.Integer.parseInt;

/**
 * Provides OSGi version support.
 */
public class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;
    private final String qualifier;

    public Version(String versionStr, String qualifier) throws NumberFormatException {
        this(versionStr + "." + (qualifier != null ? qualifier : ""));
    }

    public Version(String versionStr) throws NumberFormatException {
        final String[] tmp = versionStr.split("[\\.]");
        major = parseInt(tmp[0]);
        minor = tmp.length >= 2 ? parseInt(tmp[1]) : 0;
        patch = tmp.length >= 3 ? parseInt(tmp[2]) : 0;
        qualifier = tmp.length == 4 ? tmp[3] : null;
    }

    public Version(int major, int minor, int patch, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.qualifier = qualifier;
    }

    @Override
    public String toString() {
        return numbersAsString() + (qualifier == null ? "" : "." + qualifier);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + patch;
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
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
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        if (major != other.major) {
            return false;
        }
        if (minor != other.minor) {
            return false;
        }
        if (patch != other.patch) {
            return false;
        }
        if (qualifier == null) {
            if (other.qualifier != null) {
                return false;
            }
        } else if (!qualifier.equals(other.qualifier)) {
            return false;
        }
        return true;
    }

    public String numbersAsString() {
        return major + "." + minor + "." + patch;
    }

    public Version withNudgedPatch() {
        return new Version(major, minor, patch + 1, null);
    }

    public Version withoutQualifier() {
        return new Version(major, minor, patch, null);
    }

    public String qualifier() {
        return qualifier == null ? "" : qualifier;
    }

    public int compareUnqualified(Version other) {
        int diff = major - other.major;
        if (diff != 0) {
            return diff;
        }
        diff = minor - other.minor;
        if (diff != 0) {
            return diff;
        }
        diff = patch - other.patch;
        if (diff != 0) {
            return diff;
        }
        return 0;
    }

    public int compareTo(Version other) {
        int diff = compareUnqualified(other);
        if (diff != 0) {
            return diff;
        }
        if (qualifier == null) {
            return other.qualifier != null ? -1 : 0;
        }
        if (other.qualifier == null) {
            return 1;
        }
        return qualifier.compareTo(other.qualifier);
    }

}
