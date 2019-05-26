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
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MavenTimedSnapshotVersionMatcher}
 */
public class MavenTimedSnapshotVersionMatcherTest {

    /**
     * Tests the {@link MavenTimedSnapshotVersionMatcher#isDynamic(ModuleRevisionId)} method
     */
    @Test
    public void testIsDynamic() {
        final MavenTimedSnapshotVersionMatcher versionMatcher = new MavenTimedSnapshotVersionMatcher();
        final ModuleRevisionId regularSnapshot = ModuleRevisionId.newInstance("org.apache", "ant-ivy", "1.0.2-SNAPSHOT");
        assertFalse(regularSnapshot + " wasn't expected to be a dynamic revision", versionMatcher.isDynamic(regularSnapshot));
        final ModuleRevisionId timestampedSnapshot = ModuleRevisionId.newInstance("org.apache", "ant-ivy", "1.0.2-20100925.223013-19");
        assertTrue(timestampedSnapshot + " was expected to be a dynamic revision", versionMatcher.isDynamic(timestampedSnapshot));
        final ModuleRevisionId exactRevision = ModuleRevisionId.newInstance("org.apache", "ant-ivy", "2.4.0");
        assertFalse(exactRevision + " wasn't expected to be a dynamic revision", versionMatcher.isDynamic(exactRevision));

    }

    /**
     * Tests the {@link MavenTimedSnapshotVersionMatcher#computeIfSnapshot(String)} method
     */
    @Test
    public void testSnapshotParsing() {
        assertNull("Revision wasn't expected to be a snapshot",
            MavenTimedSnapshotVersionMatcher.computeIfSnapshot("1.9.9"));

        final String regularSnapshot = "1.9.9-SNAPSHOT";
        final MavenTimedSnapshotVersionMatcher.MavenSnapshotRevision snapshotRevision = MavenTimedSnapshotVersionMatcher.computeIfSnapshot(regularSnapshot);
        assertNotNull(regularSnapshot + " was expected to be a snapshot", snapshotRevision);
        assertFalse(regularSnapshot + " wasn't expected to be a timestamped snapshot",
            snapshotRevision.isTimestampedSnapshot());

        final String timestampedRev = "21.03.22-20150925.223013-232";
        final MavenTimedSnapshotVersionMatcher.MavenSnapshotRevision timestampedSnapshot = MavenTimedSnapshotVersionMatcher.computeIfSnapshot(timestampedRev);
        assertNotNull(timestampedRev + " was expected to be a snapshot", timestampedSnapshot);
        assertTrue(timestampedRev + " was expected to be a timestamped snapshot", timestampedSnapshot.isTimestampedSnapshot());

        final String exactRevision = "21.2.2-a20140204.232421-2";
        assertNull(exactRevision + " wasn't expected to be a snapshot",
            MavenTimedSnapshotVersionMatcher.computeIfSnapshot(exactRevision));
    }
}
