/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import junit.framework.TestCase;

public class IvyPatternHelperTest extends TestCase {
    public void testSubstitute() {
        String pattern = "[organisation]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals(
                "jayasoft/Test/build/archives/jars/test-1.0.jar", 
                IvyPatternHelper.substitute(pattern, "jayasoft", "Test", "1.0", "test", "jar", "jar"));
    }

    public void testOrganization() {
        String pattern = "[organization]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals(
                "jayasoft/Test/build/archives/jars/test-1.0.jar", 
                IvyPatternHelper.substitute(pattern, "jayasoft", "Test", "1.0", "test", "jar", "jar"));
    }

}
