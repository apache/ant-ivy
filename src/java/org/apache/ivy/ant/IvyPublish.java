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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DateUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;

/**
 * This task allow to publish a module revision to an Ivy repository.
 */
public class IvyPublish extends IvyTask {
    private String organisation;

    private String module;

    private String revision;

    private String pubRevision;

    private String srcivypattern;

    private String status;

    private String conf = null;

    private String pubdate;

    private String deliverTarget;

    private String publishResolverName = null;

    private List<String> artifactspattern = new ArrayList<String>();

    private File deliveryList;

    private boolean publishivy = true;

    private boolean warnonmissing = true;

    private boolean haltonmissing = true;

    private boolean overwrite = false;

    private boolean update = false;

    private boolean merge = true;

    private boolean replacedynamicrev = true;

    private boolean forcedeliver;

    private Collection<Artifact> artifacts = new ArrayList<Artifact>();

    private String pubBranch;

    public void setCache(File cache) {
        cacheAttributeNotSupported();
    }

    public String getSrcivypattern() {
        return srcivypattern;
    }

    public void setSrcivypattern(String destivypattern) {
        srcivypattern = destivypattern;
    }

    /**
     * @deprecated use {@link #getSrcivypattern()} instead.
     */
    @Deprecated
    public String getDeliverivypattern() {
        return srcivypattern;
    }

