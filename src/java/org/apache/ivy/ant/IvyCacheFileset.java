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
import java.util.List;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;

/**
 * Creates an ant fileset consisting in all artifacts found during a resolve. Note that this task is
 * not compatible with the useOrigin mode.
 */
public class IvyCacheFileset extends IvyCacheTask {
    private String setid;

    public String getSetid() {
        return setid;
    }

    public void setSetid(String id) {
        setid = id;
    }

    public void setUseOrigin(boolean useOrigin) {
        if (useOrigin) {
            throw new UnsupportedOperationException(
                    "the cachefileset task does not support the useOrigin mode, since filesets "
                    + "require to have only one root directory. Please use the the cachepath "
                    + "task instead");
        }
    }

    public void doExecute() throws BuildException {
        prepareAndCheck();
        if (setid == null) {
            throw new BuildException("setid is required in ivy cachefileset");
        }
        try {
            FileSet fileset = new FileSet();
            fileset.setProject(getProject());
            getProject().addReference(setid, fileset);
            fileset.setDir(getCache());

            List paths = getArtifacts();
            if (paths.isEmpty()) {
                NameEntry ne = fileset.createExclude();
                ne.setName("**/*");
            } else {
                CacheManager cache = getCacheManager();
                for (Iterator iter = paths.iterator(); iter.hasNext();) {
                    Artifact a = (Artifact) iter.next();
                    NameEntry ne = fileset.createInclude();
                    ne.setName(cache.getArchivePathInCache(a, cache.getSavedArtifactOrigin(a)));
                }
            }
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy cache fileset: " + ex, ex);
        }
    }

}
