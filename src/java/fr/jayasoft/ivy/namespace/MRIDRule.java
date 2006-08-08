/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.namespace;

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
