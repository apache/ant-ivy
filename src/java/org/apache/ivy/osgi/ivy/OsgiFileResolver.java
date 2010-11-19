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
package org.apache.ivy.osgi.ivy;

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.osgi.ivy.internal.FilePackageScanner;
import org.apache.ivy.osgi.ivy.internal.JarEntryResource;
import org.apache.ivy.osgi.ivy.internal.JarFileRepository;
import org.apache.ivy.osgi.util.ZipUtil;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.repository.file.FileResource;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;


/**
 * An OSGi file system resolver.
 * 
 * @author alex@radeski.net
 */
public class OsgiFileResolver extends FileSystemResolver {
    
    private final FilePackageScanner packageScanner = new FilePackageScanner();

    public OsgiFileResolver() {
        setRepository(new JarFileRepository());
    }

    @Override
    protected ResolvedResource findArtifactRef(Artifact artifact, Date date) {
        Message.debug(format("\tfind artifact ref: artifact=%s, date=%s", artifact, date));

        final ModuleRevisionId newMrid = artifact.getModuleRevisionId();
        ResolvedResource resolvedResource = findResourceUsingPatterns(newMrid,
                getArtifactPatterns(),
                artifact,
                getDefaultRMDParser(artifact.getModuleRevisionId().getModuleId()),
                date);

        Message.debug(format("\t\tfind artifact ref: mrid=%s, resource=%s", newMrid, resolvedResource));

        if (resolvedResource == null) {
            Message.debug("\t\tfind artifact file ref: resource was null");
            return null;
        }

        final Resource resource = resolvedResource.getResource();
        if ((resource instanceof FileResource) && ((FileResource) resource).getFile().isDirectory()) {
            FileResource dirResource = (FileResource) resource;
            try {
                final File bundleZipFile = File.createTempFile("ivy-osgi-" + newMrid, ".zip");
                ZipUtil.zip(dirResource.getFile(), new FileOutputStream(bundleZipFile));
                Message.debug("\t\tfind artifact ref: zip file=" + bundleZipFile);
                return new ResolvedResource(new FileResource(dirResource.getRepository(), bundleZipFile), resolvedResource.getRevision());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create temp zip file for bundle:" + newMrid);
            }
        }

        Message.debug("\t\tfind artifact ref: resource=" + resolvedResource);
        return resolvedResource;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        packageScanner.scanAllPackageExportHeaders(getIvyPatterns(), getSettings());
        
        Message.debug(format("\tfind ivy file ref: dd=%s, data=%s", dd, data));

        final ModuleRevisionId newMrid = dd.getDependencyRevisionId();
        final ResolvedResource bundleResolvedResource = findResourceUsingPatterns(newMrid,
                getIvyPatterns(),
                DefaultArtifact.newIvyArtifact(newMrid, data.getDate()),
                getRMDParser(dd, data),
                data.getDate());


        if (bundleResolvedResource == null) {
            Message.debug("\tfind ivy file ref: resource was null");
            return null;
        }

        final Resource bundleResource = bundleResolvedResource.getResource();

        Resource res = null;
        if ((bundleResource instanceof FileResource) && ((FileResource) bundleResource).getFile().isDirectory()) {
            final FileResource fileResource = (FileResource) bundleResource;
            res = new FileResource((FileRepository) getRepository(), new File(fileResource.getFile(), "META-INF/MANIFEST.MF"));
        } else if (bundleResource.getName().toUpperCase().endsWith(".JAR")) {
            res = new JarEntryResource(bundleResource, "META-INF/MANIFEST.MF");
        }

        ResolvedResource resolvedResource = new ResolvedResource(res, bundleResolvedResource.getRevision());

        Message.debug(format("\tfind ivy file ref: resource=%s", bundleResolvedResource));

        return resolvedResource;
    }


//    protected ModuleRevisionId modifyModuleRevisionId(final ModuleRevisionId oldMrid) {
//        String revision = oldMrid.getRevision();
//        try {
//            VersionRange versionRange = new VersionRange(oldMrid.getRevision());
//            revision = versionRange.toIvyRevision();
//        } catch (ParseException nfe) {
//            // Do nothing as we fallback to default behaviour
//        }
//        final ModuleRevisionId newMrid = ModuleRevisionId.newInstance(oldMrid.getOrganisation(),
//                oldMrid.getName(),
//                oldMrid.getBranch(),
//                revision,
//                oldMrid.getExtraAttributes());
//        return newMrid;
//    }

}