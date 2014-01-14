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
package org.apache.ivy.core.event.resolve;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.DependencyResolver;

public class EndResolveDependencyEvent extends ResolveDependencyEvent {
    public static final String NAME = "post-resolve-dependency";

    private ResolvedModuleRevision module;

    private long duration;

    public EndResolveDependencyEvent(DependencyResolver resolver, DependencyDescriptor dd,
            ModuleRevisionId requestedRevisionId, ResolvedModuleRevision module, long duration) {
        super(NAME, resolver, dd, requestedRevisionId);
        this.module = module;
        this.duration = duration;
        addAttribute("duration", String.valueOf(duration));
        if (this.module != null) {
            // override revision from the dependency descriptor
            addAttribute("revision", this.module.getDescriptor().getResolvedModuleRevisionId()
                    .getRevision());
            // now that we have loaded the dependency descriptor, we can put the extra attributes
            // contained in the descriptor too
            addAttributes(this.module.getDescriptor().getResolvedModuleRevisionId()
                    .getQualifiedExtraAttributes());
            addAttributes(this.module.getDescriptor().getResolvedModuleRevisionId()
                    .getExtraAttributes());

            addAttribute("resolved", "true");
        } else {
            addAttribute("resolved", "false");
        }
    }

    public ResolvedModuleRevision getModule() {
        return module;
    }

    /**
     * Returns the time elapsed to resolve the dependency.
     * <p>
     * The time elapsed to resolve a dependency includes the time required to locate the the actual
     * revision if the dependency descriptor use a version constraint, and to download the module
     * metadata if necessary. It doesn't include any conflict management operations nor transitive
     * dependency management. It's basically the time elapsed since the corresponding
     * {@link StartResolveDependencyEvent}
     * </p>
     * 
     * @return the time elapsed to resolve the dependency.
     */
    public long getDuration() {
        return duration;
    }

}
