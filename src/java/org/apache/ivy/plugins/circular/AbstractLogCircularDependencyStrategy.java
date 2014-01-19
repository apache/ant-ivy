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
package org.apache.ivy.plugins.circular;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;

public abstract class AbstractLogCircularDependencyStrategy extends
        AbstractCircularDependencyStrategy {

    protected AbstractLogCircularDependencyStrategy(String name) {
        super(name);
    }

    private Collection/* <String> */circularDependencies = new HashSet();

    public void handleCircularDependency(ModuleRevisionId[] mrids) {
        String circularDependencyId = getCircularDependencyId(mrids);
        if (!circularDependencies.contains(circularDependencyId)) {
            circularDependencies.add(circularDependencyId);
            logCircularDependency(mrids);
        }
    }

    protected abstract void logCircularDependency(ModuleRevisionId[] mrids);

    protected String getCircularDependencyId(ModuleRevisionId[] mrids) {
        String contextPrefix = "";
        ResolveData data = IvyContext.getContext().getResolveData();
        if (data != null) {
            contextPrefix = data.getOptions().getResolveId() + " ";
        }
        return contextPrefix + Arrays.asList(mrids);
    }

}
