package fr.jayasoft.ivy.latest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import fr.jayasoft.ivy.ArtifactInfo;

public class ComparatorLatestStrategy extends AbstractLatestStrategy {

	private Comparator _comparator;

	public ComparatorLatestStrategy() {
	}

	public ComparatorLatestStrategy(Comparator comparator) {
		_comparator = comparator;
	}

    public ArtifactInfo findLatest(ArtifactInfo[] artifacts, Date date) {
        if (artifacts == null) {
            return null;
        }
        ArtifactInfo found = null;
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactInfo art = artifacts[i];
            if (found == null || _comparator.compare(art, found) > 0) {
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
    
    public List sort(ArtifactInfo[] infos) {
    	List ret = new ArrayList(Arrays.asList(infos));
    	Collections.sort(ret, _comparator);
    	return ret;
    }

	public Comparator getComparator() {
		return _comparator;
	}

	public void setComparator(Comparator comparator) {
		_comparator = comparator;
	}

}
