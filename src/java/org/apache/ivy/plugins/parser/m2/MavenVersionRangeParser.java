/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ivy.plugins.parser.m2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * Parser that understands Maven version ranges of the form {@code (,1.0]} and such.
 * More details about such ranges in Maven, can be found
 * {@link https://cwiki.apache.org/confluence/display/MAVENOLD/Dependency+Mediation+and+Conflict+Resolution#DependencyMediationandConflictResolution-DependencyVersionRanges here}
 */
class MavenVersionRangeParser {

    private static final DeweyDecimal javaVersion;

    static {
        DeweyDecimal v = null;
        try {
            v = new DeweyDecimal(System.getProperty("java.specification.version"));
        } catch (Exception e) {
            v = null;
        }
        javaVersion = v;
    }

    /**
     * @param range The range to compare against
     * @return Returns true if the current Java version, in which the instance of this class is running,
     * is within the specified {@code range}. Else returns false.
     */
    static boolean currentJavaVersionInRange(final String range) {
        if (range == null) {
            return false;
        }
        if (javaVersion == null) {
            // this will almost never be the case, but if we couldn't
            // determine the version of Java this system is running on,
            // then there's nothing we can do
            return false;
        }
        final Range parsedRange = parse(range);
        return parsedRange != null && parsedRange.accepts(javaVersion);
    }

    /**
     * @param range The range to compare against
     * @param value The value being compared
     * @return Compares the {@code value} against the {@code range} and returns true if the {@code value}
     * lies within the {@code range}. Else returns false.
     */
    static boolean rangeAccepts(final String range, final String value) {
        if (value == null) {
            return false;
        }
        final DeweyDecimal valToCompare;
        try {
            valToCompare = new DeweyDecimal(value);
        } catch (NumberFormatException nfe) {
            return false;
        }
        final Range parsedRange = parse(range);
        return parsedRange != null && parsedRange.accepts(valToCompare);
    }

    private static Range parse(final String rangeValue) {
        if (rangeValue == null || rangeValue.trim().isEmpty()) {
            return null;
        }
        try {
            // split the version by ","
            final String[] versionParts = rangeValue.split(",");
            if (versionParts.length == 1) {
                final String boundVal = versionParts[0].trim();
                final String stripped = stripBoundChars(boundVal);
                if (stripped.isEmpty()) {
                    return null;
                }
                final DeweyDecimal bound = new DeweyDecimal(stripped);
                return new BasicRange(bound, !boundVal.startsWith("("), bound, !boundVal.endsWith(")"));
            }
            if (versionParts.length == 2) {
                final String lowerBoundVal = versionParts[0].trim();
                final String strippedLowerBound = stripBoundChars(lowerBoundVal);
                final DeweyDecimal lowerBound;
                if (strippedLowerBound.isEmpty()) {
                    lowerBound = null;
                } else {
                    lowerBound = new DeweyDecimal(strippedLowerBound);
                }
                final String upperBoundVal = versionParts[1].trim();
                final String strippedUpperBound = stripBoundChars(upperBoundVal);
                final DeweyDecimal upperBound;
                if (strippedUpperBound.isEmpty()) {
                    upperBound = null;
                } else {
                    upperBound = new DeweyDecimal(strippedUpperBound);
                }
                return new BasicRange(lowerBound, !lowerBoundVal.startsWith("("), upperBound, !upperBoundVal.endsWith(")"));
            }
            if (versionParts.length > 2) {
                // each range part can itself be a range, which is valid in maven
                final Collection<Range> ranges = new ArrayList<>();
                for (int i = 0; i < versionParts.length; i = (i + 2 < versionParts.length) ? i + 2 : i + 1) {
                    final String partOne = versionParts[i];
                    final String partTwo;
                    if (i + 1 < versionParts.length) {
                        partTwo = versionParts[i + 1];
                    } else {
                        partTwo = "";
                    }
                    final Range rangePart = parse(partOne + "," + partTwo);
                    if (rangePart == null) {
                        continue;
                    }
                    ranges.add(rangePart);

                }
                return (ranges == null || ranges.isEmpty()) ? null : new MultiSetRange(ranges);
            }
            return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private static String stripBoundChars(final String value) {
        if (value == null) {
            return null;
        }
        return value.replace("(", "").replace(")", "")
                .replace("[", "").replace("]", "");
    }

    private interface Range {
        boolean accepts(final DeweyDecimal value);
    }

    private static final class BasicRange implements Range {
        private final DeweyDecimal lowerBound;

        private final DeweyDecimal upperBound;

        private final boolean lowerInclusive;

        private final boolean upperInclusive;

        private BasicRange(final DeweyDecimal lowerBound, final boolean lowerInclusive,
                           final DeweyDecimal upperBound, final boolean upperInclusive) {
            this.lowerBound = lowerBound;
            this.lowerInclusive = lowerInclusive;
            this.upperBound = upperBound;
            this.upperInclusive = upperInclusive;
        }

        @Override
        public boolean accepts(final DeweyDecimal value) {
            return value != null
                    && (this.lowerBound == null || (this.lowerInclusive ? value.isGreaterThanOrEqual(lowerBound) : value.isGreaterThan(lowerBound)))
                    && (this.upperBound == null || (this.upperInclusive ? value.isLessThanOrEqual(upperBound) : value.isLessThan(upperBound)));
        }
    }

    private static final class MultiSetRange implements Range {

        private final Collection<Range> ranges;

        private MultiSetRange(final Collection<Range> ranges) {
            this.ranges = ranges == null ? Collections.<Range>emptySet() : ranges;
        }

        @Override
        public boolean accepts(final DeweyDecimal value) {
            if (this.ranges.isEmpty()) {
                return false;
            }
            for (final Range range : this.ranges) {
                if (range == null) {
                    continue;
                }
                // if any range matches, we consider it a match
                if (range.accepts(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    // This class is a copy of the one in Ant project, but since Ivy *core* module
    // (intentionally) doesn't rely on Ant, we use a copied version here
    private static final class DeweyDecimal {

        /**
         * Array of components that make up DeweyDecimal
         */
        private final int[] components;

        /**
         * Construct a DeweyDecimal from an array of integer components.
         *
         * @param components an array of integer components.
         */
        @SuppressWarnings("unused")
        public DeweyDecimal(final int[] components) {
            this.components = new int[components.length];
            System.arraycopy(components, 0, this.components, 0, components.length);
        }

        /**
         * Construct a DeweyDecimal from string in DeweyDecimal format.
         *
         * @param string the string in dewey decimal format
         * @throws NumberFormatException if string is malformed
         */
        public DeweyDecimal(final String string)
                throws NumberFormatException {
            final StringTokenizer tokenizer = new StringTokenizer(string, ".", true);
            final int size = tokenizer.countTokens();

            components = new int[(size + 1) / 2];

            for (int i = 0; i < components.length; i++) {
                final String component = tokenizer.nextToken();
                if (component.length() == 0) {
                    throw new NumberFormatException("Empty component in string");
                }

                components[i] = Integer.parseInt(component);

                // Strip '.' token
                if (tokenizer.hasMoreTokens()) {
                    tokenizer.nextToken();

                    // If it ended in a dot, throw an exception
                    if (!tokenizer.hasMoreTokens()) {
                        throw new NumberFormatException("DeweyDecimal ended in a '.'");
                    }
                }
            }
        }

        /**
         * Return number of components in <code>DeweyDecimal</code>.
         *
         * @return the number of components in dewey decimal
         */
        @SuppressWarnings("unused")
        public int getSize() {
            return components.length;
        }

        /**
         * Return the component at specified index.
         *
         * @param index the index of components
         * @return the value of component at index
         */
        @SuppressWarnings("unused")
        public int get(final int index) {
            return components[index];
        }

        /**
         * Return <code>true</code> if this <code>DeweyDecimal</code> is
         * equal to the other <code>DeweyDecimal</code>.
         *
         * @param other the other DeweyDecimal
         * @return true if equal to other DeweyDecimal, false otherwise
         */
        public boolean isEqual(final DeweyDecimal other) {
            final int max = Math.max(other.components.length, components.length);

            for (int i = 0; i < max; i++) {
                final int component1 = (i < components.length) ? components[i] : 0;
                final int component2 = (i < other.components.length) ? other.components[i] : 0;

                if (component2 != component1) {
                    return false;
                }
            }

            return true; // Exact match
        }

        /**
         * Return <code>true</code> if this <code>DeweyDecimal</code> is
         * less than the other <code>DeweyDecimal</code>.
         *
         * @param other the other DeweyDecimal
         * @return true if less than other DeweyDecimal, false otherwise
         */
        public boolean isLessThan(final DeweyDecimal other) {
            return !isGreaterThanOrEqual(other);
        }

        /**
         * Return <code>true</code> if this <code>DeweyDecimal</code> is
         * less than or equal to the other <code>DeweyDecimal</code>.
         *
         * @param other the other DeweyDecimal
         * @return true if less than or equal to other DeweyDecimal, false otherwise
         */
        public boolean isLessThanOrEqual(final DeweyDecimal other) {
            return !isGreaterThan(other);
        }

        /**
         * Return <code>true</code> if this <code>DeweyDecimal</code> is
         * greater than the other <code>DeweyDecimal</code>.
         *
         * @param other the other DeweyDecimal
         * @return true if greater than other DeweyDecimal, false otherwise
         */
        public boolean isGreaterThan(final DeweyDecimal other) {
            final int max = Math.max(other.components.length, components.length);

            for (int i = 0; i < max; i++) {
                final int component1 = (i < components.length) ? components[i] : 0;
                final int component2 = (i < other.components.length) ? other.components[i] : 0;

                if (component2 > component1) {
                    return false;
                }
                if (component2 < component1) {
                    return true;
                }
            }

            return false; // Exact match
        }

        /**
         * Return <code>true</code> if this <code>DeweyDecimal</code> is
         * greater than or equal to the other <code>DeweyDecimal</code>.
         *
         * @param other the other DeweyDecimal
         * @return true if greater than or equal to other DeweyDecimal, false otherwise
         */
        public boolean isGreaterThanOrEqual(final DeweyDecimal other) {
            final int max = Math.max(other.components.length, components.length);

            for (int i = 0; i < max; i++) {
                final int component1 = (i < components.length) ? components[i] : 0;
                final int component2 = (i < other.components.length) ? other.components[i] : 0;

                if (component2 > component1) {
                    return false;
                }
                if (component2 < component1) {
                    return true;
                }
            }

            return true; // Exact match
        }

        /**
         * Return string representation of <code>DeweyDecimal</code>.
         *
         * @return the string representation of DeweyDecimal.
         */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            for (int component : components) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(component);
            }

            return sb.toString();
        }

        /**
         * Compares this DeweyDecimal with another one.
         *
         * @param other another DeweyDecimal to compare with
         * @return result
         * @see java.lang.Comparable#compareTo(Object)
         */
        @SuppressWarnings("unused")
        public int compareTo(DeweyDecimal other) {
            final int max = Math.max(other.components.length, components.length);
            for (int i = 0; i < max; i++) {
                final int component1 = (i < components.length) ? components[i] : 0;
                final int component2 = (i < other.components.length) ? other.components[i] : 0;
                if (component1 != component2) {
                    return component1 - component2;
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DeweyDecimal && isEqual((DeweyDecimal) o);
        }
    }
}
