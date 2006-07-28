/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event;

import fr.jayasoft.ivy.Ivy;


public class IvyEvent {
    private Ivy _source;

	public IvyEvent(Ivy source) {
		_source = source;
	}

	public Ivy getSource() {
		return _source;
	}
    
    
}
