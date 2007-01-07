package org.apache.ivy.resolver;

import org.apache.ivy.ResolvedModuleRevision;
import org.apache.ivy.repository.Resource;

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
