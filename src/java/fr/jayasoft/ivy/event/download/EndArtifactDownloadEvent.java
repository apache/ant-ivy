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
import fr.jayasoft.ivy.report.ArtifactDownloadReport;

public class EndArtifactDownloadEvent extends DownloadEvent {
	public static final String NAME = "post-download-artifact";

    private DependencyResolver _resolver;
    private ArtifactDownloadReport _report;

    public EndArtifactDownloadEvent(Ivy source, DependencyResolver resolver, Artifact artifact, ArtifactDownloadReport report) {
        super(source, NAME, artifact);
        _resolver = resolver;
        _report = report;
        addAttribute("resolver", _resolver.getName());
        addAttribute("status", _report.getDownloadStatus().toString());
        addAttribute("size", String.valueOf(_report.getSize()));
        ArtifactOrigin origin = report.getArtifactOrigin();
        if (origin != null) {
        	addAttribute("origin", _report.getArtifactOrigin().getLocation());
        	addAttribute("local", String.valueOf(_report.getArtifactOrigin().isLocal()));
        } else {
            addAttribute("origin", "");
            addAttribute("local", "");
        }
    }

    public ArtifactDownloadReport getReport() {
        return _report;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

}
