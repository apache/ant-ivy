/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.IOException;
import java.text.ParseException;

import junit.framework.TestCase;

public class ConfigureTest extends TestCase {
    public void testDefault() throws ParseException, IOException {
        Ivy ivy = new Ivy();
        ivy.configureDefault();
        
        assertNotNull(ivy.getDefaultResolver());
    }
    
    public void testTypedefWithCustomClasspath() throws Exception {
        Ivy ivy = new Ivy();
        ivy.configure(ConfigureTest.class.getResource("ivyconf-custom-typedef.xml"));
        
        DependencyResolver custom = ivy.getResolver("custom");
        assertNotNull(custom);
        assertEquals("fr.jayasoft.ivy.resolver.CustomResolver", custom.getClass().getName());
    }

}
