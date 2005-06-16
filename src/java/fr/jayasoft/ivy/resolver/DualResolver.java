/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.util.Message;

/**
 * DualResolver is used to resolve dependencies with one dependency revolver, called ivy resolver,
 * and then download artifacts found in the resolved dependencies from a second dependency resolver,
 * called artifact resolver.
 * 
 * It is especially useful with resolvers using repository where there is a lot of artifact, but no
 * ivy file, like the maven ibiblio repository.
 * 
 * If no ivy file is found by the ivy resolver, the artifact resolver is used to check if there is
 * artifact corresponding to the request (with default ivy file).
 * 
 * For artifact download, however, only the artifact resolver is used.
 * 
 * Exactly two resolvers should be added to this resolver for it to work properly. The first resolver added
 * if the ivy resolver, the second is the artifact one.
 */
public class DualResolver extends AbstractResolver {
    private DependencyResolver _ivyResolver;
    private DependencyResolver _artifactResolver;

    public void add(DependencyResolver resolver) {
        if (_ivyResolver == null) {
            _ivyResolver = resolver;
        } else if (_artifactResolver == null) {
            _artifactResolver = resolver;
        } else {
            throw new IllegalStateException("exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
    }
    

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        if (_ivyResolver == null || _artifactResolver == null) {
            throw new IllegalStateException("exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        data = new ResolveData(data, doValidate(data));
        final ResolvedModuleRevision mr = _ivyResolver.getDependency(dd, data);
        if (mr == null) {
            Message.verbose("ivy resolver didn't find "+dd.getDependencyRevisionId()+": trying with artifact resolver");
            return _artifactResolver.getDependency(dd, data);
        } else {
            // returns the same ResolvedModuleRevision except that we say that it is dual resolver
            // which resolved the dependency, so that it's it that is used for artifact download
            // ==> forward all except getResolver method
            return new ResolvedModuleRevision() {
                public DependencyResolver getResolver() {
                    return DualResolver.this;
                }

                public ModuleRevisionId getId() {
                    return mr.getId();
                }

                public Date getPublicationDate() {
                    return mr.getPublicationDate();
                }

                public ModuleDescriptor getDescriptor() {
                    return mr.getDescriptor();
                }
                public boolean isDownloaded() {
                    return mr.isDownloaded();
                }
                public boolean isSearched() {
                    return mr.isSearched();
                }
            };
        }
    }
    
    public void reportFailure() {
        _ivyResolver.reportFailure();
        _artifactResolver.reportFailure();        
    }

    public void reportFailure(Artifact art) {
        _ivyResolver.reportFailure(art);
        _artifactResolver.reportFailure(art);        
    }

    public DownloadReport download(Artifact[] artifacts, Ivy ivy, File cache) {
        return _artifactResolver.download(artifacts, ivy, cache);
    }

    public DependencyResolver getArtifactResolver() {
        return _artifactResolver;
    }
    public void setArtifactResolver(DependencyResolver artifactResolver) {
        _artifactResolver = artifactResolver;
    }
    public DependencyResolver getIvyResolver() {
        return _ivyResolver;
    }
    public void setIvyResolver(DependencyResolver ivyResolver) {
        _ivyResolver = ivyResolver;
    }
    public void publish(Artifact artifact, File src) throws IOException {
        if ("ivy".equals(artifact.getType())) {
            _ivyResolver.publish(artifact, src);
        } else {
            _artifactResolver.publish(artifact, src);
        }
    }
    
    public void dumpConfig() {
        if (_ivyResolver == null || _artifactResolver == null) {
            throw new IllegalStateException("exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        Message.verbose("\t"+getName()+" [dual "+_ivyResolver.getName()+" "+_artifactResolver.getName()+"]");
    }
    
    public boolean exists(Artifact artifact) {
        return _artifactResolver.exists(artifact);
    }
}
