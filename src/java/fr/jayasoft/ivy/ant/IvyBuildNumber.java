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
public class IvyBuildNumber extends IvyTask {
	private String _organisation;
	private String _module;
	private String _branch;
	private String _revision;
	
	private String _revSep = ".";
	private String _prefix = "ivy.";
	private String _default = "0";
	private String _defaultBuildNumber = "0";
	
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

	public String getDefault() {
		return _default;
	}

	public void setDefault(String default1) {
		_default = default1;
	}

	public String getPrefix() {
		return _prefix;
	}

	public void setPrefix(String prefix) {
		_prefix = prefix;
	}

	public void execute() throws BuildException {
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy findmodules");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy findmodules");
        }
        if (_prefix == null) {
        	throw new BuildException("null prefix not allowed");
        }
        
        Ivy ivy = getIvyInstance();
        if (_branch == null) {
            ivy.getDefaultBranch(new ModuleId(_organisation, _module));
        }
        if (_revision == null || _revision.length() == 0) {
            _revision = "latest.integration";
        } else if (!_revision.endsWith("+")) {
        	_revision = _revision+"+";
        }
        if (!_prefix.endsWith(".") && _prefix.length() > 0) {
        	_prefix = _prefix+".";
        }
		ResolvedModuleRevision rmr = ivy.findModule(ModuleRevisionId.newInstance(_organisation, _module, _branch, _revision));
		String revision = rmr == null ? null : rmr.getId().getRevision();
		NewRevision newRevision = computeNewRevision(revision);
		setProperty("revision", newRevision.revision);
		setProperty("new.revision", newRevision.newRevision);
		setProperty("build.number", newRevision.buildNumber);
		setProperty("new.build.number", newRevision.newBuildNumber);
	}

	private void setProperty(String propertyName, String value) {
		if (value != null) {
			getProject().setProperty(_prefix+propertyName, value);
		}
	}

	private NewRevision computeNewRevision(String revision) {
		String revPrefix = "latest.integration".equals(_revision)?"":_revision.substring(0, _revision.length() - 1);
		if (revision != null && !revision.startsWith(revPrefix)) {
			throw new BuildException("invalid exception found in repository: '"+revision+"' for '"+revPrefix+"'");
		}
		if (revision == null) {
			if (revPrefix.length() > 0) {
				return new NewRevision(revision, revPrefix+(revPrefix.endsWith(_revSep)?_defaultBuildNumber:_revSep+_defaultBuildNumber), null, _defaultBuildNumber);
			} else {
				Range r = findLastNumber(_default);
				if (r == null) { // no number found
					return new NewRevision(revision, _default, null, null);
				} else {
					long n = Long.parseLong(_default.substring(r.startIndex, r.endIndex));
					return new NewRevision(revision, _default, null, String.valueOf(n));
				}
			}
		}
		Range r;
		if (revPrefix.length() == 0)  {
			r = findLastNumber(revision);
			if (r == null) {
				 return new NewRevision(revision, revision+(revision.endsWith(_revSep)?"1":_revSep+"1"), null, "1");
			}
		} else {
			r = findFirstNumber(revision, revPrefix.length());
			if (r == null) {
				 return new NewRevision(revision, revPrefix+(revPrefix.endsWith(_revSep)?"1":_revSep+"1"), null, "1");
			}
		}
		long n = Long.parseLong(revision.substring(r.startIndex, r.endIndex)) + 1;
		return new NewRevision(
				revision, 
				revision.substring(0, r.startIndex)+n, 
				String.valueOf(n-1), 
				String.valueOf(n));
	}
	
	private Range findFirstNumber(String str, int startIndex) {
		// let's find the first digit in the string
		int startNumberIndex = startIndex;
		while (startNumberIndex<str.length() && !Character.isDigit(str.charAt(startNumberIndex))) {
			startNumberIndex++;
		}
		if (startNumberIndex == str.length()) {
			return null;
		}
		// let's find the end of the number
		int endNumberIndex = startNumberIndex+1;
		while (endNumberIndex<str.length() && Character.isDigit(str.charAt(endNumberIndex))) {
			endNumberIndex++;
		}
		return new Range(startNumberIndex, endNumberIndex);
	}
	
	private Range findLastNumber(String str) {
		int endNumberIndex = str.length() - 1;
		while (endNumberIndex >= 0 && !Character.isDigit(str.charAt(endNumberIndex))) {
			endNumberIndex--;
		}
		int startNumberIndex = endNumberIndex == -1?-1:endNumberIndex - 1;
		while (startNumberIndex >= 0 && Character.isDigit(str.charAt(startNumberIndex))) {
			startNumberIndex--;
		}
		endNumberIndex ++;
		startNumberIndex ++;
		if (startNumberIndex == endNumberIndex) { // no number found
			return null;
		} else {
			return new Range(startNumberIndex, endNumberIndex);
		}
	}

	private static class Range {
		private int startIndex;
		private int endIndex;
		public Range(int startIndex, int endIndex) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
	}

	private static class NewRevision {
		private String revision;
		private String newRevision;
		private String buildNumber;
		private String newBuildNumber;
		public NewRevision(String revision, String newRevision, String buildNumber, String newBuildNumber) {
			this.revision = revision;
			this.newRevision = newRevision;
			this.buildNumber = buildNumber;
			this.newBuildNumber = newBuildNumber;
		}
	}

	public String getRevSep() {
		return _revSep;
	}

	public void setRevSep(String revSep) {
		_revSep = revSep;
	}

	public String getDefaultBuildNumber() {
		return _defaultBuildNumber;
	}

	public void setDefaultBuildNumber(String defaultBuildNumber) {
		_defaultBuildNumber = defaultBuildNumber;
	}
}
