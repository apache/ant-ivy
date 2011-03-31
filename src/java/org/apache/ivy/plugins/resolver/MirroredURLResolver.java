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
package org.apache.ivy.plugins.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.CacheDownloadOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.osgi.repo.RelativeURLRepository;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.repository.url.ChainedRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;

public class MirroredURLResolver extends RepositoryResolver {

    private URL mirrorListUrl;

    public MirroredURLResolver() {
        setRepository(new ChainedRepository());
    }

    public void setMirrorListUrl(URL mirrorListUrl) {
        this.mirrorListUrl = mirrorListUrl;
    }

    private void setupMirrors() {
        File mirrorListFile = downloadMirrorList();
        List mirrorBaseUrls;
        try {
            mirrorBaseUrls = readMirrorList(mirrorListFile);
        } catch (IOException e) {
            throw new IllegalStateException("The mirror list could not be read from "
                    + mirrorListUrl + " (" + e.getMessage() + ")");
        }
        List/* <Repository> */repositories = new ArrayList();
        Iterator it = mirrorBaseUrls.iterator();
        while (it.hasNext()) {
            String baseUrl = (String) it.next();
            URL url = null;
            try {
                url = new URL(baseUrl);
            } catch (MalformedURLException e) {
                Message.warn("In the mirror list from " + mirrorListUrl
                        + ", an incorrect url has been found and will then not be used: " + baseUrl);
            }
            if (url != null) {
                RelativeURLRepository repo = new RelativeURLRepository(url);
                repositories.add(repo);
            }
        }
        ((ChainedRepository) getRepository()).setRepositories(repositories);
    }

    private File downloadMirrorList() {
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("_mirror_list_cache_", getName(),
            Ivy.getWorkingRevision());
        Artifact artifact = new DefaultArtifact(mrid, null, "mirrorlist", "text", "txt");
        CacheDownloadOptions options = new CacheDownloadOptions();
        ArtifactDownloadReport report = getRepositoryCacheManager().download(artifact,
            new ArtifactResourceResolver() {
                public ResolvedResource resolve(Artifact artifact) {
                    return new ResolvedResource(new URLResource(mirrorListUrl), Ivy
                            .getWorkingRevision());
                }
            }, new ResourceDownloader() {
                public void download(Artifact artifact, Resource resource, File dest)
                        throws IOException {
                    if (dest.exists()) {
                        dest.delete();
                    }
                    File part = new File(dest.getAbsolutePath() + ".part");
                    FileUtil.copy(mirrorListUrl, part, null);
                    if (!part.renameTo(dest)) {
                        throw new IOException("impossible to move part file to definitive one: "
                                + part + " -> " + dest);
                    }
                }
            }, options);
        return report.getLocalFile();
    }

    private List/* <String> */readMirrorList(File mirrorListFile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                mirrorListFile)));
        List/* <String> */list = new ArrayList();
        try {
            String line = in.readLine();
            while (line != null) {
                list.add(line);
                line = in.readLine();
            }
        } finally {
            in.close();
        }
        return list;
    }

    public String getTypeName() {
        return "mirroredurl";
    }

    public void validate() {
        super.validate();
        setupMirrors();
    }
}
