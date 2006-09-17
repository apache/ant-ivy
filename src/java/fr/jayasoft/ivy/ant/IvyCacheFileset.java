/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;


/**
 * Creates an ant fileset consisting in all artifacts found during a resolve.
 * Note that this task is not compatible with the useOrigin mode.
 * 
 * @author Xavier Hanin 
 */
public class IvyCacheFileset extends IvyCacheTask {
    private String _setid;

    public String getSetid() {
        return _setid;
    }
    public void setSetid(String id) {
        _setid = id;
    }
    public void setUseOrigin(boolean useOrigin) {
    	if (useOrigin) {
    		throw new UnsupportedOperationException("the cachefileset task does not support the useOrigin mode, since filesets require to have only one root directory. Please use the the cachepath task instead");
    	}
    }

    public void execute() throws BuildException {
        prepareAndCheck();
        if (_setid == null) {
            throw new BuildException("setid is required in ivy cachefileset");
        }
        try {
            FileSet fileset = new FileSet();
            fileset.setProject(getProject());
            getProject().addReference(_setid, fileset);
            fileset.setDir(getCache());
            
            List paths = getArtifacts();
            if (paths.isEmpty()) {
                NameEntry ne = fileset.createExclude();
                ne.setName("**/*");
            } else {
            	Ivy ivy = getIvyInstance();
                for (Iterator iter = paths.iterator(); iter.hasNext();) {
                	Artifact a = (Artifact)iter.next();
                    NameEntry ne = fileset.createInclude();
                    ne.setName(ivy.getArchivePathInCache(a, ivy.getSavedArtifactOrigin(getCache(), a)));
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy cache fileset: "+ex, ex);
        }        
    }

}
