/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.namespace;

import fr.jayasoft.ivy.ModuleRevisionId;

public interface NamespaceTransformer {
    public ModuleRevisionId transform(ModuleRevisionId mrid);

    public boolean isIdentity();
}
