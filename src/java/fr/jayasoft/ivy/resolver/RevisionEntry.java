/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.DependencyResolver;



public class RevisionEntry {
    private ModuleEntry _moduleEntry;
    private String _revision;

    public RevisionEntry(ModuleEntry mod, String name) {
        _moduleEntry = mod;
        _revision = name;
    }

    public ModuleEntry getModuleEntry() {
        return _moduleEntry;
    }
    

    public String getRevision() {
        return _revision;
    }

    public String getModule() {
        return _moduleEntry.getModule();
    }

    public String getOrganisation() {
        return _moduleEntry.getOrganisation();
    }

    public OrganisationEntry getOrganisationEntry() {
        return _moduleEntry.getOrganisationEntry();
    }

    public DependencyResolver getResolver() {
        return _moduleEntry.getResolver();
    }
    
}
