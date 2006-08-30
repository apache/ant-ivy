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

	protected ResolveEvent(Ivy source, String name, ModuleDescriptor md, String[] confs) {
		super(source, name);
		_md = md;
		_confs = confs;
		addMDAttributes(md);
		addConfsAttribute(confs);
	}

	public ModuleDescriptor getModuleDescriptor() {
		return _md;
	}
}
