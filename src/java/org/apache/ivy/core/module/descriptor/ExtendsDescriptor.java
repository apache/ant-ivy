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
package org.apache.ivy.core.module.descriptor;

import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * Describes parent descriptor information for a module descriptor.
 */
public interface ExtendsDescriptor {

    /** get the module revision id of the declared parent descriptor */
    public ModuleRevisionId getParentRevisionId();

    /**
     * get the resolved revision id for {@link #getParentRevisionId}, see
     * {@link org.apache.ivy.core.module.descriptor.ModuleDescriptor#getResolvedModuleRevisionId()}
     */
    public ModuleRevisionId getResolvedParentRevisionId();

    public ModuleDescriptor getParentMd();

    /**
     * If there is an explicit path to check for the parent descriptor, return it. Otherwise returns
     * null.
     */
    public String getLocation();

    /**
     * Get the parts of the parent descriptor that are inherited. Default supported types are
     * <code>info</code>, <code>description</code>, <code>configurations</code>,
     * <code>dependencies</code>, and/or <code>all</code>. Ivy extensions may add support for
     * additional extends types.
     */
    public String[] getExtendsTypes();

    /** @return true if the <code>all</code> extend type is specified, implying all other types */
    public boolean isAllInherited();

    /** @return true if parent info attributes are inherited (organisation, branch, revision, etc) */
    public boolean isInfoInherited();

    /** @return true if parent description is inherited */
    public boolean isDescriptionInherited();

    /** @return true if parent configurations are inherited */
    public boolean areConfigurationsInherited();

    /** @return true if parent dependencies are inherited */
    public boolean areDependenciesInherited();

    public boolean isLocal();
}
