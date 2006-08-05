/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactOrigin;
import fr.jayasoft.ivy.Ivy;

public class IvyCachePath extends IvyCacheTask {
    private String _pathid;
    private boolean useOrigin = false;
    
    public boolean isUseOrigin() {
    	return useOrigin;
    }
    
    public void setUseOrigin(boolean useOrigin) {
    	this.useOrigin = useOrigin;
    }

    public String getPathid() {
        return _pathid;
    }
    public void setPathid(String id) {
        _pathid = id;
    }

    public void execute() throws BuildException {
        prepareAndCheck();
        if (_pathid == null) {
            throw new BuildException("pathid is required in ivy classpath");
        }
        try {
            Path path = new Path(getProject());
            getProject().addReference(_pathid, path);
            for (Iterator iter = getPaths().iterator(); iter.hasNext();) {
            	PathEntry p = (PathEntry) iter.next();
            	if (p.isRelativeToCache()) {
            		path.createPathElement().setLocation(new File(getCache(), p.getLocation()));
            	} else {
            		path.createPathElement().setLocation(new File(p.getLocation()));
            	}
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy path: "+ex, ex);
        }
        
    }
    
    protected void addPath(List paths, Artifact artifact, Ivy ivy) throws IOException {
    	if (!useOrigin) {
    		paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
    	} else {
    		ArtifactOrigin origin = ivy.getSavedArtifactOrigin(getCache(), artifact);
    		if (origin == null) {
    			paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
    		} else {
                if (origin.isLocal()) {
                	paths.add(new PathEntry(origin.getLocation(), false));
                } else {
                	paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
                }
    		}
    	}
    }

}
