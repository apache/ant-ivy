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

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.DependencyResolver;

public class ResolveDependencyEvent extends IvyEvent {
    private DependencyResolver resolver;

    private DependencyDescriptor dd;

    protected ResolveDependencyEvent(String name, DependencyResolver resolver,
            DependencyDescriptor dd, ModuleRevisionId requestedRevisionId) {
        super(name);
        this.resolver = resolver;
        this.dd = dd;
        addAttribute("resolver", this.resolver.getName());
        addMridAttributes(this.dd.getDependencyRevisionId());
        addAttributes(this.dd.getQualifiedExtraAttributes());
        addAttributes(this.dd.getExtraAttributes());
        addAttribute("req-revision", requestedRevisionId.getRevision());
        addAttribute("req-revision-default", dd.getDependencyRevisionId().getRevision());
        addAttribute("req-revision-dynamic", dd.getDynamicConstraintDependencyRevisionId()
                .getRevision());
        addAttribute("req-branch", requestedRevisionId.getBranch());
        addAttribute("req-branch-default", dd.getDependencyRevisionId().getBranch());
    }

    public DependencyDescriptor getDependencyDescriptor() {
        return dd;
    }

    public DependencyResolver getResolver() {
        return resolver;
    }

}
