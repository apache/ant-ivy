/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
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
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlReportParser;

//TODO: refactor this class and IvyCachePath to extract common behaviour
public abstract class IvyCacheTask extends IvyTask {
    private String _conf;

    private String _organisation;
    private String _module;
    private boolean _haltOnFailure = true;
    private File _cache;
    private String _type;
    
    private Filter _artifactFilter = null;
    
    public String getConf() {
        return _conf;
    }
    
    public void setConf(String conf) {
        _conf = conf;
    }
    
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }

    protected void prepareAndCheck() {
        Ivy ivy = getIvyInstance();
        
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");

        ensureResolved(isHaltonfailure(), getOrganisation(), getModule());
        
        _conf = getProperty(_conf, ivy, "ivy.resolved.configurations");
        if ("*".equals(_conf)) {
            _conf = getProperty(ivy, "ivy.resolved.configurations");
            if (_conf == null) {
                throw new BuildException("bad provided for ivy cache task: * can only be used with a prior call to <resolve/>");
            }
        }
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy cache task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy cache task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_conf == null) {
            throw new BuildException("no conf provided for ivy cache task: It can either be set explicitely via the attribute 'conf' or via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        _artifactFilter = FilterHelper.getArtifactTypeFilter(_type);
    }

    protected List getPaths() throws BuildException, ParseException, IOException {
        Ivy ivy = getIvyInstance();

        Collection artifacts = getAllArtifacts();
        List paths = new ArrayList();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact)iter.next();
            if (_artifactFilter.accept(artifact)) {
            	addPath(paths, artifact, ivy);
            }
        }
        
        return paths;
    }

    private Collection getAllArtifacts() throws ParseException, IOException {
        String[] confs = splitConfs(_conf);
        Collection all = new LinkedHashSet();

        ResolveReport report = (ResolveReport) getProject().getReference("ivy.resolved.report");
        
        if (report != null) {
            Message.debug("using internal report instance to get artifacts list");
            for (int i = 0; i < confs.length; i++) {
                Set revisions = report.getConfigurationReport(confs[i]).getModuleRevisionIds();
                for (Iterator it = revisions.iterator(); it.hasNext(); ) {
                	ModuleRevisionId revId = (ModuleRevisionId) it.next();
                	ArtifactDownloadReport[] aReps = report.getConfigurationReport(confs[i]).getDownloadReports(revId);
                	for (int j = 0; j < aReps.length; j++) {
                		all.add(aReps[j].getArtifact());
                	}
                }
            }
        } else {
            Message.debug("using stored report to get artifacts list");
            XmlReportParser parser = new XmlReportParser();
            
            for (int i = 0; i < confs.length; i++) {
                Artifact[] artifacts = parser.getArtifacts(new ModuleId(_organisation, _module), confs[i], _cache);
                all.addAll(Arrays.asList(artifacts));
            }
        }
        return all;
    }
    
    protected void addPath(List paths, Artifact artifact, Ivy ivy) throws IOException {
    	paths.add(new PathEntry(ivy.getArchivePathInCache(artifact), true));
    }
    
    public static class PathEntry {
    	private String location;
    	private boolean relativeToCache;
    	
    	public PathEntry(String location, boolean relativeToCache) {
    		this.location = location;
    		this.relativeToCache = relativeToCache;
    	}

		public String getLocation() {
			return location;
		}

		public boolean isRelativeToCache() {
			return relativeToCache;
		}
    }

}
