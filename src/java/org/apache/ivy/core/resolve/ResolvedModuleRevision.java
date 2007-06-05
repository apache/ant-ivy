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
public interface ResolvedModuleRevision {
    /**
     * The resolver which resolved this ResolvedModuleRevision
     * 
     * @return The resolver which resolved this ResolvedModuleRevision
     */
    DependencyResolver getResolver();

    /**
     * The resolver to use to download artifacts
     * 
     * @return The resolver to use to download artifacts
     */
    DependencyResolver getArtifactResolver();

    ModuleRevisionId getId();

    Date getPublicationDate();

    ModuleDescriptor getDescriptor();

    boolean isDownloaded();

    boolean isSearched();

    public URL getLocalMDUrl();
}
