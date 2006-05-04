/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;

public class SubVersionMatcher implements VersionMatcher {

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        return askedMrid.getRevision().endsWith("+");
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        String prefix = askedMrid.getRevision().substring(0, askedMrid.getRevision().length() - 1);
        return foundMrid.getRevision().startsWith(prefix);
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        return accept(askedMrid, foundMD.getResolvedModuleRevisionId());
    }
}
