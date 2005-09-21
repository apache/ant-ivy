package fr.jayasoft.ivy;


/**
 * Unchecked exception thrown when a circular dependency exists between projects.
 * @author baumkar
 *
 */

public class CircularDependencyException extends RuntimeException {

    /**
     * 
     * @param descriptors module descriptors in order of circular dependency
     */
    public CircularDependencyException(final ModuleDescriptor[] descriptors) {
        super(formatMessage(descriptors));
    }

    /**
     * Returns a string representation of this circular dependency graph
     * @param descriptors in order of circular dependency
     * @return
     */
    private static String formatMessage(final ModuleDescriptor[] descriptors) {
        StringBuffer buff = new StringBuffer();
        buff.append(descriptors[0].getModuleRevisionId());
        for (int i = 1; i < descriptors.length; i++) {
            buff.append("->");
            buff.append(descriptors[i].getModuleRevisionId());
        }
        return buff.toString();
    }
    

}