/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.filter.Filter;

/**
 * This task allow to retrieve dependencies from the cache to a local directory like a lib dir.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyRetrieve extends IvyPostResolveTask {
    private String _pattern;
    private String _ivypattern = null;
    private boolean _sync = false;
    
    public String getPattern() {
        return _pattern;
    }
    public void setPattern(String pattern) {
        _pattern = pattern;
    }
    
    public void execute() throws BuildException {
    	prepareAndCheck();

        _pattern = getProperty(_pattern, getIvyInstance(), "ivy.retrieve.pattern");
        try {
        	Filter artifactFilter = getArtifactFilter();
            int targetsCopied = getIvyInstance().retrieve(getResolvedModuleId(), splitConfs(getConf()), getCache(), _pattern, _ivypattern, artifactFilter, _sync, isUseOrigin());
            boolean haveTargetsBeenCopied = targetsCopied > 0;
            getProject().setProperty("ivy.nb.targets.copied", String.valueOf(targetsCopied));
            getProject().setProperty("ivy.targets.copied", String.valueOf(haveTargetsBeenCopied));
        } catch (Exception ex) {
            throw new BuildException("impossible to ivy retrieve: "+ex, ex);
        }
    }
    public String getIvypattern() {
        return _ivypattern;
    }
    public void setIvypattern(String ivypattern) {
        _ivypattern = ivypattern;
    }
	public boolean isSync() {
		return _sync;
	}
	public void setSync(boolean sync) {
		_sync = sync;
	}
    
}
