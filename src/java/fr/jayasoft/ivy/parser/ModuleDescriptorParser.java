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

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.repository.Resource;

public interface ModuleDescriptorParser {
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, boolean validate) throws ParseException, IOException;
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException;
    
    /**
     * Convert a module descriptor to an ivy file.
     * 
     * This method MUST close the given input stream when job is finished
     * 
     * @param is input stream with opened on original module descriptor resource
     * 
     */
    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md) throws ParseException, IOException;

    public boolean accept(Resource res);
}
