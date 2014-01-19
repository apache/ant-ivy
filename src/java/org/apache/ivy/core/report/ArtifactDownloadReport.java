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

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;

/**
 * Report on the download of an artifact from a repository to a local (cached) file.
 * <p>
 * Note that depending on cache implementation, the artifact may not be actually downloaded, but
 * used directly from its original location.
 * </p>
 */
public class ArtifactDownloadReport {
    /**
     * download details used when the download "fails" when the artifact is simply missing on the
     * remote repository.
     * <p>
     * For historical reason the status can't be used to distinguish a real failure from a missing
     * artifact by using the status, in both cases it's DownloadStatus.FAILED. The details message
     * can be used for this purpose though.
     * </p>
     */
    public static final String MISSING_ARTIFACT = "missing artifact";

    private Artifact artifact;

    private ArtifactOrigin origin;

    private File localFile;

    private DownloadStatus downloadStatus;

    private long size;

    private String downloadDetails = "";

    private long downloadTimeMillis;

    private File unpackedLocalFile;

    public ArtifactDownloadReport(Artifact artifact) {
        this.artifact = artifact;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public String getName() {
        return artifact.getName();
    }

    /**
     * 
     * @return the type of the downloaded artifact
     */
    public String getType() {
        return artifact.getType();
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public String getExt() {
        return artifact.getExt();
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setArtifactOrigin(ArtifactOrigin origin) {
        this.origin = origin;
    }

    public ArtifactOrigin getArtifactOrigin() {
        return origin;
    }

    public void setDownloadDetails(String message) {
        downloadDetails = message;
    }

    public String getDownloadDetails() {
        return downloadDetails;
    }

    public void setDownloadTimeMillis(long l) {
        downloadTimeMillis = l;
    }

    public long getDownloadTimeMillis() {
        return downloadTimeMillis;
    }

    public String toString() {
        if (downloadStatus == DownloadStatus.SUCCESSFUL) {
            return "[SUCCESSFUL ] " + artifact + " (" + downloadTimeMillis + "ms)";
        } else if (downloadStatus == DownloadStatus.FAILED) {
            if (downloadDetails == MISSING_ARTIFACT) {
                return "[NOT FOUND  ] " + artifact + " (" + downloadTimeMillis + "ms)";
            } else {
                return "[FAILED     ] " + artifact + ": " + downloadDetails + " ("
                        + downloadTimeMillis + "ms)";
            }
        } else if (downloadStatus == DownloadStatus.NO) {
            return "[NOT REQUIRED] " + artifact;
        } else {
            return super.toString();
        }
    }

    /**
     * Returns the File where the artifact is available on the local filesystem, or
     * <code>null</code> if and only if the artifact caching failed.
     * 
     * @return the file where the artifact is now available on the local filesystem.
     */
    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public boolean isDownloaded() {
        return DownloadStatus.SUCCESSFUL == downloadStatus;
    }

    public void setUnpackedLocalFile(File unpackedLocalFile) {
        this.unpackedLocalFile = unpackedLocalFile;
    }

    public File getUnpackedLocalFile() {
        return unpackedLocalFile;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArtifactDownloadReport other = (ArtifactDownloadReport) obj;
        if (artifact == null) {
            if (other.artifact != null) {
                return false;
            }
        } else if (!artifact.equals(other.artifact)) {
            return false;
        }
        return true;
    }

}
