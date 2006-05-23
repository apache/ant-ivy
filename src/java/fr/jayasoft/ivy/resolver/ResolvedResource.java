/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.ArtifactInfo;
import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.repository.Resource;

public class ResolvedResource implements ArtifactInfo {        
    private Resource _res;
    private String _rev;
	
    public ResolvedResource(Resource res, String rev) {
        _res = res;
        _rev = rev;
    }
    public String getRevision() {
        return _rev;
    }
    public Resource getResource() {
        return _res;
    }
    public String toString() {
        return _res + " ("+_rev+")";
    }
    public long getLastModified() {
        return getResource().getLastModified();
    }
}