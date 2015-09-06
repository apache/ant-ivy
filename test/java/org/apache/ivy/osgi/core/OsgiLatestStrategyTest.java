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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.ivy.plugins.latest.ArtifactInfo;

import junit.framework.TestCase;

public class OsgiLatestStrategyTest extends TestCase {

    public void testComparator() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2.0.a", "0.2.0.b", "0.2.0.final", "1.0",
                "1.0.0.gamma", "1.0.0.rc1", "1.0.0.rc2", "1.0.1", "2", "2.0.0.b006", "2.0.0.b012",
                "2.0.0.xyz"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        Collections.shuffle(shuffled);
        Collections.sort(shuffled, new OsgiLatestStrategy().new ArtifactInfoComparator());
        assertEquals(Arrays.asList(revs), shuffled);
    }

    public void testSort() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2.0.a", "0.2.0.b", "0.2.0.final", "1.0",
                "1.0.0.gamma", "1.0.0.rc1", "1.0.0.rc2", "1.0.1", "2", "2.0.0.b006", "2.0.0.b012",
                "2.0.0.xyz"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        ArtifactInfo[] shuffledRevs = (ArtifactInfo[]) shuffled
                .toArray(new ArtifactInfo[revs.length]);

        OsgiLatestStrategy latestRevisionStrategy = new OsgiLatestStrategy();
        List sorted = latestRevisionStrategy.sort(shuffledRevs);
        assertEquals(Arrays.asList(revs), sorted);
    }

    public void testFindLatest() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2.0.a", "0.2.0.b", "0.2.0.rc1",
                "0.2.0.final", "1.0.0.dev1", "1.0.0.dev2", "1.0.0.alpha1", "1.0.0.alpha2",
                "1.0.0.beta1", "1.0.0.beta2", "1.0.0.gamma", "1.0.0.rc1", "1.0.0.rc2", "1.0",
                "1.0.1", "2.0"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        Collections.shuffle(shuffled);
        ArtifactInfo[] shuffledRevs = (ArtifactInfo[]) shuffled
                .toArray(new ArtifactInfo[revs.length]);

        OsgiLatestStrategy latestRevisionStrategy = new OsgiLatestStrategy();
        ArtifactInfo latest = latestRevisionStrategy.findLatest(shuffledRevs, new Date());
        assertNotNull(latest);
        assertEquals("2.0", latest.getRevision());
    }

    private static class MockArtifactInfo implements ArtifactInfo {

        private long _lastModified;

        private String _rev;

        public MockArtifactInfo(String rev, long lastModified) {
            _rev = rev;
            _lastModified = lastModified;
        }

        public String getRevision() {
            return _rev;
        }

        public long getLastModified() {
            return _lastModified;
        }

        public String toString() {
            return _rev;
        }
    }

    private ArtifactInfo[] toMockAI(String[] revs) {
        ArtifactInfo[] artifactInfos = new ArtifactInfo[revs.length];
        for (int i = 0; i < artifactInfos.length; i++) {
            artifactInfos[i] = new MockArtifactInfo(revs[i], 0);
        }
        return artifactInfos;
    }
}