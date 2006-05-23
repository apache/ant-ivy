/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.net.URL;
import java.util.Date;


/**
 * @author x.hanin
 *
 */
public class DefaultModuleRevision implements ResolvedModuleRevision {
    private DependencyResolver _resolver;
    private DependencyResolver _artifactResolver;
    private ModuleDescriptor _descriptor;
    private boolean _isDownloaded;
    private boolean _isSearched;
    private URL _localMDUrl;
    
    public DefaultModuleRevision(DependencyResolver resolver, DependencyResolver artifactResolver, ModuleDescriptor descriptor, boolean searched, boolean downloaded, URL localMDUrl) {
        _resolver = resolver;
        _artifactResolver = artifactResolver;
        _descriptor = descriptor;
        _isSearched = searched;
        _isDownloaded = downloaded;
        _localMDUrl = localMDUrl;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }
    
    public DependencyResolver getArtifactResolver() {
        return _artifactResolver;
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

	public URL getLocalMDUrl() {
		return _localMDUrl;
	}
    
}
