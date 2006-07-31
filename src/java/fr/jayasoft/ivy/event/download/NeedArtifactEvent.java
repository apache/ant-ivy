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

public class NeedArtifactEvent extends DownloadEvent {
    public static final String NAME = "need-artifact";
    
	private DependencyResolver _resolver;

    public NeedArtifactEvent(Ivy source, DependencyResolver resolver, Artifact artifact) {
        super(source, NAME, artifact);
        _resolver = resolver;
        addAttribute("resolver", _resolver.getName());
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

}
