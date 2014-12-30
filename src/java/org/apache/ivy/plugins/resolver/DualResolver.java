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
import java.text.ParseException;
import java.util.Arrays;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;

/**
 * DualResolver is used to resolve dependencies with one dependency revolver, called ivy resolver,
 * and then download artifacts found in the resolved dependencies from a second dependency resolver,
 * called artifact resolver. It is especially useful with resolvers using repository where there is
 * a lot of artifact, but no ivy file, like the maven ibiblio repository. If no ivy file is found by
 * the ivy resolver, the artifact resolver is used to check if there is artifact corresponding to
 * the request (with default ivy file). For artifact download, however, only the artifact resolver
 * is used. Exactly two resolvers should be added to this resolver for it to work properly. The
 * first resolver added if the ivy resolver, the second is the artifact one.
 */
public class DualResolver extends AbstractResolver {
    public static final String DESCRIPTOR_OPTIONAL = "optional";

    public static final String DESCRIPTOR_REQUIRED = "required";

    private DependencyResolver ivyResolver;

    private DependencyResolver artifactResolver;

    private boolean allownomd = true;

    public void add(DependencyResolver resolver) {
        if (ivyResolver == null) {
            ivyResolver = resolver;
        } else if (artifactResolver == null) {
            artifactResolver = resolver;
        } else {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        if (ivyResolver == null || artifactResolver == null) {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        ResolvedModuleRevision resolved = data.getCurrentResolvedModuleRevision();

        data = new ResolveData(data, doValidate(data));
        final ResolvedModuleRevision mr = ivyResolver.getDependency(dd, data);
        if (mr == null) {
            checkInterrupted();
            if (isAllownomd()) {
                Message.verbose("ivy resolver didn't find " + dd
                        + ": trying with artifact resolver");
                return artifactResolver.getDependency(dd, data);
            } else {
                return null;
            }
        } else {
            if (mr == resolved) {
                // nothing has actually been resolved here, we don't need to touch the returned rmr
                return mr;
            }
            return new ResolvedModuleRevision(mr.getResolver(), this, mr.getDescriptor(),
                    mr.getReport(), mr.isForce());
        }
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        return ivyResolver.findIvyFileRef(dd, data);
    }

    public void reportFailure() {
        ivyResolver.reportFailure();
        artifactResolver.reportFailure();
    }

    public void reportFailure(Artifact art) {
        ivyResolver.reportFailure(art);
        artifactResolver.reportFailure(art);
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        return artifactResolver.download(artifacts, options);
    }

    public DependencyResolver getArtifactResolver() {
        return artifactResolver;
    }

    public void setArtifactResolver(DependencyResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    public DependencyResolver getIvyResolver() {
        return ivyResolver;
    }

    public void setIvyResolver(DependencyResolver ivyResolver) {
        this.ivyResolver = ivyResolver;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        if ("ivy".equals(artifact.getType())) {
            ivyResolver.publish(artifact, src, overwrite);
        } else {
            artifactResolver.publish(artifact, src, overwrite);
        }
    }

    @Override
    public void abortPublishTransaction() throws IOException {
        ivyResolver.abortPublishTransaction();
        artifactResolver.abortPublishTransaction();
    }

    @Override
    public void beginPublishTransaction(ModuleRevisionId module, boolean overwrite)
            throws IOException {
        ivyResolver.beginPublishTransaction(module, overwrite);
        artifactResolver.beginPublishTransaction(module, overwrite);
    }

    @Override
    public void commitPublishTransaction() throws IOException {
        ivyResolver.commitPublishTransaction();
        artifactResolver.commitPublishTransaction();
    }

    @Override
    public void dumpSettings() {
        if (ivyResolver == null || artifactResolver == null) {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        Message.verbose("\t" + getName() + " [dual " + ivyResolver.getName() + " "
                + artifactResolver.getName() + "]");
    }

    @Override
    public boolean exists(Artifact artifact) {
        if (artifact.isMetadata()) {
            return ivyResolver.exists(artifact);
        } else {
            return artifactResolver.exists(artifact);
        }
    }

    @Override
    public ArtifactOrigin locate(Artifact artifact) {
        if (artifact.isMetadata()) {
            return ivyResolver.locate(artifact);
        } else {
            return artifactResolver.locate(artifact);
        }
    }

    @Override
    public ArtifactDownloadReport download(ArtifactOrigin artifact, DownloadOptions options) {
        if (artifact.getArtifact().isMetadata()) {
            return ivyResolver.download(artifact, options);
        } else {
            return artifactResolver.download(artifact, options);
        }
    }

    public boolean isAllownomd() {
        return allownomd;
    }

    public void setAllownomd(boolean allownomd) {
        Message.deprecated("allownomd is deprecated, please use descriptor=\""
                + (allownomd ? DESCRIPTOR_OPTIONAL : DESCRIPTOR_REQUIRED) + "\" instead");
        this.allownomd = allownomd;
    }

    /**
     * Sets the module descriptor presence rule. Should be one of {@link #DESCRIPTOR_REQUIRED} or
     * {@link #DESCRIPTOR_OPTIONAL}.
     * 
     * @param descriptorRule
     *            the descriptor rule to use with this resolver.
     */
    public void setDescriptor(String descriptorRule) {
        if (DESCRIPTOR_REQUIRED.equals(descriptorRule)) {
            allownomd = false;
        } else if (DESCRIPTOR_OPTIONAL.equals(descriptorRule)) {
            allownomd = true;
        } else {
            throw new IllegalArgumentException("unknown descriptor rule '" + descriptorRule
                    + "'. Allowed rules are: "
                    + Arrays.asList(new String[] {DESCRIPTOR_REQUIRED, DESCRIPTOR_OPTIONAL}));
        }
    }

}
