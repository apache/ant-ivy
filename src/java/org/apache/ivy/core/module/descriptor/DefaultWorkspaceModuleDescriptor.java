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

import java.util.Date;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.repository.Resource;

public class DefaultWorkspaceModuleDescriptor extends DefaultModuleDescriptor implements
        WorkspaceModuleDescriptor {

    public DefaultWorkspaceModuleDescriptor(ModuleDescriptorParser parser, Resource res) {
        super(parser, res);
    }

    public DefaultWorkspaceModuleDescriptor(ModuleRevisionId id, String status, Date pubDate) {
        super(id, status, pubDate);
    }

    public DefaultWorkspaceModuleDescriptor(ModuleRevisionId id, String status, Date pubDate,
            boolean isDefault) {
        super(id, status, pubDate, isDefault);
    }

}
