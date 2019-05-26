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
            List<IvyNode> dependencies = new ArrayList<>(report.getDependencies());
            Collections.sort(dependencies);
            if (dependencies.size() > 0) {
                String[] confs = report.getConfigurations();
                for (IvyNode node : dependencies) {
                    if (node.isCompletelyEvicted() || node.hasProblem()) {
                        continue;
                    }
                    List<String> nodeConfs = new ArrayList<>(confs.length);
                    for (String conf : confs) {
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
            for (IvyNode evictedNode : evicted) {
                Collection<String> allEvictingNodes = evictedNode.getAllEvictingNodesDetails();
                if (allEvictingNodes == null) {
                    Message.info("\t" + evictedNode + " transitively in "
                            + Arrays.asList(evictedNode.getEvictedConfs()));
                } else if (allEvictingNodes.isEmpty()) {
                    Message.info(
                        "\t" + evictedNode + " by [] (" + evictedNode.getAllEvictingConflictManagers()
                                + ") in " + Arrays.asList(evictedNode.getEvictedConfs()));
                } else {
                    Message.info("\t" + evictedNode + " by " + allEvictingNodes + " in "
                            + Arrays.asList(evictedNode.getEvictedConfs()));
                }
                for (String conf : evictedNode.getEvictedConfs()) {
                    EvictionData evictedData = evictedNode.getEvictedData(conf);
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
            StringBuilder line = new StringBuilder("\t");
            append(line, "", 18);
            append(line, "modules", 31);
            line.append("|");
            append(line, "artifacts", 15);
            line.append("|");
            Message.rawinfo(line.toString());

            line = new StringBuilder("\t");
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

            for (String conf : report.getConfigurations()) {
                output(report.getConfigurationReport(conf));
            }
            Message.rawinfo("\t" + new String(sep));
        }

        IvyNode[] unresolved = report.getUnresolvedDependencies();
        if (unresolved.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
            Message.warn("\t::          UNRESOLVED DEPENDENCIES         ::");
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
        }
        for (IvyNode anUnresolved : unresolved) {
            Message.warn("\t:: " + anUnresolved + ": " + anUnresolved.getProblemMessage());
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
        for (ArtifactDownloadReport error : errors) {
            Message.warn("\t:: " + error.getArtifact());
        }
        if (errors.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::\n");
        }
    }

    public void output(ConfigurationResolveReport report) {
        StringBuilder line = new StringBuilder("\t");
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

    private void append(StringBuilder line, Object o, int limit) {
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
