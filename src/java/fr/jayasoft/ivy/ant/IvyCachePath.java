/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;

/**
 * Creates an ant path consisting in all artifacts found during a resolve.
 * 
 * @author Xavier Hanin
 */
public class IvyCachePath extends IvyCacheTask {
    private String _pathid;
	private String _id;

    public String getPathid() {
        return _pathid;
    }
    public void setPathid(String id) {
        _pathid = id;
    }
    
    /**
     * @deprecated use setPathid instead
     * @param id
     */
    public void setId(String id) {
        _id = id;
    }

    public void execute() throws BuildException {
        prepareAndCheck();
        if (_pathid == null) {
        	if (_id != null) {
        		_pathid = _id;
        		log("ID IS DEPRECATED, PLEASE USE PATHID INSTEAD", Project.MSG_WARN);
        	} else {
        		throw new BuildException("pathid is required in ivy classpath");
        	}
        }
        try {
            Path path = new Path(getProject());
            getProject().addReference(_pathid, path);
            Ivy ivy = getIvyInstance();
            for (Iterator iter = getArtifacts().iterator(); iter.hasNext();) {
            	Artifact a = (Artifact) iter.next();
            	path.createPathElement().setLocation(ivy.getArchiveFileInCache(getCache(), a, ivy.getSavedArtifactOrigin(getCache(), a), isUseOrigin()));
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy path: "+ex, ex);
        }
        
    }

}
