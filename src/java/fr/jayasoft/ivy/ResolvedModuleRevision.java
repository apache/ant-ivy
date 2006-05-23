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
public interface ResolvedModuleRevision {
    /**
     * The resolver which resolved this ResolvedModuleRevision
     * @return The resolver which resolved this ResolvedModuleRevision
     */
    DependencyResolver getResolver();
    /**
     * The resolver to use to download artifacts
     * @return The resolver to use to download artifacts
     */
    DependencyResolver getArtifactResolver();
    ModuleRevisionId getId();
    Date getPublicationDate();
    ModuleDescriptor getDescriptor();
    boolean isDownloaded();
    boolean isSearched();
    public URL getLocalMDUrl();
}
