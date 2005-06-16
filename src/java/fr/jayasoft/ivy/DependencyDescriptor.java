/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;


/**
 * @author x.hanin
 *
 */
public interface DependencyDescriptor {
    ModuleId getDependencyId();
    /**
     * Used to indicate that this revision must be used in case of conflicts, independently
     * of conflicts manager. This only works for direct dependencies, and not transitive ones.
     * @return true if this dependency should be used, false if conflicts manager
     * can do its work.
     */
    boolean isForce();
    ModuleRevisionId getParentRevisionId();
    ModuleRevisionId getDependencyRevisionId();
    String[] getModuleConfigurations();
    String[] getDependencyConfigurations(String moduleConfiguration);
    String[] getDependencyConfigurations(String[] moduleConfigurations);
    DependencyArtifactDescriptor[] getAllDependencyArtifactsIncludes();
    DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String moduleConfigurations);
    DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String[] moduleConfigurations);
    DependencyArtifactDescriptor[] getAllDependencyArtifactsExcludes();
    DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String moduleConfigurations);
    DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String[] moduleConfigurations);
}
