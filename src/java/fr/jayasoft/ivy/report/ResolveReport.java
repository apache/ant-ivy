/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.filter.Filter;

/**
 * Represents a whole resolution report for a module
 */
public class ResolveReport {
    private ModuleDescriptor _md;
    private Map _confReports = new LinkedHashMap();
	private List _problemMessages;
	private List _dependencies; // the list of all dependencies resolved, ordered from the more dependent to the less dependent
	private List _artifacts;
	private long _resolveTime;
	private long _downloadTime;
	
    public ResolveReport(ModuleDescriptor md) {
        _md = md;
    }
    public void addReport(String conf, ConfigurationResolveReport report) {
        _confReports.put(conf, report);
    }
    public ConfigurationResolveReport getConfigurationReport(String conf) {
        return (ConfigurationResolveReport)_confReports.get(conf);
    }
    public String[] getConfigurations() {
        return (String[])_confReports.keySet().toArray(new String[_confReports.size()]);
    }
    public boolean hasError() {
        boolean hasError = false;
        for (Iterator it = _confReports.values().iterator(); it.hasNext() && !hasError;) {
            ConfigurationResolveReport report = (ConfigurationResolveReport)it.next();
            hasError |= report.hasError();
        }
        return hasError;
    }

    public void output(ReportOutputter[] outputters, File cache) {
        for (int i = 0; i < outputters.length; i++) {
            outputters[i].output(this, cache);
        }
    }
    
    public ModuleDescriptor getModuleDescriptor() {
        return _md;
    }
    
    public IvyNode[] getEvictedNodes() {
        Collection all = new HashSet();
        for (Iterator iter = _confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport)iter.next();
            all.addAll(Arrays.asList(report.getEvictedNodes()));
        }
        return (IvyNode[])all.toArray(new IvyNode[all.size()]);
    }
    public IvyNode[] getUnresolvedDependencies() {
        Collection all = new HashSet();
        for (Iterator iter = _confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport)iter.next();
            all.addAll(Arrays.asList(report.getUnresolvedDependencies()));
        }
        return (IvyNode[])all.toArray(new IvyNode[all.size()]);
    }
    public ArtifactDownloadReport[] getFailedArtifactsReports() {
        Collection all = new HashSet();
        for (Iterator iter = _confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport)iter.next();
            all.addAll(Arrays.asList(report.getFailedArtifactsReports()));
        }
        return (ArtifactDownloadReport[])all.toArray(new ArtifactDownloadReport[all.size()]);
    }
	public boolean hasChanged() {
        for (Iterator iter = _confReports.values().iterator(); iter.hasNext();) {
            ConfigurationResolveReport report = (ConfigurationResolveReport)iter.next();
			if (report.hasChanged()) {
				return true;
			}
        }
		return false;
	}
	public void setProblemMessages(List problems) {
		_problemMessages = problems;
	}
	public List getProblemMessages() {
		return _problemMessages;
	}
	public List getAllProblemMessages() {
		List ret = new ArrayList(_problemMessages);
		for (Iterator iter = _confReports.values().iterator(); iter.hasNext();) {
			ConfigurationResolveReport r = (ConfigurationResolveReport) iter.next();
			IvyNode[] unresolved = r.getUnresolvedDependencies();
			for (int i = 0; i < unresolved.length; i++) {
				Exception e = unresolved[i].getProblem();
				if (e != null) {
					String errMsg = e instanceof RuntimeException?e.getMessage():e.toString();
					if (errMsg == null || errMsg.length()==0 || "null".equals(errMsg)) {
						errMsg = e.getClass().getName() + " at "+e.getStackTrace()[0].toString();
					}
					ret.add("unresolved dependency: "+unresolved[i].getId()+": "+errMsg);
				} else {
					ret.add("unresolved dependency: "+unresolved[i].getId());
				}
			}
			ArtifactDownloadReport[] adrs = r.getFailedArtifactsReports();
			for (int i = 0; i < adrs.length; i++) {
				ret.add("download failed: "+adrs[i].getArtifact());
			}
		}
		return ret;
	}
	public void setDependencies(List dependencies, Filter artifactFilter) {
		_dependencies = dependencies;
        // collect list of artifacts
        _artifacts = new ArrayList();
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
			IvyNode dependency = (IvyNode) iter.next();
			if (!dependency.isCompletelyEvicted() && !dependency.hasProblem()) {
				_artifacts.addAll(Arrays.asList(dependency.getSelectedArtifacts(artifactFilter)));
			} 
			// update the configurations reports with the dependencies
			// these reports will be completed later with download information, if any
			String[] dconfs = dependency.getRootModuleConfigurations();
			for (int j = 0; j < dconfs.length; j++) {
				ConfigurationResolveReport configurationReport = getConfigurationReport(dconfs[j]);
				if (configurationReport != null) {
					configurationReport.addDependency(dependency);
				}
			}
		}
	}
	/**
	 * Returns the list of all dependencies concerned by this report as a List of IvyNode
	 * ordered from the more dependent to the least one
	 * @return
	 */
	public List getDependencies() {
		return _dependencies;
	}
	/**
	 * Returns the list of all artifacts which should be downloaded per this resolve
	 * To know if the artifact have actually been downloaded use information found
	 * in ConfigurationResolveReport.
	 * @return
	 */
	public List getArtifacts() {
		return _artifacts;
	}
	/**
	 * gives all the modules ids concerned by this report, from the most dependent to the least one
	 * @return a list of ModuleId
	 */
	public List getModuleIds() {
		List ret = new ArrayList();
		List sortedDependencies = new ArrayList(_dependencies);
		for (Iterator iter = sortedDependencies.iterator(); iter.hasNext();) {
			IvyNode dependency = (IvyNode) iter.next();
			ModuleId mid = dependency.getResolvedId().getModuleId();
			if (!ret.contains(mid)) {
				ret.add(mid);
			}
		}
		return ret;
	}
	public void setResolveTime(long elapsedTime) {
		_resolveTime = elapsedTime;
	}
	public long getResolveTime() {
		return _resolveTime;
	}
	public void setDownloadTime(long elapsedTime) {
		_downloadTime = elapsedTime;
	}
	public long getDownloadTime() {
		return _downloadTime;
	}
}
