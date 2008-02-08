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
package org.apache.ivy.core.install;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishEngine;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.plugins.conflict.NoConflictManager;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.MatcherHelper;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.FilterHelper;

public class InstallEngine {
    private InstallEngineSettings settings;

    private ResolveEngine resolveEngine;

    private PublishEngine publishEngine;

    private SearchEngine searchEngine;

    public InstallEngine(InstallEngineSettings settings, SearchEngine searchEngine,
            ResolveEngine resolveEngine, PublishEngine publishEngine) {
        this.settings = settings;
        this.searchEngine = searchEngine;
        this.resolveEngine = resolveEngine;
        this.publishEngine = publishEngine;
    }

    public ResolveReport install(ModuleRevisionId mrid, String from, String to, boolean transitive,
            boolean validate, boolean overwrite, Filter artifactFilter, 
            String matcherName) throws IOException {
        if (artifactFilter == null) {
            artifactFilter = FilterHelper.NO_FILTER;
        }
        DependencyResolver fromResolver = settings.getResolver(from);
        DependencyResolver toResolver = settings.getResolver(to);
        if (fromResolver == null) {
            throw new IllegalArgumentException("unknown resolver " + from
                    + ". Available resolvers are: " + settings.getResolverNames());
        }
        if (toResolver == null) {
            throw new IllegalArgumentException("unknown resolver " + to
                    + ". Available resolvers are: " + settings.getResolverNames());
        }
        PatternMatcher matcher = settings.getMatcher(matcherName);
        if (matcher == null) {
            throw new IllegalArgumentException("unknown matcher " + matcherName
                    + ". Available matchers are: " + settings.getMatcherNames());
        }

        // build module file declaring the dependency
        Message.info(":: installing " + mrid + " ::");
        DependencyResolver oldDicator = resolveEngine.getDictatorResolver();
        boolean log = settings.logNotConvertedExclusionRule();
        try {
            settings.setLogNotConvertedExclusionRule(true);
            resolveEngine.setDictatorResolver(fromResolver);

            DefaultModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance(
                "apache", "ivy-install", "1.0"), settings.getStatusManager().getDefaultStatus(),
                    new Date());
            String resolveId = ResolveOptions.getDefaultResolveId(md);
            md.addConfiguration(new Configuration("default"));
            md.addConflictManager(new ModuleId(ExactPatternMatcher.ANY_EXPRESSION,
                    ExactPatternMatcher.ANY_EXPRESSION), ExactPatternMatcher.INSTANCE,
                new NoConflictManager());

            if (MatcherHelper.isExact(matcher, mrid)) {
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, mrid, false,
                        false, transitive);
                dd.addDependencyConfiguration("default", "*");
                md.addDependency(dd);
            } else {
                Collection mrids = searchEngine.findModuleRevisionIds(fromResolver, mrid, matcher);

                for (Iterator iter = mrids.iterator(); iter.hasNext();) {
                    ModuleRevisionId foundMrid = (ModuleRevisionId) iter.next();
                    Message.info("\tfound " + foundMrid + " to install: adding to the list");
                    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, foundMrid,
                            false, false, transitive);
                    dd.addDependencyConfiguration("default", "*");
                    md.addDependency(dd);
                }
            }

            // resolve using appropriate resolver
            ResolveReport report = new ResolveReport(md, resolveId);

            Message.info(":: resolving dependencies ::");
            ResolveOptions options = new ResolveOptions()
                                .setResolveId(resolveId)
                                .setConfs(new String[] {"default"})
                                .setValidate(validate);
            IvyNode[] dependencies = resolveEngine.getDependencies(md, options, report);
            report.setDependencies(Arrays.asList(dependencies), artifactFilter);

            Message.info(":: downloading artifacts to cache ::");
            resolveEngine.downloadArtifacts(report, artifactFilter, new DownloadOptions());

            // now that everything is in cache, we can publish all these modules
            Message.info(":: installing in " + to + " ::");
            for (int i = 0; i < dependencies.length; i++) {
                ModuleDescriptor depmd = dependencies[i].getDescriptor();
                if (depmd != null) {
                    ModuleRevisionId depMrid = depmd.getModuleRevisionId();
                    Message.verbose("installing " + depMrid);
                    boolean successfullyPublished = false;
                    try {
                        toResolver.beginPublishTransaction(depMrid, overwrite);
                        
                        // publish artifacts
                        ArtifactDownloadReport[] artifacts = 
                                report.getArtifactsReports(depMrid);
                        for (int j = 0; j < artifacts.length; j++) {
                            if (artifacts[j].getLocalFile() != null) {
                                toResolver.publish(artifacts[j].getArtifact(), 
                                    artifacts[j].getLocalFile(), overwrite);
                            }
                        }
                        
                        // publish metadata
                        File localIvyFile = dependencies[i]
                                                    .getModuleRevision().getReport().getLocalFile();
                        toResolver.publish(depmd.getMetadataArtifact(), localIvyFile, overwrite);
                        
                        // end module publish
                        toResolver.commitPublishTransaction();
                        successfullyPublished  = true;
                    } finally {
                        if (!successfullyPublished) {
                            toResolver.abortPublishTransaction();
                        }
                    }
                }
            }

            Message.info(":: install resolution report ::");

            // output report
            resolveEngine.outputReport(report, settings.getResolutionCacheManager(), options);

            return report;
        } finally {
            resolveEngine.setDictatorResolver(oldDicator);
            settings.setLogNotConvertedExclusionRule(log);
        }
    }

}
