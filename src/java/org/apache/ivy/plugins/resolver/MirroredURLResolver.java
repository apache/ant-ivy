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
import java.util.List;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.osgi.repo.RelativeURLRepository;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.url.ChainedRepository;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;
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
        List<String> mirrorBaseUrls;
        try {
            mirrorBaseUrls = readMirrorList(mirrorListFile);
        } catch (IOException e) {
            throw new IllegalStateException("The mirror list could not be read from "
                    + mirrorListUrl + " (" + e.getMessage() + ")");
        }
        List<Repository> repositories = new ArrayList<Repository>();
        for (String baseUrl : mirrorBaseUrls) {
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
        URLRepository urlRepository = new URLRepository();
        if (getEventManager() != null) {
            urlRepository.addTransferListener(getEventManager());
        }
        URLResource mirrorResource = new URLResource(mirrorListUrl);
        CacheResourceOptions options = new CacheResourceOptions();
        ArtifactDownloadReport report = getRepositoryCacheManager().downloadRepositoryResource(
            mirrorResource, "mirrorlist", "text", "txt", options, urlRepository);
        return report.getLocalFile();
    }

    private List<String> readMirrorList(File mirrorListFile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                mirrorListFile)));
        List<String> list = new ArrayList<String>();
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

    @Override
    public String getTypeName() {
        return "mirroredurl";
    }

    @Override
    public void validate() {
        super.validate();
        setupMirrors();
    }
}
