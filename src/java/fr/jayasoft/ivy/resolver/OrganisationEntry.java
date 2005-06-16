/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.DependencyResolver;

public class OrganisationEntry {
    private DependencyResolver _resolver;
    private String _organisation;

    public OrganisationEntry(DependencyResolver resolver, String organisation) {
        _resolver = resolver;
        _organisation = organisation;
    }

    public String getOrganisation() {
        return _organisation;
    }
    
    public DependencyResolver getResolver() {
        return _resolver;
    }
    
}
