/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.Date;

import fr.jayasoft.ivy.extendable.ExtendableItem;
import fr.jayasoft.ivy.version.VersionMatcher;

/**
 * @author x.hanin
 *
 */
public interface ModuleDescriptor extends ExtendableItem {
    public static final String DEFAULT_CONFIGURATION = "default";
    /**
     * Returns true if this descriptor is a default one, i.e.
     * one generated for a module not actually having one. 
     * @return
     */
    boolean isDefault();
    ModuleRevisionId getModuleRevisionId();
    /**
     * The module revision id returned here is the resolved one, 
     * i.e. it is never a latest one. If the revision has not been 
     * resolved, a null revision should be returned by getRevision()
     * of the returned ModuleRevisionId.
     * This revision must be the same as the module descriptor resolved
     * revision id unless no module descriptor is defined
     * @return
     */
    ModuleRevisionId getResolvedModuleRevisionId();
    /**
     * This method update the resolved module revision id
     * @param revId
     */
    void setResolvedModuleRevisionId(ModuleRevisionId revId);
    /**
     * This method update the resolved publication date
     * @param publicationDate
     */
    void setResolvedPublicationDate(Date publicationDate);
    
    String getStatus();
    /**
     * may be null if unknown in the descriptor itself
     * @return
     */
    Date getPublicationDate();
    /**
     * the publication date of the module revision should be the date at which it has been published,
     * i.e. in general the date of any of its published artifacts, since all published artifact
     * of a module should follow the same publishing cycle.
     */     
    Date getResolvedPublicationDate();
    /**
     * Returns all the configurations declared by this module as an array.
     * This array is never empty (a 'default' conf is assumed when none is declared
     * in the ivy file) 
     * @return all the configurations declared by this module as an array.
     */
    Configuration[] getConfigurations();
    String[] getConfigurationsNames();
    String[] getPublicConfigurationsNames();
    Artifact[] getArtifacts(String conf);
    DependencyDescriptor[] getDependencies();
    
    /**
     * Returns true if the module described by this descriptor dependes directly upon the
     * given module descriptor 
     * @param md
     * @return
     */
    boolean dependsOn(VersionMatcher matcher, ModuleDescriptor md);
    /**
     * @param confName
     * @return
     */
    Configuration getConfiguration(String confName);
    
    /**
     * Returns the conflict manager to use for the given ModuleId
     * 
     * @param id
     * @return
     */
    ConflictManager getConflictManager(ModuleId id);

    /**
     * Returns the licenses of the module described by this descriptor
     * @return
     */
    License[] getLicenses();
    
    String getHomePage();
    long getLastModified();
}
