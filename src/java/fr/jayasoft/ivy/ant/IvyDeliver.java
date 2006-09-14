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
import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.PublishingDependencyRevisionResolver;
import fr.jayasoft.ivy.status.StatusManager;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.MessageImpl;

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

    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        _revision = getProperty(_revision, ivy, "ivy.revision");
        _pubRevision = getProperty(_pubRevision, ivy, "ivy.deliver.revision");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        _deliverpattern = getProperty(_deliverpattern, ivy,
                "ivy.deliver.ivy.pattern");
        _status = getProperty(_status, ivy, "ivy.status");
        if (_deliveryList == null) {
            String deliveryListPath = getProperty(ivy, "ivy.delivery.list.file");
            if (deliveryListPath == null) {
                _deliveryList = new File(System.getProperty("java.io.tmpdir")
                        + "/delivery.properties");
            } else {
                _deliveryList = getProject().resolveFile(
                        ivy.substitute(deliveryListPath));
            }
        }
        if (_organisation == null) {
            throw new BuildException(
                    "no organisation provided for ivy deliver task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException(
                    "no module name provided for ivy deliver task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_revision == null) {
            _revision = "working@"+Ivy.getLocalHostName();
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
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(
        		_organisation, _module, _revision);
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
            ivy.deliver(mrid, _pubRevision, _cache, _deliverpattern, _status,
                    pubdate, drResolver, doValidate(ivy), _replacedynamicrev);

        } catch (Exception e) {
            throw new BuildException("impossible to deliver " + mrid + ": " + e, e);
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
