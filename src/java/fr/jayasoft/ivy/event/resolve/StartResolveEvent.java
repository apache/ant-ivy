package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;

public class StartResolveEvent extends ResolveEvent {
	public static final String NAME = "pre-resolve";

	public StartResolveEvent(Ivy source, ModuleDescriptor md, String[] confs) {
		super(source, NAME, md, confs);
	}

}
