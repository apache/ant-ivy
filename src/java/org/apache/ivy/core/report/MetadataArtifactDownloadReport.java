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

import java.io.File;

import org.apache.ivy.core.module.descriptor.Artifact;

public class MetadataArtifactDownloadReport extends ArtifactDownloadReport {
    private boolean isSearched;

    private File originalLocalFile;

    public MetadataArtifactDownloadReport(Artifact artifact) {
        super(artifact);
    }

    /**
     * Returns <code>true</code> if the resolution of this metadata artifact required at least one
     * access to the repository, or <code>false</code> if only provisioned data was used.
     * 
     * @return <code>true</code> if the resolution of this metadata artifact required at least one
     *         access to the repository
     */
    public boolean isSearched() {
        return isSearched;
    }

    public void setSearched(boolean isSearched) {
        this.isSearched = isSearched;
    }

    /**
     * Returns the location on the local filesystem where the original metadata artifact is
     * provisioned, or <code>null</code> if the provisioning failed.
     * 
     * @return the location on the local filesystem where the original metadata artifact is
     *         provisioned.
     */
    public File getOriginalLocalFile() {
        return originalLocalFile;
    }

    public void setOriginalLocalFile(File originalLocalFile) {
        this.originalLocalFile = originalLocalFile;
    }

}
