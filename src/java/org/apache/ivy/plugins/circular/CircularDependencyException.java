/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.circular;

import java.util.Collection;

import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * Unchecked exception thrown when a circular dependency exists between projects.
 */

public class CircularDependencyException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 670272039106237360L;

    private ModuleRevisionId[] mrids;

    /**
     * @param mrids
     *            module descriptors in order of circular dependency
     */
    public CircularDependencyException(final ModuleRevisionId[] mrids) {
        super(CircularDependencyHelper.formatMessage(mrids));
        this.mrids = mrids;
    }

    public CircularDependencyException(final Collection<ModuleRevisionId> mrids) {
        this(mrids.toArray(new ModuleRevisionId[mrids.size()]));
    }

    public ModuleRevisionId[] getPath() {
        return this.mrids;
    }

}
