/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;

/**
 * This interface defines a version matcher, i.e. a class able to tell if the revision
 * asked by a module for a dependency is dynamic (i.e. need to find all revisions to find the good one among them)
 * and if a found revision matches the asked one.
 * 
 * Two ways of matching are possible:
 * - based on the module revision only (known as ModuleRevisionId)
 * - based on the parsed module descriptor
 * 
 * The second being much more time consuming than the first, the version matcher should tell if it needs such parsing 
 * or not using the needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) method. Anyway, the first way is always used, and if a revision is not accepted using the first
 * method, the module descriptor won't be parsed.
 * 
 * Therefore if a version matcher uses only module descriptors to accept a revision or not it should always return true
 * to needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) and accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid).
 * 
 * @author Xavier Hanin
 */
public interface VersionMatcher {
    /**
     * Indicates if the given asked ModuleRevisionId should be considered as dynamic for
     * the current VersionMatcher or not.
     * @param askedMrid the dependency module revision id as asked by a module
     * @return true if this revision is considered as a dynamic one, false otherwise
     */
    public boolean isDynamic(ModuleRevisionId askedMrid);
    /**
     * Indicates if this version matcher considers that the module revision found matches the asked one.
     * @param askedMrid
     * @param foundMrid
     * @return
     */
    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid);
    /**
     * Indicates if this VersionMatcher needs module descriptors to determine if a module revision 
     * matches the asked one.
     * Note that returning true in this method may imply big performance issues. 
     * @return
     */
    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid);
    /**
     * Indicates if this version matcher considers that the module found matches the asked one.
     * This method can be called even needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid)
     * returns false, so it is required to implement it in any case, a usual default implementation being:
     * 
     * return accept(askedMrid, foundMD.getResolvedModuleRevisionId());
     * 
     * @param askedMrid
     * @param foundMD
     * @return
     */
    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD);
    
    /**
     * Returns the version matcher name identifying this version matcher
     * @return the version matcher name identifying this version matcher
     */
    public String getName();
}
