/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.LatestStrategy;

public interface HasLatestStrategy {
    public LatestStrategy getLatestStrategy(); 
    public void setLatestStrategy(LatestStrategy latestStrategy);
    public String getLatest();
}
