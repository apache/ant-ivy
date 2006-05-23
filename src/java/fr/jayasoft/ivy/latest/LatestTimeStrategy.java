/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Comparator;

import fr.jayasoft.ivy.ArtifactInfo;


public class LatestTimeStrategy extends ComparatorLatestStrategy {
    private static Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            long d1 = ((ArtifactInfo)o1).getLastModified();
            long d2 = ((ArtifactInfo)o2).getLastModified();
            return new Long(d1).compareTo(new Long(d2));
        }
    
    };
    public LatestTimeStrategy() {
    	super(COMPARATOR);
        setName("latest-time");
    }
}
