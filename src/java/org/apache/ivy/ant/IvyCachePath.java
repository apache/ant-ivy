/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.util.Iterator;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;


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
        	CacheManager cache = getCacheManager();
            for (Iterator iter = getArtifacts().iterator(); iter.hasNext();) {
            	Artifact a = (Artifact) iter.next();
            	path.createPathElement().setLocation(cache.getArchiveFileInCache(a, cache.getSavedArtifactOrigin(a), isUseOrigin()));
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy path: "+ex, ex);
        }
        
    }

}
