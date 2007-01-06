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
package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.status.StatusManager;

public class LatestVersionMatcher  extends AbstractVersionMatcher {
	public LatestVersionMatcher() {
		super("latest");
	}
	
    public boolean isDynamic(ModuleRevisionId askedMrid) {
        return askedMrid.getRevision().startsWith("latest.");
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return true;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return !"latest.integration".equals(askedMrid.getRevision());
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        String askedStatus = askedMrid.getRevision().substring("latest.".length());
        return StatusManager.getCurrent().getPriority(askedStatus) >= StatusManager.getCurrent().getPriority(foundMD.getStatus());
    }
}
