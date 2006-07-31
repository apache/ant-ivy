/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event.download;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactOrigin;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;

public class StartArtifactDownloadEvent extends DownloadEvent {
    public static final String NAME = "pre-download-artifact";
    
	private DependencyResolver _resolver;
	private ArtifactOrigin _origin;

    public StartArtifactDownloadEvent(Ivy source, DependencyResolver resolver, Artifact artifact, ArtifactOrigin origin) {
        super(source, NAME, artifact);
        _resolver = resolver;
        _origin = origin;
        addAttribute("resolver", _resolver.getName());
        addAttribute("origin", origin.getLocation());
        addAttribute("local", String.valueOf(origin.isLocal()));
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

	public ArtifactOrigin getOrigin() {
		return _origin;
	}

}
