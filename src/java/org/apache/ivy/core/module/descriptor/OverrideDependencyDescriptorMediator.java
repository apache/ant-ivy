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
 * DependencyDescriptorMediator used to override some dependency descriptors values, such as the
 * branch or version of the dependency.
 */
public class OverrideDependencyDescriptorMediator implements DependencyDescriptorMediator {
    private String version;

    private String branch;

    /**
     * Constructs a new instance.
     * 
     * @param branch
     *            the branch to give to mediated dependency descriptors, <code>null</code> to keep
     *            the original branch.
     * @param version
     *            the version to give to mediated dependency descriptors, <code>null</code> to keep
     *            the original one.
     */
    public OverrideDependencyDescriptorMediator(String branch, String version) {
        this.branch = branch;
        this.version = version;
    }

    /**
     * Returns the version this mediator will give to mediated descriptors, or <code>null</code> if
     * this mediator does not override version.
     * 
     * @return the version this mediator will give to mediated descriptors.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the branch this mediator will give to mediated descriptors, or <code>null</code> if
     * this mediator does not override branch.
     * 
     * @return the branch this mediator will give to mediated descriptors.
     */
    public String getBranch() {
        return branch;
    }

    public DependencyDescriptor mediate(DependencyDescriptor dd) {
        ModuleRevisionId mrid = dd.getDependencyRevisionId();
        if ((version == null || version.equals(mrid.getRevision()))
                && (branch == null || branch.equals(mrid.getBranch()))) {
            return dd;
        }

        String version = this.version == null ? mrid.getRevision() : this.version;
        String branch = this.branch == null ? mrid.getBranch() : this.branch;

        // if this is a noop, do not construct any new object
        if (version.equals(dd.getDependencyRevisionId().getRevision())
                && branch.equals(dd.getDependencyRevisionId().getBranch())) {
            return dd;
        }

        return dd.clone(ModuleRevisionId.newInstance(mrid.getOrganisation(), mrid.getName(),
            branch, version, mrid.getQualifiedExtraAttributes()));
    }
}
