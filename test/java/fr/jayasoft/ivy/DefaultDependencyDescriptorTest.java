
/* ---------------------------------------------------------------- 
 * FILENAME : DefaultDependencyDescriptorTest.java
 * DESCRIPTION : TODO Simple class to demonstrate good coding style. 
 * 		    Optional if javadoc for the class is present
 *
 * PROJECT : Coding standards
 * GROUP : Strategic Trading Initiatives
 *
 * PROGRAMMER : baumkar 
 *
 * COPYRIGHT : Copyright (c) 2005 - The Goldman, Sachs Group, Inc. - All rights reserved
 *
 * Created: Mar 5, 2006
 * Source : $Source: $
 * Revision : $Revision: $
 * Last Changed: $Date: $
 * ---------------------------------------------------------------- */

package fr.jayasoft.ivy;

import junit.framework.TestCase;

public class DefaultDependencyDescriptorTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(DefaultDependencyDescriptorTest.class);
	}

	/*
	 * Test method for 'fr.jayasoft.ivy.DefaultDependencyDescriptor.replaceSelfFallbackPattern(String, String)'
	 */
	public void testReplaceSelfFallbackPattern() {
		String replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("@(default)", "compile");
		assertEquals("compile(default)", replacedConf);
		
		replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("default", "compile");
		assertNull( replacedConf);
		
		replacedConf = DefaultDependencyDescriptor.replaceSelfFallbackPattern("@", "runtime");
		assertEquals("runtime", "runtime");

	}

	/*
	 * Test method for 'fr.jayasoft.ivy.DefaultDependencyDescriptor.replaceThisFallbackPattern(String, String)'
	 */
	public void testReplaceThisFallbackPattern() {
		String replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("#(default)", "compile");
		assertEquals("compile(default)", replacedConf);
		
		replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("default", "compile");
		assertNull( replacedConf);
		
		replacedConf = DefaultDependencyDescriptor.replaceThisFallbackPattern("#", "runtime");
		assertEquals("runtime", "runtime");

	}

}

