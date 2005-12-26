/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

public class IvyCachePath extends IvyCacheTask {
    private String _pathid;

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
        if (_pathid == null) {
            throw new BuildException("pathid is required in ivy classpath");
        }
        try {
            Path path = new Path(getProject());
            getProject().addReference(_pathid, path);
            for (Iterator iter = getPaths().iterator(); iter.hasNext();) {
                String p = (String)iter.next();
                path.createPathElement().setLocation(new File(getCache(), p));
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy path: "+ex.getMessage(), ex);
        }
        
    }

}
