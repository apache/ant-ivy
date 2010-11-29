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
package org.apache.ivy.osgi.repo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.jar.Manifest;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;

public class ManifestMDParser extends AbstractModuleDescriptorParser {

    private ExecutionEnvironmentProfileProvider profileProvider;

    public void add(ExecutionEnvironmentProfileProvider pp) {
        this.profileProvider = pp;
    }

    public boolean accept(Resource res) {
        if (res == null || res.getName() == null || res.getName().trim().equals("")) {
            return false;
        }
        return res.getName().toUpperCase().endsWith("MANIFEST.MF");
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            Resource res, boolean validate) throws ParseException, IOException {
        Manifest m = new Manifest(res.openStream());
        BundleInfo bundleInfo = ManifestParser.parseManifest(m);
        return BundleInfoAdapter.toModuleDescriptor(bundleInfo, profileProvider);
    }

    public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md)
            throws ParseException, IOException {
        try {
            XmlModuleDescriptorWriter.write(md, destFile);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

}
