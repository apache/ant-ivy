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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlReportParser;

/**
 * @author x.hanin
 *
 */
public class ConfigurationResolveReport {

    private ModuleDescriptor _md;
	private String _conf;
	private Date _date;
	private Map _dependencyReports = new HashMap();
	private Map _dependencies = new LinkedHashMap();
	private Ivy _ivy;
	private Map _modulesIdsMap = new LinkedHashMap();
	private List _modulesIds;
	private List _previousDeps;

	public ConfigurationResolveReport(Ivy ivy, ModuleDescriptor md, String conf, Date date, File cache) {
		_ivy = ivy;
    	_md = md;
    	_conf = conf;
    	_date = date;

		// parse previous deps from previous report file if any
        File previousReportFile = new File(cache, XmlReportOutputter.getReportFileName(md.getModuleRevisionId().getModuleId(), conf));
		if (previousReportFile.exists()) {
			try {
				_previousDeps = Arrays.asList(new XmlReportParser().getDependencyRevisionIds(md.getModuleRevisionId().getModuleId(), conf, cache));
			} catch (Exception e) {
				_previousDeps = null;
			}
        } else {
			_previousDeps = null;
        }
    }
	
	public boolean hasChanged() {
		if (_previousDeps == null) {
			return true;
		}
		return !new HashSet(_previousDeps).equals(getModuleRevisionIds());
	}

    /**
     * Returns all non evicted and non error dependency mrids
     * The returned set is ordered so that a dependency will always
     * be found before their own dependencies
     * @return all non evicted and non error dependency mrids
     */
    public Set getModuleRevisionIds() {
		Set mrids = new LinkedHashSet();
		for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
			IvyNode node = (IvyNode) iter.next();
            if (!node.isEvicted(getConfiguration()) && !node.hasProblem()) {
                mrids.add(node.getResolvedId());
            }
		}
		return mrids;
	}

	public void addDependency(IvyNode node) {
        _dependencies.put(node.getId(), node);
        _dependencies.put(node.getResolvedId(), node);
        _dependencyReports.put(node, Collections.EMPTY_LIST);        
    }

    public void addDependency(IvyNode node, DownloadReport report) {
        _dependencies.put(node.getId(), node);
        _dependencies.put(node.getResolvedId(), node);
        List adrs = new ArrayList();
        Artifact[] artifacts = node.getArtifacts(_conf);
        for (int i = 0; i < artifacts.length; i++) {
            ArtifactDownloadReport artifactReport = report.getArtifactReport(artifacts[i]);
            if (artifactReport != null) {
                adrs.add(artifactReport);
            } else {
                Message.debug("no report found for "+artifacts[i]);
            }
        }
        _dependencyReports.put(node, adrs);        
    }


	public String getConfiguration() {
		return _conf;
	}
	public Date getDate() {
		return _date;
	}
	public ModuleDescriptor getModuleDescriptor() {
		return _md;
	}
	public IvyNode[] getUnresolvedDependencies() {
        List unresolved = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.hasProblem()) {
                unresolved.add(node);
            }
        }
		return (IvyNode[])unresolved.toArray(new IvyNode[unresolved.size()]);
	}

    private Collection getDependencies() {
        return new LinkedHashSet(_dependencies.values());
    }
    public IvyNode[] getEvictedNodes() {
        List evicted = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.isEvicted(_conf)) {
                evicted.add(node);
            }
        }
        return (IvyNode[])evicted.toArray(new IvyNode[evicted.size()]);
    }
    public IvyNode[] getDownloadedNodes() {
        List downloaded = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.isDownloaded() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return (IvyNode[])downloaded.toArray(new IvyNode[downloaded.size()]);
    }
    public IvyNode[] getSearchedNodes() {
        List downloaded = new ArrayList();
        for (Iterator iter = getDependencies().iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.isSearched() && node.getRealNode() == node) {
                downloaded.add(node);
            }
        }
        return (IvyNode[])downloaded.toArray(new IvyNode[downloaded.size()]);
    }

	public ArtifactDownloadReport[] getDownloadReports(ModuleRevisionId mrid) {
		Collection col = (Collection)_dependencyReports.get(getDependency(mrid));
		if (col == null) {
			return new ArtifactDownloadReport[0];
		}
		return (ArtifactDownloadReport[]) col.toArray(new ArtifactDownloadReport[col.size()]);
	}

	public IvyNode getDependency(ModuleRevisionId mrid) {
		return (IvyNode) _dependencies.get(mrid);
	}

	/**
	 * gives all the modules ids concerned by this report, from the most dependent to the least one
	 * @return a list of ModuleId
	 */
	public List getModuleIds() {
		if (_modulesIds == null) {
			List sortedDependencies = _ivy.sortNodes(getDependencies());
            Collections.reverse(sortedDependencies);
			for (Iterator iter = sortedDependencies.iterator(); iter.hasNext();) {
                IvyNode dependency = (IvyNode) iter.next();
				ModuleId mid = dependency.getResolvedId().getModuleId();
				Collection deps = (Collection)_modulesIdsMap.get(mid);
				if (deps == null) {
					deps = new HashSet();
					_modulesIdsMap.put(mid, deps);
				}
				deps.add(dependency);
			}
			_modulesIds = new ArrayList(_modulesIdsMap.keySet());
		}
		return Collections.unmodifiableList(_modulesIds);
	}
	
	public Collection getNodes(ModuleId mid) {
		if (_modulesIds == null) {
			getModuleIds();
		}
		return (Collection)_modulesIdsMap.get(mid);
	}

	public Ivy getIvy() {
		return _ivy;
	}

    public int getArtifactsNumber() {
        int total = 0;
        for (Iterator iter = _dependencyReports.values().iterator(); iter.hasNext();) {
            Collection reports = (Collection)iter.next();
            total += reports==null?0:reports.size();
        }
        return total;
    }
    public ArtifactDownloadReport[] getDownloadedArtifactsReports() {
        List result = new ArrayList();
        for (Iterator iter = _dependencyReports.values().iterator(); iter.hasNext();) {
            Collection reports = (Collection)iter.next();
            for (Iterator iterator = reports.iterator(); iterator.hasNext();) {
                ArtifactDownloadReport adr = (ArtifactDownloadReport)iterator.next();
                if (adr.getDownloadStatus() == DownloadStatus.SUCCESSFUL) {
                    result.add(adr);
                }
            }
        }
        return (ArtifactDownloadReport[])result.toArray(new ArtifactDownloadReport[result.size()]);
    }
    public ArtifactDownloadReport[] getFailedArtifactsReports() {
        List result = new ArrayList();
        for (Iterator iter = _dependencyReports.values().iterator(); iter.hasNext();) {
            Collection reports = (Collection)iter.next();
            for (Iterator iterator = reports.iterator(); iterator.hasNext();) {
                ArtifactDownloadReport adr = (ArtifactDownloadReport)iterator.next();
                if (adr.getDownloadStatus() == DownloadStatus.FAILED) {
                    result.add(adr);
                }
            }
        }
        return (ArtifactDownloadReport[])result.toArray(new ArtifactDownloadReport[result.size()]);
    }
    public boolean hasError() {
        return getUnresolvedDependencies().length > 0 || getFailedArtifactsReports().length > 0;
    }

    public int getNodesNumber() {
        return getDependencies().size();
    }


}
