/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactOrigin;

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
