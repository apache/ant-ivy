/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class IvyPatternHelperTest extends TestCase {
    public void testSubstitute() {
        String pattern = "[organisation]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals(
                "jayasoft/Test/build/archives/jars/test-1.0.jar", 
                IvyPatternHelper.substitute(pattern, "jayasoft", "Test", "1.0", "test", "jar", "jar"));
    }

    public void testCyclicSubstitute() {
        String pattern = "${var}";
        Map variables = new HashMap();
        variables.put("var", "${othervar}");
        variables.put("othervar", "${var}");
        try {
            IvyPatternHelper.substituteVariables(pattern, variables);
            fail("cyclic var should raise an exception");
        } catch (Exception ex) {
            // ok
        } catch (Error er) {
            fail("cyclic var shouldn't raise an error: "+er);
        }
    }

    public void testOptionalSubstitute() {
        Map tokens = new HashMap();
        tokens.put("token", "");
        tokens.put("othertoken", "myval");
        assertEquals("test-myval", IvyPatternHelper.substituteTokens("test(-[token])(-[othertoken])", tokens));
        tokens.put("token", "val");
        assertEquals("test-val-myval", IvyPatternHelper.substituteTokens("test(-[token])(-[othertoken])", tokens));
    }

    public void testOrganization() {
        String pattern = "[organization]/[module]/build/archives/[type]s/[artifact]-[revision].[ext]";
        assertEquals(
                "jayasoft/Test/build/archives/jars/test-1.0.jar", 
                IvyPatternHelper.substitute(pattern, "jayasoft", "Test", "1.0", "test", "jar", "jar"));
    }

}
