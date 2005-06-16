/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.Date;


/**
 * @author x.hanin
 *
 */
public class DefaultModuleRevision implements ResolvedModuleRevision {
    private DependencyResolver _resolver;
    private ModuleDescriptor _descriptor;
    private boolean _isDownloaded;
    private boolean _isSearched;
    
    public DefaultModuleRevision(DependencyResolver resolver, ModuleDescriptor descriptor, boolean searched, boolean downloaded) {
        _resolver = resolver;
        _descriptor = descriptor;
        _isSearched = searched;
        _isDownloaded = downloaded;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

    public ModuleDescriptor getDescriptor() {
        return _descriptor;
    }

    public ModuleRevisionId getId() {
        return _descriptor.getResolvedModuleRevisionId();
    }
    
    public Date getPublicationDate() {
        return _descriptor.getResolvedPublicationDate();
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ResolvedModuleRevision)) {
            return false;
        }
        return ((ResolvedModuleRevision)obj).getId().equals(getId());
    }
    
    public int hashCode() {
        return getId().hashCode();
    }
    
    public String toString() {
        return getId().toString();
    }

    public boolean isDownloaded() {
        return _isDownloaded;
    }

    public boolean isSearched() {
        return _isSearched;
    }
    
}
