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
package org.apache.ivy.osgi.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.jar.Manifest;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;

public class OSGiManifestParser implements ModuleDescriptorParser {

    private static final OSGiManifestParser INSTANCE = new OSGiManifestParser();

    public static OSGiManifestParser getInstance() {
        return INSTANCE;
    }

    private ExecutionEnvironmentProfileProvider profileProvider = ExecutionEnvironmentProfileProvider
            .getInstance();

    public void add(ExecutionEnvironmentProfileProvider pp) {
        this.profileProvider = pp;
    }

    public boolean accept(Resource res) {
        if (res == null || res.getName() == null || res.getName().trim().equals("")) {
            return false;
        }
        return res.getName().toUpperCase(Locale.US).endsWith("MANIFEST.MF");
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            Resource res, boolean validate) throws ParseException, IOException {
        Manifest m = new Manifest(res.openStream());
        BundleInfo bundleInfo = ManifestParser.parseManifest(m);
        try {
            bundleInfo.addArtifact(new BundleArtifact(false, new URI(res.getName()), null));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unsupported repository, resources names are not uris", e);
        }
        return BundleInfoAdapter.toModuleDescriptor(this, null, bundleInfo, m, profileProvider);
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

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL,
            boolean validate) throws ParseException, IOException {
        URLResource resource = new URLResource(descriptorURL);
        return parseDescriptor(ivySettings, descriptorURL, resource, validate);
    }

    public String getType() {
        return "manifest";
    }

    public Artifact getMetadataArtifact(ModuleRevisionId mrid, Resource res) {
        return DefaultArtifact.newIvyArtifact(mrid, new Date(res.getLastModified()));
    }

    public String toString() {
        return "manifest parser";
    }
}
