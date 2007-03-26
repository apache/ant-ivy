/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;
import java.util.Date;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.deliver.DefaultPublishingDRResolver;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.deliver.PublishingDependencyRevisionResolver;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageImpl;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Input;
import org.apache.tools.ant.taskdefs.Property;


/**
 * Trigger the delivery of a module, which may consist in a recursive delivery of dependencies
 * and on the replacement in the ivy file of dynamic revisions (like latest.integration) by static ones.
 * 
 * @author Xavier Hanin
 * 
 */
public class IvyDeliver extends IvyTask {
    private final class DeliverDRResolver extends DefaultPublishingDRResolver {
        public String resolve(ModuleDescriptor published,
                String publishedStatus, ModuleRevisionId depMrid,
                String depStatus) {
            if (StatusManager.getCurrent().isIntegration(publishedStatus)) {
                // published status is integration one, nothing to ask
                return super.resolve(published, publishedStatus, depMrid,
                        depStatus);
            }

            // we are publishing a delivery (a non integration module)

            if (!StatusManager.getCurrent().isIntegration(depStatus)) {
                // dependency is already a delivery, nothing to ask
                return super.resolve(published, publishedStatus, depMrid,
                        depStatus);
            }
            
            // the dependency is not a delivery

            String statusProperty = depMrid.getName() + "."  + depMrid.getRevision() + ".status";
            String versionProperty = depMrid.getName() + "."  + depMrid.getRevision() + ".version";
            String deliveredProperty = depMrid.getName() + "." + depMrid.getRevision() + ".delivered";
            
            String version = getProject().getProperty(versionProperty);
            String status = getProject().getProperty(statusProperty);
            String delivered = getProject().getProperty(deliveredProperty);
            Message.debug("found version = " + version + " status=" + status+" delivered="+delivered);
            if (version != null && status != null) {
                if ("true".equals(delivered)) {
                    // delivery has already been done : just return the value
                    return version;
                } else {
                    deliverDependency(depMrid, version, status, depStatus);
                    loadDeliveryList();
                    return version;
                }
            }

            /**
             * By setting these properties: recursive.delivery.status and
             * recursive.delivery.version, then if the specific status/version
             * is not found, then we will use the status/version set in these
             * global properties. This will apply to all artifacts in the
             * system.
             * 
             * This patch is meant to be used for recursive deliveries so that
             * all deliveries will use the global status/version unless a more
             * specific one is set.
             */
            String globalStatusProperty = "recursive.delivery.status";
            String globalVersionProperty = "recursive.delivery.version";
            version = getProject().getProperty(globalVersionProperty);
            status = getProject().getProperty(globalStatusProperty);
            if (version != null && status != null) {
                // found global delivery properties
                delivered = getProject().getProperty("recursive."+depMrid.getName()+ ".delivered");
                Message.debug("found global version = " + version + " and global status=" + status+" - delivered = "+delivered);
                if ("true".equals(delivered)) {
                    // delivery has already been done : just return the value
                    return version;
                } else {
                    getProject().setProperty(statusProperty, status); 
                    deliverDependency(depMrid, version, status, depStatus);
                    loadDeliveryList();
                    return version;
                }
            }

            // we must ask the user what version and status he want to have
            // for the dependency
            Input input = (Input) getProject().createTask("input");
            input.setOwningTarget(getOwningTarget());
            input.init();

            // ask status
            input.setMessage(depMrid.getName() + " " + depMrid.getRevision()
                    + ": please enter a status: ");
            input.setValidargs(StatusManager.getCurrent().getDeliveryStatusListString());
            input.setAddproperty(statusProperty);
            input.perform();
            status = getProject().getProperty(statusProperty);
            appendDeliveryList(statusProperty + " = " + status);

            // ask version
            input.setMessage(depMrid.getName() + " " + depMrid.getRevision()
                    + ": please enter a version: ");
            input.setValidargs(null);
            input.setAddproperty(versionProperty);
            input.perform();

            version = getProject().getProperty(versionProperty);
            appendDeliveryList(versionProperty + " = " + version);
            deliverDependency(depMrid, version, status, depStatus);

            loadDeliveryList();

            return version;
        }

