package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.matcher.PatternMatcher;
import fr.jayasoft.ivy.util.IvyPatternHelper;

/**
 * Look for modules in the repository matching the given criteria, and sets a set of properties
 * according to what was found.
 * 
 * @author Xavier Hanin
 */
public class IvyListModules extends IvyTask {
	private String _organisation;
	private String _module;
	private String _branch = PatternMatcher.ANY_EXPRESSION;
	private String _revision;
	private String _matcher = PatternMatcher.EXACT_OR_REGEXP;
	
	private String _property;
	private String _value;
	
	public String getMatcher() {
		return _matcher;
	}

	public void setMatcher(String matcher) {
		_matcher = matcher;
	}

	public String getModule() {
		return _module;
	}

	public void setModule(String module) {
		_module = module;
	}

	public String getProperty() {
		return _property;
	}

	public void setProperty(String name) {
		_property = name;
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

	public String getValue() {
		return _value;
	}

	public void setValue(String value) {
		_value = value;
	}

	public String getBranch() {
		return _branch;
	}

	public void setBranch(String branch) {
		_branch = branch;
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
        if (_property == null) {
            throw new BuildException("no property provided for ivy findmodules");
        }
        if (_value == null) {
            throw new BuildException("no value provided for ivy findmodules");
        }
		Ivy ivy = getIvyInstance();
		ModuleRevisionId[] mrids = ivy.listModules(ModuleRevisionId.newInstance(_organisation, _module, _branch, _revision), ivy.getMatcher(_matcher));
		for (int i = 0; i < mrids.length; i++) {
            String name = IvyPatternHelper.substitute(ivy.substitute(_property), mrids[i]);
            String value = IvyPatternHelper.substitute(ivy.substitute(_value), mrids[i]);
            getProject().setProperty(name, value);
		}
	}
}
