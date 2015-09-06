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
package org.apache.ivy.osgi.obr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.osgi.core.BundleArtifact;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.obr.xml.OBRXMLWriter;
import org.apache.ivy.osgi.repo.BundleRepoDescriptor;
import org.apache.ivy.osgi.repo.ModuleDescriptorWrapper;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.CollectionUtils;
import org.xml.sax.ContentHandler;

import junit.framework.TestCase;

public class OBRXMLWriterTest extends TestCase {

    private static final Version BUNDLE_VERSION = new Version(1, 2, 3, null);

    private static final String BUNDLE_1 = "org.apache.ivy.test";

    private static final String BUNDLE_2 = "org.apache.ivy.test2";

    public void testWriteWithSource() throws Exception {
        List<BundleInfo> bundles = new ArrayList<BundleInfo>();

        BundleInfo bundle = new BundleInfo(BUNDLE_1, BUNDLE_VERSION);
        bundle.addArtifact(new BundleArtifact(false, new URI("file:///test.jar"), null));
        bundle.addArtifact(new BundleArtifact(true, new URI("file:///test-sources.jar"), null));
        bundles.add(bundle);

        bundle = new BundleInfo(BUNDLE_2, BUNDLE_VERSION);
        bundle.addArtifact(new BundleArtifact(false, new URI("file:///test2.jar"), null));
        bundles.add(bundle);

        new File("build/test-files").mkdirs();
        File obrFile = new File("build/test-files/obr-sources.xml");
        FileOutputStream out = new FileOutputStream(obrFile);
        try {
            ContentHandler hanlder = OBRXMLWriter.newHandler(out, "UTF-8", true);
            OBRXMLWriter.writeBundles(bundles, hanlder);
        } finally {
            out.close();
        }

        FileInputStream in = new FileInputStream(obrFile);
        BundleRepoDescriptor repo;
        try {
            repo = OBRXMLParser.parse(new URI("file:///test"), in);
        } finally {
            in.close();
        }
        assertEquals(2, CollectionUtils.toList(repo.getModules()).size());

        ModuleDescriptorWrapper bundle1 = repo.findModule(BUNDLE_1, BUNDLE_VERSION);
        assertNotNull(bundle1);
        Artifact[] artifacts = bundle1.getModuleDescriptor().getAllArtifacts();
        assertEquals(2, artifacts.length);
        if (artifacts[0].getType().equals("jar")) {
            assertEquals("source", artifacts[1].getType());
        } else {
            assertEquals("jar", artifacts[1].getType());
            assertEquals("source", artifacts[0].getType());
        }

        ModuleDescriptorWrapper bundle2 = repo.findModule(BUNDLE_2, BUNDLE_VERSION);
        assertNotNull(bundle2);
        assertEquals(1, bundle2.getModuleDescriptor().getAllArtifacts().length);
    }

}
