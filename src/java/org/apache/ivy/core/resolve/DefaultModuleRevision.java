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

import java.net.URL;
import java.util.Date;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.DependencyResolver;

/**
 *
 */
public class DefaultModuleRevision implements ResolvedModuleRevision {
    private DependencyResolver resolver;

    private DependencyResolver artifactResolver;

    private ModuleDescriptor descriptor;

    private boolean isDownloaded;

    private boolean isSearched;

    public DefaultModuleRevision(DependencyResolver resolver, DependencyResolver artifactResolver,
            ModuleDescriptor descriptor, boolean searched, boolean downloaded) {
        this.resolver = resolver;
        this.artifactResolver = artifactResolver;
        this.descriptor = descriptor;
        isSearched = searched;
        isDownloaded = downloaded;
    }

    public DependencyResolver getResolver() {
        return resolver;
    }

    public DependencyResolver getArtifactResolver() {
        return artifactResolver;
    }

    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    public ModuleRevisionId getId() {
        return descriptor.getResolvedModuleRevisionId();
    }

    public Date getPublicationDate() {
        return descriptor.getResolvedPublicationDate();
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

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public boolean isSearched() {
        return isSearched;
    }

    public static ResolvedModuleRevision searchedRmr(final ResolvedModuleRevision rmr) {
        // delegate all to previously found except isSearched
        return new ResolvedModuleRevision() {
            public boolean isSearched() {
                return true;
            }

            public boolean isDownloaded() {
                return rmr.isDownloaded();
            }

            public ModuleDescriptor getDescriptor() {
                return rmr.getDescriptor();
            }

            public Date getPublicationDate() {
                return rmr.getPublicationDate();
            }

            public ModuleRevisionId getId() {
                return rmr.getId();
            }

            public DependencyResolver getResolver() {
                return rmr.getResolver();
            }

            public DependencyResolver getArtifactResolver() {
                return rmr.getArtifactResolver();
            }
        };
    }
    
}
