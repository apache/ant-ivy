/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.Status;

public class LatestVersionMatcher implements VersionMatcher {

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        return askedMrid.getRevision().startsWith("latest.");
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return true;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return !"latest.integration".equals(askedMrid.getRevision());
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        String askedStatus = askedMrid.getRevision().substring("latest.".length());
        return Status.getPriority(askedStatus) >= Status.getPriority(foundMD.getStatus());
    }
}
