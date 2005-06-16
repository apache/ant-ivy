/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.Date;


public interface LatestStrategy {
    /**
     * Finds the latest artifact among the given artifacts info.
     * The definition of 'latest' depends on the strategy itself.
     * Given artifacts info are all good candidate. If the given date is not
     * null, then found artifact should not be later than this date. 
     * 
     * @param infos
     * @param date
     * @return the latest artifact among the given ones.
     */
    ArtifactInfo findLatest(ArtifactInfo[] infos, Date date);
    String getName();
}
