/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.Artifact;

public class PrepareDownloadEvent extends IvyEvent {
    private Artifact[] _artifacts;
    
    public PrepareDownloadEvent(Artifact[] artifacts) {
        _artifacts = artifacts;
    }
    
    public Artifact[] getArtifacts() {
        return _artifacts;
    }
}
