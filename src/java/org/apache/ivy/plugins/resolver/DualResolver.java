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
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.util.ResolvedModuleRevisionProxy;
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
    private DependencyResolver _ivyResolver;

    private DependencyResolver _artifactResolver;

    private boolean _allownomd = true;

    public void add(DependencyResolver resolver) {
        if (_ivyResolver == null) {
            _ivyResolver = resolver;
        } else if (_artifactResolver == null) {
            _artifactResolver = resolver;
        } else {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        if (_ivyResolver == null || _artifactResolver == null) {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        data = new ResolveData(data, doValidate(data));
        final ResolvedModuleRevision mr = _ivyResolver.getDependency(dd, data);
        if (mr == null) {
            checkInterrupted();
            if (isAllownomd()) {
                Message.verbose("ivy resolver didn't find " + dd.getDependencyRevisionId()
                        + ": trying with artifact resolver");
                return _artifactResolver.getDependency(dd, data);
            } else {
                return null;
            }
        } else {
            return new ResolvedModuleRevisionProxy(mr, this);
        }
    }

    public void reportFailure() {
        _ivyResolver.reportFailure();
        _artifactResolver.reportFailure();
    }

    public void reportFailure(Artifact art) {
        _ivyResolver.reportFailure(art);
        _artifactResolver.reportFailure(art);
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        return _artifactResolver.download(artifacts, options);
    }

    public DependencyResolver getArtifactResolver() {
        return _artifactResolver;
    }

    public void setArtifactResolver(DependencyResolver artifactResolver) {
        _artifactResolver = artifactResolver;
    }

    public DependencyResolver getIvyResolver() {
        return _ivyResolver;
    }

    public void setIvyResolver(DependencyResolver ivyResolver) {
        _ivyResolver = ivyResolver;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        if ("ivy".equals(artifact.getType())) {
            _ivyResolver.publish(artifact, src, overwrite);
        } else {
            _artifactResolver.publish(artifact, src, overwrite);
        }
    }

    public void dumpSettings() {
        if (_ivyResolver == null || _artifactResolver == null) {
            throw new IllegalStateException(
                    "exactly two resolvers must be added: ivy(1) and artifact(2) one");
        }
        Message.verbose("\t" + getName() + " [dual " + _ivyResolver.getName() + " "
                + _artifactResolver.getName() + "]");
    }

    public boolean exists(Artifact artifact) {
        return _artifactResolver.exists(artifact);
    }

    public boolean isAllownomd() {
        return _allownomd;
    }

    public void setAllownomd(boolean allownomd) {
        _allownomd = allownomd;
    }
}
