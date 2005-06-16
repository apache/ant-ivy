/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.latest;

import java.util.Date;

import fr.jayasoft.ivy.ArtifactInfo;


public class LatestTimeStrategy extends AbstractLatestStrategy {
    public LatestTimeStrategy() {
        setName("latest-time");
    }
    public ArtifactInfo findLatest(ArtifactInfo[] artifacts, Date date) {
        if (artifacts == null) {
            return null;
        }
        ArtifactInfo found = null;
        long foundDate = 0;
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactInfo art = artifacts[i];
            long lastModified = art.getLastModified();
            if (lastModified > foundDate && (date == null || lastModified <= date.getTime())) {
                foundDate = lastModified;
                found = art;
            }
        } 
        return found;
    }
}
