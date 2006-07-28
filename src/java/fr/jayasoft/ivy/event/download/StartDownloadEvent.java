/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event.download;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;

public class StartDownloadEvent extends DownloadEvent {

    private DependencyResolver _resolver;

    public StartDownloadEvent(Ivy source, DependencyResolver resolver, Artifact artifact) {
        super(source, artifact);
        _resolver = resolver;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

}
