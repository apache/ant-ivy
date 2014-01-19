/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.namespace;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;

public final class NameSpaceHelper {
    private NameSpaceHelper() {
    }

    public static DependencyDescriptor toSystem(DependencyDescriptor dd, Namespace ns) {
        return DefaultDependencyDescriptor.transformInstance(dd, ns);
    }

    public static DependencyDescriptor transform(DependencyDescriptor dd, NamespaceTransformer t,
            boolean fromSystem) {
        return DefaultDependencyDescriptor.transformInstance(dd, t, fromSystem);
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
        return new ResolvedModuleRevision(rmr.getResolver(), rmr.getArtifactResolver(), md,
                transform(rmr.getReport(), ns.getToSystemTransformer()), rmr.isForce());
    }

    public static Artifact transform(Artifact artifact, NamespaceTransformer t) {
        if (t.isIdentity()) {
            return artifact;
        }
        ModuleRevisionId mrid = t.transform(artifact.getModuleRevisionId());
        if (artifact.getModuleRevisionId().equals(mrid)) {
            return artifact;
        }
        return new DefaultArtifact(mrid, artifact.getPublicationDate(), artifact.getName(),
                artifact.getType(), artifact.getExt(), artifact.getUrl(),
                artifact.getQualifiedExtraAttributes());
    }

    public static MetadataArtifactDownloadReport transform(MetadataArtifactDownloadReport report,
            NamespaceTransformer t) {
        if (t.isIdentity()) {
            return report;
        }
        MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(transform(
            report.getArtifact(), t));
        madr.setSearched(report.isSearched());
        madr.setDownloadStatus(report.getDownloadStatus());
        madr.setDownloadDetails(report.getDownloadDetails());
        madr.setArtifactOrigin(report.getArtifactOrigin());
        madr.setDownloadTimeMillis(report.getDownloadTimeMillis());
        madr.setOriginalLocalFile(report.getOriginalLocalFile());
        madr.setLocalFile(report.getLocalFile());
        madr.setSize(report.getSize());
        return madr;
    }

    public static ArtifactId transform(ArtifactId artifactId, NamespaceTransformer t) {
        if (t.isIdentity()) {
            return artifactId;
        }
        ModuleId mid = transform(artifactId.getModuleId(), t);
        if (mid.equals(artifactId.getModuleId())) {
            return artifactId;
        }
        return new ArtifactId(mid, artifactId.getName(), artifactId.getType(), artifactId.getExt());
    }

    public static ModuleId transform(ModuleId mid, NamespaceTransformer t) {
        if (t.isIdentity()) {
            return mid;
        }
        return t.transform(new ModuleRevisionId(mid, "")).getModuleId();
    }

    public static String transformOrganisation(String org, NamespaceTransformer t) {
        return transform(new ModuleId(org, ""), t).getOrganisation();
    }
}
