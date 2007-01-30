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
package org.apache.ivy.plugins.namespace;

public class MRIDRule {
    private String _org;
    private String _module;
    private String _branch;
    private String _rev;
    public MRIDRule(String org, String mod, String rev) {
        _org = org;
        _module = mod;
        _rev = rev;
    }
    public MRIDRule() {        
    }
    
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrg() {
        return _org;
    }
    public void setOrg(String org) {
        _org = org;
    }
    public String getRev() {
        return _rev;
    }
    public void setRev(String rev) {
        _rev = rev;
    }
    public String toString() {
        return "[ "+_org+" "+_module+(_branch != null?" "+_branch:"")+" "+_rev+" ]";
    }
	public String getBranch() {
		return _branch;
	}
	public void setBranch(String branch) {
		_branch = branch;
	}
}
