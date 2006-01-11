/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import java.util.EventListener;

public interface IvyListener extends EventListener {
    public void progress(IvyEvent event);
}
