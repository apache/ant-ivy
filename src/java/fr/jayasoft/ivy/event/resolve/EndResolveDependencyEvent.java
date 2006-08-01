package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ResolvedModuleRevision;

public class EndResolveDependencyEvent extends ResolveDependencyEvent {
	public static final String NAME = "post-resolve-dependency";
	private ResolvedModuleRevision _module;

	public EndResolveDependencyEvent(Ivy source, DependencyResolver resolver, DependencyDescriptor dd, ResolvedModuleRevision module) {
		super(source, NAME, resolver, dd);
		_module = module;
		if (_module != null) {
			// override revision from the dependency descriptor
			addAttribute("revision", _module.getDescriptor().getResolvedModuleRevisionId().getRevision());
			addAttribute("resolved", "true");
		} else {
			addAttribute("resolved", "false");
		}
	}

	public ResolvedModuleRevision getModule() {
		return _module;
	}

}
