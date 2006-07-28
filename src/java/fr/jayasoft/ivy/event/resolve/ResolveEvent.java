/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.event.IvyEvent;

public class ResolveEvent extends IvyEvent {
	private ModuleDescriptor _md;
	private String[] _confs;

	public ResolveEvent(Ivy source, ModuleDescriptor md, String[] confs) {
		super(source);
		_md = md;
		_confs = confs;
	}

	public ModuleDescriptor getModuleDescriptor() {
		return _md;
	}
}
