/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.namespace;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactId;
import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DefaultModuleRevision;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolvedModuleRevision;

public class NameSpaceHelper {

    public static DependencyDescriptor toSystem(DependencyDescriptor dd, Namespace ns) {
        return DefaultDependencyDescriptor.transformInstance(dd, ns);
    }

    public static DependencyDescriptor transform(DependencyDescriptor dd, NamespaceTransformer t) {
        return DefaultDependencyDescriptor.transformInstance(dd, t);
    }

    public static ModuleDescriptor toSystem(ModuleDescriptor md, Namespace ns) {
        return DefaultModuleDescriptor.transformInstance(md, ns);
    }

    public static ResolvedModuleRevision toSystem(ResolvedModuleRevision rmr, Namespace ns) {
        if (ns.getToSystemTransformer().isIdentity()) {
            return rmr;
        }
        ModuleDescriptor md = toSystem(rmr.getDescriptor(), ns);
        if (md.equals(rmr.getDescriptor())) {
            return rmr;
        }
        return new DefaultModuleRevision(rmr.getResolver(), md, rmr.isSearched(), rmr.isDownloaded());
    }

    public static Artifact transform(Artifact artifact, NamespaceTransformer t) {
        if (t.isIdentity()) {
            return artifact;
        }
        ModuleRevisionId mrid = t.transform(artifact.getModuleRevisionId());
        if (artifact.getModuleRevisionId().equals(mrid)) {
            return artifact;
        }
        return new DefaultArtifact(mrid, artifact.getPublicationDate(), artifact.getName(), artifact.getType(), artifact.getExt());
    }

    public static ArtifactId transform(ArtifactId artifactId, NamespaceTransformer t) {
        if (t.isIdentity()) {
            return artifactId;
        }
        ModuleId mid = t.transform(new ModuleRevisionId(artifactId.getModuleId(), "")).getModuleId();
        if (mid.equals(artifactId.getModuleId())) {
            return artifactId;
        }
        return new ArtifactId(mid, artifactId.getName(), artifactId.getType(), artifactId.getExt());
    }
}
