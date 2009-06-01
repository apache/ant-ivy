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
package org.apache.ivy.plugins.version;

import java.util.Comparator;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * This interface defines a version matcher, i.e. a class able to tell if the revision asked by a
 * module for a dependency is dynamic (i.e. need to find all revisions to find the good one among
 * them) and if a found revision matches the asked one.
 * <p>
 * Two ways of matching are possible:
 * <ul>
 * <li>based on the module revision only (known as ModuleRevisionId)</li>
 * <li>based on the parsed module descriptor</li>
 * </ul>
 * The second being much more time consuming than the first, the version matcher should tell if it
 * needs such parsing or not using the
 * {@link #needModuleDescriptor(ModuleRevisionId, ModuleRevisionId)} method. Anyway, the first way
 * is always used, and if a revision is not accepted using the first method, the module descriptor
 * won't be parsed. Therefore if a version matcher uses only module descriptors to accept a revision
 * or not it should always return true to
 * {@link #needModuleDescriptor(ModuleRevisionId, ModuleRevisionId)} and
 * {@link #accept(ModuleRevisionId, ModuleDescriptor)}.
 */
public interface VersionMatcher {
    /**
     * Indicates if the given asked ModuleRevisionId should be considered as dynamic for the current
     * VersionMatcher or not.
     * 
     * @param askedMrid
     *            the dependency module revision id as asked by a module
     * @return true if this revision is considered as a dynamic one, false otherwise
     */
    public boolean isDynamic(ModuleRevisionId askedMrid);

    /**
     * Indicates if this version matcher considers that the module revision found matches the asked
     * one.
     * 
     * @param askedMrid
     * @param foundMrid
     * @return
     */
    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid);

    /**
     * Indicates if this VersionMatcher needs module descriptors to determine if a module revision
     * matches the asked one. Note that returning true in this method may imply big performance
     * issues.
     * 
     * @return
     */
    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid);

    /**
     * Indicates if this version matcher considers that the module found matches the asked one. This
     * method can be called even needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId
     * foundMrid) returns false, so it is required to implement it in any case, a usual default
     * implementation being: return accept(askedMrid, foundMD.getResolvedModuleRevisionId());
     * 
     * @param askedMrid
     * @param foundMD
     * @return
     */
    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD);

    /**
     * Compares a dynamic revision (askedMrid) with a static one (foundMrid) to indicate which one
     * should be considered the greater. If there is not enough information to know which one is the
     * greater, the dynamic one should be considered greater and this method should return 0. This
     * method should never be called with a askdeMrid for which isDynamic returns false.
     * 
     * @param askedMrid
     *            the dynamic revision to compare
     * @param foundMrid
     *            the static revision to compare
     * @param staticComparator
     *            a comparator which can be used to compare static revisions
     * @return 0 if it's not possible to know which one is greater, greater than 0 if askedMrid
     *         should be considered greater, lower than 0 if it can't be consider greater
     */
    public int compare(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid,
            Comparator staticComparator);

    /**
     * Returns the version matcher name identifying this version matcher
     * 
     * @return the version matcher name identifying this version matcher
     */
    public String getName();
}
