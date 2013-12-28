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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.repo.EditableRepoDescriptor;
import org.apache.ivy.osgi.repo.ModuleDescriptorWrapper;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;

public class P2Descriptor extends EditableRepoDescriptor {

    private long timestamp;

    private Map<String, Map<Version, String>> artifactUrlPatterns = new HashMap<String, Map<Version, String>>();

    private Map<String, Map<String, URI>> sourceURIs = new HashMap<String, Map<String, URI>>();

    public P2Descriptor(URI repoUri, ExecutionEnvironmentProfileProvider profileProvider) {
        super(repoUri, profileProvider);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addBundle(BundleInfo bundleInfo) {
        if (bundleInfo.isSource()) {
            if (bundleInfo.getSymbolicNameTarget() == null || bundleInfo.getVersionTarget() == null) {
                if (getLogLevel() <= Message.MSG_VERBOSE) {
                    Message.verbose("The source bundle " + bundleInfo.getSymbolicName()
                            + " did declare its target. Ignoring it");
                }
                return;
            }
            Map<String, URI> byVersion = sourceURIs.get(bundleInfo.getSymbolicNameTarget());
            if (byVersion == null) {
                byVersion = new HashMap<String, URI>();
                sourceURIs.put(bundleInfo.getSymbolicNameTarget(), byVersion);
            }
            URI sourceUri = getArtifactURI(bundleInfo);
            if (sourceUri == null) {
                if (getLogLevel() <= Message.MSG_VERBOSE) {
                    Message.verbose("The source bundle " + bundleInfo.getSymbolicName()
                            + " has no actual artifact. Ignoring it");
                }
                return;
            }
            URI old = byVersion.put(bundleInfo.getVersionTarget().toString(), sourceUri);
            if (old != null && !old.equals(sourceUri)) {
                if (getLogLevel() <= Message.MSG_VERBOSE) {
                    Message.verbose("Duplicate source for the bundle "
                            + bundleInfo.getSymbolicNameTarget() + "@"
                            + bundleInfo.getVersionTarget() + " : " + sourceUri + " is replacing "
                            + old);
                }
            }
            return;
        }

        // before transforming it and adding it into the repo, let's add the artifacts
        // and if no artifact, then no bundle
        bundleInfo.setUri(getArtifactURI(bundleInfo));
        super.addBundle(bundleInfo);
    }

    private URI getArtifactURI(BundleInfo bundleInfo) {
        Map<Version, String> urlPatternsByVersion = artifactUrlPatterns.get(bundleInfo
                .getSymbolicName());
        if (urlPatternsByVersion != null) {
            String urlPattern = urlPatternsByVersion.get(bundleInfo.getVersion());
            if (urlPattern != null) {
                String url = urlPattern.replaceAll("\\$\\{id\\}", bundleInfo.getSymbolicName());
                url = url.replaceAll("\\$\\{version\\}", bundleInfo.getVersion().toString());
                try {
                    return new URI(url);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Unable to build the artifact uri of " + bundleInfo,
                            e);
                }
            }
        }
        return null;
    }

    public void addArtifactUrl(String classifier, String id, Version version, String url) {
        if (!classifier.equals("osgi.bundle")) {
            // we only support OSGi bundle, no Eclipse feature or anything else
            return;
        }
        Map<Version, String> byVersion = artifactUrlPatterns.get(id);
        if (byVersion == null) {
            byVersion = new HashMap<Version, String>();
            artifactUrlPatterns.put(id, byVersion);
        }
        byVersion.put(version, url);
    }

    public void finish() {
        artifactUrlPatterns = null;
        Set<String> bundleNames = getCapabilityValues(BundleInfo.BUNDLE_TYPE);
        if (bundleNames == null) {
            return;
        }
        for (String bundleName : bundleNames) {
            Set<ModuleDescriptorWrapper> modules = findModule(BundleInfo.BUNDLE_TYPE, bundleName);
            for (ModuleDescriptorWrapper mdw : modules) {
                String symbolicName = mdw.getBundleInfo().getSymbolicName();
                Map<String, URI> byVersion = sourceURIs.get(symbolicName);
                if (byVersion == null) {
                    continue;
                }
                String rev = mdw.getBundleInfo().getVersion().toString();
                URI source = (URI) byVersion.get(rev);
                if (source == null) {
                    continue;
                }
                mdw.setSource(source);
            }
        }
        sourceURIs = null;
    }
}
