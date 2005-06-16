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

import junit.framework.TestCase;

public class LatestRevisionStrategyTest extends TestCase {
    public void testComparator() {
        String[] revs = new String[] {
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
                };
        
        List shuffled = new ArrayList(Arrays.asList(revs)); 
        Collections.shuffle(shuffled);
        Collections.sort(shuffled, LatestRevisionStrategy.COMPARATOR);
        assertEquals(Arrays.asList(revs), shuffled);
    }
}
