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
import fr.jayasoft.ivy.report.ArtifactDownloadReport;

public class EndDownloadEvent extends DownloadEvent {

    private DependencyResolver _resolver;
    private ArtifactDownloadReport _report;

    public EndDownloadEvent(Ivy source, DependencyResolver resolver, Artifact artifact, ArtifactDownloadReport report) {
        super(source, artifact);
        _resolver = resolver;
        _report = report;
    }

    public ArtifactDownloadReport getReport() {
        return _report;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }

}
