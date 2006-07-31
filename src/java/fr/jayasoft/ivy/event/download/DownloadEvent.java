/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event.download;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.event.IvyEvent;

public abstract class DownloadEvent extends IvyEvent {
    private Artifact _artifact;

    public DownloadEvent(Ivy source, String name, Artifact artifact) {
    	super(source, name);
        _artifact = artifact;
        addArtifactAttributes(_artifact);
    }

    protected void addArtifactAttributes(Artifact artifact) {
		addMridAttributes(artifact.getModuleRevisionId());
		addAttributes(artifact.getAttributes());
	}

	public Artifact getArtifact() {
        return _artifact;
    }
    
    
}
