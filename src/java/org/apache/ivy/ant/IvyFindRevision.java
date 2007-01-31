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
package org.apache.ivy.ant;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;


/**
 * Look for the latest module in the repository matching the given criteria, 
 * and sets a set of properties according to what was found.
 * 
 * @author Xavier Hanin
 */
public class IvyFindRevision extends IvyTask {
	private String _organisation;
	private String _module;
	private String _branch;
	private String _revision;
	
	private String _property = "ivy.revision";
	
	public String getModule() {
		return _module;
	}

	public void setModule(String module) {
		_module = module;
	}

	public String getOrganisation() {
		return _organisation;
	}

	public void setOrganisation(String organisation) {
		_organisation = organisation;
	}

	public String getRevision() {
		return _revision;
	}

	public void setRevision(String revision) {
		_revision = revision;
	}


	public String getBranch() {
		return _branch;
	}

	public void setBranch(String branch) {
		_branch = branch;
	}

	public String getProperty() {
		return _property;
	}

	public void setProperty(String prefix) {
		_property = prefix;
	}


	public void execute() throws BuildException {
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy findmodules");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy findmodules");
        }
        if (_revision == null) {
            throw new BuildException("no revision provided for ivy findmodules");
        }
        
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (_branch == null) {
            settings.getDefaultBranch(new ModuleId(_organisation, _module));
        }
		ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(_organisation, _module, _branch, _revision));
		if (rmr != null) {
			getProject().setProperty(_property, rmr.getId().getRevision());
		}
	}
}
