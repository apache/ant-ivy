/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.ArtifactInfo;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;
import fr.jayasoft.ivy.LatestStrategy;

public abstract class AbstractLatestStrategy implements LatestStrategy, IvyAware {
    private String _name;
    private Ivy _ivy;

    public Ivy getIvy() {
        return _ivy;
    }    

    public void setIvy(Ivy ivy) {
        _ivy = ivy;
    }

    public String getName() {
        return _name;
    }
    
    public void setName(String name) {
        _name = name;
    }
    
    public String toString() {
        return _name;
    }
    
    public ArtifactInfo findLatest(ArtifactInfo[] infos, Date date) {
    	List l = sort(infos);
    	for (Iterator iter = l.iterator(); iter.hasNext();) {
			ArtifactInfo info = (ArtifactInfo) iter.next();
			if (date == null || info.getLastModified() < date.getTime()) {
				return info;
			}
		}
    	return null;
    }
}
