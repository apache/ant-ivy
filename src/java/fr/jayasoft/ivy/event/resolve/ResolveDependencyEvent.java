/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.event.IvyEvent;

public class ResolveDependencyEvent extends IvyEvent {
	private DependencyResolver _resolver;
	private DependencyDescriptor _dd;

	protected ResolveDependencyEvent(Ivy source, String name, DependencyResolver resolver, DependencyDescriptor dd) {
		super(source, name);
		_resolver = resolver;
		_dd = dd;
		addAttribute("resolver", _resolver.getName());
		addMridAttributes(_dd.getDependencyRevisionId());
		addAttributes(_dd.getExtraAttributes());
	}

	public DependencyDescriptor getDependencyDescriptor() {
		return _dd;
	}

	public DependencyResolver getResolver() {
		return _resolver;
	}

}
