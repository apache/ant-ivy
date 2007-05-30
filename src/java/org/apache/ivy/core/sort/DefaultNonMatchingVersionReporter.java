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
package org.apache.ivy.core.sort;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.Message;

/**
 * A default implementation of the reporter used in the sort.  The reporting is isolated here to make
 * it easier to test, and to have a place where adding different type of reporting (silent, warning, exceptions) 
 */
public class DefaultNonMatchingVersionReporter implements NonMatchingVersionReporter {

	public void reportNonMatchingVersion(DependencyDescriptor descriptor, ModuleDescriptor md) {
    	ModuleRevisionId dependencyRevisionId = descriptor.getDependencyRevisionId();
    	ModuleRevisionId parentRevisionId = descriptor.getParentRevisionId();
    	if (parentRevisionId==null) {
    		//There are some rare case where DependencyDescriptor have no parent.  
    		//This is should not be used in the SortEngine, but if it is, we show a decent trace.
    		Message.warn("Non matching revision detected.  Dependency " + dependencyRevisionId +
        			" doesn't match " + md);
    	} else {
    		ModuleId parentModuleId = parentRevisionId.getModuleId(); 
    		Message.warn("Non matching revision detected.  " + parentModuleId + " depends on " 
    				+ dependencyRevisionId + ", doesn't match " + md);
    	}

	}

}
