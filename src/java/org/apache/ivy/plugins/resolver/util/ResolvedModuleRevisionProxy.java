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
package org.apache.ivy.plugins.resolver.util;

import java.net.URL;
import java.util.Date;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.DependencyResolver;


/** 
 * the same ResolvedModuleRevision except that we say that it is another resolver
 * the artifact resolver, so that it's it that is used for artifact download
 * ==> forward all except getArtifactResolver method
 */
public final class ResolvedModuleRevisionProxy implements ResolvedModuleRevision {
    private final ResolvedModuleRevision _mr;
    DependencyResolver _resolver;

    public ResolvedModuleRevisionProxy(ResolvedModuleRevision mr, DependencyResolver artresolver) {
        if (mr == null) {
            throw new NullPointerException("null module revision not allowed");
        }
        if (artresolver == null) {
            throw new NullPointerException("null resolver not allowed");
        }
        _mr = mr;
        _resolver = artresolver;
    }
    
    public DependencyResolver getResolver() {
        return _mr.getResolver();
    }

    public DependencyResolver getArtifactResolver() {
        return _resolver;
    }

    public ModuleRevisionId getId() {
        return _mr.getId();
    }

    public Date getPublicationDate() {
        return _mr.getPublicationDate();
    }

    public ModuleDescriptor getDescriptor() {
        return _mr.getDescriptor();
    }

    public boolean isDownloaded() {
        return _mr.isDownloaded();
    }

    public boolean isSearched() {
        return _mr.isSearched();
    }
    public URL getLocalMDUrl() {
    	return _mr.getLocalMDUrl();
    }
}