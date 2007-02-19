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

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.util.filter.Filter;
import org.apache.tools.ant.BuildException;


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
    private boolean _symlink = false;
    
    public String getPattern() {
        return _pattern;
    }
    public void setPattern(String pattern) {
        _pattern = pattern;
    }
    
    public void execute() throws BuildException {
    	prepareAndCheck();

        _pattern = getProperty(_pattern, getSettings(), "ivy.retrieve.pattern");
        try {
        	Filter artifactFilter = getArtifactFilter();
            int targetsCopied = getIvyInstance().retrieve(
            		getResolvedMrid(), 
            		_pattern, 
            		new RetrieveOptions()
            			.setConfs(splitConfs(getConf()))
            			.setCache(CacheManager.getInstance(getIvyInstance().getSettings(), getCache()))
            			.setDestIvyPattern(_ivypattern)
            			.setArtifactFilter(artifactFilter)
            			.setSync(_sync)
            			.setUseOrigin(isUseOrigin())
            			.setMakeSymlinks(_symlink));
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

    /**
     * Option to create symlinks instead of copying.
     */
    public void setSymlink(boolean symlink) {
        _symlink = symlink;
    }
}
