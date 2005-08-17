/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Input;
import org.apache.tools.ant.taskdefs.Property;

import fr.jayasoft.ivy.DefaultPublishingDRResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.PublishingDependencyRevisionResolver;
import fr.jayasoft.ivy.Status;

/**
 * @author Hanin
 *
 */
public class IvyDeliver extends IvyTask {
    private final class DeliverDRResolver implements PublishingDependencyRevisionResolver {
        public String resolve(ModuleDescriptor published, String publishedStatus, ModuleDescriptor dependency) {
            if (Status.isIntegration(publishedStatus)) {
                // published status is integration one, nothing to ask
                return dependency.getResolvedModuleRevisionId().getRevision();
            }
            
            // we are publishing a delivery (a non integration module)
            
            if (!Status.isIntegration(dependency.getStatus())) {
                // dependency is already a delivery, nothing to ask
                return dependency.getResolvedModuleRevisionId().getRevision();
            }

            // the dependency is not a delivery
            
            // we must ask the user what version and status he want to have
            // for the dependency
            String statusProperty = dependency.getModuleRevisionId().getName()+"."+dependency.getModuleRevisionId().getRevision()+".status";
            String versionProperty = dependency.getModuleRevisionId().getName()+"."+dependency.getModuleRevisionId().getRevision()+".version";
            String version = getProject().getProperty(versionProperty);
            String status = getProject().getProperty(statusProperty);
            log("found version = "+version+" status="+status);
            if (version != null && status != null) {
                // delivery has already been done : just return the value
                return version;
                
            }

            Input input = (Input)getProject().createTask("input");
            input.setOwningTarget(getOwningTarget());
            input.init();
            
            // ask status
            input.setMessage(dependency.getModuleRevisionId().getName()+" "+dependency.getModuleRevisionId().getRevision()+": please enter a status: ");
            input.setValidargs(Status.getDeliveryStatusListString());
            input.setAddproperty(statusProperty);
            input.perform();
            appendDeliveryList(statusProperty + " = "+getProject().getProperty(statusProperty));
            
            // ask version
            input.setMessage(dependency.getModuleRevisionId().getName()+" "+dependency.getModuleRevisionId().getRevision()+": please enter a version: ");
            input.setValidargs(null);
            input.setAddproperty(versionProperty);
            input.perform();
            appendDeliveryList(versionProperty + " = "+getProject().getProperty(versionProperty));
            
            version = getProject().getProperty(versionProperty);
            deliverDependency(dependency, version);
            
            loadDeliveryList();

            return version;
        }
        
        public void deliverDependency(ModuleDescriptor dependency, String version) {
            // call deliver target if any
            if (_deliverTarget != null) {
                String statusProperty = dependency.getModuleRevisionId().getName()+"."+dependency.getModuleRevisionId().getRevision()+".status";
                String status = getProject().getProperty(statusProperty);

                CallTarget ct = (CallTarget)getProject().createTask("antcall");
                ct.setOwningTarget(getOwningTarget());
                ct.init();
                ct.setTarget(_deliverTarget);
                ct.setInheritAll(true);
                ct.setInheritRefs(true);
                Property param = ct.createParam();
                param.setName("dependency.name");
                param.setValue(dependency.getModuleRevisionId().getName());
                param = ct.createParam();
                param.setName("dependency.published.status");
                param.setValue(status);
                param = ct.createParam();
                param.setName("dependency.published.version");
                param.setValue(version);
                param = ct.createParam();
                param.setName("dependency.version");
                param.setValue(dependency.getModuleRevisionId().getRevision());
                ct.perform();
            }        
        }

    }

    private String  _organisation;
    private String  _module;
    private String  _revision;
    private String  _pubRevision;
    private File 	_cache; 
    private String 	_deliverpattern;
    private String 	_status;
    private String 	_pubdate;
    private String  _deliverTarget;
    private File    _deliveryList;
    
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getDeliverpattern() {
        return _deliverpattern;
    }
    public void setDeliverpattern(String destivypattern) {
        _deliverpattern = destivypattern;
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
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        _revision = getProperty(_revision, ivy, "ivy.revision");
        _pubRevision = getProperty(_pubRevision, ivy, "ivy.deliver.revision");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _deliverpattern = getProperty(_deliverpattern, ivy, "ivy.deliver.ivy.pattern");
        _status = getProperty(_status, ivy, "ivy.status");
        if (_deliveryList == null) {
            String deliveryListPath = getProperty(ivy, "ivy.delivery.list.file");
            if (deliveryListPath == null) {
                _deliveryList = new File(System.getProperty("java.io.tmpdir")+"/delivery.properties");
            } else {
                _deliveryList = getProject().resolveFile(ivy.substitute(deliveryListPath));
            }
        }
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy deliver task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy deliver task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_revision == null) {
            throw new BuildException("no module revision provided for ivy deliver task: It can either be set explicitely via the attribute 'revision' or via 'ivy.revision' property or a prior call to <resolve/>");
        }
        Date pubdate = getPubDate(_pubdate, new Date());
        if (_pubRevision == null) {
            if (_revision.startsWith("working@")) {
                _pubRevision = Ivy.DATE_FORMAT.format(pubdate);
            } else {
                _pubRevision = _revision;
            }
        }
        if (_deliverpattern == null) {
            throw new BuildException("deliver ivy pattern is missing: either provide it as parameters or through ivy.deliver.ivy.pattern properties");
        }
        if (_status == null) {
            throw new BuildException("no status provided: either provide it as parameter or through the ivy.status.default property");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        boolean isLeading = false;
        try {
            if (!_deliveryList.exists()) {
                isLeading = true;
            }
            
            loadDeliveryList();
            
            PublishingDependencyRevisionResolver drResolver;
            if (_deliverTarget!=null) {
                drResolver = new DeliverDRResolver();
            } else {
                drResolver = new DefaultPublishingDRResolver();
            }
            ivy.deliver(mrid, _pubRevision, _cache, _deliverpattern, _status, pubdate, 
                    	drResolver, doValidate(ivy));
            
        } catch (Exception e) {
            throw new BuildException("impossible to deliver "+mrid+": "+e.getMessage(), e);
        } finally {
            if (isLeading) {
                if (_deliveryList.exists()) {
                    _deliveryList.delete();
                }
            }
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
        echo.setMessage(msg+"${line.separator}");
        echo.setAppend(true);
        echo.perform();
    }
    
    
    
    
}
