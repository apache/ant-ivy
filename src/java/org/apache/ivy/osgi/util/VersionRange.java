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
 * Provides version range support.
 */
public class VersionRange {

    private boolean startExclusive;

    private Version startVersion;

    private boolean endExclusive;

    private Version endVersion;

    public VersionRange(String versionStr) throws ParseException {
        if (versionStr == null || versionStr.length() == 0) {
            startExclusive = false;
            startVersion = new Version(0, 0, 0, null);
            endExclusive = true;
            endVersion = null;
        } else {
            new VersionRangeParser(versionStr).parse();
        }
    }

    class VersionRangeParser {

        /**
         * value to parse
         */
        private final String version;

        /**
         * the length of the source
         */
        private int length;

        /**
         * position in the source
         */
        private int pos = 0;

        /**
         * last read character
         */
        private char c;

        /**
         * Default constructor
         * 
         * @param header
         *            the header to parse
         */
        VersionRangeParser(String version) {
            this.version = version;
            this.length = version.length();
        }

        /**
         * Do the parsing
         * 
         * @throws ParseException
         */
        void parse() throws ParseException {
            boolean range = parseStart();
            startVersion = parseVersion();
            if (startVersion == null) {
                throw new ParseException("Expecting a number", pos);
            }
            if (parseVersionSeparator()) {
                endVersion = parseVersion();
                parseEnd();
            } else if (range) {
                throw new ParseException("Expecting ,", pos);
            } else {
                // simple number
                endVersion = null;
                startExclusive = false;
                endExclusive = false;
            }
        }

        private char readNext() {
            if (pos == length) {
                c = '\0';
            } else {
                c = version.charAt(pos++);
            }
            return c;
        }

        private void unread() {
            if (pos > 0) {
                pos--;
            }
        }

        private boolean parseStart() {
            skipWhiteSpace();
            switch (readNext()) {
                case '[':
                    startExclusive = false;
                    return true;
                case '(':
                    startExclusive = true;
                    return true;
                default:
                    unread();
                    return false;
            }
        }

        private void skipWhiteSpace() {
            do {
                switch (readNext()) {
                    case ' ':
                        continue;
                    default:
                        unread();
                        return;
                }
            } while (pos < length);
        }

        private Version parseVersion() {
            Integer major = parseNumber();
            if (major == null) {
                return null;
            }
            Integer minor = new Integer(0);
            Integer patch = new Integer(0);
            String qualififer = null;
            if (parseNumberSeparator()) {
                minor = parseNumber();
                if (minor == null) {
                    minor = new Integer(0);
                } else if (parseNumberSeparator()) {
                    patch = parseNumber();
                    if (patch == null) {
                        patch = new Integer(0);
                    } else if (parseNumberSeparator()) {
                        qualififer = parseQualifier();
                    }
                }
            }
            return new Version(major.intValue(), minor.intValue(), patch.intValue(), qualififer);
        }

        private Integer parseNumber() {
            skipWhiteSpace();
            Integer n = null;
            do {
                switch (readNext()) {
                    case '\0':
                        return n;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        n = new Integer((n == null ? 0 : n.intValue() * 10) + c - '0');
                        break;
                    default:
                        unread();
                        return n;
                }
            } while (pos < length);
            return n;
        }

        private boolean parseNumberSeparator() {
            switch (readNext()) {
                case '.':
                    return true;
                default:
                    unread();
                    return false;
            }
        }

        private boolean parseVersionSeparator() {
            skipWhiteSpace();
            switch (readNext()) {
                case ',':
                    return true;
                default:
                    unread();
                    return false;
            }
        }

        private String parseQualifier() {
            StringBuffer q = new StringBuffer();
            do {
                readNext();
                if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                        || c == '-' || c == '_') {
                    q.append(c);
                } else {
                    unread();
                    break;
                }
            } while (pos < length);
            if (q.length() == 0) {
                return null;
            }
            return q.toString();
        }

        private void parseEnd() throws ParseException {
            skipWhiteSpace();
            switch (readNext()) {
                case ']':
                    endExclusive = false;
                    break;
                case ')':
                    endExclusive = true;
                    break;
                default:
                    unread();
                    throw new ParseException("Expexting ] or )", pos);
            }
        }
    }

    public VersionRange(boolean startExclusive, Version startVersion, boolean endExclusive,
            Version endVersion) {
        this.startExclusive = startExclusive;
        this.startVersion = startVersion;
        this.endExclusive = endExclusive;
        this.endVersion = endVersion;
    }

    public VersionRange(Version startVersion) {
        this.startExclusive = false;
        this.startVersion = startVersion;
        this.endExclusive = true;
        this.endVersion = null;
    }

    public String toString() {
        return (startExclusive ? "(" : "[") + startVersion + ","
                + (endVersion == null ? "" : endVersion.toString()) + (endExclusive ? ")" : "]");
    }

    public String toIvyRevision() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(startExclusive ? "(" : "[");
        buffer.append(startVersion);
        if (endVersion == null) {
            buffer.append(",)");
        } else if (!endExclusive || startVersion.equals(endVersion)) {
            buffer.append(",");
            buffer.append(endVersion.withNudgedPatch());
            buffer.append(")");
        } else {
            buffer.append(",");
            buffer.append(endVersion);
            buffer.append(")");
        }
        return buffer.toString();
    }

    public boolean isEndExclusive() {
        return this.endExclusive;
    }

    public Version getEndVersion() {
        return this.endVersion;
    }

    public boolean isStartExclusive() {
        return this.startExclusive;
    }

    public Version getStartVersion() {
        return this.startVersion;
    }

    public boolean isClosedRange() {
        return startVersion.equals(endVersion);
    }

    public boolean contains(String versionStr) throws ParseException {
        return contains(new Version(versionStr));
    }

    public boolean contains(Version version) {
        if (startExclusive ? version.compareUnqualified(startVersion) <= 0 : version
                .compareUnqualified(startVersion) < 0) {
            return false;
        }
        if (endVersion == null) {
            return true;
        }
        if (endExclusive ? version.compareUnqualified(endVersion) >= 0 : version
                .compareUnqualified(endVersion) > 0) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (endExclusive ? 1231 : 1237);
        result = prime * result + ((endVersion == null) ? 0 : endVersion.hashCode());
        result = prime * result + (startExclusive ? 1231 : 1237);
        result = prime * result + ((startVersion == null) ? 0 : startVersion.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof VersionRange)) {
            return false;
        }
        VersionRange other = (VersionRange) obj;
        if (endExclusive != other.endExclusive) {
            return false;
        }
        if (endVersion == null) {
            if (other.endVersion != null) {
                return false;
            }
        } else if (!endVersion.equals(other.endVersion)) {
            return false;
        }
        if (startExclusive != other.startExclusive) {
            return false;
        }
        if (startVersion == null) {
            if (other.startVersion != null) {
                return false;
            }
        } else if (!startVersion.equals(other.startVersion)) {
            return false;
        }
        return true;
    }

}
