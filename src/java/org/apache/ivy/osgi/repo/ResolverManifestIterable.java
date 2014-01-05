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

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.osgi.core.BundleInfoAdapter;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;

public class ResolverManifestIterable implements Iterable<ManifestAndLocation> {

    // We should support the interface DependencyResolver, but the API is not convenient to get
    // references to artifact
    private final BasicResolver resolver;

    public ResolverManifestIterable(BasicResolver resolver) {
        this.resolver = resolver;
    }

    public Iterator<ManifestAndLocation> iterator() {
        return new ResolverManifestIterator();
    }

    class ResolverManifestIterator implements Iterator<ManifestAndLocation> {

        private OrganisationEntry[] organisations;

        private int indexOrganisation = 0;

        private OrganisationEntry organisation;

        private ModuleEntry[] modules;

        private int indexModule = -1;

        private ModuleEntry module;

        private ManifestAndLocation next = null;

        private RevisionEntry[] revisions;

        private int indexRevision;

        private RevisionEntry revision;

        private Artifact[] artifacts;

        private int indexArtifact;

        private Artifact artifact;

        private ModuleRevisionId mrid;

        private ResolveData data;

        public ResolverManifestIterator() {
            organisations = resolver.listOrganisations();
            IvySettings settings = new IvySettings();
            ResolveEngine engine = new ResolveEngine(settings, new EventManager(), new SortEngine(
                    settings));
            data = new ResolveData(engine, new ResolveOptions());
        }

        public boolean hasNext() {
            while (next == null) {
                if (organisation == null) {
                    if (indexOrganisation >= organisations.length) {
                        return false;
                    }
                    organisation = organisations[indexOrganisation++];
                    modules = resolver.listModules(organisation);
                    indexModule = 0;
                    module = null;
                }
                if (module == null) {
                    if (indexModule >= modules.length) {
                        organisation = null;
                        continue;
                    }
                    module = modules[indexModule++];
                    revisions = resolver.listRevisions(module);
                    indexRevision = 0;
                    revision = null;
                }
                if (revision == null) {
                    if (indexRevision >= revisions.length) {
                        module = null;
                        continue;
                    }
                    revision = revisions[indexRevision++];
                    mrid = ModuleRevisionId.newInstance(organisation.getOrganisation(),
                        module.getModule(), revision.getRevision());
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mrid, false);
                    ResolvedModuleRevision dependency;
                    try {
                        dependency = resolver.getDependency(dd, data);
                    } catch (ParseException e) {
                        Message.error("Error while resolving " + mrid + " : " + e.getMessage());
                        revision = null;
                        continue;
                    }
                    if (dependency == null) {
                        revision = null;
                        continue;
                    }
                    ModuleDescriptor md = dependency.getDescriptor();
                    mrid = md.getModuleRevisionId();
                    artifacts = md.getAllArtifacts();
                    indexArtifact = 0;
                    artifact = null;
                }
                if (artifact == null) {
                    if (indexArtifact >= artifacts.length) {
                        revision = null;
                        continue;
                    }
                    artifact = artifacts[indexArtifact++];
                }
                ResolvedResource resource = resolver.doFindArtifactRef(artifact, null);
                if (resource == null) {
                    artifact = null;
                    continue;
                }
                JarInputStream in;
                try {
                    in = new JarInputStream(resource.getResource().openStream());
                } catch (IOException e) {
                    Message.warn("Unreadable jar " + resource.getResource().getName() + " ("
                            + e.getMessage() + ")");
                    artifact = null;
                    continue;
                }
                Manifest manifest;
                try {
                    manifest = in.getManifest();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // don't care
                    }
                }
                if (manifest == null) {
                    Message.debug("No manifest on " + artifact);
                } else {
                    URI uri = BundleInfoAdapter.buildIvyURI(artifact);
                    next = new ManifestAndLocation(manifest, uri, null);
                }
                artifact = null;
            }
            return true;
        }

        public ManifestAndLocation next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ManifestAndLocation manifest = next;
            next = null;
            return manifest;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}