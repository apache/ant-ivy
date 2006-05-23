/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Comparator;

import fr.jayasoft.ivy.ArtifactInfo;


public class LatestLexicographicStrategy extends ComparatorLatestStrategy {
    /**
     * Compares two revisions.
     * Revisions are compared lexicographically unless
     * a 'latest' revision is found. If the latest revision found
     * is an absolute latest (latest. like), then it is assumed to be the greater.
     * If a partial latest is found, then it is assumed to be greater
     * than any matching fixed revision. 
     */ 
    private static Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            String rev1 = ((ArtifactInfo)o1).getRevision();
            String rev2 = ((ArtifactInfo)o2).getRevision();
            if (rev1.startsWith("latest")) {
                return 1;
            }
            if (rev1.endsWith("+") && rev2.startsWith(rev1.substring(0, rev1.length() - 1))) {
                return 1;
            }
            if (rev2.startsWith("latest")) {
                return -1;
            }
            if (rev2.endsWith("+") && rev1.startsWith(rev2.substring(0, rev2.length() - 1))) {
                return -1;
            }
            return rev1.compareTo(rev2);
        }
    
    };
    
    public LatestLexicographicStrategy() {
    	super(COMPARATOR);
        setName("latest-lexico");
    }
    
}