        public void deliverDependency(ModuleRevisionId depMrid, String version, String status, String depStatus) {
            // call deliver target if any
            if (_deliverTarget != null && _deliverTarget.trim().length() > 0) {

                CallTarget ct = (CallTarget) getProject().createTask("antcall");
                ct.setOwningTarget(getOwningTarget());
                ct.init();
                ct.setTarget(_deliverTarget);
                ct.setInheritAll(true);
                ct.setInheritRefs(true);
                Property param = ct.createParam();
                param.setName("dependency.name");
                param.setValue(depMrid.getName());
                param = ct.createParam();
                param.setName("dependency.published.status");
                param.setValue(status);
                param = ct.createParam();
                param.setName("dependency.published.version");
                param.setValue(version);
                param = ct.createParam();
                param.setName("dependency.version");
                param.setValue(depMrid.getRevision());
                param = ct.createParam();
                param.setName("dependency.status");
                param.setValue(depStatus==null?"null":depStatus);

                MessageImpl impl = IvyContext.getContext().getMessageImpl();
                try {
                	IvyContext.getContext().setMessageImpl(null);
                	ct.perform();
                } finally {
                	IvyContext.getContext().setMessageImpl(impl);
                }
                
                String deliveredProperty = depMrid.getName() + "." + depMrid.getRevision() + ".delivered";
                getProject().setProperty(deliveredProperty, "true");
                appendDeliveryList(deliveredProperty + " = true");

                getProject().setProperty("recursive."+depMrid.getName() + ".delivered", "true");
                appendDeliveryList("recursive."+depMrid.getName() + ".delivered" + " = true");
            }
        }

    }

    private String _organisation;

    private String _module;

    private String _revision;

    private String _pubRevision;

    private File _cache;

    private String _deliverpattern;

    private String _status;

    private String _pubdate;

    private String _deliverTarget;

    private File _deliveryList;

    private boolean _replacedynamicrev = true;
    
    private String _resolveId;

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

    public boolean isReplacedynamicrev() {
        return _replacedynamicrev;
    }

    public void setReplacedynamicrev(boolean replacedynamicrev) {
        _replacedynamicrev = replacedynamicrev;
    }
    
    public String getResolveId() {
    	return _resolveId;
    }
    
    public void setResolveId(String resolveId) {
    	_resolveId = resolveId;
    }

    public void execute() throws BuildException {
    	Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        
        _organisation = getProperty(_organisation, settings, "ivy.organisation", _resolveId);
        _module = getProperty(_module, settings, "ivy.module", _resolveId);
        _revision = getProperty(_revision, settings, "ivy.revision", _resolveId);
        _pubRevision = getProperty(_pubRevision, settings, "ivy.deliver.revision");
        if (_cache == null) {
            _cache = settings.getDefaultCache();
        }
        _deliverpattern = getProperty(_deliverpattern, settings,
                "ivy.deliver.ivy.pattern");
        _status = getProperty(_status, settings, "ivy.status");
        if (_deliveryList == null) {
            String deliveryListPath = getProperty(settings, "ivy.delivery.list.file");
            if (deliveryListPath == null) {
                _deliveryList = new File(System.getProperty("java.io.tmpdir")
                        + "/delivery.properties");
            } else {
                _deliveryList = getProject().resolveFile(
                        settings.substitute(deliveryListPath));
            }
        }
        if (_resolveId == null) {
	        if (_organisation == null) {
	            throw new BuildException(
	                    "no organisation provided for ivy deliver task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
	        }
	        if (_module == null) {
	            throw new BuildException(
	                    "no module name provided for ivy deliver task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
	        }
        }
        if (_revision == null) {
            _revision = Ivy.getWorkingRevision();
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
            throw new BuildException(
                    "deliver ivy pattern is missing: either provide it as parameters or through ivy.deliver.ivy.pattern properties");
        }
        if (_status == null) {
            throw new BuildException(
                    "no status provided: either provide it as parameter or through the ivy.status.default property");
        }
        
        ModuleRevisionId mrid = null;
        if (_resolveId == null) {
        	mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        }
        boolean isLeading = false;
        try {
            if (!_deliveryList.exists()) {
                isLeading = true;
            }

            loadDeliveryList();

            PublishingDependencyRevisionResolver drResolver;
            if (_deliverTarget != null && _deliverTarget.trim().length() > 0) {
                drResolver = new DeliverDRResolver();
            } else {
                drResolver = new DefaultPublishingDRResolver();
            }
            
            DeliverOptions options = new DeliverOptions(_status, pubdate, 
    				CacheManager.getInstance(settings, _cache), 
    				drResolver, doValidate(settings), _replacedynamicrev).setResolveId(_resolveId);
            if (mrid == null) {
            	ivy.deliver(_pubRevision, _deliverpattern, options);
            } else {
            	ivy.deliver(mrid, _pubRevision, _deliverpattern, options);
            }
        } catch (Exception e) {
            throw new BuildException("impossible to deliver " + mrid == null ? _resolveId : mrid + ": " + e, e);
        } finally {
            if (isLeading) {
                if (_deliveryList.exists()) {
                    _deliveryList.delete();
                }
            }
        }
    }

    private void loadDeliveryList() {
        Property property = (Property) getProject().createTask("property");
        property.setOwningTarget(getOwningTarget());
        property.init();
        property.setFile(_deliveryList);
        property.perform();
    }

    private void appendDeliveryList(String msg) {
        Echo echo = (Echo) getProject().createTask("echo");
        echo.setOwningTarget(getOwningTarget());
        echo.init();
        echo.setFile(_deliveryList);
        echo.setMessage(msg + "\n");
        echo.setAppend(true);
        echo.perform();
    }

}
