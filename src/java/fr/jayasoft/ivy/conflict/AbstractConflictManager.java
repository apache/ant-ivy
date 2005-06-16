/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.conflict;

import fr.jayasoft.ivy.ConflictManager;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;

public abstract class AbstractConflictManager implements ConflictManager, IvyAware {
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
}
