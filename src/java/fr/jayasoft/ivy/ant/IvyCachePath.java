/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ant.IvyCacheTask.PathEntry;
import fr.jayasoft.ivy.filter.Filter;

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
    /**
     * @deprecated use setPathid instead
     * @param id
     */
    public void setId(String id) {
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
            throw new BuildException("impossible to build ivy path: "+ex.getMessage(), ex);
        }
        
    }
    
    protected void addPath(List paths, Artifact artifact, Ivy ivy) throws IOException {
    	if (!useOrigin) {
    		paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
    	} else {
    		File originFile = ivy.getOriginFileInCache(getCache(), artifact);
    		if (!originFile.exists()) {
    			paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
    		} else {
    			Properties originProperties = new Properties();
                FileInputStream originInputStream = new FileInputStream(originFile);
                try {
                    originProperties.load(originInputStream);
                } finally {
                    originInputStream.close();
                }
                boolean isOriginLocal = Boolean.valueOf(originProperties.getProperty("isLocal")).booleanValue();
                if (isOriginLocal) {
                	String originName = originProperties.getProperty("name");
                	paths.add(new PathEntry(originName, false));
                } else {
                	paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
                }
    		}
    	}
    }

}
