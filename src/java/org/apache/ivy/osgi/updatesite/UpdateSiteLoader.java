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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
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

    public UpdateSiteLoader(RepositoryCacheManager repositoryCacheManager,
            EventManager eventManager, CacheResourceOptions options) {
        this.repositoryCacheManager = repositoryCacheManager;
        this.options = options;
        if (eventManager != null) {
            urlRepository.addTransferListener(eventManager);
        }
    }

    public RepoDescriptor load(String url) throws IOException, ParseException, SAXException {
        Message.verbose("Loading the update site " + url);
        // first look for a p2 repository
        RepoDescriptor repo = loadP2(url);
        if (repo != null) {
            return repo;
        }
        Message.verbose("\tNo P2 artifacts, falling back on the old fashioned updatesite");
        // then try the old update site
        UpdateSite site = loadSite(url);
        if (site == null) {
            return null;
        }
        repo = loadFromDigest(site);
        if (repo != null) {
            return repo;
        }
        return loadFromSite(site);
    }

    private P2Descriptor loadP2(String url) throws IOException, ParseException, SAXException {
        P2Descriptor p2Descriptor = new P2Descriptor(
                ExecutionEnvironmentProfileProvider.getInstance());
        if (!populateP2Descriptor(url, p2Descriptor)) {
            return null;
        }
        return p2Descriptor;
    }

    private boolean populateP2Descriptor(String url, P2Descriptor p2Descriptor) throws IOException,
            ParseException, SAXException {
        boolean exist = false;

        exist |= readComposite(url, "compositeContent", p2Descriptor);

        exist |= readComposite(url, "compositeArtifacts", p2Descriptor);

        exist |= readJarOrXml(url, "artifacts", new P2ArtifactParser(p2Descriptor));

        exist |= readJarOrXml(url, "content", new P2MetadataParser(p2Descriptor));

        return exist;
    }

    private boolean readComposite(String url, String name, P2Descriptor p2Descriptor)
            throws IOException, ParseException, SAXException {
        P2CompositeParser p2CompositeParser = new P2CompositeParser();
        boolean exist = readJarOrXml(url, name, p2CompositeParser);
        if (exist) {
            Iterator itChildLocation = p2CompositeParser.getChildLocations().iterator();
            while (itChildLocation.hasNext()) {
                String childLocation = (String) itChildLocation.next();
                String childUrl = url + childLocation + "/";
                try {
                    URL u = new URL(childLocation);
                    childUrl = u.toExternalForm();
                } catch (MalformedURLException e) {
                    // not an url, keep the relative location one
                }
                populateP2Descriptor(childUrl, p2Descriptor);
            }
        }
        return exist;
    }

    private boolean readJarOrXml(String url, String baseName, XMLInputParser reader)
            throws IOException, ParseException, SAXException {
        InputStream readIn = null; // the input stream from which the xml should be read

        URL contentUrl = new URL(url + baseName + ".jar");
        URLResource res = new URLResource(contentUrl);

        ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
            baseName, baseName, "jar", options, urlRepository);

        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            // no jar file, try the xml one
            contentUrl = new URL(url + baseName + ".xml");
            res = new URLResource(contentUrl);

            report = repositoryCacheManager.downloadRepositoryResource(res,
                baseName, baseName, "xml", options, urlRepository);

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

    private UpdateSite loadSite(String url) throws IOException, ParseException, SAXException {
        String siteUrl = normalizeSiteUrl(url, null);
        URL u = new URL(siteUrl + "site.xml");

        URLResource res = new URLResource(u);
        ArtifactDownloadReport report = repositoryCacheManager.downloadRepositoryResource(res,
            "site", "updatesite", "xml", options, urlRepository);
        if (report.getDownloadStatus() == DownloadStatus.FAILED) {
            return null;
        }
        InputStream in = new FileInputStream(report.getLocalFile());
        try {
            UpdateSite site = EclipseUpdateSiteParser.parse(in);
            site.setUrl(normalizeSiteUrl(site.getUrl(), siteUrl));
            return site;
        } finally {
            in.close();
        }
    }

    private String normalizeSiteUrl(String url, String defaultValue) {
        if (url == null) {
            return defaultValue;
        }
        if (url.endsWith("site.xml")) {
            return url.substring(0, url.length() - 8);
        }
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    private UpdateSiteDescriptor loadFromDigest(UpdateSite site) throws IOException,
            ParseException, SAXException {
        String baseUrl = site.getDigestURL();
        String siteUrl = site.getUrl();
        if (baseUrl == null) {
            baseUrl = siteUrl;
        } else if (baseUrl.startsWith(".")) {
            if (baseUrl.length() > 1 && baseUrl.charAt(1) == '/') {
                baseUrl = siteUrl + baseUrl.substring(2);
            } else {
                baseUrl = siteUrl + baseUrl.substring(1);
            }
        }
        String digestUrl;
        if (baseUrl.endsWith("/")) {
            digestUrl = baseUrl + "digest.zip";
        } else {
            digestUrl = baseUrl + "/digest.zip";
        }
        URL digest = new URL(digestUrl);
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
        UpdateSiteDescriptor repoDescriptor = new UpdateSiteDescriptor(
                ExecutionEnvironmentProfileProvider.getInstance());

        Iterator itFeatures = site.getFeatures().iterator();
        while (itFeatures.hasNext()) {
            EclipseFeature feature = (EclipseFeature) itFeatures.next();
            URL url = new URL(site.getUrl() + feature.getUrl());

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
                repoDescriptor.addFeature(site.getUrl(), f);
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
