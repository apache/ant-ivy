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
package org.apache.ivy.plugins.conflict;

import java.util.Collection;

import org.apache.ivy.core.resolve.IvyNode;

public interface ConflictManager {
    /**
     * Resolves the eventual conflicts found in the given collection of IvyNode.
     * This method return a Collection of IvyNode which have not been evicted.
     * The given conflicts Collection contains at least one IvyNode.
     * 
     * This method can be called with IvyNodes which are not yet loaded.
     * If this conflict manager is not able to resolve conflicts with the current
     * data found in the IvyNodes and need them to be fully loaded, it will 
     * return null to indicate that no conflict resolution has been done.
     * 
     * @param parent the ivy node parent for which the conflict is to be resolved
     * @param conflicts the collection of IvyNode to check for conflicts
     * @return a Collection of IvyNode which have not been evicted, or null if
     *         conflict management resolution is not possible yet
     */
    Collection resolveConflicts(IvyNode parent, Collection conflicts);
    String getName();
}
