/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.parser;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.repository.url.URLResource;

public abstract class AbstractModuleDescriptorParser implements ModuleDescriptorParser {
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, boolean validate) throws ParseException, IOException {
        return parseDescriptor(ivy, descriptorURL, new URLResource(descriptorURL), validate);
    }
}
