package fr.jayasoft.ivy.circular;

import fr.jayasoft.ivy.ModuleRevisionId;


/**
 * Unchecked exception thrown when a circular dependency exists between projects.
 * @author baumkar
 *
 */

public class CircularDependencyException extends RuntimeException {

    private ModuleRevisionId[] _mrids;

	/**
     * 
     * @param descriptors module descriptors in order of circular dependency
     */
    public CircularDependencyException(final ModuleRevisionId[] mrids) {
        super(CircularDependencyHelper.formatMessage(mrids));
        _mrids = mrids;
    }
    
    public ModuleRevisionId[] getPath() {
    	return _mrids;
    }
    

}