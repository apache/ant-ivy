/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.parser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.repository.Resource;

public interface ModuleDescriptorParser {
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, boolean validate) throws ParseException, IOException;
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException;
    
    public void toIvyFile(URL srcURL, Resource res, File destFile, ModuleDescriptor md) throws ParseException, IOException;

    public boolean accept(Resource res);
}
