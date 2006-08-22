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
	
	protected String getMatcher() {
		return _matcher;
	}

	protected void setMatcher(String matcher) {
		_matcher = matcher;
	}

	protected String getModule() {
		return _module;
	}

	protected void setModule(String module) {
		_module = module;
	}

	protected String getProperty() {
		return _property;
	}

	protected void setProperty(String name) {
		_property = name;
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

	protected String getValue() {
		return _value;
	}

	protected void setValue(String value) {
		_value = value;
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

	protected String getBranch() {
		return _branch;
	}

	protected void setBranch(String branch) {
		_branch = branch;
	}
}
