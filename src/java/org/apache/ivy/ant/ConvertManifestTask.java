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
package org.apache.ivy.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.jar.Manifest;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleInfoAdapter;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.core.OSGiManifestParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.tools.ant.BuildException;

public class ConvertManifestTask extends IvyTask {

    private File manifest = null;

    private File ivyFile = null;

    private ExecutionEnvironmentProfileProvider profileProvider;

    public void setProfileProvider(ExecutionEnvironmentProfileProvider profileProvider) {
        this.profileProvider = profileProvider;
    }

    public void setManifest(File manifest) {
        this.manifest = manifest;
    }

    public void setIvyFile(File ivyFile) {
        this.ivyFile = ivyFile;
    }

    public void doExecute() throws BuildException {
        if (ivyFile == null) {
            throw new BuildException("destination ivy file is required for convertmanifest task");
        }
        if (manifest == null) {
            throw new BuildException("source manifest file is required for convertmanifest task");
        }
        if (profileProvider == null) {
            try {
                profileProvider = new ExecutionEnvironmentProfileProvider();
            } catch (IOException e) {
                throw new BuildException("Enable to load the default environment profiles", e);
            }
        }

        Manifest m;
        try {
            m = new Manifest(new FileInputStream(manifest));
        } catch (FileNotFoundException e) {
            throw new BuildException("the manifest file '" + manifest + "' was not found", e);
        } catch (IOException e) {
            throw new BuildException("the manifest file '" + manifest + "' could not be read", e);
        }

        BundleInfo bundleInfo;
        try {
            bundleInfo = ManifestParser.parseManifest(m);
        } catch (ParseException e) {
            throw new BuildException("Incorrect manifest file '" + manifest + "'", e);
        }
        ModuleDescriptor md = BundleInfoAdapter.toModuleDescriptor(
            OSGiManifestParser.getInstance(), null, bundleInfo, m, profileProvider);

        try {
            XmlModuleDescriptorWriter.write(md, ivyFile);
        } catch (IOException e) {
            throw new BuildException("The ivyFile '" + ivyFile + "' could not be written", e);
        }
    }

}
