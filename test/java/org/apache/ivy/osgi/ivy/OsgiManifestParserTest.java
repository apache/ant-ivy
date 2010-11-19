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
package org.apache.ivy.osgi.ivy;

import static java.lang.ClassLoader.getSystemResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.ivy.OsgiManifestParser;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParserTest;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.FileUtil;

/**
 * @author jerome@benois.fr
 */
public class OsgiManifestParserTest extends AbstractModuleDescriptorParserTester {

    private URL getTestResource(String resource) throws MalformedURLException {
        return new File("java/test-ivy/" + resource).toURI().toURL();
    }

    public void testAccept() throws Exception {
        assertTrue(OsgiManifestParser.getInstance().accept(
                new URLResource(getTestResource("osgi/eclipse/plugins/test-simple/META-INF/MANIFEST.MF"))));
        assertFalse(OsgiManifestParser.getInstance().accept(
                new URLResource(XmlModuleDescriptorParserTest.class.getResource("test.xml"))));
    }

    public void testSimple() throws Exception {
        final ModuleDescriptor md = OsgiManifestParser.getInstance().parseDescriptor(new IvySettings(),
                getTestResource("osgi/eclipse/plugins/test-simple/META-INF/MANIFEST.MF"), false);
        assertNotNull(md);
        assertSimpleModuleDescriptor(md);
    }

    public void testSimpleFromJar() throws Exception {
        final ModuleDescriptor md = OsgiManifestParser.getInstance().parseDescriptor(new IvySettings(),
                getTestResource("test-simple.jar"), false);
        assertNotNull(md);
        assertSimpleModuleDescriptor(md);
    }

    public void testFull() throws Exception {
        final ModuleDescriptor md = OsgiManifestParser.getInstance().parseDescriptor(new IvySettings(),
                getTestResource("osgi/eclipse/plugins/test-full/META-INF/MANIFEST.MF"), false);
        assertNotNull(md);
        assertSimpleModuleDescriptor(md);
        assertDependencies(md);
    }

    public void testFullFromJar() throws Exception {
        final ModuleDescriptor md = OsgiManifestParser.getInstance().parseDescriptor(new IvySettings(),
                getTestResource("test-full.jar"), false);
        assertNotNull(md);
        assertSimpleModuleDescriptor(md);
        assertDependencies(md);
    }

    public void testJB() throws Exception {
        final ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId("org", "name"), "revisionId");
        final Artifact artifact = new DefaultArtifact(mrid, new Date(), "META-INF/MANIFEST", "manifest", "MF", true);
        System.out.println(artifact);
    }

    private void assertSimpleModuleDescriptor(ModuleDescriptor md) throws Exception {
        final ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.eclipse", "datatools.connectivity.ui",
                "1.0.1.200708231");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(3, md.getConfigurations().length);
        assertNotNull(md.getConfiguration("default"));

        assertNotNull(md.getAllArtifacts());
        assertEquals(2, md.getAllArtifacts().length);
        final Artifact[] artifact = md.getArtifacts("default");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("org.eclipse.datatools.connectivity.ui", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("jar", artifact[0].getType());

    }

    private void assertDependencies(ModuleDescriptor md) throws Exception {
        final DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        Set<ModuleRevisionId> actual = new HashSet<ModuleRevisionId>();
        for (DependencyDescriptor dd : md.getDependencies()) {
            actual.add(dd.getDependencyRevisionId());
        }
        Set<ModuleRevisionId> expected = new HashSet<ModuleRevisionId>();
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "core.runtime", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "core.resources", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "ui", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "ui.views", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "datatools.connectivity", "[0.9.1,1.5.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "ui.navigator", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "core.expressions", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("com.ibm", "icu", "[3.4.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "ltk.core.refactoring", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "datatools.help", "[1.0.0,2.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "help", "[3.2.0,4.0.0)"));
        expected.add(ModuleRevisionId.newInstance("org.eclipse", "help.base", "[3.2.0,4.0.0)"));

        assertEquals(expected, actual);
    }

    private String readEntirely(String resource) throws IOException {
        return FileUtil
                .readEntirely(new BufferedReader(new InputStreamReader(getSystemResource(resource).openStream())));
    }

}
