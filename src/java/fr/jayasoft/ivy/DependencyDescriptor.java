/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import fr.jayasoft.ivy.extendable.ExtendableItem;
import fr.jayasoft.ivy.namespace.Namespace;


/**
 * @author x.hanin
 *
 */
public interface DependencyDescriptor extends ExtendableItem {
    ModuleId getDependencyId();
    /**
     * Used to indicate that this revision must be used in case of conflicts, independently
     * of conflicts manager. This only works for direct dependencies, and not transitive ones.
     * @return true if this dependency should be used, false if conflicts manager
     * can do its work.
     */
    boolean isForce();
    /**
     * Used to indicate that this dependency is a changing one.
     * A changing dependency in ivy means that the revision may have its artifacts modified
     * without revision change. When new artifacts are published a new ivy file should also
     * be published with a new publication date to indicate to ivy that artifacts have changed and that they 
     * should be downloaded again. 
     * @return true if this dependency is a changing one
     */
    boolean isChanging();
    boolean isTransitive();
    ModuleRevisionId getParentRevisionId();
    ModuleRevisionId getDependencyRevisionId();
    String[] getModuleConfigurations();
    String[] getDependencyConfigurations(String moduleConfiguration, String requestedConfiguration);
    String[] getDependencyConfigurations(String moduleConfiguration);
    String[] getDependencyConfigurations(String[] moduleConfigurations);
    Namespace getNamespace();
    DependencyArtifactDescriptor[] getAllDependencyArtifactsIncludes();
    DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String moduleConfigurations);
    DependencyArtifactDescriptor[] getDependencyArtifactsIncludes(String[] moduleConfigurations);
    DependencyArtifactDescriptor[] getAllDependencyArtifactsExcludes();
    DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String moduleConfigurations);
    DependencyArtifactDescriptor[] getDependencyArtifactsExcludes(String[] moduleConfigurations);
    boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId);
    public boolean canExclude();
	DependencyDescriptor asSystem();
}
