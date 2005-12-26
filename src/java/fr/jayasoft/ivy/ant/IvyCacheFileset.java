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

public class IvyCacheFileset extends IvyCacheTask {
    private String _setid;

    public String getSetid() {
        return _setid;
    }
    public void setSetid(String id) {
        _setid = id;
    }

    public void execute() throws BuildException {
        if (_setid == null) {
            throw new BuildException("setid is required in ivy cachefileset");
        }
        try {
            FileSet fileset = new FileSet();
            fileset.setProject(getProject());
            getProject().addReference(_setid, fileset);
            fileset.setDir(getCache());
            
            List paths = getPaths();
            if (paths.isEmpty()) {
                NameEntry ne = fileset.createExclude();
                ne.setName("**/*");
            } else {
                for (Iterator iter = paths.iterator(); iter.hasNext();) {
                    String p = (String)iter.next();
                    NameEntry ne = fileset.createInclude();
                    ne.setName(p);
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy cache fileset: "+ex.getMessage(), ex);
        }        
    }

}
