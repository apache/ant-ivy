/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.vfs.VfsRepository;

/**
 * @author S. Nesbitt
 *
 */
public class VfsResolver extends RepositoryResolver {
    public VfsResolver() {
        setRepository(new VfsRepository());
    }
    
    public String getTypeName() {
        return "vfs";
    }
}
