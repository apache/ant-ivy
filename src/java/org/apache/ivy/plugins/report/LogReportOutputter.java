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
package org.apache.ivy.plugins.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;

/**
 *
 */
public class LogReportOutputter implements ReportOutputter {

    public String getName() {
        return CONSOLE;
    }

    public void output(ResolveReport report, ResolutionCacheManager cacheMgr, ResolveOptions options)
            throws IOException {
        IvySettings settings = IvyContext.getContext().getSettings();

        if (settings.logModulesInUse() && ResolveOptions.LOG_DEFAULT.equals(options.getLog())) {
            Message.info("\t:: modules in use:");
            List dependencies = new ArrayList(report.getDependencies());
            Collections.sort(dependencies);
            if (dependencies.size() > 0) {
                String[] confs = report.getConfigurations();
                for (int i = 0; i < dependencies.size(); i++) {
                    IvyNode node = (IvyNode) dependencies.get(i);
                    if (node.isCompletelyEvicted() || node.hasProblem()) {
                        continue;
                    }
                    List nodeConfs = new ArrayList(confs.length);
                    for (int j = 0; j < confs.length; j++) {
                        String conf = confs[j];
                        if (report.getConfigurationReport(conf).getModuleRevisionIds()
                                .contains(node.getResolvedId())) {
                            nodeConfs.add(conf);
                        }
                    }
                    Message.info("\t" + node + " from "
                            + node.getModuleRevision().getResolver().getName() + " in " + nodeConfs);
                }
            }
        }

        IvyNode[] evicted = report.getEvictedNodes();

        if (evicted.length > 0 && ResolveOptions.LOG_DEFAULT.equals(options.getLog())) {
            Message.info("\t:: evicted modules:");
            for (int i = 0; i < evicted.length; i++) {
                Collection allEvictingNodes = evicted[i].getAllEvictingNodesDetails();
                if (allEvictingNodes == null) {
                    Message.info("\t" + evicted[i] + " transitively in "
                            + Arrays.asList(evicted[i].getEvictedConfs()));
                } else if (allEvictingNodes.isEmpty()) {
                    Message.info("\t" + evicted[i] + " by [] ("
                            + evicted[i].getAllEvictingConflictManagers() + ") in "
                            + Arrays.asList(evicted[i].getEvictedConfs()));
                } else {
                    Message.info("\t" + evicted[i] + " by " + allEvictingNodes + " in "
                            + Arrays.asList(evicted[i].getEvictedConfs()));
                }
                String[] confs = evicted[i].getEvictedConfs();
                for (int j = 0; j < confs.length; j++) {
                    EvictionData evictedData = evicted[i].getEvictedData(confs[j]);
                    if (evictedData.getParent() != null) {
                        Message.verbose("\t  in " + evictedData.getParent() + " with "
                                + evictedData.getConflictManager());
                    }
                }
            }
        }

        if (ResolveOptions.LOG_DEFAULT.equals(options.getLog())) {
            // CheckStyle:MagicNumber| OFF
            char[] sep = new char[69];
            Arrays.fill(sep, '-');
            Message.rawinfo("\t" + new String(sep));
            StringBuffer line = new StringBuffer("\t");
            append(line, "", 18);
            append(line, "modules", 31);
            line.append("|");
            append(line, "artifacts", 15);
            line.append("|");
            Message.rawinfo(line.toString());

            line = new StringBuffer("\t");
            append(line, "conf", 18);
            append(line, "number", 7);
            append(line, "search", 7);
            append(line, "dwnlded", 7);
            append(line, "evicted", 7);
            line.append("|");
            append(line, "number", 7);
            append(line, "dwnlded", 7);
            // CheckStyle:MagicNumber| ON
            line.append("|");
            Message.rawinfo(line.toString());
            Message.rawinfo("\t" + new String(sep));

            String[] confs = report.getConfigurations();
            for (int i = 0; i < confs.length; i++) {
                output(report.getConfigurationReport(confs[i]));
            }
            Message.rawinfo("\t" + new String(sep));
        }

        IvyNode[] unresolved = report.getUnresolvedDependencies();
        if (unresolved.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
            Message.warn("\t::          UNRESOLVED DEPENDENCIES         ::");
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
        }
        for (int i = 0; i < unresolved.length; i++) {
            Message.warn("\t:: " + unresolved[i] + ": " + unresolved[i].getProblemMessage());
        }
        if (unresolved.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::\n");
        }

        ArtifactDownloadReport[] errors = report.getFailedArtifactsReports();
        if (errors.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
            Message.warn("\t::              FAILED DOWNLOADS            ::");
            Message.warn("\t:: ^ see resolution messages for details  ^ ::");
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
        }
        for (int i = 0; i < errors.length; i++) {
            Message.warn("\t:: " + errors[i].getArtifact());
        }
        if (errors.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::\n");
        }
    }

    public void output(ConfigurationResolveReport report) {
        StringBuffer line = new StringBuffer("\t");
        // CheckStyle:MagicNumber| OFF
        append(line, report.getConfiguration(), 18);
        append(line, String.valueOf(report.getNodesNumber()), 7);
        append(line, String.valueOf(report.getSearchedNodes().length), 7);
        append(line, String.valueOf(report.getDownloadedNodes().length), 7);
        append(line, String.valueOf(report.getEvictedNodes().length), 7);
        line.append("|");
        append(line, String.valueOf(report.getArtifactsNumber()), 7);
        append(line, String.valueOf(report.getDownloadedArtifactsReports().length), 7);
        // CheckStyle:MagicNumber| ON
        line.append("|");

        Message.rawinfo(line.toString());
    }

    private void append(StringBuffer line, Object o, int limit) {
        String v = String.valueOf(o);
        if (v.length() >= limit) {
            v = v.substring(0, limit);
        } else {
            int missing = limit - v.length();
            int half = missing / 2;
            char[] c = new char[limit];
            Arrays.fill(c, ' ');
            System.arraycopy(v.toCharArray(), 0, c, missing - half, v.length());
            v = new String(c);
        }
        line.append("|");
        line.append(v);
    }

}
