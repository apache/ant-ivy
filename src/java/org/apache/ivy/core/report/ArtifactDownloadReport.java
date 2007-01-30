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
package org.apache.ivy.core.report;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;

/**
 * @author x.hanin
 *
 */
public class ArtifactDownloadReport {
    private Artifact _artifact;
    private ArtifactOrigin _origin;
    private DownloadStatus _downloadStatus;
    private long _size;
    
    public ArtifactDownloadReport(Artifact artifact) {
    	_artifact = artifact;
    }
    public DownloadStatus getDownloadStatus() {
        return _downloadStatus;
    }
    public void setDownloadStatus(DownloadStatus downloadStatus) {
        _downloadStatus = downloadStatus;
    }
    public String getName() {
        return _artifact.getName();
    }
    public String getType() {
        return _artifact.getType();
    }
	public Artifact getArtifact() {
		return _artifact;
	}
    public String getExt() {
        return _artifact.getExt();
    }
    public long getSize() {
        return _size;
    }
    
    public void setSize(long size) {
        _size = size;
    }
	public void setArtifactOrigin(ArtifactOrigin origin) {
		_origin = origin;
	}
	public ArtifactOrigin getArtifactOrigin() {
		return _origin;
	}
}
