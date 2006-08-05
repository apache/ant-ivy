/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.repository.Resource;

public class ModuleDescriptorParserRegistryTest extends TestCase {
    public static class MyParser extends AbstractModuleDescriptorParser {
        public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException {
            return DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance("test", "parser", "1.0"));
        }

        public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md) throws ParseException, IOException {
        }

        public boolean accept(Resource res) {
            return true;
        }

    }
    public void testAddConfigured() throws Exception {
        Ivy ivy = new Ivy();
        ivy.addConfigured(new MyParser());
        ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(ivy, ModuleDescriptorParserRegistryTest.class.getResource("nores"), false);
        assertNotNull(md);
        assertEquals(ModuleRevisionId.newInstance("test", "parser", "1.0"), md.getModuleRevisionId());
    }
}
