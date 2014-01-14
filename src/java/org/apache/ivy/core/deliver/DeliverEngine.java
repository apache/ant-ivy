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
package org.apache.ivy.core.deliver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.xml.UpdateOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.ConfigurationUtils;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class DeliverEngine {
    private DeliverEngineSettings settings;

    public DeliverEngine(DeliverEngineSettings settings) {
        this.settings = settings;
    }

    /**
     * Delivers a resolved ivy file based upon last resolve call status. If resolve report file
     * cannot be found in cache, then it throws an IllegalStateException (maybe resolve has not been
     * called before ?).
     * 
     * @param revision
     *            the revision to which the module should be delivered
     * @param destIvyPattern
     *            the pattern to which the delivered ivy file should be written
     * @param options
     *            the options with which deliver should be done
     */
    public void deliver(String revision, String destIvyPattern, DeliverOptions options)
            throws IOException, ParseException {
        String resolveId = options.getResolveId();
        if (resolveId == null) {
            throw new IllegalArgumentException("A resolveId must be specified for delivering.");
        }
        File[] files = getCache().getConfigurationResolveReportsInCache(resolveId);
        if (files.length == 0) {
            throw new IllegalStateException("No previous resolve found for id '" + resolveId
                    + "' Please resolve dependencies before delivering.");
        }
        XmlReportParser parser = new XmlReportParser();
        parser.parse(files[0]);
        ModuleRevisionId mrid = parser.getResolvedModule();
        deliver(mrid, revision, destIvyPattern, options);
    }

    private ResolutionCacheManager getCache() {
        return settings.getResolutionCacheManager();
    }

    /**
     * Delivers a resolved ivy file based upon last resolve call status. If resolve report file
     * cannot be found in cache, then it throws an IllegalStateException (maybe resolve has not been
     * called before ?).
     * 
     * @param mrid
     *            the module revision id of the module to deliver
     * @param revision
     *            the revision to which the module should be delivered
     * @param destIvyPattern
     *            the pattern to which the delivered ivy file should be written
     * @param options
     *            the options with which deliver should be done
     */
    public void deliver(ModuleRevisionId mrid, String revision, String destIvyPattern,
            DeliverOptions options) throws IOException, ParseException {
        Message.info(":: delivering :: " + mrid + " :: " + revision + " :: " + options.getStatus()
                + " :: " + options.getPubdate());
        Message.verbose("\toptions = " + options);
        long start = System.currentTimeMillis();
        destIvyPattern = settings.substitute(destIvyPattern);

        // 1) find the resolved module descriptor in cache
        ModuleDescriptor md = getCache().getResolvedModuleDescriptor(mrid);
        md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(md.getModuleRevisionId(),
            options.getPubBranch() == null ? mrid.getBranch() : options.getPubBranch(), revision));
        md.setResolvedPublicationDate(options.getPubdate());

        // 2) parse resolvedRevisions From properties file
        Map resolvedRevisions = new HashMap(); // Map (ModuleId -> String revision)
        Map resolvedBranches = new HashMap(); // Map (ModuleId -> String branch)
        Map dependenciesStatus = new HashMap(); // Map (ModuleId -> String status)
        File ivyProperties = getCache().getResolvedIvyPropertiesInCache(mrid);
        if (!ivyProperties.exists()) {
            throw new IllegalStateException("ivy properties not found in cache for " + mrid
                    + "; please resolve dependencies before delivering!");
        }
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(ivyProperties);
        props.load(in);
        in.close();

        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String depMridStr = (String) iter.next();
            String[] parts = props.getProperty(depMridStr).split(" ");
            ModuleRevisionId decodedMrid = ModuleRevisionId.decode(depMridStr);
            if (options.isResolveDynamicRevisions()) {
                resolvedRevisions.put(decodedMrid, parts[0]);
                if (parts.length >= 4) {
                    if (parts[3] != null && !"null".equals(parts[3])) {
                        resolvedBranches.put(decodedMrid, parts[3]);
                    }
                }
            }
            dependenciesStatus.put(decodedMrid, parts[1]);

            if (options.isReplaceForcedRevisions()) {
                if (parts.length <= 2) {
                    // maybe the properties file was generated by an older Ivy version
                    // so it is possible that this part doesn't exist.
                    throw new IllegalStateException("ivy properties file generated by an older"
                            + " version of Ivy which doesn't support replacing forced revisions!");
                }

                resolvedRevisions.put(decodedMrid, parts[2]);
            }
        }

        // 3) use pdrResolver to resolve dependencies info
        Map resolvedDependencies = new HashMap(); // Map (ModuleRevisionId -> String revision)
        DependencyDescriptor[] dependencies = md.getDependencies();
        for (int i = 0; i < dependencies.length; i++) {
            String rev = (String) resolvedRevisions.get(dependencies[i].getDependencyRevisionId());
            if (rev == null) {
                rev = dependencies[i].getDependencyRevisionId().getRevision();
            }
            String bra = (String) resolvedBranches.get(dependencies[i].getDependencyRevisionId());
            if (bra == null || "null".equals(bra)) {
                bra = dependencies[i].getDependencyRevisionId().getBranch();
            }
            String depStatus = (String) dependenciesStatus.get(dependencies[i]
                    .getDependencyRevisionId());
            ModuleRevisionId mrid2 = null;
            if (bra == null) {
                mrid2 = ModuleRevisionId
                        .newInstance(dependencies[i].getDependencyRevisionId(), rev);
            } else {
                mrid2 = ModuleRevisionId.newInstance(dependencies[i].getDependencyRevisionId(),
                    bra, rev);
            }
            resolvedDependencies.put(dependencies[i].getDependencyRevisionId(), options
                    .getPdrResolver().resolve(md, options.getStatus(), mrid2, depStatus));
        }

        // 4) copy the source resolved ivy to the destination specified,
        // updating status, revision and dependency revisions obtained by
        // PublishingDependencyRevisionResolver
        File publishedIvy = settings.resolveFile(IvyPatternHelper.substitute(destIvyPattern,
            md.getResolvedModuleRevisionId()));
        Message.info("\tdelivering ivy file to " + publishedIvy);

        String[] confs = ConfigurationUtils.replaceWildcards(options.getConfs(), md);
        Set confsToRemove = new HashSet(Arrays.asList(md.getConfigurationsNames()));
        confsToRemove.removeAll(Arrays.asList(confs));

        try {
            UpdateOptions opts = new UpdateOptions()
                    .setSettings(settings)
                    .setResolvedRevisions(resolvedDependencies)
                    .setStatus(options.getStatus())
                    .setRevision(revision)
                    .setBranch(options.getPubBranch())
                    .setPubdate(options.getPubdate())
                    .setGenerateRevConstraint(options.isGenerateRevConstraint())
                    .setMerge(options.isMerge())
                    .setMergedDescriptor(md)
                    .setConfsToExclude(
                        (String[]) confsToRemove.toArray(new String[confsToRemove.size()]));
            if (!resolvedBranches.isEmpty()) {
                opts = opts.setResolvedBranches(resolvedBranches);
            }
            Resource res = md.getResource();
            XmlModuleDescriptorUpdater.update(res.openStream(), res, publishedIvy, opts);
        } catch (SAXException ex) {
            throw new RuntimeException("bad ivy file in cache for " + mrid, ex);
        }

        Message.verbose("\tdeliver done (" + (System.currentTimeMillis() - start) + "ms)");
    }
}
