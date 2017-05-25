/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.osgi.core.OSGiManifestParser;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;

public final class ModuleDescriptorParserRegistry extends AbstractModuleDescriptorParser {
    private static final ModuleDescriptorParserRegistry INSTANCE = new ModuleDescriptorParserRegistry();

    public static ModuleDescriptorParserRegistry getInstance() {
        return INSTANCE;
    }

    private List<ModuleDescriptorParser> parsers = new LinkedList<ModuleDescriptorParser>();

    private ModuleDescriptorParserRegistry() {
        parsers.add(PomModuleDescriptorParser.getInstance());
        parsers.add(OSGiManifestParser.getInstance());
        parsers.add(XmlModuleDescriptorParser.getInstance());
    }

    /**
     * Adds a the given parser to this registry.
     * 
     * @param parser
     *            the parser to add
     */
    public void addParser(ModuleDescriptorParser parser) {
        /*
         * The parser is added in the front of the list of parsers. This is necessary because the
         * XmlModuleDescriptorParser accepts all resources!
         */
        parsers.add(0, parser);
    }

    public ModuleDescriptorParser[] getParsers() {
        return parsers.toArray(new ModuleDescriptorParser[parsers.size()]);
    }

    public ModuleDescriptorParser getParser(Resource res) {
        for (ModuleDescriptorParser parser : parsers) {
            if (parser.accept(res)) {
                return parser;
            }
        }
        return null;
    }

    public ModuleDescriptor parseDescriptor(ParserSettings settings, URL descriptorURL,
            Resource res, boolean validate) throws ParseException, IOException {
        ModuleDescriptorParser parser = getParser(res);
        if (parser == null) {
            Message.warn("no module descriptor parser found for " + res);
            return null;
        }
        return parser.parseDescriptor(settings, descriptorURL, res, validate);
    }

    public boolean accept(Resource res) {
        return getParser(res) != null;
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md)
            throws ParseException, IOException {
        ModuleDescriptorParser parser = getParser(res);
        if (parser == null) {
            Message.warn("no module descriptor parser found for " + res);
        } else {
            parser.toIvyFile(is, res, destFile, md);
        }
    }

}