    /**
     * @deprecated use {@link #setSrcivypattern(String)} instead.
     */
    @Deprecated
    public void setDeliverivypattern(String destivypattern) {
        srcivypattern = destivypattern;
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

    public void setConf(String conf) {
        this.conf = conf;
    }

    public void setDelivertarget(String deliverTarget) {
        this.deliverTarget = deliverTarget;
    }

    public void setDeliveryList(File deliveryList) {
        this.deliveryList = deliveryList;
    }

    public String getResolver() {
        return publishResolverName;
    }

    public void setResolver(String publishResolverName) {
        this.publishResolverName = publishResolverName;
    }

    public String getArtifactspattern() {
        return artifactspattern.isEmpty() ? null : artifactspattern.get(0);
    }

    public void setArtifactspattern(String artifactsPattern) {
        artifactspattern.clear();
        artifactspattern.add(artifactsPattern);
    }

    public void addArtifactspattern(String artifactsPattern) {
        artifactspattern.add(artifactsPattern);
    }

    public void addConfiguredArtifacts(ArtifactsPattern p) {
        artifactspattern.add(p.getPattern());
    }

    public boolean isReplacedynamicrev() {
        return replacedynamicrev;
    }

    public void setReplacedynamicrev(boolean replacedynamicrev) {
        this.replacedynamicrev = replacedynamicrev;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    @Override
    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        organisation = getProperty(organisation, settings, "ivy.organisation");
        module = getProperty(module, settings, "ivy.module");
        revision = getProperty(revision, settings, "ivy.revision");
        pubBranch = getProperty(pubBranch, settings, "ivy.deliver.branch");
        pubRevision = getProperty(pubRevision, settings, "ivy.deliver.revision");
        if (artifactspattern.isEmpty()) {
            String p = getProperty(null, settings, "ivy.publish.src.artifacts.pattern");
            if (p != null) {
                artifactspattern.add(p);
            }
        }
        if (srcivypattern == null) {
            srcivypattern = getArtifactspattern();
        }
        status = getProperty(status, settings, "ivy.status");
        if (organisation == null) {
            throw new BuildException("no organisation provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'organisation' "
                    + "or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (module == null) {
            throw new BuildException("no module name provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'module' "
                    + "or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (revision == null) {
            throw new BuildException("no module revision provided for ivy publish task: "
                    + "It can either be set explicitely via the attribute 'revision' "
                    + "or via 'ivy.revision' property or a prior call to <resolve/>");
        }
        if (artifactspattern.isEmpty()) {
            throw new BuildException(
                    "no artifacts pattern: either provide it through parameter or "
                            + "through ivy.publish.src.artifacts.pattern property");
        }
        if (publishResolverName == null) {
            throw new BuildException(
                    "no publish deliver name: please provide it through parameter 'resolver'");
        }
        if ("working".equals(revision)) {
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
        if (status == null) {
            throw new BuildException("no status provided: either provide it as parameter "
                    + "or through the ivy.status.default property");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation, module, revision);
        try {
            File ivyFile = getProject().resolveFile(
                IvyPatternHelper.substitute(srcivypattern, organisation, module, pubRevision,
                    "ivy", "ivy", "xml"));
            if (publishivy && (!ivyFile.exists() || forcedeliver)) {
                IvyDeliver deliver = new IvyDeliver();
                deliver.setSettingsRef(getSettingsRef());
                deliver.setTaskName(getTaskName());
                deliver.setProject(getProject());
                deliver.setDeliverpattern(getSrcivypattern());
                deliver.setDelivertarget(deliverTarget);
                deliver.setDeliveryList(deliveryList);
                deliver.setModule(getModule());
                deliver.setOrganisation(getOrganisation());
                deliver.setPubdate(DateUtil.format(pubdate));
                deliver.setPubrevision(getPubrevision());
                deliver.setPubbranch(getPubbranch());
                deliver.setRevision(getRevision());
                deliver.setStatus(getStatus());
                deliver.setValidate(doValidate(settings));
                deliver.setReplacedynamicrev(isReplacedynamicrev());
                deliver.setMerge(merge);
                deliver.setConf(conf);

                deliver.execute();
            }

            ivy.publish(
                mrid,
                artifactspattern,
                publishResolverName,
                new PublishOptions().setPubrevision(getPubrevision()).setPubbranch(getPubbranch())
                        .setSrcIvyPattern(publishivy ? srcivypattern : null).setStatus(getStatus())
                        .setPubdate(pubdate)
                        .setExtraArtifacts(artifacts.toArray(new Artifact[artifacts.size()]))
                        .setValidate(doValidate(settings)).setOverwrite(overwrite)
                        .setUpdate(update).setMerge(merge).setWarnOnMissing(warnonmissing)
                        .setHaltOnMissing(haltonmissing).setConfs(splitConfs(conf)));
        } catch (Exception e) {
            if (e instanceof BuildException) {
                throw (BuildException) e;
            }
            throw new BuildException("impossible to publish artifacts for " + mrid + ": " + e, e);
        }
    }

    public PublishArtifact createArtifact() {
        PublishArtifact art = new PublishArtifact();
        artifacts.add(art);
        return art;
    }

    public boolean isPublishivy() {
        return publishivy;
    }

    public void setPublishivy(boolean publishivy) {
        this.publishivy = publishivy;
    }

    public boolean isWarnonmissing() {
        return warnonmissing;
    }

    public void setWarnonmissing(boolean warnonmissing) {
        this.warnonmissing = warnonmissing;
    }

    public boolean isHaltonmissing() {
        return haltonmissing;
    }

    public void setHaltonmissing(boolean haltonmissing) {
        this.haltonmissing = haltonmissing;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setForcedeliver(boolean b) {
        forcedeliver = b;
    }

    public boolean isForcedeliver() {
        return forcedeliver;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public class PublishArtifact implements Artifact, DynamicAttribute {
        private String ext;

        private String name;

        private String type;

        private Map<String, String> extra = new HashMap<String, String>();

        public String[] getConfigurations() {
            return null;
        }

        public String getExt() {
            return ext == null ? type : ext;
        }

        public ArtifactRevisionId getId() {
            return null;
        }

        public ModuleRevisionId getModuleRevisionId() {
            return null;
        }

        public String getName() {
            return name;
        }

        public Date getPublicationDate() {
            return null;
        }

        public String getType() {
            return type;
        }

        public URL getUrl() {
            return null;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAttribute(String attName) {
            return extra.get(attName);
        }

        public Map<String, String> getAttributes() {
            return extra;
        }

        public String getExtraAttribute(String attName) {
            return extra.get(attName);
        }

        public Map<String, String> getExtraAttributes() {
            return extra;
        }

        public Map<String, String> getQualifiedExtraAttributes() {
            return extra;
        }

        public boolean isMetadata() {
            return false;
        }

        public void setDynamicAttribute(String name, String value) {
            extra.put(name, value);
        }
    }

    public static class ArtifactsPattern {
        private String pattern;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }
}
