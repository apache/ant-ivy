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

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactTokens {

    public static final Comparator/* <ArtifactTokens> */ORDER_BY_VERSION_ASC = new OrderByVersion(
            true);

    public static final Comparator/* <ArtifactTokens> */ORDER_BY_VERSION_DESC = new OrderByVersion(
            false);

    protected static final Pattern ARTIFACT_TOKEN_REGEX = Pattern
            .compile("(.*/)?([\\w\\.]+)[\\-_](\\d*\\.\\d*\\.\\d*)[\\.]?([\\w\\-]+)?");

    public final String prefix;

    public final String module;

    public final Version version;

    public final boolean isJar;

    public ArtifactTokens(String artifactStr) {
        isJar = artifactStr.endsWith(".jar");
        artifactStr = (isJar ? artifactStr.substring(0, artifactStr.length() - 4) : artifactStr);
        final Matcher matcher = ARTIFACT_TOKEN_REGEX.matcher(artifactStr);
        if (matcher.matches()) {
            prefix = matcher.group(1);
            module = matcher.group(2);
            version = new Version(matcher.group(3), matcher.group(4));
        } else {
            prefix = null;
            module = null;
            version = null;
        }
    }

    public String toString() {
        return (prefix != null ? prefix : "") + module + "-" + version + (isJar ? ".jar" : "");
    }

    public String toDetailString() {
        return "prefix=" + prefix + ", module=" + module + ", version=" + version + ", isJar="
                + isJar;
    }

    public boolean isValid() {
        return (prefix != null) && (module != null) && (version != null);
    }

    private static class OrderByVersion implements Comparator/* <ArtifactTokens> */{

        private final boolean ascending;

        public OrderByVersion(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(Object o1, Object o2) {
            return compare((ArtifactTokens) o1, (ArtifactTokens) o2);
        }

        public int compare(ArtifactTokens a, ArtifactTokens b) {
            if (!a.module.equalsIgnoreCase(b.module)) {
                throw new IllegalArgumentException("Cannot order different modules by version. A="
                        + a + ", B=" + b);
            }
            final int val = a.version.compareTo(b.version);
            return (ascending ? val : -val);
        }
    }
}
