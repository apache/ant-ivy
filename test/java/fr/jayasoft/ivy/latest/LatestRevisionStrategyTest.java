/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.jayasoft.ivy.ArtifactInfo;

import junit.framework.TestCase;

public class LatestRevisionStrategyTest extends TestCase {
    public void testComparator() {
        ArtifactInfo[] revs = toMockAI(new String[] {
                "0.2a", 
                "0.2_b", 
                "0.2rc1", 
                "0.2-final", 
                "1.0-dev1", 
                "1.0-dev2", 
                "1.0-alpha1", 
                "1.0-alpha2", 
                "1.0-beta1", 
                "1.0-beta2", 
                "1.0-gamma",
                "1.0-rc1",
                "1.0-rc2",
                "1.0", 
                "1.0.1", 
                "2.0" 
                });
        
        List shuffled = new ArrayList(Arrays.asList(revs)); 
        Collections.shuffle(shuffled);
        Collections.sort(shuffled, new LatestRevisionStrategy().COMPARATOR);
        assertEquals(Arrays.asList(revs), shuffled);
    }
    
    public void testSpecialMeaningComparator() {
        ArtifactInfo[] revs = toMockAI(new String[] {
                "0.1", 
                "0.2-pre", 
                "0.2-dev", 
                "0.2-rc1", 
                "0.2-final", 
                "0.2-QA", 
                "1.0-dev1", 
                });
        
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
        Collections.sort(shuffled, latestRevisionStrategy.COMPARATOR);
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
        
    }
    private ArtifactInfo[] toMockAI(String[] revs) {
        ArtifactInfo[] artifactInfos = new ArtifactInfo[revs.length];
        for (int i = 0; i < artifactInfos.length; i++) {
            artifactInfos[i] = new MockArtifactInfo(revs[i], 0);
        }
        return artifactInfos;
    }

}
