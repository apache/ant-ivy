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
package org.apache.ivy.core.publish;

import java.util.Date;

import org.apache.ivy.core.module.descriptor.Artifact;

/**
 * A set of options used during publish related tasks The publish can update the ivy file to publish
 * if update is set to true. In this case it will use the given pubrevision, pubdate and status. If
 * pudate is null it will default to the current date. If status is null it will default to the
 * current ivy file status (which itself defaults to integration if none is found). If update is
 * false, then if the revision is not the same in the ivy file than the one expected (given as
 * parameter), this method will fail with an IllegalArgumentException. pubdate and status are not
 * used if update is false. extra artifacts can be used to publish more artifacts than actually
 * declared in the ivy file. This can be useful to publish additional metadata or reports. The extra
 * artifacts array can be null (= no extra artifacts), and if non null only the name, type, ext url
 * and extra attributes of the artifacts are really used. Other methods (on the artifacts) can
 * return null safely.
 * 
 * @see PublishEngine
 */
public class PublishOptions {
    private String srcIvyPattern;

    private String pubrevision;

    private String status;

    private Date pubdate;

    private Artifact[] extraArtifacts;

    private boolean validate;

    private boolean overwrite;

    private boolean update;

    private boolean merge = true;

    private String[] confs;

    private boolean haltonmissing;

    private String pubBranch;

    private boolean warnonmissing;

    public String[] getConfs() {
        return confs;
    }

    public PublishOptions setConfs(String[] confs) {
        this.confs = confs;
        return this;
    }

    public Artifact[] getExtraArtifacts() {
        return extraArtifacts;
    }

    public PublishOptions setExtraArtifacts(Artifact[] extraArtifacts) {
        this.extraArtifacts = extraArtifacts;
        return this;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public PublishOptions setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public Date getPubdate() {
        return pubdate;
    }

    public PublishOptions setPubdate(Date pubdate) {
        this.pubdate = pubdate;
        return this;
    }

    public String getPubrevision() {
        return pubrevision;
    }

    public PublishOptions setPubrevision(String pubrevision) {
        this.pubrevision = pubrevision;
        return this;
    }

    public String getSrcIvyPattern() {
        return srcIvyPattern;
    }

    public PublishOptions setSrcIvyPattern(String srcIvyPattern) {
        this.srcIvyPattern = srcIvyPattern;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public PublishOptions setStatus(String status) {
        this.status = status;
        return this;
    }

    public boolean isUpdate() {
        return update;
    }

    public PublishOptions setUpdate(boolean update) {
        this.update = update;
        return this;
    }

    public boolean isMerge() {
        return merge;
    }

    public PublishOptions setMerge(boolean merge) {
        this.merge = merge;
        return this;
    }

    public boolean isValidate() {
        return validate;
    }

    public PublishOptions setValidate(boolean validate) {
        this.validate = validate;
        return this;
    }

    public boolean isHaltOnMissing() {
        return haltonmissing;
    }

    public PublishOptions setHaltOnMissing(boolean haltonmissing) {
        this.haltonmissing = haltonmissing;
        return this;
    }

    public String getPubBranch() {
        return pubBranch;
    }

    public PublishOptions setPubbranch(String pubbranch) {
        this.pubBranch = pubbranch;
        return this;
    }

    public boolean isWarnOnMissing() {
        return warnonmissing;
    }

    public PublishOptions setWarnOnMissing(boolean warnonmissing) {
        this.warnonmissing = warnonmissing;
        return this;
    }

}
