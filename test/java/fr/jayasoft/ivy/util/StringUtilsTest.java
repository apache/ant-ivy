package fr.jayasoft.ivy.util;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {
	public void testEncryption() {
		assertEquals("jayasoft", StringUtils.decrypt(StringUtils.encrypt("jayasoft")));
		assertEquals("yet another string with 126 digits and others :;%_-$& characters", StringUtils.decrypt(StringUtils.encrypt("yet another string with 126 digits and others :;%_-$& characters")));
		
		assertFalse("jayasoft".equals(StringUtils.encrypt("jayasoft")));
	}
}
