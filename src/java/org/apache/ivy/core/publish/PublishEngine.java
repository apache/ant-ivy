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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    public Collection publish(ModuleRevisionId mrid, Collection srcArtifactPattern,
            String resolverName, PublishOptions options) throws IOException {
        Message.info(":: publishing :: " + mrid.getModuleId());
        Message.verbose("\tvalidate = " + options.isValidate());
        long start = System.currentTimeMillis();

        options.setSrcIvyPattern(settings.substitute(options.getSrcIvyPattern()));
        if (options.getPubrevision() == null) {
            options.setPubrevision(mrid.getRevision());
        }
        ModuleRevisionId pubmrid = ModuleRevisionId.newInstance(mrid, options.getPubrevision());
        File ivyFile;
        if (options.getSrcIvyPattern() != null) {
            ivyFile = new File(IvyPatternHelper.substitute(options.getSrcIvyPattern(),
                DefaultArtifact.newIvyArtifact(pubmrid, new Date())));
            if (!ivyFile.exists()) {
                throw new IllegalArgumentException("ivy file to publish not found for " + mrid
                        + ": call deliver before (" + ivyFile + ")");
            }
        } else {
            ResolutionCacheManager cacheManager = settings.getResolutionCacheManager();
            ivyFile = cacheManager.getResolvedIvyFileInCache(mrid);
            if (!ivyFile.exists()) {
                throw new IllegalStateException("ivy file not found in cache for " + mrid
                        + ": please resolve dependencies before publishing (" + ivyFile + ")");
            }
        }

        // let's find the resolved module descriptor
        ModuleDescriptor md = null;
        URL ivyFileURL = null;
        try {
            ivyFileURL = ivyFile.toURL();
            md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings, ivyFileURL,
                false);
            if (options.getSrcIvyPattern() != null) {
                if (options.isUpdate()) {
                    File tmp = File.createTempFile("ivy", ".xml");
                    tmp.deleteOnExit();

                    String[] confs = ConfigurationUtils.replaceWildcards(options.getConfs(), md);
                    Set confsToRemove = new HashSet(Arrays.asList(md.getConfigurationsNames()));
                    confsToRemove.removeAll(Arrays.asList(confs));

                    try {
                        XmlModuleDescriptorUpdater.update(settings, ivyFileURL, tmp, new HashMap(),
                            options.getStatus() == null ? md.getStatus() : options.getStatus(),
                            options.getPubrevision(), options.getPubdate() == null ? new Date()
                                    : options.getPubdate(), null, true, (String[]) confsToRemove
                                    .toArray(new String[confsToRemove.size()]));
                        ivyFile = tmp;
                        // we parse the new file to get updated module descriptor
                        md = XmlModuleDescriptorParser.getInstance().parseDescriptor(settings,
                            ivyFile.toURL(), false);
                        options.setSrcIvyPattern(ivyFile.getAbsolutePath());
                    } catch (SAXException e) {
                        throw new IllegalStateException("bad ivy file for " + mrid + ": " + ivyFile
                                + ": " + e);
                    }
                } else if (!options.getPubrevision().equals(
                            md.getModuleRevisionId().getRevision())) {
                    throw new IllegalArgumentException("cannot publish " + ivyFile + " as "
                            + options.getPubrevision()
                            + ": bad revision found in ivy file (Revision: "
                            + md.getModuleRevisionId().getRevision()
                            + "). Use forcedeliver or update.");
                }
            } else {
                md.setResolvedModuleRevisionId(pubmrid);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url obtained for file " + ivyFile);
        } catch (ParseException e) {
            throw new IllegalStateException("bad ivy file for " + mrid + ": " + ivyFile + ": " + e);
        }

        DependencyResolver resolver = settings.getResolver(resolverName);
        if (resolver == null) {
            throw new IllegalArgumentException("unknown resolver " + resolverName);
        }

        // collect all declared artifacts of this module
        Collection missing = publish(md, srcArtifactPattern, resolver, options);
        Message.verbose("\tpublish done (" + (System.currentTimeMillis() - start) + "ms)");
        return missing;
    }

    public Collection publish(ModuleDescriptor md, Collection srcArtifactPattern,
            DependencyResolver resolver, PublishOptions options) throws IOException {
        Collection missing = new ArrayList();
        Set artifactsSet = new HashSet();
        String[] confs = options.getConfs();
        if (confs == null || (confs.length == 1 && "*".equals(confs[0]))) {
            confs = md.getConfigurationsNames();
        }

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
                        extraArtifacts[i].getExtraAttributes()));
            }
        }
        boolean successfullyPublished = false;
        try {
            resolver.beginPublishTransaction(md.getModuleRevisionId(), options.isOverwrite());
            // for each declared published artifact in this descriptor, do:
            for (Iterator iter = artifactsSet.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact) iter.next();
                // copy the artifact using src patterns and resolver
                boolean published = false;
                for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext()
                        && !published;) {
                    String pattern = (String) iterator.next();
                    published = publish(
                        artifact, settings.substitute(pattern), resolver, options.isOverwrite());
                }
                if (!published) {
                    Message.info("missing artifact " + artifact + ":");
                    for (Iterator iterator = srcArtifactPattern.iterator(); iterator.hasNext();) {
                        String pattern = (String) iterator.next();
                        Message.info("\t"
                                + new File(IvyPatternHelper.substitute(pattern, artifact))
                                + " file does not exist");
                    }
                    missing.add(artifact);
                }
            }
            if (options.getSrcIvyPattern() != null) {
                Artifact artifact = MDArtifact.newIvyArtifact(md);
                if (!publish(
                        artifact, options.getSrcIvyPattern(), resolver, options.isOverwrite())) {
                    Message.info("missing ivy file for "
                            + md.getModuleRevisionId()
                            + ": "
                            + new File(IvyPatternHelper.substitute(options.getSrcIvyPattern(),
                                artifact)) + " file does not exist");
                    missing.add(artifact);
                }
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

    private boolean publish(Artifact artifact, String srcArtifactPattern,
            DependencyResolver resolver, boolean overwrite) throws IOException {
        File src = new File(IvyPatternHelper.substitute(srcArtifactPattern, artifact));
        
        IvyContext.getContext().checkInterrupted();
        //notify triggers that an artifact is about to be published
        eventManager.fireIvyEvent(
            new StartArtifactPublishEvent(resolver, artifact, src, overwrite));
        boolean successful = false; //set to true once the publish succeeds
        try {
            if (src.exists()) {
                resolver.publish(artifact, src, overwrite);
                successful = true;
            }
            return successful;
        } finally {
            //notify triggers that the publish is finished, successfully or not.
            eventManager.fireIvyEvent(
                new EndArtifactPublishEvent(resolver, artifact, src, overwrite, successful));
        }
    }
}
