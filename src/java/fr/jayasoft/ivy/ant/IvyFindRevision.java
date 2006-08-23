package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.ResolvedModuleRevision;

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
        if (_branch == null) {
            ivy.getDefaultBranch(new ModuleId(_organisation, _module));
        }
		ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(_organisation, _module, _branch, _revision));
		if (rmr != null) {
			getProject().setProperty(_property, rmr.getId().getRevision());
		}
	}
}
