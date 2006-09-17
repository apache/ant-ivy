/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.ConfigurationResolveReport;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlReportParser;

/**
 * Base class for the cache path related classes: cachepath and cachefileset.
 * 
 * Most of the behviour is common to the two, since only the produced element differs.
 * 
 * @author Xavier Hanin
 */
public abstract class IvyCacheTask extends IvyPostResolveTask {

    protected List getArtifacts() throws BuildException, ParseException, IOException {
        Collection artifacts = getAllArtifacts();
        List ret = new ArrayList();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact)iter.next();
            if (getArtifactFilter().accept(artifact)) {
            	ret.add(artifact);
            }
        }
        
        return ret;
    }

    private Collection getAllArtifacts() throws ParseException, IOException {
        String[] confs = splitConfs(getConf());
        Collection all = new LinkedHashSet();

        ResolveReport report = getResolvedReport();
        if (report != null) {
            Message.debug("using internal report instance to get artifacts list");
            for (int i = 0; i < confs.length; i++) {
                ConfigurationResolveReport configurationReport = report.getConfigurationReport(confs[i]);
                if (configurationReport == null) {
                	throw new BuildException("bad confs provided: "+confs[i]+" not found among "+Arrays.asList(report.getConfigurations()));
                }
				Set revisions = configurationReport.getModuleRevisionIds();
                for (Iterator it = revisions.iterator(); it.hasNext(); ) {
                	ModuleRevisionId revId = (ModuleRevisionId) it.next();
                	ArtifactDownloadReport[] aReps = configurationReport.getDownloadReports(revId);
                	for (int j = 0; j < aReps.length; j++) {
                		all.add(aReps[j].getArtifact());
                	}
                }
            }
        } else {
            Message.debug("using stored report to get artifacts list");
            XmlReportParser parser = new XmlReportParser();
            
            for (int i = 0; i < confs.length; i++) {
                Artifact[] artifacts = parser.getArtifacts(getResolvedModuleId(), confs[i], getCache());
                all.addAll(Arrays.asList(artifacts));
            }
        }
        return all;
    }
}
