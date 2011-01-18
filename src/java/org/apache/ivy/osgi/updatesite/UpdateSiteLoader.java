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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.p2.P2ArtifactParser;
import org.apache.ivy.osgi.p2.P2Descriptor;
import org.apache.ivy.osgi.p2.P2MetadataParser;
import org.apache.ivy.osgi.p2.XMLInputParser;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.osgi.updatesite.xml.EclipseFeature;
import org.apache.ivy.osgi.updatesite.xml.EclipseUpdateSiteParser;
import org.apache.ivy.osgi.updatesite.xml.FeatureParser;
import org.apache.ivy.osgi.updatesite.xml.UpdateSite;
import org.apache.ivy.osgi.updatesite.xml.UpdateSiteDigestParser;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.xml.sax.SAXException;

public class UpdateSiteLoader {

    public RepoDescriptor load(String url) throws IOException, ParseException, SAXException {
        // first look for a p2 repository
        RepoDescriptor repo = loadP2(url);
        if (repo != null) {
            return repo;
        }
        // then try the old update site
        UpdateSite site = loadSite(url);
        repo = loadFromDigest(site);
        if (repo != null) {
            return repo;
        }
        return loadFromSite(site);
    }

    private P2Descriptor loadP2(String url) throws IOException, ParseException, SAXException {
        P2Descriptor p2Descriptor = new P2Descriptor(
                ExecutionEnvironmentProfileProvider.getInstance());

        if (!readJarOrXml(url, "artifacts", new P2ArtifactParser(p2Descriptor))) {
            return null;
        }

        if (!readJarOrXml(url, "content", new P2MetadataParser(p2Descriptor))) {
            return null;
        }

        return p2Descriptor;
    }

    private boolean readJarOrXml(String url, String baseName, XMLInputParser reader)
            throws IOException, ParseException, SAXException {
        InputStream readIn = null; // the input stream from which the xml should be read

        URL contentUrl = new URL(url + baseName + ".jar");
        URLResource res = new URLResource(contentUrl);
        if (!res.exists()) {
            // no jar file, try the xml one
            contentUrl = new URL(url + baseName + ".xml");
            res = new URLResource(contentUrl);
            
            if (!res.exists()) {
                // no xml either
                return false;
            }
            
            // we will then read directly from that input stream
            readIn = res.openStream();
        } else {
            InputStream in = res.openStream();

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
        InputStream in = URLHandlerRegistry.getDefault().openStream(u);
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
        InputStream in;
        try {
            in = URLHandlerRegistry.getDefault().openStream(digest);
        } catch (FileNotFoundException e) {
            return null;
        }
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
            InputStream in = URLHandlerRegistry.getDefault().openStream(url);
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
