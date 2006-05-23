package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.ResolvedModuleRevision;
import fr.jayasoft.ivy.repository.Resource;

public class MDResolvedResource extends ResolvedResource {
	private ResolvedModuleRevision _rmr;

	public MDResolvedResource(Resource res, String rev, ResolvedModuleRevision rmr) {
		super(res, rev);
		_rmr = rmr;
	}
	
	public ResolvedModuleRevision getResolvedModuleRevision() {
		return _rmr;
	}

}
