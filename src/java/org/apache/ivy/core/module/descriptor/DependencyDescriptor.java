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

import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.util.extendable.ExtendableItem;

/**
 * Describes a dependency from a depender to a dependee.
 * <p>
 * The main information this descriptor contains is the constraint on the dependency. There is
 * actually two kind of dependency constraints: the default dependency constraint, which can be
 * obtained with {@link #getDependencyRevisionId()}, and corresponds to the <code>rev</code>
 * attribute in Ivy files. This is the constraint as it should be honored by Ivy in default resolve
 * mode.
 * </p>
 * <p>
 * Then there is the dynamic constraint, which can either be the same as the default constraint, or
 * the original dependency constraint when an Ivy file is delivered an published to a repository.
 * This dynamic constraint is returned by {@link #getDynamicConstraintDependencyRevisionId()}, and
 * corresponds to the <code>revconstraint</code> attribute in the Ivy file. In some resolve mode,
 * this constraint can be used instead of the default dependency constraint when performing
 * dependency resolution.
 * </p>
 */
public interface DependencyDescriptor extends ExtendableItem, InheritableItem {
    ModuleId getDependencyId();

    /**
     * Used to indicate that this revision must be used in case of conflicts, independently of
     * conflicts manager. This only works for direct dependencies, and not transitive ones.
     * 
     * @return true if this dependency should be used, false if conflicts manager can do its work.
     */
    boolean isForce();

    /**
     * Used to indicate that this dependency is a changing one. A changing dependency in ivy means
     * that the revision may have its artifacts modified without revision change. When new artifacts
     * are published a new ivy file should also be published with a new publication date to indicate
     * to ivy that artifacts have changed and that they should be downloaded again.
     * 
     * @return true if this dependency is a changing one
     */
    boolean isChanging();

    boolean isTransitive();

    ModuleRevisionId getParentRevisionId();

    /**
     * Returns the constraint on dependency this descriptor represents.
     * 
     * @return the constraint on dependency.
     */
    ModuleRevisionId getDependencyRevisionId();

    /**
     * Returns the dynamic constraint on dependency this descriptor represents.
     * 
     * @return the dynamic constraint on dependency, or exact constraint if no dynamic constraint is
     *         specified.
     */
    ModuleRevisionId getDynamicConstraintDependencyRevisionId();

    String[] getModuleConfigurations();

    String[] getDependencyConfigurations(String moduleConfiguration, String requestedConfiguration);

    String[] getDependencyConfigurations(String moduleConfiguration);

    String[] getDependencyConfigurations(String[] moduleConfigurations);

    Namespace getNamespace();

    DependencyArtifactDescriptor[] getAllDependencyArtifacts();

    DependencyArtifactDescriptor[] getDependencyArtifacts(String moduleConfigurations);

    DependencyArtifactDescriptor[] getDependencyArtifacts(String[] moduleConfigurations);

    IncludeRule[] getAllIncludeRules();

    IncludeRule[] getIncludeRules(String moduleConfigurations);

    IncludeRule[] getIncludeRules(String[] moduleConfigurations);

    ExcludeRule[] getAllExcludeRules();

    ExcludeRule[] getExcludeRules(String moduleConfigurations);

    ExcludeRule[] getExcludeRules(String[] moduleConfigurations);

    /**
     * Returns true if
     * 
     * @param moduleConfigurations
     * @param artifactId
     * @return
     */
    boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId);

    /**
     * Returns true if this descriptor contains any exclusion rule
     * 
     * @return true if this descriptor contains any exclusion rule
     */
    public boolean canExclude();

    DependencyDescriptor asSystem();

    /**
     * Clones current dependency descriptor with another revision.
     * 
     * @param revision
     *            the revision of the cloned dependency descriptor
     * @return the cloned dependency descriptor
     * @throws IllegalArgumentException
     *             if the given {@link ModuleRevisionId} has not the same {@link ModuleId} as the
     *             {@link ModuleRevisionId} of this descriptor.
     */
    DependencyDescriptor clone(ModuleRevisionId revision);
}
