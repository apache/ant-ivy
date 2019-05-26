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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public final class CircularDependencyHelper {

    /** CircularDependencyHelper is not designed to be an instance */
    private CircularDependencyHelper() {
    }

    /**
     * Returns a string representation of this circular dependency graph
     *
     * @param mrids
     *            in order of circular dependency
     * @return a string representation of this circular dependency graph
     */
    public static String formatMessage(final ModuleRevisionId[] mrids) {
        Set<ModuleRevisionId> alreadyAdded = new HashSet<>();
        StringBuilder buff = new StringBuilder();
        for (ModuleRevisionId mrid : mrids) {
            if (buff.length() > 0) {
                buff.append("->");
            }
            if (alreadyAdded.add(mrid)) {
                buff.append(mrid);
            } else {
                buff.append("...");
                break;
            }
        }
        return buff.toString();
    }

    public static String formatMessage(final ModuleDescriptor[] descriptors) {
        return formatMessageFromDescriptors(Arrays.asList(descriptors));
    }

    /**
     * @param loopElements
     *            a List&lt;ModuleDescriptor&gt;
     * @return String
     */
    public static String formatMessageFromDescriptors(List<ModuleDescriptor> loopElements) {
        List<ModuleRevisionId> mrids = new LinkedList<>();
        for (ModuleDescriptor descriptor : loopElements) {
            mrids.add(descriptor.getModuleRevisionId());
        }
        return formatMessage(mrids.toArray(new ModuleRevisionId[mrids.size()]));
    }

}
