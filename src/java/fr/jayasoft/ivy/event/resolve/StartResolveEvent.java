package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;

public class StartResolveEvent extends ResolveEvent {

	public StartResolveEvent(Ivy source, ModuleDescriptor md, String[] confs) {
		super(source, md, confs);
	}

}
