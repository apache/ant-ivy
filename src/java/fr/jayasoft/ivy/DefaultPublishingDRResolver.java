/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;


public class DefaultPublishingDRResolver implements PublishingDependencyRevisionResolver {
    public String resolve(ModuleDescriptor published, String publishedStatus, ModuleRevisionId depMrid, String status) {
        return depMrid.getRevision();
    }
}
