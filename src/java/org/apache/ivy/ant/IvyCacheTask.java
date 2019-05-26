/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;

import static org.apache.ivy.util.StringUtils.splitToArray;

/**
 * Base class for the cache path related classes: cachepath and cachefileset. Most of the behaviour
 * is common to the two, since only the produced element differs.
 */
public abstract class IvyCacheTask extends IvyPostResolveTask {

    protected List<ArtifactDownloadReport> getArtifactReports() throws BuildException,
            ParseException {
        List<ArtifactDownloadReport> ret = new ArrayList<>();
        for (ArtifactDownloadReport artifactReport : getAllArtifactReports()) {
            if (getArtifactFilter().accept(artifactReport.getArtifact())) {
                ret.add(artifactReport);
            }
        }

        return ret;
    }

    private Collection<ArtifactDownloadReport> getAllArtifactReports() throws ParseException {
        String[] confs = splitToArray(getConf());
        Collection<ArtifactDownloadReport> all = new LinkedHashSet<>();

        ResolveReport report = getResolvedReport();
        if (report != null) {
            Message.debug("using internal report instance to get artifacts list");
            for (String conf : confs) {
                ConfigurationResolveReport configurationReport = report
                        .getConfigurationReport(conf);
                if (configurationReport == null) {
                    throw new BuildException("bad confs provided: " + conf
                            + " not found among " + Arrays.asList(report.getConfigurations()));
                }
                for (ModuleRevisionId revId : configurationReport.getModuleRevisionIds()) {
                    all.addAll(Arrays.asList(configurationReport.getDownloadReports(revId)));
                }
            }
        } else {
            Message.debug("using stored report to get artifacts list");

            XmlReportParser parser = new XmlReportParser();
            ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
            String resolvedId = getResolveId();
            if (resolvedId == null) {
                resolvedId = ResolveOptions.getDefaultResolveId(getResolvedModuleId());
            }
            for (String conf : confs) {
                File reportFile = cacheMgr.getConfigurationResolveReportInCache(resolvedId,
                        conf);
                parser.parse(reportFile);

                all.addAll(Arrays.asList(parser.getArtifactReports()));
            }
        }
        return all;
    }
}
