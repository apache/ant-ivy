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
package org.apache.ivy.osgi.updatesite;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.p2.P2ArtifactParser;
import org.apache.ivy.osgi.p2.P2CompositeParser;
import org.apache.ivy.osgi.p2.P2Descriptor;
import org.apache.ivy.osgi.p2.P2MetadataParser;
import org.apache.ivy.osgi.p2.XMLInputParser;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.osgi.updatesite.xml.EclipseFeature;
import org.apache.ivy.osgi.updatesite.xml.EclipseUpdateSiteParser;
import org.apache.ivy.osgi.updatesite.xml.FeatureParser;
import org.apache.ivy.osgi.updatesite.xml.UpdateSite;
import org.apache.ivy.osgi.updatesite.xml.UpdateSiteDigestParser;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class UpdateSiteLoader {

    private final RepositoryCacheManager repositoryCacheManager;

    private final URLRepository urlRepository = new URLRepository();

    private final CacheResourceOptions options;

    private int logLevel = Message.MSG_INFO;

    public UpdateSiteLoader(RepositoryCacheManager repositoryCacheManager,
            EventManager eventManager, CacheResourceOptions options) {
        this.repositoryCacheManager = repositoryCacheManager;
        this.options = options;
        if (eventManager != null) {
            urlRepository.addTransferListener(eventManager);
        }
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public RepoDescriptor load(URI repoUri) throws IOException, ParseException, SAXException {
        if (!repoUri.toString().endsWith("/")) {
            try {
                repoUri = new URI(repoUri.toString() + "/");
            } catch (URISyntaxException e) {
                throw new RuntimeException("Cannot make an uri for the repo");
            }
        }
        Message.info("Loading the update site " + repoUri);
        // first look for a p2 repository
        RepoDescriptor repo = loadP2(repoUri);
        if (repo != null) {
            return repo;
        }
        Message.verbose("\tNo P2 artifacts, falling back on the old fashioned updatesite");
        // then try the old update site
        UpdateSite site = loadSite(repoUri);
        if (site == null) {
            return null;
        }
        repo = loadFromDigest(site);
        if (repo != null) {
            return repo;
        }
        return loadFromSite(site);
    }

    private P2Descriptor loadP2(URI repoUri) throws IOException, ParseException, SAXException {
        P2Descriptor p2Descriptor = new P2Descriptor(repoUri,
                ExecutionEnvironmentProfileProvider.getInstance());
        p2Descriptor.setLogLevel(logLevel);
        if (!populateP2Descriptor(repoUri, p2Descriptor)) {
            return null;
        }
        p2Descriptor.finish();
        return p2Descriptor;
    }

    private boolean populateP2Descriptor(URI repoUri, P2Descriptor p2Descriptor)
            throws IOException, ParseException, SAXException {
        Message.verbose("Loading P2 repository " + repoUri);
        boolean contentExists = readContent(repoUri, p2Descriptor);
        boolean artifactExists = readArtifacts(repoUri, p2Descriptor);
        return artifactExists || contentExists;
    }

    private boolean readContent(URI repoUri, P2Descriptor p2Descriptor) throws IOException,
            ParseException, SAXException {
        boolean contentExists = readCompositeContent(repoUri, "compositeContent", p2Descriptor);
        if (!contentExists) {
            P2MetadataParser metadataParser = new P2MetadataParser(p2Descriptor);
            metadataParser.setLogLevel(logLevel);
            contentExists = readJarOrXml(repoUri, "content", metadataParser);
        }
        return contentExists;
    }

    private boolean readArtifacts(URI repoUri, P2Descriptor p2Descriptor) throws IOException,
            ParseException, SAXException {
        boolean artifactExists = readCompositeArtifact(repoUri, "compositeArtifacts", p2Descriptor);
        if (!artifactExists) {
            artifactExists = readJarOrXml(repoUri, "artifacts", new P2ArtifactParser(p2Descriptor,
                    repoUri.toURL().toExternalForm()));
        }

        return artifactExists;
    }

    private boolean readCompositeContent(URI repoUri, String name, P2Descriptor p2Descriptor)
            throws IOException, ParseException, SAXException {
        P2CompositeParser p2CompositeParser = new P2CompositeParser();
        boolean exist = readJarOrXml(repoUri, name, p2CompositeParser);
        if (exist) {
            for (String childLocation : p2CompositeParser.getChildLocations()) {
                if (!childLocation.endsWith("/")) {
                    childLocation += "/";
                }
                URI childUri = repoUri.resolve(childLocation);
                readContent(childUri, p2Descriptor);
            }
        }
        return exist;
    }

    private boolean readCompositeArtifact(URI repoUri, String name, P2Descriptor p2Descriptor)
            throws IOException, ParseException, SAXException {
        P2CompositeParser p2CompositeParser = new P2CompositeParser();
        boolean exist = readJarOrXml(repoUri, name, p2CompositeParser);
        if (exist) {
            for (String childLocation : p2CompositeParser.getChildLocations()) {
                if (!childLocation.endsWith("/")) {
                    childLocation += "/";
                }
                URI childUri = repoUri.resolve(childLocation);
                readArtifacts(childUri, p2Descriptor);
            }
        }
        return exist;
    }

    private boolean readJarOrXml(URI repoUri, String baseName, XMLInputParser reader)
            throws IOException, ParseException, SAXException {
        InputStream readIn = null; // the input stream from which the xml should be read

        URL contentUrl = repoUri.resolve(baseName + ".jar").toURL();
        URLResource res = new URLResource(contentUrl);

        ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
            baseName, baseName, "jar", options, urlRepository);

        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            // no jar file, try the xml one
            contentUrl = repoUri.resolve(baseName + ".xml").toURL();
            res = new URLResource(contentUrl);

            report = repositoryCacheManager.downloadRepositoryResource(res, baseName, baseName,
                "xml", options, urlRepository);

            if (report.getDownloadStatus() == DownloadStatus.FAILED) {
                // no xml either
                return false;
            }

            readIn = new FileInputStream(report.getLocalFile());
        } else {
            InputStream in = new FileInputStream(report.getLocalFile());

            try {
                // compressed, let's get the pointer on the actual xml
                readIn = findEntry(in, baseName + ".xml");
                if (readIn == null) {
                    in.close();
                    return false;
                }
            } catch (IOException e) {
                in.close();
                throw e;
            }

        }

        try {
            reader.parse(readIn);
        } finally {
            readIn.close();
        }

        return true;
    }

    private UpdateSite loadSite(URI repoUri) throws IOException, ParseException, SAXException {
        URI siteUri = normalizeSiteUri(repoUri, null);
        URL u = siteUri.resolve("site.xml").toURL();

        URLResource res = new URLResource(u);
        ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
            "site", "updatesite", "xml", options, urlRepository);
        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            return null;
        }
        InputStream in = new FileInputStream(report.getLocalFile());
        try {
            UpdateSite site = EclipseUpdateSiteParser.parse(in);
            site.setUri(normalizeSiteUri(site.getUri(), siteUri));
            return site;
        } finally {
            in.close();
        }
    }

    private URI normalizeSiteUri(URI uri, URI defaultValue) {
        if (uri == null) {
            return defaultValue;
        }
        String uriString = uri.toString();
        if (uriString.endsWith("site.xml")) {
            try {
                return new URI(uriString.substring(0, uriString.length() - 8));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Illegal uri", e);
            }
        }
        if (!uriString.endsWith("/")) {
            try {
                return new URI(uriString + "/");
            } catch (URISyntaxException e) {
                throw new RuntimeException("Illegal uri", e);
            }
        }
        return uri;
    }

    private UpdateSiteDescriptor loadFromDigest(UpdateSite site) throws IOException,
            ParseException, SAXException {
        URI digestBaseUri = site.getDigestUri();
        if (digestBaseUri == null) {
            digestBaseUri = site.getUri();
        } else if (!digestBaseUri.isAbsolute()) {
            digestBaseUri = site.getUri().resolve(digestBaseUri);
        }
        URL digest = digestBaseUri.resolve("digest.zip").toURL();
        Message.verbose("\tReading " + digest);

        URLResource res = new URLResource(digest);
        ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
            "digest", "digest", "zip", options, urlRepository);
        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            return null;
        }
        InputStream in = new FileInputStream(report.getLocalFile());
        try {
            ZipInputStream zipped = findEntry(in, "digest.xml");
            if (zipped == null) {
                return null;
            }
            return UpdateSiteDigestParser.parse(zipped, site);
        } finally {
            in.close();
        }
    }

    private UpdateSiteDescriptor loadFromSite(UpdateSite site) throws IOException, ParseException,
            SAXException {
        UpdateSiteDescriptor repoDescriptor = new UpdateSiteDescriptor(site.getUri(),
                ExecutionEnvironmentProfileProvider.getInstance());

        for (EclipseFeature feature : site.getFeatures()) {
            URL url = site.getUri().resolve(feature.getUrl()).toURL();

            URLResource res = new URLResource(url);
            ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
                feature.getId(), "feature", "jar", options, urlRepository);
            if (report.getDownloadStatus() == DownloadStatus.FAILED) {
                return null;
            }
            InputStream in = new FileInputStream(report.getLocalFile());
            try {
                ZipInputStream zipped = findEntry(in, "feature.xml");
                if (zipped == null) {
                    return null;
                }
                EclipseFeature f = FeatureParser.parse(zipped);
                f.setURL(feature.getUrl());
                repoDescriptor.addFeature(f);
            } finally {
                in.close();
            }
        }

        return repoDescriptor;
    }

    private ZipInputStream findEntry(InputStream in, String entryName) throws IOException {
        ZipInputStream zipped = new ZipInputStream(in);
        ZipEntry zipEntry = zipped.getNextEntry();
        while (zipEntry != null && !zipEntry.getName().equals(entryName)) {
            zipEntry = zipped.getNextEntry();
        }
        if (zipEntry == null) {
            return null;
        }
        return zipped;
    }
}
