/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class LogReportOutputter implements ReportOutputter {
   
   public String getName() {
       return CONSOLE;
   }

    public void output(ResolveReport report, File destDir) {
        IvyNode[] evicted = report.getEvictedNodes();
        if (evicted.length > 0) {
            Message.info("\t:: evicted modules:");
            for (int i = 0; i < evicted.length; i++) {
                Collection allEvictingNodes = evicted[i].getAllEvictingNodes();
                if (allEvictingNodes == null) {
                    Message.info("\t"+evicted[i]+" transitively in "+Arrays.asList(evicted[i].getEvictedConfs()));
                } else if (allEvictingNodes.isEmpty()) {
                    Message.info("\t"+evicted[i]+" by [] ("+evicted[i].getAllEvictingConflictManagers()+") in "+Arrays.asList(evicted[i].getEvictedConfs()));
                } else {
                    Message.info("\t"+evicted[i]+" by "+allEvictingNodes+" in "+Arrays.asList(evicted[i].getEvictedConfs()));
                }
                String[] confs = evicted[i].getEvictedConfs();
                for (int j = 0; j < confs.length; j++) {
                    IvyNode.EvictionData evictedData = evicted[i].getEvictedData(confs[j]);
                    Message.verbose("\t  in "+evictedData.getNode()+" with "+evictedData.getConflictManager());
                }
            }
        }

        char[] sep = new char[69];
        Arrays.fill(sep, '-');
        Message.rawinfo("\t"+new String(sep));
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
        line.append("|");
        Message.rawinfo(line.toString());
        Message.rawinfo("\t"+new String(sep));
        
        String[] confs = report.getConfigurations();
        for (int i = 0; i < confs.length; i++) {
            output(report.getConfigurationReport(confs[i]));
        }
        Message.rawinfo("\t"+new String(sep));

        IvyNode[] unresolved = report.getUnresolvedDependencies();
        if (unresolved.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
            Message.warn("\t::          UNRESOLVED DEPENDENCIES         ::");
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::");
        }
        for (int i = 0; i < unresolved.length; i++) {
            Message.warn("\t:: "+unresolved[i]+": "+unresolved[i].getProblem().getMessage());
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
            Message.warn("\t:: "+errors[i].getArtifact());
        }
        if (errors.length > 0) {
            Message.warn("\t::::::::::::::::::::::::::::::::::::::::::::::\n");
        }
    }
    
    public void output(ConfigurationResolveReport report) {
        StringBuffer line = new StringBuffer("\t");
        append(line, report.getConfiguration(), 18);
        append(line, String.valueOf(report.getNodesNumber()), 7);
        append(line, String.valueOf(report.getSearchedNodes().length), 7);
        append(line, String.valueOf(report.getDownloadedNodes().length), 7);
        append(line, String.valueOf(report.getEvictedNodes().length), 7);
        line.append("|");
        append(line, String.valueOf(report.getArtifactsNumber()), 7);
        append(line, String.valueOf(report.getDownloadedArtifactsReports().length), 7);
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
