package fr.jayasoft.ivy.resolver;

import java.net.URL;
import java.util.Date;

import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolvedModuleRevision;

/** 
 * the same ResolvedModuleRevision except that we say that it is another resolver
 * the artifact resolver, so that it's it that is used for artifact download
 * ==> forward all except getArtifactResolver method
 */
public final class ResolvedModuleRevisionProxy implements ResolvedModuleRevision {
    private final ResolvedModuleRevision _mr;
    DependencyResolver _resolver;

    public ResolvedModuleRevisionProxy(ResolvedModuleRevision mr, DependencyResolver artresolver) {
        if (mr == null) {
            throw new NullPointerException("null module revision not allowed");
        }
        if (artresolver == null) {
            throw new NullPointerException("null resolver not allowed");
        }
        _mr = mr;
        _resolver = artresolver;
    }
    
    public DependencyResolver getResolver() {
        return _mr.getResolver();
    }

    public DependencyResolver getArtifactResolver() {
        return _resolver;
    }

    public ModuleRevisionId getId() {
        return _mr.getId();
    }

    public Date getPublicationDate() {
        return _mr.getPublicationDate();
    }

    public ModuleDescriptor getDescriptor() {
        return _mr.getDescriptor();
    }

    public boolean isDownloaded() {
        return _mr.isDownloaded();
    }

    public boolean isSearched() {
        return _mr.isSearched();
    }
    public URL getLocalMDUrl() {
    	return _mr.getLocalMDUrl();
    }
}