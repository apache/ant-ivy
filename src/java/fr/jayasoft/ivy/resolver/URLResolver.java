/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.url.URLRepository;

/**
 * This resolver is able to work with any URLs, it handles latest revisions
 * with file and http urls only, and it does not handle publishing
 */
public class URLResolver extends RepositoryResolver {    
    public URLResolver() {
        setRepository(new URLRepository());
    }
    public String getTypeName() {
        return "url";
    }
}
