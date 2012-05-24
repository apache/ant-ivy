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
package org.apache.ivy.osgi.p2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.osgi.util.Version;

public class P2Descriptor extends RepoDescriptor {

    private long timestamp;

    private Map/* <String, Map<Version, String>> */artifactUrlPatterns = new HashMap();

    private String repoUrl;

    public P2Descriptor(URI repoUri, ExecutionEnvironmentProfileProvider profileProvider) {
        super(repoUri, profileProvider);
        try {
            repoUrl = repoUri.toURL().toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Illegal repo uri", e);
        }
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addBundle(BundleInfo bundleInfo) {
        // before transforming it and adding it into the repo, let's add the artifacts
        // and if no artifact, then no bundle

        Map/* <Version, String> */urlPatternsByVersion = (Map) artifactUrlPatterns.get(bundleInfo
                .getSymbolicName());
        if (urlPatternsByVersion != null) {
            String urlPattern = (String) urlPatternsByVersion.get(bundleInfo.getVersion());
            if (urlPattern != null) {
                String url = urlPattern.replaceAll("\\$\\{id\\}", bundleInfo.getSymbolicName());
                url = url.replaceAll("\\$\\{version\\}", bundleInfo.getVersion().toString());
                try {
                    bundleInfo.setUri(new URI(url));
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Unable to build the artifact uri of " + bundleInfo,
                            e);
                }
                super.addBundle(bundleInfo);
            }
        }
    }

    public void addArtifactUrl(String classifier, String id, Version version, String url) {
        if (!classifier.equals("osgi.bundle")) {
            // we only support OSGi bundle, no Eclipse feature or anything else
            return;
        }
        Map/* <Version, String> */byVersion = (Map) artifactUrlPatterns.get(id);
        if (byVersion == null) {
            byVersion = new HashMap();
            artifactUrlPatterns.put(id, byVersion);
        }
        byVersion.put(version, url);
    }

}
