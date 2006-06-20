package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.latest.LatestRevisionStrategy;
import junit.framework.TestCase;

public class VersionRangeMatcherTest extends TestCase {
	VersionMatcher _vm = new VersionRangeMatcher("range", new LatestRevisionStrategy());
	
	public VersionRangeMatcherTest() {
	}
	
	public void testDynamic() {
		assertDynamic("lastest.integration", false);
		assertDynamic("[1.0]", false);
		assertDynamic("(1.0)", false);
		assertDynamic("(1.0,2.0)", false);
		assertDynamic("[1.0;2.0]", false);

		assertDynamic("[1.0,2.0]", true);
		assertDynamic("[1.0,2.0[", true);
		assertDynamic("]1.0,2.0[", true);
		assertDynamic("]1.0,2.0]", true);
		assertDynamic("[1.0,)", true);
		assertDynamic("(,1.0]", true);
	}

	public void testIncludingFinite() {
		assertAccept("[1.0,2.0]", "1.1", true);
		assertAccept("[1.0,2.0]", "0.9", false);
		assertAccept("[1.0,2.0]", "2.1", false);
		assertAccept("[1.0,2.0]", "1.0", true);
		assertAccept("[1.0,2.0]", "2.0", true);
	}
	
	public void testExcludingFinite() {
		assertAccept("]1.0,2.0[", "1.1", true);
		assertAccept("]1.0,2.0[", "0.9", false);
		assertAccept("]1.0,2.0[", "2.1", false);
		
		assertAccept("]1.0,2.0]", "1.0", false);
		assertAccept("]1.0,2.0[", "1.0", false);
		assertAccept("[1.0,2.0[", "1.0", true);
		
		assertAccept("[1.0,2.0[", "2.0", false);
		assertAccept("]1.0,2.0[", "2.0", false);
		assertAccept("]1.0,2.0]", "2.0", true);
	}
	
	public void testIncludingInfinite() {
		assertAccept("[1.0,)", "1.1", true);
		assertAccept("[1.0,)", "2.0", true);
		assertAccept("[1.0,)", "3.5.6", true);
		assertAccept("[1.0,)", "1.0", true);
		
		assertAccept("[1.0,)", "0.9", false);

		assertAccept("(,2.0]", "1.1", true);
		assertAccept("(,2.0]", "0.1", true);
		assertAccept("(,2.0]", "0.2.4", true);
		assertAccept("(,2.0]", "2.0", true);
		
		assertAccept("(,2.0]", "2.3", false);
	}
	
	public void testExcludingInfinite() {
		assertAccept("]1.0,)", "1.1", true);
		assertAccept("]1.0,)", "2.0", true);
		assertAccept("]1.0,)", "3.5.6", true);

		assertAccept("]1.0,)", "1.0", false);
		assertAccept("]1.0,)", "0.9", false);

		assertAccept("(,2.0[", "1.1", true);
		assertAccept("(,2.0[", "0.1", true);
		assertAccept("(,2.0[", "0.2.4", true);
		
		assertAccept("(,2.0[", "2.0", false);
		assertAccept("(,2.0[", "2.3", false);
	}
	
	
	// assertion helper methods

	private void assertDynamic(String askedVersion, boolean b) {
		assertEquals(b, _vm.isDynamic(ModuleRevisionId.newInstance("org", "name", askedVersion)));
	}

	private void assertAccept(String askedVersion, String depVersion, boolean b) {
		assertEquals(b, _vm.accept(
				ModuleRevisionId.newInstance("org", "name", askedVersion),
				ModuleRevisionId.newInstance("org", "name", depVersion)));
	}
}
