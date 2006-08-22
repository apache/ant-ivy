package fr.jayasoft.ivy.ant;

import java.util.Iterator;
import java.util.Map;

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
public class IvyFindModule extends IvyTask {
	private String _organisation;
	private String _module;
	private String _branch;
	private String _revision;
	
	private String _prefix = "ivy.";
	
	protected String getModule() {
		return _module;
	}

	protected void setModule(String module) {
		_module = module;
	}

	protected String getOrganisation() {
		return _organisation;
	}

	protected void setOrganisation(String organisation) {
		_organisation = organisation;
	}

	protected String getRevision() {
		return _revision;
	}

	protected void setRevision(String revision) {
		_revision = revision;
	}


	protected String getBranch() {
		return _branch;
	}

	protected void setBranch(String branch) {
		_branch = branch;
	}

	protected String getPrefix() {
		return _prefix;
	}

	protected void setPrefix(String prefix) {
		_prefix = prefix;
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
        if (_prefix == null) {
            _prefix = "";
        } else if (!_prefix.endsWith(".") && _prefix.length() != 0) {
        	_prefix = _prefix+".";
        }
		ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(_organisation, _module, _branch, _revision));
		if (rmr != null) {
			Map attributes = rmr.getId().getAttributes();
			for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
				String token = (String) iter.next();
				String value = (String) attributes.get(token);
				if (value != null) {
					getProject().setProperty(_prefix+token, value);
				}
			}
		}
	}
}
