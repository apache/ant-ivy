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
package org.apache.ivy.core.search;

import org.apache.ivy.plugins.resolver.DependencyResolver;




public class RevisionEntry {
    private ModuleEntry _moduleEntry;
    private String _revision;

    public RevisionEntry(ModuleEntry mod, String name) {
        _moduleEntry = mod;
        _revision = name;
    }

    public ModuleEntry getModuleEntry() {
        return _moduleEntry;
    }
    

    public String getRevision() {
        return _revision;
    }

    public String getModule() {
        return _moduleEntry.getModule();
    }

    public String getOrganisation() {
        return _moduleEntry.getOrganisation();
    }

    public OrganisationEntry getOrganisationEntry() {
        return _moduleEntry.getOrganisationEntry();
    }

    public DependencyResolver getResolver() {
        return _moduleEntry.getResolver();
    }
    
}
