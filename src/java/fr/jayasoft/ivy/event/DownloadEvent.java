/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.Artifact;

public abstract class DownloadEvent extends IvyEvent {
    private Artifact _artifact;

    public DownloadEvent(Artifact artifact) {
        _artifact = artifact;
    }

    public Artifact getArtifact() {
        return _artifact;
    }
    
    
}
