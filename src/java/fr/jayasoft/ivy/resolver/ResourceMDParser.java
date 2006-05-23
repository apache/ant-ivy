package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.Resource;

public interface ResourceMDParser {
	MDResolvedResource parse(Resource resource, String rev);
}
