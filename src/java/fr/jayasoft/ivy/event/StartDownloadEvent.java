/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyResolver;

public class StartDownloadEvent extends DownloadEvent {

    private DependencyResolver _resolver;

    public StartDownloadEvent(DependencyResolver resolver, Artifact artifact) {
        super(artifact);
        _resolver = resolver;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

}
