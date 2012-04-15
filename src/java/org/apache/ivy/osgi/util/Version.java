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

import java.text.ParseException;

/**
 * Provides OSGi version support.
 */
public class Version implements Comparable/* <Version> */{

    private final int major;

    private final int minor;

    private final int patch;

    private final String qualifier;

    public Version(String versionStr, String qualifier) throws ParseException {
        this(versionStr + "." + (qualifier != null ? qualifier : ""));
    }

    public Version(String versionStr) throws ParseException {
        String[] splits = versionStr.split("\\.");
        if (splits == null || splits.length == 0 || splits.length > 4) {
            throw new ParseException("Ill formed OSGi version", 0);
        }
        try {
            major = Integer.parseInt(splits[0]);
        } catch (NumberFormatException e) {
            throw new ParseException("Major part of an OSGi version should be an integer", 0);
        }
        try {
            minor = splits.length >= 2 ? Integer.parseInt(splits[1]) : 0;
        } catch (NumberFormatException e) {
            throw new ParseException("Minor part of an OSGi version should be an integer", 0);
        }
        try {
            patch = splits.length >= 3 ? Integer.parseInt(splits[2]) : 0;
        } catch (NumberFormatException e) {
            throw new ParseException("Patch part of an OSGi version should be an integer", 0);
        }
        qualifier = splits.length == 4 ? splits[3] : null;
    }

    public Version(int major, int minor, int patch, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.qualifier = qualifier;
    }

    /**
     * Build a version from another one while appending an extra qualifier
     * 
     * @param baseVersion
     * @param qualifier
     */
    public Version(Version baseVersion, String extraQualifier) {
        this.major = baseVersion.major;
        this.minor = baseVersion.minor;
        this.patch = baseVersion.patch;
        this.qualifier = baseVersion.qualifier == null ? extraQualifier
                : (baseVersion.qualifier + extraQualifier);
    }

    public String toString() {
        return numbersAsString() + (qualifier == null ? "" : "." + qualifier);
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + patch;
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
        return result;
    }

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

    public int compareTo(Object obj) {
        return compareTo((Version) obj);
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
