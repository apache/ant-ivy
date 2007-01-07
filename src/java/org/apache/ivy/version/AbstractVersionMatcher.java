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
package org.apache.ivy.version;

import org.apache.ivy.Ivy;
import org.apache.ivy.IvyAware;
import org.apache.ivy.ModuleDescriptor;
import org.apache.ivy.ModuleRevisionId;

public abstract class AbstractVersionMatcher implements VersionMatcher, IvyAware {
	private String _name;
	private Ivy _ivy;
	
	public AbstractVersionMatcher() {
	}

	public AbstractVersionMatcher(String name) {
		_name = name;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}


    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        return accept(askedMrid, foundMD.getResolvedModuleRevisionId());
    }
    
    public String toString() {
    	return getName();
    }

	public Ivy getIvy() {
		return _ivy;
	}

	public void setIvy(Ivy ivy) {
		_ivy = ivy;
	}

}
