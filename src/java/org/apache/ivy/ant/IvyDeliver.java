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
import org.apache.ivy.core.deliver.DefaultPublishingDRResolver;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.deliver.PublishingDependencyRevisionResolver;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.status.StatusManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Input;
import org.apache.tools.ant.taskdefs.Property;

/**
 * Trigger the delivery of a module, which may consist in a recursive delivery of dependencies and
 * on the replacement in the ivy file of dynamic revisions (like latest.integration) by static ones.
 */
public class IvyDeliver extends IvyTask {
    private final class DeliverDRResolver extends DefaultPublishingDRResolver {
        public String resolve(ModuleDescriptor published, String publishedStatus,
                ModuleRevisionId depMrid, String depStatus) {
            if (StatusManager.getCurrent().isIntegration(publishedStatus)) {
                // published status is integration one, nothing to ask
                return super.resolve(published, publishedStatus, depMrid, depStatus);
            }

            // we are publishing a delivery (a non integration module)

            if (!StatusManager.getCurrent().isIntegration(depStatus)) {
                // dependency is already a delivery, nothing to ask
                return super.resolve(published, publishedStatus, depMrid, depStatus);
            }

            // the dependency is not a delivery

            String statusProperty = depMrid.getName() + "." + depMrid.getRevision() + ".status";
            String versionProperty = depMrid.getName() + "." + depMrid.getRevision() + ".version";
            String deliveredProperty = depMrid.getName() + "." + depMrid.getRevision()
                    + ".delivered";

            String version = getProject().getProperty(versionProperty);
            String status = getProject().getProperty(statusProperty);
            String delivered = getProject().getProperty(deliveredProperty);
            Message.debug("found version = " + version + " status=" + status + " delivered="
                    + delivered);
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
             * recursive.delivery.version, then if the specific status/version is not found, then we
             * will use the status/version set in these global properties. This will apply to all
             * artifacts in the system. This patch is meant to be used for recursive deliveries so
             * that all deliveries will use the global status/version unless a more specific one is
             * set.
             */
            String globalStatusProperty = "recursive.delivery.status";
            String globalVersionProperty = "recursive.delivery.version";
            version = getProject().getProperty(globalVersionProperty);
            status = getProject().getProperty(globalStatusProperty);
            if (version != null && status != null) {
                // found global delivery properties
                delivered = getProject().getProperty(
                    "recursive." + depMrid.getName() + ".delivered");
                Message.debug("found global version = " + version + " and global status=" + status
                        + " - delivered = " + delivered);
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

        public void deliverDependency(ModuleRevisionId depMrid, String version, String status,
                String depStatus) {
            // call deliver target if any
            if (deliverTarget != null && deliverTarget.trim().length() > 0) {

                CallTarget ct = (CallTarget) getProject().createTask("antcall");
                ct.setOwningTarget(getOwningTarget());
                ct.init();
                ct.setTarget(deliverTarget);
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
                param.setValue(depStatus == null ? "null" : depStatus);

                ct.perform();

                String deliveredProperty = depMrid.getName() + "." + depMrid.getRevision()
                        + ".delivered";
                getProject().setProperty(deliveredProperty, "true");
                appendDeliveryList(deliveredProperty + " = true");

                getProject().setProperty("recursive." + depMrid.getName() + ".delivered", "true");
                appendDeliveryList("recursive." + depMrid.getName() + ".delivered" + " = true");
            }
        }

    }

    private String organisation;

    private String module;

    private String revision;

    private String pubRevision;

    private String deliverpattern;

    private String status;

    private String pubdate;

    private String deliverTarget;

    private File deliveryList;

    private boolean replacedynamicrev = true;

    private boolean replaceForcedRev = false;

    private String resolveId;

    private String conf;

    private String pubBranch;

    private boolean generateRevConstraint = true;

    private boolean merge = true;

    public void setCache(File cache) {
        cacheAttributeNotSupported();
    }

    public String getDeliverpattern() {
        return deliverpattern;
    }

    public void setDeliverpattern(String destivypattern) {
        this.deliverpattern = destivypattern;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getPubdate() {
        return pubdate;
    }

    public void setPubdate(String pubdate) {
        this.pubdate = pubdate;
    }

    public String getPubrevision() {
        return pubRevision;
    }

    public void setPubrevision(String pubRevision) {
        this.pubRevision = pubRevision;
    }

    public String getPubbranch() {
        return pubBranch;
    }

    public void setPubbranch(String pubBranch) {
        this.pubBranch = pubBranch;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDelivertarget(String deliverTarget) {
        this.deliverTarget = deliverTarget;
    }

    public void setDeliveryList(File deliveryList) {
        this.deliveryList = deliveryList;
    }

    public boolean isReplacedynamicrev() {
        return replacedynamicrev;
    }

    public void setReplacedynamicrev(boolean replacedynamicrev) {
        this.replacedynamicrev = replacedynamicrev;
    }

    public boolean isReplaceForcedRev() {
        return replaceForcedRev;
    }

    public void setReplaceForcedRev(boolean replaceForcedRev) {
        this.replaceForcedRev = replaceForcedRev;
    }

    public String getResolveId() {
        return resolveId;
    }

    public void setResolveId(String resolveId) {
        this.resolveId = resolveId;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String confs) {
        conf = confs;
    }

    public boolean isGenerateRevConstraint() {
        return generateRevConstraint;
    }

    public void setGenerateRevConstraint(boolean generateRevConstraint) {
        this.generateRevConstraint = generateRevConstraint;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        organisation = getProperty(organisation, settings, "ivy.organisation", resolveId);
        module = getProperty(module, settings, "ivy.module", resolveId);
        revision = getProperty(revision, settings, "ivy.revision", resolveId);
        pubBranch = getProperty(pubBranch, settings, "ivy.deliver.branch");
        pubRevision = getProperty(pubRevision, settings, "ivy.deliver.revision");
        deliverpattern = getProperty(deliverpattern, settings, "ivy.deliver.ivy.pattern");
        status = getProperty(status, settings, "ivy.status");
        if (deliveryList == null) {
            String deliveryListPath = getProperty(settings, "ivy.delivery.list.file");
            if (deliveryListPath == null) {
                deliveryList = new File(System.getProperty("java.io.tmpdir")
                        + "/delivery.properties");
            } else {
                deliveryList = getProject().resolveFile(settings.substitute(deliveryListPath));
            }
        }
        if (resolveId == null) {
            if (organisation == null) {
                throw new BuildException("no organisation provided for ivy deliver task: "
                        + "It can either be set explicitely via the attribute 'organisation' "
                        + "or via 'ivy.organisation' property or a prior call to <resolve/>");
            }
            if (module == null) {
                throw new BuildException("no module name provided for ivy deliver task: "
                        + "It can either be set explicitely via the attribute 'module' "
                        + "or via 'ivy.module' property or a prior call to <resolve/>");
            }
        }
        if (revision == null) {
            revision = Ivy.getWorkingRevision();
        }
        Date pubdate = getPubDate(this.pubdate, new Date());
        if (pubRevision == null) {
            if (revision.startsWith("working@")) {
                pubRevision = DateUtil.format(pubdate);
            } else {
                pubRevision = revision;
            }
        }
        if (deliverpattern == null) {
            throw new BuildException(
                    "deliver ivy pattern is missing: either provide it as parameters "
                            + "or through ivy.deliver.ivy.pattern properties");
        }
        if (status == null) {
            throw new BuildException(
                    "no status provided: either provide it as parameter or through "
                            + "the ivy.status.default property");
        }

        ModuleRevisionId mrid = null;
        if (resolveId == null) {
            mrid = ModuleRevisionId.newInstance(organisation, module, revision);
        }
        boolean isLeading = false;
        try {
            if (!deliveryList.exists()) {
                isLeading = true;
            }

            loadDeliveryList();

            PublishingDependencyRevisionResolver drResolver;
            if (deliverTarget != null && deliverTarget.trim().length() > 0) {
                drResolver = new DeliverDRResolver();
            } else {
                drResolver = new DefaultPublishingDRResolver();
            }

            DeliverOptions options = new DeliverOptions(status, pubdate, drResolver,
                    doValidate(settings), replacedynamicrev, splitConfs(conf))
                    .setResolveId(resolveId).setReplaceForcedRevisions(isReplaceForcedRev())
                    .setGenerateRevConstraint(generateRevConstraint).setMerge(merge)
                    .setPubBranch(pubBranch);
            if (mrid == null) {
                ivy.deliver(pubRevision, deliverpattern, options);
            } else {
                ivy.deliver(mrid, pubRevision, deliverpattern, options);
            }
        } catch (Exception e) {
            throw new BuildException("impossible to deliver " + mrid == null ? resolveId : mrid
                    + ": " + e, e);
        } finally {
            if (isLeading) {
                if (deliveryList.exists()) {
                    deliveryList.delete();
                }
            }
        }
    }

    private void loadDeliveryList() {
        Property property = (Property) getProject().createTask("property");
        property.setOwningTarget(getOwningTarget());
        property.init();
        property.setFile(deliveryList);
        property.perform();
    }

    private void appendDeliveryList(String msg) {
        Echo echo = (Echo) getProject().createTask("echo");
        echo.setOwningTarget(getOwningTarget());
        echo.init();
        echo.setFile(deliveryList);
        echo.setMessage(msg + "\n");
        echo.setAppend(true);
        echo.perform();
    }

}
