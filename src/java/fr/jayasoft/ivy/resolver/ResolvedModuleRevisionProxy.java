package fr.jayasoft.ivy.resolver;

import java.util.Date;

import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolvedModuleRevision;

/** 
 * the same ResolvedModuleRevision except that we say that it is another resolver
 * which resolved the dependency, so that it's it that is used for artifact download
 * ==> forward all except getResolver method
 */
public final class ResolvedModuleRevisionProxy implements ResolvedModuleRevision {
    private final ResolvedModuleRevision _mr;
    DependencyResolver _resolver;

    public ResolvedModuleRevisionProxy(ResolvedModuleRevision mr, DependencyResolver resolver) {
        _mr = mr;
        _resolver = resolver;
    }

    public DependencyResolver getResolver() {
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
}