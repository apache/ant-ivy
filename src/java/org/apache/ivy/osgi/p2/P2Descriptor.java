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

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.repo.RepoDescriptor;
import org.apache.ivy.osgi.util.Version;

public class P2Descriptor extends RepoDescriptor {

    private long timestamp;

    private Map/* <String, Map<Version, String>> */artifactUrls = new HashMap();

    public P2Descriptor(ExecutionEnvironmentProfileProvider profileProvider) {
        super(profileProvider);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addBundle(BundleInfo bundleInfo) {
        // before transforming it and adding it into the repo, let's add the artifacts

        Map/* <Version, String> */urlsByVersion = (Map) artifactUrls.get(bundleInfo
                .getSymbolicName());
        if (urlsByVersion != null) {
            String url = (String) urlsByVersion.get(bundleInfo.getVersion());
            if (url != null) {
                bundleInfo.setUri(url);
            }
        }

        super.addBundle(bundleInfo);
    }

    public void addArtifactUrl(String id, Version version, String url) {
        Map/* <Version, String> */byVersion = (Map) artifactUrls.get(id);
        if (byVersion == null) {
            byVersion = new HashMap();
            artifactUrls.put(id, byVersion);
        }
        byVersion.put(version, url);
    }
}
