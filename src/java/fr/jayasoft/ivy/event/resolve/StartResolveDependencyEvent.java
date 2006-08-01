package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;

public class StartResolveDependencyEvent extends ResolveDependencyEvent {
	public static final String NAME = "pre-resolve-dependency";

	public StartResolveDependencyEvent(Ivy source, DependencyResolver resolver, DependencyDescriptor dd) {
		super(source, NAME, resolver, dd);
	}

}
