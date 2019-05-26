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

package org.apache.ivy.plugins.version;

import org.apache.ivy.core.module.id.ModuleRevisionId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ivy.util.StringUtils.isNullOrEmpty;

/**
 * A {@link VersionMatcher} which understands {@code Maven timestamped snapshots}.
 */
public class MavenTimedSnapshotVersionMatcher extends AbstractVersionMatcher {

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    // The timestamped snapshot pattern that Maven uses
    private static final Pattern M2_TIMESTAMPED_SNAPSHOT_REV_PATTERN = Pattern.compile("^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$");

    public MavenTimedSnapshotVersionMatcher() {
        super("maven-timed-snapshot");
    }

    @Override
    public boolean isDynamic(final ModuleRevisionId askedMrid) {
        if (askedMrid == null) {
            return false;
        }
        // we consider only timestamped snapshots as dynamic, since unlike regular snapshots,
        // a timestamped snapshot version of the form x.y.z-<timestamped-part> represents the real
        // x.y.z-SNAPSHOT version
        final Matcher snapshotPatternMatcher = M2_TIMESTAMPED_SNAPSHOT_REV_PATTERN.matcher(askedMrid.getRevision());
        return snapshotPatternMatcher.matches();
    }

    @Override
    public boolean accept(final ModuleRevisionId askedMrid, final ModuleRevisionId foundMrid) {
        if (askedMrid == null || foundMrid == null) {
            return false;
        }
        final MavenSnapshotRevision askedSnapshotVersion = computeIfSnapshot(askedMrid.getRevision());
        if (askedSnapshotVersion == null) {
            // this isn't a snapshot, so we aren't interested in it
            return false;
        }
        // this version matcher only comes into picture if we have been asked to deal with a
        // timestamped snapshot. In other words, if the asked version isn't a timestamped snapshot,
        // then we don't accept it
        if (!askedSnapshotVersion.isTimestampedSnapshot()) {
            return false;
        }
        final MavenSnapshotRevision foundSnapshotVersion = computeIfSnapshot(foundMrid.getRevision());
        if (foundSnapshotVersion == null) {
            // this isn't a snapshot, so we aren't interested in it
            return false;
        }
        // we compare the base revisions of both these snapshot to see if they are the same revision
        // and if they are then we accept the "found" MRID for the "asked" MRID
        return askedSnapshotVersion.baseRevision.equals(foundSnapshotVersion.baseRevision);
    }

    /**
     * Parses the passed {@code revision} and returns a {@link MavenSnapshotRevision}, representing
     * that {@code revision}, if it is either a regular snapshot (for example: 1.0.2-SNAPSHOT) or a
     * timestamped snapshot (for example: 1.0.2-20100925.223013-19).
     * If the passed {@code revision} isn't a snapshot revision, then this method returns null
     *
     * @param revision The revision to parse
     * @return MavenSnapshotRevision
     */
    public static MavenSnapshotRevision computeIfSnapshot(final String revision) {
        if (isNullOrEmpty(revision)) {
            return null;
        }
        final boolean regularSnapshot = revision.endsWith(SNAPSHOT_SUFFIX);
        final Matcher snapshotPatternMatcher = M2_TIMESTAMPED_SNAPSHOT_REV_PATTERN.matcher(revision);
        final boolean timestampedSnaphost = snapshotPatternMatcher.matches();
        if (!regularSnapshot && !timestampedSnaphost) {
            // neither a regular snapshot nor a timestamped snapshot
            return null;
        }
        // the revision is now identified as a snapshot (either a regular one or a timestamped one)
        return timestampedSnaphost ? new MavenSnapshotRevision(true, revision, snapshotPatternMatcher.group(1))
                : new MavenSnapshotRevision(false, revision, revision.substring(0, revision.indexOf(SNAPSHOT_SUFFIX)));
    }


    /**
     * Represents a Maven 2 snapshot version, which is either a regular snapshot
     * (for example: 1.0.2-SNAPSHOT) or a timestamped snapshot (for example:
     * 1.0.2-20100925.223013-19)
     */
    public static final class MavenSnapshotRevision {

        private final boolean timedsnapshot;
        private final String wholeRevision;
        private final String baseRevision;

        private MavenSnapshotRevision(final boolean timedsnapshot, final String wholeRevision, final String baseRevision) {
            if (wholeRevision == null) {
                throw new IllegalArgumentException("Revision, of a Maven snapshot, cannot be null");
            }
            if (baseRevision == null) {
                throw new IllegalArgumentException("Base revision, of a Maven snapshot revision, cannot be null");
            }
            this.timedsnapshot = timedsnapshot;
            this.wholeRevision = wholeRevision;
            this.baseRevision = baseRevision;
        }

        /**
         * Returns true if this {@link MavenSnapshotRevision} represents a timestamped snapshot
         * version. Else returns false.
         *
         * @return boolean
         */
        public boolean isTimestampedSnapshot() {
            return this.timedsnapshot;
        }

        /**
         * Returns the "base" revision that this {@link MavenSnapshotRevision} represents. For
         * example, for the regular snapshot revision {@code 1.2.3-SNAPSHOT}, the base revision
         * is {@code 1.2.3}. Similarly for timestamped snapshot version
         * {@code 1.0.2-20100925.223013-19}, the base revision is {@code 1.0.2}
         *
         * @return String
         */
        public String getBaseRevision() {
            return this.baseRevision;
        }

        /**
         * Returns the complete/whole revision this {@link MavenSnapshotRevision} represents. For
         * example, if this {@link MavenSnapshotRevision} represents a regular snapshot
         * {@code 1.3.4-SNAPSHOT} revision then this method returns {@code 1.3.4-SNAPSHOT}.
         * Similarly, if this {@link MavenSnapshotRevision} represents a timestamped snapshot
         * {@code 1.0.2-20100925.223013-19} revision, then this method returns
         * {@code 1.0.2-20100925.223013-19}
         *
         * @return String
         */
        public String getRevision() {
            return this.wholeRevision;
        }
    }
}
