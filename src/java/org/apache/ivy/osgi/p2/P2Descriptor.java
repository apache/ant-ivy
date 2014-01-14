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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.osgi.core.BundleArtifact;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ExecutionEnvironmentProfileProvider;
import org.apache.ivy.osgi.repo.EditableRepoDescriptor;
import org.apache.ivy.osgi.repo.ModuleDescriptorWrapper;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.util.Message;

public class P2Descriptor extends EditableRepoDescriptor {

    private Map<String, Map<Version, BundleInfo>> sourceTargetBundles = new HashMap<String, Map<Version, BundleInfo>>();

    private Map<String, Map<Version, BundleInfo>> sourceBundles = new HashMap<String, Map<Version, BundleInfo>>();

    public P2Descriptor(URI repoUri, ExecutionEnvironmentProfileProvider profileProvider) {
        super(repoUri, profileProvider);
    }

    public void addBundle(BundleInfo bundleInfo) {
        if (bundleInfo.isSource()) {
            if (bundleInfo.getSymbolicNameTarget() == null || bundleInfo.getVersionTarget() == null) {
                if (getLogLevel() <= Message.MSG_VERBOSE) {
                    Message.verbose("The source bundle " + bundleInfo.getSymbolicName()
                            + " did not declare its target. Ignoring it");
                }
                return;
            }
            Map<Version, BundleInfo> byVersion = sourceBundles.get(bundleInfo.getSymbolicName());
            if (byVersion == null) {
                byVersion = new HashMap<Version, BundleInfo>();
                sourceBundles.put(bundleInfo.getSymbolicName(), byVersion);
            }
            byVersion.put(bundleInfo.getVersion(), bundleInfo);

            Map<Version, BundleInfo> byTargetVersion = sourceTargetBundles.get(bundleInfo
                    .getSymbolicNameTarget());
            if (byTargetVersion == null) {
                byTargetVersion = new HashMap<Version, BundleInfo>();
                sourceTargetBundles.put(bundleInfo.getSymbolicNameTarget(), byTargetVersion);
            }
            BundleInfo old = byTargetVersion.put(bundleInfo.getVersionTarget(), bundleInfo);
            if (old != null && !old.equals(bundleInfo)) {
                if (getLogLevel() <= Message.MSG_VERBOSE) {
                    Message.verbose("Duplicate source for the bundle "
                            + bundleInfo.getSymbolicNameTarget() + "@"
                            + bundleInfo.getVersionTarget() + " : " + bundleInfo + " is replacing "
                            + old);
                }
            }
            return;
        }

        super.addBundle(bundleInfo);
    }

    public void finish() {
        sourceBundles = null;
        Set<String> bundleIds = getCapabilityValues(BundleInfo.BUNDLE_TYPE);
        if (bundleIds == null) {
            return;
        }
        for (String bundleId : bundleIds) {
            Set<ModuleDescriptorWrapper> modules = findModules(BundleInfo.BUNDLE_TYPE, bundleId);
            for (ModuleDescriptorWrapper mdw : modules) {
                String symbolicName = mdw.getBundleInfo().getSymbolicName();
                Map<Version, BundleInfo> byVersion = sourceTargetBundles.get(symbolicName);
                if (byVersion == null) {
                    continue;
                }
                BundleInfo source = byVersion.get(mdw.getBundleInfo().getVersion());
                if (source == null) {
                    continue;
                }
                for (BundleArtifact artifact : source.getArtifacts()) {
                    mdw.getBundleInfo().addArtifact(artifact);
                }
            }
        }
        sourceTargetBundles = null;
    }

    public void addArtifactUrl(String classifier, String id, Version version, URI uri, String format) {
        if (!classifier.equals("osgi.bundle")) {
            // we only support OSGi bundle, no Eclipse feature or anything else
            return;
        }
        ModuleDescriptorWrapper module = findModule(id, version);
        if (module != null) {
            addArtifact(module.getBundleInfo(), new BundleArtifact(false, uri, format));
            return;
        }

        // not found in the regular bundle. Let's look up in the source ones
        Map<Version, BundleInfo> byVersion = sourceBundles.get(id);
        if (byVersion == null) {
            return;
        }
        BundleInfo source = byVersion.get(version);
        if (source == null) {
            return;
        }
        addArtifact(source, new BundleArtifact(true, uri, format));
    }

    private void addArtifact(BundleInfo bundle, BundleArtifact artifact) {
        // find an existing artifact that might be a duplicate
        BundleArtifact same = null;
        for (BundleArtifact a : bundle.getArtifacts()) {
            if (a.isSource() == artifact.isSource()) {
                same = a;
                break;
            }
        }

        BundleArtifact best = artifact;

        if (same != null) {
            // we have two artifacts for the same bundle, let's choose a "packed" one
            if (artifact.getFormat() == null || same.getFormat() != null) {
                // the new one cannot be better
                return;
            }
            bundle.removeArtifact(same);
        }

        bundle.addArtifact(best);
    }
}
