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

import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.core.module.id.ModuleRevisionId;

public class DefaultExtendsDescriptor implements ExtendsDescriptor {

    private ModuleRevisionId parentRevisionId;
    private ModuleRevisionId resolvedParentRevisionId;
    private String location;
    private List extendsTypes;

    public DefaultExtendsDescriptor(ModuleRevisionId parentRevisionId,
                                    ModuleRevisionId resolvedParentRevisionId,
                                    String location, String[] types) {
        this.parentRevisionId = parentRevisionId;
        this.resolvedParentRevisionId = resolvedParentRevisionId;
        this.location = location;
        this.extendsTypes = new ArrayList(types.length);
        for (int i = 0; i < types.length; ++i) {
            extendsTypes.add(types[i]);
        }
    }

    public ModuleRevisionId getParentRevisionId() {
        return parentRevisionId;
    }

    public ModuleRevisionId getResolvedParentRevisionId() {
        return resolvedParentRevisionId;
    }

    public String getLocation() {
        return location;
    }

    public String[] getExtendsTypes() {
        return (String[]) extendsTypes.toArray(new String[extendsTypes.size()]);
    }

    public boolean isAllInherited() {
        return extendsTypes.contains("all");
    }

    public boolean isInfoInherited() {
        return isAllInherited() || extendsTypes.contains("info");
    }

    public boolean isDescriptionInherited() {
        return isAllInherited() || extendsTypes.contains("description");
    }

    public boolean areConfigurationsInherited() {
        return isAllInherited() || extendsTypes.contains("configurations");
    }

    public boolean areDependenciesInherited() {
        return isAllInherited() || extendsTypes.contains("dependencies");
    }
}
