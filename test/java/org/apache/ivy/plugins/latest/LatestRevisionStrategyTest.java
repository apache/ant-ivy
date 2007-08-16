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
package org.apache.ivy.plugins.latest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

public class LatestRevisionStrategyTest extends TestCase {
    public void testComparator() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2a", "0.2_b", "0.2rc1", "0.2-final",
                "1.0-dev1", "1.0-dev2", "1.0-alpha1", "1.0-alpha2", "1.0-beta1", "1.0-beta2",
                "1.0-gamma", "1.0-rc1", "1.0-rc2", "1.0", "1.0.1", "2.0"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        Collections.shuffle(shuffled);
        Collections.sort(shuffled, new LatestRevisionStrategy().new ArtifactInfoComparator());
        assertEquals(Arrays.asList(revs), shuffled);
    }

    public void testSort() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2a", "0.2_b", "0.2rc1", "0.2-final",
                "1.0-dev1", "1.0-dev2", "1.0-alpha1", "1.0-alpha2", "1.0-beta1", "1.0-beta2",
                "1.0-gamma", "1.0-rc1", "1.0-rc2", "1.0", "1.0.1", "2.0"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        ArtifactInfo[] shuffledRevs = (ArtifactInfo[]) shuffled
                .toArray(new ArtifactInfo[revs.length]);

        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        List sorted = latestRevisionStrategy.sort(shuffledRevs);
        assertEquals(Arrays.asList(revs), sorted);
    }

    public void testFindLatest() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.2a", "0.2_b", "0.2rc1", "0.2-final",
                "1.0-dev1", "1.0-dev2", "1.0-alpha1", "1.0-alpha2", "1.0-beta1", "1.0-beta2",
                "1.0-gamma", "1.0-rc1", "1.0-rc2", "1.0", "1.0.1", "2.0"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        ArtifactInfo[] shuffledRevs = (ArtifactInfo[]) shuffled
                .toArray(new ArtifactInfo[revs.length]);

        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        ArtifactInfo latest = latestRevisionStrategy.findLatest(shuffledRevs, new Date());
        assertNotNull(latest);
        assertEquals("2.0", latest.getRevision());
    }

    public void testSpecialMeaningComparator() {
        ArtifactInfo[] revs = toMockAI(new String[] {"0.1", "0.2-pre", "0.2-dev", "0.2-rc1",
                "0.2-final", "0.2-QA", "1.0-dev1"});

        List shuffled = new ArrayList(Arrays.asList(revs));
        Collections.shuffle(shuffled);
        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        LatestRevisionStrategy.SpecialMeaning specialMeaning = new LatestRevisionStrategy.SpecialMeaning();
        specialMeaning.setName("pre");
        specialMeaning.setValue(new Integer(-2));
        latestRevisionStrategy.addConfiguredSpecialMeaning(specialMeaning);
        specialMeaning = new LatestRevisionStrategy.SpecialMeaning();
        specialMeaning.setName("QA");
        specialMeaning.setValue(new Integer(4));
        latestRevisionStrategy.addConfiguredSpecialMeaning(specialMeaning);
        Collections.sort(shuffled, latestRevisionStrategy.new ArtifactInfoComparator());
        assertEquals(Arrays.asList(revs), shuffled);
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
