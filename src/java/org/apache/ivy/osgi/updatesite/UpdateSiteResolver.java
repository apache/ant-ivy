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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.cache.DownloadListener;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.osgi.repo.AbstractOSGiResolver;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class UpdateSiteResolver extends AbstractOSGiResolver {

    private String url;

    private Long metadataTtl;

    private Boolean forceMetadataUpdate;

    private String logLevel;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMetadataTtl(Long metadataTtl) {
        this.metadataTtl = metadataTtl;
    }

    public void setForceMetadataUpdate(Boolean forceMetadataUpdate) {
        this.forceMetadataUpdate = forceMetadataUpdate;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    protected void init() {
        if (url == null) {
            throw new RuntimeException("Missing url");
        }
        CacheResourceOptions options = new CacheResourceOptions();
        if (metadataTtl != null) {
            options.setTtl(metadataTtl.longValue());
        }
        if (forceMetadataUpdate != null) {
            options.setForce(forceMetadataUpdate.booleanValue());
        }
        final int log;
        if (logLevel != null) {
            if ("debug".equalsIgnoreCase(logLevel)) {
                log = Message.MSG_DEBUG;
            } else if ("verbose".equalsIgnoreCase(logLevel)) {
                log = Message.MSG_VERBOSE;
            } else if ("info".equalsIgnoreCase(logLevel)) {
                log = Message.MSG_INFO;
            } else if ("warn".equalsIgnoreCase(logLevel)) {
                log = Message.MSG_WARN;
            } else if ("error".equalsIgnoreCase(logLevel)) {
                log = Message.MSG_ERR;
            } else {
                throw new RuntimeException("Unknown log level: " + logLevel);
            }
        } else {
            log = Message.MSG_INFO;
        }
        options.setListener(new DownloadListener() {
            public void startArtifactDownload(RepositoryCacheManager cache, ResolvedResource rres,
                    Artifact artifact, ArtifactOrigin origin) {
                if (log <= Message.MSG_INFO) {
                    Message.info("\tdownloading " + rres.getResource().getName());
                }
            }

            public void needArtifact(RepositoryCacheManager cache, Artifact artifact) {
                if (log <= Message.MSG_VERBOSE) {
                    Message.verbose("\ttrying to download " + artifact);
                }
            }

            public void endArtifactDownload(RepositoryCacheManager cache, Artifact artifact,
                    ArtifactDownloadReport adr, File archiveFile) {
                if (log <= Message.MSG_VERBOSE) {
                    if (adr.isDownloaded()) {
                        Message.verbose("\tdownloaded to " + archiveFile.getAbsolutePath());
                    } else {
                        Message.verbose("\tnothing to download");
                    }
                }
            }
        });
        UpdateSiteLoader loader = new UpdateSiteLoader(getRepositoryCacheManager(),
                getEventManager(), options);
        loader.setLogLevel(log);
        RepoDescriptor repoDescriptor;
        try {
            repoDescriptor = loader.load(new URI(url));
        } catch (IOException e) {
            throw new RuntimeException("IO issue while trying to read the update site ("
                    + e.getMessage() + ")");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse the updatesite (" + e.getMessage() + ")", e);
        } catch (SAXException e) {
            throw new RuntimeException("Illformed updatesite (" + e.getMessage() + ")", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Illformed url (" + e.getMessage() + ")", e);
        }
        if (repoDescriptor == null) {
            setRepoDescriptor(FAILING_REPO_DESCRIPTOR);
            throw new RuntimeException("No update site was found at the location: " + url);
        }
        setRepoDescriptor(repoDescriptor);
    }
}
