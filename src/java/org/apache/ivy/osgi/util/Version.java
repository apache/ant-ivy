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
public class Version implements Comparable<Version> {

    private int major;

    private int minor;

    private int patch;

    private String qualifier;

    private String version;

    private String input;

    private boolean splitted = false;

    private boolean toString = false;

    public Version(String versionStr, String qualifier) throws ParseException {
        this(qualifier == null ? versionStr : (versionStr + "." + qualifier));
    }

    public Version(String versionStr) throws ParseException {
        this.input = versionStr;
        splitted = false;
        toString = false;
    }

    public Version(int major, int minor, int patch, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.qualifier = qualifier;
        splitted = true;
        toString = false;
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
        splitted = true;
        toString = false;
    }

    private void ensureSplitted() {
        if (!splitted) {
            synchronized (this) {
                if (splitted) {
                    return;
                }
                String[] splits = input.split("\\.");
                if (splits == null || splits.length == 0 || splits.length > 4) {
                    throw new RuntimeException(new ParseException("Ill formed OSGi version", 0));
                }
                try {
                    major = Integer.parseInt(splits[0]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException(new ParseException(
                            "Major part of an OSGi version should be an integer", 0));
                }
                try {
                    minor = splits.length >= 2 ? Integer.parseInt(splits[1]) : 0;
                } catch (NumberFormatException e) {
                    throw new RuntimeException(new ParseException(
                            "Minor part of an OSGi version should be an integer", 0));
                }
                try {
                    patch = splits.length >= 3 ? Integer.parseInt(splits[2]) : 0;
                } catch (NumberFormatException e) {
                    throw new RuntimeException(new ParseException(
                            "Patch part of an OSGi version should be an integer", 0));
                }
                qualifier = splits.length == 4 ? splits[3] : null;
                splitted = true;
            }
        }
    }

    private void ensureToString() {
        if (!toString) {
            synchronized (this) {
                if (toString) {
                    return;
                }
                ensureSplitted();
                version = major + "." + minor + "." + patch
                        + (qualifier == null ? "" : "." + qualifier);
                toString = true;
            }
        }
    }

    public String toString() {
        ensureToString();
        return version;
    }

    public int hashCode() {
        ensureSplitted();
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
        ensureSplitted();
        other.ensureSplitted();
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

    public Version withNudgedPatch() {
        ensureSplitted();
        return new Version(major, minor, patch + 1, null);
    }

    public Version withoutQualifier() {
        ensureSplitted();
        return new Version(major, minor, patch, null);
    }

    public String qualifier() {
        ensureSplitted();
        return qualifier == null ? "" : qualifier;
    }

    public int compareUnqualified(Version other) {
        ensureSplitted();
        other.ensureSplitted();
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
        ensureSplitted();
        other.ensureSplitted();
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
