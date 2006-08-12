/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.net.URL;

import fr.jayasoft.ivy.matcher.PatternMatcher;

/**
 * This describes an artifact that is asked for a dependency.
 * 
 * It is used to resctrict the artifacts asked for a dependency, or describe them
 * when there is no ivy file.
 */
public interface DependencyArtifactDescriptor {
    /**
     * Returns the dependency descriptor in which this artifact is asked
     * @return
     */
    public DependencyDescriptor getDependency();

    /**
     * Returns the id of the described artifact, without revision information
     * @return
     */
    public ArtifactId getId();
    /**
     * Returns the name of the artifact asked
     * @return
     */
    public String getName();
    /**
     * Returns the type of the artifact asked
     * @return
     */
    public String getType();
    /**
     * Returns the ext of the artifact asked
     * @return
     */
    public String getExt();
    /**
     * Returns the url to look this artifact at
     * @return
     */
    public URL getUrl();
    /**
     * Returns the configurations of the module in which the artifact is asked
     * @return an array of configuration names in which the artifact is asked
     */
    public String[] getConfigurations();
    
    /**
     * Returns the matcher to use to know if an artifact match the current descriptor
     * @return
     */
    public PatternMatcher getMatcher();
}
