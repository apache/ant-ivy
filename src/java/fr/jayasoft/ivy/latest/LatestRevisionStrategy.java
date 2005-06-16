/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.ArtifactInfo;


public class LatestRevisionStrategy extends AbstractLatestStrategy {
    private static final Map SPECIAL_MEANINGS;
    static {
        SPECIAL_MEANINGS = new HashMap();
        SPECIAL_MEANINGS.put("dev", new Integer(-1));
        SPECIAL_MEANINGS.put("rc", new Integer(1));
        SPECIAL_MEANINGS.put("final", new Integer(2));
    }
    
    /**
     * Compares two revisions.
     * Revisions are compared using an algorithm inspired by PHP
     * version_compare one, unless
     * a 'latest' revision is found. If the latest revision found
     * is an absolute latest (latest. like), then it is assumed to be the greater.
     * If a partial latest is found, then it is assumed to be greater
     * than any matching fixed revision. 
     */ 
    public static Comparator COMPARATOR = new Comparator() {

        public int compare(Object o1, Object o2) {
            String rev1 = (String)o1;
            String rev2 = (String)o2;
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
            
            rev1 = rev1.replaceAll("([a-zA-Z])(\\d)", "$1.$2");
            rev1 = rev1.replaceAll("(\\d)([a-zA-Z])", "$1.$2");
            rev2 = rev2.replaceAll("([a-zA-Z])(\\d)", "$1.$2");
            rev2 = rev2.replaceAll("(\\d)([a-zA-Z])", "$1.$2");
            
            String[] parts1 = rev1.split("[\\._\\-\\+]");
            String[] parts2 = rev2.split("[\\._\\-\\+]");
            
            int i = 0;
            for (; i < parts1.length && i <parts2.length; i++) {
                if (parts1[i].equals(parts2[i])) {
                    continue;
                }
                boolean is1Number = isNumber(parts1[i]);
                boolean is2Number = isNumber(parts2[i]);
                if (is1Number && !is2Number) {
                    return 1;
                }
                if (is2Number && !is1Number) {
                    return -1;
                }
                if (is1Number && is2Number) {
                    return Long.valueOf(parts1[i]).compareTo(Long.valueOf(parts2[i]));
                }
                // both are strings, we compare them taking into account special meaning
                Integer sm1 = (Integer)SPECIAL_MEANINGS.get(parts1[i].toLowerCase());
                Integer sm2 = (Integer)SPECIAL_MEANINGS.get(parts2[i].toLowerCase());
                if (sm1 != null) {
                    sm2 = sm2==null?new Integer(0):sm2;
                    return sm1.compareTo(sm2);
                }
                if (sm2 != null) {
                    return new Integer(0).compareTo(sm2);
                }
                return parts1[i].compareTo(parts2[i]);
            }
            if (i < parts1.length) {
                return isNumber(parts1[i])?1:-1;
            }
            if (i < parts2.length) {
                return isNumber(parts2[i])?-1:1;
            }
            return 0;
        }

        private boolean isNumber(String str) {
            return str.matches("\\d+");
        }
    
    };
    public LatestRevisionStrategy() {
        setName("latest-revision");
    }
    
    public ArtifactInfo findLatest(ArtifactInfo[] artifacts, Date date) {
        if (artifacts == null) {
            return null;
        }
        ArtifactInfo found = null;
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactInfo art = artifacts[i];
            if (found == null || COMPARATOR.compare(art.getRevision(), found.getRevision()) > 0) {
                if (date != null) {
                    long lastModified = art.getLastModified();
                    if (lastModified > date.getTime()) {
                        continue;
                    }
                }
                found = art;
            }
        } 
        return found;
    }
}
