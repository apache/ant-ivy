/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.DependencyResolver;


public class ModuleEntry {
    private OrganisationEntry _organisationEntry;
    private String _module;

    public ModuleEntry(OrganisationEntry org, String name) {
        _organisationEntry = org;
        _module = name;
    }

    public String getOrganisation() {
        return _organisationEntry.getOrganisation();
    }
    
    public DependencyResolver getResolver() {
        return _organisationEntry.getResolver();
    }

    public String getModule() {
        return _module;
    }
    

    public OrganisationEntry getOrganisationEntry() {
        return _organisationEntry;
    }
    
}
