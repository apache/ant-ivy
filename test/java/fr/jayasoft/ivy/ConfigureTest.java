/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import fr.jayasoft.ivy.resolver.IvyRepResolver;

import junit.framework.TestCase;

public class ConfigureTest extends TestCase {
    public void testDefault() throws ParseException, IOException {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        
        assertNotNull(ivy.getDefaultResolver());
    }

    public void testDefaultWithM2Compatibility() throws ParseException, IOException {
        Ivy ivy = new Ivy();
        ivy.setVariable("ivy.default.configuration.m2compatible", "true");
        ivy.configureDefault();
        
        assertNotNull(ivy.getDefaultResolver());
        
        DependencyResolver resolver = ivy.getResolver("public");
        assertNotNull(resolver);
        IvyRepResolver ivyRepResolver = ((IvyRepResolver)resolver);
        assertTrue(ivyRepResolver.isM2compatible());
        List patterns = ivyRepResolver.getIvyPatterns();
        assertEquals(2, patterns.size());
        patterns = ivyRepResolver.getArtifactPatterns();
        assertEquals(1, patterns.size());
    }
}
