/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.ArtifactInfo;


public class LatestRevisionStrategy extends ComparatorLatestStrategy {
    public static class SpecialMeaning {
        private String _name;
        private Integer _value;
        public String getName() {
            return _name;
        }
        public void setName(String name) {
            _name = name;
        }
        public Integer getValue() {
            return _value;
        }
        public void setValue(Integer value) {
            _value = value;
        }
        public void validate() {
            if (_name == null) {
                throw new IllegalStateException("a special meaning should have a name");
            }
            if (_value == null) {
                throw new IllegalStateException("a special meaning should have a value");
            }            
        }
    }

    private static final Map DEFAULT_SPECIAL_MEANINGS;
    static {
        DEFAULT_SPECIAL_MEANINGS = new HashMap();
        DEFAULT_SPECIAL_MEANINGS.put("dev", new Integer(-1));
        DEFAULT_SPECIAL_MEANINGS.put("rc", new Integer(1));
        DEFAULT_SPECIAL_MEANINGS.put("final", new Integer(2));
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
    public Comparator COMPARATOR = new Comparator() {

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
                Map specialMeanings = getSpecialMeanings();
                Integer sm1 = (Integer)specialMeanings.get(parts1[i].toLowerCase());
                Integer sm2 = (Integer)specialMeanings.get(parts2[i].toLowerCase());
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
    
    private Map _specialMeanings = null;
    private boolean _usedefaultspecialmeanings = true;
    
    public LatestRevisionStrategy() {
    	setComparator(COMPARATOR);
        setName("latest-revision");
    }
    
    public void addConfiguredSpecialMeaning(SpecialMeaning meaning) {
        meaning.validate();
        getSpecialMeanings().put(meaning.getName().toLowerCase(), meaning.getValue());
    }

    public synchronized Map getSpecialMeanings() {
        if (_specialMeanings == null) {
            _specialMeanings = new HashMap();
            if (isUsedefaultspecialmeanings()) {
                _specialMeanings.putAll(DEFAULT_SPECIAL_MEANINGS);
            }
        }
        return _specialMeanings;
    }

    public boolean isUsedefaultspecialmeanings() {
        return _usedefaultspecialmeanings;
    }

    public void setUsedefaultspecialmeanings(boolean usedefaultspecialmeanings) {
        _usedefaultspecialmeanings = usedefaultspecialmeanings;
    }
}
