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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.jar.JarRepository;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.repository.url.URLResource;

public class JarResolver extends RepositoryResolver {

    private URL url;

    public JarResolver() {
        setRepository(new JarRepository());
    }

    @Override
    public String getTypeName() {
        return "jar";
    }

    public void setFile(String jarFile) {
        setJarFile(new File(jarFile));
    }

    public void setUrl(String jarUrl) {
        try {
            url = new URL(jarUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("the jar repository " + getName()
                    + " has an malformed url : " + jarUrl + " (" + e.getMessage() + ")");
        }
    }

    public JarRepository getJarRepository() {
        return (JarRepository) super.getRepository();
    }

    private void setJarFile(File jarLocalFile) {
        JarFile jar;
        try {
            jar = new JarFile(jarLocalFile);
        } catch (IOException e) {
            throw new RuntimeException("the jar repository " + getName() + " could not be read ("
                    + e.getMessage() + ")", e);
        }
        getJarRepository().setJarFile(jar);
    }

    @Override
    public void setSettings(ResolverSettings settings) {
        super.setSettings(settings);
        // let's resolve the url
        if (url != null) {
            ArtifactDownloadReport report;
            EventManager eventManager = getEventManager();
            try {
                if (eventManager != null) {
                    getRepository().addTransferListener(eventManager);
                }
                Resource jarResource = new URLResource(url);
                CacheResourceOptions options = new CacheResourceOptions();
                report = getRepositoryCacheManager().downloadRepositoryResource(jarResource,
                    "jarrepository", "jar", "jar", options, new URLRepository());
            } finally {
                if (eventManager != null) {
                    getRepository().removeTransferListener(eventManager);
                }
            }
            if (report.getDownloadStatus() == DownloadStatus.FAILED) {
                throw new RuntimeException("The jar file " + url.toExternalForm()
                        + " could not be downloaded (" + report.getDownloadDetails() + ")");
            }
            setJarFile(report.getLocalFile());
        }
    }
}
