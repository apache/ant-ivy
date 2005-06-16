/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.text.ParseException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class IvyResolve extends IvyTask {
    private File _file = null;
    private String _conf = null;
    private File _cache = null;
    private String _revision = null;
    private String _pubdate = null;
    private boolean _haltOnFailure = true;
    
    public String getDate() {
        return _pubdate;
    }
    public void setDate(String pubdate) {
        _pubdate = pubdate;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getConf() {
        return _conf;
    }
    public void setConf(String conf) {
        _conf = conf;
    }
    public File getFile() {
        return _file;
    }
    public void setFile(File file) {
        _file = file;
    }
    public boolean isHaltonfailure() {
        return _haltOnFailure;
    }
    public void setHaltonfailure(boolean haltOnFailure) {
        _haltOnFailure = haltOnFailure;
    }
    public void setShowprogress(boolean show) {
        Message.setShowProgress(show);
    }
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        try {
            if (_file == null) {
                _file = new File(getProject().getBaseDir(), getProperty(ivy, "ivy.dep.file"));
            }
            _conf = getProperty(_conf, ivy, "ivy.configurations");
            _revision = getProperty(_revision, ivy, "ivy.revision");
            if (_cache == null) {
                _cache = ivy.getDefaultCache();
            }
            String[] confs = splitConfs(_conf);
            ResolveReport report = ivy.resolve(_file.toURL(), _revision, confs, _cache, getPubDate(_pubdate, null), doValidate(ivy));
            if (isHaltonfailure() && report.hasError()) {
                throw new BuildException("resolve failed - see output for details");
            }
            ModuleDescriptor md = report.getModuleDescriptor();
            setResolved(md);
            getProject().setProperty("ivy.organisation", md.getModuleRevisionId().getOrganisation());
            getProject().setProperty("ivy.module", md.getModuleRevisionId().getName());
            getProject().setProperty("ivy.revision", md.getResolvedModuleRevisionId().getRevision());
            if (_conf.trim().equals("*")) {
                getProject().setProperty("ivy.resolved.configurations", mergeConfs(md.getConfigurationsNames()));
            } else {
                getProject().setProperty("ivy.resolved.configurations", _conf);
            }
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: "+_file, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file", e);
        } catch (Exception e) {
            throw new BuildException("impossible to resolve dependencies: "+e.getMessage(), e);
        }
    }
    
}
