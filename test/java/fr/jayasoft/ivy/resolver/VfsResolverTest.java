package fr.jayasoft.ivy.resolver;

import junit.framework.TestCase;

public class VfsResolverTest extends TestCase {
	VfsResolver resolver = null;
	
	public void setUp() {
		resolver = new VfsResolver();
	}
	
	public void tearDown() {
		resolver = null;
	}
	
	public void testInit() {
		assertEquals(resolver.getClass(), VfsResolver.class);		
	}
	
	public void testTypeName() {
		assertEquals(resolver.getTypeName(), "vfs");
	}
}
