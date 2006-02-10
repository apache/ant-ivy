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

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.filter.Filter;
import fr.jayasoft.ivy.filter.FilterHelper;
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

        XmlReportParser parser = new XmlReportParser();
        
        String[] confs = splitConfs(_conf);
        Collection all = new LinkedHashSet();
        for (int i = 0; i < confs.length; i++) {
            Artifact[] artifacts = parser.getArtifacts(new ModuleId(_organisation, _module), confs[i], _cache);
            all.addAll(Arrays.asList(artifacts));
        }
        List paths = new ArrayList();
        for (Iterator iter = all.iterator(); iter.hasNext();) {
            Artifact artifact = (Artifact)iter.next();
            if (_artifactFilter.accept(artifact)) {
                paths.add(ivy.getArchivePathInCache(artifact));
            }
        }
        
        return paths;
    }

}
