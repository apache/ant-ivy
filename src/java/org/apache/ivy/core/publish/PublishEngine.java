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
package org.apache.ivy.core.publish;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.publish.EndArtifactPublishEvent;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.xml.UpdateOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.ConfigurationUtils;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class PublishEngine {
    private PublishEngineSettings settings;

    private EventManager eventManager;

    public PublishEngine(PublishEngineSettings settings, EventManager eventManager) {
        this.settings = settings;
        this.eventManager = eventManager;
    }

    /**
     * Publishes a module to the repository. The publish can update the ivy file to publish if
     * update is set to true. In this case it will use the given pubrevision, pubdate and status. If
     * pubdate is null it will default to the current date. If status is null it will default to the
     * current ivy file status (which itself defaults to integration if none is found). If update is
     * false, then if the revision is not the same in the ivy file than the one expected (given as
     * parameter), this method will fail with an IllegalArgumentException. pubdate and status are
     * not used if update is false. extra artifacts can be used to publish more artifacts than
     * actually declared in the ivy file. This can be useful to publish additional metadata or
     * reports. The extra artifacts array can be null (= no extra artifacts), and if non null only
     * the name, type, ext url and extra attributes of the artifacts are really used. Other methods
     * can return null safely.
     */
    public Collection<Artifact> publish(ModuleRevisionId mrid,
            Collection<String> srcArtifactPattern, String resolverName, PublishOptions options)
            throws IOException {
        Message.info(":: publishing :: " + mrid.getModuleId());
        Message.verbose("\tvalidate = " + options.isValidate());
        long start = System.currentTimeMillis();

        options.setSrcIvyPattern(settings.substitute(options.getSrcIvyPattern()));
        if (options.getPubBranch() == null) {
            options.setPubbranch(mrid.getBranch());
        }
        if (options.getPubrevision() == null) {
            options.setPubrevision(mrid.getRevision());
        }
        ModuleRevisionId pubmrid = ModuleRevisionId.newInstance(mrid, options.getPubBranch(),
            options.getPubrevision());

        // let's find the resolved module descriptor
        ModuleDescriptor md = null;
        if (options.getSrcIvyPattern() != null) {
            File ivyFile = settings.resolveFile(IvyPatternHelper.substitute(
                options.getSrcIvyPattern(), DefaultArtifact.newIvyArtifact(pubmrid, new Date())));
            if (!ivyFile.exists()) {
                throw new IllegalArgumentException("ivy file to publish not found for " + mrid
                        + ": call deliver before (" + ivyFile + ")");
            }

            URL ivyFileURL = ivyFile.toURI().toURL();
            try {
                md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings, ivyFileURL,
                    false);

                if (options.isUpdate()) {
                    File tmp = File.createTempFile("ivy", ".xml");
                    tmp.deleteOnExit();

                    String[] confs = ConfigurationUtils.replaceWildcards(options.getConfs(), md);
                    Set<String> confsToRemove = new HashSet<String>(Arrays.asList(md
                            .getConfigurationsNames()));
                    confsToRemove.removeAll(Arrays.asList(confs));

                    try {
                        XmlModuleDescriptorUpdater.update(
                            ivyFileURL,
                            tmp,
                            new UpdateOptions()
                                    .setSettings(settings)
                                    .setStatus(
                                        options.getStatus() == null ? md.getStatus() : options
                                                .getStatus())
                                    .setRevision(options.getPubrevision())
                                    .setBranch(options.getPubBranch())
                                    .setPubdate(
                                        options.getPubdate() == null ? new Date() : options
                                                .getPubdate())
                                    .setMerge(options.isMerge())
                                    .setMergedDescriptor(md)
                                    .setConfsToExclude(
                                        confsToRemove.toArray(new String[confsToRemove.size()])));
                        ivyFile = tmp;
                        // we parse the new file to get updated module descriptor
                        md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                            ivyFile.toURI().toURL(), false);
                        options.setSrcIvyPattern(ivyFile.getAbsolutePath());
                    } catch (SAXException e) {
                        throw new IllegalStateException("bad ivy file for " + mrid + ": " + ivyFile
                                + ": " + e);
                    }
                } else if (!options.getPubrevision().equals(md.getModuleRevisionId().getRevision())) {
                    throw new IllegalArgumentException("cannot publish " + ivyFile + " as "
                            + options.getPubrevision()
                            + ": bad revision found in ivy file (Revision: "
                            + md.getModuleRevisionId().getRevision()
                            + "). Use forcedeliver or update.");
                }
            } catch (ParseException e) {
                throw new IllegalStateException("bad ivy file for " + mrid + ": " + ivyFile + ": "
                        + e);
            }
        } else {
            ResolutionCacheManager cacheManager = settings.getResolutionCacheManager();
            try {
                md = cacheManager.getResolvedModuleDescriptor(mrid);
            } catch (ParseException e) {
                throw new IllegalStateException("bad ivy file in cache for " + mrid + ": " + e);
            }
            md.setResolvedModuleRevisionId(pubmrid);
        }

        DependencyResolver resolver = settings.getResolver(resolverName);
        if (resolver == null) {
            throw new IllegalArgumentException("unknown resolver " + resolverName);
        }

        // collect all declared artifacts of this module
        Collection<Artifact> missing = publish(md, srcArtifactPattern, resolver, options);
        Message.verbose("\tpublish done (" + (System.currentTimeMillis() - start) + "ms)");
        return missing;
    }

    public Collection<Artifact> publish(ModuleDescriptor md, Collection<String> srcArtifactPattern,
            DependencyResolver resolver, PublishOptions options) throws IOException {
        Collection<Artifact> missing = new ArrayList<Artifact>();
        Set<Artifact> artifactsSet = new LinkedHashSet<Artifact>();
        String[] confs = ConfigurationUtils.replaceWildcards(options.getConfs(), md);

        for (int i = 0; i < confs.length; i++) {
            Artifact[] artifacts = md.getArtifacts(confs[i]);
            for (int j = 0; j < artifacts.length; j++) {
                artifactsSet.add(artifacts[j]);
            }
        }
        Artifact[] extraArtifacts = options.getExtraArtifacts();
        if (extraArtifacts != null) {
            for (int i = 0; i < extraArtifacts.length; i++) {
                artifactsSet.add(new MDArtifact(md, extraArtifacts[i].getName(), extraArtifacts[i]
                        .getType(), extraArtifacts[i].getExt(), extraArtifacts[i].getUrl(),
                        extraArtifacts[i].getQualifiedExtraAttributes()));
            }
        }
        // now collects artifacts files
        Map<Artifact, File> artifactsFiles = new LinkedHashMap<Artifact, File>();
        for (Artifact artifact : artifactsSet) {
            for (String pattern : srcArtifactPattern) {
                File artifactFile = settings.resolveFile(IvyPatternHelper.substitute(
                    settings.substitute(pattern), artifact));
                if (artifactFile.exists()) {
                    artifactsFiles.put(artifact, artifactFile);
                    break;
                }
            }
            if (!artifactsFiles.containsKey(artifact)) {
                StringBuffer sb = new StringBuffer();
                sb.append("missing artifact " + artifact + ":\n");
                for (String pattern : srcArtifactPattern) {
                    sb.append("\t"
                            + settings.resolveFile(IvyPatternHelper.substitute(pattern, artifact))
                            + " file does not exist\n");
                }
                if (options.isWarnOnMissing() || options.isHaltOnMissing()) {
                    Message.warn(sb.toString());
                } else {
                    Message.verbose(sb.toString());
                }
                if (options.isHaltOnMissing()) {
                    throw new IOException("missing artifact " + artifact);
                }
                missing.add(artifact);
            }
        }
        if (options.getSrcIvyPattern() != null) {
            Artifact artifact = MDArtifact.newIvyArtifact(md);
            File artifactFile = settings.resolveFile(IvyPatternHelper.substitute(
                options.getSrcIvyPattern(), artifact));
            if (!artifactFile.exists()) {
                String msg = "missing ivy file for " + md.getModuleRevisionId() + ": \n"
                        + artifactFile + " file does not exist";
                if (options.isWarnOnMissing() || options.isHaltOnMissing()) {
                    Message.warn(msg);
                } else {
                    Message.verbose(msg);
                }
                if (options.isHaltOnMissing()) {
                    throw new IOException("missing ivy artifact " + artifact);
                }
                missing.add(artifact);
            } else {
                artifactsFiles.put(artifact, artifactFile);
            }
        }

        // and now do actual publishing
        boolean successfullyPublished = false;
        try {
            resolver.beginPublishTransaction(md.getModuleRevisionId(), options.isOverwrite());
            // for each declared published artifact in this descriptor, do:
            for (Entry<Artifact, File> entry : artifactsFiles.entrySet()) {
                Artifact artifact = entry.getKey();
                File artifactFile = entry.getValue();
                publish(artifact, artifactFile, resolver, options.isOverwrite());
            }
            resolver.commitPublishTransaction();
            successfullyPublished = true;
        } finally {
            if (!successfullyPublished) {
                resolver.abortPublishTransaction();
            }
        }
        return missing;
    }

    private void publish(Artifact artifact, File src, DependencyResolver resolver, boolean overwrite)
            throws IOException {
        IvyContext.getContext().checkInterrupted();
        // notify triggers that an artifact is about to be published
        eventManager
                .fireIvyEvent(new StartArtifactPublishEvent(resolver, artifact, src, overwrite));
        boolean successful = false; // set to true once the publish succeeds
        try {
            if (src.exists()) {
                resolver.publish(artifact, src, overwrite);
                successful = true;
            }
        } finally {
            // notify triggers that the publish is finished, successfully or not.
            eventManager.fireIvyEvent(new EndArtifactPublishEvent(resolver, artifact, src,
                    overwrite, successful));
        }
    }
}
