/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

/**
 * @author Xavier Hanin
 *
 */
public interface PublishingDependencyRevisionResolver {

    /**
     * Returns the revision of the dependency for the publishing of the 'published' module 
     * in 'publishedStatus' status.
     * @param published
     * @param publishedStatus
     * @param dependency
     * @return the revision of the dependency
     */
    String resolve(ModuleDescriptor published, String publishedStatus, ModuleRevisionId depMrid, String status);

}
