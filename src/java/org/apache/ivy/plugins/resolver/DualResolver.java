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

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
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
        data = new ResolveData(data, doValidate(data));
        final ResolvedModuleRevision mr = ivyResolver.getDependency(dd, data);
        if (mr == null) {
            checkInterrupted();
            if (isAllownomd()) {
                Message.verbose("ivy resolver didn't find " + dd.getDependencyRevisionId()
                        + ": trying with artifact resolver");
                return artifactResolver.getDependency(dd, data);
            } else {
                return null;
            }
        } else {
            return new ResolvedModuleRevision(
                mr.getResolver(), this, mr.getDescriptor(), mr.getReport());
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

    public void abortPublishTransaction() throws IOException {
        ivyResolver.abortPublishTransaction();
        artifactResolver.abortPublishTransaction();
    }


    public void beginPublishTransaction(
            ModuleRevisionId module, boolean overwrite) throws IOException {
        ivyResolver.beginPublishTransaction(module, overwrite);
        artifactResolver.beginPublishTransaction(module, overwrite);
    }


    public void commitPublishTransaction() throws IOException {
        ivyResolver.commitPublishTransaction();
        artifactResolver.commitPublishTransaction();
    }

    public void dumpSettings() {
        if (ivyResolver == null || artifactResolver == null) {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        Message.verbose("\t" + getName() + " [dual " + ivyResolver.getName() + " "
                + artifactResolver.getName() + "]");
    }

    public boolean exists(Artifact artifact) {
        return artifactResolver.exists(artifact);
    }

    public boolean isAllownomd() {
        return allownomd;
    }

    public void setAllownomd(boolean allownomd) {
        this.allownomd = allownomd;
    }

}
