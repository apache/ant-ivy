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
package org.apache.ivy.osgi.repo;

import java.net.URI;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.ivy.osgi.core.BundleArtifact;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.util.Message;

public class BundleRepoDescriptor extends EditableRepoDescriptor {

    private String name;

    private String lastModified;

    public BundleRepoDescriptor(URI baseUri, ExecutionEnvironmentProfileProvider profileProvider) {
        super(baseUri, profileProvider);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void populate(Iterator<ManifestAndLocation> it) {
        while (it.hasNext()) {
            ManifestAndLocation manifestAndLocation = it.next();
            try {
                BundleInfo bundleInfo = ManifestParser.parseManifest(manifestAndLocation
                        .getManifest());
                bundleInfo
                        .addArtifact(new BundleArtifact(false, manifestAndLocation.getUri(), null));
                addBundle(bundleInfo);
            } catch (ParseException e) {
                Message.error("Rejected " + manifestAndLocation.getUri() + ": " + e.getMessage());
            }
        }
    }

}
