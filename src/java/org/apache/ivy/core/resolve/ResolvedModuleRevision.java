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
package org.apache.ivy.core.resolve;

import java.util.Date;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.plugins.resolver.DependencyResolver;

/**
 * Represents a module revision provisioned on the local filesystem.
 */
public class ResolvedModuleRevision {
    
    private DependencyResolver resolver;

    private DependencyResolver artifactResolver;

    private ModuleDescriptor descriptor;

    private MetadataArtifactDownloadReport report;

    public ResolvedModuleRevision(DependencyResolver resolver, DependencyResolver artifactResolver,
            ModuleDescriptor descriptor, MetadataArtifactDownloadReport report) {
        this.resolver = resolver;
        this.artifactResolver = artifactResolver;
        this.descriptor = descriptor;
        this.report = report;
    }

    /**
     * Returns the identifier of the resolved module.
     * 
     * @return the identifier of the resolved module.
     */
    public ModuleRevisionId getId() {
        return descriptor.getResolvedModuleRevisionId();
    }


    /**
     * Returns the date of publication of the resolved module.
     * 
     * @return the date of publication of the resolved module.
     */
    public Date getPublicationDate() {
        return descriptor.getResolvedPublicationDate();
    }
    

    /**
     * Returns the descriptor of the resolved module.
     * 
     * @return the descriptor of the resolved module.
     */
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }


    /**
     * The resolver which resolved this ResolvedModuleRevision
     * 
     * @return The resolver which resolved this ResolvedModuleRevision
     */
    public DependencyResolver getResolver() {
        return resolver;
    }

    /**
     * The resolver to use to download artifacts
     * 
     * @return The resolver to use to download artifacts
     */
    public DependencyResolver getArtifactResolver() {
        return artifactResolver;
    }


    /**
     * Returns a report of the resolved module metadata artifact provisioning.
     * 
     * @return a report of the resolved module metadata artifact provisioning.
     */
    public MetadataArtifactDownloadReport getReport() {
        return report;
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof ResolvedModuleRevision)) {
            return false;
        }
        return ((ResolvedModuleRevision) obj).getId().equals(getId());
    }

    public int hashCode() {
        return getId().hashCode();
    }

    public String toString() {
        return getId().toString();
    }

}
