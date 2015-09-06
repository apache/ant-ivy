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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.obr.xml.OBRXMLWriter;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class BundleRepoTest extends TestCase {

    private File bundlerepo = new File("test/test-repo/bundlerepo");

    private File ivyrepo = new File("test/test-repo/ivyrepo");

    public void testFS() throws Exception {
        FSManifestIterable it = new FSManifestIterable(bundlerepo);
        BundleRepoDescriptor repo = new BundleRepoDescriptor(bundlerepo.toURI(),
                ExecutionEnvironmentProfileProvider.getInstance());
        repo.populate(it.iterator());

        BundleRepoDescriptor repo2 = OBRXMLParser.parse(bundlerepo.toURI(), new FileInputStream(
                new File(bundlerepo, "repo.xml")));

        assertEquals(repo, repo2);
    }

    public void testFileRepo() throws Exception {
        RepositoryManifestIterable it = new RepositoryManifestIterable(new FileRepository(
                bundlerepo.getAbsoluteFile()));
        BundleRepoDescriptor repo = new BundleRepoDescriptor(bundlerepo.toURI(),
                ExecutionEnvironmentProfileProvider.getInstance());
        repo.populate(it.iterator());

        BundleRepoDescriptor repo2 = OBRXMLParser.parse(bundlerepo.toURI(), new FileInputStream(
                new File(bundlerepo, "repo.xml")));

        assertEquals(repo, repo2);
    }

    public void testResolver() throws Exception {
        FileSystemResolver fileSystemResolver = new FileSystemResolver();
        fileSystemResolver.setName("test");
        fileSystemResolver.addIvyPattern(ivyrepo.getAbsolutePath()
                + "/[organisation]/[module]/[revision]/ivy.xml");
        fileSystemResolver.addArtifactPattern(ivyrepo.getAbsolutePath()
                + "/[organisation]/[module]/[revision]/[type]s/[artifact]-[revision].[ext]");
        fileSystemResolver.setSettings(new IvySettings());
        ResolverManifestIterable it = new ResolverManifestIterable(fileSystemResolver);
        BundleRepoDescriptor repo = new BundleRepoDescriptor(ivyrepo.toURI(),
                ExecutionEnvironmentProfileProvider.getInstance());
        repo.populate(it.iterator());

        BundleRepoDescriptor repo2 = OBRXMLParser.parse(ivyrepo.toURI(), new FileInputStream(
                new File(ivyrepo, "repo.xml")));

        assertEquals(repo, repo2);
    }

    public void testXMLSerialisation() throws SAXException, ParseException, IOException {
        FSManifestIterable it = new FSManifestIterable(bundlerepo);
        BundleRepoDescriptor repo = new BundleRepoDescriptor(bundlerepo.toURI(),
                ExecutionEnvironmentProfileProvider.getInstance());
        repo.populate(it.iterator());

        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler hd;
        try {
            hd = tf.newTransformerHandler();
        } catch (TransformerConfigurationException e) {
            throw new BuildException("Sax configuration error: " + e.getMessage(), e);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        hd.setResult(stream);

        OBRXMLWriter.writeManifests(it, hd, false);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BundleRepoDescriptor repo2 = OBRXMLParser.parse(bundlerepo.toURI(), in);

        assertEquals(repo, repo2);
    }

}
