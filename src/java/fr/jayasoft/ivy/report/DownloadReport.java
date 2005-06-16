/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import fr.jayasoft.ivy.Artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author x.hanin
 *
 */
public class DownloadReport {
    private Map _artifacts = new HashMap();
    
    public void addArtifactReport(ArtifactDownloadReport adr) {
        _artifacts.put(adr.getArtifact(), adr);
    }
    
    public ArtifactDownloadReport[] getArtifactsReports() {
        return (ArtifactDownloadReport[])_artifacts.values().toArray(new ArtifactDownloadReport[_artifacts.size()]);
    }

    public ArtifactDownloadReport[] getArtifactsReports(DownloadStatus status) {
        List ret = new ArrayList(_artifacts.size());
        for (Iterator iter = _artifacts.values().iterator(); iter.hasNext();) {
            ArtifactDownloadReport adr = (ArtifactDownloadReport)iter.next();
            if (adr.getDownloadStatus() == status) {
                ret.add(adr);
            }
        }
        return (ArtifactDownloadReport[])ret.toArray(new ArtifactDownloadReport[ret.size()]);
    }

	public ArtifactDownloadReport getArtifactReport(Artifact artifact) {
		return (ArtifactDownloadReport)_artifacts.get(artifact);
	}
}
