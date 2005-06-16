/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Property;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class IvyPublish extends IvyTask {
    private String  _organisation;
    private String  _module;
    private String  _revision;
    private String  _pubRevision;
    private File 	_cache; 
    private String 	_srcivypattern;
    private String 	_status;
    private String 	_pubdate;
    private String  _deliverTarget;
    private String  _publishResolverName = null;
    private String  _artifactspattern = null;
    private File    _deliveryList;
    private boolean _publishivy = true;
    private boolean _warnonmissing = true;
    private boolean _haltonmissing = true;
    
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getSrcivypattern() {
        return _srcivypattern;
    }
    public void setSrcivypattern(String destivypattern) {
        _srcivypattern = destivypattern;
    }
    /**
     * @deprecated use getSrcivypattern instead
     * @return
     */
    public String getDeliverivypattern() {
        return _srcivypattern;
    }
    /**
     * @deprecated use setSrcivypattern instead
     * @return
     */
    public void setDeliverivypattern(String destivypattern) {
        _srcivypattern = destivypattern;
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
    public String getPubdate() {
        return _pubdate;
    }
    public void setPubdate(String pubdate) {
        _pubdate = pubdate;
    }
    public String getPubrevision() {
        return _pubRevision;
    }
    public void setPubrevision(String pubRevision) {
        _pubRevision = pubRevision;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public String getStatus() {
        return _status;
    }
    public void setStatus(String status) {
        _status = status;
    }
    public void setDelivertarget(String deliverTarget) {
        _deliverTarget = deliverTarget;
    }
    public void setDeliveryList(File deliveryList) {
        _deliveryList = deliveryList;
    }
    public String getResolver() {
        return _publishResolverName;
    }    
    public void setResolver(String publishResolverName) {
        _publishResolverName = publishResolverName;
    }
    public String getArtifactspattern() {
        return _artifactspattern;
    }    
    public void setArtifactspattern(String artifactsPattern) {
        _artifactspattern = artifactsPattern;
    }
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        _revision = getProperty(_revision, ivy, "ivy.revision");
        _pubRevision = getProperty(_pubRevision, ivy, "ivy.deliver.revision");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _artifactspattern = getProperty(_artifactspattern, ivy, "ivy.publish.src.artifacts.pattern");
        if (_srcivypattern == null) {
            _srcivypattern = _artifactspattern;
        }
        _status = getProperty(_status, ivy, "ivy.status");
        if (_module == null || _organisation == null || _revision == null) {
            throw new BuildException("empty or incomplete module revision id provided for publish: either call resolve, give paramaters to publish, or provide ivy.module, ivy.organisation and ivy.revision properties");
        }
        if (_artifactspattern == null) {
            throw new BuildException("no artifacts pattern: either provide it through parameter or through ivy.publish.src.artifacts.pattern property");
        }
        if (_publishResolverName == null) {
            throw new BuildException("no publish deliver name: please provide it through parameter 'resolver'");
        }
        Date pubdate = getPubDate(_pubdate, new Date());
        if (_pubRevision == null) {
            if (_revision.startsWith("working@")) {
                _pubRevision = Ivy.DATE_FORMAT.format(pubdate);
            } else {
                _pubRevision = _revision;
            }
        }
        if (_status == null) {
            throw new BuildException("no status provided: either provide it as parameter or through the ivy.status.default property");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        try {
            File ivyFile = new File(_cache, IvyPatternHelper.substitute(_srcivypattern, _organisation, _module, _pubRevision, "ivy", "ivy", "xml"));
            if (_publishivy && !ivyFile.exists()) {
                IvyDeliver deliver = new IvyDeliver();
                deliver.setProject(getProject());
                deliver.setCache(getCache());
                deliver.setDeliverpattern(getSrcivypattern());
                deliver.setDelivertarget(_deliverTarget);
                deliver.setDeliveryList(_deliveryList);
                deliver.setModule(getModule());
                deliver.setOrganisation(getOrganisation());
                deliver.setPubdate(Ivy.DATE_FORMAT.format(pubdate));
                deliver.setPubrevision(getPubrevision());
                deliver.setRevision(getRevision());
                deliver.setStatus(getStatus());
                deliver.setValidate(doValidate(ivy));
                
                deliver.execute();
            }
            
            Collection missing = ivy.publish(mrid, _pubRevision, _cache, _artifactspattern, _publishResolverName, _publishivy?_srcivypattern:null, doValidate(ivy));
            if (_warnonmissing) {
                for (Iterator iter = missing.iterator(); iter.hasNext();) {
                    Artifact artifact = (Artifact)iter.next();
                    Message.warn("missing artifact: "+artifact);
                }
            }
            if (_haltonmissing && !missing.isEmpty()) {
                throw new BuildException("missing published artifacts for "+mrid+": "+missing);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException("impossible to publish artifacts for "+mrid+": "+e.getMessage(), e);
        }
    }
    
    private void loadDeliveryList() {
        Property property = (Property)getProject().createTask("property");
        property.setOwningTarget(getOwningTarget());
        property.init();
        property.setFile(_deliveryList);
        property.perform();
    }
    private void appendDeliveryList(String msg) {
        Echo echo = (Echo)getProject().createTask("echo");
        echo.setOwningTarget(getOwningTarget());
        echo.init();
        echo.setFile(_deliveryList);
        echo.setMessage(msg+"\n");
        echo.setAppend(true);
        echo.perform();
    }
    public boolean isPublishivy() {
        return _publishivy;
    }
    
    public void setPublishivy(boolean publishivy) {
        _publishivy = publishivy;
    }
    public boolean isWarnonmissing() {
        return _warnonmissing;
    }
    
    public void setWarnonmissing(boolean warnonmissing) {
        _warnonmissing = warnonmissing;
    }
    public boolean isHaltonmissing() {
        return _haltonmissing;
    }
    
    public void setHaltonmissing(boolean haltonmissing) {
        _haltonmissing = haltonmissing;
    }
}
