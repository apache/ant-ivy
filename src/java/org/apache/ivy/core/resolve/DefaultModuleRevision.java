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
 * @author x.hanin
 *
 */
public class DefaultModuleRevision implements ResolvedModuleRevision {
    private DependencyResolver _resolver;
    private DependencyResolver _artifactResolver;
    private ModuleDescriptor _descriptor;
    private boolean _isDownloaded;
    private boolean _isSearched;
    private URL _localMDUrl;
    
    public DefaultModuleRevision(DependencyResolver resolver, DependencyResolver artifactResolver, ModuleDescriptor descriptor, boolean searched, boolean downloaded, URL localMDUrl) {
        _resolver = resolver;
        _artifactResolver = artifactResolver;
        _descriptor = descriptor;
        _isSearched = searched;
        _isDownloaded = downloaded;
        _localMDUrl = localMDUrl;
    }

    public DependencyResolver getResolver() {
        return _resolver;
    }
    
    public DependencyResolver getArtifactResolver() {
        return _artifactResolver;
    }

    public ModuleDescriptor getDescriptor() {
        return _descriptor;
    }

    public ModuleRevisionId getId() {
        return _descriptor.getResolvedModuleRevisionId();
    }
    
    public Date getPublicationDate() {
        return _descriptor.getResolvedPublicationDate();
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ResolvedModuleRevision)) {
            return false;
        }
        return ((ResolvedModuleRevision)obj).getId().equals(getId());
    }
    
    public int hashCode() {
        return getId().hashCode();
    }
    
    public String toString() {
        return getId().toString();
    }

    public boolean isDownloaded() {
        return _isDownloaded;
    }

    public boolean isSearched() {
        return _isSearched;
    }

	public URL getLocalMDUrl() {
		return _localMDUrl;
	}
    
}
