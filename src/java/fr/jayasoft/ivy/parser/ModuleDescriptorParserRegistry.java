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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;

public class ModuleDescriptorParserRegistry extends AbstractModuleDescriptorParser {
    private static ModuleDescriptorParserRegistry INSTANCE = new ModuleDescriptorParserRegistry();
    
    public static ModuleDescriptorParserRegistry getInstance() {
        return INSTANCE;
    }

    private List _parsers = new ArrayList();
    private ModuleDescriptorParserRegistry() {
        _parsers.add(XmlModuleDescriptorParser.getInstance());
    }
    
    public ModuleDescriptorParser getParser(Resource res) {
        for (Iterator iter = _parsers.iterator(); iter.hasNext();) {
            ModuleDescriptorParser parser = (ModuleDescriptorParser)iter.next();
            if (parser.accept(res)) {
                return parser;
            }
        }
        return null;
    }    
    
    public ModuleDescriptor parseDescriptor(Ivy ivy, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException {
        ModuleDescriptorParser parser = getParser(res);
        if (parser == null) {
            Message.warn("no module descriptor parser found for "+res);
            return null;
        }
        return parser.parseDescriptor(ivy, descriptorURL, res, validate);
    }

    public boolean accept(Resource res) {
        return getParser(res) != null;
    }

    public void toIvyFile(URL srcURL, Resource res, File destFile, ModuleDescriptor md) throws ParseException, IOException {
        ModuleDescriptorParser parser = getParser(res);
        if (parser == null) {
            Message.warn("no module descriptor parser found for "+res);
        } else {
            parser.toIvyFile(srcURL, res, destFile, md);
        }
    }

}
