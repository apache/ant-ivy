package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleRevisionId;

public class MockVersionMatcher extends AbstractVersionMatcher {

	public MockVersionMatcher() {
	}

	public boolean isDynamic(ModuleRevisionId askedMrid) {
		return false;
	}

	public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
		return false;
	}

}
