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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.repo.AbstractOSGiResolver;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.xml.sax.SAXException;

public class OBRResolver extends AbstractOSGiResolver {

    private String repoXmlURL;

    private String repoXmlFile;

    private Long metadataTtl;

    private Boolean forceMetadataUpdate;

    public void setRepoXmlFile(String repositoryXmlFile) {
        this.repoXmlFile = repositoryXmlFile;
    }

    public void setRepoXmlURL(String repositoryXmlURL) {
        this.repoXmlURL = repositoryXmlURL;
    }

    public void setMetadataTtl(Long metadataTtl) {
        this.metadataTtl = metadataTtl;
    }

    public void setForceMetadataUpdate(Boolean forceMetadataUpdate) {
        this.forceMetadataUpdate = forceMetadataUpdate;
    }

    @Override
    protected void init() {
        if (repoXmlFile != null && repoXmlURL != null) {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: repoXmlFile and repoXmlUrl cannot be set both");
        }
        if (repoXmlFile != null) {
            File f = new File(repoXmlFile);
            loadRepoFromFile(f.getParentFile().toURI(), f, repoXmlFile);
        } else if (repoXmlURL != null) {
            final URL url;
            try {
                url = new URL(repoXmlURL);
            } catch (MalformedURLException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: repoXmlURL '" + repoXmlURL + "' is not an URL");
            }

            ArtifactDownloadReport report;
            EventManager eventManager = getEventManager();
            try {
                if (eventManager != null) {
                    getRepository().addTransferListener(eventManager);
                }
                Resource obrResource = new URLResource(url);
                CacheResourceOptions options = new CacheResourceOptions();
                if (metadataTtl != null) {
                    options.setTtl(metadataTtl.longValue());
                }
                if (forceMetadataUpdate != null) {
                    options.setForce(forceMetadataUpdate.booleanValue());
                }
                report = getRepositoryCacheManager().downloadRepositoryResource(obrResource, "obr",
                    "obr", "xml", options, getRepository());
            } finally {
                if (eventManager != null) {
                    getRepository().removeTransferListener(eventManager);
                }
            }

            URI baseURI;
            try {
                baseURI = new URI(repoXmlURL);
            } catch (URISyntaxException e) {
                throw new RuntimeException("illegal uri");
            }
            loadRepoFromFile(baseURI, report.getLocalFile(), repoXmlURL);

        } else {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: repoXmlFile or repoXmlUrl is missing");
        }
    }

    private void loadRepoFromFile(URI baseUri, File repoFile, String sourceLocation) {
        FileInputStream in;
        try {
            in = new FileInputStream(repoFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: the file " + sourceLocation + " was not found");
        }
        try {
            setRepoDescriptor(OBRXMLParser.parse(baseUri, in));
        } catch (IOException e) {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: the file " + sourceLocation
                    + " could not be read (" + e.getMessage() + ")", e);
        } catch (SAXException e) {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: the file " + sourceLocation
                    + " has incorrect XML (" + e.getMessage() + ")", e);
        }
        try {
            in.close();
        } catch (IOException e) {
            // don't care
        }
    }
}
